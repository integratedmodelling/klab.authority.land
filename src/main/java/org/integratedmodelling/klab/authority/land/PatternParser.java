package org.integratedmodelling.klab.authority.land;

import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * Recursive descent implementation of {@code node := '+'* reference ('(' node (',' node)* ')')?}.
 * Semantic validation distinguishes configurations from identities. Instances are per parse.
 */
public final class PatternParser {
  public static final int MAX_LENGTH = 65536;
  public static final int MAX_DEPTH = 64;
  private static final Pattern REFERENCE =
      Pattern.compile("[A-Za-z][A-Za-z0-9_.]*:[A-Za-z0-9_][A-Za-z0-9_.:-]*");
  private final String source;
  private int position;
  private int nodes;

  private PatternParser(String source) {
    this.source = source;
  }

  public static CoverPattern parse(String source) {
    if (source == null || source.isBlank() || source.length() > MAX_LENGTH)
      throw new IllegalArgumentException("Pattern must contain 1 to " + MAX_LENGTH + " characters");
    var parser = new PatternParser(source);
    var result = parser.node(0);
    parser.space();
    if (parser.position != source.length()) throw parser.error("Unexpected trailing input");
    if (!result.isConfiguration())
      throw parser.error("A pattern must have a configuration with arguments");
    return result;
  }

  private CoverPattern node(int depth) {
    if (depth >= MAX_DEPTH) throw error("Maximum nesting depth exceeded");
    if (++nodes > 2048) throw error("Maximum node count exceeded");
    space();
    int marks = 0;
    while (take('+')) {
      marks++;
      space();
    }
    String reference = reference();
    space();
    var arguments = new ArrayList<CoverPattern>();
    if (take('(')) {
      space();
      if (peek(')')) throw error("Configurations require at least one argument");
      arguments.add(node(depth + 1));
      space();
      while (take(',')) {
        if (arguments.size() >= 128) throw error("Maximum configuration arity exceeded");
        arguments.add(node(depth + 1));
        space();
      }
      if (!take(')')) throw error("Expected ',' or ')'");
    }
    return new CoverPattern(reference, marks, arguments);
  }

  private String reference() {
    int start = position;
    while (position < source.length()) {
      char c = source.charAt(position);
      if (Character.isWhitespace(c) || c == '(' || c == ')' || c == ',' || c == '+') break;
      if (c == '[') {
        String prefix = source.substring(start, position);
        if (!prefix.matches("[A-Z][A-Z0-9_]*(\\.[A-Z][A-Z0-9_]*)*:"))
          throw error("Bracketed IDs require an uppercase authority name");
        position++;
        int payloadStart = position;
        boolean closed = false;
        while (position < source.length()) {
          char next = source.charAt(position++);
          if (next == '\\') {
            if (position == source.length()) throw error("Unfinished ID escape");
            char escaped = source.charAt(position++);
            if (escaped != '\\' && escaped != ']') throw error("Invalid ID escape");
          } else if (next == ']') {
            closed = true;
            break;
          }
        }
        if (!closed || position == payloadStart + 1) throw error("Empty or unclosed authority ID");
        return source.substring(start, position);
      }
      position++;
    }
    String result = source.substring(start, position);
    if (!REFERENCE.matcher(result).matches())
      throw error("Expected a qualified concept or authority reference");
    return result;
  }

  private boolean peek(char c) {
    return position < source.length() && source.charAt(position) == c;
  }

  private boolean take(char c) {
    if (!peek(c)) return false;
    position++;
    return true;
  }

  private void space() {
    while (position < source.length() && Character.isWhitespace(source.charAt(position)))
      position++;
  }

  private IllegalArgumentException error(String message) {
    return new IllegalArgumentException(message + " at offset " + position);
  }
}
