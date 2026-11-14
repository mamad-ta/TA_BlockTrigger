# Security Policy

## Supported versions

Only the latest release receives fixes.

| Version | Supported |
|---|---|
| 1.1.x | ✅ |
| 1.0.x | ❌ (superseded) |

## Reporting a vulnerability

Please **do not** open a public issue for security problems.

Use [GitHub's private vulnerability reporting](https://github.com/mamad-ta/TA_BlockTrigger/security/advisories/new)
instead. Include the plugin version, the server software and version, and the steps to reproduce.

You can expect an acknowledgement within a few days. Once a fix is released, the report is
credited in the changelog unless you prefer to stay anonymous.

## Scope

Relevant issues include permission bypasses (`/bt` reachable by non-operators, block triggers
firing for players who lack `tablocktrigger.use`), command-injection through placeholders,
and anything that would let a player run a command they should not be able to run.

Issues that require an already-compromised operator account, or that come from another plugin
misusing this one's API, are out of scope.
