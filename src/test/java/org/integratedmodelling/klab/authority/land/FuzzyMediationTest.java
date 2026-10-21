package org.integratedmodelling.klab.authority.land;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.util.*;
import org.integratedmodelling.klab.api.services.Authority;
import org.junit.jupiter.api.Test;

class FuzzyMediationTest {
  private static final String H = "physical:HorizontalArrangement", V = "physical:VerticalArrangement";
  /** Numerical fixture, not a claim of seed-worldview consistency. */
  private static final RuleSemantics SEMANTICS = new RuleSemantics() {
    public String worldview() { return "fuzzy.testing"; }
    public String validate(String ref,Role role) {
      PatternRule.reference(ref);
      boolean configuration = Set.of(H,V,"ecosystem.seed:HydroperiodCycle","ecosystem.seed:Ecosystem","ecosystem.seed:Substrate").contains(ref);
      if (configuration != (role == Role.CONFIGURATION)) throw new IllegalArgumentException("Wrong role");
      return ref.equals("seed:AliasA") ? "seed:A" : ref;
    }
    public String validateTyped(String ref,String type) { PatternRule.reference(ref); return ref; }
    public String validateObservable(String expression) { return expression; }
    public Relation relate(String a,String b) {
      if (a.equals(b)) return Relation.EQUIVALENT;
      if (a.equals("seed:Species") && b.equals("seed:Genus")) return Relation.NARROWER;
      if (b.equals("seed:Species") && a.equals("seed:Genus")) return Relation.BROADER;
      return Relation.UNKNOWN;
    }
  };
  private record Fixture(LandAuthority authority,String bridge) {
    MediationResult compare(String a,String b) { return authority.mediate(bridge,a,b); }
    Authority.SemanticDistance distance(String target,String candidate) { return authority.semanticDistance(bridge,target,candidate); }
  }
  private static Map<String,Object> binding(String measure,String interpretation,PatternRule.Support support) {
    return Map.of("measure",measure,"interpretation",interpretation,"independentArguments",true,
        "support",Map.of("space",support.space(),"time",support.time(),"aggregation",support.aggregation()));
  }
  private static Map<String,Object> policy() {
    return resourcePolicy("fuzzy-area-example.yaml");
  }
  private static Map<String,Object> resourcePolicy(String file) {
    try (var in = FuzzyMediationTest.class.getResourceAsStream("/mediation/"+file)) {
      return new com.fasterxml.jackson.databind.ObjectMapper(new com.fasterxml.jackson.dataformat.yaml.YAMLFactory())
          .readValue(Objects.requireNonNull(in),new com.fasterxml.jackson.core.type.TypeReference<Map<String,Object>>() {});
    } catch (java.io.IOException e) { throw new java.io.UncheckedIOException(e); }
  }
  private static Fixture fixture(Map<String,Object> policy) {
    var a = new LandAuthority(); a.bindWorldview(SEMANTICS);
    return new Fixture(a,configure(a,"LAND",policy));
  }
  private static String configure(LandAuthority a,String name,Map<String,Object> policy) {
    var parameters = new HashMap<String,Object>(Map.of("urn",LandAuthority.URN,"domain","LULC"));
    if (policy != null) parameters.put("mediation",policy);
    return a.configure(new Authority.ConfigurationRequest(SEMANTICS.worldview(),name,"seed:Root",parameters));
  }
  private static String pattern(int... weights) {
    var args = new ArrayList<String>();
    for (int i = 0; i < weights.length; i++) args.add("+".repeat(weights[i])+"seed:"+(char)('A'+i));
    return H+"("+String.join(",",args)+")";
  }
  @Test void absenceOfPolicyPreservesExactBehaviorAndDoesNotInventAFuzzyScore() {
    var f = fixture(null); var result = f.compare(pattern(3,1),pattern(4,1));
    assertEquals(MediationResult.Status.DISABLED,result.status()); assertNull(result.overlap());
    assertEquals(Authority.DistanceStatus.UNKNOWN,f.distance(pattern(3,1),pattern(4,1)).status());
    assertEquals(0,f.distance(pattern(3,1),pattern(3,1)).distance());
  }
  @Test void positiveTriangularRatioPossibilityMatchesAnalyticTwoComponentResult() {
    var f = fixture(policy()); var result = f.compare(pattern(3,1),pattern(4,1));
    assertEquals(MediationResult.Status.DEFINED,result.status());
    double expected = 1-4*(2-Math.sqrt(3))/(2+Math.sqrt(3));
    assertTrue(result.overlap().lower().doubleValue() <= expected);
    assertTrue(result.overlap().upper().doubleValue() >= expected);
    assertTrue(result.overlap().upper().subtract(result.overlap().lower()).compareTo(new BigDecimal("0.00000006")) < 0);
    assertEquals(MediationResult.OverlapDecision.SUFFICIENT,result.overlapDecision());
    assertFalse(result.leftEntailsRight()); assertFalse(result.rightEntailsLeft());
    assertEquals(Authority.DistanceStatus.UNKNOWN,f.distance(pattern(3,1),pattern(4,1)).status(),"Overlap alone must not admit a candidate");
  }
  @Test void relativeSpreadIsScaleInvariantButAbsoluteSpreadModelsMorePreciseLargerCounts() {
    var relative = fixture(policy()); var same = relative.compare(pattern(3,1),pattern(6,2));
    assertEquals(BigDecimal.ONE,same.overlap().lower());
    assertTrue(same.leftEntailsRight()); assertTrue(same.rightEntailsLeft());
    assertEquals(0,relative.distance(pattern(3,1),pattern(6,2)).distance());
    var parameters = policy(); parameters.put("spreadMode","ABSOLUTE");
    var absolute = fixture(parameters); var precise = absolute.compare(pattern(3,1),pattern(6,2));
    assertFalse(precise.leftEntailsRight()); assertTrue(precise.rightEntailsLeft());
    assertEquals(1,absolute.distance(pattern(3,1),pattern(6,2)).distance());
    assertEquals(Authority.DistanceStatus.UNKNOWN,absolute.distance(pattern(6,2),pattern(3,1)).status());
    assertTrue(absolute.compare(pattern(3,1),pattern(12,4)).rightEntailsLeft());
  }
  @Test void optInAlphaContainmentAdmitsDirectionalApproximationWithAConservativeDistance() {
    var parameters = policy(); parameters.put("spreadMode","ABSOLUTE"); parameters.put("spread","0.75");
    parameters.put("admission","ALPHA_CUT"); parameters.put("admissionAlpha","0.5");
    var f = fixture(parameters); var result = f.compare(pattern(3,1),pattern(7,2));
    assertFalse(result.rightEntailsLeft()); assertTrue(result.rightAdmittedForLeft()); assertFalse(result.leftAdmittedForRight());
    assertEquals(11,f.distance(pattern(3,1),pattern(7,2)).distance());
    assertEquals(Authority.Subsumption.UNKNOWN, f.authority().subsumption(f.bridge(),pattern(3,1),pattern(7,2)),
        "Fuzzy admission must not become strict inheritance");
    assertEquals(Authority.DistanceStatus.UNKNOWN,f.distance(pattern(7,2),pattern(3,1)).status());
    parameters.put("overlapThreshold","0.95"); var strict = fixture(parameters);
    assertEquals(MediationResult.OverlapDecision.LOW,strict.compare(pattern(3,1),pattern(7,2)).overlapDecision());
    assertEquals(Authority.DistanceStatus.UNKNOWN,strict.distance(pattern(3,1),pattern(7,2)).status());
  }
  @Test void thresholdDecisionsAreExactEvenInsideReportedBisectionUncertainty() {
    var parameters = policy(); parameters.put("spread","0.8"); parameters.put("overlapThreshold","0.375"); parameters.put("steps",4);
    var at = fixture(parameters).compare(pattern(1,1),pattern(9,1));
    assertEquals(MediationResult.OverlapDecision.SUFFICIENT,at.overlapDecision());
    parameters.put("overlapThreshold","0.3750000000000000000000000001");
    var above = fixture(parameters).compare(pattern(1,1),pattern(9,1));
    assertEquals(at.overlap(),above.overlap());
    assertEquals(MediationResult.OverlapDecision.LOW,above.overlapDecision());
  }
  @Test void jointVectorAndNestedScopesUseAllComponentsAndMinimumAggregation() {
    var f = fixture(policy());
    var incompatible = f.compare(pattern(1,1,1),pattern(1,2,4));
    assertEquals(BigDecimal.ZERO,incompatible.overlap().upper());
    assertEquals(MediationResult.OverlapDecision.LOW,incompatible.overlapDecision());
    var nested = f.compare(V+"("+pattern(3,1)+","+pattern(1,1,1)+")",V+"("+pattern(4,1)+","+pattern(1,2,4)+")");
    assertEquals(2,nested.scopes().size()); assertEquals(BigDecimal.ZERO,nested.overlap().upper());
    assertEquals(List.of(0),nested.scopes().getFirst().path());
  }
  @Test void unspecifiedAndSingleMarksDoNotBecomeEqualShares() {
    var f = fixture(policy());
    var unmarked = f.compare(pattern(0,0),pattern(3,1));
    assertTrue(unmarked.rightEntailsLeft()); assertFalse(unmarked.leftEntailsRight());
    assertEquals(BigDecimal.ONE,unmarked.overlap().lower());
    var single = f.compare(pattern(3,0),pattern(0,0));
    assertTrue(single.leftEntailsRight()); assertTrue(single.rightEntailsLeft()); assertTrue(single.scopes().isEmpty());
    assertEquals(List.of(0,1),f.compare(pattern(3,1,0),pattern(4,1,0)).scopes().getFirst().markedPositions());
    assertEquals(MediationResult.Status.UNKNOWN,f.compare(pattern(3,1,0),pattern(3,0,1)).status());
  }
  @Test void fuzzyWorkRequiresContextAndCannotHideUnknownOrderOrAggregation() {
    var parameters = policy(); parameters.put("configurations",Map.of());
    assertEquals(MediationResult.Status.UNKNOWN,fixture(parameters).compare(pattern(3,1),pattern(4,1)).status());
    var f = fixture(policy());
    for (String other : List.of(H+"(++++seed:B,+seed:A)",H+"(+++seed:Species,+seed:Genus)",H+"(+++seed:A,+seed:A)")) {
      String left = other.contains("Species") ? other.replace("+++","++++") : other.contains("+seed:A,+seed:A") ? other.replace("+++","++++") : pattern(3,1);
      assertEquals(MediationResult.Status.UNKNOWN,f.compare(left,other).status());
    }
    assertEquals(MediationResult.Status.UNKNOWN,f.compare("+"+pattern(3,1),"+"+pattern(4,1)).status());
    String outerA = H+"(++"+pattern(3,1)+",+seed:C)", outerB = H+"(++"+pattern(4,1)+",+seed:C)";
    assertEquals(MediationResult.Status.UNKNOWN,f.compare(outerA,outerB).status(),"Changing a quantified component's own definition needs aggregation semantics");
  }
  @Test void crispWeightsAreSupportedAndLargeSpreadStillHasPositiveDenominators() {
    var parameters = policy(); parameters.put("spread","0"); var crisp = fixture(parameters);
    assertEquals(BigDecimal.ZERO,crisp.compare(pattern(3,1),pattern(4,1)).overlap().upper());
    assertEquals(BigDecimal.ONE,crisp.compare(pattern(3,1),pattern(6,2)).overlap().lower());
    parameters.put("spread","0.999999999999999999");
    assertEquals(MediationResult.Status.DEFINED,fixture(parameters).compare(pattern(1,3),pattern(8,1)).status());
  }
  @Test void bridgeSnapshotsAreImmutableIsolatedAndDoNotChangeCanonicalIdentity() {
    var parameters = policy(); var f = fixture(parameters);
    String original = f.authority.resolveIdentity(f.bridge,pattern(3,1)).getId(); String fingerprint = f.authority.getMediationPolicy(f.bridge).fingerprint();
    parameters.put("spread","0.5"); parameters.put("configurations",Map.of());
    String other = configure(f.authority,"OTHER",parameters);
    assertEquals(new BigDecimal("0.25"),f.authority.getMediationPolicy(f.bridge).spread());
    assertNotEquals(fingerprint,f.authority.getMediationPolicy(other).fingerprint());
    assertEquals(original,f.authority.resolveIdentity(other,pattern(3,1)).getId());
    assertThrows(UnsupportedOperationException.class,()->f.authority.getMediationPolicy(f.bridge).configurations().clear());
    f.authority.releaseConfiguration(f.bridge);
    assertThrows(IllegalArgumentException.class,()->f.compare(pattern(3,1),pattern(4,1)));
  }
  @Test void symmetryScaleInvarianceAndSpreadMonotonicityHoldForVariedVectors() {
    var parameters = policy(); var narrow = fixture(parameters); parameters.put("spread","0.5"); var broad = fixture(parameters);
    var random = new Random(20261018);
    for (int i = 0; i < 24; i++) {
      int[] a = {1+random.nextInt(8),1+random.nextInt(8),1+random.nextInt(8)};
      int[] b = {1+random.nextInt(8),1+random.nextInt(8),1+random.nextInt(8)};
      var ab = narrow.compare(pattern(a),pattern(b)); var ba = narrow.compare(pattern(b),pattern(a));
      assertEquals(ab.overlap(),ba.overlap()); assertEquals(ab.leftEntailsRight(),ba.rightEntailsLeft());
      int[] scaled = Arrays.stream(a).map(n -> 2*n).toArray();
      assertEquals(BigDecimal.ONE,narrow.compare(pattern(a),pattern(scaled)).overlap().lower());
      var relaxed = broad.compare(pattern(a),pattern(b));
      assertTrue(relaxed.overlap().upper().compareTo(ab.overlap().lower()) >= 0);
    }
  }
  @Test void corinePrototypeAndGetDurationExamplesHaveIndependentMeasures() {
    String corine = PatternCatalogYaml.loadClasspath("/catalogs/corine_2018_draft.yaml").entries().stream()
        .filter(e -> "243".equals(e.sourceCode())).findFirst().orElseThrow().expression();
    var f = fixture(policy()); var cover = f.compare(corine,corine.replace("+++","++++"));
    assertEquals(PatternRule.Measure.AREA,cover.scopes().getFirst().context().measure());
    var r = PatternCatalogYaml.loadClasspath("/catalogs/ecosystem_rules_pilot.yaml").entries().stream()
        .filter(e -> e.alias().equals("SeasonalMarshCycleRule")).findFirst().orElseThrow().rule();
    var changed = new PatternRule(2,r.composition().replace("+++","++++"),r.participants(),r.profiles(),r.criteria(),r.provenance());
    var context = r.profiles().getFirst(); var parameters = resourcePolicy("fuzzy-duration-example.yaml");
    var duration = fixture(parameters).compare(PatternRuleYaml.canonical(r),PatternRuleYaml.canonical(changed));
    assertEquals(MediationResult.Status.DEFINED,duration.status());
    assertEquals(PatternRule.Measure.DURATION,duration.scopes().getFirst().context().measure());
    parameters.put("configurations",Map.of("ecosystem.seed:HydroperiodCycle",binding("AREA",context.interpretation(),context.support())));
    assertEquals(MediationResult.Status.UNKNOWN,fixture(parameters).compare(PatternRuleYaml.canonical(r),PatternRuleYaml.canonical(changed)).status());
  }
  @Test void typedIntervalsAreNotSilentlyFuzzifiedOrDiscarded() {
    var r = PatternCatalogYaml.loadClasspath("/catalogs/ecosystem_rules_pilot.yaml").entries().getFirst().rule();
    var q = (PatternRule.Constraint)r.criteria();
    var altered = new PatternRule.Constraint(q.participant(),q.observable(),q.unit(),null,false,new BigDecimal("6"),false,q.support());
    var broader = new PatternRule(2,r.composition(),r.participants(),r.profiles(),altered,r.provenance());
    var f = fixture(policy()); var result = f.compare(PatternRuleYaml.canonical(r),PatternRuleYaml.canonical(broader));
    assertTrue(result.leftEntailsRight()); assertFalse(result.rightEntailsLeft());
    assertEquals(Authority.DistanceStatus.UNKNOWN,f.distance(PatternRuleYaml.canonical(r),PatternRuleYaml.canonical(broader)).status());
  }
  @Test void strictConfigurationRejectsTyposUnsupportedProfilesAndInvalidNumbers() {
    for (var entry : Map.<String,Object>of("version","future","spread","1","steps",24.5,"admissionAlpha","0","overlapThreshold","NaN","admission","OVERLAP","typo",true).entrySet()) {
      var parameters = policy(); parameters.put(entry.getKey(),entry.getValue());
      assertThrows(IllegalArgumentException.class,()->fixture(parameters),entry.getKey());
    }
    var parameters = policy(); var bad = new HashMap<>(binding("AREA","urn:test",new PatternRule.Support("x","y","z")));
    bad.put("independentArguments",false); parameters.put("configurations",Map.of(H,bad));
    assertThrows(IllegalArgumentException.class,()->fixture(parameters));
  }
  @Test void workBudgetReturnsUnknownInsteadOfAPartialCompatibilityScore() {
    var args = new ArrayList<String>(); var scaled = new ArrayList<String>();
    for (int i = 0; i < 128; i++) { args.add("+seed:I"+i); scaled.add("++seed:I"+i); }
    String a = H+"("+String.join(",",args)+")", b = H+"("+String.join(",",scaled)+")";
    var result = fixture(policy()).compare(V+"("+a+","+a+")",V+"("+b+","+b+")");
    assertEquals(MediationResult.Status.UNKNOWN,result.status()); assertNull(result.overlap());
    assertTrue(result.explanations().getFirst().contains("65536"));
  }
}
