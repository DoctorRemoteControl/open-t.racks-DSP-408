package de.drremote.dsp408controller.core.state;

import java.util.EnumMap;
import java.util.Map;

import de.drremote.dsp408controller.core.protocol.DspChannel;

public final class MatrixOutputState {
    private final DspChannel output;
    private DspChannel routedInput;
    private final Map<DspChannel, Double> crosspointGainDb = new EnumMap<>(DspChannel.class);

    public MatrixOutputState(DspChannel output) {
        this.output = output;
    }

    public synchronized DspChannel output() {
        return output;
    }

    public synchronized DspChannel routedInput() {
        return routedInput;
    }

    public synchronized void setRoutedInput(DspChannel input) {
        this.routedInput = input;
    }

    public synchronized void setCrosspointGain(DspChannel input, double db) {
        crosspointGainDb.put(input, db);
    }

    public synchronized Double crosspointGain(DspChannel input) {
        return crosspointGainDb.get(input);
    }

    public synchronized Map<DspChannel, Double> crosspointGainsSnapshot() {
        return Map.copyOf(crosspointGainDb);
    }
}