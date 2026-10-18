package org.integratedmodelling.klab.authority.land;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import org.integratedmodelling.klab.api.services.Authority;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EcosystemCatalogTest {
  private static final List<String> PILOTS = List.of("get21_ecosystem_pilot", "eunis2021_terrestrial_pilot",
      "eunis2022_marine_pilot", "eunis2012_inland_pilot");
  private static PatternSemantics semantics() throws Exception {
    var identities = new HashSet<String>(); var configurations = new HashSet<String>();
    configurations.addAll(Set.of("physical:HorizontalArrangement", "physical:VerticalArrangement"));
    for (var namespace : List.of("landcover.seed", "ecosystem.seed")) {
      String text;
      try (var input = EcosystemCatalogTest.class.getResourceAsStream("/vocabularies/" + namespace + ".kwv")) {
        assertNotNull(input); text = new String(input.readAllBytes(), StandardCharsets.UTF_8);
      }
      Pattern.compile("(?m)^identity ([A-Z][A-Za-z0-9_]+)").matcher(text).results()
          .forEach(m -> identities.add(namespace + ":" + m.group(1)));
      Pattern.compile("(?m)^configuration ([A-Z][A-Za-z0-9_]+)").matcher(text).results()
          .forEach(m -> configurations.add(namespace + ":" + m.group(1)));
    }
    return new PatternSemantics() {
      @Override public String worldview() { return "ecosystem.testing"; }
      @Override public String validate(String reference, Role role) {
        if (!(role == Role.CONFIGURATION ? configurations : identities).contains(reference))
          throw new IllegalArgumentException("Undeclared prototype or wrong role: " + reference);
        return reference;
      }
    };
  }
  private static String configure(LandAuthority authority) throws Exception {
    authority.bindWorldview(semantics());
    return authority.configure(new Authority.ConfigurationRequest("ecosystem.testing", "ECOSYSTEM", "ecosystem.seed:EcosystemClass",
        Map.of("urn", LandAuthority.URN, "domain", "ECOSYSTEM", "catalogs", PILOTS)));
  }
  private static JsonNode reference(String name) throws Exception {
    try (var input = EcosystemCatalogTest.class.getResourceAsStream("/reference/" + name)) {
      assertNotNull(input); return new ObjectMapper().readTree(input);
    }
  }
  private static PatternCatalog.Entry entry(LandAuthority a, String catalog, String code) {
    return a.getCatalogs().get(catalog).entries().stream().filter(e -> code.equals(e.sourceCode())).findFirst().orElseThrow();
  }

  @Test void keepsCompleteSourceInventoriesSeparateFromPartialRuntimeDefinitions() throws Exception {
    var files = Map.of("get21-level3.json", 110, "eunis2021-terrestrial-level3.json", 270,
        "eunis2022-marine-level3.json", 249, "eunis2012-inland-level3.json", 21);
    for (var file : files.entrySet()) {
      var data = reference(file.getKey()); var codes = new HashSet<String>();
      assertEquals(file.getValue(), data.path("entries").size());
      assertTrue(data.path("source").path("sha256").asText().matches("[a-f0-9]{64}"));
      for (var row : data.path("entries")) {
        assertEquals(3, row.path("level").asInt()); assertTrue(codes.add(row.path("code").asText()));
        assertFalse(row.path("label").asText().isBlank()); assertTrue(row.path("row").asInt() > 1);
      }
    }
    assertEquals(4, reference("eunis2022-marine-level3.json").path("selectedDeeperRecords").size());
    assertEquals(29, new LandAuthority().getCatalogs().get(PILOTS.getFirst()).entries().size());
  }

  @Test void pilotsPreserveExactSourceCodesLabelsAndDocumentDeeperMarineLevels() throws Exception {
    var resources = Map.of(PILOTS.get(0), "get21-level3.json", PILOTS.get(1), "eunis2021-terrestrial-level3.json",
        PILOTS.get(2), "eunis2022-marine-level3.json", PILOTS.get(3), "eunis2012-inland-level3.json");
    var authority = new LandAuthority();
    for (var mapping : resources.entrySet()) {
      var data = reference(mapping.getValue()); var sourceRows = new HashMap<String, JsonNode>();
      for (var row : data.path("entries")) sourceRows.put(row.path("code").asText(), row);
      for (var row : data.path("selectedDeeperRecords")) sourceRows.put(row.path("code").asText(), row);
      var catalog = authority.getCatalogs().get(mapping.getKey());
      assertEquals(PatternCatalog.Domain.ECOSYSTEM, catalog.domain()); assertTrue(catalog.metadata().tentative());
      for (var entry : catalog.entries()) {
        assertNotNull(sourceRows.get(entry.sourceCode()), entry.sourceCode());
        assertEquals(sourceRows.get(entry.sourceCode()).path("label").asText(), entry.label());
        assertFalse(entry.notes().isEmpty());
      }
    }
    assertEquals(4, reference("eunis2022-marine-level3.json").path("selectedDeeperRecords").get(0).path("level").asInt());
  }

  @Test void allSixtyNineEcosystemAliasesResolveWithTheAuthorizedPrototypeVocabulary() throws Exception {
    var authority = new LandAuthority(); var bridge = configure(authority); int count = 0;
    assertEquals(4, authority.getCodelists(bridge).size());
    for (var list : authority.getCodelists(bridge).values()) for (var alias : list.codes()) {
      var identity = authority.resolveIdentity(bridge, (String) list.value(alias));
      assertTrue(identity.getNotifications().isEmpty(), identity.getDescription());
      assertEquals("ecosystem.seed:EcosystemClass", identity.getBaseIdentity());
      assertTrue(identity.getDescription().contains("Prototype")); count++;
    }
    assertEquals(69, count);
    assertThrows(IllegalArgumentException.class, () -> authority.configure(new Authority.ConfigurationRequest(
        "ecosystem.testing", "LULC", "ecosystem.seed:EcosystemClass",
        Map.of("urn", LandAuthority.URN, "domain", "LULC", "catalogs", List.of(PILOTS.getFirst())))));
  }

  @Test void orthogonalContextPreventsFlatteningToCoverAlone() throws Exception {
    var authority = new LandAuthority(); var bridge = configure(authority);
    for (var pair : List.of(new String[]{PILOTS.get(1), "T17", "T18"}, new String[]{PILOTS.get(2), "MA12", "MA15"},
        new String[]{PILOTS.get(0), "F1.1", "F1.4"}, new String[]{PILOTS.get(0), "FM1.2", "FM1.3"})) {
      var a = entry(authority, pair[0], pair[1]); var b = entry(authority, pair[0], pair[2]);
      assertNotEquals(a.expression(), b.expression());
      assertEquals(PatternComparison.Status.UNKNOWN, authority.comparePatterns(bridge, a.expression(), b.expression()).status());
    }
    assertTrue(entry(authority, PILOTS.get(0), "F1.4").notes().getFirst().contains("perennial"));
  }

  @Test void temporalConfigurationsPreserveSequenceAndRelativeDurationMarksWithoutInventingAClock() throws Exception {
    var authority = new LandAuthority(); var bridge = configure(authority);
    String wet = "physical:HorizontalArrangement(ecosystem.seed:InundatedPhase)";
    String dry = "physical:HorizontalArrangement(ecosystem.seed:ExposedPhase)";
    String cycle = "ecosystem.seed:HydroperiodCycle(+++" + wet + ",+" + dry + ")";
    var parsed = authority.validatePattern(bridge, cycle);
    assertEquals(3, parsed.arguments().getFirst().marks()); assertEquals(1, parsed.arguments().get(1).marks());
    assertEquals(cycle, parsed.canonical());
    assertEquals(PatternComparison.Status.UNKNOWN, authority.comparePatterns(bridge, cycle,
        "ecosystem.seed:HydroperiodCycle(+" + dry + ",+++" + wet + ")").status());
    var seasonal = PatternParser.parse(entry(authority, PILOTS.get(0), "TF1.4").expression());
    assertTrue(seasonal.canonical().contains("ecosystem.seed:HydroperiodCycle"));
    assertEquals(0, seasonal.arguments().getLast().arguments().getFirst().marks());
    // No phase duration or 3:1 cycle is asserted for the source class itself.
  }

  @Test void keepsMarineTerrestrialAndLegacyInlandProvenanceDistinct() throws Exception {
    var authority = new LandAuthority(); var bridge = configure(authority);
    assertTrue(authority.getCodelists(bridge).get(PILOTS.get(1)).getDescription().contains("2021_1"));
    assertTrue(authority.getCodelists(bridge).get(PILOTS.get(2)).getDescription().contains("2022"));
    assertTrue(authority.getCodelists(bridge).get(PILOTS.get(3)).getDescription().contains("2012"));
    assertTrue(authority.resolveIdentity(bridge, entry(authority,PILOTS.get(2),"MB252").expression())
        .getDescription().contains("level 4"));
    assertEquals(1, authority.search("C2.5", null, bridge).size());
  }

  @Test void comparisonReviewCasesReferencePilotsWithoutAssertingOfficialRelationships() throws Exception {
    var data = new ObjectMapper().readTree(java.nio.file.Path.of("docs", "data", "ecosystem-comparison-cases.json").toFile());
    assertEquals(16, data.path("cases").size());
    var authority = new LandAuthority();
    for (var row : data.path("cases")) {
      assertFalse(row.path("officialCrosswalk").asBoolean(true));
      assertFalse(row.path("runtimeAssertion").asBoolean(true));
      assertNotNull(entry(authority, PILOTS.getFirst(), row.path("getCode").asText()));
      for (var code : row.path("eunisCodes"))
        assertNotNull(entry(authority, row.path("eunisCatalog").asText(), code.asText()));
      assertFalse(row.path("requiredEvidence").asText().isBlank());
    }
  }
}
