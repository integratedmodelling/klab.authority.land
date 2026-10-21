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
import org.integratedmodelling.klab.api.services.SnapshottingAuthority;
import org.integratedmodelling.klab.api.services.ConfiguredAuthoritySnapshot;
import org.integratedmodelling.klab.api.services.reasoner.objects.AuthorityIdentitySyntax;
import org.integratedmodelling.klab.api.services.runtime.Notification;

/** Earth-cover patterns and worldview-scoped legend translations. */
@org.integratedmodelling.klab.api.services.reasoner.Authority(
    urn = LandAuthority.URN, version = "1.0.0", embeddable = true, searchable = true,
    codelists = {"corine_entry_hierarchy_example", "corine_2018_draft", "igbp_modis61_draft", "get21_ecosystem_pilot",
        "eunis2021_terrestrial_pilot", "eunis2022_marine_pilot", "eunis2012_inland_pilot", "ecosystem_rules_pilot"})
public final class LandAuthority implements SnapshottingAuthority {
  public static final String URN = "klab.authority.land";
  private final Map<String, PatternSemantics> worldviews = new ConcurrentHashMap<>();
  private final Map<String, PatternCatalog> catalogs = new ConcurrentHashMap<>();
  private final Map<String, Bridge> bridges = new ConcurrentHashMap<>();
  private boolean closed;
  /** Entry bridges carry no pattern reasoning capability. The host checks the declared root. */
  private record EntrySemantics(String worldview) implements PatternSemantics {
    @Override public String validate(String reference, Role role) {
      throw new IllegalStateException("ENTRY_ONLY bridges cannot validate or execute pattern/rule definitions");
    }
  }
  private record BoundEntry(PatternCatalog catalog, PatternCatalog.Entry entry) {}
  private record Bridge(ConfigurationRequest request, PatternSemantics semantics,
                        List<PatternCatalog> catalogs, Map<String, BoundEntry> entries, MediationPolicy mediation) {}

  /** Load syntax-checked seed data; worldview validation still occurs separately for each bridge. */
  public LandAuthority() {
    registerCatalog(PatternCatalogYaml.loadClasspath("/catalogs/corine_entry_hierarchy_example.yaml"));
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
  public synchronized void bindWorldview(PatternSemantics semantics) {
    requireOpen();
    Objects.requireNonNull(semantics);
    if (semantics.worldview() == null || semantics.worldview().isBlank())
      throw new IllegalArgumentException("A semantic context needs a worldview");
    var existing = worldviews.putIfAbsent(semantics.worldview(), semantics);
    if (existing != null && existing != semantics)
      throw new IllegalStateException("Unbind the existing worldview before replacing semantic access");
  }

  /** Release every bridge for this worldview when unloading it. */
  public synchronized void unbindWorldview(String worldview) {
    worldviews.remove(worldview);
    bridges.entrySet().removeIf(e -> e.getValue().request.worldview().equals(worldview));
  }

  public synchronized void registerCatalog(PatternCatalog catalog) {
    requireOpen();
    Objects.requireNonNull(catalog);
    var existing = catalogs.putIfAbsent(catalog.id(), catalog);
    if (existing != null && !existing.equals(catalog))
      throw new IllegalArgumentException("Catalog IDs are immutable; use a new versioned ID");
  }

  @Override public String getUrn() { return URN; }
  @Override public CachePolicy getCachePolicy() { return new CachePolicy("catalog-entry-identities-2", 0, 0, 0); }

  @Override public synchronized String configure(ConfigurationRequest request) {
    requireOpen();
    if (!URN.equals(request.parameters().get("urn"))) throw new IllegalArgumentException("Wrong LAND provider URN");
    for (var key : request.parameters().keySet())
      if (!Set.of("urn", "domain", "catalogs", "semanticBoundaries", "mediation", "mode").contains(key))
        throw new IllegalArgumentException("Unknown LAND parameter: " + key);
    request.requireSupportedBoundaries(Set.of());
    boolean entriesOnly = request.parameters().containsKey("mode");
    if (entriesOnly && !"ENTRY_ONLY".equals(request.parameters().get("mode")))
      throw new IllegalArgumentException("The explicit LAND mode must be ENTRY_ONLY");
    if (entriesOnly && request.parameters().containsKey("mediation"))
      throw new IllegalArgumentException("ENTRY_ONLY cannot configure pattern mediation");
    if (entriesOnly) request = new ConfiguredAuthoritySnapshot(EntrySnapshotCodec.FORMAT, URN, request, new byte[]{1}).request();
    PatternSemantics semantics = entriesOnly ? new EntrySemantics(request.worldview()) : worldviews.get(request.worldview());
    if (semantics == null) throw new IllegalStateException("Bind semantic access for worldview " + request.worldview() + " before configuring LAND");
    if (!entriesOnly) semantics.validate(request.rootIdentity(), PatternSemantics.Role.IDENTITY);
    MediationPolicy mediation = request.parameters().containsKey("mediation")
        ? MediationPolicy.parse(request.parameters().get("mediation"),semantics) : MediationPolicy.disabled();
    if (!(request.parameters().get("domain") instanceof String domainName))
      throw new IllegalArgumentException("domain must be LULC or ECOSYSTEM");
    var domain = PatternCatalog.Domain.valueOf(domainName);
    Object selection = request.parameters().getOrDefault("catalogs", List.of());
    if (!(selection instanceof List<?> ids)) throw new IllegalArgumentException("catalogs must be a list of registered catalog IDs");
    var selected = new ArrayList<PatternCatalog>();
    var entries = new LinkedHashMap<String, BoundEntry>();
    var unique = new HashSet<String>();
    for (var id : ids) {
      var catalog = id instanceof String ? catalogs.get(id) : null;
      if (catalog == null || catalog.domain() != domain || !unique.add(catalog.id()))
        throw new IllegalArgumentException("Unknown, duplicate or wrong-domain catalog: " + id);
      selected.add(catalog);
      for (var entry : catalog.entries()) {
        if (!entriesOnly && entry.definitionReview() == PatternCatalog.DefinitionReview.REVIEWED) {
          if (entry.rule() == null) validateCode(entry.definitionCode(), semantics);
          else {
            if (!(semantics instanceof RuleSemantics))
              throw new IllegalStateException("Reviewed typed definitions require RuleSemantics");
            if (!validateRule(entry.rule(), semantics).ready())
              throw new IllegalArgumentException("Reviewed definition still has unresolved obligations");
          }
        }
        entries.put(catalog.identityCode(entry), new BoundEntry(catalog, entry));
      }
    }
    String id = UUID.randomUUID().toString();
    bridges.put(id, new Bridge(request, semantics, List.copyOf(selected), Map.copyOf(entries),mediation));
    return id;
  }

  @Override public synchronized void releaseConfiguration(String id) { if (id != null) bridges.remove(id); }

  @Override public synchronized ConfiguredAuthoritySnapshot captureConfiguration(String id) {
    var bridge = bridge(id);
    if (!(bridge.semantics instanceof EntrySemantics))
      throw new IllegalStateException("Only ENTRY_ONLY bridges have an offline snapshot contract");
    return EntrySnapshotCodec.capture(bridge.request, bridge.catalogs);
  }

  @Override public synchronized String restoreConfiguration(ConfigurationRequest request, ConfiguredAuthoritySnapshot snapshot) {
    requireOpen();
    var selected = EntrySnapshotCodec.restore(request, snapshot);
    var entries = new LinkedHashMap<String, BoundEntry>();
    for (var catalog : selected) for (var entry : catalog.entries())
      entries.put(catalog.identityCode(entry), new BoundEntry(catalog, entry));
    String id = UUID.randomUUID().toString();
    bridges.put(id, new Bridge(snapshot.request(), new EntrySemantics(request.worldview()),
        selected, Map.copyOf(entries), MediationPolicy.disabled()));
    return id;
  }

  @Override public synchronized void close() {
    closed = true;
    bridges.clear(); worldviews.clear(); catalogs.clear();
  }
  private void requireOpen() { if (closed) throw new IllegalStateException("LAND provider is closed"); }

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
    var bridge = bridge(configurationId);
    if (CatalogEntryId.isEntryCode(left) || CatalogEntryId.isEntryCode(right)) {
      boolean a = CatalogEntryId.isEntryCode(left), b = CatalogEntryId.isEntryCode(right);
      if (a) requireEntry(bridge, left);
      if (b) requireEntry(bridge, right);
      if (a && b && left.equals(right))
        return new PatternComparison(PatternComparison.Status.EQUIVALENT,List.of("Same pinned source entry"));
      if (a && b) {
        var target = requireEntry(bridge,left); var candidate = requireEntry(bridge,right);
        if (target.catalog == candidate.catalog && target.catalog.strictlyIncludes(target.entry,candidate.entry))
          return new PatternComparison(PatternComparison.Status.RIGHT_REFINES_LEFT,
              List.of("Explicit transitive source-class inclusion in the same pinned catalog"));
      }
      return new PatternComparison(PatternComparison.Status.UNKNOWN,
          List.of("No explicit directional source-class inclusion or reviewed cross-entry mapping"));
    }
    var semantics = bridge.semantics;
    if (left.startsWith(PatternRuleYaml.PREFIX) && right.startsWith(PatternRuleYaml.PREFIX))
      return RuleComparison.compare(validateRule(configurationId,PatternRuleYaml.decode(left)).rule(),
          validateRule(configurationId,PatternRuleYaml.decode(right)).rule(),semantics);
    if (left.startsWith(PatternRuleYaml.PREFIX) || right.startsWith(PatternRuleYaml.PREFIX))
      return new PatternComparison(PatternComparison.Status.UNKNOWN,List.of("Composition-only and typed rule identities require an explicit bridge"));
    return comparePatterns(configurationId,left,right);
  }
  @Override public Subsumption subsumption(String configurationId, String targetId, String candidateId) {
    bridge(configurationId);
    try {
      var proof = compareIdentities(configurationId, targetId, candidateId);
      return proof.status() == PatternComparison.Status.EQUIVALENT
          || proof.status() == PatternComparison.Status.RIGHT_REFINES_LEFT
          ? Subsumption.SUBSUMES : Subsumption.UNKNOWN;
    } catch (IllegalArgumentException failure) {
      return Subsumption.UNKNOWN;
    }
  }

  @Override public SemanticDistance semanticDistance(String configurationId, String targetId, String candidateId) {
    bridge(configurationId);
    boolean entryRoute = CatalogEntryId.isEntryCode(targetId) || CatalogEntryId.isEntryCode(candidateId);
    PatternComparison comparison;
    try { comparison = compareIdentities(configurationId,targetId,candidateId); }
    catch (IllegalArgumentException failure) {
      if (!entryRoute) throw failure;
      return SemanticDistance.unknown("Entry identity is malformed or not selected by this bridge");
    }
    if (comparison.status() == PatternComparison.Status.EQUIVALENT)
      return new SemanticDistance(DistanceStatus.COMPATIBLE,0,comparison.explanations());
    if (comparison.status() == PatternComparison.Status.RIGHT_REFINES_LEFT)
      return new SemanticDistance(DistanceStatus.COMPATIBLE,1,comparison.explanations());
    if (entryRoute) return SemanticDistance.unknown("No reviewed mapping between these source entries or definitions");
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
    if (CatalogEntryId.isEntryCode(left) || CatalogEntryId.isEntryCode(right))
      return new MediationResult(MediationResult.Status.UNKNOWN,null,MediationResult.OverlapDecision.UNASSESSED,
          false,false,false,false,exact,bridge.mediation,bridge.mediation.fingerprint(),bridge.semantics.worldview(),left,right,List.of(),
          List.of("Source-entry annotations do not supply executable fuzzy definitions or reviewed mappings"));
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

  /** Diagnostic definition audit only, never entry equivalence or mediation authorization.
   * UNKNOWN pairs are unresolved. Cost is quadratic in entry count. */
  public List<AuditFinding> auditCatalog(String configurationId, String catalogId) {
    var bridge = bridge(configurationId);
    var catalog = bridge.catalogs.stream().filter(c -> c.id().equals(catalogId)).findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Catalog is not selected by this bridge"));
    var findings = new ArrayList<AuditFinding>();
    for (int i = 0; i < catalog.entries().size(); i++) for (int j = i + 1; j < catalog.entries().size(); j++) {
      var a = catalog.entries().get(i); var b = catalog.entries().get(j);
      PatternComparison comparison;
      try {
        comparison = a.definitionCode() == null || b.definitionCode() == null
            ? new PatternComparison(PatternComparison.Status.UNKNOWN, List.of("Optional definition absent"))
            : compareIdentities(configurationId, a.definitionCode(), b.definitionCode());
      } catch (RuntimeException failure) {
        comparison = new PatternComparison(PatternComparison.Status.UNKNOWN, List.of("Optional definition cannot be validated"));
      }
      findings.add(new AuditFinding(a.alias(), b.alias(), comparison));
    }
    return List.copyOf(findings);
  }

  @Override public Identity resolveIdentity(String configurationId, String identityId) {
    var bridge = bridge(configurationId);
    try {
      if (CatalogEntryId.isEntryCode(identityId)) {
        var bound = requireEntry(bridge, identityId);
        return new LandIdentity(identityId, conceptName(identityId), bridge.request.name(), bridge.request.rootIdentity(),
            entryDescription(bound.catalog(), bound.entry()), bound.entry().label(),
            AuthorityIdentitySyntax.encode(bridge.request.name(), identityId), List.of(),
            bound.entry().classParents().stream().map(parent -> new CatalogEntryId(bound.catalog().id(),
                bound.catalog().version(),bound.catalog().metadata().sourceVersion(),parent).code()).toList());
      }
      String canonical = validateCode(identityId, bridge.semantics);
      var descriptions = new ArrayList<String>();
      if (canonical.startsWith(PatternRuleYaml.PREFIX)) {
        var validation = validateRule(PatternRuleYaml.decode(canonical),bridge.semantics);
        descriptions.add(validation.ready() ? "Typed rule semantic checks completed; observations and evaluator bindings remain separate."
            : "Provisional typed definition. Outstanding obligations:\n\n- " + String.join("\n- ",validation.obligations()));
      }
      return new LandIdentity(canonical, conceptName(canonical), bridge.request.name(), bridge.request.rootIdentity(),
          "Earth-cover pattern:\n\n```text\n" + canonical + "\n```\n\n" + String.join("\n\n", descriptions),
          canonical,
          AuthorityIdentitySyntax.encode(bridge.request.name(), canonical), List.of());
    } catch (RuntimeException e) { return failure(bridge, identityId, e.getMessage()); }
  }

  @Override public Identity reconcile(String configurationId, Map<String, String> fields) {
    var bridge = bridge(configurationId);
    if (fields == null || fields.isEmpty()
        || !Set.of("expression", "entry", "alias", "name", "code", "catalog", "revision", "sourceEdition").containsAll(fields.keySet())
        || fields.values().stream().anyMatch(value -> value == null || value.isBlank()))
      return failure(bridge, "", "Use a direct expression, entry envelope, or one exact alias/name/code selector");
    if (fields.containsKey("expression") || fields.containsKey("entry")) {
      if (fields.size() != 1) return failure(bridge, "", "Direct identity selectors must be used alone");
      boolean entry = fields.containsKey("entry"); String code = fields.get(entry ? "entry" : "expression");
      if (entry != CatalogEntryId.isEntryCode(code)) return failure(bridge, "", "Entry and direct-definition routes are separate");
      return resolveIdentity(configurationId, code);
    }
    if (List.of("alias", "name", "code").stream().filter(fields::containsKey).count() != 1)
      return failure(bridge, "", "Choose one exact alias, name or source code");
    var matches = new TreeSet<String>();
    for (var bound : bridge.entries.entrySet()) {
      var catalog = bound.getValue().catalog(); var entry = bound.getValue().entry();
      if (fields.containsKey("catalog") && !catalog.id().equals(fields.get("catalog"))
          || fields.containsKey("revision") && !catalog.version().equals(fields.get("revision"))
          || fields.containsKey("sourceEdition") && !catalog.metadata().sourceVersion().equals(fields.get("sourceEdition"))) continue;
      boolean alias = fields.containsKey("alias") && (entry.alias().equals(fields.get("alias"))
          || entry.aliases().contains(fields.get("alias")));
      boolean name = fields.containsKey("name") && (entry.label().equals(fields.get("name"))
          || entry.alias().equals(fields.get("name")) || entry.aliases().contains(fields.get("name")));
      if (fields.containsKey("alias") && alias || fields.containsKey("name") && name
          || fields.containsKey("code") && entry.sourceCode().equals(fields.get("code"))) matches.add(bound.getKey());
    }
    return matches.size() == 1 ? resolveIdentity(configurationId, matches.first())
        : failure(bridge, "", "Exact selector is absent or ambiguous; pin catalog/revision/source edition");
  }

  @Override public List<Identity> search(String query, String subAuthority, String configurationId) {
    var bridge = bridge(configurationId);
    if (subAuthority != null && !subAuthority.isBlank()) throw new IllegalArgumentException("LAND has no sub-authorities");
    if (query == null || query.isBlank()) throw new IllegalArgumentException("A search query is required");
    String q = query.toLowerCase(Locale.ROOT);
    var matches = new TreeSet<String>();
    for (var catalog : bridge.catalogs) for (var entry : catalog.entries())
      if ((catalog.id() + " " + Objects.toString(entry.sourceCode(), "") + " " + entry.alias()
          + " " + entry.label() + " " + String.join(" ", entry.aliases())).toLowerCase(Locale.ROOT).contains(q))
        matches.add(catalog.identityCode(entry));
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
          entry.alias(), catalog.identityCode(entry), entryDescription(catalog, entry), true)).toList());
      result.put(catalog.id(), list);
    }
    return Collections.unmodifiableMap(result);
  }

  @Override public Map<String, CodelistDefinition> getCodelistDefinitions(String configurationId) {
    var bridge = bridge(configurationId);
    var result = new LinkedHashMap<String, CodelistDefinition>();
    getCodelists(configurationId).forEach((id, list) -> result.put(id,
        new CodelistDefinition(list, bridge.catalogs.stream().filter(c -> c.id().equals(id)).findFirst().orElseThrow().metadata().acceptsProposals())));
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
  private static BoundEntry requireEntry(Bridge bridge, String code) {
    CatalogEntryId.decode(code);
    var entry = bridge.entries.get(code);
    if (entry == null) throw new IllegalArgumentException("Entry catalog, revision, source edition or exact code is not selected");
    return entry;
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
        + "Entry verification: " + entry.entryVerification() + "; definition review: " + entry.definitionReview() + ". "
        + "Only explicit class-inclusion ancestry authorizes source-entry refinement; no cross-scheme mapping is implied.\n\n"
        + String.join("\n\n", entry.notes());
  }
  private static String conceptName(String canonical) {
    try { return (CatalogEntryId.isEntryCode(canonical) ? "Entry_" : "Pattern_") + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8))); }
    catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
  }
}
