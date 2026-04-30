package de.drremote.dsp408controller.core.state;

import de.drremote.dsp408controller.core.protocol.DspChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class InputGeqState {
    private final DspChannel input;
    private final Map<Integer, InputGeqBandState> bands = new TreeMap<>();

    public InputGeqState(DspChannel input) {
        this.input = input;
    }

    public synchronized DspChannel input() {
        return input;
    }

    public synchronized InputGeqBandState band(int bandIndex) {
        return bands.get(bandIndex);
    }

    public synchronized InputGeqBandState ensureBand(int bandIndex) {
        return bands.computeIfAbsent(bandIndex, InputGeqBandState::new);
    }

    public synchronized List<InputGeqBandState> bands() {
        return new ArrayList<>(bands.values());
    }

    public synchronized boolean hasAnyValues() {
        return bands.values().stream().anyMatch(InputGeqBandState::hasAnyValues);
    }
}
