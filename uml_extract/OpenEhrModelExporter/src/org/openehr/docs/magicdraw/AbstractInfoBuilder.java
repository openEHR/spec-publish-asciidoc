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

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Bostjan Lah
 */
public abstract class AbstractInfoBuilder<T> {
    protected final Formatter formatter;

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
                        builder.append(formatter.hardLineBreak());
                    }
                    builder.append(formatter.monospace(formatter.escape(line)));
                    add = true;
                }
            }
        }
        return builder.toString();
    }

    protected void addAttributes(List<ClassFeatureInfo> attributes, List<Property> properties, Map<String, Property> superClassAttributes) {
        properties.stream()
                .filter(p -> !superClassAttributes.containsKey(p.getName()))
                .filter(p -> !p.isReadOnly())
                .forEach(p -> addAttribute(attributes, p, OperationStatus.DEFINED));
        properties.stream()
                .filter(p -> superClassAttributes.containsKey(p.getName()))
                .filter(p -> !p.isReadOnly())
                .forEach(p -> addAttribute(attributes, p, OperationStatus.REDEFINED));
    }

    protected void addConstants(List<ClassFeatureInfo> attributes, List<Property> properties, Map<String, Property> superClassAttributes) {
        properties.stream()
                .filter(p -> !superClassAttributes.containsKey(p.getName()))
                .filter(StructuralFeature::isReadOnly)
                .forEach(p -> addAttribute(attributes, p, OperationStatus.DEFINED));
        properties.stream()
                .filter(p -> superClassAttributes.containsKey(p.getName()))
                .filter(StructuralFeature::isReadOnly)
                .forEach(p -> addAttribute(attributes, p, OperationStatus.REDEFINED));
    }

    /**
     * Build a ClassFeatureInfo object for property and add it to the attributes list.
     * @param attributes List of ClassFeatureInfo objects for this class so far built.
     * @param property the property to add.
     * @param attrStatus Status of attribute in this class: defined, redefined etc.
     */
    private void addAttribute(List<ClassFeatureInfo> attributes, Property property, OperationStatus attrStatus) {
        // create a ClassFeatureInfo with attribute documentation, occurrences and redefined marker
        ClassFeatureInfo classFeatureInfo = new ClassFeatureInfo()
                .setDocumentation(getDocumentation(property, formatter))
                .setStatus(formatSpecialOccurences(property.getLower(), property.getUpper()) +
                        (attrStatus.toString().isEmpty()? "" : " +" + System.lineSeparator() + "(" + attrStatus + ")"));

        // attribute signature
        StringBuilder sigBuilder = new StringBuilder(formatter.bold(property.getName()));
        sigBuilder.append(": ");

        // compute the type, based on type, except if there is a qualifier on the property, in which case use its type
        String type = property.getType() == null ? "" : property.getType().getName();
        Property qualifier = property.getAssociation() != null && property.hasQualifier() ? property.getQualifier().get(0) : null;
        StringBuilder typeInfo = new StringBuilder(formatType(type, qualifier, property.getLower(), property.getUpper()));

        // add '=' + default value, if defined
        ValueSpecification defaultValue = property.getDefaultValue();
        if (defaultValue instanceof LiteralString) {
            LiteralString value = (LiteralString)defaultValue;
            typeInfo.append("{nbsp}={nbsp}").append(formatter.escapeLiteral(value.getValue()));
        }

        // If there is any type information, append it
        if (typeInfo.length() > 0) {
            sigBuilder.append(formatter.monospace(typeInfo.toString()));
        }

        classFeatureInfo.setSignature(sigBuilder.toString());

        attributes.add(classFeatureInfo);
    }

    private String formatType(String type, Property qualifier, int lower, int upper) {
        String formattedType;

        // if there is no qualifier, output either the UML relation target type or List<target type>
        if (qualifier == null) {
            formattedType = upper == -1 || upper > 1 ? "List<" + type + '>' : type;
        }
        else {
            String qualifierType = qualifier.getType().getName();
            String qualifierName = qualifier.getName();

            // if there is a qualifier, but with no name, the output type is either the UML
            // qualifier type of List<qualifier type>
            if (qualifierName == null || qualifierName.isEmpty()) {
                formattedType = upper == -1 || upper > 1 ? "List<" + qualifierType + '>' : qualifierType;
            }
            // else if there is a qualifier name, it stands for a Hash key, and we output a Hash type sig
            // This should only occur with multiple relationships.
            else {
                formattedType = upper == -1 || upper > 1 ? "Hash<" + qualifierType + ',' + type + '>' : qualifierType;
            }
        }
        return formattedType;
    }

    /**
     * Format occurrences in the standard way.
     * @param lower lower value of occurrences.
     * @param upper upper value of occurrences.
     */
    private String formatInlineOccurences(int lower, int upper) {
        if (upper == -1)
            return lower == 0 ? "0..1" : "1";
        else
            return lower == upper ? "" + lower : lower + ".." + upper;
    }

    /**
     * Format occurrences in a way that accounts for 0..* in UML being represented as List<T>.
     * @param lower lower value of occurrences.
     * @param upper upper value of occurrences.
     */
    private String formatSpecialOccurences(int lower, int upper) {
        return upper == -1 ? lower + "..1" : lower + ".." + upper;
    }

    protected void addOperations(List<ClassFeatureInfo> features, List<Operation> operations, Map<String, Operation> superClassOperations) {
        for (Operation op : operations) {
            if (superClassOperations.containsKey(op.getName())) {
                if (superClassOperations.get(op.getName()).isAbstract())
                    addOperation(features, op, OperationStatus.EFFECTED);
                else
                    addOperation(features, op, OperationStatus.REDEFINED);
            }
            else
                addOperation(features, op, op.isAbstract()? OperationStatus.ABSTRACT : OperationStatus.DEFINED);
        }
    }

    /**
     * Build a ClassFeatureInfo for operation, and append it to the features list so far built.
     * @param features List of class features so far built.
     * @param operation UML operation definition.
     * @param opStatus Status of operation in this class: abstract, effected, defined etc.
     */
    private void addOperation(List<ClassFeatureInfo> features, Operation operation, OperationStatus opStatus) {
        // Create the documentation, which will include documentation for each
        // parameter that has it.
        StringBuilder opDocBuilder = new StringBuilder(getDocumentation(operation, formatter));
        opDocBuilder.append(System.lineSeparator());

        // Start building the operation signature
        // append the operation name, bolded
        StringBuilder opSigBuilder = new StringBuilder(formatter.bold(operation.getName()));

        // If there are parameters, output them within parentheses; also
        // add the parameter documentation to the documentary text
        if (operation.hasOwnedParameter()) {
            addSignatureParameters(opSigBuilder, operation.getOwnedParameter());
            opDocBuilder.append(System.lineSeparator());
            addDocumentParameters(opDocBuilder, operation.getOwnedParameter());
        }

        // If there is a return type, append to the signature it in monospace.
        String type = operation.getType() == null ? "" : operation.getType().getName();
        StringBuilder fullSigBuilder = type.isEmpty()
                ? new StringBuilder(opSigBuilder)
                : new StringBuilder(opSigBuilder + ": " + formatter.monospace(formatType(type, null, operation.getLower(), operation.getUpper())));

        // Output any operation pre- and post-conditions (UML constraints)
        addOperationConstraint(operation, fullSigBuilder);

        ClassFeatureInfo classFeatureInfo = new ClassFeatureInfo()
                .setStatus(opStatus.toString().isEmpty()? "" : "(" + opStatus + ")")
                .setDocumentation(opDocBuilder.toString());

        classFeatureInfo.setSignature(fullSigBuilder.toString());

        features.add(classFeatureInfo);
    }

    /**
     * Add parameters for a UML method in a class definition to the operation string.
     * @param parameters UML parameter definitions.
     * @param sigBuilder string builder containing method definition as a string.
     */
    protected void addSignatureParameters(StringBuilder sigBuilder, List<Parameter> parameters) {
        List<String> formattedParameters = new ArrayList<>();
        for (Parameter parameter : parameters) {
            String paramSignature = parameter.getName();
            if (!"return".equals(paramSignature) && !paramSignature.isEmpty()) {
                if (parameter.getType() == null)
                    formattedParameters.add(paramSignature);
                else {
                    formattedParameters.add(
                            paramSignature + ": " + formatter.monospace(
                                    formatType(parameter.getType().getName(), null, parameter.getLower(), parameter.getUpper()) +
                                            '[' + formatInlineOccurences(parameter.getLower(), parameter.getUpper()) + ']'
                            )
                    );
                }
            }
        }

        // if there are parameters, put them out on different lines, else just output "()"
        if (!formattedParameters.isEmpty()) {
            sigBuilder.append(" (").append(formatter.hardLineBreak());
            sigBuilder.append(String.join("," + formatter.hardLineBreak(), formattedParameters)).append(formatter.hardLineBreak());
            sigBuilder.append(')');
        }
        else
            sigBuilder.append(" ()");
    }

    /**
     * Add parameters for a UML method in a class definition to the operation string.
     * @param parameters UML parameter definitions.
     * @param docBuilder string builder containing parameter documentation.
     */
    protected void addDocumentParameters(StringBuilder docBuilder, List<Parameter> parameters) {
        List<String> formattedParameters = new ArrayList<>();
        for (Parameter parameter : parameters) {
            String paramName = parameter.getName();
            if (!"return".equals(paramName) && !paramName.isEmpty()) {
                String paramComment = getDocumentation(parameter, formatter);
                if (!paramComment.isEmpty()) {
                    formattedParameters.add(System.lineSeparator() + formatter.italicMonospace(paramName)  + ":: " + paramComment);
                }
            }
        }
        if (!formattedParameters.isEmpty()) {
            docBuilder.append (".Parameters");
            docBuilder.append(formatter.hardLineBreak());
            docBuilder.append ("[horizontal]");
            docBuilder.append(String.join("\n", formattedParameters));
        }
    }

    /**
     * Add the pre- or post-condition constraints attached to a UML method in a class definition
     * to the attribute string, each on a new line.
     * @param operation UML method definition.
     * @param builder string builder containing method definition as a string.
     */
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
