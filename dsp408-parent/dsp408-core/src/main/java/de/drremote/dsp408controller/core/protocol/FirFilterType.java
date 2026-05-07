package de.drremote.dsp408controller.core.protocol;

import java.util.Locale;

public enum FirFilterType {
    BYPASS(0x00),
    LOW_PASS(0x01),
    HIGH_PASS(0x02),
    BAND_PASS(0x03),
    EXTERNAL_FIR(0x04);

    private final int rawValue;

    FirFilterType(int rawValue) {
        this.rawValue = rawValue;
    }

    public int rawValue() {
        return rawValue;
    }

    public static FirFilterType fromRaw(int rawValue) {
        for (FirFilterType type : values()) {
            if (type.rawValue == rawValue) {
                return type;
            }
        }
        throw new IllegalArgumentException("FIR filter type raw must be in range 0..4");
    }

    public static FirFilterType parse(String value) {
        Integer raw = parseInteger(value);
        if (raw != null) {
            return fromRaw(raw);
        }

        String normalized = normalize(value);
        return switch (normalized) {
            case "bypass", "off" -> BYPASS;
            case "lowpass", "lp" -> LOW_PASS;
            case "highpass", "hp" -> HIGH_PASS;
            case "bandpass", "bp" -> BAND_PASS;
            case "external", "externalfir", "file" -> EXTERNAL_FIR;
            default -> throw new IllegalArgumentException("FIR type must be bypass|lowpass|highpass|bandpass|external");
        };
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("FIR type is required");
        }
        return value.trim()
                .toLowerCase(Locale.ROOT)
                .replace("_", "")
                .replace("-", "")
                .replace(" ", "");
    }

    private static Integer parseInteger(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        try {
            if (trimmed.matches("(?i)0x[0-9a-f]+")) {
                return Integer.parseInt(trimmed.substring(2), 16);
            }
            if (trimmed.matches("\\d+")) {
                return Integer.parseInt(trimmed);
            }
        } catch (NumberFormatException ignored) {
            return null;
        }
        return null;
    }
}
