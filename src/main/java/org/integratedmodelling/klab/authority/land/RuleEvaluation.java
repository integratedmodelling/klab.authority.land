package org.integratedmodelling.klab.authority.land;

import java.math.BigDecimal;
import java.util.*;
import static org.integratedmodelling.klab.authority.land.PatternRule.*;

/**
 * Crisp, three-valued reference evaluator. Hosts supply scoped evidence; this does not discover
 * data.
 */
public record RuleEvaluation(Truth truth, List<Step> explanation) {
  public enum Truth {
    TRUE,
    FALSE,
    UNKNOWN
  }

  public record Step(Clause criterion, Truth truth, boolean advisory, String reason) {}

  public record Finding(Truth present, boolean complete) {
    public Finding {
      Objects.requireNonNull(present);
    }

    public static Finding unknown() {
      return new Finding(Truth.UNKNOWN, false);
    }
  }

  public record NumericObservation(BigDecimal value, String unit, Support support) {
    public NumericObservation {
      Objects.requireNonNull(value);
      text(unit);
      Objects.requireNonNull(support);
    }
  }

  /** Evidence must concern the supplied rule, participant occurrence and exact stated support. */
  public interface Evidence {
    default Truth composition(PatternRule rule) {
      return Truth.UNKNOWN;
    }

    default Finding finding(PatternRule rule, Clause criterion) {
      return Finding.unknown();
    }

    default Optional<NumericObservation> measurement(PatternRule rule, Constraint criterion) {
      return Optional.empty();
    }
  }

  public RuleEvaluation {
    explanation = List.copyOf(explanation);
  }

  public static RuleEvaluation evaluate(PatternRule rule, Evidence evidence) {
    Objects.requireNonNull(evidence);
    var steps = new ArrayList<Step>();
    Truth criteria = evaluate(rule, rule.criteria(), evidence, steps, false);
    Truth composition = Objects.requireNonNull(evidence.composition(rule));
    Truth result = all(List.of(composition, criteria));
    steps.add(
        new Step(
            null,
            composition,
            false,
            "Composition evidence (including declared arrangement/profile interpretation)"));
    return new RuleEvaluation(result, steps);
  }

  private static Truth evaluate(
      PatternRule rule, Clause c, Evidence e, List<Step> steps, boolean advisory) {
    Truth result;
    String reason;
    if (c instanceof Typical t) {
      evaluate(rule, t.criterion(), e, steps, true);
      steps.add(
          new Step(
              c, Truth.TRUE, true, "Typical description does not decide membership: " + t.note()));
      return Truth.TRUE;
    }
    if (c instanceof Group g) {
      var values = g.children().stream().map(n -> evaluate(rule, n, e, steps, advisory)).toList();
      long yes = values.stream().filter(v -> v == Truth.TRUE).count();
      long unknown = values.stream().filter(v -> v == Truth.UNKNOWN).count();
      int required =
          g.operator() == Logic.ALL ? values.size() : g.operator() == Logic.ANY ? 1 : g.minimum();
      result =
          yes >= required ? Truth.TRUE : yes + unknown < required ? Truth.FALSE : Truth.UNKNOWN;
      reason = "Logical " + g.operator() + ": " + yes + " true, " + unknown + " unknown";
    } else if (c instanceof Constraint q) {
      var observation = e.measurement(rule, q);
      if (observation.isEmpty()
          || !observation.get().unit().equals(q.unit())
          || !observation.get().support().equals(q.support())) {
        result = Truth.UNKNOWN;
        reason = "Missing measurement or unmatched unit/support; no implicit conversion";
      } else {
        BigDecimal v = observation.get().value();
        boolean lower =
            q.minimum() == null
                || v.compareTo(q.minimum()) > 0
                || v.compareTo(q.minimum()) == 0 && q.minimumInclusive();
        boolean upper =
            q.maximum() == null
                || v.compareTo(q.maximum()) < 0
                || v.compareTo(q.maximum()) == 0 && q.maximumInclusive();
        result = lower && upper ? Truth.TRUE : Truth.FALSE;
        reason = "Exact decimal interval comparison in " + q.unit();
      }
    } else {
      Finding f = Objects.requireNonNull(e.finding(rule, c));
      boolean expected =
          c instanceof Qualification q ? q.present() : !(c instanceof Occurrent o) || o.present();
      Truth present = f.present() == Truth.FALSE && !f.complete() ? Truth.UNKNOWN : f.present();
      result =
          expected || present == Truth.UNKNOWN
              ? present
              : present == Truth.TRUE ? Truth.FALSE : Truth.TRUE;
      reason = "Scoped evidence; a negative finding requires complete coverage";
    }
    steps.add(new Step(c, result, advisory, reason));
    return result;
  }

  private static Truth all(List<Truth> values) {
    return values.contains(Truth.FALSE)
        ? Truth.FALSE
        : values.contains(Truth.UNKNOWN) ? Truth.UNKNOWN : Truth.TRUE;
  }
}
