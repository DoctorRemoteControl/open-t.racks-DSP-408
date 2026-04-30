package de.drremote.dsp408controller.core.decode;

import de.drremote.dsp408controller.core.codec.DspCodecRegistry;
import de.drremote.dsp408controller.core.library.DspLibrary;
import de.drremote.dsp408controller.core.library.InputGateLocation;
import de.drremote.dsp408controller.core.state.DspState;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public final class InputGateBlockDecoder implements BlockDecoder {
    private final DspState state;
    private final Consumer<String> log;
    private final DspLibrary library;

    public InputGateBlockDecoder(DspState state, Consumer<String> log, DspLibrary library) {
        this.state = state;
        this.log = log;
        this.library = library;
    }

    @Override
    public void decode(int blockIndex, byte[] data) {
        if (data == null) {
            return;
        }

        List<InputGateLocation> locations = library.inputGateLocationsForBlock(blockIndex);
        if (locations.isEmpty()) {
            return;
        }

        for (InputGateLocation location : locations) {
            Integer attackRaw = readUInt16LE(state.getBlock(location.attack().blockIndex()), location.attack().dataOffset());
            Integer releaseRaw = readUInt16LE(state.getBlock(location.release().blockIndex()), location.release().dataOffset());
            Integer holdRaw = readUInt16LE(state.getBlock(location.hold().blockIndex()), location.hold().dataOffset());
            Integer thresholdRaw = readUInt16LE(
                    state.getBlock(location.threshold().blockIndex()),
                    location.threshold().dataOffset()
            );

            if (attackRaw == null || releaseRaw == null || holdRaw == null || thresholdRaw == null) {
                continue;
            }

            double attackMs = DspCodecRegistry.rawToDynamicsTimeMs(attackRaw);
            double releaseMs = DspCodecRegistry.rawToDynamicsTimeMs(releaseRaw);
            double holdMs = DspCodecRegistry.rawToDynamicsTimeMs(holdRaw);
            double thresholdDb = DspCodecRegistry.rawToDynamicsThresholdDb(thresholdRaw);

            state.markInputGateFromDevice(
                    location.channel(),
                    thresholdDb,
                    holdMs,
                    attackMs,
                    releaseMs,
                    String.format(
                            Locale.ROOT,
                            "gate blocks:%02X@%02X/%02X@%02X/%02X@%02X/%02X@%02X raw=%d/%d/%d/%d",
                            location.attack().blockIndex(),
                            location.attack().dataOffset(),
                            location.release().blockIndex(),
                            location.release().dataOffset(),
                            location.hold().blockIndex(),
                            location.hold().dataOffset(),
                            location.threshold().blockIndex(),
                            location.threshold().dataOffset(),
                            attackRaw,
                            releaseRaw,
                            holdRaw,
                            thresholdRaw
                    )
            );

            if (log != null) {
                log.accept(String.format(
                        Locale.ROOT,
                        "GATE RX %s -> threshold=%.1f dB hold=%.0f ms attack=%.0f ms release=%.0f ms",
                        location.channel().displayName(),
                        thresholdDb,
                        holdMs,
                        attackMs,
                        releaseMs
                ));
            }
        }
    }

    private static Integer readUInt16LE(byte[] data, int offset) {
        if (data == null || offset < 0 || data.length < offset + 2) {
            return null;
        }
        return (data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8);
    }
}
