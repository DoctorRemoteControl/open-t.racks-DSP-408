package de.drremote.dsp408controller.core.state;

import de.drremote.dsp408controller.core.protocol.DspChannel;

public final class CrossoverState {
    private final DspChannel channel;
    private final CrossoverFilterState highPass = new CrossoverFilterState();
    private final CrossoverFilterState lowPass = new CrossoverFilterState();

    public CrossoverState(DspChannel channel) {
        this.channel = channel;
    }

    public synchronized DspChannel channel() {
        return channel;
    }

    public synchronized CrossoverFilterState highPass() {
        return highPass;
    }

    public synchronized CrossoverFilterState lowPass() {
        return lowPass;
    }

    public synchronized boolean hasAnyValues() {
        return highPass.hasAnyValues() || lowPass.hasAnyValues();
    }
}
