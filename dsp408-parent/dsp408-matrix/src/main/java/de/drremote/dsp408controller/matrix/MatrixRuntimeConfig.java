package de.drremote.dsp408controller.matrix;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

public final class MatrixRuntimeConfig {
    public final boolean enabled;
    public final String matrixUrl;
    public final String accessToken;
    public final String controlRoomId;
    public final String volumeRoomId;
    public final String machineRoomId;
    public final Set<String> allowedUsers;
    public final Set<String> adminUsers;
    public final Set<String> machineUsers;
    public final int syncTimeoutMs;
    public final int reconnectDelayMs;
    public final boolean connectDspOnStart;

    private MatrixRuntimeConfig(
            boolean enabled,
            String matrixUrl,
            String accessToken,
            String controlRoomId,
            String volumeRoomId,
            String machineRoomId,
            Set<String> allowedUsers,
            Set<String> adminUsers,
            Set<String> machineUsers,
            int syncTimeoutMs,
            int reconnectDelayMs,
            boolean connectDspOnStart
    ) {
        this.enabled = enabled;
        this.matrixUrl = blankToEmpty(matrixUrl);
        this.accessToken = blankToEmpty(accessToken);
        this.controlRoomId = blankToEmpty(controlRoomId);
        this.volumeRoomId = blankToEmpty(volumeRoomId);
        this.machineRoomId = blankToEmpty(machineRoomId);
        this.allowedUsers = allowedUsers;
        this.adminUsers = adminUsers;
        this.machineUsers = machineUsers;
        this.syncTimeoutMs = syncTimeoutMs > 0 ? syncTimeoutMs : 30000;
        this.reconnectDelayMs = reconnectDelayMs > 0 ? reconnectDelayMs : 3000;
        this.connectDspOnStart = connectDspOnStart;
    }

    public static MatrixRuntimeConfig from(MatrixBotConfiguration cfg) {
        String controlRoomId = blankToEmpty(cfg.control_room_id());
        if (controlRoomId.isBlank()) {
            controlRoomId = blankToEmpty(cfg.admin_room_id());
        }
        return new MatrixRuntimeConfig(
                cfg.enabled(),
                cfg.matrix_url(),
                cfg.access_token(),
                controlRoomId,
                cfg.volume_room_id(),
                cfg.machine_room_id(),
                normalize(cfg.allowed_users()),
                normalize(cfg.admin_users()),
                normalize(cfg.machine_users()),
                cfg.sync_timeout_ms(),
                cfg.reconnect_delay_ms(),
                cfg.connect_dsp_on_start()
        );
    }

    public boolean isValid() {
        return !matrixUrl.isBlank()
                && !accessToken.isBlank()
                && (!controlRoomId.isBlank() || !volumeRoomId.isBlank() || !machineRoomId.isBlank());
    }

    public boolean isUserAllowed(String userId) {
        if (allowedUsers.isEmpty() && adminUsers.isEmpty()) {
            return false;
        }
        return allowedUsers.contains(userId) || adminUsers.contains(userId);
    }

    public boolean isAdmin(String userId) {
        return adminUsers.contains(userId);
    }

    public boolean isMachineUser(String userId) {
        return machineUsers.contains(userId) || adminUsers.contains(userId);
    }

    private static Set<String> normalize(String[] values) {
        if (values == null || values.length == 0) {
            return Set.of();
        }
        return Arrays.stream(values)
                .filter(v -> v != null && !v.isBlank())
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static String blankToEmpty(String s) {
        return s == null ? "" : s.trim();
    }
}
