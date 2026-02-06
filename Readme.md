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
- Key: `state.dir` (relative path for cursor state, default `state`)
- Key: `fetch.lookback.days` (how many days to re-fetch, default `1`)

Output:
- Daily pages: `docs/daily/<channel>/<YYYY-MM-DD>.md`
- Channel index: `docs/daily/<channel>/index.md`
- Global index: `docs/index.md`

Environment:
- `SLACK_TOKEN` (Slack Bot token)

## Automation

GitHub Actions can run the archive update daily and commit new output.

Setup:
- Add repository secret `SLACK_TOKEN`
