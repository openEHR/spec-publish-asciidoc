package org.openehr.docs.magicdraw;

import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Enumeration;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.EnumerationLiteral;

import java.util.List;

/**
 * @author Bostjan Lah
 */
public class EnumerationInfoBuilder extends AbstractInfoBuilder<Enumeration> {
    private final Formatter formatter;

    public EnumerationInfoBuilder(Formatter formatter) {
        this.formatter = formatter;
    }

    @Override
    public ClassInfo build(Enumeration element) {
        String className = element.getName();
        ClassInfo classInfo = new ClassInfo("Enumeration")
                .setClassName(className)
                .setDocumentation(getDocumentation(element, formatter));

        if (element.hasOwnedLiteral()) {
            addLiterals(classInfo.getAttributes(), element.getOwnedLiteral(), formatter);
        }

        return classInfo;
    }

    private void addLiterals(List<ClassAttributeInfo> attributes, List<EnumerationLiteral> ownedLiteral, Formatter formatter) {
        for (EnumerationLiteral literal : ownedLiteral) {
            attributes.add(new ClassAttributeInfo()
                                   .setName(literal.getName())
                                   .setDocumentation(getDocumentation(literal, formatter)));
        }
    }
}
