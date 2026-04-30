package de.drremote.dsp408controller.core.library;

import de.drremote.dsp408controller.core.protocol.DspChannel;

public record InputGeqBandLocation(
        DspChannel channel,
        int bandIndex,
        PeqFieldLocation gain
) {
}
