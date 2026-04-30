package de.drremote.dsp408controller.core.library;

import de.drremote.dsp408controller.core.protocol.PeqFilterType;

public record OutputPeqBandDefaults(
        int bandIndex,
        double frequencyHz,
        double q,
        double gainDb,
        PeqFilterType filterType
) {
}
