package de.drremote.dsp408controller.core.state;

import de.drremote.dsp408controller.core.protocol.DspChannel;

public final class CompressorState {
    private final DspChannel output;

    private Integer ratioIndex;
    private String ratioLabel;
    private Double attackMs;
    private Double releaseMs;
    private Double kneeDb;
    private Double thresholdDb;

    private boolean ratioConfirmedFromDevice;
    private boolean attackConfirmedFromDevice;
    private boolean releaseConfirmedFromDevice;
    private boolean kneeConfirmedFromDevice;
    private boolean thresholdConfirmedFromDevice;

    private String lastRatioUpdateSource;
    private String lastAttackUpdateSource;
    private String lastReleaseUpdateSource;
    private String lastKneeUpdateSource;
    private String lastThresholdUpdateSource;

    public CompressorState(DspChannel output) {
        this.output = output;
    }

    public synchronized DspChannel output() {
        return output;
    }

    public synchronized Integer ratioIndex() {
        return ratioIndex;
    }

    public synchronized String ratioLabel() {
        return ratioLabel;
    }

    public synchronized Double attackMs() {
        return attackMs;
    }

    public synchronized Double releaseMs() {
        return releaseMs;
    }

    public synchronized Double kneeDb() {
        return kneeDb;
    }

    public synchronized Double thresholdDb() {
        return thresholdDb;
    }

    public synchronized boolean ratioConfirmedFromDevice() {
        return ratioConfirmedFromDevice;
    }

    public synchronized boolean attackConfirmedFromDevice() {
        return attackConfirmedFromDevice;
    }

    public synchronized boolean releaseConfirmedFromDevice() {
        return releaseConfirmedFromDevice;
    }

    public synchronized boolean kneeConfirmedFromDevice() {
        return kneeConfirmedFromDevice;
    }

    public synchronized boolean thresholdConfirmedFromDevice() {
        return thresholdConfirmedFromDevice;
    }

    public synchronized String lastRatioUpdateSource() {
        return lastRatioUpdateSource;
    }

    public synchronized String lastAttackUpdateSource() {
        return lastAttackUpdateSource;
    }

    public synchronized String lastReleaseUpdateSource() {
        return lastReleaseUpdateSource;
    }

    public synchronized String lastKneeUpdateSource() {
        return lastKneeUpdateSource;
    }

    public synchronized String lastThresholdUpdateSource() {
        return lastThresholdUpdateSource;
    }

    public synchronized boolean hasAnyValues() {
        return ratioIndex != null
                || ratioLabel != null
                || attackMs != null
                || releaseMs != null
                || kneeDb != null
                || thresholdDb != null;
    }

    public synchronized void updateRatio(Integer ratioIndex, String ratioLabel, String source, boolean confirmed) {
        this.ratioIndex = ratioIndex;
        this.ratioLabel = ratioLabel;
        this.lastRatioUpdateSource = source;
        this.ratioConfirmedFromDevice = confirmed;
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

    public synchronized void updateKnee(Double kneeDb, String source, boolean confirmed) {
        this.kneeDb = kneeDb;
        this.lastKneeUpdateSource = source;
        this.kneeConfirmedFromDevice = confirmed;
    }

    public synchronized void updateThreshold(Double thresholdDb, String source, boolean confirmed) {
        this.thresholdDb = thresholdDb;
        this.lastThresholdUpdateSource = source;
        this.thresholdConfirmedFromDevice = confirmed;
    }
}