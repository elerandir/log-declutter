# Security Policy

## Reporting a vulnerability

Please report suspected vulnerabilities **privately** — do not open a public issue.

Use GitHub's [private vulnerability reporting](https://docs.github.com/en/code-security/security-advisories/guidance-on-reporting-and-writing-information-about-vulnerabilities/privately-reporting-a-security-vulnerability)
for this repository: go to the **Security** tab → **Report a vulnerability**. We aim to
acknowledge reports within a few days and will keep you informed as we work on a fix.

Please include, where possible:

- affected version / commit,
- a description of the issue and its impact,
- steps to reproduce or a proof of concept.

## Supported versions

This project is pre-1.0; only the latest `main` receives security fixes.

## Threat model

`log-declutter` is an offline command-line tool. It:

- reads two local files (a log file and a patterns file) supplied by the user,
- writes one local output file,
- performs no network I/O, opens no sockets, and starts no long-running processes.

The main risks we consider are therefore:

- **Untrusted pattern input** — patterns are compiled as Java regular expressions. A
  maliciously crafted pattern could cause catastrophic backtracking (ReDoS) and make the
  process spin on CPU. Only run patterns you trust; treat the patterns file as code.
- **Untrusted / large log input** — the tool reads the log into memory. Extremely large logs
  can exhaust memory. Prefer splitting very large files.
- **Output path** — the tool writes to the path you specify (or a default derived from the
  input). Ensure the destination is a location you intend to overwrite.

Out of scope: multi-user privilege boundaries, sandboxing of regex execution, and network
threats (there is no network surface).

## Supply-chain controls

- The Gradle wrapper distribution is pinned by SHA-256 and the wrapper jar is committed and
  validated in CI.
- All resolved dependencies are checksum-verified via `gradle/verification-metadata.xml`.
- CI workflows run with least-privilege permissions and StepSecurity Harden-Runner.
- CodeQL, secret scanning (gitleaks), dependency review, and OpenSSF Scorecard run
  automatically; Dependabot keeps dependencies and actions up to date.
