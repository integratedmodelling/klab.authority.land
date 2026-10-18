# Entry-only hosting and offline replay

For source-entry identities without executable pattern definitions, explicitly configure:

```text
identity CoverIdentity requires authority LAND {
    urn: "klab.authority.land",
    mode: "ENTRY_ONLY",
    domain: "LULC",
    catalogs: ("igbp_modis61_draft"),
    codelists: { igbp_modis61_draft: "land.igbp" }
};
```

The existing Reasoner must still validate the loaded worldview, declared authority binding,
identity root, component version, rights, codelist namespaces and semantic boundaries. This
mode does not create a root-validation grant. It uses the root supplied by that host after
those checks. No semantic-boundary bindings or mediation configuration are accepted.

Entry identity is exactly catalog ID, translation revision, source edition and source code.
Different entries remain distinct even when optional definitions coincide. Source verification
and definition-review statuses remain catalog declarations; ENTRY_ONLY does not attest semantic
validation of those definitions. Direct pattern/rule resolution and validation fail closed.
Cross-entry semantic distance and mediation remain UNKNOWN. The default mode still requires
explicit PatternSemantics binding and retains its existing pattern/rule validation behavior.

Only successfully configured ENTRY_ONLY bridges export `land.catalog-entries.v1` snapshots.
Exports are deterministic data, bounded to 8 MiB, 256 catalogs and 100,000 entries per catalog.
They contain the exact worldview/name/root/parameters and selected catalog revisions, metadata,
aliases, codes and optional annotations. Restore validates the envelope and payload binding,
canonical encoding, schema, domain and selection before publishing a new opaque bridge. It
uses the frozen catalog content independently of the provider's currently registered catalogs;
it performs no network access or host semantic discovery. External host provenance and successful
configuration proof remain necessary: an arbitrary snapshot is not such proof.

Release and worldview unload remove bridge access. Close removes all provider state and prevents
new configuration/restore. Reconfiguration under changed source or provider revision requires a new
host capture; old accepted generations retain their separately owned frozen state until retired.

Services must support configured-snapshot codelist replay. Metadata discovery must not call
`getCodelistDefinitions(null)` on this configured provider. Candidate compilation restores the
proved snapshot and reads its configured codelists; it must never call live configure or fall
back to interactive provider discovery. A previously published LAND binary without snapshot
support does not acquire this behavior merely by updating Services.
