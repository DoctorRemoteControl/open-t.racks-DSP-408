package de.drremote.dsp408controller.core.state;

import de.drremote.dsp408controller.core.protocol.DspChannel;

public final class InputGateState {
    private final DspChannel input;

    private Double thresholdDb;
    private Double holdMs;
    private Double attackMs;
    private Double releaseMs;

    private boolean thresholdConfirmedFromDevice;
    private boolean holdConfirmedFromDevice;
    private boolean attackConfirmedFromDevice;
    private boolean releaseConfirmedFromDevice;

    private String lastThresholdUpdateSource;
    private String lastHoldUpdateSource;
    private String lastAttackUpdateSource;
    private String lastReleaseUpdateSource;

    public InputGateState(DspChannel input) {
        this.input = input;
    }

    public synchronized DspChannel input() {
        return input;
    }

    public synchronized Double thresholdDb() {
        return thresholdDb;
    }

    public synchronized Double holdMs() {
        return holdMs;
    }

    public synchronized Double attackMs() {
        return attackMs;
    }

    public synchronized Double releaseMs() {
        return releaseMs;
    }

    public synchronized boolean thresholdConfirmedFromDevice() {
        return thresholdConfirmedFromDevice;
    }

    public synchronized boolean holdConfirmedFromDevice() {
        return holdConfirmedFromDevice;
    }

    public synchronized boolean attackConfirmedFromDevice() {
        return attackConfirmedFromDevice;
    }

    public synchronized boolean releaseConfirmedFromDevice() {
        return releaseConfirmedFromDevice;
    }

    public synchronized String lastThresholdUpdateSource() {
        return lastThresholdUpdateSource;
    }

    public synchronized String lastHoldUpdateSource() {
        return lastHoldUpdateSource;
    }

    public synchronized String lastAttackUpdateSource() {
        return lastAttackUpdateSource;
    }

    public synchronized String lastReleaseUpdateSource() {
        return lastReleaseUpdateSource;
    }

    public synchronized boolean hasAnyValues() {
        return thresholdDb != null || holdMs != null || attackMs != null || releaseMs != null;
    }

    public synchronized void updateThreshold(Double thresholdDb, String source, boolean confirmed) {
        this.thresholdDb = thresholdDb;
        this.lastThresholdUpdateSource = source;
        this.thresholdConfirmedFromDevice = confirmed;
    }

    public synchronized void updateHold(Double holdMs, String source, boolean confirmed) {
        this.holdMs = holdMs;
        this.lastHoldUpdateSource = source;
        this.holdConfirmedFromDevice = confirmed;
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
}
