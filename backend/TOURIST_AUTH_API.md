# Tourist identity and Gmail OTP

Tourists can browse/read/listen without signing in. Android creates a random installation secret and stores it, the email session and queued playback identity snapshots using Android Keystore AES-GCM. It sends only tourist credentials to public endpoints; these never authorize `/api/admin/**`.

## Run with Gmail

Use the existing ignored local Compose environment file. Add `GMAIL_USERNAME` (the sending Gmail address) and `GMAIL_APP_PASSWORD` (a Google App Password, not the normal account password). Do not commit these values or put them in the APK. Gmail SMTP uses `smtp.gmail.com:587`, authentication and required STARTTLS. Google requires 2-Step Verification for App Passwords; availability depends on account policy: [Google instructions](https://support.google.com/accounts/answer/185833).

Rebuild/start the backend with the same existing Compose env files. With empty Gmail values, the app stays usable as a guest and OTP requests return 503. Real delivery must be checked using the configured account; automated tests replace only the mail transport and never send email. Use HTTPS when exposing the API beyond local development.

For this checkout, run from the repository root after filling the Gmail values in the ignored local env file:

```powershell
$env:BACKEND_PORT = '8080'
$env:BACKEND_BIND_ADDRESS = '127.0.0.1'
docker compose --env-file .env.demo.local --env-file .env.admin.local up -d --build backend
```

The default APK reaches that endpoint through `http://10.0.2.2:8080/` on the Android emulator. For a physical device, rebuild with the existing `-PtravelApiBaseUrl=https://your-api-origin/` option. Public playback/content-package endpoints currently run under the existing `demo` profile; this change does not replace the separate publication/production deployment work.

## Contract

All identity endpoints return `Cache-Control: no-store`. `X-Installation-Key` is a device-generated base64url secret of 32 random bytes (43 characters).

| Method / path | Body | Result |
| --- | --- | --- |
| POST `/api/tourist/guest` | `{}` | Stable anonymous `userId` for the installation |
| POST `/api/tourist/otp/request` | `{ "email": "..." }` | `challengeId`, expiry 300 seconds, resend wait 60 seconds; never includes the OTP |
| POST `/api/tourist/otp/verify` | `{ "challengeId": "...", "code": "......" }` | Verified `userId`, `email`, opaque `accessToken`, `expiresAt` (30 days) |
| POST `/api/tourist/logout` | `{}` and Bearer token | Revokes that session; 204 |

OTP is stored as a salted PBKDF2 hash, valid once for 5 minutes, with at most 5 failed attempts. Database locks serialize the 60-second resend cooldown and hourly limits (5 per email, 10 per installation, 30 per source IP). Only the direct peer IP is used; configure network controls/proxy handling appropriately before deployment behind a shared proxy. OTPs and bearer credentials are never logged. SMTP failure rolls back the new challenge.

`POST /api/playbacks` keeps its existing `{ eventId, contentId }` body and idempotency. Android adds `X-Installation-Key`, and Bearer authorization when signed in. The server resolves `listener_id`; clients cannot submit another user ID. Existing Web clients without identity remain compatible with an unattributed listener. Playbacks are counted once per event, not once per person. Guest history remains stored, with a merge reference after email verification; it is not copied into duplicate events. Signing out rotates the installation secret for the next guest.

The Android outbox preserves the identity at listening time, encrypts queued credentials, commits each server acknowledgement, and flushes at launch/new playback/before logout. Logout waits for pending plays to sync before revocation. An expired session requires reauthentication to sync its pending plays; it must not silently reattribute them to a different account.

## Catalog and cache

New migration V4.1 archives exactly the six original fictional discovery fixtures and their guide revisions. Public discovery, guides and generated content packages exclude them; historical FK references remain intact. The bundled Android BASE already contains seven real District 1 places. Signed BASE snapshots now include retired location IDs. Android applies these signed revocations to older installed packages as well, so the original fictional fixtures stay hidden offline without deleting the stored package.

Android reads all catalog pages. Guide cache keys contain location/language/FULL–SHORT, are scoped to a repository/backend instance, capped at 32 entries and expire after 60 seconds; retry explicitly clears them. Errors never introduce draft content. Signed offline packages remain the persistent fallback when the network is unavailable.

Device TTS renders the approved text at 1× into PCM WAV, then uses MediaPlayer for measured duration, second-based seek/pause/resume and playback speed. Cache identity includes content ID, script text/type/language and voice; the cache is capped at 50 MB with oldest files removed. Rendering failure keeps readable text and offers replay; it does not fabricate a time estimate. The first TTS render may take longer than replay and requires a matching installed voice.
