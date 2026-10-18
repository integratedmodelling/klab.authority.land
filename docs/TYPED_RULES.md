# Typed ecosystem rules, schema 2

The [pattern elements proposal](PATTERN_ELEMENTS_PROPOSAL.md) now has an initial Java implementation.
The original composition grammar remains unchanged. A versioned rule envelope adds participant-bound
criteria, logical groups and configuration profiles. This document describes the implemented subset
and its boundaries; the [evolution notes](ECOSYSTEM_PATTERN_EVOLUTION.md) retain the longer-term design.

## Examples and codelist

The opt-in ECOSYSTEM catalog
[`ecosystem_rules_pilot`](../src/main/resources/catalogs/ecosystem_rules_pilot.yaml) contains six
complete envelopes. These **invented fixtures** illustrate distinctions found in the existing
classification assessments. They do not replace the original pilots or assert official refinements.

| Alias | Source-class inspiration | Implemented example |
| --- | --- | --- |
| `AcidBeechSoilRule` | EUNIS 2021 T17 | Soil participant; invented annual-mean pH upper bound, strictly below 5. |
| `NonAcidBeechSoilRule` | EUNIS 2021 T18 | Soil participant; invented inclusive pH interval [5,8]. |
| `ChemosyntheticAlternativesRule` | GET 2.1 M3.7 | Any of vent, seep or organic-fall settings; they are not three required co-occurring components. |
| `PyricForestRule` | GET 2.1 T2.6 | Canopy attribute, regeneration process, semantic relationship and advisory recent-fire event. A recent fire does not define a pyric regime. |
| `SeasonalMarshCycleRule` | GET 2.1 TF1.4 | Invented 3:1 phase-duration profile, temporal relation and separate invented annual six-month inundation bound. |
| `SeagrassDiagnosticsRule` | GET 2.1 M1.1 | Invented two-of-three diagnostic group with attribute, realm and role; bearer inference and role context remain explicit obligations. |

All numeric bounds, observation supports and modalities in this new catalog are experimental. In
particular, the pH split is not an adopted EUNIS definition. The duration ratio cannot establish a
calendar period. Original GET/EUNIS source provenance remains in the earlier catalogs and reference
indexes. The additional
[`ecosystem.rules.seed.kwv`](../src/main/resources/vocabularies/ecosystem.rules.seed.kwv) is an invented
typed vocabulary, not a claim of validated worldview consistency.

Select this catalog independently:

```text
identity EcosystemClass requires authority ECOSYSTEM {
    urn: "klab.authority.land",
    domain: "ECOSYSTEM",
    catalogs: ["ecosystem_rules_pilot"],
    codelists: { ecosystem_rules_pilot: "ecosystems.rules" }
};
```

After installing the three seed vocabulary proposals in a test worldview and binding semantic
access to the provider, an alias such as `ecosystems.rules:ChemosyntheticAlternativesRule` resolves
through the normal Authority/codelist contract. Host wiring still needs to supply
`ReasonerPatternSemantics`; adding this catalog does not install worldview ontologies automatically.

## Representation

[`PatternRule`](../src/main/java/org/integratedmodelling/klab/authority/land/PatternRule.java) is an
immutable record. Its fields are:

| Field | Contract |
| --- | --- |
| `schemaVersion` | Exactly 2 for a rule envelope. |
| `composition` | An existing configuration/identity expression. Order and marks retain their original meaning. |
| `participants` | Local IDs, zero-based argument paths and explicit bearer concept or `null` for deferred inference. Empty path `[]` selects the root. |
| `profiles` | Configuration path, pinned interpretation identifier, quantity measure, order interpretation, support and exhaustiveness. |
| `criteria` | One typed criterion or recursively nested logical group. |
| `provenance` | Source, source code and review note; external to defining identity. |

Participants identify **occurrences** in the composition. Distinct paths can refer to the same
concept while retaining separate constraints. IDs and paths must be unique; an invalid path or
unbound criterion reference is rejected. Several criteria can refer to the same participant.
Profiles must identify unique configuration nodes. A profile's measure may be AREA, VOLUME,
DURATION, COUNT, BIOMASS or OTHER; OTHER's detailed interpretation belongs to its pinned profile.
Ordering may be DECLARED, SEQUENCE or CYCLE. The structure alone does not prove temporal causation,
recognize cycle rotations or supply a duration evaluator.

Every criterion has an explicit `kind`:

| Kind | Required meaning |
| --- | --- |
| `qualification` | Participant, declared predicate type (IDENTITY/ATTRIBUTE/REALM/ROLE), predicate reference, expected presence and support. |
| `constraint` | Participant, numeric quality observable, unit, nullable lower/upper decimal bounds, explicit inclusivity and support. At least one bound is required. |
| `relationship` | Source and target participants, SEMANTIC/SPATIAL/TEMPORAL type, relation concept or pinned evaluator identifier and support. |
| `occurrent` | Participant, EVENT/PROCESS type, concept, expected presence and support. This is occurrence evidence, not a complete recurrence/regime model. |
| `group` | ALL, ANY or AT_LEAST; AT_LEAST alone takes `minimum` in 1..child count. |
| `typical` | An advisory criterion and explanatory note. It cannot decide membership. |

Support always states `space`, `time` and `aggregation`. These are application/profile identifiers,
not implicit defaults or automatically inferred observation extents. `typical` may stand alone or
occur under ALL. It is rejected inside ANY/AT_LEAST branches, where treating an advisory item as
true could otherwise admit a class. Duplicate logical children are rejected, including duplicates
discovered during canonicalization. No general negation-as-failure operator is provided.

An excerpt of the soil fixture illustrates explicit interval endpoints:

```yaml
criteria:
  kind: constraint
  participant: soil
  observable: "ecosystem.rules.seed:SoilPH"
  unit: "1"
  minimum: null
  minimumInclusive: false
  maximum: 5
  maximumInclusive: false
  support:
    space: habitat-patch
    time: reference-year
    aggregation: annual-mean
```

This excerpt needs its envelope and declared participant; see the catalog for full runnable fixtures.

## Validation and API

```java
PatternCatalog examples = authority.getCatalogs().get("ecosystem_rules_pilot");
PatternRule rule = examples.entries().getFirst().rule();
RuleValidation validation = authority.validateRule(configurationId, rule);
String code = PatternRuleYaml.canonical(validation.rule());
Authority.Identity identity = authority.resolveIdentity(configurationId, code);
PatternComparison comparison = authority.compareIdentities(configurationId, code, otherCode);
```

`RuleSemantics` extends the old semantic-access interface without changing existing implementations.
It adds typed reference resolution, numeric observable resolution, combined attribution, observable/
bearer/unit binding and relationship endpoint checks. `ReasonerPatternSemantics` implements these
checks through the loaded Reasoner and retains its worldview/revision guards. Required positive
attributions on a participant are checked together with its composition identity when that path
identifies an identity leaf. Configurations remain distinct from their constituents.

`RuleValidation` returns the checked rule, resolved dependency references and outstanding obligations.
Wrong types, inconsistent required attributions, invalid units/bindings and unresolved concepts fail
validation when the host reports them. A missing bearer, conditional/negative branch attribution,
role context, occurrent contextual binding or profile/relation evaluator remains an explicit obligation.
Automatic least-generic-bearer inference, full branch satisfiability and evaluator discovery are not
implemented. An individually valid predicate is not proof that every branch is jointly satisfiable.

`ready()` means no reported validation/binding obligations. It does not attest data availability or
generate an executable k.IM model. Ordinary Authority identities may represent provisional rules;
their descriptions disclose outstanding obligations. A legacy semantic host can inspect a provisional
`validateRule` result, but must supply `RuleSemantics` before materializing typed rule identities.
The original expression-only catalogs continue to use the original semantic interface.

Catalog schema 2 adds an optional inline `rule` field to entries. `expression` remains required and
must equal the envelope's composition. Schema 1 is unchanged and rejects the additional key. Catalog
IDs/aliases stay separate from canonical identity codes. Legacy Java entry constructors remain valid.

## Canonical identity and transport

`PatternRuleYaml` reads data-only YAML/JSON with strict duplicate-key, unknown-property and scalar-type
checks. Every record field must be supplied; deferred fields are explicitly null. Input is limited
to 128 KiB, parser nesting to 96, criteria nesting to 32, total criteria to 1024, participants/profiles
to 128 each and group arity to 128. Composition retains its existing tighter limits. Decimals have
bounded precision/scale and never pass through binary floating-point comparison.

The canonical provider code is `rule2:` followed by compact JSON. Canonicalization sorts object keys,
sorts commutative logical children, assigns participant IDs by their composition paths and orders
profiles by path. It normalizes decimal spelling and excludes source provenance and advisory note
text. It preserves composition order, branch structure, typed criteria, bounds, units, support and
profile interpretation. Full semantic equivalence is not a general canonicalization algorithm:
equivalent observable spellings or independently equivalent logical formulas may still have distinct
codes and require comparison. Changed defining constraints cannot collapse into the old composition-only
identity. Unused participant/bearer declarations also remain identity-relevant.

The normal SHA-256 concept naming and escaped Authority locator still apply. Shared
`AuthorityIdentitySyntax` now permits bounded complex codes up to 131072 characters, allowing full
rule definitions to round-trip without relying on a transient hash registry. This requires the
updated sibling API; see the [shared patch handoff](patches/README.md).

## Comparison through normal Reasoner distance

LAND implements the shared `Authority.semanticDistance(configurationId, targetId, candidateId)` hook.
Direction matters: exact equivalence returns COMPATIBLE/0 and proven candidate refinement returns
COMPATIBLE/1. Optional [fuzzy mediation](../README.md#fuzzy-mediation) adds relative-quantity containment
or explicitly configured alpha-cut admission. Overlap alone does not admit a candidate, and UNKNOWN
does not assert ecological disjointness. The exact APIs below remain policy-independent.

`RuleComparison` proves a limited set of implications: existing composition refinement, identical
participant/bearer declarations and profiles, same-support/unit interval containment including
open/closed endpoints, positive predicate specialization, and sound ALL/ANY implication rules.
AT_LEAST comparison currently requires identical structure. Advisory descriptions do not impose
membership obligations. Different measures/supports, arbitrary unit conversions, different profile
interpretations, mixed expression/rule identities or unsupported logic produce UNKNOWN.

The [shared distance contract](../../klab-services/docs/AUTHORITY_DISTANCE.md) describes normal matcher
dispatch for identity heads, traits and modifiers, bridge isolation, fail-closed errors and capture
restrictions. The typed shared result preserves UNKNOWN versus INCOMPATIBLE; normal integer Reasoner
matching maps both to a rejected candidate. Existing providers' UNSUPPORTED default retains hierarchy
matching. LAND's current solver does not claim INCOMPATIBLE.

The initial `mediation` parameters are specified in the [README](../README.md#fuzzy-mediation)
and [MEDIATION.md](MEDIATION.md). Tolerances and thresholds are **per authority configuration**,
separate from source definition intervals. No global Reasoner tolerance was introduced.
Distance is uncached, and the shared matcher bypasses
aggregate distance caches while authorities are configured pending a policy-revision contract.

## Reference observation evaluation

`RuleEvaluation.evaluate(rule, evidence)` supplies a crisp three-valued reference evaluator with
per-criterion explanations. Hosts provide composition evidence, scoped categorical findings and
numeric observations. The evaluator checks exact unit/support equality and decimal interval bounds;
it performs no hidden unit conversion, aggregation or data discovery. ALL/ANY/cardinality retain
UNKNOWN when the remaining evidence could change the outcome. Advisory results appear in explanations
without deciding membership.

A negative categorical/occurrent finding establishes absence only when its evidence explicitly
asserts complete coverage for the criterion's support. Missing or incomplete negative evidence is
UNKNOWN. A positive presence finding can disprove an exclusion without complete coverage.
Composition evidence must cover the declared arrangements/profiles; simply supplying constraint
values cannot establish an ecosystem occurrence. Callers own semantic validation and evidence
admissibility. This evaluator is separate from class-definition mediation and is not yet exposed as
a k.LAB `@Library`, observation service or generated model.

Remaining extensions include reviewed observation/profile evaluators, bearer inference, contextual unit/aggregation
mediation, full logical satisfiability, cycle equivalence, coupled quantity arithmetic, richer distance
explanations in service transport and k.IM generation. The new AST records their inputs and obligations
without treating unsupported criteria as false or assigning invented compatibility scores.
