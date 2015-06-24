package org.openehr.docs.magicdraw;

import com.nomagic.magicdraw.actions.MDAction;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.export.image.ImageExporter;
import com.nomagic.magicdraw.ui.dialogs.MDDialogParentProvider;
import com.nomagic.magicdraw.uml.symbols.DiagramPresentationElement;
import com.nomagic.uml2.ext.jmi.helpers.ModelHelper;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Enumeration;
import org.openehr.docs.magicdraw.exception.OpenEhrExporterException;

import javax.annotation.CheckForNull;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import java.awt.event.ActionEvent;
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
class OpenEHRExportAction extends MDAction {
    private static final long serialVersionUID = 1L;

    private static final String ADOC_FILE_EXTENSION = ".adoc";
    private static final String OPENEHR_PACKAGE_NAME = "openehr";

    private final Formatter formatter = new AsciidocFormatter();

    OpenEHRExportAction(@CheckForNull String id, String name) {
        super(id, name, null, null);
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
     */
    @SuppressWarnings("OverlyBroadCatchBlock")
    @Override
    public void actionPerformed(ActionEvent e) {
        File outputFolder = chooseFolder();
        if (outputFolder != null) {
            try {
                exportProject(outputFolder, Application.getInstance().getProject());

                JOptionPane.showMessageDialog(MDDialogParentProvider.getProvider().getDialogParent(), "Export complete.", "Export",
                                              JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(MDDialogParentProvider.getProvider().getDialogParent(), "Unable to export data: " + ex.getMessage());
            }
        }
    }

    private void exportProject(File outputFolder, Project project) throws Exception {
        File classesFolder = new File(outputFolder, "classes");
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

        Collection<? extends Element> umlEnumerations = ModelHelper.getElementsOfType(project.getModel(),
                                                                                      new Class[]{Enumeration.class},
                                                                                      true);

        EnumerationInfoBuilder enumerationInfoBuilder = new EnumerationInfoBuilder(formatter);
        umlEnumerations.stream()
                .map(e -> (Enumeration)e)
                .filter(en -> en.getQualifiedName().contains(OPENEHR_PACKAGE_NAME))
                .forEach(en -> exportClass(enumerationInfoBuilder.build(en), classesFolder));

        File diagramsFolder = new File(outputFolder, "uml_diagrams");
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

    private File chooseFolder() {
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(null);
        chooser.setDialogTitle("Select Export Folder");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);

        File chosen;
        if (chooser.showDialog(MDDialogParentProvider.getProvider().getDialogParent(), "OK") == JFileChooser.APPROVE_OPTION) {
            File selectedFile = chooser.getSelectedFile();
            chosen = !Files.exists(selectedFile.toPath()) && Files.exists(selectedFile.toPath().getParent())
                    ? selectedFile.toPath().getParent().toFile()
                    : selectedFile.toPath().toFile();
        } else {
            chosen = null;
        }

        return chosen;
    }

    private void exportClass(ClassInfo classInfo, File targetFolder) {
        try (PrintWriter printWriter = new PrintWriter(Files.newBufferedWriter(
                targetFolder.toPath().resolve(fileName(classInfo.getClassName().toLowerCase()) + ADOC_FILE_EXTENSION), Charset.forName("UTF-8")))) {
            printWriter.println("==== " + classInfo.getClassName() + ' ' + classInfo.getType());
            printWriter.println();

            printWriter.println("[cols=\"^1,2,3\"]");
            printWriter.println("|===");
            printWriter.println('|' + formatter.bold(classInfo.getType()));
            printWriter.println(formatter.getClassBackgroundColour());
            printWriter.println("2+^|" +
                                        (classInfo.isAbstractClass()
                                                ? formatter.italicBold(classInfo.getClassName() + " (abstract)")
                                                : formatter.bold(classInfo.getClassName())));
            printWriter.println();

            printWriter.println('|' + formatter.bold("Description"));
            printWriter.println(formatter.getClassBackgroundColour());

            printWriter.println("2+|" + classInfo.getDocumentation());
            printWriter.println(formatter.resetColour());
            printWriter.println();

            if (classInfo.getParentClassName() != null) {
                printWriter.println('|' + formatter.bold("Inherit"));
                printWriter.println(formatter.getClassBackgroundColour());
                printWriter.println("2+|" + classInfo.getParentClassName());
                printWriter.println(formatter.resetColour());
                printWriter.println();
            }

            if (!classInfo.getAttributes().isEmpty()) {
                printWriter.println('|' + formatter.bold("Attributes"));
                printWriter.println(formatter.getClassBackgroundColour());
                printWriter.println("^|" + formatter.bold("Signature"));
                printWriter.println("^|" + formatter.bold("Meaning"));

                exportAttributes(classInfo, printWriter);
            }

            if (!classInfo.getFunctions().isEmpty()) {
                printWriter.println('|' + formatter.bold("Functions"));
                printWriter.println(formatter.getClassBackgroundColour());
                printWriter.println("^|" + formatter.bold("Signature"));
                printWriter.println("^|" + formatter.bold("Meaning"));
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
            printWriter.println('|' + title);
            printWriter.println(formatter.getClassBackgroundColour());

            printWriter.println("2+|" + constraintInfo.getDocumentation());
            printWriter.println(formatter.resetColour());
            title = "";
        }
    }

    private void exportFunctions(ClassInfo classInfo, PrintWriter printWriter) {
        for (ClassAttributeInfo classAttributeInfo : classInfo.getFunctions()) {
            printWriter.println();
            printWriter.println('|' + classAttributeInfo.getOccurences());
            printWriter.println(formatter.getClassBackgroundColour());

            printWriter.println('|' + classAttributeInfo.getName());
            printWriter.println(formatter.resetColour());
            printWriter.println('|' + classAttributeInfo.getDocumentation());
        }
    }

    private void exportAttributes(ClassInfo classInfo, PrintWriter printWriter) {
        for (ClassAttributeInfo classAttributeInfo : classInfo.getAttributes()) {
            printWriter.println();
            printWriter.println('|' + formatter.bold(classAttributeInfo.getOccurences()));
            printWriter.println(formatter.getClassBackgroundColour());

            printWriter.println('|' + classAttributeInfo.getName());
            printWriter.println(formatter.resetColour());
            printWriter.println('|' + classAttributeInfo.getDocumentation());
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