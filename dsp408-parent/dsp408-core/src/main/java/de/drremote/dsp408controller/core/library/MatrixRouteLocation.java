package de.drremote.dsp408controller.core.library;

import de.drremote.dsp408controller.core.protocol.DspChannel;

public record MatrixRouteLocation(
        DspChannel output,
        int blockIndex,
        int dataOffset
) {
}

