# IGBP land-cover translation attempt

This document reviews `igbp_modis61_draft`, a tentative translation of the 17 IGBP classes in
MODIS MCD12Q1 `LC_Type1`. It is testing material, not an official IGBP implementation or a validated
crosswalk. The machine-readable proposal is
[`igbp_modis61_draft.yaml`](../src/main/resources/catalogs/igbp_modis61_draft.yaml).

Source: [MODIS Collection 6.1 user guide, table 3](https://lpdaac.usgs.gov/documents/1409/MCD12_User_Guide_V61.pdf#page=9),
20 April 2022. Translation revision: `draft-1`. The scope is specifically MCD12Q1: water is code 17.
Water-0 conventions in other MODIS legends/products must not be substituted. Code 255 denotes missing
input and is intentionally excluded: no-data is not a kind of cover. No map products were downloaded.

See [shared seed setup and maintenance](SEED_CATALOGS.md) for loading, the proposed vocabulary,
YAML schema and the Reasoner working-list workflow. The [CORINE attempt](CORINE_DRAFT.md) records
its different choices and omissions.

## Translation choices

Each class becomes a `physical:HorizontalArrangement` of proposed `landcover.seed` identities. Those
configuration names come from the inspected imod source. The component identities do not currently
exist in that worldview; the companion `.kwv` is a test-only vocabulary proposal and must be reviewed
and loaded separately. It intentionally leaves bearers unspecified.

The draft records central compositional characteristics where possible. It does not convert absolute
cover thresholds to plus counts. A fraction of an entire mapping unit cannot generally be expressed
as a ratio to another explicitly present component when the remainder is unspecified. Adding a fake
background component would introduce a requirement that the source legend did not necessarily make.
No fuzzy arithmetic profile or abundance tolerance is selected.

Unmarked components mean their relative shares are unspecified. They do not imply a 50:50 mixture.
Positive presence also does not mean complete occupancy, dominance, or a vegetation height. These
limitations are why several source classes collapse to the same expression below.

## Forests: classes 1-5

Classes 1-4 retain combinations of leaf habit and leaf form as provisional component identities.
The broadleaf-evergreen entry explicitly permits palms through a union-like identity. This is a
vocabulary proposal, not an OR operator in the pattern language. Leaf-form/habit combinations may later
need a proper trait composition API rather than a growing collection of compound identity names.

Class 5 follows the guide's evergreen/deciduous mixture. It is deliberately **not** translated as the
broadleaf/conifer mixture used for CORINE 313. The similar label “mixed forest” therefore does not
justify selecting a shared pattern or reconciling an alias without naming its catalog.

Tree height, total tree cover and the mixture's stated admissible shares remain omitted-criteria notes.
The draft's forest expressions could match isolated qualifying trees; they are much broader than a
forest class definition. Neither the flat fixture vocabulary nor the existing comparator establishes
hierarchies among the forest components.

## Shrublands and savannas: classes 6-9

Classes 6 and 7 both become shrub presence. The source distinguishes them numerically, so this is a
known **translation collision**. The same applies to classes 8 and 9, represented as tree presence
without their different cover ranges. Tree/shrub height and persistence also remain unencoded.

The patterns do not require grass in savannas merely because that is a common ecological association.
The cited class descriptions distinguish these classes by tree cover; this draft avoids imposing an
unsupported mandatory understory. A more complete ecological description could be provided separately,
with its own evidence and measurement model.

After alias deduplication, these two pairs account for 17 source aliases mapping to only **15 distinct
pattern identities**. Resolution retains both source labels and their notes. Auditing reports the
same structural expression for each pair; this is an expected test of information loss, not a finding
that IGBP is inconsistent. Different numeric thresholds must eventually remain distinct constraints
rather than being hidden behind names such as “open” and “closed”.

## Other classes: 10-17

**Grasslands (10).** The proposed identity follows this guide's emphasis on herbaceous annuals; it
is not asserted to cover every ecological meaning of grassland. The height requirement is absent.
How to reconcile this definition with perennial natural grassland needs semantic review rather than
assuming that matching source labels denote equivalent communities.

**Permanent wetlands (11).** The draft requires wetland vegetation and surface water, without equal
shares. Their simultaneous presence is only a schematic account of a permanently inundated area.
Duration and absolute fractions are missing; a shared hydrological observation, extent and temporal
interpretation must be specified before making this executable.

**Croplands (12) and urban cover (13).** The draft retains cultivated crop and built-surface identity,
respectively. It omits minimum cultivated or impervious share. Built surface is a provisional proxy
that needs review against an actual imperviousness measurement model; it should not silently identify
all human structures as impervious. Coarse matches to CORINE classes are not validated crosswalks.

**Crop/natural-vegetation mosaics (14).** The two components are required, but their stated share,
small-scale spatial structure, and possible natural vegetation forms are not encoded. An unmarked
pair is not an equality requirement. No particular natural tree, shrub or herb subtype is required.

**Snow/ice, barren and water (15-17).** A union-like snow-or-ice identity avoids requiring both snow
and ice. Barren surface similarly avoids requiring sand, rock and soil together. Permanence, absolute
share, vegetation limits and seasonality remain prose. `SurfaceWater` is broad and needs review for
water-body extent and permanence; it is not yet a semantically constrained hydrological model.

## Comparison traps and open decisions

- Same class labels can hide different criteria across legends, as with mixed forest.
- Same expressions can hide omitted numerical distinctions, as with shrublands and savannas.
- Absolute cover and relative abundance have different denominators; plus marks must not silently
  turn one into the other or normalize over an assumed exhaustive list.
- Compound identities and union-like names keep this grammar small but shift work into the ontology.
  Their definitions and bearers need review before reasoning can establish useful cross-class relations.
- An overlap or compatibility result requires matching spatial/temporal extents and measurement meaning,
  not only an arithmetic tolerance. Missing inputs must stay distinct from incompatible patterns.

The mediation specification should resolve these issues together with its per-bridge fuzzy policy.
No tolerance or compatibility score has been chosen in this proposal.

## Review priorities

1. Represent numerical cover ranges, canopy height and persistence as explicit constraints, with
   documented measurement and comparison semantics.
2. Replace or validate the provisional component identities in a loaded worldview and add appropriate
   semantic relationships without equating conifer, needleleaf and evergreen by assumption.
3. Restore distinct pattern identities for classes 6/7 and 8/9 once their distinguishing constraints
   are expressible; keep their source codes and provenance stable during migration.
4. Review class 5 against CORINE 313 using actual constituent semantics, and test positive/negative
   examples of each remaining class before claiming source-class compatibility.

## Entry inventory and individual losses

The following inventory repeats the proposed components and omission notes from the YAML. Numerical
criteria in those notes are not executable constraints. These are our translation losses, not complete
reproductions of the official source definitions.

| Code / alias | Proposed components | Omitted criteria and choices |
| --- | --- | --- |
| 1 / `EvergreenNeedleleafForest` | `H(EvergreenNeedleleafTree)` | Height above 2 m and tree cover above 60 percent are omitted. |
| 2 / `EvergreenBroadleafForest` | `H(EvergreenBroadleafOrPalmTree)` | Includes palms in a union-like identity; height above 2 m and cover above 60 percent are omitted. |
| 3 / `DeciduousNeedleleafForest` | `H(DeciduousNeedleleafTree)` | Height above 2 m and tree cover above 60 percent are omitted. |
| 4 / `DeciduousBroadleafForest` | `H(DeciduousBroadleafTree)` | Height above 2 m and tree cover above 60 percent are omitted. |
| 5 / `MixedForest` | `H(EvergreenTree,DeciduousTree)` | Leaf habit mixture, not the CORINE broadleaf-conifer mixture; 40-60 percent shares and forest thresholds are omitted. |
| 6 / `ClosedShrubland` | `H(Shrub)` | Height 1-2 m and cover above 60 percent are omitted; intentionally collapses with OpenShrubland in this draft. |
| 7 / `OpenShrubland` | `H(Shrub)` | Height 1-2 m and cover 10-60 percent are omitted; same draft pattern as ClosedShrubland. |
| 8 / `WoodySavanna` | `H(Tree)` | Height above 2 m and tree cover 30-60 percent are omitted; non-tree understory is unspecified. |
| 9 / `Savanna` | `H(Tree)` | Height above 2 m and tree cover 10-30 percent are omitted; same draft pattern as WoodySavanna. |
| 10 / `Grassland` | `H(AnnualHerbaceousVegetation)` | Height below 2 m is omitted; this source legend emphasizes herbaceous annuals. |
| 11 / `PermanentWetland` | `H(WetlandVegetation,SurfaceWater)` | Permanent inundation, 30-60 percent water and vegetation above 10 percent are omitted. |
| 12 / `Cropland` | `H(Crop)` | Cultivated fraction of at least 60 percent is omitted. |
| 13 / `UrbanAndBuiltUp` | `H(BuiltSurface)` | Impervious fraction of at least 30 percent is omitted. |
| 14 / `CroplandNaturalVegetationMosaic` | `H(Crop,NaturalVegetation)` | Cultivation share 40-60 percent and spatial grain are omitted; unmarked components are not assumed equal. |
| 15 / `PermanentSnowAndIce` | `H(SnowOrIce)` | Cover of at least 60 percent for at least 10 months is omitted. |
| 16 / `Barren` | `H(BarrenSurface)` | Barren fraction of at least 60 percent and vegetation below 10 percent are omitted. |
| 17 / `WaterBodies` | `H(SurfaceWater)` | Permanent-water cover of at least 60 percent is omitted. |

In this inventory, `H` abbreviates `physical:HorizontalArrangement`, `V` abbreviates
`physical:VerticalArrangement`, and unqualified leaf names belong to `landcover.seed`.
