# Fineract Chat Archive

A standalone tool that will archive public Slack messages for Apache Fineract into a static site.

## Local run

```
./gradlew updateChatArchive
```

Configuration:
- File: `config/archive.properties`
- Key: `channels.allowlist` (comma-separated channel names, e.g. `#fineract`)
- Key: `output.dir` (relative path for site output, default `docs`)

Environment:
- `SLACK_TOKEN` (Slack Bot token)
