package org.integratedmodelling.klab.authority.land;

import java.net.URI;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.integratedmodelling.klab.api.services.Authority;
import static org.junit.jupiter.api.Assertions.*;
import static org.integratedmodelling.klab.authority.land.PatternComparison.Status.*;

class LandAuthorityTest {
  private static final String H = "physical:HorizontalCover";
  private static final String V = "physical:VerticalCover";
  private static final String A = "taxonomy.species:FagusSylvatica";
  private static final String B = "taxonomy.species:Pinus";
  private static String pattern(String args) { return H + "(" + args + ")"; }

  private static class Semantics implements PatternSemantics {
    private final String worldview;
    Semantics(String worldview) { this.worldview = worldview; }
    @Override public String worldview() { return worldview; }
    @Override public String validate(String reference, Role role) {
      var configurations = Set.of(H, V);
      var identities = Set.of(A, B, "life:Tree", "land:Root", "land:OtherRoot", "TAXA:[some, code(+)]");
      if (!(role == Role.CONFIGURATION ? configurations : identities).contains(reference))
        throw new IllegalArgumentException("Unknown concept or wrong role: " + reference);
      return reference;
    }
    @Override public Relation relate(String a, String b) {
      if (a.equals(b)) return Relation.EQUIVALENT;
      if (a.equals(A) && b.equals("life:Tree")) return Relation.NARROWER;
      if (b.equals(A) && a.equals("life:Tree")) return Relation.BROADER;
      return Relation.UNKNOWN;
    }
  }
  private static LandAuthority authority() {
    var result = new LandAuthority();
    result.bindWorldview(new Semantics("test.worldview"));
    return result;
  }
  private static Authority.ConfigurationRequest request(String name, String root, Map<String, Object> extras) {
    var params = new HashMap<String, Object>(Map.of("urn", LandAuthority.URN, "domain", "LULC"));
    params.putAll(extras);
    return new Authority.ConfigurationRequest("test.worldview", name, root, params);
  }
  private static String configure(LandAuthority authority) {
    return authority.configure(request("LAND", "land:Root", Map.of()));
  }

  @Test void preservesNestedOrderAndAbundance() {
    String text = " + " + V + " ( " + H + "(+++" + A + ", +" + B + "), ++" + H + "(" + B + ")) ";
    var parsed = PatternParser.parse(text);
    assertEquals("+" + V + "(" + H + "(+++" + A + ",+" + B + "),++" + H + "(" + B + "))", parsed.canonical());
    assertEquals(parsed, PatternParser.parse(parsed.canonical()));
    assertEquals(0, PatternParser.parse(pattern(A)).arguments().getFirst().marks());
    assertThrows(UnsupportedOperationException.class, () -> parsed.arguments().clear());
  }

  @ParameterizedTest @ValueSource(strings = {
      "", "physical:HorizontalCover", "physical:HorizontalCover()", "physical:HorizontalCover(x)",
      "physical:HorizontalCover(life:Tree,)", "physical:HorizontalCover(life:Tree)junk",
      "physical:HorizontalCover(life:Tree", "physical:HorizontalCover(+)",
      "physical:HorizontalCover(life:Tree life:Tree)", "physical:HorizontalCover(TAXA:[bad\\q])",
      "physical:HorizontalCover(TAXA:[])", "physical:HorizontalCover(TAXA:[unclosed)"})
  void rejectsMalformedPatterns(String source) { assertThrows(IllegalArgumentException.class, () -> PatternParser.parse(source)); }

  @Test void boundsInputAndDepth() {
    assertThrows(IllegalArgumentException.class, () -> PatternParser.parse("x".repeat(PatternParser.MAX_LENGTH + 1)));
    assertThrows(IllegalArgumentException.class, () -> PatternParser.parse((H + "(").repeat(65) + A + ")".repeat(65)));
    assertThrows(IllegalArgumentException.class, () -> PatternParser.parse(pattern(String.join(",", Collections.nCopies(129, A)))));
  }

  @Test void authorityPayloadIsOpaque() {
    var authority = authority(); var id = configure(authority);
    assertTrue(authority.resolveIdentity(id, pattern("TAXA:[some, code(+)]")).getNotifications().isEmpty());
    assertEquals(pattern("TAXA:[a\\]b]"), PatternParser.parse(pattern("TAXA:[a\\]b]")).canonical());
  }

  @Test void validatesRolesButDoesNotInventBearers() {
    var authority = authority(); var id = configure(authority);
    assertTrue(authority.resolveIdentity(id, pattern(A)).getNotifications().isEmpty());
    assertFalse(authority.resolveIdentity(id, pattern(H)).getNotifications().isEmpty());
    assertFalse(authority.resolveIdentity(id, A + "(" + B + ")").getNotifications().isEmpty());
    assertFalse(authority.resolveIdentity(id, pattern("life:Missing")).getNotifications().isEmpty());
  }

  @Test void isolatesConfigurationsAndReleasesThem() {
    var authority = authority(); var a = configure(authority);
    var b = authority.configure(request("ECO", "land:OtherRoot", Map.of("domain", "ECOSYSTEM")));
    var first = authority.resolveIdentity(a, pattern(A)); var second = authority.resolveIdentity(b, pattern(A));
    assertEquals("LAND", first.getAuthorityName()); assertEquals("ECO", second.getAuthorityName());
    assertEquals("land:Root", first.getBaseIdentity()); assertEquals("land:OtherRoot", second.getBaseIdentity());
    assertEquals(first.getConceptName(), second.getConceptName());
    assertTrue(first.getParentIds().isEmpty()); assertTrue(first.getLocator().startsWith("LAND:["));
    authority.releaseConfiguration(a);
    assertThrows(IllegalArgumentException.class, () -> authority.resolveIdentity(a, pattern(A)));
    assertTrue(authority.resolveIdentity(b, pattern(A)).getNotifications().isEmpty());
    authority.unbindWorldview("test.worldview");
    assertThrows(IllegalArgumentException.class, () -> authority.resolveIdentity(b, pattern(A)));
  }

  @Test void rejectsUnboundAndInvalidConfigurations() {
    assertThrows(IllegalStateException.class, () -> configure(new LandAuthority()));
    var authority = authority();
    assertThrows(IllegalArgumentException.class, () -> authority.configure(request("LAND", "land:Root", Map.of("typo", 1))));
    assertThrows(IllegalArgumentException.class, () -> authority.configure(request("LAND", H, Map.of())));
    assertThrows(IllegalArgumentException.class, () -> authority.configure(request("LAND", "land:Root", Map.of("catalogs", List.of("missing")))));
    assertThrows(IllegalArgumentException.class, () -> authority.configure(request("LAND", "land:Root", Map.of("semanticBoundaries", Map.of("X", "land:Root")))));
  }

  @Test void distinguishesUnspecifiedAndEqualAbundance() {
    var authority = authority(); var id = configure(authority);
    assertEquals(LEFT_REFINES_RIGHT, authority.comparePatterns(id, pattern("+++" + A + ",+" + B), pattern(A + "," + B)).status());
    assertEquals(RIGHT_REFINES_LEFT, authority.comparePatterns(id, pattern(A + "," + B), pattern("+" + A + ",+" + B)).status());
    assertEquals(UNKNOWN, authority.comparePatterns(id, pattern("+++" + A + ",+" + B), pattern("+" + A + ",+" + B)).status());
    assertEquals(UNKNOWN, authority.comparePatterns(id, pattern("+++" + A + ",+" + B), pattern("++++++" + A + ",++" + B)).status());
  }

  @Test void keepsAbundanceLocalAndOrderSignificant() {
    var authority = authority(); var id = configure(authority);
    assertEquals(UNKNOWN, authority.comparePatterns(id, V + "(" + pattern(A) + "," + pattern(B) + ")", V + "(" + pattern(B) + "," + pattern(A) + ")").status());
    assertEquals(UNKNOWN, authority.comparePatterns(id, V + "(" + pattern("+++" + A + ",+" + B) + "," + pattern(A) + ")", V + "(" + pattern("++++++" + A + ",++" + B) + "," + pattern(A) + ")").status());
    assertEquals(UNKNOWN, authority.comparePatterns(id, "+" + pattern(A), "++" + pattern(A)).status());
  }

  @Test void semanticRefinementNeedsProofAndDoesNotTransportAbundanceToGenus() {
    var authority = authority(); var id = configure(authority);
    assertEquals(LEFT_REFINES_RIGHT, authority.comparePatterns(id, pattern(A), pattern("life:Tree")).status());
    assertEquals(UNKNOWN, authority.comparePatterns(id, pattern(A), pattern(B)).status());
    assertEquals(UNKNOWN, authority.comparePatterns(id, pattern("+++" + A + ",+" + B), pattern("+++life:Tree,+" + B)).status());
  }

  private static PatternCatalog catalog(String id, String expression) {
    return new PatternCatalog(id, PatternCatalog.Domain.LULC, "test-1", URI.create("https://example.org/legend"),
        List.of(new PatternCatalog.Entry("Woodland", "Illustrative woodland", expression)));
  }

  @Test void publishesResolvableCodelistsAndRejectsAmbiguousReconciliation() {
    var authority = authority();
    authority.registerCatalog(catalog("example_a", pattern(A)));
    authority.registerCatalog(catalog("example_b", pattern(B)));
    var id = authority.configure(request("LAND", "land:Root", Map.of("catalogs", List.of("example_a", "example_b"))));
    var lists = authority.getCodelists(id);
    assertEquals(pattern(A), lists.get("example_a").value("Woodland"));
    assertFalse(authority.getCodelistDefinitions(id).get("example_a").acceptsProposals());
    for (var list : lists.values()) for (var code : list.codes())
      assertTrue(authority.resolveIdentity(id, (String) list.value(code)).getNotifications().isEmpty());
    assertFalse(authority.reconcile(id, Map.of("alias", "Woodland")).getNotifications().isEmpty());
    assertEquals(pattern(A), authority.reconcile(id, Map.of("alias", "Woodland", "catalog", "example_a")).getId());
    for (var hit : authority.search("wood", null, id)) assertEquals(hit.getId(), authority.resolveIdentity(id, hit.getId()).getId());
    assertEquals(2, authority.search("wood", null, id).size());
    assertThrows(IllegalArgumentException.class, () -> authority.registerCatalog(catalog("example_a", pattern(B))));
    assertThrows(IllegalArgumentException.class, () -> authority.configure(request("ECO", "land:Root", Map.of("domain", "ECOSYSTEM", "catalogs", List.of("example_a")))));
  }

  @Test void auditFindsDuplicateDefinitionsWithoutInventingIncompatibility() {
    var authority = authority();
    authority.registerCatalog(new PatternCatalog("audit", PatternCatalog.Domain.LULC, "test", URI.create("https://example.org/legend"), List.of(
        new PatternCatalog.Entry("First", "First", pattern(A)),
        new PatternCatalog.Entry("Duplicate", "Duplicate", pattern(A)),
        new PatternCatalog.Entry("Other", "Other", pattern(B)))));
    var id = authority.configure(request("LAND", "land:Root", Map.of("catalogs", List.of("audit"))));
    var audit = authority.auditCatalog(id, "audit");
    assertEquals(3, audit.size());
    assertEquals(1, audit.stream().filter(f -> f.comparison().status() == EQUIVALENT).count());
    assertEquals(2, audit.stream().filter(f -> f.comparison().status() == UNKNOWN).count());
  }

  @Test void malformedCanonicalReferenceCannotInjectArguments() {
    var authority = new LandAuthority();
    authority.bindWorldview(new Semantics("test.worldview") {
      @Override public String validate(String reference, Role role) {
        return role == Role.IDENTITY && !reference.equals("land:Root") ? A + "," + B : reference;
      }
    });
    var id = configure(authority);
    assertFalse(authority.resolveIdentity(id, pattern(A)).getNotifications().isEmpty());
  }
}
