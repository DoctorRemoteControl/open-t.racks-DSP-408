package de.drremote.dsp408controller.core.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.function.Consumer;

import de.drremote.dsp408controller.core.codec.DspCodecRegistry;
import de.drremote.dsp408controller.core.decode.DspPayloadRouter;
import de.drremote.dsp408controller.core.library.DspLibrary;
import de.drremote.dsp408controller.core.library.DspLibraryLoader;
import de.drremote.dsp408controller.core.library.OutputPeqBandDefaults;
import de.drremote.dsp408controller.core.net.DspClient;
import de.drremote.dsp408controller.core.net.DspConnectionConfig;
import de.drremote.dsp408controller.core.protocol.CrossoverSlope;
import de.drremote.dsp408controller.core.protocol.DspChannel;
import de.drremote.dsp408controller.core.protocol.DspProtocol;
import de.drremote.dsp408controller.core.protocol.PeqFilterType;
import de.drremote.dsp408controller.core.state.CrossoverFilterState;
import de.drremote.dsp408controller.core.state.DspState;
import de.drremote.dsp408controller.core.state.InputGateState;
import de.drremote.dsp408controller.core.state.PeqBandState;

public final class DspController {
	private static final String OUTPUT_PEQ_INCOMPLETE_MESSAGE = "Band state incomplete. Run scanblocks first so output gain/frequency/Q are known. "
			+ "The filter type falls back to the library default if it was not written yet.";
	private static final String INPUT_PEQ_INCOMPLETE_MESSAGE = "Band state incomplete. Input PEQ bypass is currently read back only where the library has a bypass location. "
			+ "Use ipeqset first when bypass is still unknown.";
	private static final String INPUT_GEQ_RANGE_MESSAGE = "Input GEQ band index must be in range 1..31";
	private static final String INPUT_GATE_INCOMPLETE_MESSAGE = "Gate state incomplete. Run scanblocks or use gateset first.";
	private static final String HIGH_PASS_INCOMPLETE_MESSAGE = "High-pass state incomplete. Run scanblocks or use xhpset first.";
	private static final String LOW_PASS_INCOMPLETE_MESSAGE = "Low-pass state incomplete. Output low-pass is readable, but input low-pass slope is not read back yet. Use xlpset first.";

	private final DspState state = new DspState();
	private final DspLibrary library;

	private DspClient client;
	private DspPayloadRouter router;
	private String deviceVersion;
	private byte[] lastSystemInfoPayload;

	public DspController() {
		this(DspLibraryLoader.loadDefault());
	}

	public DspController(Path libraryPath) {
		this(DspLibraryLoader.load(libraryPath));
	}

	private DspController(DspLibrary library) {
		this.library = library;
	}

	public boolean isConnected() {
		return client != null && client.isConnected();
	}

	public DspState state() {
		return state;
	}

	public String deviceVersion() {
		return deviceVersion;
	}

	public byte[] lastSystemInfoPayload() {
		return lastSystemInfoPayload == null ? null
				: Arrays.copyOf(lastSystemInfoPayload, lastSystemInfoPayload.length);
	}

	public void connect(DspConnectionConfig config, Consumer<String> log) throws IOException {
		close();
		state.reset();
		deviceVersion = null;
		lastSystemInfoPayload = null;

		client = new DspClient(config, log);
		router = new DspPayloadRouter(state, log, this::updateDeviceVersion, this::updateSystemInfo, library);
		client.setPayloadListener(router::onPayload);
		client.connect();
	}

	public void close() {
		if (client != null) {
			client.close();
			client = null;
		}
		router = null;
	}

	public void sendHandshakeSequence() throws IOException {
		requireClient().sendHandshakeSequence();
	}

	public void sendKeepAlive() throws IOException {
		requireClient().sendKeepAlive();
	}

	public void requestSystemInfo() throws IOException {
		requireClient().requestSystemInfo();
	}

	public void requestRuntimeMeters() throws IOException {
		requireClient().requestRuntimeMeters();
	}

	public void startMeterPolling(long intervalMs) {
		requireClient().startMeterPolling(intervalMs);
	}

	public void stopMeterPolling() {
		requireClient().stopMeterPolling();
	}

	public void login(String pin) throws IOException {
		requireClient().login(pin);
	}

	public void setGain(DspChannel channel, double db) throws IOException {
		double clamped = DspProtocol.clampDb(db);
		requireClient().setGain(channel, clamped);
		state.markGain(channel, clamped, "tx");
	}

	public void mute(DspChannel channel) throws IOException {
		requireClient().mute(channel);
		state.markMuted(channel, true, "tx");
	}

	public void unmute(DspChannel channel) throws IOException {
		requireClient().unmute(channel);
		state.markMuted(channel, false, "tx");
	}

	public void setPhase(DspChannel channel, boolean inverted) throws IOException {
		requireClient().setPhase(channel, inverted);
		state.markPhase(channel, inverted, "tx");
	}

	public void setDelay(DspChannel channel, double ms) throws IOException {
		double clamped = DspCodecRegistry.clampDelayMs(ms);
		requireClient().setDelay(channel, clamped);
		state.markDelay(channel, clamped, "tx");
	}

	public void sendRaw(byte[] payload) throws IOException {
		requireClient().sendPayload(payload);
	}

	public void readParameterBlock(int blockIndex) throws IOException {
		requireClient().readParameterBlock(blockIndex);
	}

	public void readPresetName(int presetIndex) throws IOException {
		requireClient().readPresetName(presetIndex);
	}

	public void loadPreset(int presetIndex) throws IOException {
		requireClient().loadPreset(presetIndex);
	}

	public void loadPreset(String slotLabel) throws IOException {
		requireClient().loadPreset(slotLabel);
	}

	public void setDelayUnit(int unitRaw) throws IOException {
		requireClient().setDelayUnit(unitRaw);
	}

	public void setDelayUnit(String unit) throws IOException {
		requireClient().setDelayUnit(unit);
	}

	public void setChannelName(DspChannel channel, String name) throws IOException {
		requireAnyChannel(channel);
		requireClient().setChannelName(channel, name);
	}

	public void setMute(DspChannel channel, boolean muted) throws IOException {
		requireAnyChannel(channel);
		requireClient().setMute(channel, muted);
		state.markMuted(channel, muted, "tx");
	}

	public void setOutputPeqFrequency(DspChannel channel, int peqIndex, double hz) throws IOException {
		requireOutput(channel);
		PeqBandState resolved = requireKnownOutputPeqBand(channel, peqIndex);
		double normalizedFrequencyHz = DspCodecRegistry.peqFrequency()
				.rawToDouble(DspCodecRegistry.peqFrequency().doubleToRaw(hz));

		requireClient().setOutputPeq(outputNumber(channel), peqIndex, resolved.gainDb(), normalizedFrequencyHz,
				resolved.q(), resolved.filterType());

		state.markOutputPeqBand(channel, peqIndex, resolved.gainDb(), normalizedFrequencyHz, resolved.q(),
				resolved.filterType(), "tx");
	}

	public void setOutputPeqQRaw(DspChannel channel, int peqIndex, int qRaw) throws IOException {
		requireOutput(channel);
		setOutputPeqQ(channel, peqIndex, DspCodecRegistry.peqQ().rawToDouble(qRaw));
	}

	public void setOutputPeqGainCode(DspChannel channel, int peqIndex, int gainCode) throws IOException {
		requireOutput(channel);
		setOutputPeqGain(channel, peqIndex, DspCodecRegistry.peqGain().rawToDouble(gainCode));
	}

	public void setOutputPeqQ(DspChannel channel, int peqIndex, double q) throws IOException {
		requireOutput(channel);
		PeqBandState resolved = requireKnownOutputPeqBand(channel, peqIndex);
		double normalizedQ = DspCodecRegistry.peqQ().rawToDouble(DspCodecRegistry.peqQ().doubleToRaw(q));

		requireClient().setOutputPeq(outputNumber(channel), peqIndex, resolved.gainDb(), resolved.frequencyHz(),
				normalizedQ, resolved.filterType());

		state.markOutputPeqBand(channel, peqIndex, resolved.gainDb(), resolved.frequencyHz(), normalizedQ,
				resolved.filterType(), "tx");
	}

	public void setOutputPeqGain(DspChannel channel, int peqIndex, double gainDb) throws IOException {
		requireOutput(channel);
		PeqBandState resolved = requireKnownOutputPeqBand(channel, peqIndex);
		double normalizedGain = DspCodecRegistry.peqGain().rawToDouble(DspCodecRegistry.peqGain().doubleToRaw(gainDb));

		requireClient().setOutputPeq(outputNumber(channel), peqIndex, normalizedGain, resolved.frequencyHz(),
				resolved.q(), resolved.filterType());

		state.markOutputPeqBand(channel, peqIndex, normalizedGain, resolved.frequencyHz(), resolved.q(),
				resolved.filterType(), "tx");
	}

	public void setOutputPeqType(DspChannel channel, int peqIndex, PeqFilterType filterType) throws IOException {
		requireOutput(channel);
		PeqBandState resolved = requireKnownOutputPeqBand(channel, peqIndex);

		requireClient().setOutputPeq(outputNumber(channel), peqIndex, resolved.gainDb(), resolved.frequencyHz(),
				resolved.q(), filterType);

		state.markOutputPeqBand(channel, peqIndex, resolved.gainDb(), resolved.frequencyHz(), resolved.q(), filterType,
				"tx");
	}

	public void setOutputPeq(DspChannel channel, int peqIndex, PeqFilterType filterType, double frequencyHz, double q,
			double gainDb) throws IOException {
		requireOutput(channel);
		if (peqIndex < 1 || peqIndex > 9) {
			throw new IllegalArgumentException("PEQ index must be in range 1..9");
		}

		double normalizedFrequencyHz = DspCodecRegistry.peqFrequency()
				.rawToDouble(DspCodecRegistry.peqFrequency().doubleToRaw(frequencyHz));
		double normalizedQ = DspCodecRegistry.peqQ().rawToDouble(DspCodecRegistry.peqQ().doubleToRaw(q));
		double normalizedGain = DspCodecRegistry.peqGain().rawToDouble(DspCodecRegistry.peqGain().doubleToRaw(gainDb));

		requireClient().setOutputPeq(outputNumber(channel), peqIndex, normalizedGain, normalizedFrequencyHz,
				normalizedQ, filterType);

		state.markOutputPeqBand(channel, peqIndex, normalizedGain, normalizedFrequencyHz, normalizedQ, filterType,
				"tx");
	}

	public void setInputPeq(DspChannel channel, int peqIndex, PeqFilterType filterType, double frequencyHz, double q,
			double gainDb, boolean bypass) throws IOException {
		requireInput(channel);
		checkInputPeqIndex(peqIndex);

		double normalizedFrequencyHz = DspCodecRegistry.peqFrequency()
				.rawToDouble(DspCodecRegistry.peqFrequency().doubleToRaw(frequencyHz));
		double normalizedQ = DspCodecRegistry.peqQ().rawToDouble(DspCodecRegistry.peqQ().doubleToRaw(q));
		double normalizedGain = DspCodecRegistry.peqGain().rawToDouble(DspCodecRegistry.peqGain().doubleToRaw(gainDb));

		requireClient().setInputPeq(inputNumber(channel), peqIndex, normalizedGain, normalizedFrequencyHz, normalizedQ,
				filterType, bypass);

		state.markInputPeqBand(channel, peqIndex, normalizedGain, normalizedFrequencyHz, normalizedQ, filterType,
				bypass, "tx");
	}

	public void setInputPeqGain(DspChannel channel, int peqIndex, double gainDb) throws IOException {
		requireInput(channel);
		checkInputPeqIndex(peqIndex);
		PeqBandState resolved = requireKnownInputPeqBand(channel, peqIndex);
		double normalizedGain = DspCodecRegistry.peqGain().rawToDouble(DspCodecRegistry.peqGain().doubleToRaw(gainDb));

		requireClient().setInputPeq(inputNumber(channel), peqIndex, normalizedGain, resolved.frequencyHz(),
				resolved.q(), resolved.filterType(), resolved.bypass());

		state.markInputPeqBand(channel, peqIndex, normalizedGain, resolved.frequencyHz(), resolved.q(),
				resolved.filterType(), resolved.bypass(), "tx");
	}

	public void setInputPeqFrequency(DspChannel channel, int peqIndex, double frequencyHz) throws IOException {
		requireInput(channel);
		checkInputPeqIndex(peqIndex);
		PeqBandState resolved = requireKnownInputPeqBand(channel, peqIndex);
		double normalizedFrequencyHz = DspCodecRegistry.peqFrequency()
				.rawToDouble(DspCodecRegistry.peqFrequency().doubleToRaw(frequencyHz));

		requireClient().setInputPeq(inputNumber(channel), peqIndex, resolved.gainDb(), normalizedFrequencyHz,
				resolved.q(), resolved.filterType(), resolved.bypass());

		state.markInputPeqBand(channel, peqIndex, resolved.gainDb(), normalizedFrequencyHz, resolved.q(),
				resolved.filterType(), resolved.bypass(), "tx");
	}

	public void setInputPeqQ(DspChannel channel, int peqIndex, double q) throws IOException {
		requireInput(channel);
		checkInputPeqIndex(peqIndex);
		PeqBandState resolved = requireKnownInputPeqBand(channel, peqIndex);
		double normalizedQ = DspCodecRegistry.peqQ().rawToDouble(DspCodecRegistry.peqQ().doubleToRaw(q));

		requireClient().setInputPeq(inputNumber(channel), peqIndex, resolved.gainDb(), resolved.frequencyHz(),
				normalizedQ, resolved.filterType(), resolved.bypass());

		state.markInputPeqBand(channel, peqIndex, resolved.gainDb(), resolved.frequencyHz(), normalizedQ,
				resolved.filterType(), resolved.bypass(), "tx");
	}

	public void setInputPeqType(DspChannel channel, int peqIndex, PeqFilterType filterType) throws IOException {
		requireInput(channel);
		checkInputPeqIndex(peqIndex);
		PeqBandState resolved = requireKnownInputPeqBand(channel, peqIndex);

		requireClient().setInputPeq(inputNumber(channel), peqIndex, resolved.gainDb(), resolved.frequencyHz(),
				resolved.q(), filterType, resolved.bypass());

		state.markInputPeqBand(channel, peqIndex, resolved.gainDb(), resolved.frequencyHz(), resolved.q(), filterType,
				resolved.bypass(), "tx");
	}

	public void setInputPeqBypass(DspChannel channel, int peqIndex, boolean bypass) throws IOException {
		requireInput(channel);
		checkInputPeqIndex(peqIndex);
		PeqBandState resolved = requireKnownInputPeqBand(channel, peqIndex);

		requireClient().setInputPeq(inputNumber(channel), peqIndex, resolved.gainDb(), resolved.frequencyHz(),
				resolved.q(), resolved.filterType(), bypass);

		state.markInputPeqBand(channel, peqIndex, resolved.gainDb(), resolved.frequencyHz(), resolved.q(),
				resolved.filterType(), bypass, "tx");
	}

	public void setInputGeq(DspChannel channel, int bandIndex, double gainDb) throws IOException {
		requireInput(channel);
		checkInputGeqBandIndex(bandIndex);

		double normalizedGain = DspCodecRegistry.peqGain().rawToDouble(DspCodecRegistry.peqGain().doubleToRaw(gainDb));
		double frequencyHz = requireKnownInputGeqFrequency(bandIndex);

		requireClient().setInputGeq(inputNumber(channel), bandIndex, normalizedGain);
		state.markInputGeqBand(channel, bandIndex, normalizedGain, frequencyHz, "tx");
	}

	public void setInputGate(DspChannel channel, double thresholdDb, double holdMs, double attackMs, double releaseMs)
			throws IOException {
		requireInput(channel);

		double normalizedThresholdDb = DspCodecRegistry
				.rawToDynamicsThresholdDb(DspCodecRegistry.dynamicsThresholdDbToRaw(thresholdDb, -90.0, 0.0));
		double normalizedHoldMs = DspCodecRegistry
				.rawToDynamicsTimeMs(DspCodecRegistry.dynamicsTimeMsToRaw(holdMs, 10.0, 999.0));
		double normalizedAttackMs = DspCodecRegistry
				.rawToDynamicsTimeMs(DspCodecRegistry.dynamicsTimeMsToRaw(attackMs, 1.0, 999.0));
		double normalizedReleaseMs = DspCodecRegistry
				.rawToDynamicsTimeMs(DspCodecRegistry.dynamicsTimeMsToRaw(releaseMs, 10.0, 3000.0));

		requireClient().setInputGate(inputNumber(channel), normalizedThresholdDb, normalizedHoldMs, normalizedAttackMs,
				normalizedReleaseMs);

		state.markInputGate(channel, normalizedThresholdDb, normalizedHoldMs, normalizedAttackMs, normalizedReleaseMs,
				"tx");
	}

	public void setInputGateThreshold(DspChannel channel, double thresholdDb) throws IOException {
		requireInput(channel);
		InputGateState resolved = requireKnownInputGate(channel);
		setInputGate(channel, thresholdDb, resolved.holdMs(), resolved.attackMs(), resolved.releaseMs());
	}

	public void setInputGateHold(DspChannel channel, double holdMs) throws IOException {
		requireInput(channel);
		InputGateState resolved = requireKnownInputGate(channel);
		setInputGate(channel, resolved.thresholdDb(), holdMs, resolved.attackMs(), resolved.releaseMs());
	}

	public void setInputGateAttack(DspChannel channel, double attackMs) throws IOException {
		requireInput(channel);
		InputGateState resolved = requireKnownInputGate(channel);
		setInputGate(channel, resolved.thresholdDb(), resolved.holdMs(), attackMs, resolved.releaseMs());
	}

	public void setInputGateRelease(DspChannel channel, double releaseMs) throws IOException {
		requireInput(channel);
		InputGateState resolved = requireKnownInputGate(channel);
		setInputGate(channel, resolved.thresholdDb(), resolved.holdMs(), resolved.attackMs(), releaseMs);
	}

	public void setMatrixRoute(DspChannel output, DspChannel input) throws IOException {
		requireOutputChannel(output);
		requireInput(input);
		requireClient().setMatrixRoute(output, input);
		state.markMatrixRoute(output, input);
	}

	public void setMatrixCrosspointGain(DspChannel output, DspChannel input, double db) throws IOException {
		requireOutputChannel(output);
		requireInput(input);

		double clamped = Math.max(-60.0, Math.min(0.0, db));
		requireClient().setMatrixCrosspointGain(output, input, clamped);
		state.markMatrixCrosspointGain(output, input, clamped);
	}

	public void setCrossoverHighPass(DspChannel channel, double frequencyHz, CrossoverSlope slope, boolean bypass)
			throws IOException {
		requireAnyChannel(channel);
		double normalizedFrequencyHz = DspCodecRegistry.peqFrequency()
				.rawToDouble(DspCodecRegistry.peqFrequency().doubleToRaw(frequencyHz));
		requireClient().setCrossoverHighPass(channel, normalizedFrequencyHz, slope, bypass);
		state.markCrossoverHighPass(channel, normalizedFrequencyHz, slope, bypass, "tx");
	}

	public void setCrossoverHighPassFrequency(DspChannel channel, double frequencyHz) throws IOException {
		requireAnyChannel(channel);
		CrossoverFilterState resolved = requireKnownHighPass(channel);
		double normalizedFrequencyHz = DspCodecRegistry.peqFrequency()
				.rawToDouble(DspCodecRegistry.peqFrequency().doubleToRaw(frequencyHz));
		requireClient().setCrossoverHighPass(channel, normalizedFrequencyHz, resolvedSlopeForWrite(resolved),
				resolved.bypass());
		state.markCrossoverHighPass(channel, normalizedFrequencyHz, resolved.slope(), resolved.bypass(), "tx");
	}

	public void setCrossoverHighPassSlope(DspChannel channel, CrossoverSlope slope) throws IOException {
		requireAnyChannel(channel);
		CrossoverFilterState resolved = requireKnownHighPass(channel);
		requireClient().setCrossoverHighPass(channel, resolved.frequencyHz(), slope, resolved.bypass());
		state.markCrossoverHighPass(channel, resolved.frequencyHz(), slope, resolved.bypass(), "tx");
	}

	public void setCrossoverHighPassBypass(DspChannel channel, boolean bypass) throws IOException {
		requireAnyChannel(channel);
		CrossoverFilterState resolved = requireKnownHighPass(channel);
		requireClient().setCrossoverHighPass(channel, resolved.frequencyHz(), resolvedSlopeForWrite(resolved), bypass);
		state.markCrossoverHighPass(channel, resolved.frequencyHz(), resolved.slope(), bypass, "tx");
	}

	public void setCrossoverLowPass(DspChannel channel, double frequencyHz, CrossoverSlope slope, boolean bypass)
			throws IOException {
		requireAnyChannel(channel);
		double normalizedFrequencyHz = DspCodecRegistry.peqFrequency()
				.rawToDouble(DspCodecRegistry.peqFrequency().doubleToRaw(frequencyHz));
		requireClient().setCrossoverLowPass(channel, normalizedFrequencyHz, slope, bypass);
		state.markCrossoverLowPass(channel, normalizedFrequencyHz, slope, bypass, "tx");
	}

	public void setCrossoverLowPassFrequency(DspChannel channel, double frequencyHz) throws IOException {
		requireAnyChannel(channel);
		CrossoverFilterState resolved = requireKnownLowPass(channel);
		double normalizedFrequencyHz = DspCodecRegistry.peqFrequency()
				.rawToDouble(DspCodecRegistry.peqFrequency().doubleToRaw(frequencyHz));
		requireClient().setCrossoverLowPass(channel, normalizedFrequencyHz, resolvedSlopeForWrite(resolved),
				resolved.bypass());
		state.markCrossoverLowPass(channel, normalizedFrequencyHz, resolved.slope(), resolved.bypass(), "tx");
	}

	public void setCrossoverLowPassSlope(DspChannel channel, CrossoverSlope slope) throws IOException {
		requireAnyChannel(channel);
		CrossoverFilterState resolved = requireKnownLowPass(channel);
		requireClient().setCrossoverLowPass(channel, resolved.frequencyHz(), slope, resolved.bypass());
		state.markCrossoverLowPass(channel, resolved.frequencyHz(), slope, resolved.bypass(), "tx");
	}

	public void setCrossoverLowPassBypass(DspChannel channel, boolean bypass) throws IOException {
		requireAnyChannel(channel);
		CrossoverFilterState resolved = requireKnownLowPass(channel);
		requireClient().setCrossoverLowPass(channel, resolved.frequencyHz(), resolvedSlopeForWrite(resolved), bypass);
		state.markCrossoverLowPass(channel, resolved.frequencyHz(), resolved.slope(), bypass, "tx");
	}

	public void setCompressor(DspChannel output, double thresholdDb, String ratioLabel, double attackMs,
			double releaseMs, double kneeDb) throws IOException {
		requireOutputChannel(output);

		int ratioRaw = DspProtocol.compressorRatioLabelToRaw(ratioLabel);
		String normalizedRatioLabel = library.compressorRatioName(ratioRaw);
		if (normalizedRatioLabel == null || normalizedRatioLabel.isBlank()) {
			normalizedRatioLabel = "raw=" + ratioRaw;
		}

		double normalizedAttackMs = DspCodecRegistry
				.rawToDynamicsTimeMs(DspCodecRegistry.dynamicsTimeMsToRaw(attackMs, 1.0, 999.0));
		double normalizedReleaseMs = DspCodecRegistry
				.rawToDynamicsTimeMs(DspCodecRegistry.dynamicsTimeMsToRaw(releaseMs, 10.0, 3000.0));
		double normalizedKneeDb = Math.max(0.0, Math.min(12.0, Math.round(kneeDb)));
		double normalizedThresholdDb = DspCodecRegistry
				.rawToDynamicsThresholdDb(DspCodecRegistry.dynamicsThresholdDbToRaw(thresholdDb, -90.0, 20.0));

		requireClient().setCompressor(output, normalizedRatioLabel, normalizedAttackMs, normalizedReleaseMs,
				normalizedKneeDb, normalizedThresholdDb);

		state.markCompressor(output, ratioRaw, normalizedRatioLabel, normalizedAttackMs, normalizedReleaseMs,
				normalizedKneeDb, normalizedThresholdDb, "tx");
	}

	public void setLimiter(DspChannel output, double thresholdDb, double attackMs, double releaseMs)
			throws IOException {
		requireOutputChannel(output);

		int unknownRaw = resolveLimiterUnknownRaw(output);
		double normalizedAttackMs = DspCodecRegistry
				.rawToDynamicsTimeMs(DspCodecRegistry.dynamicsTimeMsToRaw(attackMs, 1.0, 999.0));
		double normalizedReleaseMs = DspCodecRegistry
				.rawToDynamicsTimeMs(DspCodecRegistry.dynamicsTimeMsToRaw(releaseMs, 10.0, 3000.0));
		double normalizedThresholdDb = DspCodecRegistry
				.rawToDynamicsThresholdDb(DspCodecRegistry.dynamicsThresholdDbToRaw(thresholdDb, -90.0, 20.0));

		requireClient().setLimiter(output, normalizedAttackMs, normalizedReleaseMs, unknownRaw, normalizedThresholdDb);

		state.markLimiter(output, normalizedAttackMs, normalizedReleaseMs, unknownRaw, normalizedThresholdDb, "tx");
	}

	public void enableTestTone(double frequencyHz) throws IOException {
		int sourceRaw = 0x03;
		int sineRaw = DspProtocol.findNearestTestToneSineSelector(frequencyHz);

		requireClient().setTestToneSource(sourceRaw);
		requireClient().setTestToneSineFrequencyRaw(sineRaw);

		state.markTestToneSource(sourceRaw, DspProtocol.testToneSourceLabel(sourceRaw), "tx");
		state.markTestToneSineFrequency(sineRaw, DspProtocol.testToneSineFrequencyHz(sineRaw), "tx");
	}

	public void enableTestTone(DspChannel output, double frequencyHz, double levelDb) throws IOException {
		requireOutputChannel(output);
		enableTestTone(frequencyHz);
	}

	public void disableTestTone() throws IOException {
		requireClient().setTestToneOff();
		state.markTestToneSource(0x00, DspProtocol.testToneSourceLabel(0x00), "tx");
	}

	public void scanParameterBlocks() throws IOException {
		for (int i = 0x00; i <= 0x1C; i++) {
			readParameterBlock(i);
			sleepSilently(150);
		}
	}

	private DspClient requireClient() {
		if (client == null || !client.isConnected()) {
			throw new IllegalStateException("Not connected. Run 'connect' first.");
		}
		return client;
	}

	private void updateDeviceVersion(String version) {
		this.deviceVersion = version;
	}

	private void updateSystemInfo(byte[] payload) {
		this.lastSystemInfoPayload = payload == null ? null : Arrays.copyOf(payload, payload.length);
	}

	private static void sleepSilently(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private static void requireOutput(DspChannel channel) {
		if (channel == null || channel.index() < 4 || channel.index() > 11) {
			throw new IllegalArgumentException("PEQ write is only supported for output channels Out1..Out8");
		}
	}

	private static void requireOutputChannel(DspChannel channel) {
		if (channel == null || channel.index() < 4 || channel.index() > 11) {
			throw new IllegalArgumentException("Output channel required: Out1..Out8");
		}
	}

	private static void requireInput(DspChannel channel) {
		if (channel == null || channel.index() < 0 || channel.index() > 3) {
			throw new IllegalArgumentException("Input channel required: InA..InD");
		}
	}

	private static void requireAnyChannel(DspChannel channel) {
		if (channel == null || channel.index() < 0 || channel.index() > 11) {
			throw new IllegalArgumentException("Channel required: InA..InD or Out1..Out8");
		}
	}

	private static int outputNumber(DspChannel channel) {
		return channel.index() - 3;
	}

	private static int inputNumber(DspChannel channel) {
		return channel.index();
	}

	private PeqBandState requireKnownOutputPeqBand(DspChannel channel, int peqIndex) {
		if (peqIndex < 1 || peqIndex > 9) {
			throw new IllegalArgumentException("PEQ index must be in range 1..9");
		}

		PeqBandState existing = state.outputPeq(channel).band(peqIndex);
		if (existing == null || existing.gainDb() == null || existing.frequencyHz() == null || existing.q() == null) {
			throw new IllegalStateException(OUTPUT_PEQ_INCOMPLETE_MESSAGE);
		}

		PeqFilterType resolvedFilterType = existing.filterType();
		boolean typeConfirmed = existing.typeConfirmedFromDevice();
		String typeSource = existing.lastTypeUpdateSource();

		if (resolvedFilterType == null) {
			OutputPeqBandDefaults defaults = library.outputPeqDefault(peqIndex);
			if (defaults != null) {
				resolvedFilterType = defaults.filterType();
				typeConfirmed = false;
				typeSource = "library-default";
			}
		}

		if (resolvedFilterType == null) {
			throw new IllegalStateException(OUTPUT_PEQ_INCOMPLETE_MESSAGE);
		}

		PeqBandState resolved = new PeqBandState(peqIndex);
		resolved.updateGain(existing.gainDb(), existing.lastGainUpdateSource(), existing.gainConfirmedFromDevice());
		resolved.updateFrequency(existing.frequencyHz(), existing.lastFrequencyUpdateSource(),
				existing.frequencyConfirmedFromDevice());
		resolved.updateQ(existing.q(), existing.lastQUpdateSource(), existing.qConfirmedFromDevice());
		resolved.updateFilterType(resolvedFilterType, typeSource, typeConfirmed);

		return resolved;
	}

	private PeqBandState requireKnownInputPeqBand(DspChannel channel, int peqIndex) {
		checkInputPeqIndex(peqIndex);

		PeqBandState existing = state.inputPeq(channel).band(peqIndex);
		if (existing == null || existing.gainDb() == null || existing.frequencyHz() == null || existing.q() == null
				|| existing.filterType() == null || existing.bypass() == null) {
			throw new IllegalStateException(INPUT_PEQ_INCOMPLETE_MESSAGE);
		}
		return existing;
	}

	private CrossoverFilterState requireKnownHighPass(DspChannel channel) {
		CrossoverFilterState existing = state.crossover(channel).highPass();
		if (existing.frequencyHz() == null || existing.bypass() == null
				|| (!existing.bypass() && existing.slope() == null)) {
			throw new IllegalStateException(HIGH_PASS_INCOMPLETE_MESSAGE);
		}
		return existing;
	}

	private CrossoverFilterState requireKnownHighPassFrequency(DspChannel channel) {
		CrossoverFilterState existing = state.crossover(channel).highPass();
		if (existing.frequencyHz() == null) {
			throw new IllegalStateException(HIGH_PASS_INCOMPLETE_MESSAGE);
		}
		return existing;
	}

	private CrossoverFilterState requireKnownLowPass(DspChannel channel) {
		CrossoverFilterState existing = state.crossover(channel).lowPass();
		if (existing.frequencyHz() == null || existing.bypass() == null
				|| (!existing.bypass() && existing.slope() == null)) {
			throw new IllegalStateException(LOW_PASS_INCOMPLETE_MESSAGE);
		}
		return existing;
	}

	private CrossoverFilterState requireKnownLowPassFrequency(DspChannel channel) {
		CrossoverFilterState existing = state.crossover(channel).lowPass();
		if (existing.frequencyHz() == null) {
			throw new IllegalStateException(LOW_PASS_INCOMPLETE_MESSAGE);
		}
		return existing;
	}

	private static CrossoverSlope resolvedSlopeForWrite(CrossoverFilterState state) {
		if (Boolean.TRUE.equals(state.bypass())) {
			return state.slope() != null ? state.slope() : CrossoverSlope.LR_48;
		}
		if (state.slope() == null) {
			throw new IllegalStateException("Crossover slope is required when bypass is off");
		}
		return state.slope();
	}

	private static void checkInputPeqIndex(int peqIndex) {
		if (peqIndex < 1 || peqIndex > 8) {
			throw new IllegalArgumentException("Input PEQ index must be in range 1..8");
		}
	}

	private static void checkInputGeqBandIndex(int bandIndex) {
		if (bandIndex < 1 || bandIndex > 31) {
			throw new IllegalArgumentException(INPUT_GEQ_RANGE_MESSAGE);
		}
	}

	private InputGateState requireKnownInputGate(DspChannel channel) {
		InputGateState existing = state.inputGate(channel);
		if (existing == null || existing.thresholdDb() == null || existing.holdMs() == null
				|| existing.attackMs() == null || existing.releaseMs() == null) {
			throw new IllegalStateException(INPUT_GATE_INCOMPLETE_MESSAGE);
		}
		return existing;
	}

	private double requireKnownInputGeqFrequency(int bandIndex) {
		Double frequencyHz = library.inputGeqBandFrequency(bandIndex);
		if (frequencyHz == null) {
			throw new IllegalStateException("Input GEQ band frequency is unknown for band " + bandIndex);
		}
		return frequencyHz;
	}
	
	private int resolveLimiterUnknownRaw(DspChannel output) {
		Integer known = state.limiter(output) == null ? null : state.limiter(output).unknownValue();
		return known == null ? 0 : known;
	}

}
