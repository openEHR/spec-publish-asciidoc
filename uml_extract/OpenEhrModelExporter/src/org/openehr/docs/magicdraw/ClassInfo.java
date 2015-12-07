package org.openehr.docs.magicdraw;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Bostjan Lah
 */
public class ClassInfo implements Comparable<ClassInfo> {
    private final String type;
    private String className = "";
    private String documentation = "";
    private String parentClassName;
    private String indexComponent = "";
    private String indexPackage = "";
    private String indexSubPackage = "";
    private boolean abstractClass;

    private final List<ClassAttributeInfo> attributes = new ArrayList<>();
    private final List<ClassAttributeInfo> constants = new ArrayList<>();
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

    public List<ClassAttributeInfo> getConstants() {
        return constants;
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

    public String getIndexComponent() {
        return indexComponent;
    }

    public void setIndexComponent(String indexComponent) {
        this.indexComponent = indexComponent;
    }

    public String getIndexPackage() {
        return indexPackage;
    }

    public void setIndexPackage(String indexPackage) {
        this.indexPackage = indexPackage;
    }

    public String getIndexSubPackage() {
        return indexSubPackage;
    }

    public void setIndexSubPackage(String indexSubPackage) {
        this.indexSubPackage = indexSubPackage;
    }

    @Override
    public int compareTo(@Nonnull ClassInfo o) {
        int i = indexComponent.compareTo(o.indexComponent);
        if (i != 0) {
            return i;
        }

        int j = indexPackage.compareTo(o.indexPackage);
        if (j != 0) {
            return j;
        }

        int k = indexSubPackage.compareTo(o.indexSubPackage);
        if (k != 0) {
            return k;
        }

        return className.compareTo(o.className);
    }
}
