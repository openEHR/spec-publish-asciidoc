package org.openehr.docs.magicdraw;

/**
 * @author Bostjan Lah
 */
public class ClassFeatureInfo {
    private String occurrences = "";
    private String name = "";
    private String documentation = "";

    public String getOccurrences() {
        return occurrences;
    }

    public ClassFeatureInfo setOccurrences(String occurrences) {
        this.occurrences = occurrences;
        return this;
    }

    public String getName() {
        return name;
    }

    public ClassFeatureInfo setName(String name) {
        this.name = name;
        return this;
    }

    public String getDocumentation() {
        return documentation;
    }

    public ClassFeatureInfo setDocumentation(String documentation) {
        this.documentation = documentation;
        return this;
    }
}
