package de.drremote.dsp408controller.core.decode;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import de.drremote.dsp408controller.core.codec.DspCodecRegistry;
import de.drremote.dsp408controller.core.library.CrossoverChannelLocation;
import de.drremote.dsp408controller.core.library.DspLibrary;
import de.drremote.dsp408controller.core.library.PeqFieldLocation;
import de.drremote.dsp408controller.core.protocol.CrossoverSlope;
import de.drremote.dsp408controller.core.state.DspState;

public final class CrossoverBlockDecoder implements BlockDecoder {
    private final DspState state;
    private final Consumer<String> log;
    private final DspLibrary library;

    public CrossoverBlockDecoder(DspState state, Consumer<String> log, DspLibrary library) {
        this.state = state;
        this.log = log;
        this.library = library;
    }

    @Override
    public void decode(int blockIndex, byte[] data) {
        if (data == null) {
            return;
        }

        List<CrossoverChannelLocation> locations = library.crossoverLocationsForBlock(blockIndex);
        if (locations.isEmpty()) {
            return;
        }

        for (CrossoverChannelLocation location : locations) {
            decodeHighPass(location, blockIndex);
            decodeLowPass(location, blockIndex);
        }
    }

    private void decodeHighPass(CrossoverChannelLocation location, int blockIndex) {
        Integer frequencyRaw = readUInt16LE(
                state.getBlock(location.highPassFrequency().blockIndex()),
                location.highPassFrequency().dataOffset()
        );
        Integer modeRaw = readUInt8(location.highPassMode());
        Integer bypassRaw = readUInt8(location.highPassBypass());

        Boolean bypass = resolveBypass(
                modeRaw,
                bypassRaw,
                location.highPassMode(),
                location.highPassBypass()
        );

        if (frequencyRaw == null || bypass == null) {
            return;
        }

        double frequencyHz = DspCodecRegistry.peqFrequency().rawToDouble(frequencyRaw);

        CrossoverSlope slope;
        try {
            slope = resolveSlope(modeRaw, bypass);
        } catch (IllegalArgumentException e) {
            if (log != null) {
                log.accept(String.format(
                        Locale.ROOT,
                        "XOVER RX %s HP skipped: unsupported slope raw=%s (block %02X)",
                        location.channel().displayName(),
                        modeRaw == null ? "-" : Integer.toString(modeRaw),
                        blockIndex
                ));
            }
            return;
        }

        state.markCrossoverHighPassFromDevice(
                location.channel(),
                frequencyHz,
                slope,
                bypass,
                String.format(
                        Locale.ROOT,
                        "hp block %02X raw[f=%d mode=%s bypass=%s]",
                        blockIndex,
                        frequencyRaw,
                        modeRaw == null ? "-" : Integer.toString(modeRaw),
                        bypassRaw == null ? "-" : Integer.toString(bypassRaw)
                )
        );

        if (log != null) {
            log.accept(String.format(
                    Locale.ROOT,
                    "XOVER RX %s HP -> freq=%.2f Hz slope=%s bypass=%s (block %02X modeRaw=%s bypassRaw=%s)",
                    location.channel().displayName(),
                    frequencyHz,
                    slope == null ? "-" : slope.displayName(),
                    bypass ? "on" : "off",
                    blockIndex,
                    modeRaw == null ? "-" : Integer.toString(modeRaw),
                    bypassRaw == null ? "-" : Integer.toString(bypassRaw)
            ));
        }
    }

    private void decodeLowPass(CrossoverChannelLocation location, int blockIndex) {
        Integer frequencyRaw = readUInt16LE(
                state.getBlock(location.lowPassFrequency().blockIndex()),
                location.lowPassFrequency().dataOffset()
        );
        Integer modeRaw = readUInt8(location.lowPassMode());
        Integer bypassRaw = readUInt8(location.lowPassBypass());

        Boolean bypass = resolveBypass(
                modeRaw,
                bypassRaw,
                location.lowPassMode(),
                location.lowPassBypass()
        );

        if (frequencyRaw == null || bypass == null) {
            return;
        }

        double frequencyHz = DspCodecRegistry.peqFrequency().rawToDouble(frequencyRaw);

        CrossoverSlope slope;
        try {
            slope = resolveSlope(modeRaw, bypass);
        } catch (IllegalArgumentException e) {
            if (log != null) {
                log.accept(String.format(
                        Locale.ROOT,
                        "XOVER RX %s LP skipped: unsupported slope raw=%s (block %02X)",
                        location.channel().displayName(),
                        modeRaw == null ? "-" : Integer.toString(modeRaw),
                        blockIndex
                ));
            }
            return;
        }

        state.markCrossoverLowPassFromDevice(
                location.channel(),
                frequencyHz,
                slope,
                bypass,
                String.format(
                        Locale.ROOT,
                        "lp block %02X raw[f=%d mode=%s bypass=%s]",
                        blockIndex,
                        frequencyRaw,
                        modeRaw == null ? "-" : Integer.toString(modeRaw),
                        bypassRaw == null ? "-" : Integer.toString(bypassRaw)
                )
        );

        if (log != null) {
            log.accept(String.format(
                    Locale.ROOT,
                    "XOVER RX %s LP -> freq=%.2f Hz slope=%s bypass=%s (block %02X modeRaw=%s bypassRaw=%s)",
                    location.channel().displayName(),
                    frequencyHz,
                    slope == null ? "-" : slope.displayName(),
                    bypass ? "on" : "off",
                    blockIndex,
                    modeRaw == null ? "-" : Integer.toString(modeRaw),
                    bypassRaw == null ? "-" : Integer.toString(bypassRaw)
            ));
        }
    }

    private static Boolean resolveBypass(Integer modeRaw,
                                         Integer bypassRaw,
                                         PeqFieldLocation modeLocation,
                                         PeqFieldLocation bypassLocation) {
        if (sameField(modeLocation, bypassLocation)) {
            Integer raw = modeRaw != null ? modeRaw : bypassRaw;
            if (raw == null) {
                return null;
            }
            return raw == 0x00;
        }

        if (bypassRaw != null) {
            return bypassRaw == 0x00;
        }

        if (modeRaw != null) {
            return modeRaw == 0x00;
        }

        return null;
    }

    private static CrossoverSlope resolveSlope(Integer modeRaw, boolean bypass) {
        if (bypass || modeRaw == null || modeRaw == 0x00) {
            return null;
        }
        return CrossoverSlope.fromRaw(modeRaw);
    }

    private static boolean sameField(PeqFieldLocation a, PeqFieldLocation b) {
        if (a == null || b == null) {
            return false;
        }
        return a.blockIndex() == b.blockIndex() && a.dataOffset() == b.dataOffset();
    }

    private Integer readUInt8(PeqFieldLocation location) {
        if (location == null) {
            return null;
        }
        byte[] data = state.getBlock(location.blockIndex());
        if (data == null || location.dataOffset() < 0 || data.length <= location.dataOffset()) {
            return null;
        }
        return data[location.dataOffset()] & 0xFF;
    }

    private static Integer readUInt16LE(byte[] data, int offset) {
        if (data == null || offset < 0 || data.length < offset + 2) {
            return null;
        }
        return (data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8);
    }
}