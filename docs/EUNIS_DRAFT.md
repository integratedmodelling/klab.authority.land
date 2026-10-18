# EUNIS ecosystem-typology assessment and pilots

This initial assessment compares EUNIS with GET as a regional, versioned habitat framework, rather
than translating every EUNIS name into a global cover category. It accompanies the
[GET assessment](GET_DRAFT.md), [WEED comparison and workbook review](ECOSYSTEM_INTEROPERABILITY.md),
and [pattern evolution proposal](ECOSYSTEM_PATTERN_EVOLUTION.md).

## Editions must remain separate

The [EEA classification and crosswalk release page](https://www.eea.europa.eu/en/datahub/datahubitem-view/638330ea-90e6-4e41-81ea-e70f25ae7117/)
distributes separate terrestrial-2021, marine-2022 and legacy-2012 resources. This change retrieved
those source tables on 18 October 2026 and preserves exact download hashes in the
[source manifest](../src/main/resources/reference/ecosystem-source-manifest.json).

| Source used | Reference inventory | Runtime pilot |
| --- | --- | --- |
| Terrestrial `2021_1` workbook, seven habitat-group sheets | 270 level-3 records | 18 selected classes |
| Marine 2022 workbook, benthic, pelagic and ice-associated sheets | 249 level-3 records | 14 selected classes: 10 at level 3 and 4 seagrass/community records at level 4 |
| EUNIS 2007 classification, descriptions revised 2012/amended 2019, inland surface-water branch C | 21 level-3 inland-water records | 8 selected level-3 classes |

The terrestrial count refers to that workbook, not all terrestrial/freshwater/artificial EUNIS
habitats. Its man-made sheet retains 2018 column headings, and the tables retain source/crosswalk
history. We must not silently call every underlying definition new in 2021. The revised terrestrial
workbook does not provide a complete replacement for legacy inland surface-water branch C; hence
that branch has its own pilot and provenance. The old marine A codes are not used as aliases for
new marine codes unless a checked relationship says so.

Sources are preserved in normalized reference files under `src/main/resources/reference/`, with
code, exact label, declared level, sheet, row and marine sea-region marker where provided. The four
deeper marine examples are separately identified, not smuggled into the level-3 inventory. Code
length is not a reliable universal level rule: `MA522` and `MB252` are level 4 in the source table.

## EUNIS and EU legal classifications

EUNIS is widely used in European habitat description and policy, but it is not identical to the
Habitats Directive Annex I list. The EEA distributes crosswalks rather than asserting blanket identity.
A concrete legal use is [Regulation (EU) 2024/1991, Annex II](https://eur-lex.europa.eu/eli/reg/2024/1991/oj/eng),
which specifies the 2022 marine EUNIS typology and lists related Annex I habitat codes. This gives a
specific edition and context, not a reason to treat every EUNIS code as a legally mandated Annex I
type. Policy-specific reporting lists, assessment units and typology semantics should remain distinct.
The pilots do not implement legal reporting obligations or infer Annex I eligibility.

## Shared ecosystem framework, distinct regional provenance

The common `ecosystem.seed:Ecosystem` wrapper separates composition from hydrology, substrate,
management, biogeography and other categorical facets. This supports both terrestrial and marine
examples without forcing them into a vegetation-based hierarchy. Invented facet identities are
prototypes, not validated present-day k.LAB concepts or executable numeric classifications.

EUNIS often combines characteristic assemblages, environmental setting and regional differentiation
at finer compositional scales than GET EFGs. Source levels cannot be aligned just by number. A class
name containing a taxon may identify a community or diagnostic assemblage rather than merely the
presence of that taxon. Dominant, constant, differential and indicator species must not all become
mandatory conjunctive leaves. The pilot deliberately does not invent taxonomy authority codes.

## Terrestrial cases

**Coastal substrate and regional context.** N11 and N12 share sand but differ in regional applicability.
That distinction is retained in the prototype biogeography facet. They are not interchangeable labels,
and different source regions do not imply incompatible functional ecosystems. Coastal-terrestrial N
habitats and marine littoral M habitats may meet at boundaries that need explicit tidal/spatial support.

**Grassland and heath.** R1A carries calcareous and semi-dry context, not just grass presence. R1M's
usually dominant Nardus is not automatically a universal necessary taxon. R21 includes pasture use;
S41/S42 differ by wet/dry setting. A single moisture observation, generic grass cover or a guessed
pH threshold would not reproduce these classes. Local diagnostic species, syntaxa and management
history must be retained when mapping national typologies.

**Forest soils and assemblages.** T17 and T18 share beech composition while differing in soil acidity
and associated vegetation. Flattening both to beech cover would erase the source distinction. T11
represents a willow-or-poplar riparian context without requiring both genera simultaneously. T21
retains Mediterranean evergreen-oak context. No universal species, soil pH or canopy threshold is
invented, and the prototype source relationship to GET T2.2 is conditional rather than an asserted
subsumption.

**Peatlands and management.** Q11 raised and Q12 blanket bog remain distinct in landform and water
supply. Q22 poor fen and Q41 calcareous spring fen require mineral-water, nutrient/base-status and
assemblage interpretation; poor is not an ecosystem-condition rating. Q51 tall-helophyte beds need
not exactly match a GET permanently inundated marsh. V11 and V31 retain management distinctions;
V31 includes lawns and sports fields, so the pilot avoids requiring grazing or agricultural pasture.

## Legacy inland-water cases

C1.1-C1.3 distinguish trophic status within permanent standing water. A trophic category does not
by itself indicate degradation or a pressure source. C1.5 includes saline and brackish inland waters;
it is not automatically a soda-lake EFG or an estuary. C1.6 includes temporary standing-water regimes
that require duration and recurrence descriptions.

C2.2/C2.3 distinguish fast/turbulent versus smooth perennial non-tidal flow. Those properties do
not determine stream order or upland/lowland context alone. C2.5 temporary running water cannot be
blindly equated to GET F1.4 seasonal upland streams, which may remain perennial. Their extent,
observation window and criterion meanings must be examined before suggesting overlap or refinement.

## Marine cases

**Benthic structure and region.** MA12/MA15 share littoral rock and differ in region. MA52/MA62
retain sand/mud differences. MB12/MB15 are infralittoral rock, not automatically kelp or coral
ecosystems. The prototype keeps tidal/bathymetric setting independent of substrate, assemblage
and biogeography. A mapped bottom habitat is not the whole three-dimensional water-column ecosystem.

**Seagrass communities.** The four selected level-4 records are MA522, MA623, MB522 and MB252.
They provide concrete comparisons with GET M1.1 while retaining Atlantic/Mediterranean, substrate,
tidal context or a named species community. The invented `PosidoniaOceanica` identity is a temporary
stand-in; no external taxon code has been guessed. These records also illustrate the marine
Nature Restoration Regulation context, but that is not an implemented reporting crosswalk.

**Pelagic interfaces and mixing.** MH12 neuston is an air-sea-interface community. It must not be
made equivalent to GET's whole epipelagic ocean group. MH22/MH32 differ in reduced/full salinity
within completely mixed Atlantic water columns. Their source-relative terms need calibrated
measurements and temporal support, not a globally fixed salinity number. These processes may overlap
other functional groupings, so a one-to-one crosswalk is generally too strong.

**Ice-associated ecosystems.** MJ11 is Arctic sea ice, including biological and temporal context.
It is not terrestrial snow, nor simply a frozen pixel. Relation to GET M2.5 needs regional support,
seasonal dynamics and coupled water/ice community interpretation. The same common framework can
represent it while its marine source edition remains explicit.

## Assessment and maintenance

No pilot relationship is an official EEA or IUCN crosswalk. See the
[comparison cases](data/ecosystem-comparison-cases.json) for review questions and missing evidence.
Different source geography, hierarchy position and profile modality must not become fabricated
contradictions. Conversely, visually similar cover must not remove floristic, hydrological or
regional distinctions that matter to national communities.

The pilots are selected explicitly using `domain: ECOSYSTEM`. Provider seeds remain read-only;
maintainers can create editable working-list aliases through the existing Reasoner facilities as
explained in [seed maintenance](SEED_CATALOGS.md). No source table, original workbook, worldview or
running Reasoner ledger was modified by this assessment.

For a test worldview, load the proposed `landcover.seed` and `ecosystem.seed` namespaces and replace
the latter's plain `identity EcosystemClass;` declaration with a binding such as:

```text
identity EcosystemClass requires authority ECOSYSTEM {
    urn: "klab.authority.land",
    domain: "ECOSYSTEM",
    catalogs: ["get21_ecosystem_pilot", "eunis2021_terrestrial_pilot",
               "eunis2022_marine_pilot", "eunis2012_inland_pilot"],
    codelists: {
        get21_ecosystem_pilot: "ecosystems.get",
        eunis2021_terrestrial_pilot: "ecosystems.eunis_terrestrial",
        eunis2022_marine_pilot: "ecosystems.eunis_marine",
        eunis2012_inland_pilot: "ecosystems.eunis_inland",
        ecosystem_working: "ecosystems.working"
    }
};
```

`ecosystem_working` is an empty managed/community working list, not a provider catalog selection.
The host still needs to bind semantic access on the actual provider instance as documented in the
README. No real worldview ingestion or dashboard modification was performed by the fixture tests.

## Selected entry inventory

The following tables list every pilot entry and the main remaining interpretation issue. Full source
inventories remain separate from the partial runtime definitions.


### eunis2021_terrestrial_pilot

| Source code / level | Pilot alias | Main unresolved criterion |
| --- | --- | --- |
| N11 / 3 | `NorthernSandBeach` | Regional disjunction is a prototype category; shore processes, tidal setting and community context require review. |
| N12 / 3 | `SouthernSandBeach` | Same material as N11 with a different regional constraint; geography is not evidence of global ecosystem incompatibility. |
| R1A / 3 | `CalcareousMeadowSteppe` | Diagnostic floristic combinations, substrate chemistry, moisture envelope and management are not replaced by this profile. |
| R1M / 3 | `NardusGrassland` | Usually dominant is not universally required; dominant taxa, regional assemblages and environmental range need explicit modality. |
| R21 / 3 | `MesicPermanentPasture` | Management history and regional vegetation composition are not encoded; generic pasture cover is insufficient. |
| S41 / 3 | `WetHeath` | Wetness, peat and characteristic assemblages need diagnostic rules; a single wet observation is not a wet-heath definition. |
| S42 / 3 | `DryHeath` | Need a comparable moisture measure and temporal support before treating wet/dry identities as mutually exclusive. |
| T11 / 3 | `WillowPoplarRiparianForest` | Willow or poplar is a disjunctive aggregate identity, not a requirement for both; flooding and diagnostics remain unresolved. |
| T17 / 3 | `BeechForestNonAcidSoil` | No invented pH cutoff; assemblages and soil/base-status measurements must establish applicability. |
| T18 / 3 | `BeechForestAcidSoil` | Shares beech composition with T17; soil acidity and associated diagnostic flora retain the distinction. |
| T21 / 3 | `MediterraneanEvergreenOakForest` | Regional species assemblages and thermomediterranean setting require review; not every evergreen oak stand belongs here. |
| Q11 / 3 | `RaisedBog` | Raised ombrotrophy is narrower than generic GET bog properties, subject to climate and interpretation; no equivalence is asserted. |
| Q12 / 3 | `BlanketBog` | Blanket versus raised morphology must remain distinct, even if both may fit a broader GET bog group. |
| Q22 / 3 | `PoorFen` | Poor denotes a context-dependent nutrient/base-status category; it is not an ecosystem condition score. |
| Q41 / 3 | `CalcareousSpringFen` | Alkalinity, carbonate chemistry, peat extent and diagnostic small-sedge assemblages require review; not every spring is this type. |
| Q51 / 3 | `TallHelophyteBed` | This class need not have the exact permanence or geomorphic setting of GET permanent marshes. |
| V11 / 3 | `IntensiveUnmixedCrops` | Diversity, management intensity and rotation need explicit rules; nominal crop identity is not a GET crosswalk. |
| V31 / 3 | `ImprovedGrasslandAndLawns` | Includes pasture, sports fields and lawns; the generic managed-herbaceous identity does not impose grazing, and management intensity still needs specification. |

### eunis2022_marine_pilot

| Source code / level | Pilot alias | Main unresolved criterion |
| --- | --- | --- |
| MA12 / 3 | `AtlanticLittoralRock` | Regional community, exposure and biological zonation require review; source level 3. |
| MA15 / 3 | `MediterraneanLittoralRock` | Comparable to MA12 only after separating biogeography and assemblages; source level 3. |
| MA52 / 3 | `AtlanticLittoralSand` | Grain size, mobile sediments, exposure duration and assemblages need constraints; source level 3. |
| MA62 / 3 | `AtlanticLittoralMud` | Sediment definition, oxygen and estuarine influences are unresolved; source level 3. |
| MB12 / 3 | `AtlanticInfralittoralRock` | Cannot assume every unit is a kelp forest or photic coral reef; source level 3. |
| MB15 / 3 | `MediterraneanInfralittoralRock` | Algal, animal and mixed communities may differ from GET functional groups; source level 3. |
| MA522 / 4 | `AtlanticLittoralSandSeagrass` | Deliberately selected deeper level-4 community to compare with GET M1.1; source level is not inferred from code length. |
| MA623 / 4 | `AtlanticLittoralMudSeagrass` | Level 4; substrate and tidal context refine the seagrass case, subject to consistent meadow/bed interpretation. |
| MB522 / 4 | `AtlanticInfralittoralSandSeagrass` | Level 4; not synonymous with the global seagrass functional group; extent and species assemblages need review. |
| MB252 / 4 | `PosidoniaOceanicaBiocenosis` | Level 4; species identity is an invented stand-in, not an authority-resolved taxon. No numeric taxonomy code is guessed. |
| MH12 / 3 | `AtlanticNeuston` | Level 3; an interface community must not be equated to an entire GET epipelagic water column. |
| MH22 / 3 | `AtlanticMixedReducedSalinity` | Level 3; mixing, salinity and seasonality require explicit observation support and thresholds. |
| MH32 / 3 | `AtlanticMixedFullSalinity` | Level 3; full salinity is a source-relative qualitative category, not an invented universal number. |
| MJ11 / 3 | `ArcticSeaIce` | Level 3; freezing, brine, seasonal coverage and sea-ice community require more than frozen cover presence. |

### eunis2012_inland_pilot

| Source code / level | Pilot alias | Main unresolved criterion |
| --- | --- | --- |
| C1.1 / 3 | `PermanentOligotrophicLake` | Trophic status, light, chemistry and water-body extent require measurements; not equivalent to GET large freshwater lakes. |
| C1.2 / 3 | `PermanentMesotrophicLake` | No universal nutrient cutoff is invented; the category needs a calibrated observation and temporal support. |
| C1.3 / 3 | `PermanentEutrophicLake` | Trophic status is not automatically ecological condition or a pressure score; origin and management must be separated. |
| C1.5 / 3 | `PermanentSalineBrackishLake` | Inland saline and brackish waters need not be soda lakes; ionic composition and ocean connectivity differ from estuaries. |
| C1.6 / 3 | `TemporaryStandingWater` | Temporary water includes multiple regimes; no fixed hydroperiod duration or generic GET equivalent is assumed. |
| C2.2 / 3 | `PermanentFastWatercourse` | Not equivalent to upland by itself: stream order, gradient and catchment context are separate properties. |
| C2.3 / 3 | `PermanentSmoothWatercourse` | Smooth flow does not by itself determine lowland location, river size or ecosystem functional group. |
| C2.5 / 3 | `TemporaryRunningWater` | GET seasonal upland streams may remain perennial, so this is neither a simple synonym nor an exhaustive mapping. |
