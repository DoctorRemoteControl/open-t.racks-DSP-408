package de.drremote.dsp408controller.core.decode;

public interface BlockDecoder {
    void decode(int blockIndex, byte[] data);
}