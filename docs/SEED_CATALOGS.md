# Tentative classpath catalogs

The authority now ships two opt-in test legends:

This section describes the initial LULC legends. Additional ECOSYSTEM-domain pilots are listed in
the [GET](GET_DRAFT.md), [EUNIS](EUNIS_DRAFT.md) and [WEED comparison](ECOSYSTEM_INTEROPERABILITY.md)
assessments. Their separate editions and source scopes must be preserved.

The additional `ecosystem_rules_pilot` catalog uses schema 2 for six invented typed-rule fixtures.
Its thresholds and modalities are experimental; see [typed rules](TYPED_RULES.md) for the complete
schema and normal Authority-distance integration. Existing schema-1 catalogs remain unchanged.

| Catalog ID | Scope | Source |
| --- | --- | --- |
| `corine_2018_draft` | All 44 CORINE level-3 classes | [CLC nomenclature guidelines, version 10, 2019](https://land.copernicus.eu/en/products/corine-land-cover?tab=documentation), with [individual class pages](https://land.copernicus.eu/content/corine-land-cover-nomenclature-guidelines/html/index.html) |
| `igbp_modis61_draft` | All 17 MCD12Q1 `LC_Type1` classes | [MODIS Collection 6.1 guide, 20 April 2022, table 3](https://lpdaac.usgs.gov/documents/1409/MCD12_User_Guide_V61.pdf#page=9) |

The YAML files live under `src/main/resources/catalogs/`. They are manually authored proposals,
not generated classifications or official crosswalks. Translation revision `draft-1` is distinct
from the source edition. Each entry retains a quoted source code, a legal concept alias, label,
pattern, source link and notes about omitted criteria. Catalog-level notes apply to every entry.
These notes reach codelist descriptions and resolved identity descriptions, so users can see losses
without opening the source YAML. Search accepts source codes as text; `resolveIdentity` still
expects a pattern, not an unqualified source number such as `311`.

The separate [CORINE translation attempt](CORINE_DRAFT.md) and [IGBP translation attempt](IGBP_DRAFT.md)
record classification-specific choices, unresolved ambiguities and complete entry inventories.

The IGBP draft specifically follows **MCD12Q1** numbering: water is 17. It does not adopt the water-0
convention used in some other products. The fill/missing-input code 255 is excluded because missing
observations do not identify a kind of cover. No raster data or map-classification algorithm is included.

## Vocabulary and relationship to imod

Inspection of the sibling `imod` checkout found `physical:SpatialArrangement` with
`HorizontalArrangement` and `VerticalArrangement` children, plus `earth:EarthCover` and a provisional
`land:LandCover` authority binding. It did not provide the functional vegetation identities needed
for these legends. For example, `earth:Wetland` is a thing, not an identity suitable as a leaf.
The earlier illustrative names `physical:HorizontalCover` and `VerticalCover` are not declared in
that checkout. The seeds therefore use the actual arrangement names and a separate proposed identity
namespace, `landcover.seed`.

`src/main/resources/vocabularies/landcover.seed.kwv` supplies a small **test-only proposal** containing
`CoverClass`, `CoverComponent` and the leaf identities used by both catalogs. It is not automatically
loaded, installed into imod, or accepted as a semantic bypass. Add it explicitly to a test worldview,
or provide equivalent reviewed concepts and revise the expressions, before configuring these catalogs
against a real Reasoner. The host semantic binding described in the main README is still required.
The `.kwv` file has not been ingested and validated in a deployed worldview in this change.

The proposal leaves bearers unspecified, consistent with the pattern API. Its deliberately flat
identity vocabulary does not assert a botanical taxonomy or tree/grass measurement model. Some
terms encode function or state (`IrrigatedCrop`, `PortSurface`, `SparseVegetation`) and require
ontology review. Union-like terms (`SnowOrIce`, `FruitOrBerryCrop`) avoid wrongly requiring both
alternatives in a positive-presence argument list. They are provisional identities, not an added OR
operator. A source classification code is never used as its own defining leaf.

## What the patterns do and omit

The draft aims to exercise parsing, semantic role checks, codelists, name reconciliation, nested
configurations and audit findings. It does not aim to reproduce operational map-production rules.
Most entries retain only a diagnostic material, vegetation form or functional identity. Class-specific
absolute cover thresholds, height, duration, minimum mapping units, land-use context and exclusion
rules remain prose notes. Quantitative statements in notes are **not executable constraints**.

Examples of intentional decisions:

| Classes | Draft choice and test consequence |
| --- | --- |
| CORINE 311 / 312 | Broadleaved versus conifer tree identities. Conifers are not assumed evergreen. |
| CORINE 313 / IGBP 5 | Different mixtures: broadleaf/conifer versus evergreen/deciduous. They are not asserted equivalent. |
| CORINE 243 | `+++Crop,+NaturalVegetation` exercises marks. The 3:1 choice is illustrative and is not a translation of an official percentage requirement. |
| CORINE 244 | Schematic vertical nesting puts a tree arrangement before a crop arrangement. This exercises composition; it is not a complete agroforestry definition. |
| IGBP 6 / 7 | Both become `HorizontalArrangement(Shrub)` because the cover thresholds are omitted. |
| IGBP 8 / 9 | Both become `HorizontalArrangement(Tree)` because their cover ranges are omitted. No grass understory is required without evidence. |

Unmarked arguments never imply equal coverage. Except for the explicitly schematic agroforestry
example, sibling ordering is a stable draft serialization choice rather than an official spatial
relation. A future configuration-specific interpretation may require replacing those choices.
No fuzzy membership functions, tolerances or decision thresholds are selected by these catalogs.

Two aliases with the same pattern intentionally resolve to the same canonical authority identity.
The IGBP draft therefore has 17 aliases but only 15 distinct pattern identities. Their merged identity
labels and descriptions retain both source classes and their omission notes. Auditing marks the
closed/open shrubland and woody-savanna/savanna pairs structurally equivalent **as translated**.
That is evidence that these draft patterns lost distinctions, not evidence that IGBP is internally
inconsistent. Similar coarse matches across catalogs must be treated with the same caution.

## Selecting and binding the seeds

After loading the proposed vocabulary into the test worldview and binding semantic access to the
provider instance, select catalog IDs through `catalogs`. For a source declaration, replace the
fixture's plain `identity CoverClass;` with a requirement such as:

```text
identity CoverClass requires authority LAND {
    urn: "klab.authority.land",
    domain: "LULC",
    catalogs: ["corine_2018_draft", "igbp_modis61_draft"],
    codelists: {
        corine_2018_draft: "landcover.corine",
        igbp_modis61_draft: "landcover.igbp",
        corine_working: "landcover.corine_working",
        igbp_working: "landcover.igbp_working"
    }
};
```

The last two IDs intentionally have no provider seed catalog. Under the existing Reasoner contract,
they create empty community working lists accepting proposals. They are not entries in `catalogs`.
The reserved `codelists` namespace mapping is retained by the Reasoner rather than passed to this
provider. No selection means no seed codelists, even though the YAML is registered in the constructor.

The seed namespaces expose aliases such as `landcover.corine:BroadLeavedForest` and
`landcover.igbp:EvergreenNeedleleafForest`. On the Java API, inspect provenance through
`getCatalogs()`, use `getCodelists(bridge)` for alias-to-pattern mappings, and select a specific catalog
when reconciling aliases shared between classifications:

```java
authority.reconcile(bridge, Map.of("catalog", "corine_2018_draft", "alias", "MixedForest"));
authority.search("311", null, bridge);
authority.auditCatalog(bridge, "igbp_modis61_draft");
```

The audit returns all pairs, including unresolved ones, rather than issuing an overall validity verdict.

## Maintaining the drafts

The existing Reasoner protects **provider seed aliases** and accepted community aliases from direct
mutation. `acceptsProposals: true` would allow additional proposals; it would not make an existing
seed editable. The seed YAML sets it to `false` to keep additions deliberate during initial testing.
This behavior was checked in `klab-services/docs/AUTHORITY_CODELISTS.md` and the `AuthorityCodelists`
implementation, rather than inferred from the dashboard layout.

Maintainers can use the current UI without changing the seed contract:

1. Bind a separate working list as in the example above, with worldview editing rights.
2. Open **Authority codelists** (`/ui/authority-codelists`) as an administrator and select the working namespace.
3. Use CREATE to copy a desired source alias and its pattern from the seed into the working list.
   The alias may keep the same local name because its namespace is different.
4. Use UPDATE to refine that managed alias and point it to the revised pattern. The Reasoner validates
   the target; all new concepts must already exist in the bound worldview. Managed entries remain
   editable until they become accepted community aliases.
5. Use the revised working namespace for experiments. Review changes and their omitted-criteria notes,
   then edit the classpath YAML and release a new version to distribute them as provider seeds.

UI changes persist in the Reasoner's ledger; they do not rewrite a component archive, the source
YAML, its provenance notes or its registered catalog audit input. `auditCatalog` still audits bundled
catalogs; pass working-list pattern values to `comparePatterns` to compare experiments. Copying all
61 entries in one step would require a managed-list import feature or an explicit API client; this
change does not add one. Editing the seed in place or round-tripping UI changes to YAML would also
require a separate host API decision. No Reasoner source or running UI data was changed here.

## YAML schema

`PatternCatalogYaml` accepts exactly one schema-version-1 mapping. No polymorphic Java object
construction, external file includes or remote loading is used. Unknown fields, duplicate YAML keys,
wrong scalar types, duplicate aliases/source codes and malformed patterns fail loading. The reader
caps input at 1 MiB. Catalog syntax is checked on load; concepts are validated against the actual
worldview only when a selected bridge is configured.

```yaml
schemaVersion: 1
id: example_draft
title: "Tentative example"
domain: LULC
version: draft-1
sourceVersion: "Pinned source edition"
source: "https://example.org/legend"
tentative: true
acceptsProposals: false
notes:
  - "Catalog-wide limitations."
entries:
  - code: "001" # Always quote source codes; preserve spelling and leading zeroes.
    alias: ExampleCover
    label: "Example cover"
    expression: "physical:HorizontalArrangement(landcover.seed:Tree)"
    source: "https://example.org/legend/001" # Optional; falls back to catalog source.
    notes:
      - "Class-specific omitted criteria."
```

All displayed fields except the entry source are required by the YAML reader; lists may be empty
except `entries`. The legacy Java constructors remain available for programmatic catalogs without
extended provenance. Runtime registration is immutable per catalog ID: replacing a loaded catalog
requires a new provider instance or a distinct ID. Catalog revisions should update translation
version and notes; production migration of aliases needs its own review.

## Verification limits

The tests verify source-code completeness (44 and 17), parse every expression, check every leaf
against declarations in the supplied vocabulary proposal, resolve all 61 aliases, test code search
and ambiguity, preserve provenance in descriptions, exercise both known IGBP collisions and reject
malformed YAML. The fixture validates declared roles, not OWL satisfiability, actual bearer
compatibility, or faithful interpretation of official definitions. A real worldview ingestion test
and maintainer review are still needed before operational use.
