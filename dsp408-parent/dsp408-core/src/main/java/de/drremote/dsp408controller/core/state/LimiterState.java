package de.drremote.dsp408controller.core.state;

import de.drremote.dsp408controller.core.protocol.DspChannel;

public final class LimiterState {
    private final DspChannel output;

    private Double attackMs;
    private Double releaseMs;
    private Double thresholdDb;
    private Integer unknownValue;
    private Boolean runtimeActive;

    private boolean attackConfirmedFromDevice;
    private boolean releaseConfirmedFromDevice;
    private boolean thresholdConfirmedFromDevice;
    private boolean unknownConfirmedFromDevice;
    private boolean runtimeConfirmedFromDevice;

    private String lastAttackUpdateSource;
    private String lastReleaseUpdateSource;
    private String lastThresholdUpdateSource;
    private String lastUnknownUpdateSource;
    private String lastRuntimeUpdateSource;

    public LimiterState(DspChannel output) {
        this.output = output;
    }

    public synchronized DspChannel output() {
        return output;
    }

    public synchronized Double attackMs() {
        return attackMs;
    }

    public synchronized Double releaseMs() {
        return releaseMs;
    }

    public synchronized Double thresholdDb() {
        return thresholdDb;
    }

    public synchronized Integer unknownValue() {
        return unknownValue;
    }

    public synchronized Boolean runtimeActive() {
        return runtimeActive;
    }

    public synchronized boolean attackConfirmedFromDevice() {
        return attackConfirmedFromDevice;
    }

    public synchronized boolean releaseConfirmedFromDevice() {
        return releaseConfirmedFromDevice;
    }

    public synchronized boolean thresholdConfirmedFromDevice() {
        return thresholdConfirmedFromDevice;
    }

    public synchronized boolean unknownConfirmedFromDevice() {
        return unknownConfirmedFromDevice;
    }

    public synchronized boolean runtimeConfirmedFromDevice() {
        return runtimeConfirmedFromDevice;
    }

    public synchronized String lastAttackUpdateSource() {
        return lastAttackUpdateSource;
    }

    public synchronized String lastReleaseUpdateSource() {
        return lastReleaseUpdateSource;
    }

    public synchronized String lastThresholdUpdateSource() {
        return lastThresholdUpdateSource;
    }

    public synchronized String lastUnknownUpdateSource() {
        return lastUnknownUpdateSource;
    }

    public synchronized String lastRuntimeUpdateSource() {
        return lastRuntimeUpdateSource;
    }

    public synchronized boolean hasAnyValues() {
        return attackMs != null
                || releaseMs != null
                || thresholdDb != null
                || unknownValue != null
                || runtimeActive != null;
    }

    public synchronized void updateAttack(Double attackMs, String source, boolean confirmed) {
        this.attackMs = attackMs;
        this.lastAttackUpdateSource = source;
        this.attackConfirmedFromDevice = confirmed;
    }

    public synchronized void updateRelease(Double releaseMs, String source, boolean confirmed) {
        this.releaseMs = releaseMs;
        this.lastReleaseUpdateSource = source;
        this.releaseConfirmedFromDevice = confirmed;
    }

    public synchronized void updateThreshold(Double thresholdDb, String source, boolean confirmed) {
        this.thresholdDb = thresholdDb;
        this.lastThresholdUpdateSource = source;
        this.thresholdConfirmedFromDevice = confirmed;
    }

    public synchronized void updateUnknown(Integer unknownValue, String source, boolean confirmed) {
        this.unknownValue = unknownValue;
        this.lastUnknownUpdateSource = source;
        this.unknownConfirmedFromDevice = confirmed;
    }

    public synchronized void updateRuntimeActive(Boolean runtimeActive, String source, boolean confirmed) {
        this.runtimeActive = runtimeActive;
        this.lastRuntimeUpdateSource = source;
        this.runtimeConfirmedFromDevice = confirmed;
    }
}