package org.openehr.docs.magicdraw;

import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Comment;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Constraint;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.LiteralString;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.OpaqueExpression;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Operation;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Parameter;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Property;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.StructuralFeature;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.ValueSpecification;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Bostjan Lah
 */
public abstract class AbstractInfoBuilder<T> {
    private final Formatter formatter;

    protected AbstractInfoBuilder(Formatter formatter) {
        this.formatter = formatter;
    }

    @SuppressWarnings("HardcodedLineSeparator")
    protected String getDocumentation(Element element, Formatter formatter) {
        List<String> lines = element.getOwnedComment().stream()
                .map(Comment::getBody)
                .flatMap(body -> Stream.of(body.split("\n")))
                .map(formatter::escape)
                .collect(Collectors.toList());
        return String.join(System.lineSeparator(), lines);
    }

    public abstract ClassInfo build(T element);

    protected Formatter getFormatter() {
        return formatter;
    }

    protected void addConstraints(List<ConstraintInfo> constraints, Collection<Constraint> constraintOfConstrainedElement) {
        for (Constraint constraint : constraintOfConstrainedElement) {
            constraints.add(new ConstraintInfo().setDocumentation(formatConstraint(constraint)));
        }
    }

    private String formatConstraint(Constraint constraint) {
        StringBuilder builder = new StringBuilder(formatter.italicBold(constraint.getName())).append(": ");
        if (constraint.getSpecification() instanceof OpaqueExpression) {
            OpaqueExpression opaqueExpression = (OpaqueExpression)constraint.getSpecification();
            if (opaqueExpression.hasBody()) {
                boolean add = false;
                for (String line : opaqueExpression.getBody()) {
                    if (add) {
                        builder.append(formatter.newParagraph());
                    }
                    builder.append(formatter.escape(line));
                    add = true;
                }
            }
        }
        return builder.toString();
    }

    protected void addAttributes(List<ClassAttributeInfo> attributes, List<Property> properties, Set<String> superClassAttributes) {
        properties.stream()
                .filter(p -> !superClassAttributes.contains(p.getName()))
                .filter(p -> !p.isReadOnly())
                .forEach(p -> addAttribute(attributes, p, false));
        properties.stream()
                .filter(p -> superClassAttributes.contains(p.getName()))
                .filter(p -> !p.isReadOnly())
                .forEach(p -> addAttribute(attributes, p, true));
    }

    protected void addConstants(List<ClassAttributeInfo> attributes, List<Property> properties, Set<String> superClassAttributes) {
        properties.stream()
                .filter(p -> !superClassAttributes.contains(p.getName()))
                .filter(StructuralFeature::isReadOnly)
                .forEach(p -> addAttribute(attributes, p, false));
        properties.stream()
                .filter(p -> superClassAttributes.contains(p.getName()))
                .filter(StructuralFeature::isReadOnly)
                .forEach(p -> addAttribute(attributes, p, true));
    }

    private void addAttribute(List<ClassAttributeInfo> attributes, Property property, boolean redefined) {
        String type = property.getType() == null ? "" : property.getType().getName();
        ClassAttributeInfo classAttributeInfo = new ClassAttributeInfo()
                .setDocumentation(getDocumentation(property, formatter))
                .setOccurences(formatOccurences(property.getLower(), property.getUpper()) + (redefined ? " +" + System.lineSeparator() + "(redefined)" : ""));

        StringBuilder name = new StringBuilder(formatter.bold(property.getName()));
        name.append(": ");

        Property qualifier = property.getAssociation() != null && property.hasQualifier() ? property.getQualifier().get(0) : null;
        StringBuilder typeInfo = new StringBuilder(formatType(type, qualifier, property.getLower(), property.getUpper()));

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

    private String formatType(String type, Property qualifier, int lower, int upper) {
        String formattedType;
        if (qualifier == null) {
            formattedType = upper == -1 || upper > 1 ? "List<" + type + '>' : type;
        } else {
            String qualifierType = qualifier.getType().getName();
            formattedType = upper == -1 || upper > 1 ? "Hash<" + type + ',' + qualifierType + '>' : type;
        }
        return formattedType;
    }

    private String formatOccurences(int lower, int upper) {
        return upper == -1 ? lower + "..1" : lower + ".." + upper;

    }

    protected void addOperations(List<ClassAttributeInfo> attributes, List<Operation> operations, Set<String> superClassOperations) {
        operations.stream().filter(op -> !superClassOperations.contains(op.getName()))
                .forEach(operation -> addOperation(attributes, operation, false));
        operations.stream().filter(op -> superClassOperations.contains(op.getName()))
                .forEach(operation -> addOperation(attributes, operation, true));
    }

    private void addOperation(List<ClassAttributeInfo> attributes, Operation operation, boolean effected) {
        String type = operation.getType() == null ? "" : operation.getType().getName();
        ClassAttributeInfo classAttributeInfo = new ClassAttributeInfo()
                .setOccurences(effected ? "(effected)" : "")
                .setDocumentation(getDocumentation(operation, formatter));

        StringBuilder nameInfo = new StringBuilder(formatter.bold(operation.getName()));
        if (operation.hasOwnedParameter()) {
            addParameters(nameInfo, operation.getOwnedParameter());
        }
        StringBuilder builder = type.isEmpty()
                ? new StringBuilder(nameInfo)
                : new StringBuilder(nameInfo + ": " + formatter.monospace(formatType(type, null, operation.getLower(), operation.getUpper())));

        addOperationConstraint(operation, builder);
        classAttributeInfo.setName(builder.toString());
        attributes.add(classAttributeInfo);
    }

    protected void addParameters(StringBuilder builder, List<Parameter> parameters) {
        List<String> formattedParameters = new ArrayList<>();
        for (Parameter parameter : parameters) {
            String name = parameter.getName();
            if (!"return".equals(name) && !name.isEmpty()) {
                if (parameter.getType() == null) {
                    formattedParameters.add(name);
                } else {
                    formattedParameters.add(
                            name + ": " + formatter.monospace(formatType(parameter.getType().getName(), null, parameter.getLower(), parameter.getUpper())));
                }
            }
        }
        if (!formattedParameters.isEmpty()) {
            builder.append(" (").append(String.join(", ", formattedParameters)).append(')');
        }
    }

    private void addOperationConstraint(Operation operation, StringBuilder builder) {
        for (Constraint constraint : operation.get_constraintOfConstrainedElement()) {
            builder.append(" +").append(System.lineSeparator()).append(formatConstraint(constraint));
        }
    }

    protected void setHierarchy(String qualifiedName, ClassInfo classInfo) {
        // this is hard-coded for openehr atm
        String[] parts = qualifiedName.split("::");
        if (parts.length > 4) {
            classInfo.setIndexComponent(parts[0]);
            classInfo.setIndexSubPackage(parts[4]);
            StringBuilder indexPackage = new StringBuilder();
            for (int i = 1; i < 4; i++) {
                indexPackage.append('.').append(parts[i]);
            }
            classInfo.setIndexPackage(indexPackage.substring(1));
        }
    }
}
