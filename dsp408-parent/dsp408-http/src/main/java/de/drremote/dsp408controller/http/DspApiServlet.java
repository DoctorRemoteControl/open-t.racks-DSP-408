package de.drremote.dsp408controller.http;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.drremote.dsp408.api.DspService;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component(
        service = javax.servlet.Servlet.class,
        property = {
                HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN + "=/api/v1/*",
                HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT + "=(osgi.http.whiteboard.context.name=default)"
        }
)
public final class DspApiServlet extends HttpServlet {
    private static final Logger LOG = LoggerFactory.getLogger(DspApiServlet.class);

    private final ObjectMapper mapper = new ObjectMapper();

    @Reference
    private DspService dsp;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String path = normalizePath(req.getPathInfo());

            if (path.equals("/") || path.equals("/health")) {
                writeJson(resp, 200, Map.of(
                        "service", "dsp408-http",
                        "ok", true
                ));
                return;
            }

            if (path.equals("/help")) {
                writeJson(resp, 200, textResponse("help", dsp.helpText()));
                return;
            }

            if (path.equals("/openapi.json")) {
                try (InputStream in = DspApiServlet.class.getResourceAsStream("/openapi/dsp408-openapi.json")) {
                    if (in == null) {
                        sendError(resp, 404, "not_found", "OpenAPI resource not found.");
                        return;
                    }
                    resp.setStatus(200);
                    resp.setContentType("application/json");
                    resp.setCharacterEncoding("UTF-8");
                    in.transferTo(resp.getOutputStream());
                    return;
                }
            }

            if (path.equals("/state") || path.equals("/status")) {
                writeJson(resp, 200, stateResponse());
                return;
            }

            if (path.equals("/device") || path.equals("/deviceinfo") || path.equals("/info")) {
                writeJson(resp, 200, dsp.getDeviceInfo());
                return;
            }

            if (path.equals("/channels")) {
                writeJson(resp, 200, Map.of(
                        "connected", dsp.isConnected(),
                        "channels", dsp.getChannels()
                ));
                return;
            }

            if (path.startsWith("/channels/")) {
                String channelId = path.substring("/channels/".length());
                writeJson(resp, 200, Map.of(
                        "connected", dsp.isConnected(),
                        "channel", dsp.getChannel(channelId)
                ));
                return;
            }

            if (path.equals("/blocks")) {
                writeJson(resp, 200, Map.of(
                        "connected", dsp.isConnected(),
                        "cachedBlockIndices", dsp.getCachedBlockIndices()
                ));
                return;
            }

            if (path.startsWith("/blocks/")) {
                String blockIndex = path.substring("/blocks/".length());
                writeJson(resp, 200, Map.of(
                        "connected", dsp.isConnected(),
                        "block", dsp.getCachedBlock(blockIndex)
                ));
                return;
            }

            if (path.equals("/volume") || path.equals("/volume/status")) {
                writeJson(resp, 200, textResponse("volume status", dsp.executeVolumeRoom("!dsp volume status")));
                return;
            }

            if (path.equals("/volume/help")) {
                writeJson(resp, 200, textResponse("volume help", dsp.executeVolumeRoom("!dsp volume help")));
                return;
            }

            sendError(resp, 404, "not_found", "Unknown endpoint.");
        } catch (IllegalArgumentException e) {
            sendError(resp, 422, "unprocessable_entity", e.getMessage());
        } catch (Exception e) {
            LOG.error("HTTP GET failed", e);
            sendError(resp, 500, "internal_error", "Unexpected server error.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String path = normalizePath(req.getPathInfo());

            if (path.equals("/connection/connect")) {
                writeJson(resp, 200, actionResponse(dsp.connect()));
                return;
            }

            if (path.equals("/connection/reconnect")) {
                writeJson(resp, 200, actionResponse(dsp.reconnect()));
                return;
            }

            if (path.equals("/connection/disconnect")) {
                dsp.disconnect();
                writeJson(resp, 200, actionResponse("Disconnected."));
                return;
            }

            if (path.equals("/command")) {
                Map<String, Object> body = readJsonBody(req);
                String line = requireString(body, "line");
                String mode = stringValue(body.getOrDefault("mode", "shell"));
                boolean admin = !body.containsKey("admin") || toBoolean(body.get("admin"));
                String text = "matrix".equalsIgnoreCase(mode)
                        ? dsp.executeMatrix(line, admin)
                        : dsp.executeShell(line);
                writeJson(resp, 200, textResponse("command", text));
                return;
            }

            if (path.equals("/preset/load")) {
                Map<String, Object> body = readJsonBody(req);
                Object index = body.get("index");
                writeJson(resp, 200, index == null
                        ? dsp.loadPreset(requireString(body, "slot"))
                        : dsp.loadPreset(toInt(index)));
                return;
            }

            if (path.equals("/preset/name/read")) {
                Map<String, Object> body = readJsonBody(req);
                writeJson(resp, 200, dsp.readPresetName(requireInt(body, "index")));
                return;
            }

            if (path.equals("/delay-unit")) {
                Map<String, Object> body = readJsonBody(req);
                writeJson(resp, 200, dsp.setDelayUnit(requireString(body, "unit")));
                return;
            }

            if (path.equals("/meters/read")) {
                writeJson(resp, 200, dsp.requestRuntimeMeters());
                return;
            }

            if (path.equals("/matrix/route")) {
                Map<String, Object> body = readJsonBody(req);
                writeJson(resp, 200, dsp.setMatrixRoute(requireString(body, "output"), requireString(body, "input")));
                return;
            }

            if (path.equals("/matrix/crosspoint-gain")) {
                Map<String, Object> body = readJsonBody(req);
                writeJson(resp, 200, dsp.setMatrixCrosspointGain(
                        requireString(body, "output"),
                        requireString(body, "input"),
                        requireDouble(body, "db")
                ));
                return;
            }

            if (path.startsWith("/channels/") && path.endsWith("/gain")) {
                String channelId = path.substring("/channels/".length(), path.length() - "/gain".length());
                Map<String, Object> body = readJsonBody(req);
                double db = requireDouble(body, "db");
                writeJson(resp, 200, Map.of(
                        "ok", true,
                        "message", "Gain set.",
                        "channel", dsp.setGain(channelId, db)
                ));
                return;
            }

            if (path.startsWith("/channels/") && path.endsWith("/name")) {
                String channelId = path.substring("/channels/".length(), path.length() - "/name".length());
                Map<String, Object> body = readJsonBody(req);
                writeJson(resp, 200, dsp.setChannelName(channelId, requireString(body, "name")));
                return;
            }

            if (path.startsWith("/channels/") && path.endsWith("/phase")) {
                String channelId = path.substring("/channels/".length(), path.length() - "/phase".length());
                Map<String, Object> body = readJsonBody(req);
                writeJson(resp, 200, Map.of(
                        "ok", true,
                        "message", "Phase set.",
                        "channel", dsp.setPhase(channelId, requireBoolean(body, "inverted"))
                ));
                return;
            }

            if (path.startsWith("/channels/") && path.endsWith("/delay")) {
                String channelId = path.substring("/channels/".length(), path.length() - "/delay".length());
                Map<String, Object> body = readJsonBody(req);
                writeJson(resp, 200, Map.of(
                        "ok", true,
                        "message", "Delay set.",
                        "channel", dsp.setDelay(channelId, requireDouble(body, "ms"))
                ));
                return;
            }

            if (path.startsWith("/channels/") && path.contains("/crossover/")) {
                String[] parts = path.split("/");
                if (parts.length < 5) {
                    throw new IllegalArgumentException("Invalid crossover path.");
                }
                String channelId = parts[2];
                String filter = parts[4];
                String field = parts.length > 5 ? parts[5] : "";
                Map<String, Object> body = readJsonBody(req);
                boolean highPass = "hp".equalsIgnoreCase(filter) || "high-pass".equalsIgnoreCase(filter);

                if (field.isBlank()) {
                    double hz = requireDouble(body, "hz");
                    String slope = stringValue(body.get("slope"));
                    boolean bypass = requireBoolean(body, "bypass");
                    writeJson(resp, 200, highPass
                            ? dsp.setCrossoverHighPass(channelId, hz, slope, bypass)
                            : dsp.setCrossoverLowPass(channelId, hz, slope, bypass));
                    return;
                }
                if ("frequency".equalsIgnoreCase(field)) {
                    double hz = requireDouble(body, "hz");
                    writeJson(resp, 200, highPass
                            ? dsp.setCrossoverHighPassFrequency(channelId, hz)
                            : dsp.setCrossoverLowPassFrequency(channelId, hz));
                    return;
                }
                if ("slope".equalsIgnoreCase(field)) {
                    String slope = requireString(body, "slope");
                    writeJson(resp, 200, highPass
                            ? dsp.setCrossoverHighPassSlope(channelId, slope)
                            : dsp.setCrossoverLowPassSlope(channelId, slope));
                    return;
                }
                if ("bypass".equalsIgnoreCase(field)) {
                    boolean bypass = requireBoolean(body, "bypass");
                    writeJson(resp, 200, highPass
                            ? dsp.setCrossoverHighPassBypass(channelId, bypass)
                            : dsp.setCrossoverLowPassBypass(channelId, bypass));
                    return;
                }
            }

            if (path.startsWith("/channels/") && path.contains("/peq/") && !path.contains("/input-peq/")) {
                String[] parts = path.split("/");
                if (parts.length < 5) {
                    throw new IllegalArgumentException("Invalid PEQ path.");
                }
                String channelId = parts[2];
                int peqIndex = Integer.parseInt(parts[4]);
                String field = parts.length > 5 ? parts[5] : "";
                Map<String, Object> body = readJsonBody(req);
                if (field.isBlank()) {
                    writeJson(resp, 200, dsp.setOutputPeq(
                            channelId,
                            peqIndex,
                            requireString(body, "type"),
                            requireDouble(body, "hz"),
                            requireDouble(body, "q"),
                            requireDouble(body, "gainDb")
                    ));
                    return;
                }
                if ("q".equalsIgnoreCase(field) && parts.length == 6) {
                    writeJson(resp, 200, dsp.setOutputPeqQ(channelId, peqIndex, requireDouble(body, "q")));
                    return;
                }
                if ("gain".equalsIgnoreCase(field) && parts.length == 6) {
                    writeJson(resp, 200, dsp.setOutputPeqGain(channelId, peqIndex, requireDouble(body, "gainDb")));
                    return;
                }
                if ("type".equalsIgnoreCase(field)) {
                    writeJson(resp, 200, dsp.setOutputPeqType(channelId, peqIndex, requireString(body, "type")));
                    return;
                }
            }

            if (path.startsWith("/channels/") && path.contains("/peq/") && path.endsWith("/frequency")) {
                String[] parts = path.split("/");
                if (parts.length < 6) {
                    throw new IllegalArgumentException("Invalid PEQ path.");
                }
                String channelId = parts[2];
                int peqIndex = Integer.parseInt(parts[4]);
                Map<String, Object> body = readJsonBody(req);
                double hz = requireDouble(body, "hz");
                writeJson(resp, 200, dsp.setOutputPeqFrequency(channelId, peqIndex, hz));
                return;
            }

            if (path.startsWith("/channels/") && path.contains("/peq/") && path.endsWith("/q/raw")) {
                String[] parts = path.split("/");
                if (parts.length < 7) {
                    throw new IllegalArgumentException("Invalid PEQ path.");
                }
                String channelId = parts[2];
                int peqIndex = Integer.parseInt(parts[4]);
                Map<String, Object> body = readJsonBody(req);
                int raw = requireInt(body, "raw");
                writeJson(resp, 200, dsp.setOutputPeqQRaw(channelId, peqIndex, raw));
                return;
            }

            if (path.startsWith("/channels/") && path.contains("/peq/") && path.endsWith("/gain/code")) {
                String[] parts = path.split("/");
                if (parts.length < 7) {
                    throw new IllegalArgumentException("Invalid PEQ path.");
                }
                String channelId = parts[2];
                int peqIndex = Integer.parseInt(parts[4]);
                Map<String, Object> body = readJsonBody(req);
                int code = requireInt(body, "code");
                writeJson(resp, 200, dsp.setOutputPeqGainCode(channelId, peqIndex, code));
                return;
            }

            if (path.startsWith("/channels/") && path.contains("/input-peq/")) {
                String[] parts = path.split("/");
                if (parts.length < 5) {
                    throw new IllegalArgumentException("Invalid input PEQ path.");
                }
                String channelId = parts[2];
                int peqIndex = Integer.parseInt(parts[4]);
                String field = parts.length > 5 ? parts[5] : "";
                Map<String, Object> body = readJsonBody(req);
                if (field.isBlank()) {
                    writeJson(resp, 200, dsp.setInputPeq(
                            channelId,
                            peqIndex,
                            requireString(body, "type"),
                            requireDouble(body, "hz"),
                            requireDouble(body, "q"),
                            requireDouble(body, "gainDb"),
                            requireBoolean(body, "bypass")
                    ));
                    return;
                }
                if ("frequency".equalsIgnoreCase(field)) {
                    writeJson(resp, 200, dsp.setInputPeqFrequency(channelId, peqIndex, requireDouble(body, "hz")));
                    return;
                }
                if ("q".equalsIgnoreCase(field)) {
                    writeJson(resp, 200, dsp.setInputPeqQ(channelId, peqIndex, requireDouble(body, "q")));
                    return;
                }
                if ("gain".equalsIgnoreCase(field)) {
                    writeJson(resp, 200, dsp.setInputPeqGain(channelId, peqIndex, requireDouble(body, "gainDb")));
                    return;
                }
                if ("type".equalsIgnoreCase(field)) {
                    writeJson(resp, 200, dsp.setInputPeqType(channelId, peqIndex, requireString(body, "type")));
                    return;
                }
                if ("bypass".equalsIgnoreCase(field)) {
                    writeJson(resp, 200, dsp.setInputPeqBypass(channelId, peqIndex, requireBoolean(body, "bypass")));
                    return;
                }
            }

            if (path.startsWith("/channels/") && path.contains("/geq/") && path.endsWith("/gain")) {
                String[] parts = path.split("/");
                if (parts.length < 6) {
                    throw new IllegalArgumentException("Invalid GEQ path.");
                }
                Map<String, Object> body = readJsonBody(req);
                writeJson(resp, 200, dsp.setInputGeq(parts[2], Integer.parseInt(parts[4]), requireDouble(body, "gainDb")));
                return;
            }

            if (path.startsWith("/channels/") && path.contains("/gate")) {
                String[] parts = path.split("/");
                String channelId = parts[2];
                String field = parts.length > 4 ? parts[4] : "";
                Map<String, Object> body = readJsonBody(req);
                if (field.isBlank()) {
                    writeJson(resp, 200, dsp.setInputGate(
                            channelId,
                            requireDouble(body, "thresholdDb"),
                            requireDouble(body, "holdMs"),
                            requireDouble(body, "attackMs"),
                            requireDouble(body, "releaseMs")
                    ));
                    return;
                }
                if ("threshold".equalsIgnoreCase(field)) {
                    writeJson(resp, 200, dsp.setInputGateThreshold(channelId, requireDouble(body, "thresholdDb")));
                    return;
                }
                if ("hold".equalsIgnoreCase(field)) {
                    writeJson(resp, 200, dsp.setInputGateHold(channelId, requireDouble(body, "holdMs")));
                    return;
                }
                if ("attack".equalsIgnoreCase(field)) {
                    writeJson(resp, 200, dsp.setInputGateAttack(channelId, requireDouble(body, "attackMs")));
                    return;
                }
                if ("release".equalsIgnoreCase(field)) {
                    writeJson(resp, 200, dsp.setInputGateRelease(channelId, requireDouble(body, "releaseMs")));
                    return;
                }
            }

            if (path.startsWith("/channels/") && path.endsWith("/compressor")) {
                String channelId = path.substring("/channels/".length(), path.length() - "/compressor".length());
                Map<String, Object> body = readJsonBody(req);
                writeJson(resp, 200, dsp.setCompressor(
                        channelId,
                        requireDouble(body, "thresholdDb"),
                        requireString(body, "ratio"),
                        requireDouble(body, "attackMs"),
                        requireDouble(body, "releaseMs"),
                        requireDouble(body, "kneeDb")
                ));
                return;
            }

            if (path.startsWith("/channels/") && path.endsWith("/limiter")) {
                String channelId = path.substring("/channels/".length(), path.length() - "/limiter".length());
                Map<String, Object> body = readJsonBody(req);
                writeJson(resp, 200, dsp.setLimiter(
                        channelId,
                        requireDouble(body, "thresholdDb"),
                        requireDouble(body, "attackMs"),
                        requireDouble(body, "releaseMs")
                ));
                return;
            }

            if (path.startsWith("/channels/") && path.endsWith("/mute")) {
                String channelId = path.substring("/channels/".length(), path.length() - "/mute".length());
                writeJson(resp, 200, Map.of(
                        "ok", true,
                        "message", "Muted.",
                        "channel", dsp.mute(channelId)
                ));
                return;
            }

            if (path.startsWith("/channels/") && path.endsWith("/unmute")) {
                String channelId = path.substring("/channels/".length(), path.length() - "/unmute".length());
                writeJson(resp, 200, Map.of(
                        "ok", true,
                        "message", "Unmuted.",
                        "channel", dsp.unmute(channelId)
                ));
                return;
            }

            if (path.equals("/test-tone/source")) {
                Map<String, Object> body = readJsonBody(req);
                writeJson(resp, 200, dsp.setTestToneSource(requireString(body, "source")));
                return;
            }

            if (path.equals("/test-tone/sine")) {
                Map<String, Object> body = readJsonBody(req);
                Object raw = body.get("raw");
                writeJson(resp, 200, raw == null
                        ? dsp.setTestToneSineFrequency(requireDouble(body, "hz"))
                        : dsp.setTestToneSineFrequencyRaw(toInt(raw)));
                return;
            }

            if (path.equals("/test-tone/off")) {
                writeJson(resp, 200, dsp.disableTestTone());
                return;
            }

            if (path.startsWith("/blocks/") && path.endsWith("/read")) {
                String blockIndex = path.substring("/blocks/".length(), path.length() - "/read".length());
                writeJson(resp, 200, Map.of(
                        "connected", dsp.isConnected(),
                        "block", dsp.readBlock(blockIndex)
                ));
                return;
            }

            if (path.equals("/blocks/scan") || path.equals("/scanblocks") || path.equals("/refresh")) {
                writeJson(resp, 200, dsp.scanBlocks());
                return;
            }

            if (path.equals("/device/login")) {
                Map<String, Object> body = readJsonBody(req);
                Object pinValue = body.get("pin");
                if (pinValue == null) {
                    sendError(resp, 400, "bad_request", "Missing pin.");
                    return;
                }
                dsp.loginPin(String.valueOf(pinValue).trim());
                writeJson(resp, 200, actionResponse("Login PIN sent."));
                return;
            }

            if (path.equals("/raw")) {
                Map<String, Object> body = readJsonBody(req);
                Object payloadHex = body.get("payloadHex");
                if (payloadHex == null) {
                    sendError(resp, 400, "bad_request", "Missing payloadHex.");
                    return;
                }
                writeJson(resp, 200, dsp.sendRawHex(String.valueOf(payloadHex)));
                return;
            }

            if (path.equals("/volume/up")) {
                writeJson(resp, 200, textResponse("volume up", dsp.executeVolumeRoom("!dsp volume up")));
                return;
            }

            if (path.equals("/volume/down")) {
                writeJson(resp, 200, textResponse("volume down", dsp.executeVolumeRoom("!dsp volume down")));
                return;
            }

            if (path.equals("/volume/mute")) {
                writeJson(resp, 200, textResponse("volume mute", dsp.executeVolumeRoom("!dsp volume mute")));
                return;
            }

            if (path.equals("/volume/unmute")) {
                writeJson(resp, 200, textResponse("volume unmute", dsp.executeVolumeRoom("!dsp volume unmute")));
                return;
            }

            if (path.equals("/volume/refresh")) {
                writeJson(resp, 200, textResponse("volume refresh", dsp.executeVolumeRoom("!dsp volume refresh")));
                return;
            }

            if (path.equals("/volume/set")) {
                Map<String, Object> body = readJsonBody(req);
                double db = requireDouble(body, "db");
                writeJson(resp, 200, textResponse(
                        "volume set",
                        dsp.executeVolumeRoom("!dsp volume set " + db)
                ));
                return;
            }

            if (path.equals("/volume/step")) {
                Map<String, Object> body = readJsonBody(req);
                double db = requireDouble(body, "db");
                String signed = db >= 0 ? "+" + db : String.valueOf(db);
                writeJson(resp, 200, textResponse(
                        "volume step",
                        dsp.executeVolumeRoom("!dsp volume step " + signed)
                ));
                return;
            }

            sendError(resp, 404, "not_found", "Unknown endpoint.");
        } catch (IllegalArgumentException e) {
            sendError(resp, 422, "unprocessable_entity", e.getMessage());
        } catch (Exception e) {
            LOG.error("HTTP POST failed", e);
            sendError(resp, 500, "internal_error", "Unexpected server error.");
        }
    }

    private Map<String, Object> stateResponse() {
        return Map.of(
                "connected", dsp.isConnected(),
                "deviceInfo", dsp.getDeviceInfo(),
                "channels", dsp.getChannels(),
                "cachedBlockIndices", dsp.getCachedBlockIndices()
        );
    }

    private Map<String, Object> actionResponse(String message) {
        return Map.of(
                "ok", true,
                "message", message
        );
    }

    private Map<String, Object> textResponse(String command, String text) {
        return Map.of(
                "ok", true,
                "command", command,
                "connected", dsp.isConnected(),
                "text", text == null ? "" : text
        );
    }

    private void writeJson(HttpServletResponse resp, int status, Object body) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        mapper.writeValue(resp.getOutputStream(), body);
    }

    private void sendError(HttpServletResponse resp, int status, String error, String message) throws IOException {
        writeJson(resp, status, Map.of(
                "error", error,
                "message", message,
                "status", status
        ));
    }

    private Map<String, Object> readJsonBody(HttpServletRequest req) throws IOException {
        if (req.getContentLengthLong() == 0) {
            return Collections.emptyMap();
        }
        return mapper.readValue(req.getInputStream(), Map.class);
    }

    private double requireDouble(Map<String, Object> body, String field) {
        Object value = body.get(field);
        if (value == null) {
            throw new IllegalArgumentException("Missing " + field + ".");
        }
        return toDouble(value);
    }

    private int requireInt(Map<String, Object> body, String field) {
        Object value = body.get(field);
        if (value == null) {
            throw new IllegalArgumentException("Missing " + field + ".");
        }
        return toInt(value);
    }

    private String requireString(Map<String, Object> body, String field) {
        Object value = body.get(field);
        if (value == null || String.valueOf(value).isBlank()) {
            throw new IllegalArgumentException("Missing " + field + ".");
        }
        return String.valueOf(value).trim();
    }

    private boolean requireBoolean(Map<String, Object> body, String field) {
        Object value = body.get(field);
        if (value == null) {
            throw new IllegalArgumentException("Missing " + field + ".");
        }
        return toBoolean(value);
    }

    private int toInt(Object value) {
        if (value instanceof Number n) {
            return n.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private boolean toBoolean(Object value) {
        if (value instanceof Boolean b) {
            return b;
        }
        String text = String.valueOf(value).trim().toLowerCase();
        return switch (text) {
            case "true", "1", "yes", "on" -> true;
            case "false", "0", "no", "off" -> false;
            default -> throw new IllegalArgumentException("Expected boolean value.");
        };
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private double toDouble(Object value) {
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        return Double.parseDouble(String.valueOf(value));
    }

    private String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        return path.trim();
    }
}
