package org.integratedmodelling.klab.authority.land;

/** Optional richer host validation. Unsupported checks are deferred, never silently successful. */
public interface RuleSemantics extends PatternSemantics {
  String validateTyped(String reference, String semanticType);
  /** Resolve a numeric quality, retaining any derived-observable operators. */
  String validateObservable(String expression);
  /** Validate all required attributions together with the participant's composition identity. */
  default boolean validateAttribution(String bearer, java.util.List<String> predicates) { return false; }
  default boolean validateObservableBinding(String observable, String bearer, String unit) { return false; }
  default boolean validateRelationship(String relation, String sourceBearer, String targetBearer) { return false; }
}
