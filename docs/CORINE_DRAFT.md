# CORINE land-cover translation attempt

This document reviews `corine_2018_draft`, a tentative translation of all 44 CORINE level-3
classes into the earth-cover pattern language. It is testing material, not an official CORINE
implementation or a validated crosswalk. The machine-readable proposal is
[`corine_2018_draft.yaml`](../src/main/resources/catalogs/corine_2018_draft.yaml).

Source: [CORINE nomenclature guidelines](https://land.copernicus.eu/content/corine-land-cover-nomenclature-guidelines/html/index.html),
corresponding to [guidelines version 10, published in 2019](https://land.copernicus.eu/en/products/corine-land-cover?tab=documentation),
for the CLC 1990-2018 nomenclature. Translation revision: `draft-1`. Each YAML entry links directly
to the corresponding class page. Source names and codes are retained separately from our pattern
choices. No map products were downloaded.

See [shared seed setup and maintenance](SEED_CATALOGS.md) for loading, the proposed vocabulary,
YAML schema and the Reasoner working-list workflow. The [IGBP attempt](IGBP_DRAFT.md) discusses
its different classification criteria and the resulting comparison traps.

## Translation choices

The current `imod` checkout supplies `physical:HorizontalArrangement` and `VerticalArrangement`,
not the earlier illustrative names `HorizontalCover` and `VerticalCover`. Almost every draft class
is a horizontal arrangement of one or more proposed `landcover.seed` identities. The only vertical
example is agroforestry, intended to exercise nested configurations.

The proposed identities express a diagnostic component or functional characteristic, without specifying
its bearer. They are not CORINE class-code identities used circularly to define themselves. For example,
311 uses a broadleaved-tree component that can also be used outside CORINE; 243 combines crop and natural
vegetation components. The flat vocabulary is deliberately provisional and does not claim that broadleaf,
conifer, crop and surface categories form a scientifically adequate classification of bearers.

Artificial and agricultural classes expose a limitation of a purely material cover account. A port,
airport or recreation facility can contain many different surface materials. This draft uses broad
functional identities, which could be borne by a site or managed object. A later ontology review must
decide how those identities relate to spatial surface observations and land-use processes. The patterns
currently identify those distinctions nominally; they do not discover them from material proportions.

One argument means the diagnostic identity is present, not that the entire area is covered by it.
Multiple arguments express joint presence; they do not express an exhaustive partition, a disjunction,
equal shares or adjacency. Apart from the schematic vertical example, argument order is simply a stable
serialization choice. This choice must not be mistaken for an official interpretation of CORINE geometry.

## Choices and ambiguities by group

**Artificial surfaces (111-142).** Continuous urban fabric is represented by built surface alone.
Discontinuous urban fabric adds vegetation as a schematic mixture, without reproducing the sealing
criteria. This is an approximation that may over-require vegetation in some source interpretations;
other non-built surfaces are not represented. Industrial, transport, extraction, waste, construction
and recreation classes use functional/state identities rather than arbitrary mandatory lists of
materials. For example, a port pattern does not require water to be a component of every mapped unit.
Urban vegetation and recreation identities retain context only nominally.

**Agriculture (211-244).** Irrigation, crop type and pasture use are retained through provisional
identities. Fallow, rotation, cultivation duration, abandonment and management processes are not encoded.
Fruit/berry and crop-mosaic identities avoid requiring every possible subtype to occur together.
Natural grassland and pasture receive different identities because their use/management distinction
would disappear if both were simply grass presence. The proposal does not prove that they are disjoint.

For 243, the `+++Crop,+NaturalVegetation` pattern introduces a dominance example. The specific 3:1
central ratio is **our testing choice**, not an official conversion of a CORINE criterion. It should
be revised or removed before using the draft for scientific mediation. The absolute share and spatial
qualification remain unencoded.

For 244, a vertical arrangement puts a tree arrangement before a crop arrangement. This schematically
suggests canopy over cultivation, but not every agroforestry configuration is a strict two-layer stack.
Pasture understories, spatially separated trees and crops, management and cover limits need review.
The example is useful for parser/composition tests, not an operational classifier.

**Forests and semi-natural vegetation (311-324).** 311 is broadleaved-tree presence without forcing
an evergreen or deciduous habit. 312 is conifer-tree presence; conifer is not treated as a synonym
for evergreen. 313 requires both components without equal shares. These are distinct from IGBP's
habit-based mixed-forest description. Height, canopy closure, dominance requirements and special
regional forest cases in CORINE are absent. Thus the patterns are much broader than the source classes.

Heath, sclerophyllous vegetation and successional woody vegetation receive provisional functional
identities. Succession direction and development stage cannot be inferred from a static tree/shrub
list. The draft does not require all possible trees, shrubs and herbs in a transitional woodland.
Sparse vegetation is a placeholder identity, not an implemented cover threshold.

**Open/bare surfaces (331-335).** Sand and exposed rock retain material distinctions; beaches and
dunes lose their geomorphological distinction. Burnt surfaces retain state only nominally, with no
elapsed time since burning. Snow-or-ice is a union-like proposed identity; requiring separate snow
and ice arguments would incorrectly require both. It does not encode permanence or glacier dynamics.

**Wetlands and water (411-523).** The draft uses wetland-vegetation, sediment and water identities,
not `earth:Wetland` as a leaf: the latter is a thing in the inspected imod source. Hydrological regimes,
tides, salinity, water depth, inland/coastal context and permanence mostly remain in notes. Flowing,
standing inland, lagoon, estuarine and marine water are distinct provisional identities, but their
physical applicability and bearer hierarchy need review. Presence alone does not reproduce the mapped
extent or hydrological behavior of any of these classes.

## Expected test behavior and interpretation

All 44 source codes have legal codelist aliases and parseable expressions. No source code is itself
a canonical authority code; the canonical code is the expression. For example, search for `311` finds
`BroadLeavedForest`, whose alias points to the broadleaved-tree pattern.

Cross-catalog coincidences such as CORINE 111 and IGBP 13 both using built-surface presence are **lossy
draft matches**. They do not establish equivalence of the source classes, because omitted thresholds
and contexts differ. CORINE 313 and IGBP 5 intentionally have different component vocabularies. The
current comparator will need semantic evidence even to relate those components, and fuzzy arithmetic
alone cannot supply the missing semantic mapping.

No classification is declared inconsistent by this proposal. Audit results apply to the expressions
we wrote, not directly to the original definitions. Refining a pattern may change its canonical identity;
retain source code and translation provenance when reviewing changes.

## Review priorities

1. Replace or formally define provisional functional identities in a reviewed worldview, including
   their possible bearers and the distinction between material cover and land use.
2. Decide whether the grammar needs independent contextual predicates, disjunction, numerical bounds,
   time requirements and measurement definitions rather than hiding these distinctions in leaf names.
3. Specify configuration-local abundance policy, and revisit the arbitrary 3:1 demonstration in 243.
4. Review agroforestry's vertical schematic, forest special cases, wetland regimes and class exclusions.
5. Test the translations on reviewed examples and counterexamples before inferring interoperability.

## Entry inventory and individual losses

The following inventory repeats the proposed component choices and per-entry omission notes from the
YAML so they can be reviewed as one document. These notes describe **our translation losses**, not a
complete paraphrase of the official class definitions.

| Code / alias | Proposed components | Omitted criteria and choices |
| --- | --- | --- |
| 111 / `ContinuousUrbanFabric` | `H(BuiltSurface)` | Sealing fraction, urban use and mapping-unit rules are omitted; incidental vegetation is not required. |
| 112 / `DiscontinuousUrbanFabric` | `H(BuiltSurface,Vegetation)` | Schematic built and vegetated mixture; sealing thresholds and settlement context are omitted. |
| 121 / `IndustrialOrCommercialUnits` | `H(IndustrialOrCommercialSurface)` | Uses a provisional functional identity; associated transport, yards and detailed exclusions are omitted. |
| 122 / `RoadAndRailNetworks` | `H(RoadOrRailSurface)` | Transport function is schematic; network geometry, width and associated land are omitted. |
| 123 / `PortAreas` | `H(PortSurface)` | Port function is represented as a draft identity; adjoining water and infrastructure are optional, not required here. |
| 124 / `Airports` | `H(AirportSurface)` | Airport function only; runway dimensions, adjacent grass and ancillary units are omitted. |
| 131 / `MineralExtractionSites` | `H(ExtractionSurface)` | Extraction function only; operational state, excavation structure and substrate are omitted. |
| 132 / `DumpSites` | `H(WasteDeposit)` | Deposit presence only; waste composition, management and surface sealing are omitted. |
| 133 / `ConstructionSites` | `H(ConstructionSurface)` | Construction function only; temporal stage and disturbed-material mixtures are omitted. |
| 141 / `GreenUrbanAreas` | `H(UrbanVegetation)` | Urban vegetation identity is provisional; urban setting, public use and geometry require review. |
| 142 / `SportAndLeisureFacilities` | `H(RecreationSurface)` | Functional identity only; heterogeneous vegetation and artificial cover are unspecified. |
| 211 / `NonIrrigatedArableLand` | `H(RainfedCrop)` | Provisional management identity; rotation, fallow, irrigation exceptions and crop seasonality are omitted. |
| 212 / `PermanentlyIrrigatedLand` | `H(IrrigatedCrop)` | Irrigation is a proposed identity, not a modeled process; persistence and exceptions are omitted. |
| 213 / `RiceFields` | `H(RiceCrop)` | Crop identity only; flooding schedule and cultivation stage are omitted. |
| 221 / `Vineyards` | `H(GrapevineCrop)` | Crop identity only; planting geometry, inter-row cover and abandonment rules are omitted. |
| 222 / `FruitTreesAndBerryPlantations` | `H(FruitOrBerryCrop)` | A broad functional crop identity avoids requiring all fruit and berry types together; management rules are omitted. |
| 223 / `OliveGroves` | `H(OliveCrop)` | Crop identity only; tree spacing, understory and abandonment rules are omitted. |
| 231 / `Pastures` | `H(PastureVegetation)` | Pasture use is distinct from natural grassland in the draft vocabulary; duration and management criteria are omitted. |
| 241 / `AnnualCropsWithPermanentCrops` | `H(AnnualCrop,PermanentCrop)` | A schematic mixture; parcel relationships, crop cycle and area requirements are omitted. |
| 242 / `ComplexCultivationPatterns` | `H(CropMosaic)` | A broad mosaic identity avoids falsely requiring particular crop types; spatial grain and composition need review. |
| 243 / `AgricultureWithNaturalVegetation` | `H(+++Crop,+NaturalVegetation)` | Plus counts illustrate agriculture dominance only; 3:1 is a testing choice, not an official ratio or threshold. |
| 244 / `AgroForestryAreas` | `V(H(Tree),H(Crop))` | Tentative canopy-over-crop nesting exercises the grammar; management, spatial association and non-crop understories need review. |
| 311 / `BroadLeavedForest` | `H(BroadleavedTree)` | Tree height, canopy closure and broadleaf share above 75 percent are omitted; an evergreen or deciduous habit is not required. |
| 312 / `ConiferousForest` | `H(ConiferTree)` | Tree height, canopy closure and conifer share above 75 percent are omitted; conifer is not equated to evergreen. |
| 313 / `MixedForest` | `H(BroadleavedTree,ConiferTree)` | Both tree groups are present; their admissible shares and forest structure are omitted. Unmarked does not mean equal shares. |
| 321 / `NaturalGrassland` | `H(NaturalHerbaceousVegetation)` | Natural herbaceous identity only; productivity, use, woody cover and local interpretation rules are omitted. |
| 322 / `MoorsAndHeathland` | `H(HeathVegetation)` | Broad heath identity; dwarf-shrub composition, climate and height criteria are omitted. |
| 323 / `SclerophyllousVegetation` | `H(SclerophyllousVegetation)` | Functional vegetation identity only; shrub structure, evergreen habit and regional rules need review. |
| 324 / `TransitionalWoodlandShrub` | `H(SuccessionalWoodyVegetation)` | Successional state is provisional; no specific tree-shrub mixture or succession direction is imposed. |
| 331 / `BeachesDunesSands` | `H(SandSurface)` | Sand presence only; beach and dune geomorphology, vegetation limits and coastal context are omitted. |
| 332 / `BareRock` | `H(RockSurface)` | Exposed-rock identity only; vegetation limits and substrate exceptions are omitted. |
| 333 / `SparselyVegetatedAreas` | `H(SparseVegetation)` | Sparsity is a placeholder identity pending quantitative constraints; substrate and cover ranges are omitted. |
| 334 / `BurntAreas` | `H(BurntVegetationSurface)` | Burn state is provisional; elapsed time, residual cover and fire history are omitted. |
| 335 / `GlaciersAndPerpetualSnow` | `H(SnowOrIce)` | Union-like snow-or-ice identity avoids requiring both; persistence and glacier dynamics are omitted. |
| 411 / `InlandMarshes` | `H(InlandMarshVegetation)` | Marsh vegetation identity only; hydrology, salinity, flood duration and inland setting need review. |
| 412 / `Peatbogs` | `H(PeatFormingVegetation)` | Peat-forming vegetation only; existing peat, trophic conditions and hydrological regime are omitted. |
| 421 / `SaltMarshes` | `H(SaltMarshVegetation)` | Salt-marsh identity only; tidal regime, salinity and coastal setting are omitted. |
| 422 / `Salines` | `H(SaltExtractionSurface)` | Extraction function only; evaporative basins, water management and operational state are omitted. |
| 423 / `IntertidalFlats` | `H(IntertidalSediment)` | Intertidal sediment identity only; inundation timing and vegetation exclusions are omitted. |
| 511 / `WaterCourses` | `H(FlowingWater)` | Flow identity only; width, channel continuity and natural versus artificial origins are omitted. |
| 512 / `InlandWaterBodies` | `H(StandingInlandWater)` | Standing inland water identity only; size, salinity and artificial reservoirs need review. |
| 521 / `CoastalLagoons` | `H(LagoonWater)` | Lagoon identity only; isolation from the sea, salinity and coastal geometry are omitted. |
| 522 / `Estuaries` | `H(EstuarineWater)` | Estuarine identity only; river-sea mixing, tides and geometry are omitted. |
| 523 / `SeaAndOcean` | `H(MarineWater)` | Marine-water identity only; coastal boundaries and mapping-unit rules are omitted. |

In this inventory, `H` abbreviates `physical:HorizontalArrangement`, `V` abbreviates
`physical:VerticalArrangement`, and unqualified leaf names belong to `landcover.seed`.
