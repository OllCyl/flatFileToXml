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

@Service
public class FlatFileConverterService {

    private static final Logger log = LoggerFactory.getLogger(FlatFileConverterService.class);

    private final FlatFileReaderService flatFileReaderService;

    public FlatFileConverterService(FlatFileReaderService flatFileReaderService) {
        this.flatFileReaderService = flatFileReaderService;
    }

    @PostConstruct
    public void printFileContent() throws ParserConfigurationException, TransformerException {
        Document document = convert(flatFileReaderService.getFileContent());

        var transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        var writer = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(writer));
        log.info("Konverterad XML:\n{}", writer);
    }

    Document convert(List<String> lines) throws ParserConfigurationException {
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Element root = document.createElement("people");
        document.appendChild(root);

        groupRowsByPrefix(lines, "P").forEach(personSegment ->
                root.appendChild(parsePerson(document, personSegment)));

        return document;
    }

    private Element parsePerson(Document document, List<String> personSegment) {
        Element person = createElementFromRow(document, "person",
                personSegment.getFirst().split("\\|"), "firstname", "lastname");

        groupRowsByPrefix(personSegment.subList(1, personSegment.size()), "F").forEach(group -> {
            if (group.getFirst().startsWith("F")) {
                Element family = createElementFromRow(document, "family",
                        group.getFirst().split("\\|"), "name", "born");
                rowsToElements(document, group.subList(1, group.size()))
                        .forEach(family::appendChild);
                person.appendChild(family);
            } else {
                rowsToElements(document, group).forEach(person::appendChild);
            }
        });

        return person;
    }

    private List<Element> rowsToElements(Document document, List<String> rows) {
        return rows.stream()
                .map(row -> rowToElement(document, row))
                .toList();
    }

    private Element rowToElement(Document document, String row) {
        String[] fields = row.split("\\|");

        return switch (fields[0]) {
            case "T" -> createElementFromRow(document, "phone", fields, "mobile", "office");
            case "A" -> createElementFromRow(document, "address", fields, "street", "city", "postalcode");
            default -> throw new IllegalArgumentException("Okänd radtyp: " + fields[0]);
        };
    }

    private static Element createElementFromRow(Document document, String name, String[] rowValues, String... elementNames) {
        Element element = document.createElement(name);
        
        for (int i = 0; i < elementNames.length && i + 1 < rowValues.length; i++) {
            Element child = document.createElement(elementNames[i]);
            child.setTextContent(rowValues[i + 1]);
            element.appendChild(child);
        }

        return element;
    }

    private static List<List<String>> groupRowsByPrefix(List<String> lines, String prefix) {
        List<List<String>> segments = new ArrayList<>();
        List<String> current = new ArrayList<>();

        for (String line : lines) {
            if (line.startsWith(prefix)) {
                if (!current.isEmpty()) {
                    segments.add(current);   // ny start-markör stänger förra segmentet
                }
                current = new ArrayList<>(); // och öppnar nästa
            }
            current.add(line);
        }
        if (!current.isEmpty()) {
            segments.add(current);           // slutet på raderna stänger sista segmentet
        }
        
        return segments;
    }
}
