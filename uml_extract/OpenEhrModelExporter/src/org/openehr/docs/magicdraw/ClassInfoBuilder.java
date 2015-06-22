package org.openehr.docs.magicdraw;

import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Comment;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Constraint;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.LiteralString;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.NamedElement;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.OpaqueExpression;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Operation;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Parameter;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Property;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.ValueSpecification;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Bostjan Lah
 */
public class ClassInfoBuilder {
    private ClassInfoBuilder() {
    }

    public static ClassInfo build(com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class element, Formatter formatter) {
        String className = element.getName();
        ClassInfo classInfo = new ClassInfo()
                .setClassName(className)
                .setDocumentation(getDocumentation(element, formatter))
                .setAbstractClass(element.isAbstract());

        if (element.hasSuperClass()) {
            classInfo.setParentClassName(element.getSuperClass().iterator().next().getName());
        }

        Set<String> superClassAttributes = new HashSet<>();
        Set<String> superClassOperations = new HashSet<>();
        getSuperClassData(element, superClassAttributes, superClassOperations);

        if (element.hasOwnedAttribute()) {
            addAttributes(classInfo.getAttributes(), element.getOwnedAttribute(), formatter, superClassAttributes);
        }
        if (element.hasOwnedOperation()) {
            addOperations(classInfo.getFunctions(), element.getOwnedOperation(), formatter, superClassOperations);
        }

        addConstraints(classInfo.getConstraints(), element.get_constraintOfConstrainedElement(), formatter);

        return classInfo;
    }

    private static void getSuperClassData(Class element, Set<String> superClassAttributes, Set<String> superClassOperations) {
        if (element.hasSuperClass()) {
            for (Class superClass : element.getSuperClass()) {
                superClassAttributes.addAll(superClass.getOwnedAttribute().stream().map(NamedElement::getName).collect(Collectors.toSet()));
                superClassOperations.addAll(superClass.getOwnedOperation().stream().map(NamedElement::getName).collect(Collectors.toSet()));
                getSuperClassData(superClass, superClassAttributes, superClassOperations);
            }
        }
    }

    private static void addConstraints(List<ConstraintInfo> constraints, Collection<Constraint> constraintOfConstrainedElement, Formatter formatter) {
        for (Constraint constraint : constraintOfConstrainedElement) {
            constraints.add(new ConstraintInfo().setDocumentation(formatConstraint(constraint, formatter)));
        }
    }

    private static String formatConstraint(Constraint constraint, Formatter formatter) {
        StringBuilder builder = new StringBuilder(formatter.italicBold(constraint.getName())).append(": ");
        if (constraint.getSpecification() instanceof OpaqueExpression) {
            OpaqueExpression opaqueExpression = (OpaqueExpression)constraint.getSpecification();
            if (opaqueExpression.hasBody()) {
                boolean add = false;
                for (String line : opaqueExpression.getBody()) {
                    if (add) {
                        builder.append(formatter.newParagraph());
                    }
                    builder.append(line);
                    add = true;
                }
            }
        }
        return builder.toString();
    }

    private static void addAttributes(List<ClassAttributeInfo> attributes, List<Property> properties, Formatter formatter, Set<String> superClassAttributes) {
        properties.stream().filter(p -> !superClassAttributes.contains(p.getName())).forEach(p -> addAttribute(attributes, formatter, p, false));
        properties.stream().filter(p -> superClassAttributes.contains(p.getName())).forEach(p -> addAttribute(attributes, formatter, p, true));
    }

    private static void addAttribute(List<ClassAttributeInfo> attributes, Formatter formatter, Property property, boolean redefined) {
        String type = property.getType() == null ? "" : property.getType().getName();
        ClassAttributeInfo classAttributeInfo = new ClassAttributeInfo()
                .setDocumentation(getDocumentation(property, formatter))
                .setOccurences(formatOccurences(property.getLower(), property.getUpper()) + (redefined ? " +" + System.lineSeparator() + "(redefined)" : ""));

        StringBuilder name = new StringBuilder(formatter.bold(property.getName()));
        name.append(": ");
        StringBuilder typeInfo = new StringBuilder(formatType(type, property.getUpper()));
        ValueSpecification defaultValue = property.getDefaultValue();
        if (defaultValue instanceof LiteralString) {
            LiteralString value = (LiteralString)defaultValue;
            typeInfo.append("{nbsp}={nbsp}").append(formatter.escapeLiteral(value.getValue()));
        }
        if (typeInfo.length() > 0) {
            name.append(formatter.monospace(typeInfo.toString()));
        }
        classAttributeInfo.setName(name.toString());
        attributes.add(classAttributeInfo);
    }

    private static String formatType(String type, int upper) {
        return upper == -1 || upper > 1 ? "List<" + type + '>' : type;
    }

    private static String formatOccurences(int lower, int upper) {
        return upper == -1 ? lower + "..1" : lower + ".." + upper;

    }

    private static void addOperations(List<ClassAttributeInfo> attributes, List<Operation> operations, Formatter formatter, Set<String> superClassOperations) {
        operations.stream().filter(op -> !superClassOperations.contains(op.getName())).forEach(
                operation -> addOperation(attributes, formatter, operation, false));
        operations.stream().filter(op -> superClassOperations.contains(op.getName())).forEach(
                operation -> addOperation(attributes, formatter, operation, true));
    }

    private static void addOperation(List<ClassAttributeInfo> attributes, Formatter formatter, Operation operation, boolean effected) {
        String type = operation.getType() == null ? "" : operation.getType().getName();
        ClassAttributeInfo classAttributeInfo = new ClassAttributeInfo()
                .setOccurences(effected ? "(effected)" : "")
                .setDocumentation(getDocumentation(operation, formatter));

        StringBuilder nameInfo = new StringBuilder(formatter.bold(operation.getName()));
        if (operation.hasOwnedParameter()) {
            addParameters(nameInfo, operation.getOwnedParameter(), formatter);
        }
        StringBuilder builder = new StringBuilder(nameInfo + ": " + formatter.monospace(formatType(type, operation.getUpper())));

        addOperationConstraint(operation, builder, formatter);
        classAttributeInfo.setName(builder.toString());
        attributes.add(classAttributeInfo);
    }

    private static void addParameters(StringBuilder builder, List<Parameter> parameters, Formatter formatter) {
        List<String> formattedParameters = new ArrayList<>();
        for (Parameter parameter : parameters) {
            String name = parameter.getName();
            if (!"return".equals(name) && !name.isEmpty()) {
                if (parameter.getType() == null) {
                    formattedParameters.add(name);
                } else {
                    formattedParameters.add(name + ": " + formatter.monospace(formatType(parameter.getType().getName(), parameter.getUpper())));
                }
            }
        }
        if (!formattedParameters.isEmpty()) {
            builder.append(" (").append(String.join(", ", formattedParameters)).append(')');
        }
    }

    private static void addOperationConstraint(Operation operation, StringBuilder builder, Formatter formatter) {
        for (Constraint constraint : operation.get_constraintOfConstrainedElement()) {
            builder.append(" +").append(System.lineSeparator()).append(formatConstraint(constraint, formatter));
        }
    }

    @SuppressWarnings("HardcodedLineSeparator")
    private static String getDocumentation(Element element, Formatter formatter) {
        return String.join(formatter.newParagraph(), element.getOwnedComment().stream()
                .map(Comment::getBody)
                .flatMap(body -> Stream.of(body.split("\n")))
                .filter(line -> !line.trim().isEmpty())
                .collect(Collectors.toList()));
    }
}
