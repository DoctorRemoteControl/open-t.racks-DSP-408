# dsp408-core

Protocol, state and command core for the t.racks / Thomann DSP 408.

## Bundle

- Maven artifact: `de.drremote:dsp408-core`
- OSGi symbolic name: `de.drremote.dsp408.core`
- Bundle name: `DSP408 Core`

## Purpose

This bundle is the protocol engine. It knows how to build DSP408 payloads, encode/decode frames, connect to the DSP over TCP, parse parameter block dumps and maintain the in-memory DSP state.

## Important Packages

- `core.protocol` - channel enums, PEQ/crossover enums and payload builders.
- `core.net` - TCP client, connection config and frame codec.
- `core.decode` - block and meter payload decoders.
- `core.library` - loader for `DspLib-408.json` read layouts.
- `core.state` - mutable DSP state model.
- `core.service` - `DspController`, the high-level core facade.
- `command` - text command processor used by shell/runtime compatibility paths.
- `util` - hex and state formatting helpers.

## DSP Library

The bundled protocol library is:

`src/main/resources/dsp/DspLib-408.json`

It documents observed commands, value mappings and parameter block read locations. The loader uses this file as the source of truth for decoded read layouts.

## Main Entry Point

`DspController` is the main Java facade. It supports:

- connect, disconnect, handshake and keepalive
- block scan and runtime meter polling
- gain, mute, phase, delay and channel name writes
- output PEQ, input PEQ, input GEQ
- crossover, gate, compressor and limiter
- matrix route and crosspoint gain
- test tone source/frequency
- raw payload sending

## Build

From `dsp408-parent`:

```bash
mvn -pl dsp408-core -am test
```

## Notes

Some field-level writes require known existing state because the original DSP protocol writes complete parameter structures. In those cases, run a block scan before modifying a single field.

Still-undecoded functions such as preset storing, network IP, device ID, linking and copy-channel are not implemented here.
