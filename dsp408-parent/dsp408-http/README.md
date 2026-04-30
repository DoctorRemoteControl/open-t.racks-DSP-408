# dsp408-http

HTTP/JSON control API for the DSP408 runtime.

## Bundle

- Maven artifact: `de.drremote:dsp408-http`
- OSGi symbolic name: `de.drremote.dsp408.http`
- Bundle name: `DSP408 HTTP`
- Requires service: `de.drremote.dsp408.api.DspService`

## Purpose

This bundle exposes `DspService` through an OSGi HTTP Whiteboard servlet. It is useful for scripts, dashboards, integrations and smoke tests without using the Karaf shell.

## Servlet

- Class: `DspApiServlet`
- Pattern: `/api/v1/*`

## Useful Endpoints

- `GET /api/v1/health`
- `GET /api/v1/state`
- `POST /api/v1/connection/connect`
- `POST /api/v1/connection/disconnect`
- `GET /api/v1/channels`
- `POST /api/v1/channels/{channelId}/gain`
- `POST /api/v1/channels/{channelId}/mute`
- `POST /api/v1/channels/{channelId}/delay`
- `POST /api/v1/matrix/route`
- `POST /api/v1/matrix/crosspoint-gain`
- `POST /api/v1/channels/{channelId}/peq/{peqIndex}`
- `POST /api/v1/channels/{channelId}/input-peq/{peqIndex}`
- `POST /api/v1/channels/{channelId}/gate`
- `POST /api/v1/channels/{channelId}/compressor`
- `POST /api/v1/channels/{channelId}/limiter`
- `POST /api/v1/test-tone/sine`
- `POST /api/v1/blocks/scan`
- `POST /api/v1/raw`
- `POST /api/v1/command`

## OpenAPI

The bundled API description is available at:

`GET /api/v1/openapi.json`

Source file:

`src/main/resources/openapi/dsp408-openapi.json`

## Example

```bash
curl -X POST http://localhost:8181/api/v1/channels/ina/gain \
  -H "Content-Type: application/json" \
  -d "{\"db\": -6.0}"
```

## Build

From `dsp408-parent`:

```bash
mvn -pl dsp408-http -am test
```

## Notes

This bundle does not authenticate requests. Put it behind a trusted network boundary or reverse proxy if exposing it beyond localhost/LAN.
