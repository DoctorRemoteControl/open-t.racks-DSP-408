package de.drremote.dsp408controller.core.state;

import de.drremote.dsp408controller.core.protocol.DspChannel;

public final class ChannelState {
  private final DspChannel channel;

  private Double gainDb;
  private Boolean muted;

  private Boolean phaseInverted;
  private Double delayMs;

  private boolean gainConfirmedFromDevice;
  private boolean muteConfirmedFromDevice;
  private boolean phaseConfirmedFromDevice;
  private boolean delayConfirmedFromDevice;

  private boolean gainDirty;
  private boolean muteDirty;
  private boolean phaseDirty;
  private boolean delayDirty;

  private String lastGainUpdateSource;
  private String lastMuteUpdateSource;
  private String lastPhaseUpdateSource;
  private String lastDelayUpdateSource;

  public ChannelState(DspChannel channel) {
    this.channel = channel;
  }

  public synchronized DspChannel channel() {
    return channel;
  }

  public synchronized Double gainDb() {
    return gainDb;
  }

  public synchronized Boolean muted() {
    return muted;
  }

  public synchronized Boolean phaseInverted() {
    return phaseInverted;
  }

  public synchronized Double delayMs() {
    return delayMs;
  }

  public synchronized boolean gainConfirmedFromDevice() {
    return gainConfirmedFromDevice;
  }

  public synchronized boolean muteConfirmedFromDevice() {
    return muteConfirmedFromDevice;
  }

  public synchronized boolean phaseConfirmedFromDevice() {
    return phaseConfirmedFromDevice;
  }

  public synchronized boolean delayConfirmedFromDevice() {
    return delayConfirmedFromDevice;
  }

  public synchronized boolean gainDirty() {
    return gainDirty;
  }

  public synchronized boolean muteDirty() {
    return muteDirty;
  }

  public synchronized boolean phaseDirty() {
    return phaseDirty;
  }

  public synchronized boolean delayDirty() {
    return delayDirty;
  }

  public synchronized String lastGainUpdateSource() {
    return lastGainUpdateSource;
  }

  public synchronized String lastMuteUpdateSource() {
    return lastMuteUpdateSource;
  }

  public synchronized String lastPhaseUpdateSource() {
    return lastPhaseUpdateSource;
  }

  public synchronized String lastDelayUpdateSource() {
    return lastDelayUpdateSource;
  }

  public synchronized void updateGain(Double gainDb, String source, boolean confirmed) {
    this.gainDb = gainDb;
    this.lastGainUpdateSource = source;
    this.gainConfirmedFromDevice = confirmed;
    this.gainDirty = !confirmed;
  }

  public synchronized void updateMuted(Boolean muted, String source, boolean confirmed) {
    this.muted = muted;
    this.lastMuteUpdateSource = source;
    this.muteConfirmedFromDevice = confirmed;
    this.muteDirty = !confirmed;
  }

  public synchronized void updatePhase(Boolean phaseInverted, String source, boolean confirmed) {
    this.phaseInverted = phaseInverted;
    this.lastPhaseUpdateSource = source;
    this.phaseConfirmedFromDevice = confirmed;
    this.phaseDirty = !confirmed;
  }

  public synchronized void updateDelay(Double delayMs, String source, boolean confirmed) {
    this.delayMs = delayMs;
    this.lastDelayUpdateSource = source;
    this.delayConfirmedFromDevice = confirmed;
    this.delayDirty = !confirmed;
  }
}
