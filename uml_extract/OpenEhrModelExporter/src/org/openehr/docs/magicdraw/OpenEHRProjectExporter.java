package org.openehr.docs.magicdraw;

import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.export.image.ImageExporter;
import com.nomagic.magicdraw.uml.symbols.DiagramPresentationElement;
import com.nomagic.uml2.ext.jmi.helpers.ModelHelper;
import com.nomagic.uml2.ext.magicdraw.classes.mdinterfaces.Interface;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Enumeration;
import org.openehr.docs.magicdraw.exception.OpenEhrExporterException;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Collection;

/**
 * Action which displays its name.
 *
 * @author Bostjan Lah
 */
public class OpenEHRProjectExporter {
    private static final String ADOC_FILE_EXTENSION = ".adoc";
    private static final String OPENEHR_PACKAGE_NAME = "openehr";
    
    private static final String DIAGRAMS_FOLDER = "diagrams";
    private static final String CLASSES_FOLDER = "classes";

    private final Formatter formatter = new AsciidocFormatter();
    private final String headingPrefix;

    public OpenEHRProjectExporter(int headingLevel) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < headingLevel; i++) {
            builder.append('=');
        }
        headingPrefix = builder.toString();
    }

    public void exportProject(File outputFolder, Project project) throws Exception {
        File classesFolder = new File(outputFolder, CLASSES_FOLDER);
        if (!classesFolder.exists()) {
            if (!classesFolder.mkdir()) {
                throw new OpenEhrExporterException("Unable to create folder: " + classesFolder);
            }
        }

        Collection<? extends Element> umlClasses = ModelHelper.getElementsOfType(project.getModel(),
                                                                                 new Class[]{com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class.class},
                                                                                 true);
        ClassInfoBuilder classInfoBuilder = new ClassInfoBuilder(formatter);
        umlClasses.stream()
                .map(e -> (com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class)e)
                .filter(cl -> cl.getQualifiedName().contains(OPENEHR_PACKAGE_NAME))
                .forEach(cl -> exportClass(classInfoBuilder.build(cl), classesFolder));

        Collection<? extends Element> umlInterfaces = ModelHelper.getElementsOfType(project.getModel(),
                                                                                 new Class[]{Interface.class},
                                                                                 true);
        InterfaceInfoBuilder interfaceInfoBuilder = new InterfaceInfoBuilder(formatter);
        umlInterfaces.stream()
                .map(e -> (Interface)e)
                .filter(cl -> cl.getQualifiedName().contains(OPENEHR_PACKAGE_NAME))
                .forEach(cl -> exportClass(interfaceInfoBuilder.build(cl), classesFolder));

        Collection<? extends Element> umlEnumerations = ModelHelper.getElementsOfType(project.getModel(),
                                                                                      new Class[]{Enumeration.class},
                                                                                      true);

        EnumerationInfoBuilder enumerationInfoBuilder = new EnumerationInfoBuilder(formatter);
        umlEnumerations.stream()
                .map(e -> (Enumeration)e)
                .filter(en -> en.getQualifiedName().contains(OPENEHR_PACKAGE_NAME))
                .forEach(en -> exportClass(enumerationInfoBuilder.build(en), classesFolder));

        File diagramsFolder = new File(outputFolder, DIAGRAMS_FOLDER);
        if (!diagramsFolder.exists()) {
            if (!diagramsFolder.mkdir()) {
                throw new OpenEhrExporterException("Unable to create folder: " + diagramsFolder);
            }
        }
        project.getDiagrams().stream().forEach(d -> exportDiagram(diagramsFolder, d));
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
//            printWriter.println(formatter.getClassBackgroundColour());
            printWriter.println("2+^h|" +
                                        (classInfo.isAbstractClass()
                                                ? formatter.italicBold(classInfo.getClassName() + " (abstract)")
                                                : formatter.bold(classInfo.getClassName())));
            printWriter.println();

            printWriter.println("h|" + formatter.bold("Description"));
//            printWriter.println(formatter.getClassBackgroundColour());

            printWriter.println("2+a|" + formatter.escapeColumnSeparator(formatter.normalizeLines(classInfo.getDocumentation())));
//            printWriter.println(formatter.resetColour());
            printWriter.println();

            if (classInfo.getParentClassName() != null) {
                printWriter.println("h|" + formatter.bold("Inherit"));
//                printWriter.println(formatter.getClassBackgroundColour());
                printWriter.println("2+|" + classInfo.getParentClassName());
//                printWriter.println(formatter.resetColour());
                printWriter.println();
            }

            if (!classInfo.getAttributes().isEmpty()) {
                printWriter.println("h|" + formatter.bold("Attributes"));
//                printWriter.println(formatter.getClassBackgroundColour());
                printWriter.println("^h|" + formatter.bold("Signature"));
                printWriter.println("^h|" + formatter.bold("Meaning"));

                exportAttributes(classInfo, printWriter);
            }

            if (!classInfo.getFunctions().isEmpty()) {
                printWriter.println("h|" + formatter.bold("Functions"));
//                printWriter.println(formatter.getClassBackgroundColour());
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
//            printWriter.println(formatter.getClassBackgroundColour());

            printWriter.println("2+a|" + formatter.escapeColumnSeparator(formatter.normalizeLines(constraintInfo.getDocumentation())));
//            printWriter.println(formatter.resetColour());
            title = "";
        }
    }

    private void exportFunctions(ClassInfo classInfo, PrintWriter printWriter) {
        for (ClassAttributeInfo classAttributeInfo : classInfo.getFunctions()) {
            printWriter.println();
            printWriter.println("h|" + classAttributeInfo.getOccurences());
//            printWriter.println(formatter.getClassBackgroundColour());

            printWriter.println('|' + classAttributeInfo.getName());
//            printWriter.println(formatter.resetColour());
            printWriter.println("a|" + formatter.escapeColumnSeparator(formatter.normalizeLines(classAttributeInfo.getDocumentation())));
        }
    }

    private void exportAttributes(ClassInfo classInfo, PrintWriter printWriter) {
        for (ClassAttributeInfo classAttributeInfo : classInfo.getAttributes()) {
            printWriter.println();
            printWriter.println("h|" + formatter.bold(classAttributeInfo.getOccurences()));
//            printWriter.println(formatter.getClassBackgroundColour());

            printWriter.println('|' + classAttributeInfo.getName());
//            printWriter.println(formatter.resetColour());

            printWriter.println("a|" + formatter.escapeColumnSeparator(formatter.normalizeLines(classAttributeInfo.getDocumentation())));
        }
    }

    private String fileName(String className) {
        String name = className.replaceAll("[^a-z0-9]", "_");
        return name.replaceAll("^_+", "");
    }

    private static String formatDiagramName(String name) {
        return name;
    }
}