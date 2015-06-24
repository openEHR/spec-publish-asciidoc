package org.openehr.docs.magicdraw;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Bostjan Lah
 */
public class ClassInfo {
    private final String type;
    private String className = "";
    private String documentation = "";
    private String parentClassName;
    private boolean abstractClass;

    private final List<ClassAttributeInfo> attributes = new ArrayList<>();
    private final List<ClassAttributeInfo> functions = new ArrayList<>();
    private final List<ConstraintInfo> constraints = new ArrayList<>();

    public ClassInfo(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public String getClassName() {
        return className;
    }

    public ClassInfo setClassName(String className) {
        this.className = className;
        return this;
    }

    public String getDocumentation() {
        return documentation;
    }

    public ClassInfo setDocumentation(String documentation) {
        this.documentation = documentation;
        return this;
    }

    public String getParentClassName() {
        return parentClassName;
    }

    public ClassInfo setParentClassName(String parentClassName) {
        this.parentClassName = parentClassName;
        return this;
    }

    public List<ClassAttributeInfo> getAttributes() {
        return attributes;
    }

    public List<ClassAttributeInfo> getFunctions() {
        return functions;
    }

    public List<ConstraintInfo> getConstraints() {
        return constraints;
    }

    public boolean isAbstractClass() {
        return abstractClass;
    }

    public ClassInfo setAbstractClass(boolean abstractClass) {
        this.abstractClass = abstractClass;
        return this;
    }
}
