package org.integratedmodelling.klab.authority.land;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/** A versioned legend translation; metadata explicitly distinguishes tentative test material. */
public record PatternCatalog(String id, Domain domain, String version, URI source, List<Entry> entries,
                             Metadata metadata) {
  public enum Domain { LULC, ECOSYSTEM }
  public record Metadata(String title, String sourceVersion, boolean tentative,
                         List<String> notes, boolean acceptsProposals) {
    public Metadata {
      if (title == null || title.isBlank() || sourceVersion == null || sourceVersion.isBlank())
        throw new IllegalArgumentException("Catalog title and source version are required");
      notes = List.copyOf(notes);
    }
  }

  public PatternCatalog(String id, Domain domain, String version, URI source, List<Entry> entries) {
    this(id, domain, version, source, entries, new Metadata(id, version, false, List.of(), false));
  }

  public record Entry(String alias, String label, String expression, String sourceCode,
                      URI source, List<String> notes, PatternRule rule) {
    public Entry(String alias, String label, String expression, String sourceCode, URI source, List<String> notes) {
      this(alias,label,expression,sourceCode,source,notes,null);
    }
    public Entry(String alias, String label, String expression) {
      this(alias, label, expression, null, null, List.of());
    }
    public Entry {
      if (alias == null || !alias.matches("[A-Z][A-Za-z0-9_]*") || label == null || label.isBlank())
        throw new IllegalArgumentException("An entry needs a concept alias and label");
      expression = PatternParser.parse(expression).canonical();
      if (sourceCode != null && sourceCode.isBlank()) throw new IllegalArgumentException("Empty source code");
      if (source != null && !source.isAbsolute()) throw new IllegalArgumentException("Source must be absolute");
      notes = List.copyOf(notes);
      if (rule != null && !expression.equals(rule.composition()))
        throw new IllegalArgumentException("Entry composition and rule disagree");
    }
    public String identityCode() { return rule == null ? expression : PatternRuleYaml.canonical(rule); }
  }
  public PatternCatalog {
    if (id == null || !id.matches("[a-z][a-z0-9_]*") || version == null || version.isBlank())
      throw new IllegalArgumentException("A catalog needs a lowercase ID and pinned version");
    Objects.requireNonNull(domain);
    if (source == null || !source.isAbsolute()) throw new IllegalArgumentException("An absolute source URI is required");
    entries = List.copyOf(entries);
    Objects.requireNonNull(metadata);
    var aliases = new HashSet<String>();
    var codes = new HashSet<String>();
    for (var entry : entries) if (!aliases.add(entry.alias()))
      throw new IllegalArgumentException("Duplicate catalog alias: " + entry.alias());
    for (var entry : entries) if (entry.sourceCode() != null && !codes.add(entry.sourceCode()))
      throw new IllegalArgumentException("Duplicate source code: " + entry.sourceCode());
  }
}
