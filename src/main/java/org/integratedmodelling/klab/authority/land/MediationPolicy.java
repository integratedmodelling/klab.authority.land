package org.integratedmodelling.klab.authority.land;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Immutable, strictly parsed per-bridge numerical interpretation and admission policy. */
public record MediationPolicy(String version, SpreadMode spreadMode, BigDecimal spread, int steps,
                              BigDecimal overlapThreshold, Admission admission, BigDecimal admissionAlpha,
                              Map<String, QuantityContext> configurations) {
  public static final String VERSION = "fuzzy-ratios-1";
  public enum SpreadMode { RELATIVE, ABSOLUTE }
  public enum Admission { PROVEN_ONLY, ALPHA_CUT }
  public record QuantityContext(String interpretation, PatternRule.Measure measure, PatternRule.Support support,
                                boolean independentArguments) {
    public QuantityContext {
      PatternRule.text(interpretation); Objects.requireNonNull(measure); Objects.requireNonNull(support);
      if (!independentArguments) throw new IllegalArgumentException("Initial solver requires independentArguments: true");
    }
  }
  public MediationPolicy {
    if (!Set.of(VERSION,"disabled").contains(version)) throw new IllegalArgumentException("Unknown mediation policy version");
    Objects.requireNonNull(spreadMode); Objects.requireNonNull(admission);
    spread = number(spread); overlapThreshold = number(overlapThreshold); admissionAlpha = number(admissionAlpha);
    if (spread.signum() < 0 || spread.compareTo(BigDecimal.ONE) >= 0)
      throw new IllegalArgumentException("spread must be in [0,1), ensuring strictly positive weight support");
    if (overlapThreshold.signum() <= 0 || overlapThreshold.compareTo(BigDecimal.ONE) > 0
        || admissionAlpha.signum() <= 0 || admissionAlpha.compareTo(BigDecimal.ONE) > 0)
      throw new IllegalArgumentException("overlapThreshold and admissionAlpha must be in (0,1]");
    if (steps < 4 || steps > 40) throw new IllegalArgumentException("steps must be an integer in 4..40");
    if (configurations.size() > 64) throw new IllegalArgumentException("At most 64 quantity configurations");
    configurations.keySet().forEach(PatternRule::reference);
    configurations = Collections.unmodifiableMap(new TreeMap<>(configurations));
  }
  public boolean enabled() { return VERSION.equals(version); }
  static MediationPolicy disabled() {
    return new MediationPolicy("disabled",SpreadMode.RELATIVE,new BigDecimal("0.25"),24,
        new BigDecimal("0.5"),Admission.PROVEN_ONLY,new BigDecimal("0.5"),Map.of());
  }
  /** Missing mediation disables fuzzy work. Present policies require an explicit version. */
  static MediationPolicy parse(Object value, PatternSemantics semantics) {
    if (value == null) throw new IllegalArgumentException("mediation must be a mapping, not null");
    Map<String,Object> map = mapping(value,Set.of("version","spreadMode","spread","steps","overlapThreshold","admission","admissionAlpha","configurations"));
    if (!VERSION.equals(string(map.get("version")))) throw new IllegalArgumentException("Expected mediation.version: " + VERSION);
    var contexts = new TreeMap<String,QuantityContext>();
    Object raw = map.getOrDefault("configurations",Map.of());
    if (!(raw instanceof Map<?,?> entries) || entries.size() > 64) throw new IllegalArgumentException("configurations must map at most 64 functors");
    for (var entry : entries.entrySet()) {
      String concept = string(entry.getKey()); PatternRule.reference(concept);
      String canonical = semantics.validate(concept,PatternSemantics.Role.CONFIGURATION); PatternRule.reference(canonical);
      var context = mapping(entry.getValue(),Set.of("interpretation","measure","support","independentArguments"));
      var support = mapping(context.get("support"),Set.of("space","time","aggregation"));
      if (!(context.get("independentArguments") instanceof Boolean independent))
        throw new IllegalArgumentException("independentArguments must be an explicit boolean");
      var binding = new QuantityContext(string(context.get("interpretation")),PatternRule.Measure.valueOf(string(context.get("measure"))),
          new PatternRule.Support(string(support.get("space")),string(support.get("time")),string(support.get("aggregation"))),independent);
      if (contexts.putIfAbsent(canonical,binding) != null) throw new IllegalArgumentException("Duplicate canonical quantity configuration");
    }
    BigDecimal rawSteps = number(map.getOrDefault("steps",24));
    int steps;
    try { steps = rawSteps.intValueExact(); } catch (ArithmeticException e) { throw new IllegalArgumentException("steps must be integral",e); }
    return new MediationPolicy(VERSION,SpreadMode.valueOf(string(map.getOrDefault("spreadMode","RELATIVE"))),
        number(map.getOrDefault("spread","0.25")),steps,number(map.getOrDefault("overlapThreshold","0.5")),
        Admission.valueOf(string(map.getOrDefault("admission","PROVEN_ONLY"))),number(map.getOrDefault("admissionAlpha","0.5")),contexts);
  }
  public String fingerprint() {
    try {
      // Record fields and sorted context keys produce an unambiguous reproducible policy snapshot.
      byte[] bytes = new ObjectMapper().writeValueAsString(this).getBytes(StandardCharsets.UTF_8);
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    } catch (Exception e) { throw new IllegalStateException("Cannot fingerprint mediation policy",e); }
  }
  private static BigDecimal number(Object value) {
    if (!(value instanceof String || value instanceof Number)) throw new IllegalArgumentException("Expected a decimal number or decimal string");
    String text = value.toString();
    if (text.length() > 128) throw new IllegalArgumentException("Decimal exceeds limits");
    try {
      BigDecimal n = new BigDecimal(text);
      if (n.precision() > 64 || Math.abs((long)n.scale()) > 128) throw new IllegalArgumentException("Decimal exceeds limits");
      return n.signum() == 0 ? BigDecimal.ZERO : n.stripTrailingZeros();
    } catch (NumberFormatException e) { throw new IllegalArgumentException("Expected finite decimal",e); }
  }
  private static String string(Object value) {
    if (!(value instanceof String s)) throw new IllegalArgumentException("Expected a string"); PatternRule.text(s); return s;
  }
  private static Map<String,Object> mapping(Object value,Set<String> keys) {
    if (!(value instanceof Map<?,?> map)) throw new IllegalArgumentException("Expected a mediation mapping");
    var result = new LinkedHashMap<String,Object>();
    map.forEach((k,v)->{ if (!(k instanceof String s) || !keys.contains(s)) throw new IllegalArgumentException("Unknown mediation field: " + k); result.put((String)k,v); });
    return result;
  }
}
