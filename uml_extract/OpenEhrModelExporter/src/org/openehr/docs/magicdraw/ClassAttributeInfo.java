package org.openehr.docs.magicdraw;

/**
 * @author Bostjan Lah
 */
public class ClassAttributeInfo {
    private String occurences = "";
    private String name = "";
    private String documentation = "";

    public String getOccurences() {
        return occurences;
    }

    public ClassAttributeInfo setOccurences(String occurences) {
        this.occurences = occurences;
        return this;
    }

    public String getName() {
        return name;
    }

    public ClassAttributeInfo setName(String name) {
        this.name = name;
        return this;
    }

    public String getDocumentation() {
        return documentation;
    }

    public ClassAttributeInfo setDocumentation(String documentation) {
        this.documentation = documentation;
        return this;
    }
}
