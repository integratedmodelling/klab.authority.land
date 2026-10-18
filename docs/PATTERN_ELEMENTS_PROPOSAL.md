# Pattern elements beyond configurations and identities

This proposal follows the ecosystem pilots and the requirement to represent spatial arrangements,
temporal consequentiality and relative duration. It is grounded in the sibling k.LAB
[observable contract](../../klab-services/docs/OBSERVABLES.md),
[worldview language](../../klab-services/docs/ONTOLOGY_LANGUAGE.md), and the current
[`Observable.Builder`](../../klab-services/klab.core.api/src/main/java/org/integratedmodelling/klab/api/knowledge/Observable.java)
and [`SemanticType`](../../klab-services/klab.core.api/src/main/java/org/integratedmodelling/klab/api/knowledge/SemanticType.java)
APIs. The initial implementation is documented in [typed rules](TYPED_RULES.md), with explicit limits
on validation, entailment and observation evaluation. The composition parser still accepts only
configurations, identity leaves and relative quantity marks; catalog schema 2 adds a typed rule envelope.

## Recommended elements

Configurations and identities remain the core composition language. Ecosystem definitions also need
criteria about characteristics, relationships and change. They should retain their actual semantic
types instead of acquiring nominal identity concepts solely to fit the current parser.

| Proposed element | Purpose and example | Relationship to k.LAB |
| --- | --- | --- |
| Predicate qualification | Attribute a deciduous habit to the vegetation component, or a management role to its bearer. | Resolve the predicate as its declared type: identity, attribute, realm or role. Use admissible trait/role attribution; a role needs its applicable context. |
| Observable constraint | Require a soil-pH interval, canopy-cover bound or inundation-duration criterion. | Resolve a quality or derived observable with its bearer, units and operators. The rule constraint is an authority object; it is not itself an identity or a new ontology concept. |
| Participant binding and relationship | State which canopy lies above which understorey, or which water body connects two components. | Bind named participant occurrences to inferred or explicit bearers. A semantic relationship must satisfy the worldview's endpoint restrictions. Pattern topology can also be an evaluator relation without claiming a corresponding ontology relationship. |
| Occurrent criterion | Require recurrent flooding, a disturbance event or a succession involving a process. | Preserve the distinction between `EVENT` and `PROCESS`; select presence, occurrence, duration or another legitimate observable according to the intended criterion. |
| Logical group | Express “vent, seep or organic fall”, a diagnostic combination, or a minimum number of criteria. | Add explicit rule-level conjunction, alternatives and cardinality. Do not infer these from the comma-separated composition syntax or assume that ontology union/intersection supplies an evidence evaluator. |

Participant binding is especially useful: two occurrences of the same identity can represent different
components. A constraint must identify the occurrence it concerns, not attach globally to every leaf
having the same concept. Participant names should be local bindings, with canonical renaming, rather
than new authority identities.

## Bearers can be deferred, but their obligations must survive

The existing pattern language deliberately allows identity leaves without substantial bearers.
Preserve this useful abstraction. A proposed qualification or constraint can similarly retain an
unresolved participant/bearer obligation while a definition is being assembled. Distinguish that
provisional definition from one validated against a particular worldview and from an executable model.

k.LAB predicates generally require an inherent observable to form a useful observation request. Its
contract distinguishes `P of S` (characterization) from `P of each S` (classification); a bare predicate
does not supply an executable request. A generated classifier must therefore resolve admissible
bearers and their distribution, not mechanically convert an identity leaf into a dependency.
The inferred least generic bearer also needs to remain consistent with the other predicates and
constraints on that participant. If several incomparable candidates remain, report the ambiguity.

For qualifications, use the builder's trait and role mechanisms with the intended main concept and
inherency. For relationships, `linking(source, target)` requires both endpoints, substantial operands
and compatibility with inherited endpoint restrictions. Resolving concept names or checking their
`SemanticType` alone cannot establish these conditions.

## Observation constraints need a separate, typed representation

A constraint should identify its target participant, resolved observable, comparison, value/unit,
spatial and temporal support, aggregation, modality and source. For example, “at least six months
inundated per year” and “inundated for three times the exposed duration” are different constraints.
Neither can be reconstructed from an unqualified integer weight.

The current `Observable.Builder` provides units, unary semantic operators and value operators.
These are building blocks, not a guarantee that every ecological threshold can be expressed as one
observable. Keep unsupported aggregations or cross-participant comparisons explicit in the authority
constraint AST and its evaluator. Validate dimensional compatibility before applying fuzzy arithmetic.
An area fraction, duration fraction, frequency and biomass fraction need distinct measure bindings.

Keep definition thresholds separate from observational uncertainty and the authority configuration's
comparison policy. Preserve source inclusivity at interval endpoints. “Typical”, “necessary” and
“diagnostic in combination” also need explicit modality; otherwise an informative description can
silently become a mandatory criterion.

## Temporal structure is more than an occurrent concept

An event or process identifies what occurs; a temporal configuration identifies how occurrences or
states are arranged. Retain both when needed. A sequence may constrain before/after, overlap, gaps
or cyclic recurrence. Its profile must establish whether order also asserts causation.

The builder's `withTemporalInherent` is an observable contextualization facility, documented as not
changing semantics; it does not implement a temporal relation algebra or cycle evaluator. Those
belong to explicit configuration profiles and authority evaluation services. Relative duration marks
remain scoped to their enclosing functor and measure, as detailed in the
[pattern evolution notes](ECOSYSTEM_PATTERN_EVOLUTION.md#temporal-consequentiality-and-relative-duration).

For a fire-dependent ecosystem, distinguish a fire observation, a fire-event duration and a fire
regime over a reference window. Their absence, recurrence and diagnostic role are different questions.
A missing fire observation supplies none of these conclusions on its own.

## Logic and evidence must be explicit

Use dedicated rule nodes for alternatives and cardinality, with stable participant scope. Keep
composition separate: the present parser's ordered argument list is not a general Boolean language.
An optional component means a permitted alternative, whereas an optional data dependency means an
evaluator can continue without that input. These should not share an undifferentiated flag.

An exclusion criterion requires an explicit evidence and completeness policy. Under incomplete
knowledge, failure to observe a species or process cannot prove its absence. In the inspected
`SemanticType` source, `ALL` and `NONE` are retained syntactic qualifiers without implemented semantic
consequences; they must not be used as shortcuts for universal quantification or measured absence.
Similarly, an ecological alternative is not automatically equivalent to a union concept's semantic
subsumption behavior.

Keep provenance, confidence, reviewer notes, national applicability and available data sources in
the rule envelope. They should affect explanations and applicability as specified, rather than
silently changing an identity's ontology semantics. A resource URL belongs to an evidence binding,
not the definition of an ecosystem.

## Proposed implementation boundary

1. Retain the current composition AST and grammar as version 1. Introduce a versioned rule envelope
   with typed qualification, constraint, binding, relation and logical-group nodes. Select concrete
   serialization only after representative cases have been reviewed.
2. Extend semantic validation beyond the current `CONFIGURATION`/`IDENTITY` role check. Add resolved
   predicate types, observable construction, bearer obligations, relationship endpoint validation and
   configuration-profile validation. Keep parsing, provisional validation and executable validation
   distinguishable in results.
3. Include every defining criterion, participant binding and profile interpretation in canonical rule
   identity. Canonicalize local binding names. Keep tolerances and decision thresholds in versioned
   per-configuration policy snapshots and result/cache provenance.
4. Preserve the normal `Authority.Identity` contract for the identity denoted by a complete pattern.
   Expose structured rule definitions and comparisons through the specific authority API. Internal
   quality, process and relationship nodes do not thereby become authority identity leaves.
5. Specify evaluators and explanatory results before generating k.IM models. The existing conservative
   comparator should continue to return `UNKNOWN` for unsupported comparisons; a new node must not
   produce a compatibility score merely because its serialization resembles another node.

The first useful extension would be **participant-bound observable constraints plus explicit
alternatives**. These capture many distinctions currently retained only in the GET/EUNIS notes.
Occurrent criteria and relationship bindings can then support richer temporal and spatial patterns
without forcing measurements, processes and contextual roles into the identity category.
