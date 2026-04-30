# DSP408 Karaf Configuration Using Only `config:property-set`

This guide shows how to configure the DSP408 service and the Matrix bot directly in Apache Karaf.

Only these Karaf commands are used:

* `config:edit`
* `config:property-set`
* `config:update`
* optional `config:proplist`

No files in `etc/` are used.

---

## Overview

Your project contains two configurations:

```text
de.drremote.dsp408controller
de.drremote.dsp408controller.matrix
```

The first configuration is for the DSP connection.
The second configuration is for the Matrix bot.

---

## Important Property Names

The property names are derived from the method names in your configuration interfaces.
An underscore `_` becomes a dot `.`.

### DSP Configuration

```text
dsp_ip()                  -> dsp.ip
dsp_port()                -> dsp.port
auto_connect()            -> auto.connect
auto_read_on_connect()    -> auto.read.on.connect
volume_step_db()          -> volume.step.db
```

### Matrix Bot Configuration

```text
enabled()                 -> enabled
matrix_url()              -> matrix.url
access_token()            -> access.token
admin_room_id()           -> admin.room.id
volume_room_id()          -> volume.room.id
machine_room_id()         -> machine.room.id
allowed_users()           -> allowed.users
admin_users()             -> admin.users
machine_users()           -> machine.users
sync_timeout_ms()         -> sync.timeout.ms
reconnect_delay_ms()      -> reconnect.delay.ms
connect_dsp_on_start()    -> connect.dsp.on.start
```

---

## 1. DSP Configuration

PID:

```text
de.drremote.dsp408controller
```

### Example

```bash
config:edit de.drremote.dsp408controller
config:property-set dsp.ip 192.168.50.166
config:property-set dsp.port 9761
config:property-set auto.connect true
config:property-set auto.read.on.connect true
config:property-set volume.step.db 1.0
config:update
```

### Meaning

`dsp.ip` is the IP address of the DSP.
`dsp.port` is the TCP port of the DSP.
`auto.connect` automatically connects to the DSP when the component is activated.
`auto.read.on.connect` automatically reads the parameter blocks after connecting.
`volume.step.db` is the step size for `louder` and `quieter` in the volume room.

---

## 2. Matrix Bot Configuration

PID:

```text
de.drremote.dsp408controller.matrix
```

### Full Example

```bash
config:edit de.drremote.dsp408controller.matrix
config:property-set enabled true
config:property-set matrix.url https://matrix.example.net
config:property-set access.token syt_dummy_access_token_123456
config:property-set admin.room.id '!dummyadminroom:example.net'
config:property-set volume.room.id '!dummyvolumeroom:example.net'
config:property-set machine.room.id '!dummymachineroom:example.net'
config:property-set allowed.users @alice:example.net,@bob:example.net,@admin:example.net
config:property-set admin.users @admin:example.net
config:property-set machine.users @orchestrator:example.net,@automation:example.net
config:property-set sync.timeout.ms 30000
config:property-set reconnect.delay.ms 3000
config:property-set connect.dsp.on.start true
config:update
```

### Meaning

`enabled` enables the bot.
`matrix.url` is the base URL of your Matrix homeserver.
`access.token` is the access token of the bot account.
`admin.room.id` is the room for `!dsp ...` commands.
`volume.room.id` is the room for volume commands like `louder`, `quieter`, or `set -10`.
`machine.room.id` is the room for machine JSON commands.
`allowed.users` contains all users allowed to use the bot.
`admin.users` contains the users with extended permissions.
`machine.users` contains the users allowed to send machine commands.
`sync.timeout.ms` is the Matrix sync interval.
`reconnect.delay.ms` is the delay before a new connection attempt.
`connect.dsp.on.start` automatically connects to the DSP when the bot starts.

---

## 3. Complete Example Configuration

### DSP

```bash
config:edit de.drremote.dsp408controller
config:property-set dsp.ip 192.168.50.166
config:property-set dsp.port 9761
config:property-set auto.connect true
config:property-set auto.read.on.connect true
config:property-set volume.step.db 1.0
config:update
```

### Matrix

```bash
config:edit de.drremote.dsp408controller.matrix
config:property-set enabled true
config:property-set matrix.url https://matrix.example.net
config:property-set access.token syt_dummy_access_token_123456
config:property-set admin.room.id '!dummyadminroom:example.net'
config:property-set volume.room.id '!dummyvolumeroom:example.net'
config:property-set machine.room.id '!dummymachineroom:example.net'
config:property-set allowed.users @alice:example.net,@bob:example.net,@admin:example.net
config:property-set admin.users @admin:example.net
config:property-set machine.users @orchestrator:example.net,@automation:example.net
config:property-set sync.timeout.ms 30000
config:property-set reconnect.delay.ms 3000
config:property-set connect.dsp.on.start true
config:update
```

---

## 4. Verify the Configuration

After setting the properties, you can verify the active configuration.

### DSP

```bash
config:edit de.drremote.dsp408controller
config:proplist
```

### Matrix

```bash
config:edit de.drremote.dsp408controller.matrix
config:proplist
```

---

## 5. Recommended Order

First set the DSP configuration:

```bash
config:edit de.drremote.dsp408controller
config:property-set dsp.ip 192.168.50.166
config:property-set dsp.port 9761
config:property-set auto.connect true
config:property-set auto.read.on.connect true
config:property-set volume.step.db 1.0
config:update
```

Then set the Matrix configuration:

```bash
config:edit de.drremote.dsp408controller.matrix
config:property-set enabled true
config:property-set matrix.url https://matrix.example.net
config:property-set access.token syt_dummy_access_token_123456
config:property-set admin.room.id '!dummyadminroom:example.net'
config:property-set volume.room.id '!dummyvolumeroom:example.net'
config:property-set allowed.users @alice:example.net,@bob:example.net,@admin:example.net
config:property-set admin.users @admin:example.net
config:property-set sync.timeout.ms 30000
config:property-set reconnect.delay.ms 3000
config:property-set connect.dsp.on.start true
config:update
```

---

## 6. Common Mistakes

### Wrong Property Names

Wrong:

```text
matrix_url
access_token
admin_room_id
```

Correct:

```text
matrix.url
access.token
admin.room.id
```

### Forgetting `config:update`

If you do not run `config:update`, the changes will not be applied.

### Wrong Room ID

For `admin.room.id` and `volume.room.id`, you should use the real Matrix room ID, for example:

```text
!dummyroomid123:example.net
```

not a room alias like:

```text
#myroom:example.net
```

### Empty User Lists

If `allowed.users` and `admin.users` are empty, the bot will not respond to anyone.

### Admin Commands Without `!dsp`

In the admin room, your code only processes commands that start with `!dsp`, for example:

```text
!dsp state
!dsp connect
!dsp gain ina -10
```

---

## 7. Minimal Test

### DSP Minimal Test

```bash
config:edit de.drremote.dsp408controller
config:property-set dsp.ip 192.168.50.166
config:property-set dsp.port 9761
config:property-set auto.connect true
config:property-set auto.read.on.connect false
config:update
```

### Matrix Minimal Test

```bash
config:edit de.drremote.dsp408controller.matrix
config:property-set enabled true
config:property-set matrix.url https://matrix.example.net
config:property-set access.token syt_dummy_test_token
config:property-set admin.room.id '!testroom:example.net'
config:property-set allowed.users @tester:example.net
config:property-set admin.users @tester:example.net
config:update
```

---

## 8. Short Version

### DSP

```bash
config:edit de.drremote.dsp408controller
config:property-set dsp.ip 192.168.50.166
config:property-set dsp.port 9761
config:property-set auto.connect true
config:property-set auto.read.on.connect true
config:update
```

### Matrix

```bash
config:edit de.drremote.dsp408controller.matrix
config:property-set enabled true
config:property-set matrix.url https://matrix.example.net
config:property-set access.token syt_dummy_access_token
config:property-set admin.room.id '!dummyadminroom:example.net'
config:property-set volume.room.id '!dummyvolumeroom:example.net'
config:property-set machine.room.id '!dummymachineroom:example.net'
config:property-set allowed.users @botuser:example.net
config:property-set admin.users @botuser:example.net
config:property-set machine.users @orchestrator:example.net
config:property-set sync.timeout.ms 30000
config:property-set reconnect.delay.ms 3000
config:property-set connect.dsp.on.start true
config:update
```


