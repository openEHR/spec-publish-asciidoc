package org.openehr.docs.magicdraw;

/**
 * @author Bostjan Lah
 */
public class ClassFeatureInfo {
    private String occurrences = "";
    private String signature = "";
    private String documentation = "";

    public String getOccurrences() {
        return occurrences;
    }

    public ClassFeatureInfo setOccurrences(String occurrences) {
        this.occurrences = occurrences;
        return this;
    }

    public String getSignature() {
        return signature;
    }

    public ClassFeatureInfo setSignature(String signature) {
        this.signature = signature;
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
