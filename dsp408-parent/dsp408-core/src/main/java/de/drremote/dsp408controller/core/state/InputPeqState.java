package de.drremote.dsp408controller.core.state;

import de.drremote.dsp408controller.core.protocol.DspChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class InputPeqState {
    private final DspChannel input;
    private final Map<Integer, PeqBandState> bands = new TreeMap<>();

    public InputPeqState(DspChannel input) {
        this.input = input;
    }

    public synchronized DspChannel input() {
        return input;
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
