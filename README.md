# LavaSonic

Adaptive audio integrity and quality diagnostics plugin for Lavalink v4.

LavaSonic operates at the PCM stage before Discord Opus encoding. Normal samples pass through unchanged. When enabled by a player, it can apply bounded gain and a soft ceiling, while reporting RMS, peak, clipping, silence, non-finite samples, and limiter activity.

## Features

- Lavalink v4 AudioFilterExtension named lavasonic.
- Optional soft limiter with a bounded ceiling.
- Per-filter RMS dBFS and peak dBFS metrics.
- Clipping, silence, non-finite sample, and limiter counters.
- Read-only health and metrics endpoints.
- Java 17 and Gradle 8.2 build.
- No external source scraping, credentials, or platform bypass behavior.

## Build

~~~bash
./gradlew build
~~~

The plugin jar is written to build/libs/lavasonic-0.1.0.jar. Copy it into the plugins directory of a Lavalink v4 server.

## Enable from a Lavalink v4 client

Send a normal Lavalink v4 filters operation and put the plugin configuration under pluginFilters:

~~~json
{
  "op": "filters",
  "guildId": "123456789012345678",
  "filters": {
    "pluginFilters": {
      "lavasonic": {
        "enabled": true,
        "mode": "max-detail",
        "guildId": "123456789012345678",
        "trackId": "optional-track-id",
        "gain": 1.0,
        "limiter": true,
        "ceilingDb": -1.0
      }
    }
  }
}
~~~

The mode value is recorded in diagnostics. max-detail is recommended for music. Keep gain at 1.0 unless the client has a specific reason to change it.

## Diagnostics

~~~text
GET /v4/lavasonic/health
GET /v4/lavasonic/metrics
~~~

Metrics are measured before Opus encoding. They cannot describe the final Discord network bitrate or the listener's playback device. Protect the Lavalink node with its normal authorization or a trusted reverse proxy before exposing diagnostics outside the node network.

## Design boundary

This first release is a quality gate and diagnostics layer. It does not pretend to create detail that was absent in the source, and it does not implement automatic cross-provider source fallback. That routing layer requires source metadata and client coordination and will be added separately rather than hiding unreliable behavior inside an audio filter.

## Compatibility

Built against Lavalink API/server 4.0.6 with Java 17. The implementation uses the v4 AudioFilterExtension API and should be validated against the exact Lavalink minor version used in deployment.

## License

MIT
