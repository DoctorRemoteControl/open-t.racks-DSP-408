package de.drremote.dsp408.api;

public record RawTxDto(String payloadHex,
                       String frameHex,
                       int payloadLength,
                       int frameLength) {
}
