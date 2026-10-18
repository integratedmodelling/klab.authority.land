package org.integratedmodelling.klab.authority.land;

import java.util.*;
import static org.integratedmodelling.klab.authority.land.ExactFraction.*;

/** Projective triangular fuzzy boxes, exact rational feasibility and conservative directional proofs. */
final class FuzzyMediation {
  private static final int MAX_PAIR_CHECKS = 65536;
  private record Cut(ExactFraction lower, ExactFraction upper) {}
  /** Affine function of alpha. */
  private record Linear(ExactFraction constant, ExactFraction slope) {
    ExactFraction at(ExactFraction alpha) { return constant.add(slope.multiply(alpha)); }
  }
  private record Weight(Linear lower, Linear upper) {
    Cut cut(ExactFraction alpha) { return new Cut(lower.at(alpha),upper.at(alpha)); }
  }
  private record Quantities(List<Integer> path, MediationPolicy.QuantityContext context,
                            List<Integer> positions, List<Weight> left, List<Weight> right) {}
  private static final class Work {
    int pairs;
    void pair() { if (++pairs > MAX_PAIR_CHECKS) throw new UnsupportedOperationException("Quantity comparison exceeds 65536 pair checks"); }
  }
  private FuzzyMediation() {}

  static MediationResult compare(CoverPattern left, CoverPattern right, PatternRule leftRule, PatternRule rightRule,
                                  PatternSemantics semantics, MediationPolicy policy, PatternComparison exact) {
    var scopes = new ArrayList<MediationResult.Scope>();
    String leftCode = leftRule == null ? left.canonical() : PatternRuleYaml.canonical(leftRule);
    String rightCode = rightRule == null ? right.canonical() : PatternRuleYaml.canonical(rightRule);
    if (!policy.enabled()) return unavailable(MediationResult.Status.DISABLED,exact,policy,semantics,leftCode,rightCode,scopes,"Fuzzy mediation is disabled for this bridge");
    try {
      if (left.marks() != 0 || right.marks() != 0) throw new UnsupportedOperationException("Root marks have no enclosing quantity context");
      PatternComparison structural = leftRule == null
          ? PatternComparison.compare(withoutMarks(left),withoutMarks(right),semantics)
          : RuleComparison.compare(withoutMarks(leftRule),withoutMarks(rightRule),semantics);
      if (structural.status() == PatternComparison.Status.UNKNOWN)
        throw new UnsupportedOperationException("Ordered structure, criteria, participants or profiles lack a proven semantic direction");
      var quantities = new ArrayList<Quantities>(); var work = new Work();
      collect(left,right,List.of(),leftRule,rightRule,semantics,policy,quantities,work);
      boolean lr = leftRefines(structural), rl = rightRefines(structural);
      boolean alphaLR = lr, alphaRL = rl;
      ExactFraction lower = ONE, upper = ONE;
      boolean sufficient = true;
      ExactFraction threshold = of(policy.overlapThreshold()), alpha = of(policy.admissionAlpha());
      for (var q : quantities) {
        var bounds = possibility(q,policy.steps());
        if (of(bounds.lower()).compareTo(lower) < 0) lower = of(bounds.lower());
        if (of(bounds.upper()).compareTo(upper) < 0) upper = of(bounds.upper());
        boolean ab = contained(q.left(),q.right(),null,work), ba = contained(q.right(),q.left(),null,work);
        lr &= ab; rl &= ba;
        if (policy.admission() == MediationPolicy.Admission.ALPHA_CUT) {
          alphaLR &= contained(q.left(),q.right(),alpha,work); alphaRL &= contained(q.right(),q.left(),alpha,work);
        }
        sufficient &= feasible(q,threshold);
        scopes.add(new MediationResult.Scope(q.path(),q.context(),q.positions(),bounds,ab,ba));
      }
      boolean admittedLR = sufficient && (policy.admission() == MediationPolicy.Admission.PROVEN_ONLY ? lr : alphaLR);
      boolean admittedRL = sufficient && (policy.admission() == MediationPolicy.Admission.PROVEN_ONLY ? rl : alphaRL);
      return new MediationResult(MediationResult.Status.DEFINED,new MediationResult.Bounds(lower.decimal(),upper.decimal()),
          sufficient ? MediationResult.OverlapDecision.SUFFICIENT : MediationResult.OverlapDecision.LOW,
          lr,rl,admittedLR,admittedRL,exact,policy,policy.fingerprint(),semantics.worldview(),leftCode,rightCode,scopes,
          List.of("Joint projective alpha-cut feasibility; nested independent scopes aggregate by minimum",
              "Threshold decisions are exact; reported possibility enclosure width is at most 2^-" + policy.steps(),
              policy.admission() == MediationPolicy.Admission.PROVEN_ONLY ? "Admission requires containment at every alpha in [0,1]"
                  : "Operational admission requires candidate alpha-cut containment at alpha=" + policy.admissionAlpha() + "; this is not full fuzzy entailment"));
    } catch (UnsupportedOperationException e) {
      return unavailable(MediationResult.Status.UNKNOWN,exact,policy,semantics,leftCode,rightCode,scopes,e.getMessage());
    }
  }
  private static MediationResult unavailable(MediationResult.Status status,PatternComparison exact,MediationPolicy policy,
                                              PatternSemantics semantics,String left,String right,List<MediationResult.Scope> scopes,String reason) {
    return new MediationResult(status,null,MediationResult.OverlapDecision.UNASSESSED,leftRefines(exact),rightRefines(exact),
        leftRefines(exact),rightRefines(exact),exact,policy,policy.fingerprint(),semantics.worldview(),left,right,scopes,List.of(reason));
  }
  private static boolean leftRefines(PatternComparison c) {
    return c.status() == PatternComparison.Status.EQUIVALENT || c.status() == PatternComparison.Status.LEFT_REFINES_RIGHT;
  }
  private static boolean rightRefines(PatternComparison c) {
    return c.status() == PatternComparison.Status.EQUIVALENT || c.status() == PatternComparison.Status.RIGHT_REFINES_LEFT;
  }
  private static CoverPattern withoutMarks(CoverPattern p) {
    return new CoverPattern(p.concept(),0,p.arguments().stream().map(FuzzyMediation::withoutMarks).toList());
  }
  private static PatternRule withoutMarks(PatternRule r) {
    return new PatternRule(2,withoutMarks(PatternParser.parse(r.composition())).canonical(),r.participants(),r.profiles(),r.criteria(),r.provenance());
  }
  private static CoverPattern withoutRootMarks(CoverPattern p) { return new CoverPattern(p.concept(),0,p.arguments()); }

  private static void collect(CoverPattern a,CoverPattern b,List<Integer> path,PatternRule ar,PatternRule br,
                               PatternSemantics semantics,MediationPolicy policy,List<Quantities> result,Work work) {
    if (!a.isConfiguration()) return;
    var ai = marked(a); var bi = marked(b);
    if (!ai.isEmpty() || !bi.isEmpty()) {
      independent(a,ai,semantics,path,work); independent(b,bi,semantics,path,work);
      if (ai.isEmpty() || bi.isEmpty()) {
        // No ratio on one side: universal quantity domain, not an invented equal-weight vector.
        var context = context(a,b,path,ar,br,policy);
        var positions = ai.isEmpty() ? bi : ai;
        result.add(new Quantities(path,context,positions,weights(a,ai,policy),weights(b,bi,policy)));
      } else {
        if (!ai.equals(bi)) throw new UnsupportedOperationException("Different marked-position sets at " + path + " require a more general constraint solver");
        var context = context(a,b,path,ar,br,policy);
        for (int i : ai) {
          work.pair();
          if (PatternComparison.compare(withoutRootMarks(a.arguments().get(i)),withoutRootMarks(b.arguments().get(i)),semantics).status()
              != PatternComparison.Status.EQUIVALENT)
            throw new UnsupportedOperationException("Quantified participant differs at " + path + "/" + i + "; aggregation is not inferred");
        }
        result.add(new Quantities(path,context,ai,weights(a,ai,policy),weights(b,bi,policy)));
      }
    }
    for (int i = 0; i < a.arguments().size(); i++) {
      var next = new ArrayList<>(path); next.add(i);
      collect(a.arguments().get(i),b.arguments().get(i),List.copyOf(next),ar,br,semantics,policy,result,work);
    }
  }
  private static void independent(CoverPattern node,List<Integer> positions,PatternSemantics semantics,List<Integer> path,Work work) {
    for (int i = 0; i < positions.size(); i++) for (int j = i+1; j < positions.size(); j++) {
      work.pair();
      var x = node.arguments().get(positions.get(i)); var y = node.arguments().get(positions.get(j));
      if (x.isConfiguration() && y.isConfiguration()) {
        if (PatternComparison.compare(withoutRootMarks(x),withoutRootMarks(y),semantics).status() == PatternComparison.Status.EQUIVALENT)
          throw new UnsupportedOperationException("Repeated equivalent quantified configurations at " + path);
      } else if (!x.isConfiguration() && !y.isConfiguration()
          && semantics.relate(x.concept(),y.concept()) != PatternSemantics.Relation.UNKNOWN)
        throw new UnsupportedOperationException("Equivalent or taxonomically coupled quantified identities at " + path);
    }
  }
  /** A single marked argument places no relative constraint on the domain. */
  private static List<Integer> marked(CoverPattern p) {
    var result = new ArrayList<Integer>();
    for (int i = 0; i < p.arguments().size(); i++) if (p.arguments().get(i).marks() > 0) result.add(i);
    return result.size() >= 2 ? List.copyOf(result) : List.of();
  }
  private static MediationPolicy.QuantityContext context(CoverPattern a,CoverPattern b,List<Integer> path,
                                                          PatternRule ar,PatternRule br,MediationPolicy policy) {
    var x = policy.configurations().get(a.concept()); var y = policy.configurations().get(b.concept());
    if (x == null || !x.equals(y)) throw new UnsupportedOperationException("Missing or different quantity contexts at " + path);
    if (ar != null) for (var rule : List.of(ar,br)) {
      var profile = rule.profiles().stream().filter(p -> p.path().equals(path)).findFirst();
      if (profile.isPresent() && (!profile.get().interpretation().equals(x.interpretation()) || profile.get().measure() != x.measure()
          || !profile.get().support().equals(x.support())))
        throw new UnsupportedOperationException("Rule profile and configured quantity context disagree at " + path);
    }
    return x;
  }
  private static List<Weight> weights(CoverPattern node,List<Integer> positions,MediationPolicy policy) {
    return positions.stream().map(i -> weight(node.arguments().get(i).marks(),policy)).toList();
  }
  private static Weight weight(int n,MediationPolicy policy) {
    var center = of(n); var width = of(policy.spread());
    if (policy.spreadMode() == MediationPolicy.SpreadMode.RELATIVE) width = width.multiply(center);
    return new Weight(new Linear(center.subtract(width),width),new Linear(center.add(width),ZERO.subtract(width)));
  }
  /** One common positive scale must satisfy every aligned component, retaining shared-variable dependence. */
  private static boolean feasible(Quantities q,ExactFraction alpha) {
    if (q.left().isEmpty() || q.right().isEmpty()) return true;
    ExactFraction lower = ZERO, upper = null;
    for (int i = 0; i < q.left().size(); i++) {
      Cut a = q.left().get(i).cut(alpha), b = q.right().get(i).cut(alpha);
      var l = a.lower().divide(b.upper()); var u = a.upper().divide(b.lower());
      if (l.compareTo(lower) > 0) lower = l;
      if (upper == null || u.compareTo(upper) < 0) upper = u;
      if (lower.compareTo(upper) > 0) return false;
    }
    return true;
  }
  private static MediationResult.Bounds possibility(Quantities q,int steps) {
    if (!feasible(q,ZERO)) return new MediationResult.Bounds(java.math.BigDecimal.ZERO,java.math.BigDecimal.ZERO);
    if (feasible(q,ONE)) return new MediationResult.Bounds(java.math.BigDecimal.ONE,java.math.BigDecimal.ONE);
    var low = ZERO; var high = ONE;
    for (int i = 0; i < steps; i++) { var mid = low.add(high).divide(TWO); if (feasible(q,mid)) low = mid; else high = mid; }
    return new MediationResult.Bounds(low.decimal(),high.decimal());
  }
  /** Projective box inclusion reduces to all cross-component upper/lower ratio bounds. */
  private static boolean contained(List<Weight> candidate,List<Weight> target,ExactFraction alpha,Work work) {
    if (target.isEmpty()) return true;
    if (candidate.isEmpty()) return false;
    for (int i = 0; i < candidate.size(); i++) for (int j = 0; j < candidate.size(); j++) if (i != j) {
      work.pair();
      if (alpha != null) {
        if (candidate.get(i).upper().at(alpha).multiply(target.get(j).lower().at(alpha))
            .compareTo(target.get(i).upper().at(alpha).multiply(candidate.get(j).lower().at(alpha))) > 0) return false;
      } else if (!nonpositive(candidate.get(i).upper(),target.get(j).lower(),target.get(i).upper(),candidate.get(j).lower())) return false;
    }
    return true;
  }
  /** Exact maximum of (a*b-c*d)(alpha), a quadratic, over the entire closed unit interval. */
  private static boolean nonpositive(Linear a,Linear b,Linear c,Linear d) {
    var constant = a.constant().multiply(b.constant()).subtract(c.constant().multiply(d.constant()));
    var linear = a.constant().multiply(b.slope()).add(a.slope().multiply(b.constant()))
        .subtract(c.constant().multiply(d.slope())).subtract(c.slope().multiply(d.constant()));
    var quadratic = a.slope().multiply(b.slope()).subtract(c.slope().multiply(d.slope()));
    if (constant.compareTo(ZERO) > 0 || constant.add(linear).add(quadratic).compareTo(ZERO) > 0) return false;
    if (quadratic.compareTo(ZERO) < 0) {
      var vertex = ZERO.subtract(linear).divide(TWO.multiply(quadratic));
      if (vertex.compareTo(ZERO) > 0 && vertex.compareTo(ONE) < 0
          && constant.add(linear.multiply(vertex)).add(quadratic.multiply(vertex).multiply(vertex)).compareTo(ZERO) > 0) return false;
    }
    return true;
  }
}
