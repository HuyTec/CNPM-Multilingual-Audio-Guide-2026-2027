# Shared demo content packages (Web / Android)

Requires the running backend's `demo` profile and demo migration V3_3. This is the
local demo protocol; production signing/trust and package history are not implemented.
V3_4 adds the authenticated editor workspace described in [ADMIN_CONTENT_API.md](ADMIN_CONTENT_API.md).
Saving/submitting a workspace does not change any public package.

- `GET /api/content-packages/key`: `{algorithm, keyId, spki}`. SPKI is a base64
  public RSA key, keyId is SHA-256 of the decoded SPKI. No private key is exposed.
- `GET /api/content-packages`: BASE and OPTIONAL manifests. BASE contains the
  seven new locations / 84 scripts (VI, EN, FR, JA, ZH, RU × FULL/SHORT);
  OPTIONAL contains the older demo locations and their existing WAV assets.
- Manifest: `schemaVersion:1`, `scope`, `version`, `status:"APPROVED"`, `isBase`,
  `sizeBytes`, `sha256`, base64 `signature`, `keyId`, `downloadUrl`, `assets`.
  Asset: `{url,sizeBytes,sha256}`. Size includes JSON bytes; asset sizes are separate.
- `GET /api/content-packages/{BASE|OPTIONAL}/{version}` returns exact UTF-8 JSON
  bytes. Verify byte length, SHA-256 and SHA256withRSA / RSASSA-PKCS1-v1_5 signature
  **before parsing**. Payload: `{schemaVersion,status,scope,isBase,locations,guides,languages,assets}`.
  Location fields match discovery API; guide fields match [GUIDE_API.md](GUIDE_API.md).
  Version is `scope.toLowerCase() + "-" + sha256`; changed data produces a new version.
  Stale version → 404; unknown scope → 400. Only latest APPROVED, nonblank scripts
  from public locations and active languages/categories are published.

Clients download only after confirmation, verify every declared audio asset and
replace a scope atomically. Cancel/failure keeps the previous package. BASE is
protected from deletion. Offline guide selection preserves scriptType and follows
Selected → EN → VI; no matching guide means unavailable, never generated translation.
Web provisions BASE online, uses IndexedDB + production service worker for reopen;
Android also bundles a verified public signed BASE envelope in assets/content/base.json.
Map tiles and network-dependent TTS voices are not part of these packages.

Audio assets accept the existing `/demo-audio/*.wav` fixtures and approved uploaded
`/api/content-audio/{uuid}.{mp3|wav|ogg}` files. Uploaded bytes are stored in PostgreSQL;
draft previews require Admin authentication and are excluded from public packages.
Clients verify the same asset bytes/checksum and preserve the format when playing offline.

The demo generates an ephemeral signing key on backend startup and serves its
public key at the same origin. This verifies integrity against that fetched key;
it does **not** establish independent publisher trust over local HTTP. Production
requires HTTPS, a stable pinned/rotatable trust key and durable published versions.
Restarting the demo server leaves payload versions unchanged but changes signatures/keyId;
already verified installed packages retain their own public key and remain readable.

Fixture source: docs/en/multilingual_tour_content.md. Check exact generated SQL with
`node scripts/generate-tour-fixture.mjs --check` in backend/multi-audio-guide.
Do not regenerate/modify an already applied migration. Coordinates are approximate
demo markers; content and translations still need editorial review.
