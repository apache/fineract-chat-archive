# Slack Chat Archiver

A standalone tool for archiving Slack messages into a static site.

## Install

GitHub Actions can run the archive update daily and commit new output.

### Local

- Clone this repo
- Install Java 25
- Test (see below)
- Set `SLACK_TOKEN` and `CHANNELS_ALLOWLIST` env vars
- Run (see below)

### GitHub Actions

- Create a new repo
- Copy `update-archive.yml` to a folder named `.github/workflows/`
- Customize `CHANNELS_ALLOWLIST` env var in `update-archive.yml`
- Add repository secret `SLACK_TOKEN`

## Test

```bash
./gradlew test
```

## Run

```bash
./gradlew --quiet updateChatArchive
```

Configuration (environment variables):

- `SLACK_TOKEN` (required; Slack Bot token) ⚠️ warning: keep this secret!
- `CHANNELS_ALLOWLIST` (required; comma-separated channel names, e.g. `#general,#random`)
- `OUTPUT_DIR` (optional; relative path for site output, default `docs`)
- `STATE_DIR` (optional; relative path for cursor state, default `state`)
- `LOOKBACK_DAYS` (optional; how many days to re-fetch, default `1`)

Output:

- Daily pages: `docs/daily/<channel>/<YYYY-MM-DD>.md`
- Channel index: `docs/daily/<channel>/index.md`
- Global index: `docs/index.md`
- Thread replies are rendered below parent messages with a simple prefix.

Slack app setup:

1. Create a Slack app (from scratch) in the target workspace.
2. Add a bot user.
3. Add the required scopes listed below.
4. Install the app to the workspace.
5. Copy the Bot User OAuth Token (starts with `xoxb-`) into `SLACK_TOKEN`.

Required Slack scopes:

- `channels:read` (list public channels)
- `channels:history` (read public channel history)
- `users:read` (resolve user display names)

Permalinks are resolved via `chat.getPermalink`. If Slack returns `missing_scope`,
add the scope Slack reports and re-install the app.

GitHub Pages:
- The `docs/` directory is intended for publishing via GitHub Pages.

## Instances

If you run this bot and archive data relevant to Fineract, please list your instance here.
These also serve as working examples if you're looking for extra help installing the archiver.

* #fineract unofficial main public chat (PENDING) - [source code](https://github.com/mifos/chat-archive)

## History

The [original intent](https://issues.apache.org/jira/browse/FINERACT-2171) of this code was to [archive every public Fineract chat message](https://lists.apache.org/thread/9x6rftxoc4kdwp754odqvktdq2cz106h) into a public, index-able, simple/static HTML website.

## Copyright and License

See `LICENSE` and `NOTICE`.
