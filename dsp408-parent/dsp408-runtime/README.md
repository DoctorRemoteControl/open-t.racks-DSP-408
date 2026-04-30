# dsp408-runtime

OSGi Declarative Services runtime implementation of `DspService`.

## Bundle

- Maven artifact: `de.drremote:dsp408-runtime`
- OSGi symbolic name: `de.drremote.dsp408.runtime`
- Bundle name: `DSP408 Runtime`
- Provides service: `de.drremote.dsp408.api.DspService`

## Purpose

This bundle wires `dsp408-api` to `dsp408-core`. It owns one `DspController`, exposes it as an OSGi service and provides configuration for connecting to the physical DSP.

## Main Classes

- `DspRuntimeComponent` - OSGi component implementing `DspService`.
- `Dsp408Configuration` - metatype configuration for DSP IP/port and startup behavior.
- `VolumeRoomHandler` - grouped InA-InD volume helper used by HTTP, shell and Matrix.

## Configuration PID

`de.drremote.dsp408controller`

## Configuration Properties

- `dsp_ip` - DSP IPv4 address, default `192.168.0.166`.
- `dsp_port` - DSP TCP port, default `9761`.
- `auto_connect` - connect during component activation.
- `auto_read_on_connect` - scan parameter blocks after connecting.
- `volume_step_db` - default grouped volume step size.

## Responsibilities

- Open and close the TCP connection.
- Maintain current `DspState`.
- Translate API calls into core controller calls.
- Return DTOs and raw transmitted frame info.
- Provide shell-compatible and Matrix-compatible command execution helpers.

## Build

From `dsp408-parent`:

```bash
mvn -pl dsp408-runtime -am test
```

## Notes

The runtime does not expose a user interface by itself. Install `dsp408-shell`, `dsp408-http` or `dsp408-matrix` on top of it for interactive control.
