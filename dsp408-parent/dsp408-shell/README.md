# dsp408-shell

Apache Karaf shell commands for DSP408 control.

## Bundle

- Maven artifact: `de.drremote:dsp408-shell`
- OSGi symbolic name: `de.drremote.dsp408.shell`
- Bundle name: `DSP408 Shell`
- Requires service: `de.drremote.dsp408.api.DspService`

## Purpose

This bundle provides interactive Karaf commands under the `dsp408:` scope. It is the quickest way to operate and test the controller from a Karaf console.

## Command Scope

All commands use the scope:

`dsp408:<command>`

Run:

```text
dsp408:help
```

## Command Groups

Connection:

- `connect`
- `disconnect`
- `reconnect`
- `connected`

State and device:

- `state`
- `status`
- `channels`
- `get <channel>`
- `deviceinfo`
- `login <1234>`
- `meters`

Channel controls:

- `gain <channel> <db>`
- `mute <channel>`
- `unmute <channel>`
- `name <channel> <ascii-name>`
- `phase <channel> <0|180|normal|inverted>`
- `delay <channel> <ms>`
- `delayunit <ms|m|ft>`

Matrix and crossover:

- `route <out> <in>`
- `xgain <out> <in> <db>`
- `xhpset`, `xhpfreq`, `xhpslope`, `xhpbypass`
- `xlpset`, `xlpfreq`, `xlpslope`, `xlpbypass`

EQ and dynamics:

- `opeqset`, `opeqfreq`, `opeqq`, `opeqqraw`, `opeqgain`, `opeqgcode`, `opeqtype`
- `ipeqset`, `ipeqfreq`, `ipeqq`, `ipeqgain`, `ipeqtype`, `ipeqbypass`
- `igeq`
- `gateset`, `gatethreshold`, `gatehold`, `gateattack`, `gaterelease`
- `compset`, `compget`
- `limitset`, `limitget`

Test tone and raw:

- `tone <hz>`
- `toneon <hz>`
- `tonesource <analog|pink|white|sine>`
- `tonefreq <hz>`
- `toneraw <0..30>`
- `toneoff`
- `raw <hex payload>`

Volume helper:

- `volume <help|status|refresh|up|down|mute|unmute|set|step>`
- shortcuts: `volstatus`, `volrefresh`, `volup`, `voldown`, `volmute`, `volunmute`, `volset`, `volstep`

## Build

From `dsp408-parent`:

```bash
mvn -pl dsp408-shell -am test
```

## Notes

Some commands return raw payload/frame data so protocol behavior can be inspected while testing. Single-field PEQ, gate and crossover edits may require a preceding `scanblocks`.
