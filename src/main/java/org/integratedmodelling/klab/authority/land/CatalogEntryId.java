package org.integratedmodelling.klab.authority.land;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Base64;
import java.util.List;

/** Opaque source-entry identity. Definitions, labels and aliases never contribute to its key. */
public record CatalogEntryId(String catalog, String revision, String sourceEdition, String sourceCode) {
  public static final String PREFIX = "entry1:";
  private static final int MAX_BYTES = 65536;
  private static final ObjectMapper JSON = new ObjectMapper();
  public CatalogEntryId {
    if (catalog == null || !catalog.matches("[a-z][a-z0-9_]*"))
      throw new IllegalArgumentException("Invalid entry catalog ID");
    for (String part : new String[] {catalog, revision, sourceEdition, sourceCode})
      if (part == null || part.isBlank() || part.length() > 4096 || part.chars().anyMatch(Character::isISOControl))
        throw new IllegalArgumentException("Entry key fields must be nonblank bounded strings without control characters");
  }
  public String code() {
    try {
      byte[] bytes = JSON.writeValueAsBytes(List.of(catalog, revision, sourceEdition, sourceCode));
      if (bytes.length > MAX_BYTES) throw new IllegalArgumentException("Entry envelope is too large");
      return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    } catch (IOException e) { throw new IllegalArgumentException("Cannot encode entry identity", e); }
  }
  public static boolean isEntryCode(String code) {
    if (code == null) return false;
    int separator = code.indexOf(':');
    return separator > 0 && code.substring(0, separator).matches("entry[0-9]+");
  }
  public static CatalogEntryId decode(String code) {
    if (code == null || !code.startsWith(PREFIX) || code.length() > PREFIX.length() + (MAX_BYTES + 2) / 3 * 4)
      throw new IllegalArgumentException("Expected bounded entry1 identity");
    try {
      byte[] bytes = Base64.getUrlDecoder().decode(code.substring(PREFIX.length()));
      if (bytes.length > MAX_BYTES) throw new IllegalArgumentException("Entry envelope is too large");
      var node = JSON.readTree(bytes);
      if (node == null || !node.isArray() || node.size() != 4)
        throw new IllegalArgumentException("Entry envelope requires exactly four key strings");
      for (var field : node) if (!field.isTextual()) throw new IllegalArgumentException("Entry key fields must be strings");
      var result = new CatalogEntryId(node.get(0).textValue(), node.get(1).textValue(),
          node.get(2).textValue(), node.get(3).textValue());
      if (!result.code().equals(code)) throw new IllegalArgumentException("Noncanonical entry envelope");
      return result;
    } catch (IOException e) { throw new IllegalArgumentException("Invalid entry envelope", e); }
  }
}
