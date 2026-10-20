# Guide text API — UC-02 / UC-03 contract

`GET /api/locations/{locationId}/guide?language=VI&scriptType=FULL`

Public, no login, available in default and demo profiles. `locationId` is a UUID;
`language` defaults to `VI` (trimmed and uppercased; 2–3 letters, optionally `-XX`);
`scriptType` is case-sensitive `FULL` (default) or `SHORT`.

```json
{
  "contentId": "30000000-0000-0000-0000-000000000004",
  "locationId": "20000000-0000-0000-0000-000000000001",
  "locationName": "Bảo tàng Demo",
  "languageCode": "VI",
  "scriptType": "FULL",
  "scriptText": "Exact stored APPROVED text",
  "audioUrl": null
}
```

- Selection: requested language → EN → VI, always preserving scriptType. Within
  each language, return the highest approved revision with non-whitespace text.
  Location must also be APPROVED; category and effective language must be active.
- `languageCode` is the **effective** language. Compare it with the normalized
  requested language to display a translation-unavailable/fallback notice. Keep
  requested language separately when switching FULL/SHORT; do not overwrite it
  with the fallback language. Discard responses for superseded requests.
- `contentId` identifies the exact returned revision. Text is not translated,
  truncated or summarized at runtime. `audioUrl` is nullable and belongs to that
  revision only; a relative URL is resolved against the API origin. Missing audio
  does not prevent reading. TTS must read this exact text in languageCode.
- No approved match (including hidden/unknown location): **404**
  `application/problem+json`, `detail` =
  `Nội dung cho địa điểm này hiện đang được cập nhật.` No text/audio is returned.
  Clear stale content, disable Listen and keep discovery navigation available.
  Malformed UUID/language/scriptType: **400**; database failure: **503**, retryable.
- Browser integration: use a same-origin `/api` reverse proxy (or Vite dev proxy).
  Cross-origin CORS is not configured by this change. Reading never starts
  playback or records an event.

## Dedicated demo database fixture

Start backend with profile `demo` and its existing datasource settings. Flyway
adds `V3_2__seed_multilingual_guides.sql`; no applied migration is edited. Default
profile exposes the same API but does not seed fictional data.

For location `20000000-0000-0000-0000-000000000001`:

| language | scriptType | contentId suffix | audioUrl |
| --- | --- | --- | --- |
| VI | SHORT | 001 | /demo-audio/museum.wav (unchanged) |
| VI | FULL | 004 | null |
| EN | FULL | 005 | null |
| EN | SHORT | 006 | null |

All content IDs start with `30000000-0000-0000-0000-000000000`.
Call all four combinations above; `language=JA` chooses EN for either mode.
VI fallback, no-approved, revision/status and whitespace branches are exercised
transactionally by `GuideIntegrationTest`, without changing persistent fixtures.
Android demo already explicitly requests `language=VI&scriptType=SHORT`; its
content ID, audio URL and JSON fields remain unchanged. Omitted scriptType now
selects FULL to follow UC-02.

V3_3 adds the group's seven tourism demo locations (ID suffixes 101–107), each with
VI/EN/FR/JA/ZH/RU × FULL/SHORT. Their text is copied exactly from
`docs/en/multilingual_tour_content.md`, with no new pre-rendered audio. The new
Android tour entry defaults to VI/FULL; the older live demo's explicit VI/SHORT
request remains compatible. Shared offline publication is documented in
[CONTENT_PACKAGE_API.md](CONTENT_PACKAGE_API.md). Coordinates/text are demo data
pending verification; a database APPROVED fixture status is not editorial certification.

## Verification commands (PowerShell)

Use a **dedicated PostgreSQL/PostGIS test database**, never a shared/production DB.
Tests migrate the demo fixtures and roll back individual test changes.

```powershell
$env:TEST_DATABASE_URL = 'jdbc:postgresql://localhost:5432/audio_guide_test'
$env:TEST_DATABASE_USERNAME = '<test user>'
$env:TEST_DATABASE_PASSWORD = '<test password>'
Set-Location backend/multi-audio-guide
.\mvnw.cmd -B -ntp test
```

Without `TEST_DATABASE_URL`, PostgreSQL integration tests are **skipped**; passing
controller/service tests with mocked repositories does not prove SQL selection,
fixture installation, fallback or a live Web integration. See SESSION_REPORT.md
in `docs/vi/web-demo-prompts/` for this session's observed results.
