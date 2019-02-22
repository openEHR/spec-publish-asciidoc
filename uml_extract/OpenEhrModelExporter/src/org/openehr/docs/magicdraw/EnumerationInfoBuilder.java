package org.openehr.docs.magicdraw;

import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Enumeration;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.EnumerationLiteral;

import java.util.List;

/**
 * @author Bostjan Lah
 */
public class EnumerationInfoBuilder extends AbstractInfoBuilder<Enumeration> {
    public EnumerationInfoBuilder(Formatter formatter) {
        super(formatter);
    }

    @Override
    public ClassInfo build(Enumeration element) {
        String className = element.getName();
        ClassInfo classInfo = new ClassInfo("Enumeration")
                .setClassTypeName(className)
                .setDocumentation(getDocumentation(element, getFormatter()));
        setHierarchy(element.getQualifiedName(), classInfo);

        if (element.hasOwnedLiteral()) {
            addLiterals(classInfo.getAttributes(), element.getOwnedLiteral(), getFormatter());
        }

        return classInfo;
    }

    private void addLiterals(List<ClassFeatureInfo> attributes, List<EnumerationLiteral> ownedLiteral, Formatter formatter) {
        for (EnumerationLiteral literal : ownedLiteral) {
            attributes.add(new ClassFeatureInfo()
                                   .setSignature(literal.getName())
                                   .setDocumentation(getDocumentation(literal, formatter)));
        }
    }
}
