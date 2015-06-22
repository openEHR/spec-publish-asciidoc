package org.openehr.docs.magicdraw;

/**
 * @author Bostjan Lah
 */
public class AsciidocFormatter implements Formatter {
    @Override
    public String bold(String text) {
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
        return value.replace("|", "&#124;").replace("*", "&#42;");
    }

    @Override
    public String getClassBackgroundColour() {
        return "{set:cellbgcolor:lightblue}";
    }

    @Override
    public String resetColour() {
        return "{set:cellbgcolor!}";
    }
}