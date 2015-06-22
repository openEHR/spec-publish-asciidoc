package org.openehr.docs.magicdraw;

import com.nomagic.magicdraw.actions.MDAction;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.export.image.ImageExporter;
import com.nomagic.magicdraw.ui.dialogs.MDDialogParentProvider;
import com.nomagic.magicdraw.uml.Visitor;
import com.nomagic.magicdraw.uml.symbols.DiagramPresentationElement;
import com.nomagic.uml2.ext.jmi.reflect.VisitorContext;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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

    private void exportProject(File outputFolder, Project project) throws IOException {
        MyVisitor visitor = new MyVisitor(formatter);
        for (Element element : project.getModel().getOwnedElement()) {
            visitModel(element, visitor);
        }

        File classesFolder = new File(outputFolder, "classes");
        if (!classesFolder.exists()) {
            if (!classesFolder.mkdir()) {
                throw new OpenEhrExporterException("Unable to create folder: " + classesFolder);
            }
        }
        visitor.getClasses().stream().forEach(cl -> exportClass(cl, classesFolder));

        File diagramsFolder = new File(outputFolder, "uml_diagrams");
        if (!diagramsFolder.exists()) {
            if (!diagramsFolder.mkdir()) {
                throw new OpenEhrExporterException("Unable to create folder: " + diagramsFolder);
            }
        }
        exportDiagrams(project.getDiagrams(), diagramsFolder);
    }

    private void exportDiagrams(Collection<DiagramPresentationElement> diagrams, File outputFolder) throws IOException {
        for (DiagramPresentationElement diagramPresentationElement : diagrams) {
            ImageExporter.export(diagramPresentationElement, 1, new File(outputFolder, formatDiagramName(diagramPresentationElement.getName()) + ".png"));
            ImageExporter.export(diagramPresentationElement, 5, new File(outputFolder, formatDiagramName(diagramPresentationElement.getName()) + ".svg"));
        }
    }

    @SuppressWarnings({"OverlyBroadCatchBlock", "MethodWithMultipleLoops"})
    private void visitModel(Element parent, Visitor... visitors) {
        for (Element element : parent.getOwnedElement()) {
            try {
                for (Visitor visitor : visitors) {
                    element.accept(visitor);
                }
                if (element.hasOwnedElement()) {
                    visitModel(element, visitors);
                }
            } catch (Exception e) {
                throw new OpenEhrExporterException(e);
            }
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
            printWriter.println("==== " + classInfo.getClassName() + " Class");
            printWriter.println();

            printWriter.println("[cols=\"^1,2,3\"]");
            printWriter.println("|===");
            printWriter.println('|' + formatter.bold("Class"));
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

    private static class MyVisitor extends Visitor {
        private final List<ClassInfo> classes = new ArrayList<>();
        private final Formatter formatter;

        private MyVisitor(Formatter formatter) {
            this.formatter = formatter;
        }

        @Override
        public void visitClass(com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class element, VisitorContext context) {
            if (element.getQualifiedName().contains(OPENEHR_PACKAGE_NAME)) {
                classes.add(ClassInfoBuilder.build(element, formatter));
            }
        }

        public List<ClassInfo> getClasses() {
            return classes;
        }
    }
}