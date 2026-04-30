package de.drremote.dsp408controller.core.decode;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import de.drremote.dsp408controller.core.codec.DspCodecRegistry;
import de.drremote.dsp408controller.core.library.DspFieldLocation;
import de.drremote.dsp408controller.core.library.DspLibrary;
import de.drremote.dsp408controller.core.state.DspState;

public final class DelayBlockDecoder implements BlockDecoder {
    private final DspState state;
    private final Consumer<String> log;
    private final DspLibrary library;

    public DelayBlockDecoder(DspState state, Consumer<String> log, DspLibrary library) {
        this.state = state;
        this.log = log;
        this.library = library;
    }

    @Override
    public void decode(int blockIndex, byte[] data) {
        if (data == null) {
            return;
        }

        List<DspFieldLocation> locations = library.delayReadLocationsForBlock(blockIndex);
        if (locations.isEmpty()) {
            return;
        }

        for (DspFieldLocation location : locations) {
            if (data.length < location.dataOffset() + 2) {
                continue;
            }

            int raw = readUInt16LE(data, location.dataOffset());
            double delayMs = DspCodecRegistry.delayMs().rawToDouble(raw);

            state.markDelayFromDevice(
                    location.channel(),
                    delayMs,
                    String.format(Locale.ROOT, "block:%02X@%02X raw=%d", blockIndex, location.dataOffset(), raw)
            );

            if (log != null) {
                log.accept(String.format(
                        Locale.ROOT,
                        "DELAY RX %s -> raw=%d -> %.3f ms (block %02X offset %02X)",
                        location.channel().displayName(),
                        raw,
                        delayMs,
                        blockIndex,
                        location.dataOffset()
                ));
            }
        }
    }

    private static int readUInt16LE(byte[] data, int offset) {
        return (data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8);
    }
}

