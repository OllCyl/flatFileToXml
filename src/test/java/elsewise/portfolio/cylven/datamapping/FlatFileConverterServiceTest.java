package elsewise.portfolio.cylven.datamapping;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.w3c.dom.Document;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FlatFileConverterServiceTest {

    private final FlatFileConverterService converter =
            new FlatFileConverterService(new FlatFileReaderService());

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
        List<String> rows = readResourceLines("testdata/" + testCase + "/input.txt");

        InputStream expected = getResourceAsStream("testdata/" + testCase + "/expected.xml");
        if (expected == null) {
            assertThrows(IllegalArgumentException.class, () -> converter.parseToXmlDocument(rows));
            return;
        }

        Document actual = converter.parseToXmlDocument(rows);
        assertEquals(
                normalize(readResource(expected)),
                normalize(toXmlString(actual)));
    }

    private static String readResource(InputStream stream) {
        try (var reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            return String.join("\n", reader.lines().toList());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static List<String> readResourceLines(String path) {
        try (var reader = new BufferedReader(new InputStreamReader(
                getResourceAsStream(path), StandardCharsets.UTF_8))) {
            return reader.lines()
                    .filter(line -> !line.isBlank())
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static InputStream getResourceAsStream(String path) {
        return FlatFileConverterServiceTest.class.getResourceAsStream("/" + path);
    }

    private static String toXmlString(Document document) throws Exception {
        var writer = new StringWriter();
        TransformerFactory.newInstance().newTransformer()
                .transform(new DOMSource(document), new StreamResult(writer));
        return writer.toString();
    }

    private static String normalize(String xml) {
        return xml.replaceAll("<\\?xml[^?]*\\?>", "")
                .replaceAll(">\\s+<", "><")
                .trim();
    }
}
