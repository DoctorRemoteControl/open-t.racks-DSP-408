package de.drremote.dsp408controller.core.protocol;

import java.util.Locale;

public enum FirProcessingMode {
    IIR(0x00),
    FIR(0x01);

    private final int rawValue;

    FirProcessingMode(int rawValue) {
        this.rawValue = rawValue;
    }

    public int rawValue() {
        return rawValue;
    }

    public static FirProcessingMode fromRaw(int rawValue) {
        return switch (rawValue) {
            case 0x00 -> IIR;
            case 0x01 -> FIR;
            default -> throw new IllegalArgumentException("FIR processing mode raw must be 0 or 1");
        };
    }

    public static FirProcessingMode parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("FIR processing mode is required");
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "0", "0x00", "iir" -> IIR;
            case "1", "0x01", "fir" -> FIR;
            default -> throw new IllegalArgumentException("FIR processing mode must be iir|fir");
        };
    }
}
