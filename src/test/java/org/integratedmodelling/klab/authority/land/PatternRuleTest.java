package org.integratedmodelling.klab.authority.land;

import static org.junit.jupiter.api.Assertions.*;
import static org.integratedmodelling.klab.authority.land.PatternRule.*;
import static org.integratedmodelling.klab.authority.land.RuleEvaluation.Truth.*;
import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.integratedmodelling.klab.api.services.Authority;
import org.junit.jupiter.api.Test;

class PatternRuleTest {
  private static final String CATALOG = "ecosystem_rules_pilot";
  private static final PatternCatalog DATA = PatternCatalogYaml.loadClasspath("/catalogs/"+CATALOG+".yaml");
  private static final Support SUPPORT = new Support("habitat-patch","reference-year","annual-mean");
  private static final class Semantics implements RuleSemantics {
    final Map<String,String> types = new HashMap<>();
    Semantics() {
      for (String namespace : List.of("landcover.seed","ecosystem.seed","ecosystem.rules.seed")) {
        try (var in = getClass().getResourceAsStream("/vocabularies/"+namespace+".kwv")) {
          String text = new String(Objects.requireNonNull(in).readAllBytes(), StandardCharsets.UTF_8);
          var m = java.util.regex.Pattern.compile("(?m)^(identity|configuration|thing|quantity|duration|event|process|attribute|realm|role|relationship) ([A-Z][A-Za-z0-9_]*)").matcher(text);
          while(m.find()) types.put(namespace+":"+m.group(2),m.group(1).toUpperCase());
        } catch (IOException e) { throw new UncheckedIOException(e); }
      }
      types.put("physical:HorizontalArrangement","CONFIGURATION");
      types.put("physical:VerticalArrangement","CONFIGURATION");
    }
    public String worldview() { return "rule.testing"; }
    public String validate(String reference,Role role) { return validateTyped(reference,role.name()); }
    public String validateTyped(String reference,String type) {
      String actual=types.get(reference);
      if (!(type.equals("SUBSTANTIAL") ? Set.of("THING","EVENT","PROCESS","CONFIGURATION","RELATIONSHIP").contains(actual == null ? "" : actual) : type.equals(actual)))
        throw new IllegalArgumentException("Wrong type/missing: "+reference+" expected "+type);
      return reference;
    }
    public String validateObservable(String expression) {
      if (!Set.of("QUANTITY","DURATION").contains(types.getOrDefault(expression,""))) throw new IllegalArgumentException("Not a quality");
      return expression;
    }
    public boolean validateAttribution(String bearer,List<String> predicates) { return true; }
    public boolean validateObservableBinding(String observable,String bearer,String unit) {
      if (!Set.of("1","month").contains(unit)) throw new IllegalArgumentException("Wrong unit"); return true;
    }
    public boolean validateRelationship(String relation,String source,String target) { return true; }
  }
  private static PatternRule rule(String alias) { return DATA.entries().stream().filter(e->e.alias().equals(alias)).findFirst().orElseThrow().rule(); }
  private static LandAuthority authority() { var a=new LandAuthority(); a.bindWorldview(new Semantics()); return a; }
  private static String bridge(LandAuthority a) {
    return a.configure(new Authority.ConfigurationRequest("rule.testing","ECO","ecosystem.seed:EcosystemClass",
        Map.of("urn",LandAuthority.URN,"domain","ECOSYSTEM","catalogs",List.of(CATALOG))));
  }
  private static PatternRule replace(PatternRule r,Clause c) {
    return new PatternRule(2,r.composition(),r.participants(),r.profiles(),c,r.provenance());
  }
  private static Constraint bound(BigDecimal min,boolean mi,BigDecimal max,boolean ma) {
    return new Constraint("soil","ecosystem.rules.seed:SoilPH","1",min,mi,max,ma,SUPPORT);
  }
  @Test void allSixExamplesResolveThroughNormalCodelistsAndOpaqueAuthorityCodes() {
    var a=authority(); String bridge=bridge(a);
    assertEquals(6,a.getCodelists(bridge).get(CATALOG).size());
    for(var entry:DATA.entries()) {
      var identity=a.reconcile(bridge,Map.of("alias",entry.alias(),"catalog",CATALOG));
      assertTrue(identity.getNotifications().isEmpty(),identity.getDescription());
      assertEquals(entry.identityCode(),identity.getId());
      assertEquals(identity.getId(),a.resolveIdentity(bridge,identity.getId()).getId());
      String token=identity.getLocator();
      assertEquals(identity.getId(),org.integratedmodelling.klab.api.services.reasoner.objects.AuthorityIdentitySyntax.decode(token.substring(4)));
    }
  }
  @Test void canonicalizationIgnoresParticipantNamesProvenanceAndLogicalOrder() {
    var r=rule("ChemosyntheticAlternativesRule"); var g=(Group)r.criteria();
    var children=new ArrayList<Clause>();
    for(var c:g.children()) { var q=(Qualification)c;children.add(new Qualification("renamed",q.type(),q.predicate(),q.present(),q.support())); }
    Collections.reverse(children);
    var renamed=new PatternRule(2,r.composition(),List.of(new Participant("renamed",List.of(),"ecosystem.rules.seed:EcosystemPatch")),
        r.profiles(),new Group(Logic.ANY,0,children),new Provenance("urn:other","other","Different provenance"));
    assertEquals(PatternRuleYaml.canonical(r),PatternRuleYaml.canonical(renamed));
    assertEquals(PatternRuleYaml.canonical(r),PatternRuleYaml.canonical(PatternRuleYaml.decode(PatternRuleYaml.canonical(r))));
  }
  @Test void canonicalizationPreservesDefiningUnitsSupportsProfilesAndBounds() {
    var r=rule("AcidBeechSoilRule");
    assertNotEquals(PatternRuleYaml.canonical(r),PatternRuleYaml.canonical(replace(r,bound(null,false,new BigDecimal("6"),false))));
    var q=(Constraint)r.criteria();
    var changed=new Constraint(q.participant(),q.observable(),"month",q.minimum(),q.minimumInclusive(),q.maximum(),q.maximumInclusive(),q.support());
    assertNotEquals(PatternRuleYaml.canonical(r),PatternRuleYaml.canonical(replace(r,changed)));
  }
  @Test void exactDecimalsAndEndpointInclusivitySurviveSerializationAndImplication() {
    var r=rule("AcidBeechSoilRule"); BigDecimal exact=new BigDecimal("0.12345678901234567890123456789");
    var precise=replace(r,bound(null,false,exact,true));
    assertEquals(exact,((Constraint)PatternRuleYaml.decode(PatternRuleYaml.canonical(precise)).criteria()).maximum());
    var closed=replace(r,bound(null,false,new BigDecimal("5"),true));
    assertEquals(PatternComparison.Status.LEFT_REFINES_RIGHT,RuleComparison.compare(r,closed,new Semantics()).status());
    assertThrows(IllegalArgumentException.class,()->bound(BigDecimal.ONE,false,BigDecimal.ONE,true));
    assertThrows(IllegalArgumentException.class,()->bound(null,true,BigDecimal.ONE,true));
  }
  @Test void implicationHandlesAlternativesWithoutRequiringAllBranches() {
    var alternatives=rule("ChemosyntheticAlternativesRule");
    var vent=replace(alternatives,((Group)alternatives.criteria()).children().getFirst());
    assertEquals(PatternComparison.Status.LEFT_REFINES_RIGHT,RuleComparison.compare(vent,alternatives,new Semantics()).status());
    var other=replace(alternatives,((Group)alternatives.criteria()).children().get(1));
    assertEquals(PatternComparison.Status.UNKNOWN,RuleComparison.compare(vent,other,new Semantics()).status());
  }
  @Test void authorityDistanceAdmitsOnlyCandidateEntailmentAndRetainsUnknown() {
    var a=authority(); String bridge=bridge(a); var narrow=rule("AcidBeechSoilRule");
    var broad=replace(narrow,bound(null,false,new BigDecimal("6"),false));
    String n=PatternRuleYaml.canonical(narrow),b=PatternRuleYaml.canonical(broad);
    assertEquals(0,a.semanticDistance(bridge,n,n).distance());
    assertEquals(1,a.semanticDistance(bridge,b,n).distance());
    assertEquals(Authority.DistanceStatus.UNKNOWN,a.semanticDistance(bridge,n,b).status());
    assertEquals(Authority.DistanceStatus.UNKNOWN,a.semanticDistance(bridge,n,narrow.composition()).status());
    a.releaseConfiguration(bridge);
    assertThrows(IllegalArgumentException.class,()->a.semanticDistance(bridge,n,n));
  }
  @Test void validationRecordsBearerRoleProcessAndProfileObligations() {
    var a=authority(); String bridge=bridge(a);
    assertTrue(a.validateRule(bridge,rule("AcidBeechSoilRule")).ready());
    var seagrass=a.validateRule(bridge,rule("SeagrassDiagnosticsRule"));
    assertTrue(seagrass.obligations().stream().anyMatch(s->s.contains("Infer bearer")));
    assertTrue(seagrass.obligations().stream().anyMatch(s->s.contains("role context")));
    var cycle=a.validateRule(bridge,rule("SeasonalMarshCycleRule"));
    assertTrue(cycle.obligations().stream().anyMatch(s->s.contains("profile")));
    assertTrue(cycle.obligations().stream().anyMatch(s->s.contains("relation evaluator")));
    assertFalse(a.validateRule(bridge,rule("PyricForestRule")).ready());
    var bad=replace(rule("AcidBeechSoilRule"),new Qualification("soil",PredicateType.IDENTITY,"ecosystem.rules.seed:Fire",true,SUPPORT));
    assertThrows(IllegalArgumentException.class,()->a.validateRule(bridge,bad));
  }
  @Test void legacySemanticHostsRemainProvisional() {
    var r=rule("AcidBeechSoilRule");
    PatternSemantics legacy=new PatternSemantics() {
      public String worldview(){return "test";} public String validate(String ref,Role role){return ref;}
    };
    assertFalse(RuleValidation.validate(r,legacy).ready());
  }
  private static RuleEvaluation.Evidence evidence(BigDecimal value,String unit,Support support) {
    return new RuleEvaluation.Evidence() {
      public RuleEvaluation.Truth composition(PatternRule r) { return TRUE; }
      public Optional<RuleEvaluation.NumericObservation> measurement(PatternRule r,Constraint c) {
        return Optional.of(new RuleEvaluation.NumericObservation(value,unit,support));
      }
    };
  }
  @Test void numericEvaluationChecksEndpointsUnitsAndSupportWithoutInventingConversions() {
    var r=rule("AcidBeechSoilRule");
    assertEquals(TRUE,RuleEvaluation.evaluate(r,evidence(new BigDecimal("4.99"),"1",SUPPORT)).truth());
    assertEquals(FALSE,RuleEvaluation.evaluate(r,evidence(new BigDecimal("5"),"1",SUPPORT)).truth());
    assertEquals(UNKNOWN,RuleEvaluation.evaluate(r,evidence(new BigDecimal("4"),"pH",SUPPORT)).truth());
    assertEquals(UNKNOWN,RuleEvaluation.evaluate(r,evidence(new BigDecimal("4"),"1",new Support("plot","day","minimum"))).truth());
    assertEquals(UNKNOWN,RuleEvaluation.evaluate(r,new RuleEvaluation.Evidence() {}).truth());
  }
  @Test void oneTrueAlternativeSufficesAndUnknownDoesNotCountAsFalse() {
    var r=rule("ChemosyntheticAlternativesRule");
    var evaluation=RuleEvaluation.evaluate(r,new RuleEvaluation.Evidence() {
      public RuleEvaluation.Truth composition(PatternRule rule){return TRUE;}
      public RuleEvaluation.Finding finding(PatternRule rule,Clause c) {
        return ((Qualification)c).predicate().endsWith("ColdSeep") ? new RuleEvaluation.Finding(TRUE,false) : RuleEvaluation.Finding.unknown();
      }
    });
    assertEquals(TRUE,evaluation.truth());
  }
  @Test void absenceRequiresCompleteNegativeEvidence() {
    var r=rule("ChemosyntheticAlternativesRule");var q=(Qualification)((Group)r.criteria()).children().getFirst();
    var absent=replace(r,new Qualification(q.participant(),q.type(),q.predicate(),false,q.support()));
    for(boolean complete:List.of(false,true)) {
      var result=RuleEvaluation.evaluate(absent,new RuleEvaluation.Evidence(){
        public RuleEvaluation.Truth composition(PatternRule rule){return TRUE;}
        public RuleEvaluation.Finding finding(PatternRule rule,Clause c){return new RuleEvaluation.Finding(FALSE,complete);}
      });
      assertEquals(complete?TRUE:UNKNOWN,result.truth());
    }
  }
  @Test void cardinalityAndTypicalDescriptionsHaveDistinctTruthBehavior() {
    var r=rule("ChemosyntheticAlternativesRule");var children=((Group)r.criteria()).children();
    var cardinality=replace(r,new Group(Logic.AT_LEAST,2,children));
    var evidence=new RuleEvaluation.Evidence(){
      public RuleEvaluation.Truth composition(PatternRule rule){return TRUE;}
      public RuleEvaluation.Finding finding(PatternRule rule,Clause c){return new RuleEvaluation.Finding(c.equals(children.getFirst())?TRUE:FALSE,true);}
    };
    assertEquals(FALSE,RuleEvaluation.evaluate(cardinality,evidence).truth());
    var typical=replace(r,new Typical(children.get(1),"Typical only"));
    assertEquals(TRUE,RuleEvaluation.evaluate(typical,evidence).truth());
    assertTrue(RuleEvaluation.evaluate(typical,evidence).explanation().stream().anyMatch(RuleEvaluation.Step::advisory));
    assertThrows(IllegalArgumentException.class,()->new Group(Logic.ANY,0,List.of(new Typical(children.getFirst(),"advisory"),children.get(1))));
    assertThrows(IllegalArgumentException.class,()->new Group(Logic.ANY,0,List.of(new Group(Logic.ALL,0,
        List.of(new Typical(children.getFirst(),"nested advisory"))),children.get(1))));
  }
  @Test void malformedBindingsLogicAndYamlAreRejected() throws Exception {
    var r=rule("AcidBeechSoilRule");
    assertThrows(IllegalArgumentException.class,()->new PatternRule(2,r.composition(),List.of(new Participant("other",List.of(),null)),List.of(),r.criteria(),r.provenance()));
    assertThrows(IllegalArgumentException.class,()->new Participant("soil",List.of(-1),null));
    assertThrows(IllegalArgumentException.class,()->new Group(Logic.AT_LEAST,2,List.of(r.criteria())));
    String json=new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(r);
    for(String bad:List.of(json.replace("\"schemaVersion\":2","\"schemaVersion\":3"),json.replaceFirst("\\{","{\"unknown\":true,"),
        json+"\n---\n{}",json.replace("\"maximumInclusive\":false","\"maximumInclusive\":\"false\""))) {
      assertThrows(IllegalArgumentException.class,()->PatternRuleYaml.load(new ByteArrayInputStream(bad.getBytes(StandardCharsets.UTF_8))));
    }
    assertThrows(IllegalArgumentException.class,()->PatternRuleYaml.load(new ByteArrayInputStream(new byte[PatternRuleYaml.MAX_BYTES+1])));
  }
}
