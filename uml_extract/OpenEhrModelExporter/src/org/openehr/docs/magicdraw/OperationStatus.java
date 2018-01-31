package org.openehr.docs.magicdraw;

public enum OperationStatus {
    // operation is abstract in current class
    ABSTRACT ("abstract"),

    // operation effects previous abstract definition
    EFFECTED ("effected"),

    // operation effects previous abstract definition
    REDEFINED ("redefined"),

    // operation is defined in current class
    DEFINED ("");

    private String literalName;
    OperationStatus(String litName) {
        this.literalName = litName;
    }

    @Override
    public String toString(){
        return literalName;
    }
}
