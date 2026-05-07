package de.drremote.dsp408.api;

public record DspInstanceDto(String id,
                             String ip,
                             int port,
                             boolean defaultDevice,
                             boolean connected,
                             DeviceInfoDto deviceInfo) {
}
