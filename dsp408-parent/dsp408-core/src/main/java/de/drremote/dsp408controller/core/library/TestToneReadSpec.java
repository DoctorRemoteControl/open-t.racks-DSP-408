package de.drremote.dsp408controller.core.library;

public record TestToneReadSpec(
        int blockIndex,
        int sourceDataOffset,
        int sineFrequencyDataOffset
) {
}
