package org.openehr.docs.magicdraw;

import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.NamedElement;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Bostjan Lah
 */
public class ClassInfoBuilder extends AbstractInfoBuilder<com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class> {
    public ClassInfoBuilder(Formatter formatter) {
        super(formatter);
    }

    @Override
    public ClassInfo build(com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class element) {
        String className = element.getName();
        ClassInfo classInfo = new ClassInfo("Class")
                .setClassName(className)
                .setDocumentation(getDocumentation(element, getFormatter()))
                .setAbstractClass(element.isAbstract());
        setHierarchy(element.getQualifiedName(), classInfo);

        Set<String> superClassAttributes = new HashSet<>();
        Set<String> superClassOperations = new HashSet<>();

        if (element.hasSuperClass()) {
            classInfo.setParentClassName(String.join(", ", element.getSuperClass().stream()
                                            .map(NamedElement::getName)
                                            .collect(Collectors.toList())));
            getSuperClassData(element, superClassAttributes, superClassOperations);
        }

        if (element.hasOwnedAttribute()) {
            addAttributes(classInfo.getAttributes(), element.getOwnedAttribute(), superClassAttributes);
            addConstants(classInfo.getConstants(), element.getOwnedAttribute(), superClassAttributes);
        }
        if (element.hasOwnedOperation()) {
            addOperations(classInfo.getFunctions(), element.getOwnedOperation(), superClassOperations);
        }

        addConstraints(classInfo.getConstraints(), element.get_constraintOfConstrainedElement());

        return classInfo;
    }

    private void getSuperClassData(Class element, Set<String> superClassAttributes, Set<String> superClassOperations) {
        for (Class superClass : element.getSuperClass()) {
            superClassAttributes.addAll(superClass.getOwnedAttribute().stream().map(NamedElement::getName).collect(Collectors.toSet()));
            superClassOperations.addAll(superClass.getOwnedOperation().stream().map(NamedElement::getName).collect(Collectors.toSet()));
            getSuperClassData(superClass, superClassAttributes, superClassOperations);
        }
    }
}
