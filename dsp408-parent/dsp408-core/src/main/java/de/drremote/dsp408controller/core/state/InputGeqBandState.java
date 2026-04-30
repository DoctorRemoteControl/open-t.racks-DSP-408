package de.drremote.dsp408controller.core.state;

public final class InputGeqBandState {
    private final int bandIndex;

    private Double frequencyHz;
    private Double gainDb;
    private boolean frequencyConfirmedFromDevice;
    private boolean gainConfirmedFromDevice;
    private String lastFrequencyUpdateSource;
    private String lastGainUpdateSource;

    public InputGeqBandState(int bandIndex) {
        this.bandIndex = bandIndex;
    }

    public synchronized int bandIndex() {
        return bandIndex;
    }

    public synchronized Double frequencyHz() {
        return frequencyHz;
    }

    public synchronized Double gainDb() {
        return gainDb;
    }

    public synchronized boolean frequencyConfirmedFromDevice() {
        return frequencyConfirmedFromDevice;
    }

    public synchronized boolean gainConfirmedFromDevice() {
        return gainConfirmedFromDevice;
    }

    public synchronized String lastFrequencyUpdateSource() {
        return lastFrequencyUpdateSource;
    }

    public synchronized String lastGainUpdateSource() {
        return lastGainUpdateSource;
    }

    public synchronized boolean hasAnyValues() {
        return frequencyHz != null || gainDb != null;
    }

    public synchronized void updateFrequency(Double frequencyHz, String source, boolean confirmed) {
        this.frequencyHz = frequencyHz;
        this.lastFrequencyUpdateSource = source;
        this.frequencyConfirmedFromDevice = confirmed;
    }

    public synchronized void updateGain(Double gainDb, String source, boolean confirmed) {
        this.gainDb = gainDb;
        this.lastGainUpdateSource = source;
        this.gainConfirmedFromDevice = confirmed;
    }
}
