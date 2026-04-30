package de.drremote.dsp408controller.core.library;

import de.drremote.dsp408controller.core.protocol.DspChannel;

public record InputGateLocation(
        DspChannel channel,
        PeqFieldLocation attack,
        PeqFieldLocation release,
        PeqFieldLocation hold,
        PeqFieldLocation threshold
) {
}
