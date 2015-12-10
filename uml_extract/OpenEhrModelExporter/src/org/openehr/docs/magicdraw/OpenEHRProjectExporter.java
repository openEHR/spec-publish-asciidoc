package org.openehr.docs.magicdraw;

import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.export.image.ImageExporter;
import com.nomagic.magicdraw.uml.symbols.DiagramPresentationElement;
import com.nomagic.uml2.ext.jmi.helpers.ModelHelper;
import com.nomagic.uml2.ext.magicdraw.classes.mdinterfaces.Interface;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Enumeration;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.NamedElement;
import org.openehr.docs.magicdraw.exception.OpenEhrExporterException;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Action which displays its name.
 *
 * @author Bostjan Lah
 */
public class OpenEHRProjectExporter {
    private static final String ADOC_FILE_EXTENSION = ".adoc";

    private static final String DIAGRAMS_FOLDER = "diagrams";
    private static final String CLASSES_FOLDER = "classes";
    // component, release, html file, subref classname + type, description
    private static final String INDEX_LINK_FORMAT = "[.xcode]\n* http://www.openehr.org/releases/%s/%s/%s.html#_%s_%s[%s]";

    private final Formatter formatter = new AsciidocFormatter();
    private final String headingPrefix;
    private final Set<String> rootPackageNames;

    private final String indexRelease;

    public OpenEHRProjectExporter(int headingLevel, Set<String> rootPackageNames, String indexRelease) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < headingLevel; i++) {
            builder.append('=');
        }
        headingPrefix = builder.toString();
        this.rootPackageNames = rootPackageNames;
        this.indexRelease = indexRelease;
    }

    public void exportProject(File outputFolder, Project project) throws Exception {
        File classesFolder = new File(outputFolder, CLASSES_FOLDER);
        if (!classesFolder.exists()) {
            if (!classesFolder.mkdir()) {
                throw new OpenEhrExporterException("Unable to create folder: " + classesFolder);
            }
        }

        ClassInfoBuilder classInfoBuilder = new ClassInfoBuilder(formatter);
        Collection<? extends Element> umlClasses = ModelHelper.getElementsOfType(project.getModel(),
                                                                                 new Class[]{com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class.class},
                                                                                 true);
        List<ClassInfo> classes = umlClasses.stream()
                .map(e -> (com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class)e)
                .filter(this::matchesRootPackages)
                .map(classInfoBuilder::build)
                .collect(Collectors.toList());
        classes.forEach(cl -> exportClass(cl, classesFolder));

        Collection<? extends Element> umlInterfaces = ModelHelper.getElementsOfType(project.getModel(),
                                                                                    new Class[]{Interface.class},
                                                                                    true);
        InterfaceInfoBuilder interfaceInfoBuilder = new InterfaceInfoBuilder(formatter);
        List<ClassInfo> interfaces = umlInterfaces.stream()
                .map(e -> (Interface)e)
                .filter(this::matchesRootPackages)
                .map(interfaceInfoBuilder::build)
                .collect(Collectors.toList());
        interfaces.forEach(cl -> exportClass(cl, classesFolder));

        Collection<? extends Element> umlEnumerations = ModelHelper.getElementsOfType(project.getModel(),
                                                                                      new Class[]{Enumeration.class},
                                                                                      true);

        EnumerationInfoBuilder enumerationInfoBuilder = new EnumerationInfoBuilder(formatter);
        List<ClassInfo> enumerations = umlEnumerations.stream()
                .map(e -> (Enumeration)e)
                .filter(this::matchesRootPackages)
                .map(enumerationInfoBuilder::build)
                .collect(Collectors.toList());
        enumerations.forEach(en -> exportClass(en, classesFolder));

        if (indexRelease != null) {
            generateIndex(outputFolder, classes, interfaces, enumerations);
        }

        File diagramsFolder = new File(outputFolder, DIAGRAMS_FOLDER);
        if (!diagramsFolder.exists()) {
            if (!diagramsFolder.mkdir()) {
                throw new OpenEhrExporterException("Unable to create folder: " + diagramsFolder);
            }
        }
        project.getDiagrams().stream().forEach(d -> exportDiagram(diagramsFolder, d));
    }

    private boolean matchesRootPackages(NamedElement namedElement) {
        return rootPackageNames.stream().filter(rn -> namedElement.getQualifiedName().contains(rn)).findFirst().isPresent();
    }

    private void exportDiagram(File outputFolder, DiagramPresentationElement diagramPresentationElement) {
        String name = diagramPresentationElement.getName();
        try {
            ImageExporter.export(diagramPresentationElement, 1, new File(outputFolder, formatDiagramName(name) + ".png"));
            ImageExporter.export(diagramPresentationElement, 5, new File(outputFolder, formatDiagramName(name) + ".svg"));
        } catch (IOException e) {
            throw new OpenEhrExporterException("Unable to export diagrams for " + name + '!', e);
        }
    }

    private void exportClass(ClassInfo classInfo, File targetFolder) {
        try (PrintWriter printWriter = new PrintWriter(Files.newBufferedWriter(
                targetFolder.toPath().resolve(fileName(classInfo.getClassName().toLowerCase()) + ADOC_FILE_EXTENSION), Charset.forName("UTF-8")))) {
            printWriter.println(headingPrefix + ' ' + classInfo.getClassName() + ' ' + classInfo.getType());
            printWriter.println();

            printWriter.println("[cols=\"^1,2,3\"]");
            printWriter.println("|===");
            printWriter.println("h|" + formatter.bold(classInfo.getType()));
            printWriter.println("2+^h|" +
                                        (classInfo.isAbstractClass()
                                                ? formatter.italicBold(classInfo.getClassName() + " (abstract)")
                                                : formatter.bold(classInfo.getClassName())));
            printWriter.println();

            printWriter.println("h|" + formatter.bold("Description"));

            printWriter.println("2+a|" + formatter.escapeColumnSeparator(formatter.normalizeLines(classInfo.getDocumentation())));
            printWriter.println();

            if (classInfo.getParentClassName() != null) {
                printWriter.println("h|" + formatter.bold("Inherit"));
                printWriter.println("2+|" + classInfo.getParentClassName());
                printWriter.println();
            }

            if (!classInfo.getConstants().isEmpty()) {
                printWriter.println("h|" + formatter.bold("Constants"));
                printWriter.println("^h|" + formatter.bold("Signature"));
                printWriter.println("^h|" + formatter.bold("Meaning"));

                exportConstants(classInfo, printWriter);
            }

            if (!classInfo.getAttributes().isEmpty()) {
                printWriter.println("h|" + formatter.bold("Attributes"));
                printWriter.println("^h|" + formatter.bold("Signature"));
                printWriter.println("^h|" + formatter.bold("Meaning"));

                exportAttributes(classInfo, printWriter);
            }

            if (!classInfo.getFunctions().isEmpty()) {
                printWriter.println("h|" + formatter.bold("Functions"));
                printWriter.println("^h|" + formatter.bold("Signature"));
                printWriter.println("^h|" + formatter.bold("Meaning"));
            }

            if (!classInfo.getFunctions().isEmpty()) {
                exportFunctions(classInfo, printWriter);
            }

            if (!classInfo.getConstraints().isEmpty()) {
                exportConstraints(classInfo, printWriter);
            }

            printWriter.println("|===");
        } catch (IOException e) {
            throw new OpenEhrExporterException(e);
        }
    }

    private void exportConstraints(ClassInfo classInfo, PrintWriter printWriter) {
        String title = formatter.bold("Invariant");
        for (ConstraintInfo constraintInfo : classInfo.getConstraints()) {
            printWriter.println();
            printWriter.println("h|" + formatter.escapeColumnSeparator(title));

            printWriter.println("2+a|" + formatter.escapeColumnSeparator(formatter.normalizeLines(constraintInfo.getDocumentation())));
            title = "";
        }
    }

    private void generateIndex(File targetFolder, List<ClassInfo> classes, List<ClassInfo> interfaces, List<ClassInfo> enumerations) {
        List<ClassInfo> allTypes = new ArrayList<>(classes.size() + interfaces.size() + enumerations.size());
        allTypes.addAll(classes);
        allTypes.addAll(interfaces);
        allTypes.addAll(enumerations);

        Collections.sort(allTypes);

        Path targetPath = targetFolder.toPath().resolve("class_index" + ADOC_FILE_EXTENSION);
        try (PrintWriter printWriter = new PrintWriter(Files.newBufferedWriter(targetPath, Charset.forName("UTF-8")))) {
            String indexComponent = "";
            String indexPackage = "";
            String indexSubPackage = "";

            for (ClassInfo classInfo : allTypes) {
                if (!"BASE".equals(classInfo.getIndexComponent()) && !"T".equals(classInfo.getClassName())) {
                    if (!indexComponent.equals(classInfo.getIndexComponent())) {
                        printWriter.println();
                        printWriter.println("== Component " + classInfo.getIndexComponent());
                        indexComponent = classInfo.getIndexComponent();
                    }
                    if (!indexPackage.equals(classInfo.getIndexPackage())) {
                        printWriter.println();
                        printWriter.println("=== Model " + classInfo.getIndexPackage());
                        indexPackage = classInfo.getIndexPackage();
                    }
                    if (!indexSubPackage.equals(classInfo.getIndexSubPackage())) {
                        printWriter.println();
                        printWriter.println("==== Package " + classInfo.getIndexSubPackage());
                        printWriter.println();
                        indexSubPackage = classInfo.getIndexSubPackage();
                    }
                    printWriter.printf(INDEX_LINK_FORMAT, indexComponent, indexRelease, convertToHtmlName(indexSubPackage), // base link
                                       classInfo.getClassName().toLowerCase(), classInfo.getType().toLowerCase(), // #href
                                       classInfo.getClassName()); // [descr]
                    printWriter.println();
                }
            }
        } catch (IOException e) {
            throw new OpenEhrExporterException("Unable to write to " + targetPath + '!', e);
        }
    }

    private String convertToHtmlName(String indexSubPackage) {
        if ("composition".equals(indexSubPackage)) {
            return "ehr";
        }
        if ("base".equals(indexSubPackage)) {
            return "support";
        }
        return indexSubPackage;
    }

    private void exportFunctions(ClassInfo classInfo, PrintWriter printWriter) {
        for (ClassAttributeInfo classAttributeInfo : classInfo.getFunctions()) {
            printWriter.println();
            printWriter.println("h|" + classAttributeInfo.getOccurences());

            printWriter.println('|' + classAttributeInfo.getName());
            printWriter.println("a|" + formatter.escapeColumnSeparator(formatter.normalizeLines(classAttributeInfo.getDocumentation())));
        }
    }

    private void exportAttributes(ClassInfo classInfo, PrintWriter printWriter) {
        for (ClassAttributeInfo classAttributeInfo : classInfo.getAttributes()) {
            exportAttribute(printWriter, classAttributeInfo);
        }
    }

    private void exportConstants(ClassInfo classInfo, PrintWriter printWriter) {
        for (ClassAttributeInfo classAttributeInfo : classInfo.getConstants()) {
            exportAttribute(printWriter, classAttributeInfo);
        }
    }

    private void exportAttribute(PrintWriter printWriter, ClassAttributeInfo classAttributeInfo) {
        printWriter.println();
        printWriter.println("h|" + formatter.bold(classAttributeInfo.getOccurences()));
        printWriter.println('|' + classAttributeInfo.getName());
        printWriter.println("a|" + formatter.escapeColumnSeparator(formatter.normalizeLines(classAttributeInfo.getDocumentation())));
    }

    private String fileName(String className) {
        String name = className.replaceAll("[^a-z0-9]", "_");
        return name.replaceAll("^_+", "");
    }

    private static String formatDiagramName(String name) {
        return name;
    }
}