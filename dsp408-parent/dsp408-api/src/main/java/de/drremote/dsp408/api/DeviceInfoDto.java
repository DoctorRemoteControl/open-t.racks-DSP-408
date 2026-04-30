package de.drremote.dsp408.api;

public record DeviceInfoDto(String deviceVersion,
                            String systemInfoHex,
                            String systemInfoAscii,
                            Integer systemInfoLength) {
}
