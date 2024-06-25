package eu.dissco.nusearch.maven;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMethod;

public class MavenRunner {

  private static final Logger LOGGER = LoggerFactory.getLogger(MavenRunner.class);

  public static void main(String[] args) {
    LOGGER.info("Starting the MavenRunner to download and parse json schemas");
    for (String schemaUrl : args) {
      LOGGER.info("Processing json schema: {}", schemaUrl);
      var fileName = schemaUrl.substring(schemaUrl.lastIndexOf('/') + 1);
      String outputFilePath = "src/main/resources/json-schema/" + fileName;
      try {
        String schema = downloadSchema(schemaUrl);
        saveSchemaToFile(schema, outputFilePath);
        LOGGER.info("JSON schema downloaded and saved to: {} ", outputFilePath);
      } catch (IOException e) {
        LOGGER.error("Error downloading or saving the JSON schema", e);
      }
    }
  }

  private static String downloadSchema(String schemaUrl) throws IOException {
    StringBuilder result = new StringBuilder();
    URL url = new URL(schemaUrl);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod(RequestMethod.GET.name());
    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        result.append(line).append("\n");
      }
    }
    return result.toString();
  }

  private static void saveSchemaToFile(String schema, String filePath) throws IOException {
    try (BufferedWriter writer = new BufferedWriter(
        new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8))) {
      writer.write(schema);
    }
  }

}
