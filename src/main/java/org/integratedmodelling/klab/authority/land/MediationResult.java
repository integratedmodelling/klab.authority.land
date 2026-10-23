package org.integratedmodelling.klab.authority.land;

import java.math.BigDecimal;
import java.util.List;

/**
 * Quantitative overlap is distinct from entailment and policy admission; UNKNOWN is never score
 * zero.
 */
public record MediationResult(
    Status status,
    Bounds overlap,
    OverlapDecision overlapDecision,
    boolean leftEntailsRight,
    boolean rightEntailsLeft,
    boolean leftAdmittedForRight,
    boolean rightAdmittedForLeft,
    PatternComparison exactComparison,
    MediationPolicy policy,
    String policyFingerprint,
    String worldview,
    String leftIdentity,
    String rightIdentity,
    List<Scope> scopes,
    List<String> explanations) {
  public enum Status {
    DEFINED,
    UNKNOWN,
    DISABLED
  }

  public enum OverlapDecision {
    SUFFICIENT,
    LOW,
    UNASSESSED
  }

  public record Bounds(BigDecimal lower, BigDecimal upper) {
    public Bounds {
      if (lower.signum() < 0 || upper.compareTo(BigDecimal.ONE) > 0 || lower.compareTo(upper) > 0)
        throw new IllegalArgumentException("Invalid possibility bounds");
    }
  }

  public record Scope(
      List<Integer> path,
      MediationPolicy.QuantityContext context,
      List<Integer> markedPositions,
      Bounds overlap,
      boolean leftEntailsRight,
      boolean rightEntailsLeft) {
    public Scope {
      path = List.copyOf(path);
      markedPositions = List.copyOf(markedPositions);
    }
  }

  public MediationResult {
    scopes = List.copyOf(scopes);
    explanations = List.copyOf(explanations);
  }
}
