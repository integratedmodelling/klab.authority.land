# Ecosystem reference data attribution

The JSON indexes in this folder retain source codes, labels, hierarchy metadata and record locations,
not complete semantic definitions or runtime codelists. They are research reference material for the
partial ecosystem pilots. Exact source URLs, retrieval date and download hashes are in
`ecosystem-source-manifest.json`.

GET source: Keith, D. A. et al. (2022), *A function-based typology for Earth's ecosystems*, Nature 610,
513-518, https://doi.org/10.1038/s41586-022-05318-4, together with the maintained profile export at
https://github.com/Ecosystem-Indicators-Workflows/IUCN-GET-vocabulary, commit
`cadcd1c0e323e23751b660ce37431a133ce33bf5`, export 2026-04-01. The export identifies GET 2.1 with
minor updates to September 2025. The repository's related FAIR vocabulary output is released under
CC BY 4.0; see its README and output/LICENSE. These indexes select English code/label records and
add source sheet/row metadata. They are not an IUCN-endorsed redefinition or a new GET edition.

EUNIS source: European Environment Agency, *EUNIS habitat classification and crosswalks (tabular data)*,
https://www.eea.europa.eu/en/datahub/datahubitem-view/638330ea-90e6-4e41-81ea-e70f25ae7117/.
The indexes select level-3 records from terrestrial workbook 2021_1, marine workbook 2022 and the
legacy inland-water branch in the EUNIS 2007 classification with descriptions revised 2012/amended
2019. Four selected marine level-4 records are retained separately in the marine index to document
the pilot's deeper examples. The indexes preserve exact source labels; original source tables and
crosswalk meanings remain authoritative. No blanket equivalence with EU Habitats Directive Annex I
or between EEA source editions is asserted.

The original team workbook was inspected read-only. It is not distributed in these resources; only
selected review observations and its hash are recorded in docs/data/workbook-review-provenance.json.
