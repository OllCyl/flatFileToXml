package elsewise.portfolio.cylven.datamapping;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FlatFileConverterServiceTest {

    private final FlatFileReaderService reader =
            new FlatFileReaderService(new DefaultResourceLoader());
    private final FlatFileConverterService converter =
            new FlatFileConverterService(reader);

    @TempDir
    Path tempDir;

    static Stream<String> testCases() throws Exception {
        Path testdataDir = Path.of(
                FlatFileConverterServiceTest.class.getResource("/testdata").toURI());
        try (Stream<Path> stream = Files.list(testdataDir)) {
            return stream.filter(Files::isDirectory)
                    .map(path -> path.getFileName().toString())
                    .sorted()
                    .toList()
                    .stream();
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("testCases")
    void convertsOrRejectsInput(String testCase) throws Exception {
        ReflectionTestUtils.setField(reader, "filePath",
                "classpath:testdata/" + testCase + "/input.txt");
        Path output = tempDir.resolve(testCase + ".xml");
        ReflectionTestUtils.setField(converter, "outputPath", output.toString());

        InputStream expected = getResourceAsStream("testdata/" + testCase + "/expected.xml");
        if (expected == null) {
            assertThrows(IllegalArgumentException.class, converter::convertAndLog);
            return;
        }

        converter.convertAndLog();
        assertEquals(
                normalize(readResource(expected)),
                normalize(Files.readString(output)));
    }

    private static String readResource(InputStream stream) {
        try (var reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            return String.join("\n", reader.lines().toList());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static InputStream getResourceAsStream(String path) {
        return FlatFileConverterServiceTest.class.getResourceAsStream("/" + path);
    }

    private static String normalize(String xml) {
        return xml.replaceAll("<\\?xml[^?]*\\?>", "")
                .replaceAll(">\\s+<", "><")
                .trim();
    }
}
