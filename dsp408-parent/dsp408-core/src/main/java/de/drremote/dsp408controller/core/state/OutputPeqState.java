package de.drremote.dsp408controller.core.state;

import de.drremote.dsp408controller.core.protocol.DspChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class OutputPeqState {
    private final DspChannel output;
    private final Map<Integer, PeqBandState> bands = new TreeMap<>();

    public OutputPeqState(DspChannel output) {
        this.output = output;
    }

    public synchronized DspChannel output() {
        return output;
    }

    public synchronized PeqBandState band(int bandIndex) {
        return bands.get(bandIndex);
    }

    public synchronized PeqBandState ensureBand(int bandIndex) {
        return bands.computeIfAbsent(bandIndex, PeqBandState::new);
    }

    public synchronized List<PeqBandState> bands() {
        return new ArrayList<>(bands.values());
    }

    public synchronized boolean hasAnyValues() {
        return bands.values().stream().anyMatch(PeqBandState::hasAnyValues);
    }
}
