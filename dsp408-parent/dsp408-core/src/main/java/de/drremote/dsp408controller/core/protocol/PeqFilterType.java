package de.drremote.dsp408controller.core.protocol;

import java.util.Locale;

public enum PeqFilterType {
    PEAK(0x00, "Peak"),
    LOW_SHELF(0x01, "Low Shelf"),
    HIGH_SHELF(0x02, "High Shelf");

    private final int rawValue;
    private final String displayName;

    PeqFilterType(int rawValue, String displayName) {
        this.rawValue = rawValue;
        this.displayName = displayName;
    }

    public int rawValue() {
        return rawValue;
    }

    public String displayName() {
        return displayName;
    }

    public static PeqFilterType parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("PEQ filter type is missing");
        }

        String normalized = value
                .trim()
                .toLowerCase(Locale.ROOT)
                .replace("-", "")
                .replace("_", "")
                .replace(" ", "");

        return switch (normalized) {
            case "peak" -> PEAK;
            case "lowshelf", "lowshelve" -> LOW_SHELF;
            case "highshelf", "highshelve" -> HIGH_SHELF;
            default -> throw new IllegalArgumentException(
                    "Unsupported PEQ filter type: " + value + " (use peak|lowshelf|highshelf)"
            );
        };
    }
}
