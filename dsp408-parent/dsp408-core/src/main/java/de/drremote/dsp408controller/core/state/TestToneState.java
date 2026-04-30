package de.drremote.dsp408controller.core.state;

public final class TestToneState {
    private Integer sourceIndex;
    private String sourceLabel;
    private String lastSourceUpdateSource;
    private boolean sourceConfirmedFromDevice;

    private Integer sineFrequencyRaw;
    private Double sineFrequencyHz;
    private String lastSineFrequencyUpdateSource;
    private boolean sineFrequencyConfirmedFromDevice;

    public synchronized void reset() {
        sourceIndex = null;
        sourceLabel = null;
        lastSourceUpdateSource = null;
        sourceConfirmedFromDevice = false;

        sineFrequencyRaw = null;
        sineFrequencyHz = null;
        lastSineFrequencyUpdateSource = null;
        sineFrequencyConfirmedFromDevice = false;
    }

    public synchronized Integer sourceIndex() {
        return sourceIndex;
    }

    public synchronized String sourceLabel() {
        return sourceLabel;
    }

    public synchronized String lastSourceUpdateSource() {
        return lastSourceUpdateSource;
    }

    public synchronized boolean sourceConfirmedFromDevice() {
        return sourceConfirmedFromDevice;
    }

    public synchronized Integer sineFrequencyRaw() {
        return sineFrequencyRaw;
    }

    public synchronized Double sineFrequencyHz() {
        return sineFrequencyHz;
    }

    public synchronized String lastSineFrequencyUpdateSource() {
        return lastSineFrequencyUpdateSource;
    }

    public synchronized boolean sineFrequencyConfirmedFromDevice() {
        return sineFrequencyConfirmedFromDevice;
    }

    public synchronized boolean hasAnyValues() {
        return sourceIndex != null || sourceLabel != null || sineFrequencyRaw != null || sineFrequencyHz != null;
    }

    public synchronized void updateSource(int sourceIndex, String sourceLabel, String source, boolean confirmed) {
        this.sourceIndex = sourceIndex;
        this.sourceLabel = sourceLabel;
        this.lastSourceUpdateSource = source;
        this.sourceConfirmedFromDevice = confirmed;
    }

    public synchronized void updateSineFrequency(int sineFrequencyRaw,
                                                 double sineFrequencyHz,
                                                 String source,
                                                 boolean confirmed) {
        this.sineFrequencyRaw = sineFrequencyRaw;
        this.sineFrequencyHz = sineFrequencyHz;
        this.lastSineFrequencyUpdateSource = source;
        this.sineFrequencyConfirmedFromDevice = confirmed;
    }
}
