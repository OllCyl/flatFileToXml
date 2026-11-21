package elsewise.portfolio.cylven.datamapping;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class FlatFileConverterService {

    private static final Logger log = LoggerFactory.getLogger(FlatFileConverterService.class);
    private static final Pattern FIELD_SEPARATOR = Pattern.compile("\\|");
    private final FlatFileReaderService flatFileReaderService;

    public FlatFileConverterService(FlatFileReaderService flatFileReaderService) {
        this.flatFileReaderService = flatFileReaderService;
    }

    @PostConstruct
    public void convertAndLog() throws ParserConfigurationException, TransformerException {
        Document document = toDocument(flatFileReaderService.readInputLines());

        var transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        var writer = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(writer));
        log.info("Converted XML:\n{}", writer);
    }

    Document toDocument(List<String> rows) throws ParserConfigurationException {
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Element root = document.createElement("people");
        document.appendChild(root);

        splitIntoSegments(rows, RowType.PERSON.prefix()).forEach(personSegment ->
                root.appendChild(parsePerson(document, personSegment)));

        return document;
    }

    private Element parsePerson(Document document, List<String> personSegment) {
        if (!personSegment.getFirst().startsWith(RowType.PERSON.prefix())) {
            throw new IllegalArgumentException(
                    "Person segment must start with a " + RowType.PERSON.prefix()
                            + " row: " + personSegment.getFirst());
        }
        Element person = parseRow(document, RowType.PERSON, personSegment.getFirst());

        splitIntoSegments(personSegment.subList(1, personSegment.size()), RowType.FAMILY.prefix())
                .forEach(subSegment -> {
                    if (subSegment.getFirst().startsWith(RowType.FAMILY.prefix())) {
                        Element family = parseRow(document, RowType.FAMILY, subSegment.getFirst());
                        parseLeafRows(document, subSegment.subList(1, subSegment.size()))
                                .forEach(family::appendChild);
                        person.appendChild(family);
                    } else {
                        parseLeafRows(document, subSegment).forEach(person::appendChild);
                    }
                });

        return person;
    }

    private List<Element> parseLeafRows(Document document, List<String> rows) {
        return rows.stream()
                .map(row -> parseLeafRow(document, row))
                .toList();
    }

    private Element parseLeafRow(Document document, String row) {
        RowType type = RowType.fromPrefix(FIELD_SEPARATOR.split(row, 2)[0]);
        if (type.isSegmentStart()) {
            throw new IllegalArgumentException("Expected leaf row (T/A), got segment start: " + row);
        }
        return parseRow(document, type, row);
    }

    private Element parseRow(Document document, RowType type, String row) {
        return buildElement(document, type.elementName(), FIELD_SEPARATOR.split(row), type.fields());
    }

    private static Element buildElement(Document document, String name, String[] rowValues, List<String> elementNames) {
        Element element = document.createElement(name);

        for (int i = 0; i < elementNames.size() && i + 1 < rowValues.length; i++) {
            Element child = document.createElement(elementNames.get(i));
            child.setTextContent(rowValues[i + 1]);
            element.appendChild(child);
        }

        return element;
    }

    private static List<List<String>> splitIntoSegments(List<String> rows, String prefix) {
        List<List<String>> segments = new ArrayList<>();
        List<String> current = new ArrayList<>();

        for (String row : rows) {
            // When we encounter a new prefix, we close the current segment and start a new one
            if (row.startsWith(prefix)) {
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
