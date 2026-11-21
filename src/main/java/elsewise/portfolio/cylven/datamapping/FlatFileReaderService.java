package elsewise.portfolio.cylven.datamapping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class FlatFileReaderService {

    /*
     * This service is responsible for reading the flat file and returning its content as a string.
     * If we instead need to file content to be shared in the application, we could use a singleton pattern.
     */

    private static final Logger log = LoggerFactory.getLogger(FlatFileReaderService.class);

    private final ResourceLoader resourceLoader;

    @Value("${flatfile.input}")
    private String filePath;

    public FlatFileReaderService(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public List<String> readLines(String resourceName) throws IOException {
        Resource resource = resourceLoader.getResource(resourceName);
        try (var reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            return reader.lines()
                    .filter(line -> !line.isBlank())
                    .toList();
        }
    }


    public List<String> readInputLines() {
        try {
            log.info("==================== Application run at {} ====================", LocalDateTime.now());
            List<String> lines = readLines(filePath);
            log.info("Input flat file: {} ({} rows)\n{}", filePath, lines.size(), String.join("\n", lines));
            return lines;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read file: " + filePath, e);
        }
    }
}
