package de.drremote.dsp408controller.core.decode;

import java.util.Locale;
import java.util.function.Consumer;

import de.drremote.dsp408controller.core.library.DspLibrary;
import de.drremote.dsp408controller.core.library.MatrixRouteLocation;
import de.drremote.dsp408controller.core.protocol.DspChannel;
import de.drremote.dsp408controller.core.state.DspState;

public final class MatrixRoutingBlockDecoder implements BlockDecoder {
    private final DspState state;
    private final Consumer<String> log;
    private final DspLibrary library;

    public MatrixRoutingBlockDecoder(DspState state, Consumer<String> log, DspLibrary library) {
        this.state = state;
        this.log = log;
        this.library = library;
    }

    @Override
    public void decode(int blockIndex, byte[] data) {
        if (data == null) {
            return;
        }

        for (DspChannel output : DspChannel.values()) {
            if (output.index() < 4) {
                continue;
            }

            MatrixRouteLocation location = library.matrixRouteLocation(output);
            if (location == null || location.blockIndex() != blockIndex) {
                continue;
            }

            if (data.length <= location.dataOffset()) {
                continue;
            }

            int raw = data[location.dataOffset()] & 0xFF;
            DspChannel input = decodeMask(raw);
            if (input == null) {
                if (log != null) {
                    log.accept(String.format(
                            Locale.ROOT,
                            "MATRIX ROUTE RX %s -> unknown raw mask 0x%02X (block %02X offset %02X)",
                            output.displayName(),
                            raw,
                            blockIndex,
                            location.dataOffset()
                    ));
                }
                continue;
            }

            state.markMatrixRoute(output, input);

            if (log != null) {
                log.accept(String.format(
                        Locale.ROOT,
                        "MATRIX ROUTE RX %s <- %s (block %02X offset %02X raw=0x%02X)",
                        output.displayName(),
                        input.displayName(),
                        blockIndex,
                        location.dataOffset(),
                        raw
                ));
            }
        }
    }

    private static DspChannel decodeMask(int raw) {
        return switch (raw) {
            case 0x01 -> DspChannel.IN_A;
            case 0x02 -> DspChannel.IN_B;
            case 0x04 -> DspChannel.IN_C;
            case 0x08 -> DspChannel.IN_D;
            default -> null;
        };
    }
}
