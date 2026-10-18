package org.integratedmodelling.klab.authority.land;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** Ordered immutable tree. Zero marks means unspecified relative quantity, never equal shares.
 * The enclosing configuration determines whether weights concern area, volume, duration or another measure. */
public record CoverPattern(String concept, int marks, List<CoverPattern> arguments) {
  public CoverPattern {
    Objects.requireNonNull(concept);
    if (concept.isBlank() || marks < 0) throw new IllegalArgumentException("Invalid pattern node");
    arguments = List.copyOf(arguments);
  }

  public boolean isConfiguration() { return !arguments.isEmpty(); }

  /** Stable syntax serialization; deliberately preserves order, marks and concept spelling. */
  public String canonical() {
    return "+".repeat(marks) + concept + (isConfiguration()
        ? arguments.stream().map(CoverPattern::canonical).collect(Collectors.joining(",", "(", ")"))
        : "");
  }
}
