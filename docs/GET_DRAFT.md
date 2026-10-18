# GET ecosystem-typology assessment and pilot

This is an initial community-review assessment of the IUCN Global Ecosystem Typology (GET), with
level 3 Ecosystem Functional Groups (EFGs) as the global target. It is paired with the
[EUNIS assessment](EUNIS_DRAFT.md), the [WEED comparison and workbook review](ECOSYSTEM_INTEROPERABILITY.md),
and [pattern evolution proposal](ECOSYSTEM_PATTERN_EVOLUTION.md).

## Sources, editions and scope

GET is one hierarchical typology with evolving editions, not several interchangeable code systems.
The [2020 GET 2.0 publication](https://doi.org/10.2305/iucn.ch.2020.13.en) and the
[2022 function-based typology paper and GET 2.1 profiles](https://doi.org/10.1038/s41586-022-05318-4)
are distinct source editions. The maintained profile export used here identifies itself as **2.1,
June 2022, including minor updates through September 2025**, exported on **1 April 2026**.
It was retrieved from the [GET vocabulary maintainers' repository](https://github.com/Ecosystem-Indicators-Workflows/IUCN-GET-vocabulary)
at commit `cadcd1c0e323e23751b660ce37431a133ce33bf5`. Its exact download URL and SHA-256 are preserved
in the [source manifest](../src/main/resources/reference/ecosystem-source-manifest.json).
The export is a maintained research input, not a claim that the associated beta linked-data vocabulary
is a newly ratified classification edition. Indicative distribution-map versions must also be recorded
separately from profile versions.

The upper levels distinguish realms, functional biomes and EFGs. The lower levels retain regional and
compositional differentiation, linking the global functional framework to local ecosystem types.
Terminology for lower levels should be checked against the chosen publication; a local vegetation
association or national ecosystem class is not automatically another level-3 EFG. A level number
in GET is not equivalent to the same level number in EUNIS.

The source contains 110 English EFG records. All are indexed in
[`get21-level3.json`](../src/main/resources/reference/get21-level3.json), including source sheet/row,
realm and biome. The realm inventory includes the atmospheric core realm even though this export
has no atmospheric level-3 EFG record. This assessment does not invent such records to fill a hierarchy.
The source-index resource is not a runtime codelist and contains no completed semantic definitions.

The runtime pilot, [`get21_ecosystem_pilot.yaml`](../src/main/resources/catalogs/get21_ecosystem_pilot.yaml),
contains **29 selected EFGs**, not all 110. It includes terrestrial, freshwater, marine, transitional
and groundwater examples. Selection emphasizes differences likely to affect WEED testing and future
unification. Offshore pelagic, chemosynthetic and sea-ice examples test generality beyond the
[WEED project's stated terrestrial, freshwater and coastal-to-intertidal mapping scope](https://esa-worldecosystems.org/en/about-weed).

## What makes an ecosystem functional group

GET's organizing principle is not simply a list of visible cover components or dominant taxa.
Its functional profiles connect ecosystem properties to resource supply, environmental filters,
disturbance, biotic interactions and human activities. Source drivers can be causal explanations,
characteristic tendencies or mechanisms, rather than individually necessary membership thresholds.
Converting every descriptive sentence into an AND condition would overconstrain the class.

The proposed patterns therefore keep composition separate from prototype facet configurations:
`Climate`, `Hydrology`, `Connectivity`, `Substrate`, `Management`, `Disturbance`, `EnergyRegime`,
`BioticTraits`, `WaterColumn` and related contexts. They live in the authorized experimental
`ecosystem.seed` namespace and do not purport to be the current imod ontology. Their categorical
arguments are invented identity prototypes. They preserve distinctions for testing without pretending
to supply measured thresholds, inferred bearers or executable causal models.

A profile such as the deciduous temperate-forest example can already be expressed using the generic
functor grammar. Numeric bounds, evidence modality and measurement support still need a richer rule
representation. The current structural comparator cannot infer full EFG compatibility from these
prototype facet names.

## Critical choices and ambiguities

**Forests and grasslands.** T1.1 retains humid tropical/subtropical and lowland context with a broad
evergreen-tree component. It is not identified by latitude alone. T2.2 retains temperate climate and
winter-deciduous habit without choosing a universal species list. Temperature seasonality, canopy
share and growing-season criteria remain unresolved. T2.5 and T2.6 both involve fire-related dynamics,
but differ in moisture, structure and resource constraints. Recent fire occurrence is not equivalent
to a fire-maintained ecosystem or its recurrence regime. T4.5 requires seasonal water-balance, soil
and grass-trait review; a global generic grassland cover is not an adequate definition.

**Bogs, fens and marshes.** TF1.6 is not reduced to strictly ombrotrophic raised bog. Its profile
covers broader peatland settings, whereas EUNIS Q11 specifically describes raised bog and Q12 blanket
bog. TF1.7 emphasizes mineral-water supply, while regional poor/rich-fen distinctions involve chemistry,
substrate and assemblages. TF1.3 and TF1.4 need inundation regime and geomorphic context rather than
wetland vegetation alone. The TF1.4 prototype includes a hydroperiod cycle with inundated and exposed
phases; no phase shares, periodicity or synchronization across an entire floodplain are assumed.

**Streams and lakes.** F1.1 is perennial upland flow, not constant discharge. F1.4 can include
perennial streams with strong seasonality as well as intermittent streams; it must not be classified
as necessarily dry for part of every year. Lake area, depth, permanence, stratification, trophic
status and salinity are independent dimensions. GET F2.6 salt/soda lakes must not be treated as one
fixed salinity or as a synonym for all saline inland water. Reservoir management and rice-paddy
management require temporal behavior and origin, not only standing water or crop cover.

**Transitional coasts.** FM1.2 and FM1.3 differ by ocean connectivity regimes and associated exchange
processes. Mouth closure and opening can recur at very different timescales. A single image cannot
establish that regime. MFT1.2 includes mangrove tree and shrub forms; the pilot uses an aggregate
prototype without requiring a tree canopy in every unit. MFT1.3 needs tidal, freshwater and salinity
gradients. A shared realm must not erase the distinctions among river-ocean, land-ocean and
land-river-ocean transitions.

**Marine benthic, pelagic and ice systems.** M1.1 includes seagrass contexts beyond a single sediment
or tidal zone. M1.2 requires more than any macroalgae. M1.3 involves reef accretion and symbiosis,
not any coral occurrence. M1.8 concerns subtidal mud with energy and food-web properties, whereas a
littoral mud class has a different tidal support. M2.1 is a photic pelagic water-column system, not
just a mapped water surface; photic depth varies. M2.5 includes a sea-ice-associated community and
coupled water/ice dynamics, not generic terrestrial snow cover. EUNIS regional sea codes can refine
geographical applicability without establishing a different global functional mechanism.

**Deep sea and groundwater.** M3.7 includes alternative chemoautotrophic settings such as vents,
seeps and organic falls; requiring all three as simultaneous components would be wrong. SF1.2 needs
aquifer/hyporheic connectivity, resources, pore space and residence time. These cases demonstrate why
a universal vegetation-cover classifier cannot serve as the ecosystem authority's full model.

## Consequences for national interoperability

Use the EFG as a shared functional comparison target, not a replacement name for national classes.
Keep national agency, edition, code, geographical support, diagnostic assemblages and interpretation
rules on the source class. A regional class may have a conditional narrower relationship, overlap
several EFGs, or remain unresolved. Record the rationale and excluded cases rather than selecting the
first globally plausible name. Environmental suitability inferred from EO is separate evidence from
an authoritative national class assignment.

The [candidate cases](data/ecosystem-comparison-cases.json) are review hypotheses, not an official
GET-EUNIS crosswalk or automatically asserted ontology subsumptions. They include deliberately
non-equivalent cases and identify the criteria needed to judge each relationship.

## Pilot inventory and remaining work

The table below lists every selected source code, its proposed alias and the main unresolved issue.
All are ECOSYSTEM-domain patterns, selected explicitly at bridge configuration. The complete source
inventory remains separate. Numerical abundance or duration ratios are not guessed from profile prose.


### get21_ecosystem_pilot

| Source code / level | Pilot alias | Main unresolved criterion |
| --- | --- | --- |
| T1.1 / 3 | `LowlandRainforest` | Water balance, layered canopy, productivity and resource cycling need quantified and causal rules; tropical is not a latitude-only test. |
| T2.2 / 3 | `DeciduousTemperateForest` | Canopy share, temperature seasonality, growing season and soil fertility remain unquantified; no tree species is universally required. |
| T2.5 / 3 | `TemperatePyricHumidForest` | Fire regime, tall multilayer structure and summer drought episodes need explicit recurrence, severity and succession semantics. |
| T2.6 / 3 | `TemperatePyricSclerophyllForest` | Quantified water deficit, nutrients and fire intervals are not encoded; a recent burn is not evidence of a pyric ecosystem by itself. |
| T4.5 / 3 | `TemperateSubhumidGrassland` | Seasonal water balance, C3/C4 traits, soil properties, disturbance and tree-exclusion mechanisms need review. |
| T7.1 / 3 | `AnnualCroplandEcosystem` | Annual management cycle and resource subsidies need temporal rules; high-input practice is not a universal numeric threshold here. |
| TF1.3 / 3 | `PermanentMarsh` | Water depth, anoxia, nutrients and marsh physiognomy remain qualitative; permanent inundation is not exhaustive water cover. |
| TF1.4 / 3 | `SeasonalFloodplainMarsh` | Flooding then exposure is a schematic recurring cycle; durations, gaps, spatial extent and interannual variation are unspecified. No equal phase length is assumed. |
| TF1.6 / 3 | `BorealTemperateMontanePeatBog` | GET bogs must not all be narrowed to strictly ombrotrophic raised bogs; peat chemistry and formation feedbacks need separate constraints. |
| TF1.7 / 3 | `BorealTemperateFen` | Mineral-water input, productivity, anoxia and water-table regime matter; rich and poor fens must remain possible regional refinements. |
| F1.1 / 3 | `PermanentUplandStream` | Stream order, slope, velocity, oxygen and catchment subsidies are not executable; perennial is not constant discharge. |
| F1.4 / 3 | `SeasonalUplandStream` | This GET group may include perennial flows as well as intermittent streams. Seasonal must not be interpreted as obligatory dry-channel absence. |
| F2.1 / 3 | `LargePermanentFreshwaterLake` | Area, depth, stratification and mixing need explicit units and observation scales; large is an uncalibrated prototype identity. |
| F2.6 / 3 | `PermanentSaltAndSodaLake` | Salinity, ionic composition and alkalinity need independent measurements; not all saline lakes are soda lakes. |
| F3.1 / 3 | `LargeReservoir` | Water-level operation, residence time, impoundment history and size require context and temporal rules. |
| F3.3 / 3 | `RicePaddyEcosystem` | Management and inundation cycles vary; the prototype does not impose year-round flooding or a universal wet/dry duration ratio. |
| FM1.2 / 3 | `PermanentlyOpenEstuary` | Salinity gradients, exchange, freshwater inflow, tides and residence time are more informative than a single brackish identity. |
| FM1.3 / 3 | `IntermittentlyOpenCoastalLagoon` | Mouth closure cycles can be infrequent or frequent; phase durations and triggers cannot be inferred from the name. |
| MFT1.2 / 3 | `IntertidalForestShrubland` | The aggregate mangrove identity includes tree and shrub forms; salinity, thermal limits and sediment dynamics need constraints. |
| MFT1.3 / 3 | `CoastalSaltmarshReedbed` | Freshwater inputs, halophyte traits, tidal exposure and microtopography need explicit scales; a universal salinity value is inappropriate. |
| MT1.1 / 3 | `RockyShorelineEcosystem` | Zonation, exposure duration and sessile/mobile biota are omitted; substrate presence does not define the full community. |
| M1.1 / 3 | `SeagrassMeadow` | Meadow structure, light, salinity and sediment stabilization need rules; GET includes intertidal examples and occasional rocky substrates. |
| M1.2 / 3 | `KelpForest` | Light, nutrients, temperature, substrate attachment and herbivory/trophic cascades require more than canopy identity. |
| M1.3 / 3 | `PhoticCoralReef` | Symbiosis, light-depth-temperature envelope, salinity and accretion must be modeled; all corals do not define a photic reef. |
| M1.8 / 3 | `SubtidalMudPlain` | Deposition, benthic food-web subsidies, oxygen and organic matter need measurable constraints. |
| M2.1 / 3 | `EpipelagicOcean` | Photic depth depends on turbidity and season, not a universally fixed depth; pelagic and benthic contexts must remain distinct. |
| M2.5 / 3 | `SeaIceEcosystem` | Sea ice is not terrestrial snow; freezing, brine channels, seasonal retreat and connected water communities need temporal structure. |
| M3.7 / 3 | `ChemosyntheticEcosystem` | Vents, cold seeps and organic falls are alternatives, not mandatory co-occurring components. Pressure and donor chemistry require constraints. |
| SF1.2 / 3 | `GroundwaterEcosystem` | Aquifer and hyporheic connectivity, residence time, pore space and oxygen need rules; microbial and animal communities vary. |

## Complete level-3 source index

This is an inventory, not a claim that all profiles have been translated. “Pending” records have no runtime definition in this pilot.

| Code | Source label | Prototype coverage |
| --- | --- | --- |
| F1.1 | Permanent upland streams | Selected pilot |
| F1.2 | Permanent lowland rivers | Pending |
| F1.3 | Freeze-thaw rivers and streams | Pending |
| F1.4 | Seasonal upland streams | Selected pilot |
| F1.5 | Seasonal lowland rivers | Pending |
| F1.6 | Episodic arid rivers | Pending |
| F1.7 | Large lowland rivers | Pending |
| F2.1 | Large permanent freshwater lakes | Selected pilot |
| F2.10 | Subglacial lakes | Pending |
| F2.2 | Small permanent freshwater lakes | Pending |
| F2.3 | Seasonal freshwater lakes | Pending |
| F2.4 | Freeze-thaw freshwater lakes | Pending |
| F2.5 | Ephemeral freshwater lakes | Pending |
| F2.6 | Permanent salt and soda lakes | Selected pilot |
| F2.7 | Ephemeral salt lakes | Pending |
| F2.8 | Artesian springs and oases | Pending |
| F2.9 | Geothermal pools and wetlands | Pending |
| F3.1 | Large reservoirs | Selected pilot |
| F3.2 | Constructed lacustrine wetlands | Pending |
| F3.3 | Rice paddies | Selected pilot |
| F3.4 | Freshwater aquafarms | Pending |
| F3.5 | Canals, ditches and drains | Pending |
| FM1.1 | Deepwater coastal inlets | Pending |
| FM1.2 | Permanently open riverine estuaries and bays | Selected pilot |
| FM1.3 | Intermittently closed and open lakes and lagoons | Selected pilot |
| M1.1 | Seagrass meadows | Selected pilot |
| M1.10 | Rhodolith/Maërl beds | Pending |
| M1.2 | Kelp forests | Selected pilot |
| M1.3 | Photic coral reefs | Selected pilot |
| M1.4 | Shellfish beds and reefs | Pending |
| M1.5 | Photo-limited marine animal forests | Pending |
| M1.6 | Subtidal rocky reefs | Pending |
| M1.7 | Subtidal sand beds | Pending |
| M1.8 | Subtidal mud plains | Selected pilot |
| M1.9 | Upwelling zones | Pending |
| M2.1 | Epipelagic ocean waters | Selected pilot |
| M2.2 | Mesopelagic ocean water | Pending |
| M2.3 | Bathypelagic ocean waters | Pending |
| M2.4 | Abyssopelagic ocean waters | Pending |
| M2.5 | Sea ice | Selected pilot |
| M3.1 | Continental and island slopes | Pending |
| M3.2 | Submarine canyons | Pending |
| M3.3 | Abyssal plains | Pending |
| M3.4 | Seamounts, ridges and plateaus | Pending |
| M3.5 | Deepwater biogenic beds | Pending |
| M3.6 | Hadal trenches and troughs | Pending |
| M3.7 | Chemosynthetic-based-ecosystems (CBE) | Selected pilot |
| M4.1 | Submerged artificial structures | Pending |
| M4.2 | Marine aquafarms | Pending |
| MFT1.1 | Coastal river deltas | Pending |
| MFT1.2 | Intertidal forests and shrublands | Selected pilot |
| MFT1.3 | Coastal saltmarshes and reedbeds | Selected pilot |
| MT1.1 | Rocky Shorelines | Selected pilot |
| MT1.2 | Muddy Shorelines | Pending |
| MT1.3 | Sandy Shorelines | Pending |
| MT1.4 | Boulder and cobble shores | Pending |
| MT2.1 | Coastal shrublands and grasslands | Pending |
| MT2.2 | Large seabird and pinniped colonies | Pending |
| MT3.1 | Artificial shorelines | Pending |
| S1.1 | Aerobic caves | Pending |
| S1.2 | Endolithic systems | Pending |
| S2.1 | Anthropogenic subterranean voids | Pending |
| SF1.1 | Underground streams and pools | Pending |
| SF1.2 | Groundwater ecosystems | Selected pilot |
| SF2.1 | Water pipes and subterranean canals | Pending |
| SF2.2 | Flooded mines and other voids | Pending |
| SM1.1 | Anchialine caves | Pending |
| SM1.2 | Anchialine pools | Pending |
| SM1.3 | Sea caves | Pending |
| T1.1 | Tropical/Subtropical lowland rainforests | Selected pilot |
| T1.2 | Tropical/Subtropical dry forests and thickets | Pending |
| T1.3 | Tropical/Subtropical montane rainforests | Pending |
| T1.4 | Tropical heath forests | Pending |
| T2.1 | Boreal and temperate high montane forests and woodlands | Pending |
| T2.2 | Deciduous temperate forests | Selected pilot |
| T2.3 | Oceanic cool temperate rainforests | Pending |
| T2.4 | Warm temperate laurophyll forests | Pending |
| T2.5 | Temperate pyric humid forests | Selected pilot |
| T2.6 | Temperate pyric sclerophyll forests and woodlands | Selected pilot |
| T3.1 | Seasonally dry tropical shrublands | Pending |
| T3.2 | Seasonally dry temperate heath and shrublands | Pending |
| T3.3 | Cool temperate heathlands | Pending |
| T3.4 | Young rocky pavements, lava flows and screes | Pending |
| T4.1 | Trophic savannas | Pending |
| T4.2 | Pyric tussock savannas | Pending |
| T4.3 | Hummock savannas | Pending |
| T4.4 | Temperate woodlands | Pending |
| T4.5 | Temperate subhumid grasslands | Selected pilot |
| T5.1 | Semi-desert steppe | Pending |
| T5.2 | Succulent or Thorny deserts and semi-deserts | Pending |
| T5.3 | Sclerophyll hot deserts and semi-deserts | Pending |
| T5.4 | Cool deserts and semi-deserts | Pending |
| T5.5 | Hyper-arid deserts | Pending |
| T6.1 | Ice sheets, glaciers and perennial snowfields | Pending |
| T6.2 | Polar/alpine cliffs, screes, outcrops and lava flows | Pending |
| T6.3 | Polar tundra and deserts | Pending |
| T6.4 | Temperate alpine grasslands and shrublands | Pending |
| T6.5 | Tropical alpine grasslands and herbfields | Pending |
| T7.1 | Annual croplands | Selected pilot |
| T7.2 | Sown pastures and fields | Pending |
| T7.3 | Plantations | Pending |
| T7.4 | Urban and industrial ecosystems | Pending |
| T7.5 | Derived semi-natural pastures and old fields | Pending |
| TF1.1 | Tropical flooded forests and peat forests | Pending |
| TF1.2 | Subtropical/temperate forested wetlands | Pending |
| TF1.3 | Permanent marshes | Selected pilot |
| TF1.4 | Seasonal floodplain marshes | Selected pilot |
| TF1.5 | Episodic arid floodplains | Pending |
| TF1.6 | Boreal, temperate and montane peat bogs | Selected pilot |
| TF1.7 | Boreal and temperate fens | Selected pilot |
