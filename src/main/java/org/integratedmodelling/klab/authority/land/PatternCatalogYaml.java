package org.integratedmodelling.klab.authority.land;

import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Strict, data-only YAML catalog reader. No YAML constructors, external includes or remote loading. */
public final class PatternCatalogYaml {
  private static final int MAX_BYTES = 1024 * 1024;
  private static final ObjectMapper YAML = new ObjectMapper(
      YAMLFactory.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build());
  private PatternCatalogYaml() {}

  public static PatternCatalog loadClasspath(String resource) {
    try (var input = PatternCatalogYaml.class.getResourceAsStream(resource)) {
      if (input == null) throw new IllegalArgumentException("Missing catalog resource: " + resource);
      return load(input);
    } catch (IOException e) { throw new IllegalArgumentException("Cannot read catalog: " + resource, e); }
  }

  /** The caller owns the stream. Versions 1/2 retain their syntax checks; version 3 separates entries from optional definitions. */
  public static PatternCatalog load(InputStream input) {
    try {
      byte[] bytes = input.readNBytes(MAX_BYTES + 1);
      if (bytes.length > MAX_BYTES) throw new IllegalArgumentException("Catalog exceeds 1 MiB");
      JsonNode root;
      try (var parser = YAML.createParser(bytes)) {
        root = YAML.readTree(parser);
        if (parser.nextToken() != null) throw new IllegalArgumentException("Only one YAML document is allowed");
      }
      fields(root, Set.of("schemaVersion", "id", "domain", "version", "source", "entries",
          "title", "sourceVersion", "tentative", "acceptsProposals", "notes"));
      if (!root.path("schemaVersion").isIntegralNumber() || !root.path("schemaVersion").canConvertToInt()
          || (root.path("schemaVersion").intValue() < 1 || root.path("schemaVersion").intValue() > 4))
        throw new IllegalArgumentException("Expected catalog schemaVersion: 1, 2, 3 or 4");
      URI source = URI.create(string(root, "source"));
      if (!root.path("entries").isArray() || root.path("entries").isEmpty())
        throw new IllegalArgumentException("entries must be a nonempty list");
      var entries = new ArrayList<PatternCatalog.Entry>();
      for (var entry : root.path("entries")) {
        int schema = root.path("schemaVersion").intValue();
        var supported = new HashSet<>(Set.of("code", "alias", "label", "expression", "source", "notes"));
        if (schema >= 2) supported.add("rule");
        if (schema >= 3) supported.addAll(Set.of("entryVerification", "definitionReview", "aliases"));
        if (schema == 4) supported.add("classParents");
        fields(entry, supported);
        PatternRule rule = entry.has("rule") ? PatternRuleYaml.load(new java.io.ByteArrayInputStream(
            entry.get("rule").toString().getBytes(java.nio.charset.StandardCharsets.UTF_8))) : null;
        String expression = schema < 3 ? PatternParser.parse(string(entry, "expression")).canonical()
            : entry.has("expression") ? string(entry, "expression") : null;
        entries.add(new PatternCatalog.Entry(string(entry, "alias"), string(entry, "label"),
            expression, string(entry, "code"),
            entry.has("source") ? URI.create(string(entry, "source")) : source, strings(entry, "notes"), rule,
            schema >= 3 ? PatternCatalog.EntryVerification.valueOf(string(entry, "entryVerification"))
                : PatternCatalog.EntryVerification.UNVERIFIED,
            schema >= 3 ? PatternCatalog.DefinitionReview.valueOf(string(entry, "definitionReview"))
                : PatternCatalog.DefinitionReview.DRAFT,
            entry.has("aliases") ? strings(entry, "aliases") : List.of(),
            schema == 4 && entry.has("classParents") ? strings(entry, "classParents") : List.of()));
      }
      return new PatternCatalog(string(root, "id"), PatternCatalog.Domain.valueOf(string(root, "domain")),
          string(root, "version"), source, entries,
          new PatternCatalog.Metadata(string(root, "title"), string(root, "sourceVersion"),
              bool(root, "tentative"), strings(root, "notes"), bool(root, "acceptsProposals")));
    } catch (IOException e) { throw new IllegalArgumentException("Invalid catalog YAML", e); }
  }

  private static void fields(JsonNode node, Set<String> supported) {
    if (node == null || !node.isObject()) throw new IllegalArgumentException("Expected a YAML mapping");
    var keys = new HashSet<String>(); node.fieldNames().forEachRemaining(keys::add);
    keys.removeAll(supported);
    if (!keys.isEmpty()) throw new IllegalArgumentException("Unknown catalog fields: " + keys);
  }
  private static String string(JsonNode node, String key) {
    var value = node.path(key);
    if (!value.isTextual() || value.textValue().isBlank())
      throw new IllegalArgumentException(key + " must be a nonblank string (quote numeric codes)");
    return value.textValue();
  }
  private static boolean bool(JsonNode node, String key) {
    if (!node.path(key).isBoolean()) throw new IllegalArgumentException(key + " must be a boolean");
    return node.path(key).booleanValue();
  }
  private static List<String> strings(JsonNode node, String key) {
    if (!node.path(key).isArray()) throw new IllegalArgumentException(key + " must be a list");
    var result = new ArrayList<String>();
    for (var value : node.path(key)) {
      if (!value.isTextual() || value.textValue().isBlank()) throw new IllegalArgumentException("Invalid " + key);
      result.add(value.textValue());
    }
    return List.copyOf(result);
  }
}
