package org.openehr.docs.magicdraw;

/**
 * @author Bostjan Lah
 */
public class ClassFeatureInfo {
    private String status = "";
    private String signature = "";
    private String documentation = "";

    public String getStatus() {
        return status;
    }

    public ClassFeatureInfo setStatus(String status) {
        this.status = status;
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
