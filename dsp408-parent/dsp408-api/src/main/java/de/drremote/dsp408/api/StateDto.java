package de.drremote.dsp408.api;

import java.util.List;

public record StateDto(boolean connected,
                       DeviceInfoDto deviceInfo,
                       List<ChannelDto> channels,
                       List<String> cachedBlockIndices) {
}
