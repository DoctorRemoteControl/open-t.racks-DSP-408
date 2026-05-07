# dsp408-features

Apache Karaf feature descriptor module.

## Artifact

- Maven artifact: `de.drremote:dsp408-features`
- Packaging: `pom`
- Attached artifact: `features.xml` with classifier `features`

## Purpose

This module packages the Karaf feature repository used to install the DSP408 bundles and their runtime dependencies.

## Feature File

Source:

`src/main/resources/features.xml`

Generated/attached during Maven package:

`target/features.xml`

## Features

- `dsp408-runtime` - installs SCR support plus API, core and runtime.
- `dsp408-shell` - installs runtime plus Karaf shell commands.
- `dsp408-matrix` - installs runtime plus Matrix bot and Jackson dependencies.
- `dsp408-http` - installs runtime, HTTP Whiteboard support, HTTP servlet and Jackson dependencies.
- `dsp408-all` - installs runtime, shell, Matrix and HTTP bundles.

## Build

From `dsp408-parent`:

```bash
mvn -pl dsp408-features -am package
```

## Karaf Usage

After publishing or installing the artifacts into a Maven repository visible to Karaf:

```text
feature:repo-add mvn:de.drremote/dsp408-features/0.0.1-SNAPSHOT/xml/features
feature:install dsp408-all
```

For a smaller install:

```text
feature:install dsp408-runtime
feature:install dsp408-shell
```

## Notes

The feature descriptor does not configure the DSP connection or Matrix bot. Use the OSGi configuration PIDs documented in the runtime and Matrix bundle READMEs.
