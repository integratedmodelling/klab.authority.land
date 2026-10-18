# WEED ecosystem interoperability: initial assessment

This assessment compares ecosystem typologies as multi-dimensional definitions, while retaining
national and regional meanings. It follows the experimental CORINE/IGBP work but does not assume
that a land-cover composition is a complete ecosystem definition. Separate source reviews are in
[GET](GET_DRAFT.md) and [EUNIS](EUNIS_DRAFT.md); the [pattern evolution proposal](ECOSYSTEM_PATTERN_EVOLUTION.md)
explains how the findings could guide the authority API.

## Scope and evidence

The [ESA WEED project](https://esa-worldecosystems.org/en/about-weed) targets terrestrial, freshwater
and coastal ecosystems up to the intertidal zone, supports GET, EUNIS and EU-ET, and seeks national
crosswalks. This change addresses GET/EUNIS and includes additional offshore examples to test a
future unified framework. It does not assess EU-ET or substitute one of these classifications for it.
A common model can span realms while preserving distinct marine, terrestrial and inland-water sources.

Source inventories cover all 110 English GET 2.1 functional groups in the maintained April-2026
export, 270 level-3 records from the EEA terrestrial-2021_1 workbook, 249 level-3 marine-2022 records,
and 21 legacy inland-water level-3 records. Exact editions, source rows and hashes are retained.
Runtime pilots cover 29 GET, 18 revised-terrestrial EUNIS, 14 marine EUNIS and 8 legacy-inland EUNIS
classes: **69 new aliases**. The full indexes are not completed semantic definitions or codelists.
The four deeper marine seagrass examples are level 4 according to the EEA table.

The supplied **Primitive characteristic annotation.xlsx** is exploratory team material, not an
authoritative taxonomy or specification. It was inspected read-only, including formula text and saved
cached values. It has 28 sheets, and includes Google Sheets import/function wrappers, cached errors
and provisional legacy k.LAB strings. No formulas, external imports, models, resource URLs or instructions
inside the workbook were executed. The source file was not edited. The exact digest and selected
cell references are recorded in [workbook provenance](data/workbook-review-provenance.json).
The original workbook is not copied into this repository.

## The dimensional structure suggested by the workbook

The hardcoded semantic-mapping sheet contains 46 distinct variable labels. They are useful evidence
of the team's intended distinctions, not a ready-made ontology. The current centralisation sheet's
populated source rows emphasize land/ground-layer dominance; other tabs carry much broader variables.
Counting the many error-populated formula rows as completed annotations would overstate progress.

| Dimension | Illustrative workbook evidence | Consequence for patterns and observations |
| --- | --- | --- |
| Composition and layering | `Template to organize centralisa!D3:F3`; `landcover_inputs!Y2:AH2` | Distinguish components, layers, coverage and nominal cover-class identities. |
| Hydrological regime and connectivity | `Semantic_mapping(hardcoded)` water occurrence, hydroform and managed-water variables | Separate flowing/standing, persistence, seasonal variability, ocean connection and management. |
| Climate and temporal support | `1.Observation_annotation!AC9:AD9`; climate-related variable rows | Means, totals, seasonal amplitudes and variability require a named window, aggregation and unit. |
| Substrate and chemistry | `classifications!B2:D5`; soil/fertility and salinity variables | Peat thickness, nitrogen, base saturation and salinity are different observables, not generic adjectives. |
| Disturbance regime | `Semantic_mapping(hardcoded)!A171:I171` | Frequency, recurrence, duration, severity and recent occurrence are different quantities. |
| Productivity and function | `classifications!B8:D8` | Flux, integrated annual production and functionally explanatory drivers cannot be interchanged. |
| Management and origin | `Semantic_mapping(hardcoded)!A2:I5` | Impoundment, aquaculture, cultivation and mining can define history/function rather than surface material. |
| Data and model implementation | Resource annotations, STAC variables, primary/secondary annotations | A dataset identifier is an implementation binding, not part of the ecosystem class's meaning. |

The workbook also motivates local observations, resource annotation and eventual model generation.
Those should be connected to a class definition through explicit observables and transformations;
replacing the class definition with the available data layer would make meaning depend on data availability.

## Specific review issues found in the exploratory workbook

These are review leads, not repairs or validated diagnoses of the authors' intended models:

- `Template to organize centralisa!B3:E3` describes a subglacial-lake/snow-or-ice row, while `AL3`
  contains a shrubland annotation. Check lookup alignment and the intended target column before
  treating that generated annotation as semantic evidence.
- `classifications!B8:D8` associates an annual-total productivity variable with a per-day annotation.
  Verify whether this is an intended conversion, a rate or an integrated annual quantity. Do not
  infer temporal aggregation from the asset name alone.
- `Semantic_mapping(hardcoded)!A171:I171` reduces a wildfire-periodicity case to presence/absence.
  That is insufficient to express a disturbance regime without an observation period and recurrence model.
- `1. Thresholds annotation!A3` contains adjacent lookup bands sharing endpoint 4. Check the legacy
  language's interval inclusivity and the intended criterion before using them as mutually exclusive bins.
- `status of biomes!F4` explicitly raises missing fire observations versus no fire. Keep unobserved,
  outside coverage, no-data, observed absence and incompatible evidence distinct.
- Legacy expressions include undeclared or incubation namespaces, incomplete concept strings and
  import wrappers. No current-worldview consistency is inferred from their presence in a cell.

These observations reinforce the user's instruction to treat the workbook as directional rather
than authoritative. They do not justify replacing its content or altering the attached file.

## What the two typologies compare

GET EFGs generalize functional organization and drivers at global scale. EUNIS often distinguishes
regional environmental settings and compositional assemblages at finer granularity. Neither is
adequately characterized by its class label, a single dominant taxon, a land-cover category or a level
number. Similar surface cover can support different hydrological regimes or trophic systems, while
similar functional systems can contain different national or regional species assemblages.

A proposed mediation result should therefore preserve several possible relationships:

| Relationship | What must be established |
| --- | --- |
| Equivalent definitions | Mutual entailment of all defining criteria under compatible measurement, spatial and temporal support. |
| Conditional refinement | A regional/national definition entails the global definition only under explicit conditions. |
| Partial overlap | Some realizations satisfy both, but neither definition contains all realizations of the other. |
| Part/whole or interface | Habitat components, water columns and interfaces are related without being equivalent ecosystem types. |
| Incompatible within a stated support | Joint realization is excluded by actual constraints on the same bearer/extent/time. |
| Unresolved | Missing definitions, unsupported measurements, typical-only evidence or unvalidated concept relationships prevent a decision. |

The current comparator implements none of the full multi-dimensional conclusions above merely from
prototype facet names. Its structural matches apply to the submitted patterns, not the original source
class definitions. The [16 comparison cases](data/ecosystem-comparison-cases.json) are review hypotheses
with required evidence and no runtime ontology assertions or official-crosswalk status.

## Preserving national peculiarities

Represent each national class as an independently versioned source definition with agency, code,
source edition, geographical support and diagnostic interpretation. Link it to one or more GET/EUNIS
candidates with conditional, reviewable assertions. Keep local species combinations, management
history, legal reporting scope and threshold conventions available even when they are not needed to
recognize the broader functional grouping. Do not make a national class disappear into a global alias.

An uncertainty or compatibility threshold may belong to the authority bridge's policy. A source
class's defining threshold must remain attributable to that source edition. If a national convention
changes a defining boundary, record a different profile or interpretation rather than silently tuning
it through a global mediation tolerance. The policy snapshot, class definition revision and evidence
must be reported separately in results.

A map can use an exclusive legend to allocate pixels even when the concepts overlap or refer to
nested ecological units. Allocation precedence is not an ontology disjointness axiom. Conversely,
one coarse cell may contain several ecological units whose relative area changes without a change
in each unit's ecosystem identity. Spatial grain and object delineation belong in the assessment.

## Prototype implementation and limits

The new `ecosystem.seed.kwv` uses experimental functors to preserve categorical facets and temporal
structure with the existing grammar. It is explicitly authorized invented vocabulary; no migration
of legacy workbook semantics is claimed. Loading it into a real worldview is not required by the
fixture tests and was not performed here. A test semantic adapter verifies declared roles only.

Four classpath YAML pilots are registered automatically and remain opt-in using `domain: ECOSYSTEM`.
The classpath reference inventories retain source metadata but are not exposed as 650 completed
pattern identities. Source and tentative-profile notes appear in normal codelist/identity descriptions.
The legacy CORINE/IGBP drafts continue to use the independent LULC domain.

No workbook repair, native Excel recalculation, model deployment, official crosswalk import,
automated taxonomy-code guessing, numeric fuzzy mediation, or actual-worldview validation was
performed. The useful next deliverable is a reviewed definition/constraint model and positive,
negative and ambiguous ecosystem examples, followed by evaluators and k.IM generation.

## Review-case index

These relationships are hypotheses or cautions. They are not automated comparator results.

| Case | GET | EUNIS source codes | Assessment lead |
| --- | --- | --- | --- |
| EC01 | T2.2 | eunis2021_terrestrial_pilot: T17, T18 | Regional beech forests may fit deciduous temperate forests, but soil and assemblage distinctions must remain. |
| EC02 | TF1.6 | eunis2021_terrestrial_pilot: Q11, Q12 | Raised and blanket bog morphology should not replace the broader functional bog description. |
| EC03 | TF1.7 | eunis2021_terrestrial_pilot: Q22, Q41 | Fen groupings share mineral-water context but differ in chemistry, flora and geographical support. |
| EC04 | TF1.3 | eunis2021_terrestrial_pilot: Q51 | A tall-helophyte bed is not automatically a permanently inundated functional marsh. |
| EC05 | T4.5 | eunis2021_terrestrial_pilot: R1A, R1M, R21 | Similar grass cover can retain different flora, substrate and management meanings. |
| EC06 | T7.1 | eunis2021_terrestrial_pilot: V11 | An intensive unmixed crop class might fit annual cropland when its crop cycle and management agree. |
| EC07 | F1.1 | eunis2012_inland_pilot: C2.2 | Fast perennial water is not sufficient to infer upland stream context. |
| EC08 | F1.4 | eunis2012_inland_pilot: C2.2, C2.5 | Seasonal variation can occur in perennial as well as temporary upland streams. |
| EC09 | F2.1 | eunis2012_inland_pilot: C1.1, C1.2, C1.3 | GET lake size/permanence and EUNIS trophic status partition different dimensions. |
| EC10 | F2.6 | eunis2012_inland_pilot: C1.5 | Saline, brackish and soda lake categories need different chemistry and connectivity tests. |
| EC11 | MT1.1 | eunis2022_marine_pilot: MA12, MA15 | Regional littoral rock may refine a global rocky-shoreline context without changing the underlying function. |
| EC12 | M1.1 | eunis2022_marine_pilot: MA522, MA623, MB522, MB252 | Regional seagrass community classes are candidates, not automatic synonyms for the global functional group. |
| EC13 | M1.2 | eunis2022_marine_pilot: MB12, MB15 | A rock class can contain kelp ecosystems but need not be a kelp forest. |
| EC14 | M1.8 | eunis2022_marine_pilot: MA62 | The selected EUNIS example is littoral mud whereas GET M1.8 is subtidal; mud identity alone cannot establish equivalence. |
| EC15 | M2.1 | eunis2022_marine_pilot: MH12 | Neuston is an air-sea-interface community, not the complete epipelagic water-column system. |
| EC16 | M2.5 | eunis2022_marine_pilot: MJ11 | Arctic sea-ice communities may be a regional instance of global sea-ice functioning. |
