package org.integratedmodelling.klab.authority.land;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.*;
import java.net.URI;
import java.util.*;
import org.integratedmodelling.klab.api.services.Authority;
import org.integratedmodelling.klab.api.services.ConfiguredAuthoritySnapshot;

/** Data-only entry snapshots. Optional definitions remain annotations, never executable grants. */
final class EntrySnapshotCodec {
  static final String FORMAT = "land.catalog-entries.v2";
  static final int MAX_BYTES = 8 * 1024 * 1024;
  private static final ObjectMapper JSON =
      new ObjectMapper(
              JsonFactory.builder()
                  .streamReadConstraints(
                      StreamReadConstraints.builder()
                          .maxNestingDepth(32)
                          .maxStringLength(131072)
                          .build())
                  .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                  .build())
          .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
          .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
          .enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
          .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
          .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS);

  private record Entry(
      String alias,
      String label,
      String expression,
      String sourceCode,
      URI source,
      List<String> notes,
      String rule,
      PatternCatalog.EntryVerification entryVerification,
      PatternCatalog.DefinitionReview definitionReview,
      List<String> aliases,
      List<String> classParents) {
    Entry(PatternCatalog.Entry e) {
      this(
          e.alias(),
          e.label(),
          e.expression(),
          e.sourceCode(),
          e.source(),
          e.notes(),
          e.rule() == null ? null : PatternRuleYaml.canonical(e.rule()),
          e.entryVerification(),
          e.definitionReview(),
          e.aliases(),
          e.classParents());
    }

    PatternCatalog.Entry entry() {
      return new PatternCatalog.Entry(
          alias,
          label,
          expression,
          sourceCode,
          source,
          notes,
          rule == null ? null : PatternRuleYaml.decode(rule),
          entryVerification,
          definitionReview,
          aliases,
          classParents);
    }
  }

  private record Catalog(
      String id,
      PatternCatalog.Domain domain,
      String version,
      URI source,
      PatternCatalog.Metadata metadata,
      List<Entry> entries) {
    Catalog(PatternCatalog c) {
      this(
          c.id(),
          c.domain(),
          c.version(),
          c.source(),
          c.metadata(),
          c.entries().stream().map(Entry::new).toList());
    }

    PatternCatalog catalog() {
      if (entries.size() > 100000)
        throw new IllegalArgumentException("Snapshot catalog exceeds entry bound");
      return new PatternCatalog(
          id, domain, version, source, entries.stream().map(Entry::entry).toList(), metadata);
    }
  }

  private record State(Authority.ConfigurationRequest request, List<Catalog> catalogs) {}

  static ConfiguredAuthoritySnapshot capture(
      Authority.ConfigurationRequest request, List<PatternCatalog> catalogs) {
    var result =
        new ConfiguredAuthoritySnapshot(
            FORMAT,
            LandAuthority.URN,
            request,
            encode(new State(request, catalogs.stream().map(Catalog::new).toList())));
    // Export and restore share the same schema, selection and size gates.
    restore(request, result);
    return result;
  }

  static List<PatternCatalog> restore(
      Authority.ConfigurationRequest request, ConfiguredAuthoritySnapshot snapshot) {
    if (snapshot == null
        || !FORMAT.equals(snapshot.format())
        || !LandAuthority.URN.equals(snapshot.providerUrn())
        || !request.equals(snapshot.request())
        || !LandAuthority.URN.equals(request.parameters().get("urn"))
        || !"ENTRY_ONLY".equals(request.parameters().get("mode")))
      throw new IllegalArgumentException(
          "LAND snapshot format, provider, exact binding and ENTRY_ONLY mode must match");
    if (!Set.of("urn", "domain", "catalogs", "semanticBoundaries", "mode")
        .containsAll(request.parameters().keySet()))
      throw new IllegalArgumentException("Unsupported entry snapshot parameter");
    request.requireSupportedBoundaries(Set.of());
    byte[] bytes = snapshot.state();
    if (bytes.length > MAX_BYTES)
      throw new IllegalArgumentException("LAND snapshot exceeds byte bound");
    try {
      var state = JSON.readValue(bytes, State.class);
      // Bind payload as well as envelope: rewrapping under a different root/name/worldview fails.
      if (!state.request().equals(request)
          || state.catalogs().size() > 256
          || !Arrays.equals(bytes, encode(state)))
        throw new IllegalArgumentException(
            "Snapshot payload is not canonical or differs from its binding");
      var catalogs = state.catalogs().stream().map(Catalog::catalog).toList();
      if (!Arrays.equals(
          bytes, encode(new State(request, catalogs.stream().map(Catalog::new).toList()))))
        throw new IllegalArgumentException("Snapshot annotations are not canonical catalog data");
      var selection = request.parameters().getOrDefault("catalogs", List.of());
      var ids = catalogs.stream().map(PatternCatalog::id).toList();
      var domain = PatternCatalog.Domain.valueOf((String) request.parameters().get("domain"));
      if (!ids.equals(selection)
          || new HashSet<>(ids).size() != ids.size()
          || catalogs.stream().anyMatch(c -> c.domain() != domain))
        throw new IllegalArgumentException(
            "Snapshot catalog selection or domain differs from exact request");
      return catalogs;
    } catch (IllegalArgumentException failure) {
      throw failure;
    } catch (Exception failure) {
      throw new IllegalArgumentException("Malformed LAND entry snapshot", failure);
    }
  }

  private static byte[] encode(State state) {
    try {
      var bytes = JSON.writeValueAsBytes(state);
      if (bytes.length > MAX_BYTES)
        throw new IllegalArgumentException("LAND snapshot exceeds byte bound");
      return bytes;
    } catch (java.io.IOException failure) {
      throw new IllegalArgumentException("Cannot encode LAND entry snapshot", failure);
    }
  }
}
