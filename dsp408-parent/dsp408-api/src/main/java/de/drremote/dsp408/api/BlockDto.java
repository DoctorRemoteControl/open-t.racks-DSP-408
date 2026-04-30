package de.drremote.dsp408.api;

public record BlockDto(String blockIndexHex,
                       Integer blockIndex,
                       boolean present,
                       String dataHex,
                       String dataAscii,
                       Integer dataLength) {
}
