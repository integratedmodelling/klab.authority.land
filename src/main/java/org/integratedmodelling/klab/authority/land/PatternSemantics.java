package org.integratedmodelling.klab.authority.land;

/** Host-supplied, worldview-scoped semantic access. Implementations must be thread safe. */
public interface PatternSemantics {
  enum Role {
    CONFIGURATION,
    IDENTITY
  }

  enum Relation {
    EQUIVALENT,
    NARROWER,
    BROADER,
    UNKNOWN
  }

  String worldview();

  /**
   * Resolve, verify satisfiability and role, and return a parseable canonical reference. Must
   * reject unresolved references, wrong roles and stale worldview contexts. This validates
   * individual concepts, not the inherency/bearer of the whole expression.
   */
  String validate(String reference, Role role);

  /** Proven semantic relation; unrelated names do not imply disjoint patterns. */
  default Relation relate(String left, String right) {
    return left.equals(right) ? Relation.EQUIVALENT : Relation.UNKNOWN;
  }
}
