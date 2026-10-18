package org.integratedmodelling.klab.authority.land;

import static org.junit.jupiter.api.Assertions.*;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.eclipse.xtext.parser.IParser;
import org.integratedmodelling.languages.WorldviewStandaloneSetup;
import org.junit.jupiter.api.Test;

/** Parse the binding statements read directly from the checked-in documentation. */
class DocumentationSyntaxTest {
  private record Snippet(Path path, String declaration) {}
  private static final Pattern FENCE = Pattern.compile("(?ms)^```(?:text|kwv)\\R(.*?)^```[\\t ]*");
  private static final Pattern BINDING = Pattern.compile(
      "(?s)identity\\s+\\w+\\s+requires\\s+authority\\s+\\w+\\s*\\{.*?\\};");
  private static final String HEADER = "ontology example in domain root version 1.0.0;\n";

  private static List<Snippet> snippets() throws Exception {
    var paths = new ArrayList<Path>(); paths.add(Path.of("README.md"));
    try (var files = Files.walk(Path.of("docs"))) {
      paths.addAll(files.filter(p -> p.toString().endsWith(".md")).sorted().toList());
    }
    var result = new ArrayList<Snippet>();
    for (var path : paths) {
      var fences = FENCE.matcher(Files.readString(path));
      while (fences.find()) {
        var bindings = BINDING.matcher(fences.group(1));
        while (bindings.find()) result.add(new Snippet(path, bindings.group()));
      }
    }
    return result;
  }

  @Test void everyDocumentedAuthorityBindingParsesWithoutChangingItsText() throws Exception {
    var parser = new WorldviewStandaloneSetup().createInjectorAndDoEMFRegistration().getInstance(IParser.class);
    var snippets = snippets(); assertEquals(6, snippets.size(), "Audit all documented binding examples");
    for (var snippet : snippets) {
      var parsed = parser.parse(new StringReader(HEADER + snippet.declaration()));
      var errors = new ArrayList<String>();
      for (var node : parsed.getSyntaxErrors()) errors.add(node.getSyntaxErrorMessage() + " at " + node.getText());
      assertFalse(parsed.hasSyntaxErrors(), snippet.path() + ": " + errors);
      assertFalse(Pattern.compile("catalogs\\s*:\\s*\\[").matcher(snippet.declaration()).find(), snippet.path().toString());
      System.out.println("DOCUMENTED_BINDING_PARSED " + snippet.path());
    }
  }

  @Test void squareBracketsRemainInvalidForTheWorldviewListRule() throws Exception {
    var snippet = snippets().stream().filter(s -> s.path().endsWith("ENTRY_IDENTITIES.md")).findFirst().orElseThrow();
    String old = snippet.declaration().replace("(\"igbp_modis61_draft\")", "[\"igbp_modis61_draft\"]");
    assertNotEquals(snippet.declaration(), old, "Negative control must restore the original defect");
    var parser = new WorldviewStandaloneSetup().createInjectorAndDoEMFRegistration().getInstance(IParser.class);
    assertTrue(parser.parse(new StringReader(HEADER + old)).hasSyntaxErrors());
  }
}
