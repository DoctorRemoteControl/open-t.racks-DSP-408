package de.drremote.dsp408controller.core.decode;

import de.drremote.dsp408controller.core.codec.DspCodecRegistry;
import de.drremote.dsp408controller.core.library.DspLibrary;
import de.drremote.dsp408controller.core.library.InputGeqBandLocation;
import de.drremote.dsp408controller.core.state.DspState;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public final class InputGeqBlockDecoder implements BlockDecoder {
    private final DspState state;
    private final Consumer<String> log;
    private final DspLibrary library;

    public InputGeqBlockDecoder(DspState state, Consumer<String> log, DspLibrary library) {
        this.state = state;
        this.log = log;
        this.library = library;
    }

    @Override
    public void decode(int blockIndex, byte[] data) {
        if (data == null) {
            return;
        }

        List<InputGeqBandLocation> locations = library.inputGeqLocationsForBlock(blockIndex);
        if (locations.isEmpty()) {
            return;
        }

        for (InputGeqBandLocation location : locations) {
            if (data.length < location.gain().dataOffset() + 2) {
                continue;
            }

            int raw = readUInt16LE(data, location.gain().dataOffset());
            double gainDb = DspCodecRegistry.peqGain().rawToDouble(raw);
            Double frequencyHz = library.inputGeqBandFrequency(location.bandIndex());
            if (frequencyHz == null) {
                continue;
            }

            state.markInputGeqBandFromDevice(
                    location.channel(),
                    location.bandIndex(),
                    gainDb,
                    frequencyHz,
                    String.format(
                            Locale.ROOT,
                            "block:%02X@%02X raw=%d",
                            blockIndex,
                            location.gain().dataOffset(),
                            raw
                    )
            );

            if (log != null) {
                log.accept(String.format(
                        Locale.ROOT,
                        "IGEQ RX %s GEQ%d -> %.1f dB @ %.2f Hz (block %02X offset %02X)",
                        location.channel().displayName(),
                        location.bandIndex(),
                        gainDb,
                        frequencyHz,
                        blockIndex,
                        location.gain().dataOffset()
                ));
            }
        }
    }

    private static int readUInt16LE(byte[] data, int offset) {
        return (data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8);
    }
}
