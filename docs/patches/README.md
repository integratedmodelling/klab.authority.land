# Shared authority-distance compatibility patch

`klab-services-authority-distance.patch` contains the shared edits made for LAND typed rules.

- Base: `38e6fe75f615f63219d23950c186d0f41f8e8054` (`develop`).
- SHA-256: `a284ba94417b2524ac6616063d3b3cc1d8362869d466947257aa848ef9951281`.
- Includes tracked changes and the new `docs/AUTHORITY_DISTANCE.md`; no staging or commit was performed.
- Apply from the `klab-services` root with `git apply --check <patch>` followed by `git apply <patch>`.
- Relevant checks: `mvn -o -pl klab.services.reasoner -am "-Dtest=AuthorityIdentitySyntaxTest,SemanticMatcherTest,AuthorityIdentityResolverTest,CachedAuthorityTest,CapturedAuthorityCatalogTest" "-Dsurefire.failIfNoSpecifiedTests=false" install` (43 tests passed).

Integration surface: `Authority` default distance hook/result/capability; `AuthorityIdentitySyntax`
bounded complex-code transport; `SemanticMatcher`, `ReasonerService`, `AuthorityIdentityResolver`,
`CachedAuthority`, and `CapturedAuthorityCatalog`. The TAXA source change is a **test fixture**.
Existing providers inherit UNSUPPORTED and retain hierarchy matching. UNKNOWN is fail-closed for
resolution and does not mean disjointness. Cross-binding comparisons are UNKNOWN. Aggregate matching
caches are bypassed while authorities are configured; candidate compilation does not call live providers.

No service-controller route was added: delegation runs through normal Reasoner semantic-distance
operations. No fuzzy score, remote provider protocol, cross-binding bridge or distance capture/replay
is implemented. LAND must be compiled with this updated shared API.

## Broader validation (patch unchanged)

The complete reactor test attempt stopped in core services at
`SpatialScanTest.policyChangesRejectCachedPlansAndPreviouslyIssuedHandles` (704 core-services tests,
one failure, two skipped), before reaching Reasoner. This path was not edited by this patch.

A standalone full Reasoner attempt initially lacked the required `klab.test.core.directory` property.
Rerunning with pinned sibling `odo-im/releases/0.1.1` reduced the result to 396 tests, three failures
and two errors. The remaining checks are:

- `CoreDomainAliasValidationTest`: three fundamental-domain diagnostic assertions.
- `CapabilitiesPublicationTest.acceptedCandidateComponentsArePublishedAlongsideHostComponents`: mock accepted-runtime status is null.
- `DomainInterpretationSupportTest.compilerKeepsBranchInterpretationOutOfTheWholeUnion`: unresolved PhysicalDomain interpretation target.

These broader-suite failures remain unresolved; they are not counted as passing validation. The
distance-focused 43 tests pass. No shared files were changed after exporting the patch. The pinned
ODO file SHA-256 is `2e840f49147a07b971146c24111c56f6831ea87347ff86e45588c1233a58c9bc`.
