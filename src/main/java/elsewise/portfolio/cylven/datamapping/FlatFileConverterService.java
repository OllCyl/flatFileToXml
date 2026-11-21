package elsewise.portfolio.cylven.datamapping;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class FlatFileConverterService {

    private static final Logger log = LoggerFactory.getLogger(FlatFileConverterService.class);
    private static final Pattern FIELD_SEPARATOR = Pattern.compile("\\|");
    private final FlatFileReaderService flatFileReaderService;

    @Value("${flatfile.output}")
    private String outputPath;

    public FlatFileConverterService(FlatFileReaderService flatFileReaderService) {
        this.flatFileReaderService = flatFileReaderService;
    }

    @PostConstruct
    public void convertAndLog() throws ParserConfigurationException, TransformerException {
        Document document = parseToXmlDocument(flatFileReaderService.readInputLines());

        var transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        var writer = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(writer));
        String xml = writer.toString();

        writeOutput(xml);
        log.info("Output XML:\n{}", xml);
    }

    private void writeOutput(String xml) {
        String path = outputPath.startsWith("file:") ? outputPath.substring("file:".length()) : outputPath;
        try {
            Files.writeString(Path.of(path), xml);
            log.info("Output written to {}", path);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write output file: " + path, e);
        }
    }

    Document parseToXmlDocument(List<String> rows) throws ParserConfigurationException {
        Document document = createDocument();

        Element people = document.createElement("people");
        document.appendChild(people);

        for (List<String> personRows : findSegments(rows, RowType.PERSON)) {
            people.appendChild(createPerson(document, personRows));
        }

        return document;
    }

    private Document createDocument() throws ParserConfigurationException {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
    }

    private Element createPerson(Document document, List<String> rows) {
        if (!rows.getFirst().startsWith(RowType.PERSON.prefix())) {
            throw new IllegalArgumentException(
                    "Unexpected row before " + RowType.PERSON + ": " + rows.getFirst());
        }
        Element person = parseRow(document, RowType.PERSON, rows.getFirst());

        for (List<String> segment : findSegments(rows.subList(1, rows.size()), RowType.FAMILY)) {

            if (!segment.getFirst().startsWith(RowType.FAMILY.prefix())) {
                appendLeafElements(document, person, segment);
            } else {
                person.appendChild(createFamily(document, segment));
            }
        }

        return person;
    }

    private Element createFamily(Document document, List<String> rows) {
        Element family = parseRow(document, RowType.FAMILY, rows.getFirst());

        appendLeafElements(document, family, rows.subList(1, rows.size()));

        return family;
    }

    private void appendLeafElements(Document document, Element parent, List<String> rows) {
        for (String row : rows) {
            parent.appendChild(parseLeafRow(document, row));
        }
    }

    private Element parseLeafRow(Document document, String row) {
        RowType type = rowTypeOf(row);
        if (type.isSegmentStart()) {
            throw new IllegalArgumentException("Expected leaf row (T/A), got segment start: " + row);
        }
        return parseRow(document, type, row);
    }

    private static RowType rowTypeOf(String row) {
        return RowType.fromPrefix(FIELD_SEPARATOR.split(row, 2)[0]);
    }

    private Element parseRow(Document document, RowType type, String row) {
        return buildElement(
                document,
                type.elementName(),
                FIELD_SEPARATOR.split(row, -1),
                type.fields()
        );
    }

    private static Element buildElement(Document document, String name, String[] values, List<String> fieldNames) {
        Element element = document.createElement(name);

        for (int i = 0; i < fieldNames.size() && i + 1 < values.length; i++) {
            Element child = document.createElement(fieldNames.get(i));
            child.setTextContent(values[i + 1]);
            element.appendChild(child);
        }

        return element;
    }

    private static List<List<String>> findSegments(List<String> rows, RowType type) {
        List<List<String>> segments = new ArrayList<>();
        List<String> current = new ArrayList<>();

        for (String row : rows) {
            // When we encounter a new prefix, we close the current segment and start a new one
            if (row.startsWith(type.prefix())) {
                if (!current.isEmpty()) {
                    segments.add(current);
                }
                current = new ArrayList<>();
            }
            current.add(row);
        }
        if (!current.isEmpty()) {
            segments.add(current);
        }

        return segments;
    }
}
