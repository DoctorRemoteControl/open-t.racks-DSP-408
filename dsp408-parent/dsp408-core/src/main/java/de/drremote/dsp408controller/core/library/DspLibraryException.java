package de.drremote.dsp408controller.core.library;

public final class DspLibraryException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public DspLibraryException(String message) {
        super(message);
    }

    public DspLibraryException(String message, Throwable cause) {
        super(message, cause);
    }
}