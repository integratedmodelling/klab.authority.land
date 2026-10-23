package org.integratedmodelling.klab.authority.land;

import java.util.List;

/**
 * Conservative structural comparison. Configuration-specific quantity semantics need a mediation
 * policy.
 */
public record PatternComparison(Status status, List<String> explanations) {
  public enum Status {
    EQUIVALENT,
    LEFT_REFINES_RIGHT,
    RIGHT_REFINES_LEFT,
    UNKNOWN
  }

  public PatternComparison {
    explanations = List.copyOf(explanations);
  }

  static PatternComparison compare(CoverPattern a, CoverPattern b, PatternSemantics semantics) {
    if (a.marks() != b.marks())
      return new PatternComparison(
          Status.UNKNOWN,
          List.of("Root quantitative marks have no shared enclosing configuration"));
    var status = nodes(a, b, semantics);
    return new PatternComparison(
        status,
        List.of(
            switch (status) {
              case EQUIVALENT ->
                  "Same ordered structure, equivalent concepts and identical local quantitative marks";
              case LEFT_REFINES_RIGHT ->
                  "Left entails the right under ordered, positive-presence pattern semantics";
              case RIGHT_REFINES_LEFT ->
                  "Right entails the left under ordered, positive-presence pattern semantics";
              case UNKNOWN ->
                  "Structure, concept relationships or relative quantities require further evidence; incompatibility is not established";
            }));
  }

  private static Status nodes(CoverPattern a, CoverPattern b, PatternSemantics semantics) {
    if (a.isConfiguration() != b.isConfiguration() || a.arguments().size() != b.arguments().size())
      return Status.UNKNOWN;
    var relation = semantics.relate(a.concept(), b.concept());
    if (a.isConfiguration() && relation != PatternSemantics.Relation.EQUIVALENT)
      return Status.UNKNOWN;
    Status result =
        switch (relation) {
          case EQUIVALENT -> Status.EQUIVALENT;
          case NARROWER -> Status.LEFT_REFINES_RIGHT;
          case BROADER -> Status.RIGHT_REFINES_LEFT;
          case UNKNOWN -> Status.UNKNOWN;
        };
    boolean coupled = hasRatios(a.arguments()) || hasRatios(b.arguments());
    for (int i = 0; i < a.arguments().size(); i++) {
      var child = nodes(a.arguments().get(i), b.arguments().get(i), semantics);
      // Abundance of a taxon does not entail the same abundance of its broader genus.
      if (child != Status.EQUIVALENT && coupled) return Status.UNKNOWN;
      result = merge(result, child);
    }
    return merge(result, abundance(a.arguments(), b.arguments()));
  }

  private static Status abundance(List<CoverPattern> a, List<CoverPattern> b) {
    if (a.stream()
        .map(CoverPattern::marks)
        .toList()
        .equals(b.stream().map(CoverPattern::marks).toList())) return Status.EQUIVALENT;
    // Even proportional counts may encode different uncertainty under a future fuzzy policy.
    if (a.stream().allMatch(n -> n.marks() == 0) && hasRatios(b)) return Status.RIGHT_REFINES_LEFT;
    if (b.stream().allMatch(n -> n.marks() == 0) && hasRatios(a)) return Status.LEFT_REFINES_RIGHT;
    return Status.UNKNOWN;
  }

  private static boolean hasRatios(List<CoverPattern> nodes) {
    return nodes.stream().filter(n -> n.marks() > 0).limit(2).count() == 2;
  }

  private static Status merge(Status a, Status b) {
    if (a == b || b == Status.EQUIVALENT) return a;
    if (a == Status.EQUIVALENT) return b;
    return Status.UNKNOWN;
  }
}
