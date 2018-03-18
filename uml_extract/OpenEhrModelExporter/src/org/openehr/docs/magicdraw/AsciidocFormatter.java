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
    public String italicMonospace(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }
        return "`_" + text + "_`";
    }

    @Override
    public String boldMonospace(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }
        return "`*" + text + "*`";
    }

    @Override
    public String italicBold(String text) {
        return "*_" + text + "_*";
    }

    @Override
    public String hardLineBreak() {
        return " +" + System.lineSeparator();
    }

    /**
     * Do any escaping needed for AsciiDoc processing within literal strings occurring in type signatures.
     * @param value documentation string.
     */
    @Override
    public String escapeLiteral(String value) {
        return value.replace("|", "&#124;").replace("*", "&#42;").replace("<=", "\\<=");
    }

    /**
     * Do any escaping needed for AsciiDoc processing.
     * @param value documentation string.
     */
    @Override
    public String escape(String value) {
        return value.replace("<=", "\\<=");
    }

    /**
     * Convert pipe characters in text to their char code equivalent, to prevent being processed as
     * an AsciiDoc table delimiter.
     * @param value documentation string.
     */
    @Override
    public String escapeColumnSeparator(String value) {
        return value.replace("|", "&#124;");
    }

    /**
     * Removing leading and trailing spaces from lines, except in literal (code etc) blocks.
     * @param doc documentation string.
     */
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

    /**
     * Generate the line of text ".Errors", which will be interpreted by Asciidoctor
     * as a special heading.
     */
    @Override
    public String errorDelimiterLine() {
        return (System.lineSeparator() + ".Errors" + System.lineSeparator());
    }

}
