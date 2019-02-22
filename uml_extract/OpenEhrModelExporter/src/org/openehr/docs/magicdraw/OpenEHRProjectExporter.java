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
import java.util.*;
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
    private static final String INDEX_LINK_FORMAT = "[.xcode]\n* link:/releases/%s/%s/%s.html#_%s_%s[%s^]\n";

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

    /**
     * Export a UML project as a set of files.
     * @param outputFolder Directory in which to write the files.
     * @param project MD descriptor for a project.
     * @exception IOException on fail to write to file.
     */
    public void exportProject(File outputFolder, Project project) throws Exception {
        File classesFolder = new File(outputFolder, CLASSES_FOLDER);
        if (!classesFolder.exists()) {
            if (!classesFolder.mkdir()) {
                throw new OpenEhrExporterException("Unable to create folder: " + classesFolder);
            }
        }

        // Gather UML classes, enumerations and interfaces, run through a pipeline that does:
        // * cast to an MD class object
        // * retain only classes within the root package(s) specified on the command line
        // * convert to ClassInfo objects (local representation used here)
        // Then export each ClassInfo object as an output file
        ClassInfoBuilder classInfoBuilder = new ClassInfoBuilder(formatter);
        Collection<? extends Element> umlClasses = ModelHelper.getElementsOfType(project.getPrimaryModel(),
                new Class[]{com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class.class},
                true);
        List<ClassInfo> classes = umlClasses.stream()
                .map(e -> (com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class)e)
                .filter(c -> ! c.getName().contains("<"))// ignore classes with names simulating template type names
                .filter(this::matchesRootPackages)
                .map(classInfoBuilder::build)
                .collect(Collectors.toList());
        classes.forEach(cl -> exportClass(cl, classesFolder));

        Collection<? extends Element> umlInterfaces = ModelHelper.getElementsOfType(project.getPrimaryModel(),
                new Class[]{Interface.class},
                true);
        InterfaceInfoBuilder interfaceInfoBuilder = new InterfaceInfoBuilder(formatter);
        List<ClassInfo> interfaces = umlInterfaces.stream()
                .map(e -> (Interface)e)
                .filter(c -> ! c.getName().contains("<"))// ignore classes with names simulating template type names
                .filter(this::matchesRootPackages)
                .map(interfaceInfoBuilder::build)
                .collect(Collectors.toList());
        interfaces.forEach(cl -> exportClass(cl, classesFolder));

        Collection<? extends Element> umlEnumerations = ModelHelper.getElementsOfType(project.getPrimaryModel(),
                new Class[]{Enumeration.class},
                true);

        EnumerationInfoBuilder enumerationInfoBuilder = new EnumerationInfoBuilder(formatter);
        List<ClassInfo> enumerations = umlEnumerations.stream()
                .map(e -> (Enumeration)e)
                .filter(this::matchesRootPackages)
                .map(enumerationInfoBuilder::build)
                .collect(Collectors.toList());
        enumerations.forEach(en -> exportClass(en, classesFolder));

     //   Collection<? extends Element> umlStateMachines = ModelHelper.getElementsOfType(project.getPrimaryModel(),
     //           new Class[]{StateMachine.class},
     //           true);

        // Generate the index file
        if (indexRelease != null) {
            generateIndex(outputFolder, classes, interfaces, enumerations);
        }

        // obtain and generate the diagrams
        File diagramsFolder = new File(outputFolder, DIAGRAMS_FOLDER);
        if (!diagramsFolder.exists()) {
            if (!diagramsFolder.mkdir()) {
                throw new OpenEhrExporterException("Unable to create folder: " + diagramsFolder);
            }
        }

        List<DiagramPresentationElement> diagrams = project.getDiagrams().stream()
                .filter(this::diagMatchesRootPackages)
                .collect(Collectors.toList());
        diagrams.forEach(d -> exportDiagram(diagramsFolder, d));
    }

    private boolean matchesRootPackages(NamedElement namedElement) {
        return rootPackageNames.stream().anyMatch (rn -> namedElement.getQualifiedName().contains(rn + "::"));
     //   return rootPackageNames.stream().filter(rn -> namedElement.getQualifiedName().contains(rn)).findFirst().isPresent();
    }

    private boolean diagMatchesRootPackages(DiagramPresentationElement diagElement) {
        return rootPackageNames.stream().anyMatch (rn -> diagElement.getName().contains(rn + "-"));
    }

    /**
     * Export a UML diagram in PNG and SVG format to the export folder.
     * @param outputFolder target folder on file system.
     * @param diagramPresentationElement UML diagram representation.
     */
    private void exportDiagram(File outputFolder, DiagramPresentationElement diagramPresentationElement) {
        String name = diagramPresentationElement.getName();
        try {
            ImageExporter.export(diagramPresentationElement, 1, new File(outputFolder, formatDiagramName(name) + ".png"));
            ImageExporter.export(diagramPresentationElement, 5, new File(outputFolder, formatDiagramName(name) + ".svg"));
        } catch (IOException e) {
            throw new OpenEhrExporterException("Unable to export diagrams for " + name + '!', e);
        }
    }

    /**
     * Export a class as an Asciidoctor (.adoc) file to the output folder on the file system.
     * @param targetFolder Directory in which to write the file.
     * @param classInfo info object for the class.
     * @exception IOException on fail to write to file.
     */
    private void exportClass(ClassInfo classInfo, File targetFolder) {
        try (PrintWriter printWriter = new PrintWriter(Files.newBufferedWriter(
                targetFolder.toPath().resolve(fileName(classInfo.getClassName().toLowerCase()) + ADOC_FILE_EXTENSION), Charset.forName("UTF-8")))) {
            printWriter.println(headingPrefix + ' ' + classInfo.getClassTypeName() + ' ' + classInfo.getMetaType());
            printWriter.println();

            printWriter.println("[cols=\"^1,3,5\"]");
            printWriter.println("|===");
            printWriter.println("h|" + formatter.bold(classInfo.getMetaType()));
            printWriter.println("2+^h|" +
                                        (classInfo.isAbstractClass()
                                                ? formatter.italicBold(classInfo.getClassTypeName() + " (abstract)")
                                                : formatter.bold(classInfo.getClassTypeName())));
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

            if (!classInfo.getOperations().isEmpty()) {
                printWriter.println("h|" + formatter.bold("Functions"));
                printWriter.println("^h|" + formatter.bold("Signature"));
                printWriter.println("^h|" + formatter.bold("Meaning"));

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

    /**
     * Export all constraints in a class as text (invariants) in an Asciidoctor (.adoc) file.
     * @param classInfo info object for the class.
     * @param printWriter File outputter.
     */
    private void exportConstraints(ClassInfo classInfo, PrintWriter printWriter) {
        String title = formatter.bold("Invariants");
        for (ConstraintInfo constraintInfo : classInfo.getConstraints()) {
            printWriter.println();
            printWriter.println("h|" + formatter.escapeColumnSeparator(title));

            printWriter.println("2+a|" + formatter.escapeColumnSeparator(formatter.normalizeLines(constraintInfo.getDocumentation())));
            title = "";
        }
    }
    /**
     * Generate an HTML file containing a clickable index of Class names that contain links to the location of
     * the class within the relevant specification.
     * @param targetFolder Directory in which to write the file.
     * @param classes classes to include in index.
     * @param interfaces interfaces to include in index.
     * @param enumerations enumerations to include in index.
     * @exception IOException on fail to write to file.
     */
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
                // The test for className > 2 is to avoid generic parameters like 'T', and
                // occasionally 'TT' or similar.
                if (classInfo.getClassName().length() > 2) {

                    // if Component of class has changed since last iteration, output a new header line
                    if (!indexComponent.equals(classInfo.getIndexComponent())) {
                        printWriter.println();
                        printWriter.println("== Component " + classInfo.getIndexComponent());
                        indexComponent = classInfo.getIndexComponent();
                    }

                    // if Package of class has changed since last iteration, output a new header line
                    if (!indexPackage.equals(classInfo.getIndexPackage())) {
                        printWriter.println();
                        printWriter.println("=== Model " + classInfo.getIndexPackage());
                        indexPackage = classInfo.getIndexPackage();
                    }

                    // if Sub-package of class has changed since last iteration, output a new header line
                    if (!indexSubPackage.equals(classInfo.getIndexSubPackage())) {
                        printWriter.println();
                        printWriter.println("==== Package " + classInfo.getIndexSubPackage());
                        printWriter.println();
                        indexSubPackage = classInfo.getIndexSubPackage();
                    }

                    // Output the class as a linked text line
                    printWriter.printf(INDEX_LINK_FORMAT, indexComponent, indexRelease,
                            classSpecMap.containsKey(indexSubPackage) ? classSpecMap.get(indexSubPackage) : indexSubPackage, // base link
                            classInfo.getClassName().toLowerCase(), classInfo.getMetaType().toLowerCase(), // #href
                            classInfo.getClassName()); // [descr]
                }
            }
        } catch (IOException e) {
            throw new OpenEhrExporterException("Unable to write to " + targetPath + '!', e);
        }
    }

    /*
     * Handle exceptions to regular relationship between package name and
     * specification document name.
     */
    static Hashtable<String, String> classSpecMap = new Hashtable<String, String>();

    static {
        classSpecMap.put("composition", "ehr");
        classSpecMap.put("aom2", "AOM2");
        classSpecMap.put("aom2_profile", "AOM2");
        classSpecMap.put("p_aom2", "AOM2");
    }

    /**
     * Export all methods in a class as text in an Asciidoctor (.adoc) file.
     * @param classInfo info object for the class.
     * @param printWriter File outputter.
     */
    private void exportFunctions(ClassInfo classInfo, PrintWriter printWriter) {
        for (ClassFeatureInfo classFeatureInfo : classInfo.getOperations()) {
            printWriter.println();
            printWriter.println("h|" + classFeatureInfo.getStatus());

            printWriter.println('|' + classFeatureInfo.getSignature());
            printWriter.println("a|" + formatter.escapeColumnSeparator(formatter.normalizeLines(classFeatureInfo.getDocumentation())));
        }
    }

    /**
     * Export all attributes in a class as text in an Asciidoctor (.adoc) file.
     * @param classInfo info object for the class.
     * @param printWriter File outputter.
     */
    private void exportAttributes(ClassInfo classInfo, PrintWriter printWriter) {
        for (ClassFeatureInfo classFeatureInfo : classInfo.getAttributes()) {
            exportAttribute(printWriter, classFeatureInfo);
        }
    }

    /**
     * Export all constants in a class as text in an Asciidoctor (.adoc) file.
     * @param classInfo info object for the class.
     * @param printWriter File outputter.
     */
    private void exportConstants(ClassInfo classInfo, PrintWriter printWriter) {
        for (ClassFeatureInfo classFeatureInfo : classInfo.getConstants()) {
            exportAttribute(printWriter, classFeatureInfo);
        }
    }

    /**
     * Export a single attribute in a class as text in an Asciidoctor (.adoc) file.
     * @param classFeatureInfo info object for the attribute.
     * @param printWriter File outputter.
     */
    private void exportAttribute(PrintWriter printWriter, ClassFeatureInfo classFeatureInfo) {
        printWriter.println();
        printWriter.println("h|" + formatter.bold(classFeatureInfo.getStatus()));
        printWriter.println('|' + classFeatureInfo.getSignature());
        printWriter.println("a|" + formatter.escapeColumnSeparator(formatter.normalizeLines(classFeatureInfo.getDocumentation())));
    }

    /**
     * Convert a class name to a legal file name.
     * @param className name of class.
     * @return filename..
     */
    private String fileName(String className) {
        String name = className.replaceAll("[^a-z0-9]", "_");
        return name.replaceAll("^_+", "");
    }

    private static String formatDiagramName(String name) {
        return name;
    }
}