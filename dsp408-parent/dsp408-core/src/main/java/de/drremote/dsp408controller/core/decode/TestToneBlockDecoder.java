package de.drremote.dsp408controller.core.decode;

import java.util.Locale;
import java.util.function.Consumer;

import de.drremote.dsp408controller.core.library.DspLibrary;
import de.drremote.dsp408controller.core.library.TestToneReadSpec;
import de.drremote.dsp408controller.core.protocol.DspProtocol;
import de.drremote.dsp408controller.core.state.DspState;

public final class TestToneBlockDecoder implements BlockDecoder {
    private final DspState state;
    private final Consumer<String> log;
    private final TestToneReadSpec spec;

    public TestToneBlockDecoder(DspState state, Consumer<String> log, DspLibrary library) {
        this.state = state;
        this.log = log;
        this.spec = library.testToneReadSpec();
    }

    @Override
    public void decode(int blockIndex, byte[] data) {
        if (spec == null || data == null || blockIndex != spec.blockIndex()) {
            return;
        }

        Integer sourceRaw = readUInt8(data, spec.sourceDataOffset());
        Integer sineFrequencyRaw = readUInt8(data, spec.sineFrequencyDataOffset());
        if (sourceRaw == null || sineFrequencyRaw == null) {
            return;
        }

        String sourceLabel = DspProtocol.testToneSourceLabel(sourceRaw);
        double sineFrequencyHz = DspProtocol.testToneSineFrequencyHz(sineFrequencyRaw);

        state.markTestToneSourceFromDevice(
                sourceRaw,
                sourceLabel,
                String.format(Locale.ROOT, "block:%02X@%02X raw=%d", blockIndex, spec.sourceDataOffset(), sourceRaw)
        );
        state.markTestToneSineFrequencyFromDevice(
                sineFrequencyRaw,
                sineFrequencyHz,
                String.format(Locale.ROOT, "block:%02X@%02X raw=%d", blockIndex, spec.sineFrequencyDataOffset(), sineFrequencyRaw)
        );

        if (log != null) {
            log.accept(String.format(
                    Locale.ROOT,
                    "TONE RX source=%s(0x%02X) sineRaw=%d sineHz=%.1f",
                    sourceLabel,
                    sourceRaw,
                    sineFrequencyRaw,
                    sineFrequencyHz
            ));
        }
    }

    private static Integer readUInt8(byte[] data, int offset) {
        if (data == null || offset < 0 || data.length <= offset) {
            return null;
        }
        return data[offset] & 0xFF;
    }
}
