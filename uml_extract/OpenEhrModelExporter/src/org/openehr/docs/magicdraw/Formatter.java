package org.openehr.docs.magicdraw;

/**
 * @author Bostjan Lah
 */
public interface Formatter {
    String bold(String text);

    String monospace(String text);

    String italicBold(String text);

    String newParagraph();

    String escapeLiteral(String value);

    String escapeColumnSeparator(String value);

    String normalizeLines(String doc);
}
