package org.integratedmodelling.klab.authority.land;

import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Strict data-only rule codec and stable, alpha-renamed identity serialization. */
public final class PatternRuleYaml {
  public static final String PREFIX = "rule2:";
  public static final int MAX_BYTES = 131072;
  private static final ObjectMapper YAML = new ObjectMapper(YAMLFactory.builder()
      .streamReadConstraints(StreamReadConstraints.builder().maxNestingDepth(96).maxStringLength(65536).build())
      .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build())
      .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
      .enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
      .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
      .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS);
  private static final ObjectMapper JSON = new ObjectMapper();
  private PatternRuleYaml() {}

  public static PatternRule loadClasspath(String path) {
    try (var in = PatternRuleYaml.class.getResourceAsStream(path)) {
      if (in == null) throw new IllegalArgumentException("Missing rule resource: " + path);
      return load(in);
    } catch (IOException e) { throw new IllegalArgumentException("Cannot read rule", e); }
  }
  public static PatternRule load(InputStream input) {
    try { return read(input.readNBytes(MAX_BYTES + 1), false); }
    catch (IOException e) { throw new IllegalArgumentException("Cannot read rule", e); }
  }
  public static PatternRule decode(String code) {
    if (code == null || !code.startsWith(PREFIX)) throw new IllegalArgumentException("Expected rule2 identity");
    return read(code.substring(PREFIX.length()).getBytes(StandardCharsets.UTF_8), true);
  }
  private static PatternRule read(byte[] bytes, boolean identity) {
    if (bytes.length > MAX_BYTES) throw new IllegalArgumentException("Rule exceeds 128 KiB");
    try (var parser = YAML.createParser(bytes)) {
      JsonNode node = YAML.readTree(parser);
      if (parser.nextToken() != null) throw new IllegalArgumentException("Only one rule document is allowed");
      if (node == null || !node.isObject()) throw new IllegalArgumentException("Expected rule mapping");
      if (identity && !node.has("provenance")) ((ObjectNode)node).set("provenance", JSON.valueToTree(
          new PatternRule.Provenance("urn:land:canonical-rule", "inline", "Source provenance is external to canonical identity")));
      return YAML.treeToValue(node, PatternRule.class);
    } catch (IOException e) { throw new IllegalArgumentException("Invalid rule YAML/JSON: " + e.getMessage(), e); }
  }

  /** Defining semantics only; participant naming, map ordering and provenance do not affect identity. */
  public static String canonical(PatternRule rule) {
    ObjectNode tree = JSON.valueToTree(rule);
    tree.remove("provenance");
    var ordered = new ArrayList<>(rule.participants());
    ordered.sort((a,b) -> comparePaths(a.path(), b.path()));
    var names = new HashMap<String,String>();
    var participants = JSON.createArrayNode();
    for (int i = 0; i < ordered.size(); i++) {
      var p = ordered.get(i); names.put(p.id(), "p" + i);
      ObjectNode value = JSON.valueToTree(p); value.put("id", "p" + i); participants.add(value);
    }
    tree.set("participants", participants);
    var profiles = new ArrayList<>(rule.profiles());
    profiles.sort((a,b) -> comparePaths(a.path(), b.path()));
    tree.set("profiles", JSON.valueToTree(profiles));
    rename(tree.get("criteria"), names);
    String result = PREFIX + sorted(tree).toString();
    if (result.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES)
      throw new IllegalArgumentException("Canonical rule exceeds limits");
    return result;
  }
  private static int comparePaths(List<Integer> a, List<Integer> b) {
    for (int i = 0; i < Math.min(a.size(), b.size()); i++) {
      int c = Integer.compare(a.get(i), b.get(i)); if (c != 0) return c;
    }
    return Integer.compare(a.size(), b.size());
  }
  private static void rename(JsonNode node, Map<String,String> names) {
    if (node.isObject()) {
      var object = (ObjectNode)node;
      for (String key : List.of("participant", "source", "target"))
        if (object.has(key)) object.put(key, Objects.requireNonNull(names.get(object.get(key).asText())));
      if (object.path("kind").asText().equals("typical")) object.remove("note");
      // Typical notes are non-defining but remain required when decoding a canonical identity.
      if (object.path("kind").asText().equals("typical")) object.put("note", "Typical criterion");
      object.elements().forEachRemaining(n -> rename(n, names));
      if (object.path("kind").asText().equals("group")) {
        var children = new ArrayList<JsonNode>(); object.path("children").forEach(children::add);
        children.sort(Comparator.comparing(n -> sorted(n).toString()));
        if (children.stream().map(n -> sorted(n).toString()).distinct().count() != children.size())
          throw new IllegalArgumentException("Duplicate canonical criteria");
        object.set("children", JSON.valueToTree(children));
      }
    } else if (node.isArray()) node.forEach(n -> rename(n, names));
  }
  private static JsonNode sorted(JsonNode node) {
    if (node.isObject()) {
      ObjectNode result = JSON.createObjectNode(); var keys = new TreeSet<String>(); node.fieldNames().forEachRemaining(keys::add);
      keys.forEach(k -> result.set(k, sorted(node.get(k)))); return result;
    }
    if (node.isArray()) { ArrayNode result = JSON.createArrayNode(); node.forEach(n -> result.add(sorted(n))); return result; }
    return node;
  }
}
