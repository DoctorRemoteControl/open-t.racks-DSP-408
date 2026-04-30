package de.drremote.dsp408controller.core.library;

import de.drremote.dsp408controller.core.protocol.DspChannel;

public record MatrixCrosspointLocation(
        DspChannel output,
        DspChannel input,
        int blockIndex,
        int dataOffset
) {
}

