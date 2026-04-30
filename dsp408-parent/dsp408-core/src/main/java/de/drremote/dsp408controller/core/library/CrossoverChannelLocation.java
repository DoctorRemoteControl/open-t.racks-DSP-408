package de.drremote.dsp408controller.core.library;

import de.drremote.dsp408controller.core.protocol.DspChannel;

public record CrossoverChannelLocation(
        DspChannel channel,
        PeqFieldLocation highPassFrequency,
        PeqFieldLocation highPassMode,
        PeqFieldLocation highPassBypass,
        PeqFieldLocation lowPassFrequency,
        PeqFieldLocation lowPassMode,
        PeqFieldLocation lowPassBypass
) {
}
