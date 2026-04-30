package de.drremote.dsp408controller.runtime;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.drremote.dsp408.api.BlockDto;
import de.drremote.dsp408.api.ChannelDto;
import de.drremote.dsp408.api.DeviceInfoDto;
import de.drremote.dsp408.api.DspService;
import de.drremote.dsp408.api.RawTxDto;
import de.drremote.dsp408.api.StateDto;
import de.drremote.dsp408controller.command.DspCommandProcessor;
import de.drremote.dsp408controller.core.net.DspConnectionConfig;
import de.drremote.dsp408controller.core.net.DspFrameCodec;
import de.drremote.dsp408controller.core.protocol.CrossoverSlope;
import de.drremote.dsp408controller.core.protocol.DspChannel;
import de.drremote.dsp408controller.core.protocol.DspProtocol;
import de.drremote.dsp408controller.core.protocol.PeqFilterType;
import de.drremote.dsp408controller.core.service.DspController;
import de.drremote.dsp408controller.core.state.ChannelState;
import de.drremote.dsp408controller.core.state.CrossoverFilterState;
import de.drremote.dsp408controller.core.state.InputGateState;
import de.drremote.dsp408controller.core.state.LimiterState;
import de.drremote.dsp408controller.core.state.PeqBandState;
import de.drremote.dsp408controller.util.Hex;

@Component(
        service = DspService.class,
        immediate = true,
        configurationPid = "de.drremote.dsp408controller"
)
@Designate(ocd = Dsp408Configuration.class)
public final class DspRuntimeComponent implements DspService {
    private static final Logger LOG = LoggerFactory.getLogger(DspRuntimeComponent.class);

    private final DspController controller = new DspController();
    private final VolumeRoomHandler volumeRoomHandler = new VolumeRoomHandler(this);

    private final DspCommandProcessor shellProcessor =
            new DspCommandProcessor(
                    controller,
                    this::currentConfig,
                    this::log,
                    DspCommandProcessor.CommandMode.CLI
            );

    private final DspCommandProcessor matrixProcessor =
            new DspCommandProcessor(
                    controller,
                    this::currentConfig,
                    this::log,
                    DspCommandProcessor.CommandMode.MATRIX
            );

    private volatile Dsp408Configuration configuration;

    @Activate
    void activate(Dsp408Configuration configuration) {
        this.configuration = configuration;
        LOG.info("DSP408 activated ({}:{})", configuration.dsp_ip(), configuration.dsp_port());

        if (configuration.auto_connect()) {
            try {
                connect();
            } catch (Exception e) {
                LOG.error("Auto-connect failed", e);
            }
        }
    }

    @Modified
    void modified(Dsp408Configuration configuration) {
        boolean reconnectNeeded =
                this.configuration != null
                        && controller.isConnected()
                        && connectionChanged(this.configuration, configuration);

        this.configuration = configuration;

        if (reconnectNeeded) {
            try {
                reconnect();
            } catch (Exception e) {
                LOG.error("Reconnect after config change failed", e);
            }
        }
    }

    @Deactivate
    void deactivate() {
        try {
            controller.close();
        } catch (Exception e) {
            LOG.warn("Error while closing DSP controller", e);
        }
    }

    @Override
    public synchronized String helpText() {
        return executeWithCapture(shellProcessor, "help", true);
    }

    @Override
    public synchronized String executeShell(String line) throws Exception {
        String normalized = line == null ? "" : line.trim();
        if (normalized.isEmpty()) {
            return "";
        }

        String lower = normalized.toLowerCase(Locale.ROOT);

        if (lower.equals("connect") || lower.equals("dsp connect") || lower.equals("!dsp connect")) {
            return connect();
        }
        if (lower.equals("disconnect") || lower.equals("dsp disconnect") || lower.equals("!dsp disconnect")) {
            disconnect();
            return "Disconnected.";
        }
        if (lower.equals("reconnect") || lower.equals("dsp reconnect") || lower.equals("!dsp reconnect")) {
            return reconnect();
        }

        return executeWithCapture(shellProcessor, normalized, true);
    }

    @Override
    public synchronized String executeMatrix(String line, boolean isAdmin) throws Exception {
        String normalized = line == null ? "" : line.trim();
        if (normalized.isEmpty()) {
            return "";
        }
        return executeWithCapture(matrixProcessor, normalized, isAdmin);
    }

    @Override
    public synchronized String executeVolumeRoom(String line) throws Exception {
        return volumeRoomHandler.handleVolumeRoom(line);
    }

    @Override
    public synchronized String connect() throws Exception {
        DspConnectionConfig config = currentConfig();
        controller.connect(config, this::log);

        if (configuration != null && configuration.auto_read_on_connect()) {
            controller.scanParameterBlocks();
        }

        return "Connected to " + config.ip() + ":" + config.port();
    }

    @Override
    public synchronized String reconnect() throws Exception {
        controller.close();
        return connect();
    }

    @Override
    public synchronized void disconnect() {
        controller.close();
    }

    @Override
    public synchronized boolean isConnected() {
        return controller.isConnected();
    }

    @Override
    public synchronized DeviceInfoDto getDeviceInfo() {
        return toDeviceInfoDto();
    }

    @Override
    public synchronized List<ChannelDto> getChannels() {
        return controller.state().allChannels().stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public synchronized ChannelDto getChannel(String channelId) {
        DspChannel channel = DspChannel.parse(channelId);
        return toDto(controller.state().channel(channel));
    }

    @Override
    public synchronized ChannelDto setGain(String channelId, double db) throws Exception {
        DspChannel channel = DspChannel.parse(channelId);
        controller.setGain(channel, db);
        Integer blockIndex = gainBlockIndex(channel);
        if (blockIndex != null) {
            readBlockWithWait(blockIndex);
        }
        return toDto(controller.state().channel(channel));
    }

    @Override
    public synchronized ChannelDto mute(String channelId) throws Exception {
        DspChannel channel = DspChannel.parse(channelId);
        controller.mute(channel);
        readBlockWithWait(0x1C);
        return toDto(controller.state().channel(channel));
    }

    @Override
    public synchronized ChannelDto unmute(String channelId) throws Exception {
        DspChannel channel = DspChannel.parse(channelId);
        controller.unmute(channel);
        readBlockWithWait(0x1C);
        return toDto(controller.state().channel(channel));
    }

    @Override
    public synchronized List<String> getCachedBlockIndices() {
        return controller.state().cachedBlockIndices().stream()
                .map(this::formatIndexHex)
                .toList();
    }

    @Override
    public synchronized BlockDto getCachedBlock(String blockIndexHex) {
        int index = parseHexIndex(blockIndexHex);
        byte[] data = controller.state().getBlock(index);
        return toBlockDto(index, data);
    }

    @Override
    public synchronized BlockDto readBlock(String blockIndexHex) throws Exception {
        int index = parseHexIndex(blockIndexHex);
        controller.readParameterBlock(index);
        // Give the device a short moment to respond and the cache to update.
        Thread.sleep(250);
        byte[] data = controller.state().getBlock(index);
        return toBlockDto(index, data);
    }

    @Override
    public synchronized StateDto scanBlocks() throws Exception {
        controller.scanParameterBlocks();
        return toStateDto();
    }

    @Override
    public synchronized RawTxDto sendRawHex(String payloadHex) throws Exception {
        byte[] payload = Hex.parseFlexible(payloadHex);
        byte[] frame = DspFrameCodec.encode(payload);
        controller.sendRaw(payload);
        return new RawTxDto(
                Hex.format(payload),
                Hex.format(frame),
                payload.length,
                frame.length
        );
    }

    @Override
    public synchronized void loginPin(String pin) throws Exception {
        controller.login(pin);
    }

    @Override
    public synchronized RawTxDto loadPreset(String slotLabel) throws Exception {
        controller.loadPreset(slotLabel);
        return buildRawTxDto(DspProtocol.loadPreset(slotLabel));
    }

    @Override
    public synchronized RawTxDto loadPreset(int presetIndex) throws Exception {
        controller.loadPreset(presetIndex);
        return buildRawTxDto(DspProtocol.loadPreset(presetIndex));
    }

    @Override
    public synchronized RawTxDto readPresetName(int presetIndex) throws Exception {
        controller.readPresetName(presetIndex);
        return buildRawTxDto(DspProtocol.readPresetName(presetIndex));
    }

    @Override
    public synchronized RawTxDto setDelayUnit(String unit) throws Exception {
        controller.setDelayUnit(unit);
        return buildRawTxDto(DspProtocol.buildDelayUnit(unit));
    }

    @Override
    public synchronized RawTxDto setChannelName(String channelId, String name) throws Exception {
        DspChannel channel = DspChannel.parse(channelId);
        controller.setChannelName(channel, name);
        return buildRawTxDto(DspProtocol.buildChannelName(channel, name));
    }

    @Override
    public synchronized ChannelDto setPhase(String channelId, boolean inverted) throws Exception {
        DspChannel channel = DspChannel.parse(channelId);
        controller.setPhase(channel, inverted);
        return toDto(controller.state().channel(channel));
    }

    @Override
    public synchronized ChannelDto setDelay(String channelId, double ms) throws Exception {
        DspChannel channel = DspChannel.parse(channelId);
        controller.setDelay(channel, ms);
        return toDto(controller.state().channel(channel));
    }

    @Override
    public synchronized RawTxDto setMatrixRoute(String outputId, String inputId) throws Exception {
        DspChannel output = DspChannel.parse(outputId);
        DspChannel input = DspChannel.parse(inputId);
        controller.setMatrixRoute(output, input);
        return buildRawTxDto(DspProtocol.buildMatrixRoute(output, input));
    }

    @Override
    public synchronized RawTxDto setMatrixCrosspointGain(String outputId, String inputId, double db) throws Exception {
        DspChannel output = DspChannel.parse(outputId);
        DspChannel input = DspChannel.parse(inputId);
        controller.setMatrixCrosspointGain(output, input, db);
        return buildRawTxDto(DspProtocol.buildMatrixCrosspointGain(output, input, db));
    }

    @Override
    public synchronized RawTxDto setCrossoverHighPass(String channelId, double frequencyHz, String slope, boolean bypass)
            throws Exception {
        DspChannel channel = DspChannel.parse(channelId);
        CrossoverSlope parsedSlope = bypass && (slope == null || slope.isBlank()) ? CrossoverSlope.LR_48 : CrossoverSlope.parse(slope);
        controller.setCrossoverHighPass(channel, frequencyHz, parsedSlope, bypass);
        return buildRawTxDto(DspProtocol.buildCrossoverHighPass(channel, frequencyHz, parsedSlope, bypass));
    }

    @Override
    public synchronized RawTxDto setCrossoverHighPassFrequency(String channelId, double frequencyHz) throws Exception {
        DspChannel channel = DspChannel.parse(channelId);
        CrossoverFilterState existing = controller.state().crossover(channel).highPass();
        controller.setCrossoverHighPassFrequency(channel, frequencyHz);
        CrossoverSlope slope = resolvedSlope(existing);
        return buildRawTxDto(DspProtocol.buildCrossoverHighPass(channel, frequencyHz, slope, Boolean.TRUE.equals(existing.bypass())));
    }

    @Override
    public synchronized RawTxDto setCrossoverHighPassSlope(String channelId, String slope) throws Exception {
        DspChannel channel = DspChannel.parse(channelId);
        CrossoverFilterState existing = controller.state().crossover(channel).highPass();
        CrossoverSlope parsedSlope = CrossoverSlope.parse(slope);
        controller.setCrossoverHighPassSlope(channel, parsedSlope);
        return buildRawTxDto(DspProtocol.buildCrossoverHighPass(channel, existing.frequencyHz(), parsedSlope, Boolean.TRUE.equals(existing.bypass())));
    }

    @Override
    public synchronized RawTxDto setCrossoverHighPassBypass(String channelId, boolean bypass) throws Exception {
        DspChannel channel = DspChannel.parse(channelId);
        CrossoverFilterState existing = controller.state().crossover(channel).highPass();
        controller.setCrossoverHighPassBypass(channel, bypass);
        return buildRawTxDto(DspProtocol.buildCrossoverHighPass(channel, existing.frequencyHz(), resolvedSlope(existing), bypass));
    }

    @Override
    public synchronized RawTxDto setCrossoverLowPass(String channelId, double frequencyHz, String slope, boolean bypass)
            throws Exception {
        DspChannel channel = DspChannel.parse(channelId);
        CrossoverSlope parsedSlope = bypass && (slope == null || slope.isBlank()) ? CrossoverSlope.LR_48 : CrossoverSlope.parse(slope);
        controller.setCrossoverLowPass(channel, frequencyHz, parsedSlope, bypass);
        return buildRawTxDto(DspProtocol.buildCrossoverLowPass(channel, frequencyHz, parsedSlope, bypass));
    }

    @Override
    public synchronized RawTxDto setCrossoverLowPassFrequency(String channelId, double frequencyHz) throws Exception {
        DspChannel channel = DspChannel.parse(channelId);
        CrossoverFilterState existing = controller.state().crossover(channel).lowPass();
        controller.setCrossoverLowPassFrequency(channel, frequencyHz);
        return buildRawTxDto(DspProtocol.buildCrossoverLowPass(channel, frequencyHz, resolvedSlope(existing), Boolean.TRUE.equals(existing.bypass())));
    }

    @Override
    public synchronized RawTxDto setCrossoverLowPassSlope(String channelId, String slope) throws Exception {
        DspChannel channel = DspChannel.parse(channelId);
        CrossoverFilterState existing = controller.state().crossover(channel).lowPass();
        CrossoverSlope parsedSlope = CrossoverSlope.parse(slope);
        controller.setCrossoverLowPassSlope(channel, parsedSlope);
        return buildRawTxDto(DspProtocol.buildCrossoverLowPass(channel, existing.frequencyHz(), parsedSlope, Boolean.TRUE.equals(existing.bypass())));
    }

    @Override
    public synchronized RawTxDto setCrossoverLowPassBypass(String channelId, boolean bypass) throws Exception {
        DspChannel channel = DspChannel.parse(channelId);
        CrossoverFilterState existing = controller.state().crossover(channel).lowPass();
        controller.setCrossoverLowPassBypass(channel, bypass);
        return buildRawTxDto(DspProtocol.buildCrossoverLowPass(channel, existing.frequencyHz(), resolvedSlope(existing), bypass));
    }

    @Override
    public synchronized RawTxDto setOutputPeq(String channelId, int peqIndex, String filterType, double frequencyHz,
                                              double q, double gainDb) throws Exception {
        DspChannel channel = DspChannel.parse(channelId);
        PeqFilterType type = PeqFilterType.parse(filterType);
        controller.setOutputPeq(channel, peqIndex, type, frequencyHz, q, gainDb);
        return buildRawTxDto(DspProtocol.buildOutputPeq(outputNumber(channel), peqIndex, gainDb, frequencyHz, q, type));
    }

    @Override
    public synchronized RawTxDto setOutputPeqFrequency(String channelId, int peqIndex, double hz) throws Exception {
        DspChannel channel = DspChannel.parse(channelId);
        controller.setOutputPeqFrequency(channel, peqIndex, hz);
        return buildOutputPeqRawTxDto(channel, peqIndex);
    }

    @Override
    public synchronized RawTxDto setOutputPeqQ(String channelId, int peqIndex, double q) throws Exception {
        DspChannel channel = DspChannel.parse(channelId);
        controller.setOutputPeqQ(channel, peqIndex, q);
        return buildOutputPeqRawTxDto(channel, peqIndex);
    }

    @Override
    public synchronized RawTxDto setOutputPeqQRaw(String channelId, int peqIndex, int raw) throws Exception {
        DspChannel channel = DspChannel.parse(channelId);
        controller.setOutputPeqQRaw(channel, peqIndex, raw);
        return buildOutputPeqRawTxDto(channel, peqIndex);
    }

    @Override
    public synchronized RawTxDto setOutputPeqGain(String channelId, int peqIndex, double gainDb) throws Exception {
        DspChannel channel = DspChannel.parse(channelId);
        controller.setOutputPeqGain(channel, peqIndex, gainDb);
        return buildOutputPeqRawTxDto(channel, peqIndex);
    }

    @Override
    public synchronized RawTxDto setOutputPeqGainCode(String channelId, int peqIndex, int code) throws Exception {
        DspChannel channel = DspChannel.parse(channelId);
        controller.setOutputPeqGainCode(channel, peqIndex, code);
        return buildOutputPeqRawTxDto(channel, peqIndex);
    }

    @Override
    public synchronized RawTxDto setOutputPeqType(String channelId, int peqIndex, String filterType) throws Exception {
        DspChannel channel = DspChannel.parse(channelId);
        controller.setOutputPeqType(channel, peqIndex, PeqFilterType.parse(filterType));
        return buildOutputPeqRawTxDto(channel, peqIndex);
    }

    @Override
    public synchronized RawTxDto setInputPeq(String channelId, int peqIndex, String filterType, double frequencyHz,
                                             double q, double gainDb, boolean bypass) throws Exception {
        DspChannel channel = DspChannel.parse(channelId);
        PeqFilterType type = PeqFilterType.parse(filterType);
        controller.setInputPeq(channel, peqIndex, type, frequencyHz, q, gainDb, bypass);
        return buildRawTxDto(DspProtocol.buildInputPeq(inputNumber(channel), peqIndex, gainDb, frequencyHz, q, type, bypass));
    }

    @Override
    public synchronized RawTxDto setInputPeqFrequency(String channelId, int peqIndex, double frequencyHz) throws Exception {
        DspChannel channel = DspChannel.parse(channelId);
        controller.setInputPeqFrequency(channel, peqIndex, frequencyHz);
        return buildInputPeqRawTxDto(channel, peqIndex);
    }

    @Override
    public synchronized RawTxDto setInputPeqQ(String channelId, int peqIndex, double q) throws Exception {
        DspChannel channel = DspChannel.parse(channelId);
        controller.setInputPeqQ(channel, peqIndex, q);
        return buildInputPeqRawTxDto(channel, peqIndex);
    }

    @Override
    public synchronized RawTxDto setInputPeqGain(String channelId, int peqIndex, double gainDb) throws Exception {
        DspChannel channel = DspChannel.parse(channelId);
        controller.setInputPeqGain(channel, peqIndex, gainDb);
        return buildInputPeqRawTxDto(channel, peqIndex);
    }

    @Override
    public synchronized RawTxDto setInputPeqType(String channelId, int peqIndex, String filterType) throws Exception {
        DspChannel channel = DspChannel.parse(channelId);
        controller.setInputPeqType(channel, peqIndex, PeqFilterType.parse(filterType));
        return buildInputPeqRawTxDto(channel, peqIndex);
    }

    @Override
    public synchronized RawTxDto setInputPeqBypass(String channelId, int peqIndex, boolean bypass) throws Exception {
        DspChannel channel = DspChannel.parse(channelId);
        controller.setInputPeqBypass(channel, peqIndex, bypass);
        return buildInputPeqRawTxDto(channel, peqIndex);
    }

    @Override
    public synchronized RawTxDto setInputGeq(String channelId, int bandIndex, double gainDb) throws Exception {
        DspChannel channel = DspChannel.parse(channelId);
        controller.setInputGeq(channel, bandIndex, gainDb);
        return buildRawTxDto(DspProtocol.buildInputGeq(inputNumber(channel), bandIndex, gainDb));
    }

    @Override
    public synchronized RawTxDto setInputGate(String channelId, double thresholdDb, double holdMs, double attackMs,
                                              double releaseMs) throws Exception {
        DspChannel channel = DspChannel.parse(channelId);
        controller.setInputGate(channel, thresholdDb, holdMs, attackMs, releaseMs);
        return buildRawTxDto(DspProtocol.buildInputGate(inputNumber(channel), thresholdDb, holdMs, attackMs, releaseMs));
    }

    @Override
    public synchronized RawTxDto setInputGateThreshold(String channelId, double thresholdDb) throws Exception {
        DspChannel channel = DspChannel.parse(channelId);
        InputGateState gate = controller.state().inputGate(channel);
        controller.setInputGateThreshold(channel, thresholdDb);
        return buildRawTxDto(DspProtocol.buildInputGate(inputNumber(channel), thresholdDb, gate.holdMs(), gate.attackMs(), gate.releaseMs()));
    }

    @Override
    public synchronized RawTxDto setInputGateHold(String channelId, double holdMs) throws Exception {
        DspChannel channel = DspChannel.parse(channelId);
        InputGateState gate = controller.state().inputGate(channel);
        controller.setInputGateHold(channel, holdMs);
        return buildRawTxDto(DspProtocol.buildInputGate(inputNumber(channel), gate.thresholdDb(), holdMs, gate.attackMs(), gate.releaseMs()));
    }

    @Override
    public synchronized RawTxDto setInputGateAttack(String channelId, double attackMs) throws Exception {
        DspChannel channel = DspChannel.parse(channelId);
        InputGateState gate = controller.state().inputGate(channel);
        controller.setInputGateAttack(channel, attackMs);
        return buildRawTxDto(DspProtocol.buildInputGate(inputNumber(channel), gate.thresholdDb(), gate.holdMs(), attackMs, gate.releaseMs()));
    }

    @Override
    public synchronized RawTxDto setInputGateRelease(String channelId, double releaseMs) throws Exception {
        DspChannel channel = DspChannel.parse(channelId);
        InputGateState gate = controller.state().inputGate(channel);
        controller.setInputGateRelease(channel, releaseMs);
        return buildRawTxDto(DspProtocol.buildInputGate(inputNumber(channel), gate.thresholdDb(), gate.holdMs(), gate.attackMs(), releaseMs));
    }

    @Override
    public synchronized RawTxDto setCompressor(String outputId, double thresholdDb, String ratioLabel, double attackMs,
                                               double releaseMs, double kneeDb) throws Exception {
        DspChannel output = DspChannel.parse(outputId);
        controller.setCompressor(output, thresholdDb, ratioLabel, attackMs, releaseMs, kneeDb);
        return buildRawTxDto(DspProtocol.buildCompressor(output, ratioLabel, attackMs, releaseMs, kneeDb, thresholdDb));
    }

    @Override
    public synchronized RawTxDto setLimiter(String outputId, double thresholdDb, double attackMs, double releaseMs)
            throws Exception {
        DspChannel output = DspChannel.parse(outputId);
        LimiterState limiter = controller.state().limiter(output);
        int unknownRaw = limiter == null || limiter.unknownValue() == null ? 0 : limiter.unknownValue();
        controller.setLimiter(output, thresholdDb, attackMs, releaseMs);
        return buildRawTxDto(DspProtocol.buildLimiter(output, attackMs, releaseMs, unknownRaw, thresholdDb));
    }

    @Override
    public synchronized RawTxDto setTestToneSource(String source) throws Exception {
        int sourceRaw = switch (source == null ? "" : source.trim().toLowerCase(Locale.ROOT).replace("_", "").replace("-", "")) {
            case "analoginput", "analog", "input", "off" -> 0;
            case "pinknoise", "pink" -> 1;
            case "whitenoise", "white" -> 2;
            case "sine" -> 3;
            default -> throw new IllegalArgumentException("Unknown test tone source: " + source);
        };
        if (sourceRaw == 0) {
            controller.disableTestTone();
        } else {
            controller.sendRaw(DspProtocol.buildTestToneSource(sourceRaw));
            controller.state().markTestToneSource(sourceRaw, DspProtocol.testToneSourceLabel(sourceRaw), "tx");
        }
        return buildRawTxDto(DspProtocol.buildTestToneSource(sourceRaw));
    }

    @Override
    public synchronized RawTxDto setTestToneSineFrequency(double hz) throws Exception {
        controller.enableTestTone(hz);
        return buildRawTxDto(DspProtocol.buildTestToneSineFrequencyHz(hz));
    }

    @Override
    public synchronized RawTxDto setTestToneSineFrequencyRaw(int selectorRaw) throws Exception {
        controller.sendRaw(DspProtocol.buildTestToneSineFrequencyRaw(selectorRaw));
        controller.state().markTestToneSource(0x03, DspProtocol.testToneSourceLabel(0x03), "tx");
        controller.state().markTestToneSineFrequency(selectorRaw, DspProtocol.testToneSineFrequencyHz(selectorRaw), "tx");
        return buildRawTxDto(DspProtocol.buildTestToneSineFrequencyRaw(selectorRaw));
    }

    @Override
    public synchronized RawTxDto disableTestTone() throws Exception {
        controller.disableTestTone();
        return buildRawTxDto(DspProtocol.buildTestToneOff());
    }

    @Override
    public synchronized StateDto requestRuntimeMeters() throws Exception {
        controller.requestRuntimeMeters();
        Thread.sleep(250);
        return toStateDto();
    }

    public synchronized <T> T withController(ControllerCallback<T> callback) throws Exception {
        return callback.execute(controller);
    }

    @FunctionalInterface
    public interface ControllerCallback<T> {
        T execute(DspController controller) throws Exception;
    }

    private DspConnectionConfig currentConfig() {
        Dsp408Configuration cfg = configuration;
        if (cfg == null) {
            return DspConnectionConfig.defaultConfig("192.168.0.166", 9761);
        }
        return DspConnectionConfig.defaultConfig(cfg.dsp_ip(), cfg.dsp_port());
    }

    double currentVolumeStepDb() {
        Dsp408Configuration cfg = configuration;
        if (cfg == null) {
            return 1.0;
        }
        double step = cfg.volume_step_db();
        return step > 0.0 ? step : 1.0;
    }

    private void log(String message) {
        LOG.info("[DSP408] {}", message);
    }

    private ChannelDto toDto(ChannelState state) {
        if (state == null) {
            return null;
        }
        DspChannel channel = state.channel();
        return new ChannelDto(
                toChannelId(channel),
                channel.displayName(),
                channel.index(),
                state.gainDb(),
                state.muted(),
                state.gainConfirmedFromDevice(),
                state.muteConfirmedFromDevice(),
                state.gainDirty(),
                state.muteDirty(),
                state.lastGainUpdateSource(),
                state.lastMuteUpdateSource()
        );
    }

    private DeviceInfoDto toDeviceInfoDto() {
        String version = controller.deviceVersion();
        byte[] sysinfo = controller.lastSystemInfoPayload();
        if (version == null && sysinfo == null) {
            return new DeviceInfoDto(null, null, null, null);
        }
        String sysinfoHex = sysinfo == null ? null : Hex.format(sysinfo);
        String sysinfoAscii = sysinfo == null ? null : Hex.ascii(sysinfo);
        Integer sysinfoLength = sysinfo == null ? null : sysinfo.length;
        return new DeviceInfoDto(version, sysinfoHex, sysinfoAscii, sysinfoLength);
    }

    private StateDto toStateDto() {
        return new StateDto(
                controller.isConnected(),
                toDeviceInfoDto(),
                getChannels(),
                getCachedBlockIndices()
        );
    }

    private RawTxDto buildRawTxDto(byte[] payload) {
        byte[] frame = DspFrameCodec.encode(payload);
        return new RawTxDto(
                Hex.format(payload),
                Hex.format(frame),
                payload.length,
                frame.length
        );
    }

    private BlockDto toBlockDto(int index, byte[] data) {
        boolean present = data != null;
        String dataHex = data == null ? null : Hex.format(data);
        String dataAscii = data == null ? null : Hex.ascii(data);
        Integer length = data == null ? null : data.length;
        return new BlockDto(formatIndexHex(index), index & 0xFF, present, dataHex, dataAscii, length);
    }

    private String formatIndexHex(int index) {
        return Hex.byteToHex(index);
    }

    private int parseHexIndex(String value) {
        String cleaned = value == null ? "" : value.trim();
        if (cleaned.startsWith("0x") || cleaned.startsWith("0X")) {
            cleaned = cleaned.substring(2);
        }
        if (cleaned.isEmpty()) {
            throw new IllegalArgumentException("Block index is missing");
        }
        if (!cleaned.matches("(?i)[0-9a-f]{1,2}")) {
            throw new IllegalArgumentException("Block index must be a hex value in range 00..1C");
        }
        int index = Integer.parseInt(cleaned, 16);
        if (index < 0x00 || index > 0x1C) {
            throw new IllegalArgumentException("Block index must be in range 00..1C");
        }
        return index;
    }

    private String toChannelId(DspChannel channel) {
        return switch (channel) {
            case IN_A -> "ina";
            case IN_B -> "inb";
            case IN_C -> "inc";
            case IN_D -> "ind";
            case OUT_1 -> "out1";
            case OUT_2 -> "out2";
            case OUT_3 -> "out3";
            case OUT_4 -> "out4";
            case OUT_5 -> "out5";
            case OUT_6 -> "out6";
            case OUT_7 -> "out7";
            case OUT_8 -> "out8";
        };
    }

    private Integer gainBlockIndex(DspChannel channel) {
        return switch (channel) {
            case IN_A -> 0x02;
            case IN_B -> 0x05;
            case IN_C -> 0x08;
            case IN_D -> 0x0B;
            case OUT_1 -> 0x0D;
            case OUT_2 -> 0x0F;
            case OUT_3 -> 0x11;
            case OUT_4 -> 0x13;
            case OUT_5 -> 0x15;
            case OUT_6 -> 0x17;
            case OUT_7 -> 0x19;
            case OUT_8 -> 0x1C;
        };
    }

    private static int outputNumber(DspChannel channel) {
        return channel.index() - 3;
    }

    private static int inputNumber(DspChannel channel) {
        return channel.index();
    }

    private static CrossoverSlope resolvedSlope(CrossoverFilterState state) {
        return state.slope() == null ? CrossoverSlope.LR_48 : state.slope();
    }

    private RawTxDto buildOutputPeqRawTxDto(DspChannel channel, int peqIndex) {
        PeqBandState band = controller.state().outputPeq(channel).band(peqIndex);
        if (band == null
                || band.gainDb() == null
                || band.frequencyHz() == null
                || band.q() == null
                || band.filterType() == null) {
            throw new IllegalStateException("Output PEQ state is incomplete for " + channel.displayName() + " PEQ" + peqIndex);
        }

        byte[] payload = DspProtocol.buildOutputPeq(
                outputNumber(channel),
                peqIndex,
                band.gainDb(),
                band.frequencyHz(),
                band.q(),
                band.filterType()
        );
        return buildRawTxDto(payload);
    }

    private RawTxDto buildInputPeqRawTxDto(DspChannel channel, int peqIndex) {
        PeqBandState band = controller.state().inputPeq(channel).band(peqIndex);
        if (band == null
                || band.gainDb() == null
                || band.frequencyHz() == null
                || band.q() == null
                || band.filterType() == null
                || band.bypass() == null) {
            throw new IllegalStateException("Input PEQ state is incomplete for " + channel.displayName() + " PEQ" + peqIndex);
        }

        byte[] payload = DspProtocol.buildInputPeq(
                inputNumber(channel),
                peqIndex,
                band.gainDb(),
                band.frequencyHz(),
                band.q(),
                band.filterType(),
                band.bypass()
        );
        return buildRawTxDto(payload);
    }

    private void readBlockWithWait(int blockIndex) throws Exception {
        controller.readParameterBlock(blockIndex);
        Thread.sleep(500);
    }

    private String executeWithCapture(DspCommandProcessor template, String line, boolean isAdmin) {
        List<String> lines = new CopyOnWriteArrayList<>();
        DspCommandProcessor processor = new DspCommandProcessor(
                controller,
                this::currentConfig,
                lines::add,
                template == shellProcessor
                        ? DspCommandProcessor.CommandMode.CLI
                        : DspCommandProcessor.CommandMode.MATRIX
        );

        String prefixed = line == null ? "" : line.trim();
        if (!prefixed.isEmpty() && !prefixed.toLowerCase(Locale.ROOT).startsWith("!dsp")) {
            prefixed = "dsp " + prefixed;
        }

        processor.process(prefixed, isAdmin);
        return String.join(System.lineSeparator(), lines);
    }

    private static boolean connectionChanged(Dsp408Configuration oldCfg, Dsp408Configuration newCfg) {
        return !oldCfg.dsp_ip().equals(newCfg.dsp_ip())
                || oldCfg.dsp_port() != newCfg.dsp_port();
    }
}
