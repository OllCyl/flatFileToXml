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
        Document document = createDocument();
        try {
            Element people = document.createElement("people");
            document.appendChild(people);

            var personSegments = findSegmentsByType(flatFileReaderService.readInputLines(), RowType.PERSON);
            for (List<String> personRows : personSegments) {
                Element person = createPerson(document, personRows);
                people.appendChild(person);
            }
        } catch (IllegalArgumentException e) {
            log.error("Conversion failed — invalid flat file structure: {}", e.getMessage());
            throw e;
        }

        var transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        var writer = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(writer));
        String xml = writer.toString();

        writeOutput(xml);
    }

    private void writeOutput(String xml) {
        String path = outputPath.startsWith("file:") ? outputPath.substring("file:".length()) : outputPath;
        try {
            Files.writeString(Path.of(path), xml);
            log.info("Output written to {}", path);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write output file '" + path + "'", e);
        }
    }

    private Document createDocument() throws ParserConfigurationException {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
    }

    private Element createPerson(Document document, List<String> rows) {

        if (!rows.getFirst().startsWith(RowType.PERSON.prefix())) {
            throw new IllegalArgumentException("Invalid flat file: '" + rows.getFirst()
                    + "' — rows may not appear before the first person row (P|...).");
        }

        Element person = parseRow(document, RowType.PERSON, rows.getFirst());
        
        var familySegments = findSegmentsByType(rows.subList(1, rows.size()), RowType.FAMILY);

        for (List<String> familySegment : familySegments) {

            if (!familySegment.getFirst().startsWith(RowType.FAMILY.prefix())) {
                appendChildElements(document, person, familySegment);
            } else {
                Element familyNode = parseRow(document, RowType.FAMILY, familySegment.getFirst());
                appendChildElements(document, familyNode, familySegment.subList(1, familySegment.size()));

                person.appendChild(familyNode);
            }
        }

        return person;
    }

    private void appendChildElements(Document document, Element parent, List<String> rows) {
        for (String row : rows) {
            RowType type;

            String prefix = FIELD_SEPARATOR.split(row, 2)[0];
            try {
                type = RowType.fromPrefix(prefix);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid flat file: '" + row
                        + "' — unknown record type '" + prefix + "', expected P, T, A or F.");
            }

            if (type.isSegmentStart()) {
                throw new IllegalArgumentException("Invalid flat file: '" + row
                        + "' — only T (phone) and A (address) rows are allowed in this position.");
            }

            Element rowNode = parseRow(document, type, row);

            parent.appendChild(rowNode);
        }
    }

    private Element parseRow(Document document, RowType type, String row) {
        String[] values = FIELD_SEPARATOR.split(row, -1);
        List<String> fieldNames = type.fields();

        Element element = document.createElement(type.elementName());
        for (int i = 0; i < fieldNames.size() && i + 1 < values.length; i++) {
            Element child = document.createElement(fieldNames.get(i));
            child.setTextContent(values[i + 1]);
            element.appendChild(child);
        }

        return element;
    }

    private static List<List<String>> findSegmentsByType(List<String> rows, RowType type) {
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
