package de.drremote.dsp408controller.core.protocol;

import java.util.Locale;

public enum FirWindowFunction {
    RECT(0x00),
    WINDOW_1(0x01),
    WINDOW_2(0x02),
    HAMMING(0x03),
    BLACKMAN(0x04),
    SINE(0x05),
    SINC(0x06),
    NUTTALL(0x07),
    KAISER(0x08),
    BLACKMAN_NUTTALL(0x09),
    BLACKMAN_HARRIS(0x0A);

    private final int rawValue;

    FirWindowFunction(int rawValue) {
        this.rawValue = rawValue;
    }

    public int rawValue() {
        return rawValue;
    }

    public static FirWindowFunction fromRaw(int rawValue) {
        for (FirWindowFunction window : values()) {
            if (window.rawValue == rawValue) {
                return window;
            }
        }
        throw new IllegalArgumentException("FIR window raw must be in range 0..10");
    }

    public static FirWindowFunction parse(String value) {
        Integer raw = parseInteger(value);
        if (raw != null) {
            return fromRaw(raw);
        }

        String normalized = normalize(value);
        return switch (normalized) {
            case "rect", "rectangle" -> RECT;
            case "window1", "unknown1" -> WINDOW_1;
            case "window2", "unknown2" -> WINDOW_2;
            case "hamming" -> HAMMING;
            case "blackman" -> BLACKMAN;
            case "sine" -> SINE;
            case "sinc" -> SINC;
            case "nuttall" -> NUTTALL;
            case "kaiser" -> KAISER;
            case "blackmannuttall" -> BLACKMAN_NUTTALL;
            case "blackmanharris", "harris" -> BLACKMAN_HARRIS;
            default -> throw new IllegalArgumentException(
                    "FIR window must be rect|window1|window2|hamming|blackman|sine|sinc|nuttall|kaiser|blackman_nuttall|blackman_harris"
            );
        };
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("FIR window is required");
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
