/*
 * Copyright 2026 Firefly Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.Map;
import org.fireflyframework.utils.template.RestrictedHtmlTemplateRenderer;
import org.fireflyframework.validators.schema.OfflineJsonSchemaValidator;

/** Offline authoring preview; does not publish a master or issue a customer document. */
public final class RenderSandboxDocumentPreviews {
  public static void main(String[] args) throws Exception {
    if (args.length != 2)
      throw new IllegalArgumentException("Usage: <sandbox-root> <output-directory>");
    Path root = Path.of(args[0]).toRealPath();
    Path output = Path.of(args[1]).toAbsolutePath().normalize();
    if (output.startsWith(root))
      throw new IllegalArgumentException("Preview output must be outside source");
    var json =
        new ObjectMapper()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .enable(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS);
    var catalog = json.readTree(Files.readString(root.resolve("catalog.json")));
    if (!"SANDBOX_ONLY".equals(catalog.path("scope").asText()))
      throw new IllegalArgumentException("Explicit sandbox catalog required");
    var validator = new OfflineJsonSchemaValidator();
    var renderer = new RestrictedHtmlTemplateRenderer();
    var renderedEntries = new ArrayList<Map<String, Object>>();
    Files.createDirectories(output);
    for (var entry : catalog.path("templates")) {
      String code = entry.path("templateCode").asText();
      if (!code.matches("SBX_[A-Z][A-Z0-9_]{0,45}"))
        throw new IllegalArgumentException("Invalid code");
      String schema = source(root, entry.path("schema").asText());
      String facts = source(root, entry.path("example").asText());
      if (!validator.validateSchema(schema).valid()
          || !validator.validateInstance(schema, facts).valid())
        throw new IllegalArgumentException("Invalid sandbox facts: " + code);
      var result =
          renderer.render(
              source(root, entry.path("template").asText()),
              Map.of("facts", json.readValue(facts, Map.class)));
      if (!result.success())
        throw new IllegalArgumentException("Restricted rendering failed: " + code);
      String filename = code.toLowerCase(java.util.Locale.ROOT) + ".fragment.html";
      Files.writeString(output.resolve(filename), result.html());
      renderedEntries.add(
          Map.of(
              "code",
              code,
              "name",
              entry.path("templateName").asText(),
              "version",
              entry.path("version").asText(),
              "fragment",
              filename,
              "documentReference",
              json.readTree(facts).path("documentReference").asText(),
              "localeCode",
              catalog.path("localeCode").asText(),
              "currency",
              json.readTree(facts).path("currency").asText(""),
              "sha256",
              HexFormat.of()
                  .formatHex(
                      MessageDigest.getInstance("SHA-256")
                          .digest(result.html().getBytes(StandardCharsets.UTF_8)))));
    }
    json.writerWithDefaultPrettyPrinter()
        .writeValue(
            output.resolve("rendered-manifest.json").toFile(),
            Map.of("scope", "SANDBOX_DESIGN_PREVIEW", "entries", renderedEntries));
    System.out.println("Rendered " + renderedEntries.size() + " validated sandbox specimens.");
  }

  private static String source(Path root, String relative) throws Exception {
    Path path = root.resolve(relative).toRealPath();
    if (!path.startsWith(root))
      throw new IllegalArgumentException("Source escapes sandbox catalog");
    return Files.readString(path);
  }
}
