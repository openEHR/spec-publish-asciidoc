package org.openehr.docs.magicdraw;

/**
 * @author Bostjan Lah
 */
public class AsciidocFormatter implements Formatter {
    @Override
    public String bold(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }
        return '*' + text + '*';
    }

    @Override
    public String monospace(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }
        return '`' + text + '`';
    }

    @Override
    public String italicBold(String text) {
        return "*_" + text + "_*";
    }

    @Override
    public String newParagraph() {
        return " +" + System.lineSeparator();
    }

    @Override
    public String escapeLiteral(String value) {
        return value.replace("|", "&#124;").replace("*", "&#42;").replace("<=", "\\<=");
    }

    @Override
    public String escape(String value) {
        return value.replace("<=", "\\<=");
    }

    @Override
    public String escapeColumnSeparator(String value) {
        return value.replace("|", "&#124;");
    }

    @Override
    public String normalizeLines(String doc) {
        StringBuilder classDoc = new StringBuilder();
        boolean inLiteralBlock = false;
        for (String line : doc.split("\n")) {
            if (line.trim().startsWith("----")) {
                inLiteralBlock = !inLiteralBlock;
            }
            classDoc.append(inLiteralBlock ? line : line.trim()).append('\n');
        }
        return classDoc.toString().trim();
    }
}
