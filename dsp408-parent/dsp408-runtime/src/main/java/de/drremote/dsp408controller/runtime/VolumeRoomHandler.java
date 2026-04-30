package de.drremote.dsp408controller.runtime;

import java.util.Locale;

import de.drremote.dsp408controller.core.protocol.DspChannel;
import de.drremote.dsp408controller.core.service.DspController;
import de.drremote.dsp408controller.core.state.ChannelState;
import de.drremote.dsp408controller.util.DspTextFormatter;

public final class VolumeRoomHandler {
    private static final DspChannel[] INPUT_CHANNELS = {
            DspChannel.IN_A,
            DspChannel.IN_B,
            DspChannel.IN_C,
            DspChannel.IN_D
    };

    private final DspRuntimeComponent runtime;

    public VolumeRoomHandler(DspRuntimeComponent runtime) {
        this.runtime = runtime;
    }

    public String handleVolumeRoom(String body) throws Exception {
        String raw = body == null ? "" : body.trim();
        if (raw.isEmpty()) {
            return "";
        }

        String lower = raw.toLowerCase(Locale.ROOT);
        if (!lower.startsWith("!dsp")) {
            return "";
        }

        String[] parts = raw.split("\\s+");
        if (parts.length < 2 || !"volume".equalsIgnoreCase(parts[1])) {
            return "Use: !dsp volume <command>";
        }

        String cmd = parts.length > 2 ? parts[2].toLowerCase(Locale.ROOT) : "";
        String arg = parts.length > 3 ? parts[3] : "";

        if (cmd.isEmpty()) {
            return "Use: !dsp volume <command>";
        }

        return runtime.withController(controller -> handleVolumeRoomInternal(controller, cmd, arg));
    }

    private String handleVolumeRoomInternal(DspController controller, String cmd, String arg) throws Exception {
        if (cmd.equals("help") || cmd.equals("?")) {
            double step = runtime.currentVolumeStepDb();
            return """
                    Volume room (InA-InD grouped)

                    Commands:
                      !dsp volume up          -> +%.2f dB
                      !dsp volume down        -> -%.2f dB
                      !dsp volume step +1     -> +1.0 dB
                      !dsp volume step -1     -> -1.0 dB
                      !dsp volume step +2.5   -> +2.5 dB
                      !dsp volume set -10     -> set InA-InD to -10.0 dB
                      !dsp volume set 0       -> set InA-InD to 0.0 dB
                      !dsp volume mute        -> mute InA-InD
                      !dsp volume unmute      -> unmute InA-InD
                      !dsp volume status      -> show InA-InD
                      !dsp volume refresh     -> refresh current DSP values
                    """.formatted(step, step).trim();
        }

        ensureConnected();

        if (cmd.equals("status")) {
            return formatInputStatus(controller);
        }

        if (cmd.equals("refresh")) {
            controller.scanParameterBlocks();
            return formatInputStatus(controller);
        }

        if (cmd.equals("mute")) {
            setMuteInputs(controller, true);
            sleepSilently(200);
            controller.scanParameterBlocks();
            return "InA-InD muted" + System.lineSeparator() + formatInputStatus(controller);
        }

        if (cmd.equals("unmute")) {
            setMuteInputs(controller, false);
            sleepSilently(200);
            controller.scanParameterBlocks();
            return "InA-InD unmuted" + System.lineSeparator() + formatInputStatus(controller);
        }

        if (cmd.equals("up")) {
            ensureKnownInputGains(controller);
            double step = runtime.currentVolumeStepDb();
            applyDeltaToInputs(controller, step);
            sleepSilently(200);
            controller.scanParameterBlocks();
            return "InA-InD: +" + formatDb(step) + " dB"
                    + System.lineSeparator()
                    + formatInputStatus(controller);
        }

        if (cmd.equals("down")) {
            ensureKnownInputGains(controller);
            double step = runtime.currentVolumeStepDb();
            applyDeltaToInputs(controller, -step);
            sleepSilently(200);
            controller.scanParameterBlocks();
            return "InA-InD: -" + formatDb(step) + " dB"
                    + System.lineSeparator()
                    + formatInputStatus(controller);
        }

        if (cmd.equals("set")) {
            String valueText = arg == null ? "" : arg.trim();
            if (!valueText.matches("[+-]?\\d+(\\.\\d+)?")) {
                return "Usage: !dsp volume set <db>, e.g. set -10 or set 0";
            }

            double targetDb = Double.parseDouble(valueText);
            for (DspChannel channel : INPUT_CHANNELS) {
                controller.setGain(channel, targetDb);
                sleepSilently(120);
            }

            sleepSilently(200);
            controller.scanParameterBlocks();

            return "InA-InD set to " + formatSignedDb(targetDb) + " dB"
                    + System.lineSeparator()
                    + formatInputStatus(controller);
        }

        if (cmd.equals("step")) {
            String valueText = arg == null ? "" : arg.trim();
            if (!isSignedNumber(valueText)) {
                return "Usage: !dsp volume step <+/-db>, e.g. step +1 or step -2.5";
            }

            ensureKnownInputGains(controller);
            double delta = Double.parseDouble(valueText);
            applyDeltaToInputs(controller, delta);
            sleepSilently(200);
            controller.scanParameterBlocks();

            return "InA-InD changed by " + formatSignedDb(delta) + " dB"
                    + System.lineSeparator()
                    + formatInputStatus(controller);
        }

        return """
                Unknown volume command.
                Use: !dsp volume help
                """.trim();
    }

    private void ensureConnected() {
        if (!runtime.isConnected()) {
            throw new IllegalStateException("DSP is not connected.");
        }
    }

    private void ensureKnownInputGains(DspController controller) throws Exception {
        boolean missing = false;
        for (DspChannel channel : INPUT_CHANNELS) {
            if (controller.state().channel(channel).gainDb() == null) {
                missing = true;
                break;
            }
        }

        if (missing) {
            controller.scanParameterBlocks();
            sleepSilently(300);
        }

        for (DspChannel channel : INPUT_CHANNELS) {
            if (controller.state().channel(channel).gainDb() == null) {
                throw new IllegalStateException(
                        "No known gain value for " + channel.displayName() + ". Refresh from DSP failed.");
            }
        }
    }

    private void applyDeltaToInputs(DspController controller, double deltaDb) throws Exception {
        for (DspChannel channel : INPUT_CHANNELS) {
            ChannelState state = controller.state().channel(channel);
            Double current = state.gainDb();
            if (current == null) {
                throw new IllegalStateException(
                        "No known gain value for " + channel.displayName() + ". Run 'refresh' first.");
            }
            controller.setGain(channel, current + deltaDb);
            sleepSilently(120);
        }
    }

    private void setMuteInputs(DspController controller, boolean muted) throws Exception {
        for (DspChannel channel : INPUT_CHANNELS) {
            if (muted) {
                controller.mute(channel);
            } else {
                controller.unmute(channel);
            }
            sleepSilently(120);
        }
    }

    private String formatInputStatus(DspController controller) {
        StringBuilder sb = new StringBuilder();
        for (DspChannel channel : INPUT_CHANNELS) {
            if (sb.length() > 0) {
                sb.append(System.lineSeparator());
            }
            sb.append(DspTextFormatter.formatChannelState(controller.state(), channel));
        }
        return sb.toString();
    }

    private static boolean isSignedNumber(String text) {
        return text.matches("[+-]?\\d+(\\.\\d+)?");
    }

    private static String formatDb(double value) {
        return String.format(Locale.ROOT, "%.2f", Math.abs(value));
    }

    private static String formatSignedDb(double value) {
        return String.format(Locale.ROOT, "%+.2f", value);
    }

    private static void sleepSilently(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
