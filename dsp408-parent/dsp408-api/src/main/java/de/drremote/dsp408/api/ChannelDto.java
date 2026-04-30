package de.drremote.dsp408.api;

public record ChannelDto(String id,
                         String displayName,
                         int index,
                         Double gainDb,
                         Boolean muted,
                         boolean gainConfirmedFromDevice,
                         boolean muteConfirmedFromDevice,
                         boolean gainDirty,
                         boolean muteDirty,
                         String lastGainUpdateSource,
                         String lastMuteUpdateSource) {
}
