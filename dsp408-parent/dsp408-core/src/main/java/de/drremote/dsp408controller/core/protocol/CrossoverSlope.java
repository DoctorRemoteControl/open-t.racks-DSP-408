package de.drremote.dsp408controller.core.protocol;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public enum CrossoverSlope {
    BW_6(0x01, "BW -6"),
    BS_6(0x02, "BS -6"),
    BW_12(0x03, "BW -12"),
    BS_12(0x04, "BS -12"),
    LR_12(0x05, "LR -12"),
    BW_18(0x06, "BW -18"),
    BS_18(0x07, "BS -18"),
    BW_24(0x08, "BW -24"),
    BS_24(0x09, "BS -24"),
    LR_24(0x0A, "LR -24"),
    BW_30(0x0B, "BW -30"),
    BS_30(0x0C, "BS -30"),
    BW_36(0x0D, "BW -36"),
    BS_36(0x0E, "BS -36"),
    LR_36(0x0F, "LR -36"),
    BW_42(0x10, "BW -42"),
    BS_42(0x11, "BS -42"),
    BW_48(0x12, "BW -48"),
    BS_48(0x13, "BS -48"),
    LR_48(0x14, "LR -48");

    private static final Map<String, CrossoverSlope> ALIASES = buildAliases();

    private final int rawValue;
    private final String displayName;

    CrossoverSlope(int rawValue, String displayName) {
        this.rawValue = rawValue;
        this.displayName = displayName;
    }

    public int rawValue() {
        return rawValue;
    }

    public String displayName() {
        return displayName;
    }

    public static CrossoverSlope parse(String value) {
        CrossoverSlope slope = ALIASES.get(normalize(value));
        if (slope == null) {
            throw new IllegalArgumentException("Unknown crossover slope: " + value);
        }
        return slope;
    }

    public static CrossoverSlope fromRaw(int rawValue) {
        for (CrossoverSlope slope : values()) {
            if (slope.rawValue == rawValue) {
                return slope;
            }
        }
        throw new IllegalArgumentException("Unsupported crossover slope raw value: " + rawValue);
    }

    private static Map<String, CrossoverSlope> buildAliases() {
        Map<String, CrossoverSlope> map = new HashMap<>();
        for (CrossoverSlope slope : values()) {
            map.put(normalize(slope.displayName), slope);
            map.put(normalize(slope.name()), slope);
        }
        return Map.copyOf(map);
    }

    private static String normalize(String value) {
        return value.trim()
                .toLowerCase(Locale.ROOT)
                .replace("_", "")
                .replace("-", "")
                .replace(" ", "");
    }
}
