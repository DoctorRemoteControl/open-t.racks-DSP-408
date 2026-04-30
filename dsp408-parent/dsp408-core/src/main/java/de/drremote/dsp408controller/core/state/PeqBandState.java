package de.drremote.dsp408controller.core.state;

import de.drremote.dsp408controller.core.protocol.PeqFilterType;

public class PeqBandState {
    private final int bandIndex;

    private Double gainDb;
    private Double frequencyHz;
    private Double q;
    private PeqFilterType filterType;
    private Boolean bypass;

    private boolean gainConfirmedFromDevice;
    private boolean frequencyConfirmedFromDevice;
    private boolean qConfirmedFromDevice;
    private boolean typeConfirmedFromDevice;
    private boolean bypassConfirmedFromDevice;

    private String lastGainUpdateSource;
    private String lastFrequencyUpdateSource;
    private String lastQUpdateSource;
    private String lastTypeUpdateSource;
    private String lastBypassUpdateSource;

    public PeqBandState(int bandIndex) {
        this.bandIndex = bandIndex;
    }

    public synchronized int bandIndex() {
        return bandIndex;
    }

    public synchronized Double gainDb() {
        return gainDb;
    }

    public synchronized Double frequencyHz() {
        return frequencyHz;
    }

    public synchronized Double q() {
        return q;
    }

    public synchronized PeqFilterType filterType() {
        return filterType;
    }

    public synchronized Boolean bypass() {
        return bypass;
    }

    public synchronized boolean gainConfirmedFromDevice() {
        return gainConfirmedFromDevice;
    }

    public synchronized boolean frequencyConfirmedFromDevice() {
        return frequencyConfirmedFromDevice;
    }

    public synchronized boolean qConfirmedFromDevice() {
        return qConfirmedFromDevice;
    }

    public synchronized boolean typeConfirmedFromDevice() {
        return typeConfirmedFromDevice;
    }

    public synchronized boolean bypassConfirmedFromDevice() {
        return bypassConfirmedFromDevice;
    }

    public synchronized String lastGainUpdateSource() {
        return lastGainUpdateSource;
    }

    public synchronized String lastFrequencyUpdateSource() {
        return lastFrequencyUpdateSource;
    }

    public synchronized String lastQUpdateSource() {
        return lastQUpdateSource;
    }

    public synchronized String lastTypeUpdateSource() {
        return lastTypeUpdateSource;
    }

    public synchronized String lastBypassUpdateSource() {
        return lastBypassUpdateSource;
    }

    public synchronized boolean hasAnyValues() {
        return gainDb != null || frequencyHz != null || q != null || filterType != null || bypass != null;
    }

    public synchronized void updateGain(Double gainDb, String source, boolean confirmed) {
        this.gainDb = gainDb;
        this.lastGainUpdateSource = source;
        this.gainConfirmedFromDevice = confirmed;
    }

    public synchronized void updateFrequency(Double frequencyHz, String source, boolean confirmed) {
        this.frequencyHz = frequencyHz;
        this.lastFrequencyUpdateSource = source;
        this.frequencyConfirmedFromDevice = confirmed;
    }

    public synchronized void updateQ(Double q, String source, boolean confirmed) {
        this.q = q;
        this.lastQUpdateSource = source;
        this.qConfirmedFromDevice = confirmed;
    }

    public synchronized void updateFilterType(PeqFilterType filterType, String source, boolean confirmed) {
        this.filterType = filterType;
        this.lastTypeUpdateSource = source;
        this.typeConfirmedFromDevice = confirmed;
    }

    public synchronized void updateBypass(Boolean bypass, String source, boolean confirmed) {
        this.bypass = bypass;
        this.lastBypassUpdateSource = source;
        this.bypassConfirmedFromDevice = confirmed;
    }
}
