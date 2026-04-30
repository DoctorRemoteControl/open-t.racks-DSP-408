package de.drremote.dsp408controller.core.state;

import de.drremote.dsp408controller.core.protocol.CrossoverSlope;

public final class CrossoverFilterState {
    private Double frequencyHz;
    private CrossoverSlope slope;
    private Boolean bypass;

    private boolean frequencyConfirmedFromDevice;
    private boolean slopeConfirmedFromDevice;
    private boolean bypassConfirmedFromDevice;

    private boolean frequencyDirty;
    private boolean slopeDirty;
    private boolean bypassDirty;

    private String lastFrequencyUpdateSource;
    private String lastSlopeUpdateSource;
    private String lastBypassUpdateSource;

    public synchronized Double frequencyHz() {
        return frequencyHz;
    }

    public synchronized CrossoverSlope slope() {
        return slope;
    }

    public synchronized Boolean bypass() {
        return bypass;
    }

    public synchronized boolean frequencyConfirmedFromDevice() {
        return frequencyConfirmedFromDevice;
    }

    public synchronized boolean slopeConfirmedFromDevice() {
        return slopeConfirmedFromDevice;
    }

    public synchronized boolean bypassConfirmedFromDevice() {
        return bypassConfirmedFromDevice;
    }

    public synchronized boolean frequencyDirty() {
        return frequencyDirty;
    }

    public synchronized boolean slopeDirty() {
        return slopeDirty;
    }

    public synchronized boolean bypassDirty() {
        return bypassDirty;
    }

    public synchronized String lastFrequencyUpdateSource() {
        return lastFrequencyUpdateSource;
    }

    public synchronized String lastSlopeUpdateSource() {
        return lastSlopeUpdateSource;
    }

    public synchronized String lastBypassUpdateSource() {
        return lastBypassUpdateSource;
    }

    public synchronized boolean hasAnyValues() {
        return frequencyHz != null || slope != null || bypass != null;
    }

    public synchronized void updateFrequency(Double frequencyHz, String source, boolean confirmed) {
        this.frequencyHz = frequencyHz;
        this.lastFrequencyUpdateSource = source;
        this.frequencyConfirmedFromDevice = confirmed;
        this.frequencyDirty = !confirmed;
    }

    public synchronized void updateSlope(CrossoverSlope slope, String source, boolean confirmed) {
        if (slope == null) {
            return;
        }
        this.slope = slope;
        this.lastSlopeUpdateSource = source;
        this.slopeConfirmedFromDevice = confirmed;
        this.slopeDirty = !confirmed;
    }

    public synchronized void updateBypass(Boolean bypass, String source, boolean confirmed) {
        this.bypass = bypass;
        this.lastBypassUpdateSource = source;
        this.bypassConfirmedFromDevice = confirmed;
        this.bypassDirty = !confirmed;
    }
}
