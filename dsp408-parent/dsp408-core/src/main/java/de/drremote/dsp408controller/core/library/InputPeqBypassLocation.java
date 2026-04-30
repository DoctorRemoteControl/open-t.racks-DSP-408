package de.drremote.dsp408controller.core.library;

public record InputPeqBypassLocation(
        int blockIndex,
        int dataOffset,
        int bitIndex
) {
}
