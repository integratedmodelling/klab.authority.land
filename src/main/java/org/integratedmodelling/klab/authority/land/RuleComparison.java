package org.integratedmodelling.klab.authority.land;

import java.math.BigDecimal;
import java.util.List;
import static org.integratedmodelling.klab.authority.land.PatternRule.*;

/**
 * Sound but deliberately incomplete implication rules. No fuzzy distance or incompatibility
 * inferred.
 */
public final class RuleComparison {
  private RuleComparison() {}

  public static PatternComparison compare(
      PatternRule left, PatternRule right, PatternSemantics semantics) {
    var a = PatternRuleYaml.decode(PatternRuleYaml.canonical(left));
    var b = PatternRuleYaml.decode(PatternRuleYaml.canonical(right));
    if (!a.participants().equals(b.participants()) || !a.profiles().equals(b.profiles()))
      return unknown();
    var composition =
        PatternComparison.compare(
                PatternParser.parse(a.composition()),
                PatternParser.parse(b.composition()),
                semantics)
            .status();
    boolean ab =
        (composition == PatternComparison.Status.EQUIVALENT
                || composition == PatternComparison.Status.LEFT_REFINES_RIGHT)
            && entails(a.criteria(), b.criteria(), semantics, 0);
    boolean ba =
        (composition == PatternComparison.Status.EQUIVALENT
                || composition == PatternComparison.Status.RIGHT_REFINES_LEFT)
            && entails(b.criteria(), a.criteria(), semantics, 0);
    if (!ab && !ba) return unknown();
    return new PatternComparison(
        ab && ba
            ? PatternComparison.Status.EQUIVALENT
            : ab
                ? PatternComparison.Status.LEFT_REFINES_RIGHT
                : PatternComparison.Status.RIGHT_REFINES_LEFT,
        List.of(
            "Proven directional entailment of composition, bindings, profiles and required criteria; typical descriptions are advisory"));
  }

  private static PatternComparison unknown() {
    return new PatternComparison(
        PatternComparison.Status.UNKNOWN,
        List.of(
            "No entailment proven for these bindings, profiles or criteria; this does not establish disjointness"));
  }

  private static boolean entails(Clause a, Clause b, PatternSemantics s, int depth) {
    if (depth > 64) return false;
    if (b instanceof Typical) return true;
    if (a.equals(b)) return true;
    if (a instanceof Typical) return false;
    if (b instanceof Group g && g.operator() == Logic.ALL)
      return g.children().stream().allMatch(c -> entails(a, c, s, depth + 1));
    if (a instanceof Group g && g.operator() == Logic.ANY)
      return g.children().stream().allMatch(c -> entails(c, b, s, depth + 1));
    if (a instanceof Group g && g.operator() == Logic.ALL)
      return g.children().stream().anyMatch(c -> entails(c, b, s, depth + 1));
    if (b instanceof Group g && g.operator() == Logic.ANY)
      return g.children().stream().anyMatch(c -> entails(a, c, s, depth + 1));
    if (a instanceof Constraint x && b instanceof Constraint y)
      return x.participant().equals(y.participant())
          && x.observable().equals(y.observable())
          && x.unit().equals(y.unit())
          && x.support().equals(y.support())
          && lower(x.minimum(), x.minimumInclusive(), y.minimum(), y.minimumInclusive())
          && upper(x.maximum(), x.maximumInclusive(), y.maximum(), y.maximumInclusive());
    if (a instanceof Qualification x
        && b instanceof Qualification y
        && x.type() == y.type()
        && x.present() == y.present()
        && x.participant().equals(y.participant())
        && x.support().equals(y.support())) {
      var relation = s.relate(x.predicate(), y.predicate());
      return relation == PatternSemantics.Relation.EQUIVALENT
          || (x.present() && relation == PatternSemantics.Relation.NARROWER);
    }
    return false;
  }

  private static boolean lower(BigDecimal a, boolean ai, BigDecimal b, boolean bi) {
    return b == null || a != null && (a.compareTo(b) > 0 || a.compareTo(b) == 0 && (!ai || bi));
  }

  private static boolean upper(BigDecimal a, boolean ai, BigDecimal b, boolean bi) {
    return b == null || a != null && (a.compareTo(b) < 0 || a.compareTo(b) == 0 && (!ai || bi));
  }
}
