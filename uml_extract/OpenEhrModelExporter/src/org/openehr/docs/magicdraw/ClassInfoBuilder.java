package org.openehr.docs.magicdraw;

import com.nomagic.uml2.ext.magicdraw.auxiliaryconstructs.mdtemplates.TemplateSignature;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.NamedElement;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Operation;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Property;

import java.util.HashMap;
import java.util.Map;
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

        // check for template parts
        TemplateSignature tplSig = element.getOwnedTemplateSignature();
        if (tplSig != null) {
            // List<TemplateParameter>;
            // FIXME: we have to get the name using getHumanName() but remove
            // the 'Class '. There is no other way to get the parameter type name
            String tplParamsStr = tplSig.getOwnedParameter().stream()
                    .map(t -> t.getParameteredElement().getHumanName().replace("Class ", ""))
                    .collect(Collectors.joining(","));
            className = className + '<' + tplParamsStr + '>';
        }

        ClassInfo classInfo = new ClassInfo("Class")
                .setClassTypeName(className)
                .setDocumentation(getDocumentation(element, getFormatter()))
                .setAbstractClass(element.isAbstract());

        setHierarchy(element.getQualifiedName(), classInfo);

        Map<String, Property> superClassAttributes = new HashMap<>();
        Map<String, Operation> superClassOperations = new HashMap<>();

        if (element.hasSuperClass()) {
            classInfo.setParentClassName(String.join(", ", element.getSuperClass().stream()
                                            .map(NamedElement::getName)
                                            .map(formatter::monospace)
                                            .collect(Collectors.toList())));
            getSuperClassData(element, superClassAttributes, superClassOperations);
        }

        if (element.hasOwnedAttribute()) {
            addAttributes(classInfo.getAttributes(), element.getOwnedAttribute(), superClassAttributes);
            addConstants(classInfo.getConstants(), element.getOwnedAttribute(), superClassAttributes);
        }
        if (element.hasOwnedOperation()) {
            addOperations(classInfo.getOperations(), element.getOwnedOperation(), superClassOperations);
        }

        addConstraints(classInfo.getConstraints(), element.get_constraintOfConstrainedElement());

        return classInfo;
    }

    private void getSuperClassData(Class element, Map<String, Property> superClassAttributes, Map<String, Operation> superClassOperations) {
        for (Class superClass : element.getSuperClass()) {
            superClassAttributes.putAll(superClass.getOwnedAttribute().stream().collect(Collectors.toMap(NamedElement::getName, p -> p)));
            superClassOperations.putAll(superClass.getOwnedOperation().stream().collect(Collectors.toMap(NamedElement::getName, p -> p)));
            getSuperClassData(superClass, superClassAttributes, superClassOperations);
        }
    }
}
