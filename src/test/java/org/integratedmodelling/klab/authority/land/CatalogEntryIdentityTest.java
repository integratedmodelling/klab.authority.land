package org.integratedmodelling.klab.authority.land;

import static org.junit.jupiter.api.Assertions.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.io.ByteArrayInputStream;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.integratedmodelling.klab.api.services.Authority;
import org.integratedmodelling.klab.api.services.reasoner.objects.AuthorityIdentitySyntax;
import org.junit.jupiter.api.Test;

class CatalogEntryIdentityTest {
  private static final String ROOT = "land:Root";
  private static final String H = "physical:HorizontalArrangement";
  private static final String PATTERN = H + "(land:Tree)";
  private static final URI SOURCE = URI.create("https://example.org/source/edition-1");
  private static class Semantics implements PatternSemantics {
    final List<String> validations = new ArrayList<>();
    public String worldview() { return "entry.testing"; }
    public String validate(String ref, Role role) {
      validations.add(ref);
      if (role == Role.CONFIGURATION ? !ref.equals(H) : !Set.of(ROOT, "land:Tree", "land:Grass").contains(ref))
        throw new IllegalArgumentException("Unknown or wrong-role concept");
      return ref;
    }
  }
  private static LandAuthority provider(Semantics semantics) {
    var a = new LandAuthority(); a.bindWorldview(semantics); return a;
  }
  private static PatternCatalog.Entry entry(String alias, String label, String code, String definition,
      PatternCatalog.EntryVerification verification, PatternCatalog.DefinitionReview review, List<String> aliases) {
    return new PatternCatalog.Entry(alias, label, definition, code, SOURCE, List.of(), null, verification, review, aliases);
  }
  private static PatternCatalog.Entry draft(String alias, String label, String code) {
    return entry(alias, label, code, PATTERN, PatternCatalog.EntryVerification.UNVERIFIED,
        PatternCatalog.DefinitionReview.DRAFT, List.of());
  }
  private static PatternCatalog catalog(String id, String revision, String edition, PatternCatalog.Entry... entries) {
    return new PatternCatalog(id, PatternCatalog.Domain.LULC, revision, SOURCE, List.of(entries),
        new PatternCatalog.Metadata(id, edition, true, List.of(), false));
  }
  private static String bridge(LandAuthority a, String... ids) {
    return a.configure(new Authority.ConfigurationRequest("entry.testing", "LAND", ROOT,
        Map.of("urn", LandAuthority.URN, "domain", "LULC", "catalogs", List.of(ids))));
  }
  private static void assertSuccess(Authority.Identity identity) {
    assertTrue(identity.getNotifications().isEmpty(), identity.getDescription());
  }

  @Test void entryEnvelopeRoundTripsThroughExistingAuthoritySyntaxWithoutChangingSourceStrings() {
    var key = new CatalogEntryId("catalog", "r:1", "source edition 2022", "006.01/\u00d6]");
    assertEquals(key, CatalogEntryId.decode(key.code()));
    var locator = AuthorityIdentitySyntax.encode("LAND", key.code());
    assertEquals(key.code(), AuthorityIdentitySyntax.decode(locator.substring(5)));
    assertNotEquals(key.code(), new CatalogEntryId("catalog", "r:1", "source edition 2022", "6.01/\u00d6]").code());
  }

  @Test void malformedUnknownAndNoncanonicalEnvelopesAreRejected() {
    var key = new CatalogEntryId("catalog", "r1", "edition", "006");
    for (String bad : List.of(key.code() + "=", key.code().replace("entry1:", "entry2:"), "entry1:!",
        "entry1:" + Base64.getUrlEncoder().withoutPadding().encodeToString("[\"catalog\",\"r1\",\"edition\",6]".getBytes(StandardCharsets.UTF_8)),
        "entry1:" + Base64.getUrlEncoder().withoutPadding().encodeToString("[ \"catalog\",\"r1\",\"edition\",\"006\"]".getBytes(StandardCharsets.UTF_8))))
      assertThrows(IllegalArgumentException.class, () -> CatalogEntryId.decode(bad), bad);
    assertThrows(IllegalArgumentException.class, () -> new CatalogEntryId("catalog", "r1", "edition", "6\n"));
    assertThrows(IllegalArgumentException.class, () -> new CatalogEntryId("catalog", "r1", "edition", "x".repeat(4097)));
  }

  @Test void closedAndOpenIgbpShrublandRemainDistinctAcrossEveryCatalogRoute() {
    var a = provider(new Semantics()); var id = bridge(a, "igbp_modis61_draft");
    var catalog = a.getCatalogs().get("igbp_modis61_draft");
    var values = a.getCodelists(id).get(catalog.id());
    String closed = (String) values.value("ClosedShrubland"); String open = (String) values.value("OpenShrubland");
    assertNotEquals(closed, open); assertEquals("6", CatalogEntryId.decode(closed).sourceCode());
    assertEquals("7", CatalogEntryId.decode(open).sourceCode());
    assertNotEquals(a.resolveIdentity(id, closed).getConceptName(), a.resolveIdentity(id, open).getConceptName());
    assertEquals(2, a.search("shrubland", null, id).size());
    assertEquals(closed, a.reconcile(id, Map.of("catalog", catalog.id(), "code", "6")).getId());
    assertEquals(open, a.reconcile(id, Map.of("alias", "OpenShrubland")).getId());
    assertEquals(Authority.DistanceStatus.UNKNOWN, a.semanticDistance(id, closed, open).status());
    assertEquals(Authority.DistanceStatus.UNKNOWN, a.semanticDistance(id, open, closed).status());
    assertEquals(0, a.semanticDistance(id, closed, closed).distance());
    assertEquals(17, a.search(catalog.id(), null, id).size());
  }

  @Test void exactCodesIncludingLeadingZerosNeverCoerceOrDeduplicateByDefinition() {
    var a = provider(new Semantics()); var c = catalog("codes", "r1", "edition", draft("ZeroSix", "Class", "006"), draft("Six", "Class", "6"));
    a.registerCatalog(c); var id = bridge(a, c.id());
    var x = a.reconcile(id, Map.of("code", "006")); var y = a.reconcile(id, Map.of("code", "6"));
    assertSuccess(x); assertSuccess(y); assertNotEquals(x.getId(), y.getId());
    assertEquals("006", CatalogEntryId.decode(x.getId()).sourceCode());
    assertEquals(2, a.search("Class", null, id).size());
    assertFalse(a.reconcile(id, Map.of("code", "06")).getNotifications().isEmpty());
    assertFalse(a.reconcile(id, Map.of("name", "Class")).getNotifications().isEmpty());
  }

  @Test void everyKeyCoordinateParticipatesInIdentityAndBridgeAdmission() {
    var base = new CatalogEntryId("catalog", "r1", "edition1", "006");
    var keys = List.of(base, new CatalogEntryId("other", "r1", "edition1", "006"),
        new CatalogEntryId("catalog", "r2", "edition1", "006"), new CatalogEntryId("catalog", "r1", "edition2", "006"));
    assertEquals(4, keys.stream().map(CatalogEntryId::code).distinct().count());
    var a = provider(new Semantics()); var c = catalog("catalog", "r1", "edition1", draft("Class", "Class", "006"));
    a.registerCatalog(c); var id = bridge(a, c.id());
    assertSuccess(a.resolveIdentity(id, base.code()));
    for (var key : keys.subList(1, keys.size())) {
      assertFalse(a.resolveIdentity(id, key.code()).getNotifications().isEmpty());
      assertEquals(Authority.DistanceStatus.UNKNOWN, a.semanticDistance(id, key.code(), key.code()).status());
    }
    String unknownVersion = base.code().replace("entry1:", "entry2:");
    assertEquals(Authority.DistanceStatus.UNKNOWN, a.semanticDistance(id, unknownVersion, unknownVersion).status());
    assertFalse(a.reconcile(id, Map.of("catalog", "catalog", "revision", "r2", "code", "006")).getNotifications().isEmpty());
    assertFalse(a.reconcile(id, Map.of("catalog", "catalog", "sourceEdition", "edition2", "code", "006")).getNotifications().isEmpty());
  }

  @Test void definitionsLabelsAliasesAndReviewStatusDoNotReplaceTheSourceKey() {
    var a = draft("First", "Original", "006");
    var b = entry("Renamed", "Updated", "006", H + "(land:Grass)", PatternCatalog.EntryVerification.VERIFIED,
        PatternCatalog.DefinitionReview.REVIEWED, List.of("Curated synonym"));
    var first = catalog("same", "r1", "edition", a); var second = catalog("same", "r1", "edition", b);
    assertEquals(first.identityCode(a), second.identityCode(b));
    assertNotEquals(a.definitionCode(), b.definitionCode());
  }

  @Test void samePatternAndSameAliasAcrossCatalogsRemainAmbiguous() {
    var a = provider(new Semantics()); var x = catalog("one", "r1", "edition", draft("Shared", "Shared label", "1"));
    var y = catalog("two", "r1", "edition", draft("Shared", "Shared label", "1"));
    a.registerCatalog(x); a.registerCatalog(y); var id = bridge(a, x.id(), y.id());
    assertEquals(2, a.search("Shared", null, id).size());
    assertFalse(a.reconcile(id, Map.of("alias", "Shared")).getNotifications().isEmpty());
    assertFalse(a.reconcile(id, Map.of("name", "Shared label")).getNotifications().isEmpty());
    assertFalse(a.reconcile(id, Map.of("code", "1")).getNotifications().isEmpty());
    assertEquals(x.identityCode(x.entries().getFirst()), a.reconcile(id, Map.of("catalog", "one", "alias", "Shared")).getId());
  }

  @Test void curatedExactAliasesSelectOneEntryWithoutFuzzyOrAmbiguousNameAdmission() {
    var a = provider(new Semantics());
    var e = entry("Primary", "Original label", "006", null, PatternCatalog.EntryVerification.VERIFIED,
        PatternCatalog.DefinitionReview.ABSENT, List.of("Curated exact name", "Known alias"));
    var c = catalog("aliases", "r1", "edition", e); a.registerCatalog(c); var id = bridge(a, c.id());
    for (var fields : List.of(Map.of("alias", "Primary"), Map.of("alias", "Known alias"),
        Map.of("name", "Original label"), Map.of("name", "Curated exact name")))
      assertEquals(c.identityCode(e), a.reconcile(id, fields).getId());
    assertFalse(a.reconcile(id, Map.of("name", "curated exact name")).getNotifications().isEmpty());
    assertFalse(a.reconcile(id, Map.of("alias", "Known")).getNotifications().isEmpty());
  }

  @Test void ambiguousCuratedNamesWithinOneCatalogDoNotChooseTheFirstEntry() {
    var a = provider(new Semantics());
    var x = entry("First", "Same", "1", null, PatternCatalog.EntryVerification.UNVERIFIED,
        PatternCatalog.DefinitionReview.ABSENT, List.of("Shared"));
    var y = entry("Second", "Same", "2", null, PatternCatalog.EntryVerification.UNVERIFIED,
        PatternCatalog.DefinitionReview.ABSENT, List.of("Shared"));
    var c = catalog("ambiguous", "r1", "edition", x, y); a.registerCatalog(c); var id = bridge(a, c.id());
    assertFalse(a.reconcile(id, Map.of("catalog", c.id(), "name", "Same")).getNotifications().isEmpty());
    assertFalse(a.reconcile(id, Map.of("catalog", c.id(), "alias", "Shared")).getNotifications().isEmpty());
    assertSuccess(a.reconcile(id, Map.of("catalog", c.id(), "code", "1")));
  }

  @Test void annotationOnlyEntriesNeedOnlyTheConfiguredIdentityRoot() {
    var semantics = new Semantics(); var a = provider(semantics);
    var e = entry("Annotation", "Annotation only", "001", null, PatternCatalog.EntryVerification.UNVERIFIED,
        PatternCatalog.DefinitionReview.ABSENT, List.of());
    var c = catalog("annotations", "r1", "edition", e); a.registerCatalog(c); var id = bridge(a, c.id());
    var identity = a.resolveIdentity(id, c.identityCode(e)); assertSuccess(identity);
    assertEquals(ROOT, identity.getBaseIdentity()); assertEquals("LAND", identity.getAuthorityName());
    assertTrue(identity.getParentIds().isEmpty()); assertTrue(identity.getDescription().contains("UNVERIFIED"));
    assertTrue(identity.getDescription().contains("ABSENT")); assertEquals(List.of(ROOT), semantics.validations);
    assertEquals(0, a.semanticDistance(id, identity.getId(), identity.getId()).distance());
  }

  @Test void incompleteDraftDoesNotBlockAnnotationsOrBecomeAResolutionDefinition() {
    var semantics = new Semantics(); var a = provider(semantics);
    var e = entry("Draft", "Draft", "001", "not a valid full pattern", PatternCatalog.EntryVerification.UNVERIFIED,
        PatternCatalog.DefinitionReview.DRAFT, List.of());
    var c = catalog("draft", "r1", "edition", e); a.registerCatalog(c); var id = bridge(a, c.id());
    assertSuccess(a.resolveIdentity(id, c.identityCode(e))); assertEquals(List.of(ROOT), semantics.validations);
    assertFalse(a.resolveIdentity(id, e.definitionCode()).getNotifications().isEmpty());
  }

  @Test void reviewedInvalidDefinitionsFailConfigurationWithoutReplacingAnExistingBridge() {
    var a = provider(new Semantics()); var old = bridge(a);
    var e = entry("Reviewed", "Reviewed", "1", H + "(land:Missing)", PatternCatalog.EntryVerification.VERIFIED,
        PatternCatalog.DefinitionReview.REVIEWED, List.of());
    var c = catalog("bad_review", "r1", "edition", e); a.registerCatalog(c);
    assertThrows(IllegalArgumentException.class, () -> bridge(a, c.id()));
    assertSuccess(a.resolveIdentity(old, PATTERN));
  }

  @Test void reviewedIdenticalDefinitionsStillDoNotAuthorizeCrossEntryOrEntryToPatternMediation() {
    var a = provider(new Semantics());
    var x = entry("One", "One", "1", PATTERN, PatternCatalog.EntryVerification.VERIFIED, PatternCatalog.DefinitionReview.REVIEWED, List.of());
    var y = entry("Two", "Two", "2", PATTERN, PatternCatalog.EntryVerification.VERIFIED, PatternCatalog.DefinitionReview.REVIEWED, List.of());
    var c = catalog("reviewed", "r1", "edition", x, y); a.registerCatalog(c); var id = bridge(a, c.id());
    String one = c.identityCode(x), two = c.identityCode(y);
    assertEquals(Authority.DistanceStatus.UNKNOWN, a.semanticDistance(id, one, two).status());
    assertEquals(Authority.DistanceStatus.UNKNOWN, a.semanticDistance(id, one, PATTERN).status());
    assertEquals(Authority.DistanceStatus.UNKNOWN, a.semanticDistance(id, PATTERN, one).status());
    assertEquals(MediationResult.Status.UNKNOWN, a.mediate(id, one, two).status());
    assertEquals(0, a.semanticDistance(id, one, one).distance());
  }

  @Test void existingFuzzyPolicyCannotAdmitDistinctCatalogEntries() throws Exception {
    var a = provider(new Semantics()); var x = draft("One", "One", "1"); var y = draft("Two", "Two", "2");
    var c = catalog("fuzzy_guard", "r1", "edition", x, y); a.registerCatalog(c);
    Map<String,Object> policy;
    try (var in = getClass().getResourceAsStream("/mediation/fuzzy-area-example.yaml")) {
      policy = new ObjectMapper(new YAMLFactory()).readValue(in, new com.fasterxml.jackson.core.type.TypeReference<Map<String,Object>>() {});
    }
    var id = a.configure(new Authority.ConfigurationRequest("entry.testing", "LAND", ROOT,
        Map.of("urn", LandAuthority.URN, "domain", "LULC", "catalogs", List.of(c.id()), "mediation", policy)));
    assertTrue(a.getMediationPolicy(id).enabled());
    assertEquals(Authority.DistanceStatus.UNKNOWN, a.semanticDistance(id, c.identityCode(x), c.identityCode(y)).status());
    assertFalse(a.mediate(id, c.identityCode(x), c.identityCode(y)).rightAdmittedForLeft());
  }

  @Test void directPatternRouteRetainsItsContractAndCannotBeConfusedWithAnEntrySelector() {
    var a = provider(new Semantics()); var c = catalog("route", "r1", "edition", draft("Entry", "Entry", "1"));
    a.registerCatalog(c); var id = bridge(a, c.id());
    assertSuccess(a.resolveIdentity(id, PATTERN)); assertEquals(0, a.semanticDistance(id, PATTERN, PATTERN).distance());
    assertNotEquals(PATTERN, c.identityCode(c.entries().getFirst()));
    assertSuccess(a.reconcile(id, Map.of("expression", PATTERN)));
    assertSuccess(a.reconcile(id, Map.of("entry", c.identityCode(c.entries().getFirst()))));
    assertFalse(a.reconcile(id, Map.of("expression", c.identityCode(c.entries().getFirst()))).getNotifications().isEmpty());
    assertFalse(a.reconcile(id, Map.of("entry", PATTERN)).getNotifications().isEmpty());
    a.releaseConfiguration(id);
    assertThrows(IllegalArgumentException.class, () -> a.resolveIdentity(id, c.identityCode(c.entries().getFirst())));
  }

  @Test void draftTypedRulesRemainOptionalForSourceEntryAnnotations() {
    var semantics = new Semantics(); var a = provider(semantics);
    var catalog = a.getCatalogs().get("ecosystem_rules_pilot");
    var id = a.configure(new Authority.ConfigurationRequest("entry.testing", "ECOSYSTEM", ROOT,
        Map.of("urn", LandAuthority.URN, "domain", "ECOSYSTEM", "catalogs", List.of(catalog.id()))));
    for (var entry : catalog.entries()) {
      String code = catalog.identityCode(entry); assertSuccess(a.resolveIdentity(id, code));
      assertEquals(0, a.semanticDistance(id, code, code).distance());
      assertFalse(a.resolveIdentity(id, entry.definitionCode()).getNotifications().isEmpty());
    }
    assertEquals(List.of(ROOT), semantics.validations);
  }

  @Test void registeredButUnselectedCatalogCannotAdmitAnEntryOrAnExactName() {
    var a = provider(new Semantics()); var x = catalog("selected", "r1", "edition", draft("One", "One", "1"));
    var y = catalog("unselected", "r1", "edition", draft("Two", "Two", "2"));
    a.registerCatalog(x); a.registerCatalog(y); var id = bridge(a, x.id()); String code = y.identityCode(y.entries().getFirst());
    assertFalse(a.resolveIdentity(id, code).getNotifications().isEmpty());
    assertFalse(a.reconcile(id, Map.of("catalog", y.id(), "alias", "Two")).getNotifications().isEmpty());
    assertEquals(Authority.DistanceStatus.UNKNOWN, a.semanticDistance(id, code, code).status());
    assertTrue(a.search("unselected", null, id).isEmpty());
  }

  @Test void reviewedTypedDefinitionRequiresACompleteHostValidatedInterpretation() {
    var support = new PatternRule.Support("patch", "window", "type");
    var criteria = new PatternRule.Qualification("tree", PatternRule.PredicateType.IDENTITY, "land:Tree", true, support);
    var provenance = new PatternRule.Provenance("urn:test:definition", "001", "Test-only interpretation");
    var pending = new PatternRule(2, PATTERN, List.of(new PatternRule.Participant("tree", List.of(0), null)),
        List.of(), criteria, provenance);
    var reviewed = new PatternCatalog.Entry("Reviewed", "Reviewed", PATTERN, "001", SOURCE, List.of(), pending,
        PatternCatalog.EntryVerification.VERIFIED, PatternCatalog.DefinitionReview.REVIEWED, List.of());
    var c = catalog("typed", "r1", "edition", reviewed);
    var plain = provider(new Semantics()); plain.registerCatalog(c);
    assertThrows(IllegalStateException.class, () -> bridge(plain, c.id()));
    var typed = new RuleSemantics() {
      final Semantics delegate = new Semantics();
      public String worldview() { return delegate.worldview(); }
      public String validate(String reference, Role role) { return delegate.validate(reference, role); }
      public String validateTyped(String reference, String type) { return reference; }
      public String validateObservable(String expression) { return expression; }
      public boolean validateAttribution(String bearer, List<String> predicates) { return true; }
    };
    var a = new LandAuthority(); a.bindWorldview(typed); a.registerCatalog(c);
    assertThrows(IllegalArgumentException.class, () -> bridge(a, c.id()));
    var complete = new PatternRule(2, PATTERN, List.of(new PatternRule.Participant("tree", List.of(0), "land:TreeBearer")),
        List.of(), criteria, provenance);
    var e = new PatternCatalog.Entry("Complete", "Complete", PATTERN, "002", SOURCE, List.of(), complete,
        PatternCatalog.EntryVerification.VERIFIED, PatternCatalog.DefinitionReview.REVIEWED, List.of());
    var ready = catalog("complete", "r1", "edition", e); a.registerCatalog(ready);
    assertSuccess(a.resolveIdentity(bridge(a, ready.id()), ready.identityCode(e)));
  }

  private static String schema3(String entry) {
    return "schemaVersion: 3\nid: example\ndomain: LULC\nversion: r1\nsource: https://example.org/edition\n"
        + "title: Example\nsourceVersion: edition\ntentative: true\nacceptsProposals: false\nnotes: []\nentries:\n" + entry;
  }
  @Test void schemaThreeSeparatesVerificationFromAbsentOrUnparsedDraftDefinitions() {
    var text = schema3("  - code: \"001\"\n    alias: Annotation\n    label: Annotation\n    entryVerification: VERIFIED\n"
        + "    definitionReview: ABSENT\n    notes: []\n    aliases: [Curated]\n");
    var c = PatternCatalogYaml.load(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));
    assertNull(c.entries().getFirst().definitionCode());
    assertEquals(PatternCatalog.EntryVerification.VERIFIED, c.entries().getFirst().entryVerification());
    var draft = text.replace("definitionReview: ABSENT", "definitionReview: DRAFT\n    expression: not a full pattern");
    assertEquals("not a full pattern", PatternCatalogYaml.load(new ByteArrayInputStream(draft.getBytes(StandardCharsets.UTF_8)))
        .entries().getFirst().definitionCode());
    for (String invalid : List.of(text.replace("ABSENT", "APPROVED"), text.replace("VERIFIED", "TRUSTED"),
        text.replace("code: \"001\"", "code: 001"), text.replace("ABSENT", "REVIEWED"),
        text + "    unexpected: true\n"))
      assertThrows(IllegalArgumentException.class, () -> PatternCatalogYaml.load(new ByteArrayInputStream(invalid.getBytes(StandardCharsets.UTF_8))));
  }
}
