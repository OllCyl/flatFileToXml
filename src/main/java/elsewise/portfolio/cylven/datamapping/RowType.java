package elsewise.portfolio.cylven.datamapping;

import java.util.List;

enum RowType {
    PERSON("P", "person", true, List.of("firstname", "lastname")),
    FAMILY("F", "family", true, List.of("name", "born")),
    PHONE("T", "phone", false, List.of("mobile", "office")),
    ADDRESS("A", "address", false, List.of("street", "city", "postalcode"));

    private final String prefix;
    private final String elementName;
    private final boolean segmentStart;
    private final List<String> fields;

    RowType(String prefix, String elementName, boolean segmentStart, List<String> fields) {
        this.prefix = prefix;
        this.elementName = elementName;
        this.segmentStart = segmentStart;
        this.fields = fields;
    }

    String prefix() {
        return prefix;
    }

    String elementName() {
        return elementName;
    }

    boolean isSegmentStart() {
        return segmentStart;
    }

    List<String> fields() {
        return fields;
    }

    static RowType fromPrefix(String prefix) {
        for (RowType type : values()) {
            if (type.prefix.equals(prefix)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown row type: " + prefix);
    }
}
