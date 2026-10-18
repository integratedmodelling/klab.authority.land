package org.integratedmodelling.klab.authority.land;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.integratedmodelling.klab.api.collections.Pair;
import org.integratedmodelling.klab.api.knowledge.Codelist;
import org.integratedmodelling.klab.api.knowledge.impl.CodelistImpl;
import org.integratedmodelling.klab.api.services.Authority;
import org.integratedmodelling.klab.api.services.reasoner.objects.AuthorityIdentitySyntax;
import org.integratedmodelling.klab.api.services.runtime.Notification;

/** Earth-cover patterns and worldview-scoped legend translations. */
@org.integratedmodelling.klab.api.services.reasoner.Authority(
    urn = LandAuthority.URN, version = "1.0.0", embeddable = true, searchable = true,
    codelists = {"corine_2018_draft", "igbp_modis61_draft", "get21_ecosystem_pilot",
        "eunis2021_terrestrial_pilot", "eunis2022_marine_pilot", "eunis2012_inland_pilot", "ecosystem_rules_pilot"})
public final class LandAuthority implements Authority {
  public static final String URN = "klab.authority.land";
  private final Map<String, PatternSemantics> worldviews = new ConcurrentHashMap<>();
  private final Map<String, PatternCatalog> catalogs = new ConcurrentHashMap<>();
  private final Map<String, Bridge> bridges = new ConcurrentHashMap<>();
  private record Bridge(ConfigurationRequest request, PatternSemantics semantics,
                        List<PatternCatalog> catalogs, Map<String, String> identities, MediationPolicy mediation) {}

  /** Load syntax-checked seed data; worldview validation still occurs separately for each bridge. */
  public LandAuthority() {
    registerCatalog(PatternCatalogYaml.loadClasspath("/catalogs/corine_2018_draft.yaml"));
    registerCatalog(PatternCatalogYaml.loadClasspath("/catalogs/igbp_modis61_draft.yaml"));
    registerCatalog(PatternCatalogYaml.loadClasspath("/catalogs/get21_ecosystem_pilot.yaml"));
    registerCatalog(PatternCatalogYaml.loadClasspath("/catalogs/eunis2021_terrestrial_pilot.yaml"));
    registerCatalog(PatternCatalogYaml.loadClasspath("/catalogs/eunis2022_marine_pilot.yaml"));
    registerCatalog(PatternCatalogYaml.loadClasspath("/catalogs/eunis2012_inland_pilot.yaml"));
    registerCatalog(PatternCatalogYaml.loadClasspath("/catalogs/ecosystem_rules_pilot.yaml"));
  }

  /** Includes provenance and known losses; registration does not attest semantic validity. */
  public Map<String, PatternCatalog> getCatalogs() { return Map.copyOf(catalogs); }

  /** Register on the actual provider instance before configuring its worldview bridge. */
  public void bindWorldview(PatternSemantics semantics) {
    Objects.requireNonNull(semantics);
    if (semantics.worldview() == null || semantics.worldview().isBlank())
      throw new IllegalArgumentException("A semantic context needs a worldview");
    var existing = worldviews.putIfAbsent(semantics.worldview(), semantics);
    if (existing != null && existing != semantics)
      throw new IllegalStateException("Unbind the existing worldview before replacing semantic access");
  }

  /** Release every bridge for this worldview when unloading it. */
  public void unbindWorldview(String worldview) {
    worldviews.remove(worldview);
    bridges.entrySet().removeIf(e -> e.getValue().request.worldview().equals(worldview));
  }

  public void registerCatalog(PatternCatalog catalog) {
    Objects.requireNonNull(catalog);
    var existing = catalogs.putIfAbsent(catalog.id(), catalog);
    if (existing != null && !existing.equals(catalog))
      throw new IllegalArgumentException("Catalog IDs are immutable; use a new versioned ID");
  }

  @Override public String getUrn() { return URN; }
  @Override public CachePolicy getCachePolicy() { return new CachePolicy("cover-fuzzy-ratios-1", 0, 0, 0); }

  @Override public String configure(ConfigurationRequest request) {
    if (!URN.equals(request.parameters().get("urn"))) throw new IllegalArgumentException("Wrong LAND provider URN");
    for (var key : request.parameters().keySet())
      if (!Set.of("urn", "domain", "catalogs", "semanticBoundaries", "mediation").contains(key))
        throw new IllegalArgumentException("Unknown LAND parameter: " + key);
    request.requireSupportedBoundaries(Set.of());
    var semantics = worldviews.get(request.worldview());
    if (semantics == null) throw new IllegalStateException("Bind semantic access for worldview " + request.worldview() + " before configuring LAND");
    semantics.validate(request.rootIdentity(), PatternSemantics.Role.IDENTITY);
    MediationPolicy mediation = request.parameters().containsKey("mediation")
        ? MediationPolicy.parse(request.parameters().get("mediation"),semantics) : MediationPolicy.disabled();
    if (!(request.parameters().get("domain") instanceof String domainName))
      throw new IllegalArgumentException("domain must be LULC or ECOSYSTEM");
    var domain = PatternCatalog.Domain.valueOf(domainName);
    Object selection = request.parameters().getOrDefault("catalogs", List.of());
    if (!(selection instanceof List<?> ids)) throw new IllegalArgumentException("catalogs must be a list of registered catalog IDs");
    var selected = new ArrayList<PatternCatalog>();
    var patterns = new LinkedHashMap<String, String>();
    var unique = new HashSet<String>();
    for (var id : ids) {
      var catalog = id instanceof String ? catalogs.get(id) : null;
      if (catalog == null || catalog.domain() != domain || !unique.add(catalog.id()))
        throw new IllegalArgumentException("Unknown, duplicate or wrong-domain catalog: " + id);
      selected.add(catalog);
      for (var entry : catalog.entries()) {
        String code = entry.identityCode();
        patterns.put(code, validateCode(code, semantics));
      }
    }
    String id = UUID.randomUUID().toString();
    bridges.put(id, new Bridge(request, semantics, List.copyOf(selected), Map.copyOf(patterns),mediation));
    return id;
  }

  @Override public void releaseConfiguration(String id) { if (id != null) bridges.remove(id); }

  /** Validate concepts individually, without imposing inherency or supplying identity bearers. */
  public CoverPattern validatePattern(String configurationId, String expression) {
    var bridge = bridge(configurationId);
    return validate(PatternParser.parse(expression), bridge.semantics);
  }

  private CoverPattern validate(CoverPattern node, PatternSemantics semantics) {
    String concept = semantics.validate(node.concept(), node.isConfiguration()
        ? PatternSemantics.Role.CONFIGURATION : PatternSemantics.Role.IDENTITY);
    var probe = PatternParser.parse("land:Probe(" + concept + ")");
    if (probe.arguments().size() != 1 || probe.arguments().getFirst().isConfiguration()
        || probe.arguments().getFirst().marks() != 0 || !probe.arguments().getFirst().concept().equals(concept))
      throw new IllegalArgumentException("Semantic resolver returned a non-reference");
    return new CoverPattern(concept, node.marks(), node.arguments().stream().map(n -> validate(n, semantics)).toList());
  }

  public PatternComparison comparePatterns(String configurationId, String left, String right) {
    var bridge = bridge(configurationId);
    return PatternComparison.compare(validatePattern(configurationId, left), validatePattern(configurationId, right), bridge.semantics);
  }

  /** Validate a richer definition, retaining explicit bearer/evaluator obligations. */
  public RuleValidation validateRule(String configurationId, PatternRule rule) {
    return validateRule(rule, bridge(configurationId).semantics);
  }
  private RuleValidation validateRule(PatternRule rule, PatternSemantics semantics) {
    String composition = validate(PatternParser.parse(rule.composition()), semantics).canonical();
    return RuleValidation.validate(new PatternRule(2,composition,rule.participants(),rule.profiles(),rule.criteria(),rule.provenance()),semantics);
  }
  private String validateCode(String code, PatternSemantics semantics) {
    if (code.startsWith(PatternRuleYaml.PREFIX) && !(semantics instanceof RuleSemantics))
      throw new IllegalStateException("Typed rule identities require RuleSemantics; use validateRule for a provisional definition");
    return code.startsWith(PatternRuleYaml.PREFIX)
        ? PatternRuleYaml.canonical(validateRule(PatternRuleYaml.decode(code),semantics).rule())
        : validate(PatternParser.parse(code),semantics).canonical();
  }
  public PatternComparison compareIdentities(String configurationId, String left, String right) {
    var semantics = bridge(configurationId).semantics;
    if (left.startsWith(PatternRuleYaml.PREFIX) && right.startsWith(PatternRuleYaml.PREFIX))
      return RuleComparison.compare(validateRule(configurationId,PatternRuleYaml.decode(left)).rule(),
          validateRule(configurationId,PatternRuleYaml.decode(right)).rule(),semantics);
    if (left.startsWith(PatternRuleYaml.PREFIX) || right.startsWith(PatternRuleYaml.PREFIX))
      return new PatternComparison(PatternComparison.Status.UNKNOWN,List.of("Composition-only and typed rule identities require an explicit bridge"));
    return comparePatterns(configurationId,left,right);
  }
  @Override public SemanticDistance semanticDistance(String configurationId, String targetId, String candidateId) {
    var comparison = compareIdentities(configurationId,targetId,candidateId);
    if (comparison.status() == PatternComparison.Status.EQUIVALENT)
      return new SemanticDistance(DistanceStatus.COMPATIBLE,0,comparison.explanations());
    if (comparison.status() == PatternComparison.Status.RIGHT_REFINES_LEFT)
      return new SemanticDistance(DistanceStatus.COMPATIBLE,1,comparison.explanations());
    var result = mediate(configurationId,targetId,candidateId);
    var explanation = new ArrayList<>(result.explanations());
    explanation.add("Mediation policy " + result.policyFingerprint() + "; overlap=" + result.overlap());
    if (result.status() != MediationResult.Status.DEFINED || !result.rightAdmittedForLeft())
      return new SemanticDistance(DistanceStatus.UNKNOWN,-1,explanation);
    if (result.leftEntailsRight() && result.rightEntailsLeft()) return new SemanticDistance(DistanceStatus.COMPATIBLE,0,explanation);
    int distance = java.math.BigDecimal.ONE.subtract(result.overlap().lower()).multiply(new java.math.BigDecimal("100"))
        .setScale(0,java.math.RoundingMode.CEILING).intValueExact();
    return new SemanticDistance(DistanceStatus.COMPATIBLE,Math.max(1,Math.min(100,distance)),explanation);
  }

  /** Immutable snapshot. Changing a policy requires configuring a new independent bridge. */
  public MediationPolicy getMediationPolicy(String configurationId) { return bridge(configurationId).mediation; }

  /** Symmetric fuzzy overlap plus both directional proofs/admissions; never substitutes overlap for entailment. */
  public MediationResult mediate(String configurationId,String left,String right) {
    var bridge = bridge(configurationId);
    var exact = compareIdentities(configurationId,left,right);
    boolean a = left.startsWith(PatternRuleYaml.PREFIX), b = right.startsWith(PatternRuleYaml.PREFIX);
    if (a != b) return new MediationResult(MediationResult.Status.UNKNOWN,null,MediationResult.OverlapDecision.UNASSESSED,
        false,false,false,false,exact,bridge.mediation,bridge.mediation.fingerprint(),bridge.semantics.worldview(),left,right,List.of(),
        List.of("Expression-only and typed-rule identities require an explicit semantic bridge"));
    PatternRule ar = a ? validateRule(configurationId,PatternRuleYaml.decode(left)).rule() : null;
    PatternRule br = b ? validateRule(configurationId,PatternRuleYaml.decode(right)).rule() : null;
    CoverPattern ap = a ? PatternParser.parse(ar.composition()) : validatePattern(configurationId,left);
    CoverPattern bp = b ? PatternParser.parse(br.composition()) : validatePattern(configurationId,right);
    return FuzzyMediation.compare(ap,bp,ar,br,bridge.semantics,bridge.mediation,exact);
  }

  public record AuditFinding(String left, String right, PatternComparison comparison) {}

  /** UNKNOWN pairs are unresolved, never findings of inconsistency. Cost is quadratic in entry count. */
  public List<AuditFinding> auditCatalog(String configurationId, String catalogId) {
    var bridge = bridge(configurationId);
    var catalog = bridge.catalogs.stream().filter(c -> c.id().equals(catalogId)).findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Catalog is not selected by this bridge"));
    var findings = new ArrayList<AuditFinding>();
    for (int i = 0; i < catalog.entries().size(); i++) for (int j = i + 1; j < catalog.entries().size(); j++) {
      var a = catalog.entries().get(i); var b = catalog.entries().get(j);
      findings.add(new AuditFinding(a.alias(), b.alias(), compareIdentities(configurationId, a.identityCode(), b.identityCode())));
    }
    return List.copyOf(findings);
  }

  @Override public Identity resolveIdentity(String configurationId, String identityId) {
    var bridge = bridge(configurationId);
    try {
      String canonical = validateCode(identityId, bridge.semantics);
      var descriptions = new ArrayList<String>();
      if (canonical.startsWith(PatternRuleYaml.PREFIX)) {
        var validation = validateRule(PatternRuleYaml.decode(canonical),bridge.semantics);
        descriptions.add(validation.ready() ? "Typed rule semantic checks completed; observations and evaluator bindings remain separate."
            : "Provisional typed definition. Outstanding obligations:\n\n- " + String.join("\n- ",validation.obligations()));
      }
      var labels = new LinkedHashSet<String>();
      for (var catalog : bridge.catalogs) for (var entry : catalog.entries())
        if (canonical.equals(bridge.identities.get(entry.identityCode()))) {
          labels.add(entry.label());
          descriptions.add(entryDescription(catalog, entry));
        }
      return new LandIdentity(canonical, conceptName(canonical), bridge.request.name(), bridge.request.rootIdentity(),
          "Earth-cover pattern:\n\n```text\n" + canonical + "\n```\n\n" + String.join("\n\n", descriptions),
          labels.isEmpty() ? canonical : String.join(" / ", labels),
          AuthorityIdentitySyntax.encode(bridge.request.name(), canonical), List.of());
    } catch (RuntimeException e) { return failure(bridge, identityId, e.getMessage()); }
  }

  @Override public Identity reconcile(String configurationId, Map<String, String> fields) {
    var bridge = bridge(configurationId);
    if (fields == null || fields.isEmpty() || !Set.of("expression", "alias", "catalog").containsAll(fields.keySet()))
      return failure(bridge, "", "Use expression, or alias with optional catalog");
    if (fields.containsKey("expression")) {
      if (fields.size() != 1) return failure(bridge, "", "expression cannot be combined with alias fields");
      return resolveIdentity(configurationId, fields.get("expression"));
    }
    var matches = new TreeSet<String>();
    for (var catalog : bridge.catalogs) if (!fields.containsKey("catalog") || catalog.id().equals(fields.get("catalog")))
      for (var entry : catalog.entries()) if (entry.alias().equals(fields.get("alias")))
        matches.add(bridge.identities.get(entry.identityCode()));
    return matches.size() == 1 ? resolveIdentity(configurationId, matches.first())
        : failure(bridge, Objects.toString(fields.get("alias"), ""), "Alias is absent or ambiguous; specify a catalog");
  }

  @Override public List<Identity> search(String query, String subAuthority, String configurationId) {
    var bridge = bridge(configurationId);
    if (subAuthority != null && !subAuthority.isBlank()) throw new IllegalArgumentException("LAND has no sub-authorities");
    if (query == null || query.isBlank()) throw new IllegalArgumentException("A search query is required");
    String q = query.toLowerCase(Locale.ROOT);
    var matches = new TreeSet<String>();
    for (var catalog : bridge.catalogs) for (var entry : catalog.entries())
      if ((catalog.id() + " " + Objects.toString(entry.sourceCode(), "") + " " + entry.alias()
          + " " + entry.label()).toLowerCase(Locale.ROOT).contains(q))
        matches.add(bridge.identities.get(entry.identityCode()));
    return matches.stream().limit(100).map(p -> resolveIdentity(configurationId, p)).toList();
  }

  @Override public Map<String, Codelist> getCodelists(String configurationId) {
    var bridge = bridge(configurationId);
    var result = new LinkedHashMap<String, Codelist>();
    for (var catalog : bridge.catalogs) {
      var list = new CodelistImpl();
      list.setName(catalog.id()); list.setUrn(URN + ":" + catalog.id() + ":" + catalog.version());
      list.setAuthorityId(bridge.request.name()); list.setWorldview(bridge.request.worldview());
      list.setDescription((catalog.metadata().tentative() ? "TENTATIVE TEST DATA. " : "")
          + catalog.metadata().title() + "; translation " + catalog.version() + "; source edition: "
          + catalog.metadata().sourceVersion() + "; source: " + catalog.source() + "\n\n"
          + String.join("\n\n", catalog.metadata().notes()));
      list.setEntries(catalog.entries().stream().map(entry -> new CodelistImpl.Entry(bridge.request.name(),
          entry.alias(), bridge.identities.get(entry.identityCode()), entryDescription(catalog, entry), true)).toList());
      result.put(catalog.id(), list);
    }
    return Collections.unmodifiableMap(result);
  }

  @Override public Map<String, CodelistDefinition> getCodelistDefinitions(String configurationId) {
    var result = new LinkedHashMap<String, CodelistDefinition>();
    getCodelists(configurationId).forEach((id, list) -> result.put(id,
        new CodelistDefinition(list, catalogs.get(id).metadata().acceptsProposals())));
    return Map.copyOf(result);
  }

  @Override public Authority subAuthority(String catalog) { throw new IllegalArgumentException("Use separate LAND bridges and codelists"); }
  @Override public Capabilities getCapabilities() {
    return new Capabilities() {
      @Override public String getDescription() { return "Worldview-validated earth-cover patterns and curated legend translations"; }
      @Override public boolean isSearchable() { return true; }
      @Override public boolean isFuzzy() { return false; }
      @Override public boolean isSemanticDistanceSupported() { return true; }
      @Override public boolean isReconciliationSupported() { return true; }
      @Override public List<Pair<String, String>> getSubAuthorities() { return List.of(); }
      @Override public List<String> getDocumentationFormats() { return List.of(); }
      @Override public String getWorldview() { return null; }
    };
  }

  private Bridge bridge(String id) {
    var bridge = id == null ? null : bridges.get(id);
    if (bridge == null) throw new IllegalArgumentException("Unknown or released LAND configuration");
    return bridge;
  }
  private Identity failure(Bridge bridge, String id, String message) {
    return new LandIdentity(id, null, bridge.request.name(), null, message, id, null,
        List.of(Notification.error("LAND: " + message)));
  }
  private static String entryDescription(PatternCatalog catalog, PatternCatalog.Entry entry) {
    return (catalog.metadata().tentative() ? "**Tentative test translation.** " : "")
        + entry.label() + " (" + catalog.id()
        + (entry.sourceCode() == null ? "" : ", source code " + entry.sourceCode()) + "). "
        + "Source edition: " + catalog.metadata().sourceVersion() + ". "
        + "[Source](" + (entry.source() == null ? catalog.source() : entry.source()) + ").\n\n"
        + String.join("\n\n", entry.notes());
  }
  private static String conceptName(String canonical) {
    try { return "Pattern_" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8))); }
    catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
  }
}
