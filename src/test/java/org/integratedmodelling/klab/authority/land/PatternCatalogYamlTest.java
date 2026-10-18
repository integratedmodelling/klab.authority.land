package org.integratedmodelling.klab.authority.land;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.integratedmodelling.klab.api.services.Authority;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class PatternCatalogYamlTest {
  private static final String CORINE = "corine_2018_draft";
  private static final String IGBP = "igbp_modis61_draft";
  private static final String H = "physical:HorizontalArrangement";

  /** Role fixture checks every leaf against the proposed vocabulary; it is not an OWL reasoner. */
  private static PatternSemantics fixtureSemantics() throws Exception {
    String vocabulary;
    try (var input = PatternCatalogYamlTest.class.getResourceAsStream("/vocabularies/landcover.seed.kwv")) {
      assertNotNull(input);
      vocabulary = new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
    var identities = Pattern.compile("(?m)^identity ([A-Z][A-Za-z0-9_]+)").matcher(vocabulary).results()
        .map(m -> "landcover.seed:" + m.group(1)).collect(Collectors.toSet());
    return new PatternSemantics() {
      @Override public String worldview() { return "seed.testing"; }
      @Override public String validate(String reference, Role role) {
        boolean valid = role == Role.IDENTITY ? identities.contains(reference)
            : Set.of(H, "physical:VerticalArrangement").contains(reference);
        if (!valid) throw new IllegalArgumentException("Undeclared test concept or wrong role: " + reference);
        return reference;
      }
    };
  }

  private static String configure(LandAuthority authority, List<String> catalogs) throws Exception {
    authority.bindWorldview(fixtureSemantics());
    return authority.configure(new Authority.ConfigurationRequest("seed.testing", "LAND", "landcover.seed:CoverClass",
        Map.of("urn", LandAuthority.URN, "domain", "LULC", "catalogs", catalogs)));
  }
  private static PatternCatalog.Entry entry(PatternCatalog catalog, String code) {
    return catalog.entries().stream().filter(e -> code.equals(e.sourceCode())).findFirst().orElseThrow();
  }
  private static String yaml() throws Exception {
    try (var input = PatternCatalogYamlTest.class.getResourceAsStream("/catalogs/igbp_modis61_draft.yaml")) {
      assertNotNull(input);
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
  private static PatternCatalog load(String yaml) {
    return PatternCatalogYaml.load(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
  }

  @Test void bundlesTheTwoCompletePinnedCodeSetsWithoutNoData() {
    var catalogs = new LandAuthority().getCatalogs();
    assertEquals(Set.of(CORINE, IGBP, "get21_ecosystem_pilot", "eunis2021_terrestrial_pilot",
        "eunis2022_marine_pilot", "eunis2012_inland_pilot", "ecosystem_rules_pilot"), catalogs.keySet());
    assertEquals(44, catalogs.get(CORINE).entries().size());
    assertEquals(17, catalogs.get(IGBP).entries().size());
    assertEquals(Set.of("111", "112", "121", "122", "123", "124", "131", "132", "133", "141", "142",
        "211", "212", "213", "221", "222", "223", "231", "241", "242", "243", "244", "311", "312",
        "313", "321", "322", "323", "324", "331", "332", "333", "334", "335", "411", "412", "421",
        "422", "423", "511", "512", "521", "522", "523"),
        catalogs.get(CORINE).entries().stream().map(PatternCatalog.Entry::sourceCode).collect(Collectors.toSet()));
    assertEquals(IntStream.rangeClosed(1, 17).mapToObj(Integer::toString).collect(Collectors.toSet()),
        catalogs.get(IGBP).entries().stream().map(PatternCatalog.Entry::sourceCode).collect(Collectors.toSet()));
    for (var catalog : catalogs.values()) {
      assertTrue(catalog.metadata().tentative());
      assertFalse(catalog.metadata().acceptsProposals());
      assertFalse(catalog.metadata().sourceVersion().isBlank());
      for (var entry : catalog.entries()) {
        assertNotNull(entry.source()); assertFalse(entry.notes().isEmpty());
        assertEquals(entry.expression(), PatternParser.parse(entry.expression()).canonical());
      }
    }
    assertThrows(UnsupportedOperationException.class, catalogs::clear);
  }

  @Test void allSixtyOneAliasesResolveUsingTheDeclaredFixtureVocabulary() throws Exception {
    var authority = new LandAuthority(); var bridge = configure(authority, List.of(CORINE, IGBP));
    int checked = 0;
    for (var list : authority.getCodelists(bridge).values()) {
      assertTrue(list.getDescription().contains("TENTATIVE TEST DATA"));
      for (var code : list.codes()) {
        var identity = authority.resolveIdentity(bridge, (String) list.value(code));
        assertTrue(identity.getNotifications().isEmpty(), identity.getDescription());
        assertEquals(list.value(code), identity.getId());
        assertTrue(identity.getDescription().contains("Tentative test translation"));
        assertTrue(list.getDescription(code).contains("source code"));
        checked++;
      }
    }
    assertEquals(61, checked);
    for (var definition : authority.getCodelistDefinitions(bridge).values()) assertFalse(definition.acceptsProposals());
    assertThrows(IllegalArgumentException.class, () -> authority.validatePattern(bridge,
        H + "(landcover.seed:InventedIdentity)"));
  }

  @Test void searchesCodesAndReconcilesNamesWithoutConfusingSourceCodesWithPatternIds() throws Exception {
    var authority = new LandAuthority(); var bridge = configure(authority, List.of(CORINE, IGBP));
    var hits = authority.search("311", null, bridge);
    assertEquals(1, hits.size());
    assertEquals("Broad-leaved forest", hits.getFirst().getLabel());
    assertTrue(hits.getFirst().getDescription().contains("above 75 percent"));
    assertTrue(hits.getFirst().getDescription().contains("index-clc-311.html"));
    assertFalse(authority.resolveIdentity(bridge, "311").getNotifications().isEmpty());
    assertEquals(hits.getFirst().getId(), authority.reconcile(bridge,
        Map.of("catalog", CORINE, "alias", "BroadLeavedForest")).getId());
    assertFalse(authority.reconcile(bridge, Map.of("alias", "MixedForest")).getNotifications().isEmpty());
    assertTrue(authority.reconcile(bridge, Map.of("catalog", IGBP, "alias", "MixedForest")).getNotifications().isEmpty());
  }

  @Test void recordsKnownLossesAndDoesNotPretendTheDraftIsFaithful() throws Exception {
    var authority = new LandAuthority(); var bridge = configure(authority, List.of(CORINE, IGBP));
    var igbp = authority.getCatalogs().get(IGBP); var corine = authority.getCatalogs().get(CORINE);
    assertEquals(entry(igbp, "6").expression(), entry(igbp, "7").expression());
    assertEquals(entry(igbp, "8").expression(), entry(igbp, "9").expression());
    assertNotEquals(entry(corine, "313").expression(), entry(igbp, "5").expression());
    var closed = authority.resolveIdentity(bridge, igbp.identityCode(entry(igbp, "6")));
    var open = authority.resolveIdentity(bridge, igbp.identityCode(entry(igbp, "7")));
    assertEquals("Closed shrublands", closed.getLabel());
    assertEquals("Open shrublands", open.getLabel());
    assertNotEquals(closed.getId(), open.getId());
    assertEquals(Authority.DistanceStatus.UNKNOWN, authority.semanticDistance(bridge, closed.getId(), open.getId()).status());
    var findings = authority.auditCatalog(bridge, IGBP);
    assertEquals(136, findings.size());
    assertTrue(findings.stream().anyMatch(f -> f.left().equals("ClosedShrubland") && f.right().equals("OpenShrubland")
        && f.comparison().status() == PatternComparison.Status.EQUIVALENT));
    assertEquals(3, PatternParser.parse(entry(corine, "243").expression()).arguments().getFirst().marks());
    assertEquals("physical:VerticalArrangement", PatternParser.parse(entry(corine, "244").expression()).concept());
  }

  @Test void seedsAreOptInAndStillRequireWorldviewValidation() throws Exception {
    var empty = new LandAuthority(); var id = configure(empty, List.of());
    assertTrue(empty.getCodelists(id).isEmpty());
    assertThrows(IllegalStateException.class, () -> new LandAuthority().configure(new Authority.ConfigurationRequest(
        "seed.testing", "LAND", "landcover.seed:CoverClass", Map.of("urn", LandAuthority.URN, "domain", "LULC", "catalogs", List.of(CORINE)))));
    assertThrows(IllegalArgumentException.class, () -> empty.configure(new Authority.ConfigurationRequest(
        "seed.testing", "ECO", "landcover.seed:CoverClass", Map.of("urn", LandAuthority.URN, "domain", "ECOSYSTEM", "catalogs", List.of(CORINE)))));
    var bare = new LandAuthority();
    bare.bindWorldview(new PatternSemantics() {
      @Override public String worldview() { return "seed.testing"; }
      @Override public String validate(String reference, Role role) {
        if (!reference.equals("landcover.seed:CoverClass")) throw new IllegalArgumentException("Missing seed vocabulary");
        return reference;
      }
    });
    assertDoesNotThrow(() -> bare.configure(new Authority.ConfigurationRequest(
        "seed.testing", "LAND", "landcover.seed:CoverClass", Map.of("urn", LandAuthority.URN, "domain", "LULC", "catalogs", List.of(IGBP)))));
  }

  @ParameterizedTest @ValueSource(strings = {"schema", "unknown", "duplicateKey", "numericCode", "duplicateCode",
      "duplicateAlias", "invalidExpression", "notesScalar", "badBoolean", "multiDocument", "overflowVersion"})
  void rejectsMalformedCatalogs(String mutation) throws Exception {
    String yaml = yaml();
    String changed = switch (mutation) {
      case "schema" -> yaml.replace("schemaVersion: 1", "schemaVersion: 3");
      case "unknown" -> yaml + "\nnotAField: true\n";
      case "duplicateKey" -> yaml + "\nid: repeated\n";
      case "numericCode" -> yaml.replace("code: \"1\"", "code: 1");
      case "duplicateCode" -> yaml.replace("code: \"2\"", "code: \"1\"");
      case "duplicateAlias" -> yaml.replace("alias: EvergreenBroadleafForest", "alias: EvergreenNeedleleafForest");
      case "invalidExpression" -> yaml.replace("physical:HorizontalArrangement(landcover.seed:Shrub)", "physical:HorizontalArrangement()");
      case "notesScalar" -> yaml.replaceFirst("notes:", "notes: not-a-list\noldNotes:");
      case "badBoolean" -> yaml.replace("tentative: true", "tentative: \"true\"");
      case "multiDocument" -> yaml + "\n---\nid: second\n";
      case "overflowVersion" -> yaml.replace("schemaVersion: 1", "schemaVersion: 4294967297");
      default -> throw new AssertionError(mutation);
    };
    assertNotEquals(yaml, changed);
    assertThrows(IllegalArgumentException.class, () -> load(changed));
  }

  @Test void boundsYamlAndReportsMissingResources() {
    assertThrows(IllegalArgumentException.class, () -> PatternCatalogYaml.load(new ByteArrayInputStream(new byte[1024 * 1024 + 1])));
    assertThrows(IllegalArgumentException.class, () -> PatternCatalogYaml.loadClasspath("/catalogs/missing.yaml"));
  }

  @Test void preservesQuotedCodesAndInheritedSourceUris() throws Exception {
    var catalog = load(yaml().replace("code: \"1\"", "code: \"001\""));
    assertEquals("001", catalog.entries().getFirst().sourceCode());
    assertEquals(catalog.source(), catalog.entries().getFirst().source());
    assertThrows(UnsupportedOperationException.class, () -> catalog.entries().getFirst().notes().clear());
  }
}
