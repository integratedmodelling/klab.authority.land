package org.integratedmodelling.klab.authority.land;

import java.util.*;
import static org.integratedmodelling.klab.authority.land.PatternRule.*;

/** Individual type validation plus explicit unresolved execution obligations. Invalid assertions throw. */
public record RuleValidation(PatternRule rule, List<String> obligations, Map<String,String> dependencies) {
  public RuleValidation { obligations = List.copyOf(obligations); dependencies = Map.copyOf(dependencies); }
  /** No reported semantic/binding obligations. Does not attest data availability or model execution. */
  public boolean ready() { return obligations.isEmpty(); }

  static RuleValidation validate(PatternRule rule, PatternSemantics semantics) {
    var pending = new ArrayList<String>(); var dependencies = new TreeMap<String,String>();
    var participants = new HashMap<String,Participant>();
    rule.participants().forEach(p -> participants.put(p.id(), p));
    if (!(semantics instanceof RuleSemantics typed)) {
      pending.add("Host has no typed rule semantic validation");
      return new RuleValidation(rule, pending, dependencies);
    }
    var predicates = new HashMap<String,List<String>>();
    for (var p : rule.participants()) {
      var node = node(rule.composition(), p.path());
      if (!node.isConfiguration()) predicates.put(p.id(), new ArrayList<>(List.of(node.concept())));
      if (p.bearer() == null) pending.add("Infer bearer for participant " + p.id());
      else dependencies.put("bearer:" + p.id(), typed.validateTyped(p.bearer(), "SUBSTANTIAL"));
    }
    visit(rule.criteria(), typed, participants, predicates, pending, dependencies, false);
    for (var p : rule.participants()) if (p.bearer() != null) {
      var assertions = predicates.getOrDefault(p.id(), List.of());
      if (!typed.validateAttribution(p.bearer(), assertions)) pending.add("Validate combined attribution on " + p.id());
    }
    for (var profile : rule.profiles()) pending.add("Bind evaluator for configuration profile " + profile.interpretation());
    if (PatternParser.parse(rule.composition()).marks() > 0) pending.add("Root marks need an enclosing measure context");
    checkProfiles(PatternParser.parse(rule.composition()), new ArrayList<>(), rule.profiles(), pending);
    return new RuleValidation(rule, pending, dependencies);
  }
  private static void checkProfiles(CoverPattern node, List<Integer> path, List<Profile> profiles, List<String> pending) {
    if (node.arguments().stream().anyMatch(n -> n.marks() > 0)
        && profiles.stream().noneMatch(p -> p.path().equals(path))) pending.add("Declare quantity profile at " + path);
    for (int i = 0; i < node.arguments().size(); i++) {
      var child = new ArrayList<>(path); child.add(i); checkProfiles(node.arguments().get(i), child, profiles, pending);
    }
  }
  private static void visit(Clause c, RuleSemantics s, Map<String,Participant> ps, Map<String,List<String>> predicates,
                            List<String> pending, Map<String,String> dependencies, boolean conditional) {
    if (c instanceof Group g) {
      g.children().forEach(n -> visit(n,s,ps,predicates,pending,dependencies,conditional || g.operator() != Logic.ALL));
    } else if (c instanceof Typical t) visit(t.criterion(),s,ps,predicates,pending,dependencies,true);
    else if (c instanceof Qualification q) {
      String predicate = s.validateTyped(q.predicate(), q.type().name());
      dependencies.put(q.predicate(), predicate);
      if (!conditional && q.present()) predicates.computeIfAbsent(q.participant(), k -> new ArrayList<>()).add(predicate);
      else pending.add("Validate conditional/negative attribution jointly with its branch on " + q.participant() + ": " + q.predicate());
      if (q.type() == PredicateType.ROLE) pending.add("Bind role context for " + q.predicate());
    } else if (c instanceof Constraint q) {
      dependencies.put(q.observable(), s.validateObservable(q.observable()));
      var bearer = ps.get(q.participant()).bearer();
      if (bearer != null && !s.validateObservableBinding(q.observable(),bearer,q.unit()))
        pending.add("Validate observable/unit binding on " + q.participant() + ": " + q.observable());
    } else if (c instanceof Occurrent o) {
      dependencies.put(o.concept(), s.validateTyped(o.concept(), o.type().name()));
      pending.add("Bind " + o.type() + " occurrence to participant " + o.participant());
    } else if (c instanceof Relationship r) {
      if (r.type() != RelationType.SEMANTIC) pending.add("Bind relation evaluator " + r.relation());
      else {
        dependencies.put(r.relation(), s.validateTyped(r.relation(), "RELATIONSHIP"));
        var a = ps.get(r.source()).bearer(); var b = ps.get(r.target()).bearer();
        if (a != null && b != null && !s.validateRelationship(r.relation(),a,b))
          pending.add("Validate relationship endpoints " + r.relation());
      }
    }
  }
}
