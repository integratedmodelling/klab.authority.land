# Initial mediation specification: fuzzy-ratios-1

The [README](../README.md#fuzzy-mediation) provides configuration, parameter bounds, defaults,
examples and API usage. This document specifies the implemented mathematical model. It replaces
the earlier design alternatives; this remains a relative-quantity solver rather than a general
ecosystem satisfiability or observation-classification engine.

## Separate exact implication, overlap and admission

`comparePatterns` and `compareIdentities` retain policy-independent structural/semantic checks.
Typed-rule intervals remain crisp. `mediate` adds quantitative possibility, full fuzzy containment
and operational admission. `Authority.semanticDistance` exposes admitted candidates through normal
Reasoner matching. Overlap alone never admits a candidate. LOW overlap is not ecological disjointness;
UNKNOWN is not a numeric zero. In the integer Reasoner result, -1 means no admitted match.

## Positive triangular weights and projective boxes

For plus count n > 0 and configured 0 <= beta < 1:

```text
d(n) = beta*n       RELATIVE
d(n) = beta         ABSOLUTE
L_n(alpha) = n - d(n)*(1-alpha)
U_n(alpha) = n + d(n)*(1-alpha),  0 <= alpha <= 1
```

The mode is n, alpha=0 uses the closed support, and alpha=1 gives the singleton mode. Beta=0 is
crisp. Every denominator is strictly positive, including n=1 at alpha=0. Wider absolute supports
need another version. Relative spread is invariant under common scaling; absolute spread gives
larger counts greater relative precision. This choice belongs to explicit bridge policy.

For the marked positions M, the quantity vector q is in a configuration's alpha-cut if one positive
scale s satisfies s*L_i <= q_i <= s*U_i for every i in M. There is no automatic sum-to-one assumption.
Unmarked arguments supply no ratio. A single mark supplies no relative constraint; an unquantified
scope denotes the unrestricted positive domain, not equal shares.

Both quantified sides must use identical marked-position sets and equivalent quantified participants.
Known taxonomic coupling and duplicate equivalent quantities are rejected. Unknown semantic relation
does not prove independence: `independentArguments: true` is an explicit reviewed assertion that no
additional local coupling/aggregation constraints need to be solved. Quantified inner definitions
cannot change without additional aggregation semantics.

## Joint feasibility and possibility

The two projective cuts share an assignment exactly when one common scale ratio r satisfies:

```text
max_i (L_left_i / U_right_i) <= r <= min_i (U_left_i / L_right_i)
Pi(left,right) = sup { alpha in [0,1] : those joint cuts are feasible }
```

Every marked component participates in this intersection. Different scale values are not selected
independently for different pairs. Feasibility is monotone as alpha increases and cuts shrink.
Disjoint supports give Pi=0; shared modes give Pi=1. Otherwise exact bisection returns dyadic endpoints
enclosing Pi with width at most 2^-steps. All arithmetic and decisions use reduced BigInteger fractions;
only terminating dyadic report endpoints become BigDecimal values.

The threshold is not compared to a rounded midpoint. Feasibility at the exact decimal
`overlapThreshold` decides Pi >= threshold, inclusively, even when that threshold is inside the
reported enclosure. The coefficient concerns only the relative-quantity subsystem under its stated
assumptions. It is not a joint satisfiability proof involving other observables or hidden correlations.

## Full-model containment and operational admission

For candidate C and target T, projective-box inclusion at one alpha is equivalent to all inequalities
for distinct marked positions i,j:

```text
U_C_i(alpha) / L_C_j(alpha) <= U_T_i(alpha) / L_T_j(alpha)
P_ij(alpha) = U_C_i(alpha)*L_T_j(alpha) - U_T_i(alpha)*L_C_j(alpha) <= 0
```

The target's common scale exists if and only if all pair bounds hold. Taking the candidate's extreme
ratio therefore gives an exact containment test for every candidate vector. P is quadratic. To prove
containment for all alpha in [0,1], check endpoints and, for a concave quadratic with an interior
vertex, its exact maximum at that vertex. Sampling a finite set of alpha cuts is not used as proof.
Cut inclusion at every alpha establishes membership inclusion for these normalized fuzzy models.

Rule implication also requires a proven semantic direction after removing marks, retaining all typed
criteria, participant/bearer declarations and profiles. Quantities cannot repair an unknown semantic
relationship. Absolute observable intervals are not given an invented fuzzy halo.

`PROVEN_ONLY` is the default admission policy. `ALPHA_CUT` deliberately chooses a weaker operational
contract: the candidate's `admissionAlpha` cut must be entirely contained in the target's cut at that
same alpha, and the possibility threshold must pass. Merely intersecting cuts is insufficient. This
may admit different central ratios without asserting full fuzzy entailment. Strict implications and
operational admission flags are reported separately. Changing alpha changes both cuts and is not a
general monotonic tolerance control; calibrate it separately from the possibility threshold.

Normal distance preserves exact equivalence/refinement as 0/1. Otherwise full fuzzy equivalence
returns 0; an admitted candidate receives ceil(100*(1-Pi_lower)), clamped to 1..100. Using the lower
bound makes rounding conservative. These are provider ranking units, not ontology edge counts.
Unsupported/unadmitted cases return UNKNOWN/-1. This engine does not claim INCOMPATIBLE.

## Scope, lifecycle and numerical limits

Independent nested scopes aggregate possibility by minimum. Each occurrence has its own measure,
support and interpretation; explicit rule profiles must match. Spatial and temporal quantities are
not moved between scopes. Written ordering is preserved. A duration ratio supplies no calendar,
recurrence period or causation. Constraints and data discovery remain separate operations.

Policies are immutable per-bridge snapshots. Invalid values, unknown fields/versions and incomplete
contexts fail configuration. A tolerance change does not rewrite canonical identity codes; a measure
change is a bridge interpretation change. Normal shared matching isolates distinct bridges. No result
cache is introduced, and semantic access retains its worldview/revision guard.

Results contain canonical input codes, worldview, policy snapshot/fingerprint, occurrence paths,
measure/support, marked positions, local enclosures and explanations. They do not capture a knowledge
revision number; source editions remain available in catalog metadata.

The budget permits 65536 alignment/coupling/inclusion pair checks. Feasibility and bisection are linear
in marked count per cut, additionally bounded by parser limits and 4..40 steps. Exceeding the pair
budget returns UNKNOWN with no aggregate score; any local scopes are partial evidence. The small
exact fraction kernel avoids a fuzzy-controller dependency while preserving shared-scale reasoning.

Tests cover analytic values, exact threshold edges inside coarse report bounds, scaling laws,
directionality, symmetry, uncertainty relaxation, whole sibling vectors, nesting, missing marks,
context mismatches, coupling, immutable policy isolation, budget exhaustion and CORINE/GET fixtures.
Community calibration remains necessary. Future versions may add other distributions, per-measure
uncertainty, differing partial variable sets, unit/support mediation, aggregation across taxa, coupled
absolute/relative constraints and full logical satisfiability.
