package org.integratedmodelling.klab.authority.land;

import java.util.Objects;
import org.integratedmodelling.klab.api.knowledge.Concept;
import org.integratedmodelling.klab.api.knowledge.SemanticType;
import org.integratedmodelling.klab.api.services.Reasoner;

/** Adapter to a loaded Reasoner, including worldview and knowledge-revision guards. */
public final class ReasonerPatternSemantics implements RuleSemantics {
  private final Reasoner reasoner;
  private final String worldview;
  private final String worldviewId;
  private final long revision;

  public ReasonerPatternSemantics(Reasoner reasoner) {
    this.reasoner = Objects.requireNonNull(reasoner);
    var capabilities = reasoner.capabilities(reasoner.serviceScope());
    worldview = capabilities.getWorldviewUrn();
    worldviewId = capabilities.getWorldviewId();
    revision = capabilities.getKnowledgeRevision();
    if (worldview == null || worldviewId == null || !capabilities.isConsistent())
      throw new IllegalArgumentException("A consistent loaded worldview is required");
  }

  @Override
  public String worldview() {
    return worldview;
  }

  private void checkRevision() {
    var capabilities = reasoner.capabilities(reasoner.serviceScope());
    if (!worldview.equals(capabilities.getWorldviewUrn())
        || !worldviewId.equals(capabilities.getWorldviewId())
        || revision != capabilities.getKnowledgeRevision()
        || !capabilities.isConsistent())
      throw new IllegalStateException("Worldview changed; release and reconfigure LAND bridges");
  }

  private Concept resolve(String reference) {
    checkRevision();
    var concept = reasoner.resolveConcept(reference);
    if (concept == null || concept.is(SemanticType.NOTHING) || !reasoner.satisfiable(concept))
      throw new IllegalArgumentException("Unresolved or unsatisfiable concept: " + reference);
    return concept;
  }

  @Override
  public String validate(String reference, Role role) {
    var concept = resolve(reference);
    if (!concept.is(
        role == Role.CONFIGURATION ? SemanticType.CONFIGURATION : SemanticType.IDENTITY))
      throw new IllegalArgumentException("Expected " + role + ": " + reference);
    return concept.getUrn();
  }

  @Override
  public Relation relate(String left, String right) {
    var a = resolve(left);
    var b = resolve(right);
    boolean ab = reasoner.is(a, b), ba = reasoner.is(b, a);
    return ab && ba
        ? Relation.EQUIVALENT
        : ab ? Relation.NARROWER : ba ? Relation.BROADER : Relation.UNKNOWN;
  }

  @Override
  public String validateTyped(String reference, String type) {
    PatternRule.reference(reference);
    var concept = resolve(reference);
    boolean valid =
        "SUBSTANTIAL".equals(type)
            ? concept.is(SemanticType.SUBJECT)
                || concept.is(SemanticType.AGENT)
                || concept.is(SemanticType.EVENT)
                || concept.is(SemanticType.PROCESS)
                || concept.is(SemanticType.CONFIGURATION)
                || concept.is(SemanticType.RELATIONSHIP)
            : concept.is(SemanticType.valueOf(type));
    if (!valid) throw new IllegalArgumentException("Expected " + type + ": " + reference);
    return concept.getUrn();
  }

  @Override
  public String validateObservable(String expression) {
    PatternRule.text(expression);
    // Reuse the revision guard even when resolving a complete observable expression.
    checkRevision();
    var observable = reasoner.resolveObservable(expression);
    if (observable == null
        || !observable.is(SemanticType.QUALITY)
        || observable.getArtifactType() == null
        || !observable.getArtifactType().isNumeric()
        || !reasoner.satisfiable(observable))
      throw new IllegalArgumentException(
          "Expected a satisfiable numeric quality observable: " + expression);
    return observable.getUrn();
  }

  @Override
  public boolean validateAttribution(String bearer, java.util.List<String> predicates) {
    resolve(bearer);
    if (predicates.isEmpty()) return true;
    var observable = reasoner.resolveObservable(String.join(" ", predicates) + " " + bearer);
    if (observable == null || !reasoner.satisfiable(observable))
      throw new IllegalArgumentException("Inconsistent participant attribution on " + bearer);
    return true;
  }

  @Override
  public boolean validateObservableBinding(String observable, String bearer, String unit) {
    resolve(bearer);
    var bound = reasoner.resolveObservable("(" + observable + ") of " + bearer + " in " + unit);
    if (bound == null
        || !bound.is(SemanticType.QUALITY)
        || bound.getArtifactType() == null
        || !bound.getArtifactType().isNumeric()
        || !reasoner.satisfiable(bound)
        || bound.getUnit() == null)
      throw new IllegalArgumentException("Invalid observable/bearer/unit binding: " + observable);
    return true;
  }

  @Override
  public boolean validateRelationship(String relation, String source, String target) {
    resolve(relation);
    resolve(source);
    resolve(target);
    var bound = reasoner.resolveObservable(relation + " linking " + source + " to " + target);
    if (bound == null || !reasoner.satisfiable(bound))
      throw new IllegalArgumentException("Invalid relationship endpoints");
    return true;
  }
}
