package de.drremote.dsp408controller.core.service;

public final class UnsupportedDspOperationException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public UnsupportedDspOperationException(String message) {
        super(message);
    }
}