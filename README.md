# Earth cover authority

`klab.authority.land` describes earth-cover and ecosystem classes through ordered patterns of
worldview configurations and identities. Its purpose is to make verbal classification legends
explicit enough to compare, mediate, and review. Users normally encounter curated codelist
names rather than the expressions themselves.

This is an initial implementation of the pattern language, semantic validation adapter, standard
Authority provider, versioned catalog/codelist API, typed rule envelopes and conservative entailment.
The fuzzy mediation engine needs a separate specification; its proposed design and open decisions
are in [Mediation design](docs/MEDIATION.md). No fuzzy thresholds or tolerances are implemented.
Two tentative classpath YAML catalogs now supply all 44 CORINE level-3 aliases and all 17
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
[klab-services patch](docs/patches/README.md) is required. This remains exact, conservative mediation.

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
provider configuration. Unknown provider parameters are rejected, including prospective mediation
settings until their schema and interpretation are specified.

`registerCatalog(PatternCatalog)` accepts immutable legend translations with a provider-local ID,
domain, pinned version, source URI and entries containing an alias, label and pattern expression.
Optional catalog metadata records title, source edition, tentative status, notes and proposal policy;
entries can retain the original source code, per-class source URI and omitted-criteria notes.
Registration checks syntax and unique aliases. Bridge configuration validates every selected entry
against its own worldview and rejects a catalog from the other domain. Catalog IDs are immutable
within a provider instance; revisions should use new IDs, such as `corine_2018_translation_v1`.
Sources identify provenance; this API does not attest that a submitted translation is official.
The no-argument constructor automatically registers `/catalogs/corine_2018_draft.yaml` and
`/catalogs/igbp_modis61_draft.yaml`, plus the four ecosystem pilot YAMLs and `ecosystem_rules_pilot.yaml`. None is selected until
requested through `catalogs`. See the [ecosystem assessments](docs/ECOSYSTEM_INTEROPERABILITY.md)
for their source inventory and vocabulary proposal.
`PatternCatalogYaml` loads schema-versioned YAML from a stream or the classpath and rejects unknown
fields, duplicate keys/codes/aliases, invalid expressions and malformed types. Loading is bounded at
1 MiB. Semantic validation still happens at bridge configuration, never merely from loading YAML.
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

`resolveIdentity` accepts a pattern and returns its canonical expression as the provider code,
a full SHA-256-based concept name, an escaped standard authority locator, Markdown description,
and the configured identity root. Nested patterns are arguments, never superclass edges.
Parse/semantic failures return error notifications and no concept name/base/locator. Unknown or
released configuration handles throw. Caches are disabled so host caches cannot bypass worldview
validation. Canonical codes preserve the plus counts even when a future mediation policy might
judge two different codes equivalent.

`search` matches selected catalog IDs, original source codes, aliases and labels, deduplicates by canonical pattern, and returns
up to 100 deterministically ordered, resolvable identities. `reconcile` accepts exactly `expression`,
or `alias` with optional `catalog`. An ambiguous alias is an error; no first match is selected.
Codelists map concept aliases to canonical pattern expressions and carry source/version metadata.
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
counts such as 3:1 versus 6:2, remain unresolved until uncertainty semantics are specified. Neither
unknown semantic relationships nor disjoint taxa establish incompatible covers: different bearers
can coexist. No incompatibility result or numeric mediation distance is currently claimed.

`auditCatalog` compares every pair of selected catalog entries (quadratic cost). Equivalent entries
are duplicate definitions worth reviewing; strict refinement can flag potential overlap for review.
`UNKNOWN` is not evidence of inconsistency, ambiguity or compatibility. This first audit is not an
internal satisfiability proof for a whole classification. Cross-catalog pairs can be submitted through
`comparePatterns`; an indexed, provenance-rich classification mediation API remains future work.
The standard Authority name-reconciliation API is separate from pattern comparison. The current
klab-services Authority contract has no pattern-distance delegation hook or remote route for this API.

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

Verification on 2026-10-18: `mvn -o package` passed all 65 tests against the updated shared API and
generated the component archive. The JAR includes seven YAML catalogs, three proposed vocabularies,
the typed rule API and separate source-reference indexes. The shared distance patch passed 43 focused
tests; broader shared-suite limitations are recorded in the [handoff notes](docs/patches/README.md).
Earlier in the implementation, the sibling archetype's
`mvn -o verify` also passed both full and minimal generation/compilation checks after correcting its
Authority starter, provided dependency scope and server artifact coordinates.
