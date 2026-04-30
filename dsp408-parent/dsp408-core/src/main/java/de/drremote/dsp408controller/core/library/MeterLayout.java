package de.drremote.dsp408controller.core.library;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import de.drremote.dsp408controller.core.protocol.DspChannel;

public final class MeterLayout {
    private final List<MeterSlotLocation> slots;
    private final Map<DspChannel, MeterSlotLocation> slotsByChannel;
    private final int responseLengthBytes;
    private final int statusByteOffset;
    private final int limiterMaskOffset;

    public MeterLayout(List<MeterSlotLocation> slots,
                       int responseLengthBytes,
                       int statusByteOffset,
                       int limiterMaskOffset) {
        this.slots = List.copyOf(new ArrayList<>(slots));

        Map<DspChannel, MeterSlotLocation> byChannel = new EnumMap<>(DspChannel.class);
        for (MeterSlotLocation slot : this.slots) {
            byChannel.put(slot.channel(), slot);
        }
        this.slotsByChannel = Map.copyOf(byChannel);
        this.responseLengthBytes = responseLengthBytes;
        this.statusByteOffset = statusByteOffset;
        this.limiterMaskOffset = limiterMaskOffset;
    }

    public List<MeterSlotLocation> slots() {
        return slots;
    }

    public MeterSlotLocation slot(DspChannel channel) {
        return slotsByChannel.get(channel);
    }

    public int responseLengthBytes() {
        return responseLengthBytes;
    }

    public int statusByteOffset() {
        return statusByteOffset;
    }

    public int limiterMaskOffset() {
        return limiterMaskOffset;
    }

    public boolean hasSlots() {
        return !slots.isEmpty();
    }
}