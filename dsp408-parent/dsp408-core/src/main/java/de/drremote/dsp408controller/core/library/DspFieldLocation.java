package de.drremote.dsp408controller.core.library;

import de.drremote.dsp408controller.core.protocol.DspChannel;

public record DspFieldLocation(
        DspChannel channel,
        int blockIndex,
        int dataOffset
) {
}