# Evolving ecosystem patterns: design proposal

This document records design hints from the [GET](GET_DRAFT.md), [EUNIS](EUNIS_DRAFT.md) and
[WEED/workbook assessment](ECOSYSTEM_INTEROPERABILITY.md). It incorporates the requirement that
configurations may express temporal consequentiality and quantitative specifications may describe
relative time spans. It is a proposal, not a new accepted numeric grammar or an implemented evaluator.
See also the [fuzzy mediation notes](MEDIATION.md).

An initial typed-envelope implementation now exists; see [TYPED_RULES.md](TYPED_RULES.md) for the
accepted schema, six invented GET/EUNIS examples, implemented checks and remaining obligations.
The sections below retain the broader design rationale rather than specifying every implemented key.

The companion [pattern elements proposal](PATTERN_ELEMENTS_PROPOSAL.md) maps qualifications,
observable constraints, participant relationships, occurrent criteria and logical groups to the
current k.LAB semantic contract, including bearer validation and the normal Authority identity API.

## Retain generic configurations and make their interpretation explicit

The current grammar already supports invented categorical facet configurations. For example:

```text
ecosystem.seed:Ecosystem(
  physical:HorizontalArrangement(ecosystem.seed:Beech),
  ecosystem.seed:Substrate(ecosystem.seed:AcidSoil)
)
```

The prototype preserves a soil distinction without equating a soil quality to a tree species.
A later worldview can supply validated identities, bearers and observable constraints. It need not
adopt these experimental names. Grouping all non-cover characteristics into a flat list of required
leaves would lose which observable, bearer and extent each characteristic concerns.

Give each configuration a versioned semantic profile describing its argument roles, ordering,
spatial/temporal relation, measure associated with quantitative marks and any observation requirements.
Distinguish a spatial arrangement, a set of categorical characteristics, a temporal succession and a
causal-process model. An ordered parser tree alone cannot establish adjacency, causation or a clock.
Worldview equivalence of functor concepts also does not automatically establish equality of their
parameter/role interpretation.

## Temporal consequentiality and relative duration

A temporal configuration may describe one state or configuration following another. For hydroperiods,
it may additionally describe a recurring cycle. These are different from coexistence, partial temporal
overlap and a proven causal relation. If a particular functor denotes causal succession, that must be
part of its declared semantics rather than inferred from the comma order alone.

The following valid prototype syntax exercises temporal order and marks on nested configurations:

```text
ecosystem.seed:HydroperiodCycle(
  +++physical:HorizontalArrangement(ecosystem.seed:InundatedPhase),
  +physical:HorizontalArrangement(ecosystem.seed:ExposedPhase)
)
```

Under a profile binding the immediate children of `HydroperiodCycle` to **duration**, the central
interpretation is approximately three units of inundated-phase duration to one unit of exposed-phase
duration. These are relative time spans, not area fractions. The example is invented for testing;
it is not an asserted GET/EUNIS ratio, a nine-month/three-month annual cycle or an implemented duration
calculation. The actual seasonal-marsh pilot leaves both phases unmarked.

An analogous spatial functor could bind its child marks to covered area. A nested spatial pattern
can use its own marks for area while its parent temporal functor uses marks on that entire child for
duration. Scope and measure follow the enclosing configuration, so unlike quantities must not be
compared merely because they use the same plus syntax.

A quantitative profile should specify:

- The measure: area, volume, duration, event count, biomass or another named observable.
- The denominator/reference support: one identified cycle, a fixed time window, a particular object,
  a region, or just pairwise ratios between the marked arguments.
- Whether arguments exhaust that support. Unspecified phases/components must not silently acquire
  the remainder, zero, weight one or equal duration.
- Temporal relation: strict before, meets, overlap, recurrence, optional gaps, ordering of a cycle's
  phases and whether the cycle has an anchored start. Cyclic rotations need not be distinct once
  formal semantics are specified; the current syntax serializer preserves the written start/order.
- Whether duration means total occupancy, individual event length, mean event length or another
  statistic. Event frequency, return interval and occupancy fraction are not interchangeable.
- Calendar and sampling rules: observation window, calendar, irregular sampling, censoring, treatment
  of repeated/overlapping occurrences and spatially asynchronous phases.
- Absolute constraints such as minimum months inundated, alongside relative duration ratios when
  needed. Relative shares cannot determine an absolute period without an additional clock.

For example, two short floods and one long flood can have the same total occupancy while differing
in frequency and event duration. Seasonal flow variability may occur without a zero-flow phase.
An absence of a fire record is neither a zero duration nor a guarantee of no disturbance regime.

The existing parser and tests preserve temporal structure and marks. The current structural
comparator does not evaluate a duration measure, recognize cyclic equivalence or infer causation.
Those conclusions require the configuration profile and evaluators.

## Add typed observation constraints without hiding their roles

Categorical functors are useful prototypes. Quantitative and logical criteria need a rule envelope
or a carefully extended AST, rather than encoding every bound into a new nominal identity. A future
constraint should be attached to an explicit pattern node or role and identify:

| Field | Needed distinction |
| --- | --- |
| Observable and bearer | Soil pH, water salinity, tree canopy cover and sea-ice duration concern different subjects. |
| Comparison | Equality, open/closed intervals, inequalities, categorical membership, existence or absence. |
| Value and unit | Absolute salinity versus practical salinity, area versus canopy cover, rates versus totals. |
| Spatial support | Plot, habitat patch, catchment, depth stratum, water-column volume or mixed map cell. |
| Temporal support | Instant, season, year, multi-year regime, event duration, recurrence or cycle occupancy. |
| Aggregation | Mean, minimum, maximum, total, quantile, proportion or temporal sequence relation. |
| Modality | Necessary, diagnostic combination, typical, optional, explanatory or explicitly excluded. |
| Logical grouping | All-of, any-of, alternatives, cardinality or conditional applicability. |
| Evidence | Source edition/section, reviewer, local interpretation and measurement uncertainty. |

In particular, one alternative from a list of vents, seeps and organic falls must not become three
mandatory co-occurring components. A typical dominant species is not necessarily a required species.
Exclusion/negation needs an explicit open-world and evidence policy; missing data must not satisfy it.
No threshold examples from the exploratory workbook should enter this model without their original
meaning, units, inclusivity, aggregation and measurement support being reviewed.

A future rule representation could have separate `composition`, `constraints`, `logic`,
`configurationProfiles` and `provenance` parts. These names are conceptual headings, **not currently
accepted YAML catalog keys or authority parameters**. Keep syntax migration, canonicalization and
backward compatibility explicit before implementing such an envelope.

## Keep definition, interpretation, evidence and decision policy separate

The national or international source definition records its own criteria, including any crisp bounds.
A reviewed bridge may bind those criteria to worldview observables and configuration profiles.
Observations carry their own spatial/temporal support, uncertainty and missingness. A decision policy
then controls how uncertain evidence is reconciled and when a compatibility/classification decision
is reported. Adjusting a fuzzy overlap threshold must not silently revise the official class definition.

Tolerances, fuzzy profiles and decision thresholds remain **per authority configuration**, as requested.
Persist an immutable policy snapshot and include it in result provenance/cache fingerprints. Share
common policies across realms only when the measure and interpretation agree. Salinity, duration and
area may require different uncertainty profiles even inside the same bridge.

Semantic interpretation also matters to identity. Changing a weight from duration to area changes
meaning and should require a distinct configuration concept/profile or bridge semantics. Merely
changing an uncertainty tolerance can leave the same canonical pattern identity while changing its
assessment result. Full canonical rule identity must eventually include the actual defining constraints,
not only the composition expression. Preserve external source-class records independently even when
two reviewed definitions denote equivalent semantics.

## Evidence and future model generation

Infer observables and least generic bearers from reviewed definitions and the bound worldview, then
bind available resources to those dependencies. Do not put a STAC URL, local resource URN or a sensor
availability check into the ecosystem's definition. EO-compatible measurements can supply evidence;
causal mechanisms, management history, diagnostic floristics and underground processes may require
other observations or expert evidence. Report unsupported dependencies rather than treating them as
false criteria or zero compatibility.

The eventual `@Library` should expose explicit evaluators for arrangement, temporal relations,
relative quantities and typed constraints. k.IM namespace generation can then state dependencies,
measurement support, transformation and missing-data behavior. Crosswalk evaluation and observing an
ecosystem occurrence are related tasks but not the same API operation.

## A staged implementation path

1. Review the invented categorical vocabulary and select positive, negative and ambiguous examples
   for the pilot classes. Keep source modalities and regional applicability on each criterion.
2. Specify configuration profiles for spatial composition and temporal succession/cycles, including
   their measure roles and relative-duration semantics. No new token is required to begin that work.
3. Define a typed constraint/logic AST and its canonicalization, with explicit units, support and
   source-definition provenance. Provide a migration path from the current expression-only identity.
4. Specify fuzzy quantity arithmetic and compatibility measures against the scoped profiles. Test
   dimensional consistency, shared-variable feasibility, interval endpoints, cycle behavior and missing data.
5. Add evaluators, result explanations and authenticated service routes; only then generate classifiers
   and compare their observations with reviewed reference cases.

This keeps the generic pattern language useful now while giving ecosystem drivers, national
interpretations and temporal consequentiality room to acquire precise, testable meaning.
