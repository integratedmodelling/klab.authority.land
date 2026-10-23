package org.integratedmodelling.klab.authority.land;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.math.BigDecimal;
import java.util.*;

/**
 * Versioned, immutable rule envelope. No policy, resource or observation enters defining semantics.
 */
public record PatternRule(
    int schemaVersion,
    String composition,
    List<Participant> participants,
    List<Profile> profiles,
    Clause criteria,
    Provenance provenance) {
  public enum PredicateType {
    IDENTITY,
    ATTRIBUTE,
    REALM,
    ROLE
  }

  public enum OccurrentType {
    EVENT,
    PROCESS
  }

  public enum RelationType {
    SEMANTIC,
    SPATIAL,
    TEMPORAL
  }

  public enum Logic {
    ALL,
    ANY,
    AT_LEAST
  }

  public enum Measure {
    AREA,
    VOLUME,
    DURATION,
    COUNT,
    BIOMASS,
    OTHER
  }

  public enum Order {
    DECLARED,
    SEQUENCE,
    CYCLE
  }

  public record Provenance(String source, String sourceCode, String note) {
    public Provenance {
      text(source);
      text(sourceCode);
      text(note);
    }
  }

  /**
   * Zero-based argument path; [] identifies the composition root. Null bearer is explicitly
   * deferred.
   */
  public record Participant(String id, List<Integer> path, String bearer) {
    public Participant {
      if (id == null || !id.matches("[a-z][A-Za-z0-9_]*"))
        throw new IllegalArgumentException("Invalid participant ID");
      path = PatternRule.path(path);
      if (bearer != null) reference(bearer);
    }
  }

  /**
   * All support strings are pinned vocabulary/application identifiers, not free implicit defaults.
   */
  public record Support(String space, String time, String aggregation) {
    public Support {
      text(space);
      text(time);
      text(aggregation);
    }
  }

  /**
   * The interpretation/version and support are defining; numerical uncertainty belongs to a policy.
   */
  public record Profile(
      List<Integer> path,
      String interpretation,
      Measure measure,
      Order order,
      Support support,
      boolean exhaustive) {
    public Profile {
      path = PatternRule.path(path);
      text(interpretation);
      Objects.requireNonNull(measure);
      Objects.requireNonNull(order);
      Objects.requireNonNull(support);
    }
  }

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
  @JsonSubTypes({
    @JsonSubTypes.Type(value = Qualification.class, name = "qualification"),
    @JsonSubTypes.Type(value = Constraint.class, name = "constraint"),
    @JsonSubTypes.Type(value = Relationship.class, name = "relationship"),
    @JsonSubTypes.Type(value = Occurrent.class, name = "occurrent"),
    @JsonSubTypes.Type(value = Group.class, name = "group"),
    @JsonSubTypes.Type(value = Typical.class, name = "typical")
  })
  public sealed interface Clause
      permits Qualification, Constraint, Relationship, Occurrent, Group, Typical {}

  public record Qualification(
      String participant, PredicateType type, String predicate, boolean present, Support support)
      implements Clause {
    public Qualification {
      text(participant);
      Objects.requireNonNull(type);
      reference(predicate);
      Objects.requireNonNull(support);
    }
  }

  /** Closed/open decimal interval in an explicit unit. Either endpoint may be null, never both. */
  public record Constraint(
      String participant,
      String observable,
      String unit,
      BigDecimal minimum,
      boolean minimumInclusive,
      BigDecimal maximum,
      boolean maximumInclusive,
      Support support)
      implements Clause {
    public Constraint {
      text(participant);
      text(observable);
      text(unit);
      Objects.requireNonNull(support);
      minimum = decimal(minimum);
      maximum = decimal(maximum);
      if (minimum == null && maximum == null)
        throw new IllegalArgumentException("An interval needs a bound");
      if ((minimum == null && minimumInclusive) || (maximum == null && maximumInclusive))
        throw new IllegalArgumentException("Unbounded endpoint cannot be inclusive");
      if (minimum != null
          && maximum != null
          && (minimum.compareTo(maximum) > 0
              || (minimum.compareTo(maximum) == 0 && (!minimumInclusive || !maximumInclusive))))
        throw new IllegalArgumentException("Empty interval");
    }
  }

  /**
   * SEMANTIC relations name worldview concepts; other relations name versioned evaluator contracts.
   */
  public record Relationship(
      String source, String target, RelationType type, String relation, Support support)
      implements Clause {
    public Relationship {
      text(source);
      text(target);
      Objects.requireNonNull(type);
      text(relation);
      Objects.requireNonNull(support);
      if (type == RelationType.SEMANTIC) reference(relation);
    }
  }

  /**
   * Presence of an event/process within support, not an assertion about its frequency or regime.
   */
  public record Occurrent(
      String participant, OccurrentType type, String concept, boolean present, Support support)
      implements Clause {
    public Occurrent {
      text(participant);
      Objects.requireNonNull(type);
      reference(concept);
      Objects.requireNonNull(support);
    }
  }

  public record Group(Logic operator, int minimum, List<Clause> children) implements Clause {
    public Group {
      Objects.requireNonNull(operator);
      children = List.copyOf(children);
      if (children.isEmpty() || children.size() > 128)
        throw new IllegalArgumentException("Group needs 1..128 children");
      if (operator == Logic.AT_LEAST ? minimum < 1 || minimum > children.size() : minimum != 0)
        throw new IllegalArgumentException("Only AT_LEAST takes a minimum, in 1..child count");
      // Bound programmatically assembled descendants before recursive record equality/hashCode.
      checkSize(children);
      if (new HashSet<>(children).size() != children.size())
        throw new IllegalArgumentException("Duplicate logical criteria");
      if (operator != Logic.ALL && children.stream().anyMatch(PatternRule::containsTypical))
        throw new IllegalArgumentException(
            "Typical descriptions cannot occupy alternative/cardinality slots");
    }
  }

  /** Explanatory only. Nested criterion is reported but cannot establish or defeat membership. */
  public record Typical(Clause criterion, String note) implements Clause {
    public Typical {
      Objects.requireNonNull(criterion);
      text(note);
    }
  }

  public PatternRule {
    if (schemaVersion != 2) throw new IllegalArgumentException("Expected rule schemaVersion 2");
    composition = PatternParser.parse(composition).canonical();
    participants = List.copyOf(participants);
    profiles = List.copyOf(profiles);
    Objects.requireNonNull(criteria);
    Objects.requireNonNull(provenance);
    if (participants.isEmpty() || participants.size() > 128 || profiles.size() > 128)
      throw new IllegalArgumentException("Rule needs 1..128 participants and at most 128 profiles");
    var ids = new HashSet<String>();
    var paths = new HashSet<List<Integer>>();
    for (var p : participants) {
      if (!ids.add(p.id()) || !paths.add(p.path()))
        throw new IllegalArgumentException("Duplicate participant or path");
      node(composition, p.path());
    }
    paths.clear();
    for (var p : profiles) {
      if (!paths.add(p.path()) || !node(composition, p.path()).isConfiguration())
        throw new IllegalArgumentException("Profile must name a unique configuration path");
    }
    check(criteria, ids, 0, new int[] {0});
  }

  static void check(Clause c, Set<String> ids, int depth, int[] count) {
    if (depth >= 32 || ++count[0] > 1024)
      throw new IllegalArgumentException("Rule criteria exceed limits");
    if (c instanceof Group g) g.children().forEach(n -> check(n, ids, depth + 1, count));
    else if (c instanceof Typical t) check(t.criterion(), ids, depth + 1, count);
    else if (c instanceof Relationship r) {
      require(ids, r.source());
      require(ids, r.target());
    } else
      require(
          ids,
          c instanceof Qualification q
              ? q.participant()
              : c instanceof Constraint q ? q.participant() : ((Occurrent) c).participant());
  }

  private static boolean containsTypical(Clause clause) {
    return clause instanceof Typical
        || clause instanceof Group group
            && group.children().stream().anyMatch(PatternRule::containsTypical);
  }

  private static void checkSize(List<Clause> children) {
    record Pending(Clause clause, int depth) {}
    var stack = new ArrayDeque<Pending>();
    children.forEach(c -> stack.push(new Pending(c, 1)));
    int count = 1;
    while (!stack.isEmpty()) {
      var p = stack.pop();
      if (p.depth() >= 32 || ++count > 1024)
        throw new IllegalArgumentException("Rule criteria exceed limits");
      if (p.clause() instanceof Group g)
        g.children().forEach(c -> stack.push(new Pending(c, p.depth() + 1)));
      else if (p.clause() instanceof Typical t)
        stack.push(new Pending(t.criterion(), p.depth() + 1));
    }
  }

  private static void require(Set<String> ids, String id) {
    if (!ids.contains(id)) throw new IllegalArgumentException("Unbound participant: " + id);
  }

  static CoverPattern node(String composition, List<Integer> path) {
    var node = PatternParser.parse(composition);
    for (int index : path) {
      if (index >= node.arguments().size())
        throw new IllegalArgumentException("Invalid participant/profile path: " + path);
      node = node.arguments().get(index);
    }
    return node;
  }

  private static List<Integer> path(List<Integer> value) {
    value = List.copyOf(value);
    if (value.size() >= PatternParser.MAX_DEPTH || value.stream().anyMatch(i -> i < 0))
      throw new IllegalArgumentException("Invalid path");
    return value;
  }

  static void reference(String reference) {
    var probe = PatternParser.parse("land:Probe(" + reference + ")");
    if (probe.arguments().size() != 1
        || probe.arguments().getFirst().isConfiguration()
        || probe.arguments().getFirst().marks() != 0
        || !probe.arguments().getFirst().concept().equals(reference))
      throw new IllegalArgumentException("Expected one concept reference");
  }

  static void text(String value) {
    if (value == null
        || value.isBlank()
        || value.length() > 8192
        || value.chars().anyMatch(c -> c < 32))
      throw new IllegalArgumentException("Expected bounded, nonblank single-line text");
  }

  private static BigDecimal decimal(BigDecimal value) {
    if (value == null) return null;
    if (value.precision() > 64 || Math.abs((long) value.scale()) > 128)
      throw new IllegalArgumentException("Decimal exceeds limits");
    return value.signum() == 0 ? BigDecimal.ZERO : value.stripTrailingZeros();
  }
}
