# Mediation design notes (not an implemented specification)

The mediation engine must distinguish semantic evidence, uncertain abundance and classification
policy. Current `comparePatterns` performs conservative structural checks only. This document
records a possible design and decisions that must be made before implementing fuzzy arithmetic.

Typed rules now add conservative logical/interval entailment through the normal shared
`Authority.semanticDistance(configurationId, targetId, candidateId)` hook. See
[typed rules](TYPED_RULES.md) and the [shared contract](../../klab-services/docs/AUTHORITY_DISTANCE.md).
Candidate entailment is required for a nonnegative distance; mere overlap is insufficient. The current
provider reports UNKNOWN rather than inventing fuzzy scores or claiming disjointness.

Relative quantity is not restricted to abundance: temporal configurations may assign marks to
relative phase durations. A configuration profile must bind the measure and reference support,
including cycles, events, gaps and sampling. The [ecosystem pattern proposal](ECOSYSTEM_PATTERN_EVOLUTION.md)
develops these requirements. Ratios from different measures or supports cannot be compared merely
because they use the same integer marks.

## Configuration ownership and reproducibility

Tolerances and thresholds belong to each **Authority.ConfigurationRequest / bridge**. Two bindings
of this provider may use different policies, even inside one worldview. No component-global mutable
tolerance should affect previously configured bridges. Store an immutable validated policy snapshot
with each bridge, and include it in any future mediation cache fingerprint. Unknown fields, invalid
ranges and unsupported policy versions must fail configuration rather than select a guessed default.

A future policy schema should identify at least:

- A versioned arithmetic/membership profile and its interpretation of a plus count.
- Relative and/or absolute uncertainty parameters, with units, ranges and documented defaults.
- The treatment of common scaling (3:1 versus 6:2), and the effect of mark count on uncertainty.
- The alpha-cut schedule or analytic comparison method, and the chosen possibility/necessity measure.
- Separate thresholds for possible overlap, strong compatibility and a classification decision.
- The aggregation operator across sibling constraints and nested configurations.
- Identity overlap, bearer aggregation, duplicate arguments and missing-evidence policies.

These are proposed fields of a specification, not accepted parameter names. The implementation
currently rejects mediation parameters. A later implementation should provide a complete example
configuration, formulas and boundary examples for every parameter, explain threshold inclusivity,
and disclose the cost and numerical precision of the selected profile.

A result should report the exact policy version/parameters or stable fingerprint, worldview
revision, catalog/source versions, matched positions, semantic evidence, abundance evidence and
unresolved conditions. Changing mediation policy must not change the pattern's canonical identity;
it may change its compatibility results. Caches must include the policy and semantic revision.

## Candidate numerical representation

Model each positive integer mark count n as a positive fuzzy weight W(n), rather than immediately
normalizing it into a percentage. A triangular or trapezoidal membership function is one candidate,
not a chosen default. At alpha > 0, its alpha-cut is a positive closed interval [l_n, u_n]. For two
positive sibling weights, a candidate ratio enclosure is:

```text
W(i) / W(j) at alpha: [l_i / u_j, u_i / l_j]
```

Require positive denominators; separately specify support and alpha=0 behavior. This interval
calculation encloses possible ratios. It does not by itself define fuzzy equivalence, a distance,
or an entailment score. Shared variables make sibling ratios dependent; independently overlapping
pairwise intervals do not prove that all constraints admit one common assignment. Normalizing fuzzy
weights to sum to one introduces the same dependency issue. A sound solver must retain these shared
variables or document a conservative approximation.

An unmarked argument supplies no abundance constraint. It must never become W(1), zero, or an equal
share. Constraints compare only explicitly marked siblings under the same enclosing configuration.
Root marks have no parent comparison scope. Nesting does not transport ratios across configurations.
Multiple identities can overlap taxonomically: abundance of a species and abundance of its genus
cannot automatically be added or independently compared as disjoint classes. The bearer, measurement
and aggregation rules need specification alongside the arithmetic.

An example uncertainty function like W(n) = triangular(n-d, n, n+d) would make common scaling alter
relative uncertainty. A profile based on relative spread could behave differently. Therefore current
comparison does not declare 3:1 and 6:2 fuzzy-equivalent merely because their central ratios agree.
Do not add either profile as an implicit policy before this distinction has been decided.

## Java implementation options

My recommendation is a small domain-specific fuzzy-number kernel behind an explicit policy interface.
[Apache Commons Numbers Fraction](https://commons.apache.org/proper/commons-numbers/userguide/index.html)
provides `Fraction` and arbitrary-precision `BigFraction`. The latter is a candidate for exact rational
alpha-cut endpoints and central ratios. It supplies rational arithmetic, **not fuzzy arithmetic**;
membership functions, interval bounds, dependency handling and comparison measures remain our code.
No dependency has been added pending the specification. Floating-point implementations are another
option, but must specify rounding and demonstrate conservative bounds near decision thresholds.

[jFuzzyLogic](https://jfuzzylogic.sourceforge.net/html/manual.html) implements FCL and fuzzy controller
inference. [jfuzzylite](https://fuzzylite.com/) also targets fuzzy controllers, with membership terms,
norms and defuzzification. These are relevant if classification eventually uses linguistic IF/THEN
rules; their documented focus does not make them a direct choice for dependent fuzzy ratio arithmetic.
I would not introduce a controller engine solely to compare relative cover abundance.

## Specification and verification work

Define the denotation of ordered configuration arguments and what constitutes a shared occurrence;
resolve how taxonomic subsumption, arrangement equivalence and spatial/temporal extent interact.
Then specify fuzzy compatibility separately from logical refinement or contradiction. A low overlap
score must not be presented as a proven semantic incompatibility. Preserve UNKNOWN when evidence
is missing, and distinguish it from measured intermediate compatibility.

Before accepting an engine, use reviewed examples and mathematical properties: symmetry of
compatibility; directionality of entailment; monotonicity under relaxing a constraint; the selected
scaling law; independence of nested scopes; unspecified abundance; zero-denominator boundaries;
shared-variable feasibility; overlapping identities; and threshold-edge numerical behavior.
Calibration against actual reviewed legend translations is needed before choosing defaults.
