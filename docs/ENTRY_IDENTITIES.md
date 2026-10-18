# Stable catalog-entry annotation identities

A catalog entry identifies a source classification record. Its optional LAND pattern or typed rule
is a separate interpretation of that record. Two source entries remain distinct even when a draft
translation loses the distinction between them. In particular IGBP source codes `"6"` (Closed
shrublands) and `"7"` (Open shrublands) no longer collapse into one authority identity.

## Identity and near-final notation

The identity key is the exact four-string tuple:

```text
[catalog ID, pinned catalog revision, source edition, exact source-code string]
```

Catalog revision is `PatternCatalog.version`; source edition is `Metadata.sourceVersion`. No numeric
conversion, trimming, case folding or Unicode normalization changes a key. `"006"` and `"6"` are
different codes. Source URI, label, aliases, notes, verification state and the optional interpretation
are provenance/metadata, not replacements for the key. Catalog IDs remain immutable within a provider
instance; loading a new revision requires a new provider lifecycle or a separately versioned catalog
ID under the existing selection contract. The bridge admits only its selected exact catalog revision.

Use the existing k.LAB authority and codelist syntax. For example, the following binding shape is
illustrative; the host must already supply the real worldview and validated identity root:

```text
identity CoverIdentity requires authority LAND {
    urn: "klab.authority.land",
    domain: "LULC",
    catalogs: ("igbp_modis61_draft"),
    codelists: { igbp_modis61_draft: "land.igbp" }
};

land.igbp:ClosedShrubland
land.igbp:OpenShrubland
```

Codelist values are now opaque `entry1:` envelopes. `CatalogEntryId` encodes the tuple as a compact
JSON array in unpadded base64url, with strict canonical decoding and bounded input. For the synthetic
key `["legend","r1","edition-1","006"]`, the normal authority locator is:

```text
LAND:[entry1:WyJsZWdlbmQiLCJyMSIsImVkaXRpb24tMSIsIjAwNiJd]
```

Applications should use the returned locator or `AuthorityIdentitySyntax.encode`, not assemble an
envelope themselves. Unknown envelope versions, extra fields, numeric source codes and noncanonical
encodings are rejected. An envelope is an identifier, not a signature or an assertion of provenance.

The legacy direct-definition route remains separate:

```text
LAND:[physical:HorizontalArrangement(land:Tree)]
```

This denotes the standalone validated pattern, not any catalog class that happens to use it as a
proposal. Typed direct definitions retain their existing `rule2:` envelope. No grammar or host wiring
change is introduced here; automatic provider binding and worldview ingestion remain outside scope.

## Verification and interpretation review

Catalog schema 3 adds explicit independent entry fields:

```yaml
schemaVersion: 3
id: legend
domain: LULC
version: r1
sourceVersion: edition-1
source: https://example.org/edition-1
title: Illustrative annotations
tentative: true
acceptsProposals: false
notes: []
entries:
  - code: "006"
    alias: ClosedShrubland
    label: Closed shrubland
    entryVerification: UNVERIFIED
    definitionReview: ABSENT
    aliases: ["Curated exact name"]
    notes: []
```

- `entryVerification`: `UNVERIFIED` or `VERIFIED`, recording curator verification of the source entry.
  This metadata is not a remote attestation and does not change the annotation key.
- `definitionReview`: `ABSENT`, `DRAFT`, or `REVIEWED`. Absent definitions omit `expression` and `rule`.
  Draft expressions may be incomplete or unparseable; they never participate in entry matching.
  Reviewed definitions must pass syntax and worldview validation during bridge configuration; reviewed
  typed rules additionally require RuleSemantics and completed validation obligations.
- Optional `expression` and/or `rule` carry interpretation data. `Entry.definitionCode()` exposes that
  data for explicit inspection; `catalog.identityCode(entry)` supplies the annotation identity.
- `aliases` are curated exact names for this same source entry. They are not cross-entry mappings.

Schemas 1/2 keep their previous syntax checks and load entries as `UNVERIFIED` with `DRAFT`
interpretations. The three-argument Java `Entry(alias, label, expression)` convenience constructor
is for synthetic local catalogs and uses the supplied alias as its declared local code. Real source
catalogs should supply the explicit exact source code instead. Annotation-only and draft catalogs require the configured identity root, not the
proposed vocabulary of every optional definition. Direct pattern/rule resolution still performs its
original semantic validation. Entry descriptions expose both statuses without marking an admitted
annotation as a failed identity merely because its interpretation is absent or provisional.

## Matching, search and reconciliation

The exact same admitted source entry, including any of its curated aliases, has distance zero.
Different entries return `UNKNOWN` even if definitions, labels or aliases coincide and even when both
definitions are reviewed. Entry-to-direct-definition comparisons are also `UNKNOWN`. A separately
specified, reviewed mapping would be needed to authorize more; this implementation introduces no
mapping registry, fuzzy entry admission or executable class-membership assertion. Existing direct
pattern/rule comparison and optional fuzzy mathematics remain confined to their explicit route.

Search retains each matching source-entry ID, with its own label/provenance, and returns up to the
existing 100-result limit. Exact reconciliation accepts one selector (`alias`, `name`, or `code`),
optionally constrained by `catalog`, `revision`, and `sourceEdition`. Names include the exact source
label and curated aliases. An exact name referring to multiple entries is rejected, even if all those
entries share one proposed pattern. There is no first-match, approximate-name or pattern-deduplication
admission. `expression` alone selects the direct-definition route; `entry` alone selects an envelope.

`auditCatalog` remains an explicitly requested diagnostic comparison of optional definitions. An
equivalent draft definition is a review finding, never entry equivalence or a mediation permission.
Missing or invalid definitions yield an UNKNOWN audit result.

## Migration and integration boundary

Old codelist values were direct pattern/rule identities; new codelist values identify source entries.
Existing saved direct definitions remain valid on their separate route. They cannot be automatically
migrated to a source entry because multiple source records may have collapsed into the old code.
Pin a reviewed source catalog/revision/code when migrating annotations. The provider cache-policy
revision changes to `catalog-entry-identities-1`; no existing host cache or capture is rewritten.

This patch does not import draft catalogs or proposed vocabulary into a worldview, implement a
classifier, add a remote protocol, register/configure a running provider, or extend host snapshot
support. `LandIdentity` still has no pattern-derived parent edges and uses the actual configured
identity root. Existing host binding, authorization and component-version checks remain required.
