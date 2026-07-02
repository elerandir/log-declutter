# log-declutter

[![CI](https://github.com/elerandir/log-declutter/actions/workflows/ci.yml/badge.svg)](https://github.com/elerandir/log-declutter/actions/workflows/ci.yml)
[![CodeQL](https://github.com/elerandir/log-declutter/actions/workflows/codeql.yml/badge.svg)](https://github.com/elerandir/log-declutter/actions/workflows/codeql.yml)
[![Secret scan](https://github.com/elerandir/log-declutter/actions/workflows/gitleaks.yml/badge.svg)](https://github.com/elerandir/log-declutter/actions/workflows/gitleaks.yml)
[![OpenSSF Scorecard](https://api.securityscorecards.dev/projects/github.com/elerandir/log-declutter/badge)](https://securityscorecards.dev/viewer/?uri=github.com/elerandir/log-declutter)
[![Java](https://img.shields.io/badge/Java-21-blue.svg)](https://adoptium.net/temurin/releases/?version=21)
[![Gradle](https://img.shields.io/badge/Gradle-9.6.1-blue.svg)](https://gradle.org/)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)

A small command-line tool that declutters application logs. It can strip noisy leading
prefixes (e.g. the Kubernetes CRI runtime prefix), drop log lines matching partial strings,
remove blank lines, and convert structured JSON logs into readable classic-style lines —
then write the cleaned result to a new file.

## Usage

```console
log-declutter <LOG_FILE> [PATTERNS_FILE] [--strip-cri] [-s <REGEX>]... [-j] [-o <OUTPUT_FILE>]
```

### Processing pipeline

Each line flows through these steps, in order:

1. **Strip prefix** — remove any configured leading prefix (`--strip-cri` / `--strip-prefix`).
2. **Remove** — drop the line if it is blank or matches a removal **pattern**. Matching is done
   on the stripped line *before* JSON unwrapping, so your patterns match the raw log content
   (JSON fields and all), regardless of how the survivors are later reformatted.
3. **Unwrap JSON** — for surviving lines, optionally convert a JSON object into a classic-style
   line (`--unwrap-json`).
4. **Write** — emit what remains, in the original order.

> **Note:** if you omit `PATTERNS_FILE`, no removal patterns are loaded and **no lines are
> removed** — the tool only strips/unwraps and drops blank lines. Pass a patterns file to
> remove matching lines.

- `LOG_FILE` — the log to clean.
- `PATTERNS_FILE` *(optional)* — one pattern per line. Each line is a **literal** partial
  string matched anywhere in a log line (so `DEBUG` removes any line *containing* `DEBUG`).
  Regex metacharacters such as `.`, `[`, `]`, `(`, `)`, `*`, `$`, and `\` are matched verbatim
  and need no escaping. Blank lines and lines starting with `#` are ignored, so you can comment
  your pattern file. Omit this argument to only strip/unwrap and drop blank lines.
- `--strip-cri` — strip the Kubernetes CRI runtime prefix (e.g.
  `2026-07-01T12:12:58.4378384Z stdout F `) from each line, leaving the payload.
- `-s, --strip-prefix <REGEX>` — strip the leading match of a custom regex from each line. The
  regex is anchored at the start of the line (no `^` needed). Repeatable; applied in order.
- `-j, --unwrap-json` — convert JSON-object log lines into classic
  `<timestamp>  <LEVEL> [<thread>] <logger> - <message>` lines (`[thread]` and `<logger>` are
  included only when present). Common field names are recognised (`@timestamp`/`timestamp`,
  `log.level`/`level`, `log.logger`/`logger`, `process.thread.name`/`thread`, `message`/`msg`),
  and nested objects (`{"log":{"level":...}}`) resolve too. Lines that are not JSON objects
  (plain text, stack traces) pass through unchanged.
- `-o, --output` — where to write the decluttered log. Defaults to the input file name with a
  `.decluttered` suffix (e.g. `app.log` → `app.log.decluttered`).

### Example — cleaning up CRI-wrapped JSON logs

`app.log`:

```
2026-07-01T12:12:58.4378384Z stdout F {"@timestamp":"2026-07-01T12:12:58.437Z","log.level":"INFO","log.logger":"com.elerandir.App","message":"started"}
2026-07-01T12:12:59.1000000Z stderr F {"@timestamp":"2026-07-01T12:12:59.100Z","log.level":"DEBUG","message":"cache hit"}
2026-07-01T12:13:00.2000000Z stdout F {"@timestamp":"2026-07-01T12:13:00.200Z","log.level":"WARN","message":"retry"}
```

Strip the runtime prefix, unwrap the JSON, and drop DEBUG lines (`patterns.txt` contains `DEBUG`):

```console
./gradlew run --args="app.log patterns.txt --strip-cri --unwrap-json -o app.clean.log"
```

`app.clean.log`:

```
2026-07-01T12:12:58.437Z  INFO com.elerandir.App - started
2026-07-01T12:13:00.200Z  WARN - retry
```

### Example — removing noisy lines

`app.log`:

```
INFO  server started
DEBUG cache hit key=42
ERROR failed to connect

INFO  healthcheck ping ok
WARN  retrying request
```

`patterns.txt`:

```
# drop debug noise and healthchecks
DEBUG
healthcheck
```

Run it (via the Gradle `run` task during development):

```console
./gradlew run --args="app.log patterns.txt -o app.clean.log"
```

`app.clean.log`:

```
INFO  server started
ERROR failed to connect
WARN  retrying request
```

The `DEBUG` and `healthcheck` lines and the blank line are gone; everything else is kept in
its original order.

## Options

| Argument / option        | Required | Description                                                                 |
| ------------------------ | -------- | --------------------------------------------------------------------------- |
| `LOG_FILE`               | yes      | The log file to declutter.                                                  |
| `PATTERNS_FILE`          | no       | File of literal partial strings; lines containing any are removed.          |
| `--strip-cri`            | no       | Strip the Kubernetes CRI runtime prefix from each line.                     |
| `-s`, `--strip-prefix`   | no       | Strip a custom leading regex from each line (repeatable, applied in order). |
| `-j`, `--unwrap-json`    | no       | Convert JSON-object lines to classic `TIMESTAMP LEVEL [thread] logger - message`. |
| `-o`, `--output`         | no       | Output path. Defaults to `<LOG_FILE>.decluttered`.                          |
| `-h`, `--help`           | no       | Show usage help and exit.                                                   |
| `-V`, `--version`        | no       | Show version and exit.                                                      |

## Build

Requires **JDK 21** (Temurin recommended). The project uses the running JDK rather than a
Gradle toolchain, so no JDK is auto-downloaded.

```console
./gradlew build          # compile, run tests, assemble
./gradlew run --args="…"  # run the CLI
./gradlew installDist     # produce a runnable distribution under build/install
```

**IDE setup:** enable annotation processing and install the **Lombok** plugin (Lombok is a
compile-time dependency; without the plugin your IDE will report missing generated members).
The DI wiring is generated by Dagger's annotation processor at compile time.

**Regenerate dependency verification metadata** after any dependency change (Dependabot PRs
will fail until you do):

```console
./gradlew --write-verification-metadata sha256 build
```

Then re-apply the two local settings in `gradle/verification-metadata.xml`: keep
`<verify-metadata>false</verify-metadata>` and the `<trusted-artifacts>` block for
`*-sources.jar` / `*-javadoc.jar`.

## Security / supply chain

This project ships with several supply-chain hardening measures:

- **Gradle wrapper pinned** — `distributionSha256Sum` and `validateDistributionUrl=true` in
  `gradle-wrapper.properties`; the wrapper jar is committed and validated in CI via
  `gradle/actions/wrapper-validation`.
- **Dependency verification** — `gradle/verification-metadata.xml` checksum-verifies every
  resolved jar.
- **Least-privilege CI** — every workflow defaults to `contents: read` and each job starts
  with StepSecurity Harden-Runner (`egress-policy: audit`); actions are pinned to released
  tags.
- **Static analysis & scanning** — CodeQL, gitleaks secret scanning, GitHub
  dependency-review on PRs, and an OpenSSF Scorecard run.
- **Automated updates** — Dependabot watches both Gradle and GitHub Actions dependencies.

See [SECURITY.md](SECURITY.md) for the threat model and how to report a vulnerability.

## License

Licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE).

Copyright © 2026 elerandir
