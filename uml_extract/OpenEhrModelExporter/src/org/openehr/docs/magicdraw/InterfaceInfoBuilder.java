package org.openehr.docs.magicdraw;

import com.nomagic.uml2.ext.magicdraw.classes.mdinterfaces.Interface;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Bostjan Lah
 */
public class InterfaceInfoBuilder extends AbstractInfoBuilder<Interface> {
    public InterfaceInfoBuilder(Formatter formatter) {
        super(formatter);
    }

    @Override
    public ClassInfo build(Interface element) {
        String className = element.getName();
        ClassInfo classInfo = new ClassInfo("Interface")
                .setClassName(className)
                .setDocumentation(getDocumentation(element, getFormatter()))
                .setAbstractClass(element.isAbstract());
        setHierarchy(element.getQualifiedName(), classInfo);

        Set<String> superClassAttributes = new HashSet<>();
        Set<String> superClassOperations = new HashSet<>();

        if (element.hasOwnedAttribute()) {
            addAttributes(classInfo.getAttributes(), element.getOwnedAttribute(), superClassAttributes);
        }
        if (element.hasOwnedOperation()) {
            addOperations(classInfo.getFunctions(), element.getOwnedOperation(), superClassOperations);
        }

        addConstraints(classInfo.getConstraints(), element.get_constraintOfConstrainedElement());

        return classInfo;
    }

}
