package de.drremote.dsp408controller.core.library;

import de.drremote.dsp408controller.core.protocol.DspChannel;

public record MeterSlotLocation(
        int slotIndex,
        DspChannel channel,
        int payloadOffset,
        int widthBytes
) {
}