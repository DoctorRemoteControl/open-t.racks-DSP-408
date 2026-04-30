package de.drremote.dsp408controller.core.codec;

public interface ValueCodec {
    double rawToDouble(int raw);
    int doubleToRaw(double value);
}