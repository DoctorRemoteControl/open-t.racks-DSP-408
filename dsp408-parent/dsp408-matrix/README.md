# dsp408-matrix

Matrix bot integration for DSP408 control.

## Bundle

- Maven artifact: `de.drremote:dsp408-matrix`
- OSGi symbolic name: `de.drremote.dsp408.matrix`
- Bundle name: `DSP408 Matrix Bot`
- Requires service: `de.drremote.dsp408.api.DspService`

## Purpose

This bundle lets Matrix rooms control the DSP408. It supports human-readable `!dsp ...` commands in a control room, a restricted volume room and structured machine events for service-to-service control.

## Main Classes

- `MatrixBotComponent` - OSGi component and sync loop.
- `MatrixBotConfiguration` - metatype configuration.
- `MatrixRuntimeConfig` - normalized runtime config.
- `MatrixApiClient` - minimal Matrix Client-Server API client.
- `MatrixMessageHandler` - dispatches room events.
- `MatrixDspCommandDispatcher` - maps `!dsp` commands to `DspService`.

## Configuration PID

`de.drremote.dsp408controller.matrix`

## Configuration Properties

- `enabled` - start/stop the bot.
- `matrix_url` - Matrix homeserver base URL.
- `access_token` - access token for the bot account.
- `control_room_id` - room for full `!dsp` control.
- `volume_room_id` - room for `!dsp volume ...` commands.
- `machine_room_id` - room for structured machine events.
- `allowed_users` - users allowed to issue normal commands.
- `admin_users` - users allowed to issue admin commands.
- `machine_users` - users allowed to send machine events.
- `sync_timeout_ms` - Matrix sync timeout.
- `reconnect_delay_ms` - delay after Matrix errors.
- `connect_dsp_on_start` - connect the DSP during bot startup.

## Human Commands

Control room messages must start with `!dsp`.

Examples:

```text
!dsp status
!dsp gain ina -6
!dsp route out1 ina
!dsp xgain out1 inb -12
!dsp opeqfreq out1 1 1000
!dsp tone 1000
!dsp toneoff
```

Admin-only human commands:

- `connect`
- `disconnect`
- `reconnect`
- `raw`
- `sendraw`

## Machine Events

Machine control uses Matrix event type:

`de.drremote.dsp408.command`

Responses are sent as:

`de.drremote.dsp408.response`

Example event content:

```json
{
  "requestId": "req-001",
  "command": "channel.gain.set",
  "args": {
    "channelId": "ina",
    "db": -6.0
  }
}
```

Supported machine command families include:

- `connection.*`
- `device.info.get`
- `state.get`
- `blocks.scan`
- `block.read`
- `channel.*`
- `matrix.*`
- `crossover.*`
- `output.peq.*`
- `input.peq.*`
- `input.geq.*`
- `input.gate.*`
- `compressor.set`
- `limiter.set`
- `testtone.*`
- `volume.command`

## Build

From `dsp408-parent`:

```bash
mvn -pl dsp408-matrix -am test
```

## Notes

The bot trusts Matrix room/user configuration for authorization. Keep access tokens out of source control and use Karaf config for deployment-specific values.
