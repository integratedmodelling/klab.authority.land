package org.integratedmodelling.klab.authority.land;

import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.integratedmodelling.klab.api.knowledge.Concept;
import org.integratedmodelling.klab.api.knowledge.SemanticType;
import org.integratedmodelling.klab.api.services.Reasoner;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReasonerPatternSemanticsTest {
  @SuppressWarnings("unchecked")
  private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
    return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, handler);
  }
  private static Concept concept(String urn, SemanticType type) {
    return proxy(Concept.class, (object, method, args) -> switch (method.getName()) {
      case "getUrn" -> urn;
      case "is" -> type == args[0];
      default -> throw new UnsupportedOperationException(method.getName());
    });
  }
  private static Reasoner reasoner(AtomicLong revision) {
    var concepts = Map.of("physical:Cover", concept("physical:Cover", SemanticType.CONFIGURATION),
        "taxonomy:Alias", concept("taxonomy:Canonical", SemanticType.IDENTITY));
    var capabilities = proxy(Reasoner.Capabilities.class, (object, method, args) -> switch (method.getName()) {
      case "getWorldviewUrn" -> "test.worldview";
      case "getWorldviewId" -> "loaded-1";
      case "getKnowledgeRevision" -> revision.get();
      case "isConsistent" -> true;
      default -> throw new UnsupportedOperationException(method.getName());
    });
    return proxy(Reasoner.class, (object, method, args) -> switch (method.getName()) {
      case "serviceScope" -> null;
      case "capabilities" -> capabilities;
      case "resolveConcept" -> concepts.get(args[0]);
      case "satisfiable" -> true;
      case "is" -> args[0] == args[1];
      default -> throw new UnsupportedOperationException(method.getName());
    });
  }

  @Test void validatesRolesAndCanonicalizesAliasesThroughReasoner() {
    var semantics = new ReasonerPatternSemantics(reasoner(new AtomicLong(1)));
    assertEquals("test.worldview", semantics.worldview());
    assertEquals("taxonomy:Canonical", semantics.validate("taxonomy:Alias", PatternSemantics.Role.IDENTITY));
    assertEquals("physical:Cover", semantics.validate("physical:Cover", PatternSemantics.Role.CONFIGURATION));
    assertThrows(IllegalArgumentException.class, () -> semantics.validate("taxonomy:Alias", PatternSemantics.Role.CONFIGURATION));
    assertThrows(IllegalArgumentException.class, () -> semantics.validate("physical:Cover", PatternSemantics.Role.IDENTITY));
    assertThrows(IllegalArgumentException.class, () -> semantics.validate("missing:Identity", PatternSemantics.Role.IDENTITY));
  }

  @Test void rejectsStaleKnowledgeAndDoesNotEquateUnrelatedConcepts() {
    var revision = new AtomicLong(1);
    var semantics = new ReasonerPatternSemantics(reasoner(revision));
    assertEquals(PatternSemantics.Relation.EQUIVALENT, semantics.relate("taxonomy:Alias", "taxonomy:Alias"));
    assertEquals(PatternSemantics.Relation.UNKNOWN, semantics.relate("taxonomy:Alias", "physical:Cover"));
    revision.incrementAndGet();
    assertThrows(IllegalStateException.class, () -> semantics.validate("taxonomy:Alias", PatternSemantics.Role.IDENTITY));
  }

  @Test void typedValidationBuildsCombinedAttributionUnitsAndRelationshipEndpoints() {
    var revision = new AtomicLong(1);
    var seen = new java.util.ArrayList<String>();
    var capabilities = proxy(Reasoner.Capabilities.class,(o,m,a)->switch(m.getName()) {
      case "getWorldviewUrn" -> "test.worldview"; case "getWorldviewId" -> "loaded-1";
      case "getKnowledgeRevision" -> revision.get(); case "isConsistent" -> true;
      default -> throw new UnsupportedOperationException(m.getName());
    });
    var concepts = Map.of("test:Soil",concept("test:Soil",SemanticType.SUBJECT),
        "test:Acid",concept("test:Acid",SemanticType.ATTRIBUTE),
        "test:Linked",concept("test:Linked",SemanticType.RELATIONSHIP));
    var numeric = proxy(org.integratedmodelling.klab.api.knowledge.Observable.class,(o,m,a)->switch(m.getName()) {
      case "getUrn" -> "test:PH";
      case "is" -> a[0] == SemanticType.QUALITY;
      case "getArtifactType" -> org.integratedmodelling.klab.api.knowledge.Artifact.Type.NUMBER;
      case "getUnit" -> proxy(org.integratedmodelling.klab.api.data.mediation.Unit.class,(u,v,w)->null);
      default -> throw new UnsupportedOperationException(m.getName());
    });
    var reasoner = proxy(Reasoner.class,(o,m,a)->switch(m.getName()) {
      case "serviceScope" -> null; case "capabilities" -> capabilities;
      case "resolveConcept" -> concepts.get(a[0]); case "satisfiable" -> true;
      case "resolveObservable" -> { seen.add((String)a[0]); yield ((String)a[0]).contains("bad") ? null : numeric; }
      default -> throw new UnsupportedOperationException(m.getName());
    });
    var s = new ReasonerPatternSemantics(reasoner);
    assertEquals("test:Soil",s.validateTyped("test:Soil","SUBSTANTIAL"));
    assertThrows(IllegalArgumentException.class,()->s.validateTyped("test:Acid","IDENTITY"));
    assertTrue(s.validateAttribution("test:Soil",java.util.List.of("test:Acid")));
    assertTrue(s.validateObservableBinding("test:PH","test:Soil","1"));
    assertTrue(s.validateRelationship("test:Linked","test:Soil","test:Soil"));
    assertEquals(java.util.List.of("test:Acid test:Soil","(test:PH) of test:Soil in 1","test:Linked linking test:Soil to test:Soil"),seen);
    assertThrows(IllegalArgumentException.class,()->s.validateObservableBinding("test:PH","test:Soil","bad"));
    revision.incrementAndGet();
    assertThrows(IllegalStateException.class,()->s.validateObservable("test:PH"));
  }
}
