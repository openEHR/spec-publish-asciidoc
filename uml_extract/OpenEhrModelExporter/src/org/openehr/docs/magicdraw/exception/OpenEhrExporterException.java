package org.openehr.docs.magicdraw.exception;

/**
 * @author Bostjan Lah
 */
public class OpenEhrExporterException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public OpenEhrExporterException(String message) {
        super(message);
    }

    public OpenEhrExporterException(Throwable cause) {
        super(cause);
    }

    public OpenEhrExporterException(String message, Throwable cause) {
        super(message, cause);
    }
}
