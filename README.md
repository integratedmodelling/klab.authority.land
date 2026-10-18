# Earth cover authority

`klab.authority.land` describes earth-cover and ecosystem classes through ordered patterns of
worldview configurations and identities. Its purpose is to make verbal classification legends
explicit enough to compare, mediate, and review. Users normally encounter curated codelist
names rather than the expressions themselves.

This is an initial implementation of the pattern language, semantic validation adapter, standard
Authority provider, versioned catalog/codelist API, typed rule envelopes and conservative entailment.
The initial [fuzzy mediation engine](#fuzzy-mediation) compares relative quantities using positive
triangular weights, exact rational alpha cuts and an immutable policy for each authority bridge.
It supports spatial cover and relative phase duration when their measure and support are defined.
The [mediation specification](docs/MEDIATION.md) gives the mathematics and remaining limitations.
Catalog annotation identities now preserve catalog ID, pinned revision, source edition and exact
source code independently of optional patterns/rules. See [entry identities and notation](docs/ENTRY_IDENTITIES.md)
for verification/review status, schema 3, conservative matching and migration from old pattern-valued
codelists. Two tentative classpath YAML catalogs now supply all 44 CORINE level-3 aliases and all 17
MODIS MCD12Q1 IGBP aliases. These are deliberately incomplete semantic translations for testing,
not official implementations. See [Seed catalogs and maintenance](docs/SEED_CATALOGS.md) for their
sources, vocabulary proposal, known collisions, YAML schema and Reasoner UI working-list workflow.
The separate [CORINE attempt](docs/CORINE_DRAFT.md) and [IGBP attempt](docs/IGBP_DRAFT.md)
explain the choices and ambiguities for each classification and include full entry inventories.
Four additional ECOSYSTEM-domain pilots now cover 29 GET, 18 EUNIS revised-terrestrial,
14 EUNIS marine and 8 EUNIS legacy-inland classes. The [GET review](docs/GET_DRAFT.md),
[EUNIS review](docs/EUNIS_DRAFT.md), [WEED comparison](docs/ECOSYSTEM_INTEROPERABILITY.md) and
[pattern evolution proposal](docs/ECOSYSTEM_PATTERN_EVOLUTION.md) explain the partial coverage,
distinct source editions and prototype facet/temporal semantics. Complete reference inventories
are separate from runtime definitions. No LCCS or national-class translations are bundled yet.

A seventh opt-in catalog, `ecosystem_rules_pilot`, adds six invented typed-rule examples based on
GET/EUNIS review cases. The [typed rule guide](docs/TYPED_RULES.md) documents participant bindings,
observable intervals, alternatives/cardinality, qualifications, events/processes, relationships,
temporal profiles, validation obligations and reference evidence evaluation. LAND now implements
the shared Authority semantic-distance hook through normal Reasoner matching; the accompanying
[klab-services patch](docs/patches/README.md) supplies the shared contract. Exact comparison remains
available alongside optional fuzzy mediation; no further sibling edits are needed for this engine.

## Pattern language

```ebnf
pattern       = configuration, EOF ;
configuration = marks, reference, "(", node, { ",", node }, ")" ;
node          = configuration | marks, reference ;
marks         = { "+" } ;
reference     = qualified-reference | authority-name, ":[", escaped-payload, "]" ;
```

A qualified reference is a namespace or authority name followed by `:` and a local identifier.
The implemented token forms are documented in `PatternParser`: namespaces start with an ASCII
letter and allow letters, digits, underscores and dots; unbracketed local identifiers start with
a letter, digit or underscore and also allow dots, colons and hyphens. Arbitrary external codes
use the standard uppercase authority bracket form, e.g. `TAXA:[external code]`. Inside that payload,
`\\` and `\]` escape backslash and closing bracket. Payload punctuation is opaque to this parser.
Whitespace between tokens is ignored. Comments, negation, percentages, alternatives and general
observable expressions are not part of this first grammar.

```text
physical:HorizontalCover(+++taxonomy.species:FagusSylvatica,+taxonomy.species:Pinus)

physical:VerticalCover(
    physical:HorizontalCover(+++taxonomy.species:FagusSylvatica,+taxonomy.species:Pinus),
    ++physical:HorizontalCover(vegetation:GrassIdentity)
)
```

These are illustrative references, not declarations that the named concepts exist in any particular
worldview. Arguments retain their order, including horizontal arguments. The functor's worldview
meaning determines the spatial or temporal interpretation; the parser does not hardcode top/bottom,
adjacency or temporal direction. Configurations always have at least one argument. A bare
configuration reference is rejected during role validation; a bare identity is allowed only as an
argument. The root must be a configuration.

Each plus count describes a provisional relative quantity within the node's **immediate enclosing
configuration**. Its measure depends on the functor: spatial cover may compare area or volume;
temporal succession may compare the relative duration of its phases. In the first example, the
intended central abundance comparison is approximately 3:1.
Marks on a nested configuration describe its quantity under its parent's measure;
the marks inside it describe a separate comparison, possibly using another measure. An unmarked node has **unspecified** quantity.
It is never assigned weight one, and an unmarked list is never interpreted as equal proportions.
A marked root is preserved but has no enclosing comparison context. A sole marked child does not
establish a ratio to unmarked siblings. Numeric tolerance, normalization, and the aggregation of
species into a genus remain mediation policy questions.

Parsing is bounded at 65,536 characters, 64 nesting levels, 2,048 nodes and 128 arguments per node.
Errors include the input offset. `CoverPattern` is an immutable ordered tree; `canonical()` removes
token whitespace and preserves reference spelling, order and plus counts.

The parser is a small recursive descent implementation with its grammar next to the code. I reviewed
[dropincc.java](https://github.com/pfmiles/dropincc.java), which expresses Java grammars fluently and
compiles them dynamically. Direct parsing currently gives this small grammar bounded recursion,
precise offsets and no dynamic compiler/classloader dependency in a hosted component. The immutable
syntax tree and semantic APIs allow replacing the parser if the grammar grows. No parser-generator
dependency has been added.

## Semantic validation and host integration

Every functor must resolve to a satisfiable `CONFIGURATION`; every leaf must resolve to a
satisfiable `IDENTITY` in the configured worldview. References may resolve through that worldview's
ontologies, authority bindings or codelists. Identity bearers and full observable inherency are
intentionally not required: validating the components is distinct from constructing a semantically
well-formed observation model.

`PatternSemantics` is the host integration interface. It validates references and supplies proven
concept equivalence/subsumption. `ReasonerPatternSemantics` delegates to an actual loaded Reasoner,
checks role and satisfiability, and uses resolved concept URNs as canonical references. It guards
worldview name, loaded-worldview ID, consistency and knowledge revision. A changed revision requires
releasing and rebinding the context; this conservative guard may also require rebinding after
external authority materialization. Preload required references before constructing the adapter.

The current Authority contract constructs providers through a public no-argument constructor and
passes no Scope or Reasoner in `ConfigurationRequest`. **Automatic host wiring is not implemented
in klab-services by this component.** The hosting integration must register semantic access on the
actual provider instance before configuring it:

```java
var authority = new LandAuthority();
authority.bindWorldview(new ReasonerPatternSemantics(reasoner));
String bridge = authority.configure(new Authority.ConfigurationRequest(
    worldviewUrn, "LAND", "land:CoverIdentity",
    Map.of("urn", LandAuthority.URN, "domain", "LULC")));
var pattern = authority.validatePattern(bridge, expression);
```

`reasoner`, `worldviewUrn` and `expression` above are supplied by the host. The reasoner's stable
worldview URN must match the request. Configuration fails without semantic access; the component
never treats syntax-only acceptance as semantic validation. Replacing a context requires
`unbindWorldview(worldview)`, which also releases its bridges. Individual bridges can be released
with `releaseConfiguration(id)`. The host must invoke these lifecycle methods on reload/unload.
Cross-authority cycles and remote authorization belong to the host Reasoner, not the parser.

## Authority and catalog API

The annotated provider implements `api.services.Authority` with URN `klab.authority.land`.
Configuration parameters currently accepted are:

| Parameter | Meaning |
| --- | --- |
| `urn` | Required, `klab.authority.land` |
| `domain` | Required, `LULC` or `ECOSYSTEM` |
| `catalogs` | Optional list of registered catalog IDs, empty by default |
| `semanticBoundaries` | Only an empty mapping is supported in this first version |

The Reasoner retains the standard reserved `codelists` namespace mapping and removes it before
provider configuration. Unknown provider parameters are rejected. The optional `mediation` parameter
is documented below; it is parsed into an immutable policy rather than retaining mutable input maps.

`registerCatalog(PatternCatalog)` accepts immutable legend translations with a provider-local ID,
domain, pinned version, source URI and entries containing an alias, label and pattern expression.
Optional catalog metadata records title, source edition, tentative status, notes and proposal policy;
entries can retain the original source code, per-class source URI and omitted-criteria notes.
Registration checks source keys and unique primary aliases. Bridge configuration validates the
identity root and explicitly reviewed interpretations, and rejects a catalog from the other domain.
Draft or absent interpretations do not block source-entry annotations. Catalog IDs are immutable
within a provider instance; revisions should use new IDs, such as `corine_2018_translation_v1`.
Sources identify provenance; this API does not attest that a submitted translation is official.
The no-argument constructor automatically registers `/catalogs/corine_2018_draft.yaml` and
`/catalogs/igbp_modis61_draft.yaml`, plus the four ecosystem pilot YAMLs and `ecosystem_rules_pilot.yaml`. None is selected until
requested through `catalogs`. See the [ecosystem assessments](docs/ECOSYSTEM_INTEROPERABILITY.md)
for their source inventory and vocabulary proposal.
`PatternCatalogYaml` loads schema-versioned YAML from a stream or the classpath and rejects unknown
fields, duplicate keys/codes/primary aliases and malformed types. Schemas 1/2 retain expression syntax
checks; schema 3 permits annotation-only entries and unparsed draft definitions. Loading is bounded
at 1 MiB. Reviewed definitions undergo semantic validation at bridge configuration.
`getCatalogs()` exposes immutable data and provenance for inspection. Remote registry loading remains
a future extension.

```java
authority.registerCatalog(new PatternCatalog(
    "illustrative_v1", PatternCatalog.Domain.LULC, "draft-1",
    URI.create("https://example.org/reviewed-legend"),
    List.of(new PatternCatalog.Entry("BeechDominatedCover", "Illustrative beech cover",
        "physical:HorizontalCover(+++taxonomy.species:FagusSylvatica,+taxonomy.species:Pinus)"))));
```

This example is synthetic, not a translation of an official class. Select its ID through `catalogs`
and bind its namespace through the worldview, after adding the host wiring above:

```text
identity CoverIdentity requires authority LAND {
    urn: "klab.authority.land",
    domain: "LULC",
    catalogs: ["illustrative_v1"],
    codelists: { illustrative_v1: "land.cover" }
};
```

The namespace then exposes `land.cover:BeechDominatedCover`. Configure a separate bridge with
`domain: "ECOSYSTEM"` and another root identity for ecosystem types. These are independent bindings,
not search-filter sub-authorities.

`resolveIdentity` accepts a selected source-entry envelope or, separately, a direct pattern/rule.
Both return a full SHA-256-based concept name, a standard authority locator and the configured
identity root. Nested patterns never become superclass edges. Entry descriptions expose source
verification and interpretation-review status; unreviewed definitions cannot authorize mediation.
Malformed or unselected identities return error notifications and no concept name/base/locator.
Unknown or released configuration handles throw. Caches remain disabled.

`search` retains distinct source entries, including entries sharing one draft pattern. `reconcile`
accepts an exact alias/name/code with optional catalog/revision/sourceEdition qualifiers, or one direct
`expression` / opaque `entry` selector. Ambiguous exact names are rejected. Codelists map concept
aliases to catalog-entry envelopes rather than proposed definitions. See the [entry contract](docs/ENTRY_IDENTITIES.md).
Provider catalogs reject proposals by default; YAML metadata explicitly records the policy. The
worldview may separately bind community lists under the standard codelist contract. Provider seeds
are read-only in the existing Reasoner UI, even when proposals are allowed. Editable managed copies
must be created in a separate working list; they do not write back to bundled YAML. See the
[maintenance workflow](docs/SEED_CATALOGS.md#maintaining-the-drafts). No semantic boundaries or deferred ancestry are advertised.
Descriptions are inline Markdown; retrievable documentation resources are not advertised yet.

## Comparison and audit

`comparePatterns(bridge, left, right)` validates both expressions and returns a `PatternComparison`.
This is a conservative structural helper, **not the fuzzy mediation engine**:

| Result | Current evidence |
| --- | --- |
| `EQUIVALENT` | Same ordered tree, proven equivalent concepts, identical local marks |
| `LEFT_REFINES_RIGHT` | Proven leaf specialization in an unweighted context, or added abundance constraints where all corresponding right siblings are unmarked |
| `RIGHT_REFINES_LEFT` | Reverse of the preceding relation |
| `UNKNOWN` | Structure, semantic relationships, or abundance interpretation need further evidence |

Refinement assumes ordered positive-presence requirements. Configuration functors must be equivalent;
no covariance is assumed for arbitrary configuration types. Substitution of a species for its genus
does not transport relative abundance constraints. Different marked ratios, including proportional
counts such as 3:1 versus 6:2, remain unresolved by this policy-independent helper. Neither
unknown semantic relationships nor disjoint taxa establish incompatible covers: different bearers
can coexist. This helper does not claim quantitative incompatibility.

`auditCatalog` compares optional definitions diagnostically (quadratic cost). Equivalent definitions
are proposals worth reviewing, not equivalent source entries; strict refinement can flag potential overlap for review.
`UNKNOWN` is not evidence of inconsistency, ambiguity or compatibility. This first audit is not an
internal satisfiability proof for a whole classification. Cross-catalog pairs can be submitted through
`comparePatterns`; an indexed, provenance-rich classification mediation API remains future work.
The standard Authority name-reconciliation API is separate from pattern comparison. Configured
pattern distance delegates through the shared Authority hook and normal Reasoner matching routes.

## Fuzzy mediation

Fuzzy comparison is **opt-in for each authority configuration**. Omitting `mediation` preserves the
previous exact behavior; `mediate()` then reports `DISABLED` with no numeric overlap score. Existing
direct-definition codes do not change. Source-entry codes never enter fuzzy mediation. These are initial mathematical defaults for testing,
not thresholds calibrated against CORINE, GET, EUNIS or a particular ecosystem community.

### Configure a bridge

For a spatial cover interpretation, extend an existing worldview authority declaration as follows:

```text
identity CoverClass requires authority LAND {
    urn: "klab.authority.land",
    domain: "LULC",
    catalogs: ["corine_2018_draft"],
    codelists: { corine_2018_draft: "land.cover" },
    mediation: {
        version: "fuzzy-ratios-1",
        spreadMode: "RELATIVE",
        spread: 0.25,
        steps: 24,
        overlapThreshold: 0.5,
        admission: "PROVEN_ONLY",
        admissionAlpha: 0.5,
        configurations: {
            "physical:HorizontalArrangement": {
                interpretation: "urn:land:profiles:horizontal-area:1",
                measure: "AREA",
                support: {
                    space: "habitat-patch",
                    time: "reference-window",
                    aggregation: "total-area"
                },
                independentArguments: true
            }
        }
    }
};
```

The same mapping can be passed as `parameters.get("mediation")` in a Java
`Authority.ConfigurationRequest`. Host semantic access must already be bound, as described above.
The context is a reviewed definition of how to compare quantities, not a data resource or an
automatic spatial/temporal alignment operation. Missing context yields `UNKNOWN`, not an area default.
Complete standalone policy mappings are bundled as
[`fuzzy-area-example.yaml`](src/main/resources/mediation/fuzzy-area-example.yaml) and
[`fuzzy-duration-example.yaml`](src/main/resources/mediation/fuzzy-duration-example.yaml).
Load a mapping with the host YAML tooling and pass it as `mediation`; these files are not selected
automatically. Both are exercised by the numerical tests.

| Field | Default / required value | Meaning and validation |
| --- | --- | --- |
| `version` | Required: `fuzzy-ratios-1` | Pins this numerical and decision specification. Unknown versions fail configuration. |
| `spreadMode` | `RELATIVE` | `RELATIVE` uses half-width `spread*n`; `ABSOLUTE` uses half-width `spread`. |
| `spread` | `0.25` | Decimal in [0,1). Zero gives crisp ratios. The upper bound guarantees positive denominators even for one plus. |
| `steps` | `24` | Integral value 4..40. Possibility enclosure width is at most `2^-steps`; 24 gives approximately 0.0000000596. |
| `overlapThreshold` | `0.5` | Decimal in (0,1], inclusive. Tests the quantitative possibility exactly, including threshold boundaries. |
| `admission` | `PROVEN_ONLY` | `PROVEN_ONLY` requires candidate containment at every alpha. `ALPHA_CUT` opts into directional operational admission at one alpha. |
| `admissionAlpha` | `0.5` | Decimal in (0,1], used only by `ALPHA_CUT`. It is separate from the overlap threshold. |
| `configurations` | Empty mapping | Up to 64 quantity-functor bindings. An empty mapping permits no quantified scope. Keys resolve as worldview configurations. |
| Binding `interpretation` | Required | Pinned identifier for the quantity interpretation. Typed-rule profiles must match it exactly. |
| Binding `measure` | Required | `AREA`, `VOLUME`, `DURATION`, `COUNT`, `BIOMASS` or `OTHER`. OTHER must be explained by the pinned interpretation. |
| Binding `support` | Required | Nonblank `space`, `time`, `aggregation` identifiers. These identify comparison support, not observed coordinates or inferred units. |
| Binding `independentArguments` | Required: `true` | Explicit review assertion that local quantity variables have no additional coupling/aggregation constraints for this solver. False is unsupported and rejected. |

Numeric parameters accept decimal numbers or decimal strings; strings avoid loss of precision in
clients. `steps` must have an integral value. Decimal precision/scale are bounded; NaN, infinity,
unknown fields, incomplete contexts, duplicate canonical functors and malformed types are rejected.
Each context is immutable. Configure a new bridge to change policy; one bridge cannot change another's
tolerance. The policy snapshot and SHA-256 fingerprint are available in every mediation result and
through `getMediationPolicy(configurationId)`.

The binding's measure/support/interpretation are bridge semantics, while spread and admission settings
are comparison policy. Changing a measure requires a distinct or reconfigured bridge interpretation;
it cannot make area and duration interchangeable. Changing uncertainty does not rewrite canonical
identity codes. Normal shared matching isolates independently configured bridges, and distance caches
remain disabled. `Capabilities.isFuzzy()` retains its existing identifier/alias-search meaning; numerical
mediation is controlled by this policy and advertised through the semantic-distance capability.

### Use the API and interpret the result

```java
String left = "physical:HorizontalArrangement(+++landcover.seed:Crop,+landcover.seed:NaturalVegetation)";
String right = "physical:HorizontalArrangement(++++landcover.seed:Crop,+landcover.seed:NaturalVegetation)";
MediationResult result = authority.mediate(configurationId, left, right);

result.status();                  // DEFINED, UNKNOWN or DISABLED
result.overlap();                 // certified [lower, upper] possibility enclosure, or null
result.overlapDecision();         // SUFFICIENT, LOW or UNASSESSED
result.leftEntailsRight();        // proof under the full configured fuzzy model
result.rightEntailsLeft();
result.rightAdmittedForLeft();    // candidate=right, target=left; includes the configured admission policy
result.scopes();                  // configuration paths, context, marked positions and local results
result.policyFingerprint();       // immutable policy provenance

Authority.SemanticDistance distance = authority.semanticDistance(configurationId, left, right);
```

The first expression is the **invented** 3:1 dominance fixture for CORINE 243, not an official class
ratio; the 4:1 expression is another testing variant. With the policy above, their quantitative
possibility is approximately **0.7128129211**, above 0.5. Neither entails the other, so normal
Reasoner matching still returns `UNKNOWN/-1`. Overlap can motivate community review without
authorizing a replacement classification. A low score is not an ecological disjointness proof.

`RELATIVE` spread makes proportional counts such as 3:1 and 6:2 fuzzy-equivalent: common scaling
does not change uncertainty. `ABSOLUTE` spread makes 6:2 more precise than 3:1, so it can supply the
broader 3:1 target while the reverse is unproven. The original `comparePatterns`/`compareIdentities`
methods and `auditCatalog` retain their exact, policy-independent results; use `mediate` for fuzzy
evidence. Inputs must be expressions or canonical `rule2:` identity codes; resolve catalog aliases
through reconciliation/codelists first.

For an explicitly tolerant operational classifier, use e.g. `spreadMode: "ABSOLUTE"`, `spread: 0.75`,
`admission: "ALPHA_CUT"`, `admissionAlpha: 0.5`. A 7:2 candidate's 0.5-cut is contained in a 3:1
target's 0.5-cut. Their possibility is approximately 0.8974358974; at overlap threshold 0.5, normal
distance is **11**. This is a configured alpha-cut admission, not full fuzzy entailment. Raising the
overlap threshold to 0.95 rejects it. Increasing admission alpha need not monotonically tighten
containment: both candidate and target cuts change, so calibrate these two controls separately.

The hook preserves existing exact equivalence/refinement (distance 0/1). Otherwise, full-model fuzzy
equivalence returns 0, and an admitted candidate receives
`max(1, ceil(100*(1-overlap.lower)))`, bounded at 100. Using the lower bound makes rounding conservative.
Unsupported or non-admitted comparisons return `UNKNOWN/-1`; LAND does not use a low score to return
`INCOMPATIBLE`. Explanations identify the policy and numeric enclosure. No new shared distance API or
LAND-specific HTTP route is required.

### Duration and nested scopes

The existing `SeasonalMarshCycleRule` fixture supplies a DURATION profile. To compare its invented
3:1 phase ratio with a 4:1 variant, bind `ecosystem.seed:HydroperiodCycle` to:

```yaml
interpretation: "urn:land:profiles:hydroperiod-duration:1"
measure: DURATION
support:
  space: habitat-patch
  time: one-unanchored-cycle
  aggregation: total-phase-occupancy
independentArguments: true
```

The complete support must match the rule profile. It measures relative time spans within a cycle,
not nine versus three calendar months. The fixture's separate six-month annual interval remains a
crisp defining constraint. Spatial child quantities use their own AREA binding and reference support;
they are not combined with a duration ratio. Nested independent quantitative possibilities aggregate
by minimum. Written phase order is retained; this engine does not discover temporal causality,
align cyclic rotations, align extents, infer bearers or evaluate occurrence observations.

### Arithmetic and supported boundary

For plus count `n`, let `d=spread*n` (RELATIVE) or `d=spread` (ABSOLUTE). Its triangular weight has
center `n`, support `[n-d,n+d]`, and alpha-cut `[n-d*(1-alpha),n+d*(1-alpha)]`. A configuration is a
**projective box**: its marked quantities may take those weights up to one common positive scale.
It is not automatically normalized into cover percentages. Unmarked arguments impose no ratio;
a single marked argument also imposes no relative constraint. An unmarked vector is unrestricted,
so adding marks refines it rather than treating it as equal shares.

At a fixed alpha, the two aligned vectors overlap only if **one scale** satisfies every marked
component. The solver intersects `[left.lower/right.upper, left.upper/right.lower]` over all marked
positions. This retains shared-variable dependence. Possibility is the highest feasible alpha;
bisection returns an enclosure, while threshold feasibility is tested directly with exact rational
arithmetic. Full fuzzy containment is proved over **all** alpha values using exact quadratic maxima,
not inferred from a finite sampling grid. No external fuzzy-controller library is required.

The initial solver needs a proven structural/semantic direction after removing marks, identical
marked-position sets when both sides specify ratios, and equivalent quantified participants. It
rejects repeated equivalent quantities and known species/genus coupling. Unknown taxonomic overlap
is not proof of independence: the explicit context assertion must be reviewed. A changed inner
definition of an already quantified component requires aggregation semantics and yields UNKNOWN.
One side may be wholly unquantified. Root marks remain unscoped and cannot receive a fuzzy score.

Typed rules retain their criteria, participant/bearer declarations and profiles during comparison.
Absolute intervals remain crisp; different units, supports or unresolved logical directions are not
smoothed away. The overlap score concerns the relative-quantity subsystem, conditional on those
structural checks and the declared independence assumptions. It is not a full joint satisfiability
proof involving other observables, hidden correlations or data support. Missing/unsupported evidence
has a null score, never zero. A 65,536-pair work budget returns UNKNOWN rather than a partial score.
General variable matching, differing partial mark sets, aggregation across taxa, automatic unit/support
mediation and coupled observable/ratio constraints remain future extensions.

## Future model generation

A later extension will inspect identity inherency, find the least generic valid bearer (or report
ambiguity), infer dependencies from the active worldview, and generate k.IM namespaces containing
models that discover the classes. Their classifiers will invoke services exported by an authority
`@Library`. Neither namespace generation nor these service functions are implemented now. Future
work must define spatial/temporal arrangement observations, abundance measurements, missing-data
behavior and reproducible mediation policies before emitting runnable classifiers.

## Classification sources and design references

The design adopts the descriptive legend principle, rather than asserting an implementation of
LCCS or ISO LCML. FAO's [LCCS manual](https://www.fao.org/4/y7220e/y7220e05.htm) describes classes
through combinations of independent classifiers; the [legend module](https://www.fao.org/4/x0596e/X0596e01i.htm)
stores selected classes and mixed mapping units. This authority makes the vocabulary worldview-bound
and allows arbitrary validated identities. FAO's [LCHS documentation](https://docs-lcs.fao.org/)
provides context for the newer LCML-based tooling.

Planned curated translations need pinned releases, source definitions, explicit unsupported criteria,
and community review. Initial source families are [CORINE nomenclature](https://land.copernicus.eu/content/corine-land-cover-nomenclature-guidelines/html/index.html),
FAO LCCS, [IGBP/MODIS](https://lpdaac.usgs.gov/documents/196/wms_UserGuide_v1.3.pdf), national land-cover
systems, [EUNIS](https://www.eea.europa.eu/en/datahub/datahubitem-view/123d0c9a-a6fa-4f2d-b887-5d8e5468faed),
and [IUCN GET](https://global-ecosystems.org/). The brief mentions “NLCS”; that name is retained as an
unresolved catalog target rather than assumed to mean the US [NLCD](https://www.usgs.gov/ngp-standards-and-specifications/national-map-land-cover-content).
Thresholds such as absolute cover percentage, land use, climate and diagnostic ecological processes
must not be silently approximated by plus marks when translating legends.

The implementation follows the sibling klab-services references
[Authority contract](../klab-services/docs/AUTHORITIES.md) and
[codelist contract](../klab-services/docs/AUTHORITY_CODELISTS.md), and inspected TAXA, PubChem and UN
providers. The existing klab-services `PatternAuthority` is an unimplemented design stub; this
component does not install OWL pattern axioms or assume an OWL encoding solves numeric mediation.

## Build and verification

The [pattern elements proposal](docs/PATTERN_ELEMENTS_PROPOSAL.md) motivates the implemented
[typed rule layer](docs/TYPED_RULES.md), grounded in the k.LAB observable contract.

Use JDK 21 and matching current k.LAB snapshots:

```shell
mvn test
mvn package
```

The PF4J entry point is `KlabLandComponent`. Host dependencies are provided; packaging targets
Reasoner and Resources and produces a component KAR under `target/`. Nothing is deployed by these
commands. For development against a built sibling API without replacing shared Maven snapshots:

```powershell
mvn "-Dklab.api.jar=C:/path/to/klab-services/klab.core.api/target/klab.core.api-1.0.0-SNAPSHOT.jar" package
```

An annotation requiring legacy `name` or missing `urn`, `version` and `embeddable` indicates an older
resolved API. Refresh matching snapshots or use the development override. The parent and other host
modules must still be API-compatible. The development profile uses a local system dependency and
is not a release configuration.

The initial fixture suite covers parsing, bounds, opaque external IDs, semantic roles, missing
references, bridge isolation/release, abundance scope, unspecified abundance, conservative semantic
refinement, catalog domain isolation, codelist resolution, alias ambiguity, search round-trips,
duplicate auditing and resolver-output injection. These are local tests with synthetic concepts,
not a deployed multi-service/worldview integration test.

The catalog fixtures additionally check the exact 44/17 source-code sets, every alias against the
supplied draft vocabulary, source/limitation metadata, code search, catalog-scoped reconciliation,
known lossy collisions, opt-in selection and strict YAML errors. These checks do not validate the
proposed `.kwv` vocabulary through an actual loaded worldview or establish faithful classification
definitions.

The ecosystem fixtures check complete source inventories (110 GET, 270 revised-terrestrial EUNIS,
249 marine EUNIS and 21 legacy inland-water level-3 records), all 69 selected ecosystem aliases,
separate editions, declared source levels, the 16 review-case references and temporal order/marks.
They deliberately do not claim a quantitative duration evaluator or official GET-EUNIS crosswalk.
The exploratory team workbook was read-only; its digest and selected review leads are documented
in the [WEED assessment](docs/ECOSYSTEM_INTEROPERABILITY.md).

Source-entry identity verification: the full offline test suite now includes 19 new entry-envelope,
collision, source-code, edition/revision, annotation-only, review-gate and ambiguity tests against the
shared API build from services `df9c92af`. No host wiring or deployed worldview acceptance is claimed.
The exact build/test command and dependency hashes are retained in the separate review packet.

Earlier illustrative baseline, before source-entry separation: `mvn -o package` passed 80 tests and
generated the component archive. The JAR includes seven YAML catalogs, three proposed vocabularies,
the typed rule API and separate source-reference indexes. The shared distance patch passed 43 focused
tests; broader shared-suite limitations are recorded in the [handoff notes](docs/patches/README.md).
Earlier in the implementation, the sibling archetype's
`mvn -o verify` also passed both full and minimal generation/compilation checks after correcting its
Authority starter, provided dependency scope and server artifact coordinates.
