package org.integratedmodelling.klab.authority.land;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/** A versioned legend translation; metadata explicitly distinguishes tentative test material. */
public record PatternCatalog(
    String id, Domain domain, String version, URI source, List<Entry> entries, Metadata metadata) {
  public enum Domain {
    LULC,
    ECOSYSTEM
  }

  public enum EntryVerification {
    UNVERIFIED,
    VERIFIED
  }

  public enum DefinitionReview {
    ABSENT,
    DRAFT,
    REVIEWED
  }

  public record Metadata(
      String title,
      String sourceVersion,
      boolean tentative,
      List<String> notes,
      boolean acceptsProposals) {
    public Metadata {
      if (title == null || title.isBlank() || sourceVersion == null || sourceVersion.isBlank())
        throw new IllegalArgumentException("Catalog title and source version are required");
      notes = List.copyOf(notes);
    }
  }

  public PatternCatalog(String id, Domain domain, String version, URI source, List<Entry> entries) {
    this(id, domain, version, source, entries, new Metadata(id, version, false, List.of(), false));
  }

  public record Entry(
      String alias,
      String label,
      String expression,
      String sourceCode,
      URI source,
      List<String> notes,
      PatternRule rule,
      EntryVerification entryVerification,
      DefinitionReview definitionReview,
      List<String> aliases,
      List<String> classParents) {
    /** Class parents mean explicit source-class inclusion, never navigational broader links. */
    public Entry(
        String alias,
        String label,
        String expression,
        String sourceCode,
        URI source,
        List<String> notes,
        PatternRule rule,
        EntryVerification verification,
        DefinitionReview review,
        List<String> aliases) {
      this(
          alias,
          label,
          expression,
          sourceCode,
          source,
          notes,
          rule,
          verification,
          review,
          aliases,
          List.of());
    }

    public Entry(
        String alias,
        String label,
        String expression,
        String sourceCode,
        URI source,
        List<String> notes) {
      this(alias, label, expression, sourceCode, source, notes, null);
    }

    public Entry(
        String alias,
        String label,
        String expression,
        String sourceCode,
        URI source,
        List<String> notes,
        PatternRule rule) {
      this(
          alias,
          label,
          expression,
          sourceCode,
          source,
          notes,
          rule,
          EntryVerification.UNVERIFIED,
          expression == null && rule == null ? DefinitionReview.ABSENT : DefinitionReview.DRAFT,
          List.of());
    }

    /** Programmatic local catalogs use the explicitly supplied alias as their local source code. */
    public Entry(String alias, String label, String expression) {
      this(alias, label, expression, alias, null, List.of());
    }

    public Entry {
      if (alias == null || !alias.matches("[A-Z][A-Za-z0-9_]*") || label == null || label.isBlank())
        throw new IllegalArgumentException("An entry needs a concept alias and label");
      if (sourceCode == null || sourceCode.isBlank())
        throw new IllegalArgumentException("An exact source code is required");
      if (source != null && !source.isAbsolute())
        throw new IllegalArgumentException("Source must be absolute");
      Objects.requireNonNull(entryVerification);
      Objects.requireNonNull(definitionReview);
      notes = List.copyOf(notes);
      aliases = List.copyOf(aliases);
      classParents = List.copyOf(classParents);
      if (classParents.size() > 256
          || new HashSet<>(classParents).size() != classParents.size()
          || classParents.stream()
              .anyMatch(code -> code == null || code.isBlank() || code.equals(sourceCode)))
        throw new IllegalArgumentException("Invalid, duplicate or self class parent");
      if (aliases.stream().anyMatch(name -> name == null || name.isBlank()))
        throw new IllegalArgumentException("Curated aliases must be nonblank strings");
      if (rule != null && expression == null) expression = rule.composition();
      if (expression != null
          && (expression.isBlank() || expression.length() > PatternParser.MAX_LENGTH))
        throw new IllegalArgumentException("Invalid or oversized optional definition");
      if (rule != null && !Objects.equals(expression, rule.composition()))
        throw new IllegalArgumentException("Entry composition and rule disagree");
      if (definitionReview == DefinitionReview.ABSENT && expression != null
          || definitionReview != DefinitionReview.ABSENT && expression == null)
        throw new IllegalArgumentException(
            "Definition review status disagrees with optional definition");
      if (definitionReview == DefinitionReview.REVIEWED)
        expression = PatternParser.parse(expression).canonical();
    }

    /** Optional interpretation only; never use this as the source-entry identity. */
    public String definitionCode() {
      return rule == null ? expression : PatternRuleYaml.canonical(rule);
    }
  }

  public String identityCode(Entry entry) {
    if (!entries.contains(entry))
      throw new IllegalArgumentException("Entry is not in this catalog");
    return new CatalogEntryId(id, version, metadata.sourceVersion(), entry.sourceCode()).code();
  }

  /** Directed source-class inclusion only, confined to this exact pinned catalog. */
  public boolean strictlyIncludes(Entry parent, Entry child) {
    if (!entries.contains(parent)
        || !entries.contains(child)
        || parent.sourceCode().equals(child.sourceCode())) return false;
    var byCode = new java.util.HashMap<String, Entry>();
    for (var entry : entries) byCode.put(entry.sourceCode(), entry);
    var pending = new java.util.ArrayDeque<>(child.classParents());
    var seen = new HashSet<String>();
    while (!pending.isEmpty()) {
      var code = pending.removeFirst();
      if (code.equals(parent.sourceCode())) return true;
      if (seen.add(code)) pending.addAll(byCode.get(code).classParents());
    }
    return false;
  }

  public PatternCatalog {
    if (id == null || !id.matches("[a-z][a-z0-9_]*") || version == null || version.isBlank())
      throw new IllegalArgumentException("A catalog needs a lowercase ID and pinned version");
    Objects.requireNonNull(domain);
    if (source == null || !source.isAbsolute())
      throw new IllegalArgumentException("An absolute source URI is required");
    entries = List.copyOf(entries);
    Objects.requireNonNull(metadata);
    var aliases = new HashSet<String>();
    var codes = new HashSet<String>();
    for (var entry : entries)
      if (!aliases.add(entry.alias()))
        throw new IllegalArgumentException("Duplicate catalog alias: " + entry.alias());
    for (var entry : entries) {
      new CatalogEntryId(id, version, metadata.sourceVersion(), entry.sourceCode()).code();
      if (!codes.add(entry.sourceCode()))
        throw new IllegalArgumentException("Duplicate source code: " + entry.sourceCode());
    }
    var indegree = new java.util.HashMap<String, Integer>();
    var children = new java.util.HashMap<String, java.util.List<String>>();
    for (var entry : entries) {
      indegree.put(entry.sourceCode(), entry.classParents().size());
      for (var parent : entry.classParents()) {
        if (!codes.contains(parent))
          throw new IllegalArgumentException("Unknown class parent: " + parent);
        children
            .computeIfAbsent(parent, key -> new java.util.ArrayList<>())
            .add(entry.sourceCode());
      }
    }
    var ready = new java.util.ArrayDeque<String>();
    indegree.forEach(
        (code, count) -> {
          if (count == 0) ready.add(code);
        });
    int visited = 0;
    while (!ready.isEmpty()) {
      var code = ready.removeFirst();
      visited++;
      for (var child : children.getOrDefault(code, List.of()))
        if (indegree.compute(child, (key, count) -> count - 1) == 0) ready.add(child);
    }
    if (visited != entries.size())
      throw new IllegalArgumentException("Cycle in source-class hierarchy");
  }
}
