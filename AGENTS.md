1

# Project agent instructions

## ADHD-friendly mode

The `i-have-adhd` Codex plugin is installed for this project. When the user
invokes `$i-have-adhd`, apply its output rules for the rest of the session,
until the user says `stop adhd mode` or `normal mode`.

In this mode:

1. Start with the next concrete action or answer.
2. Use short numbered steps for multi-step work, with one bounded action per step.
3. State visible progress and give estimates in minutes when relevant.
4. Keep lists to five items or fewer; group and rank longer material.
5. State errors as location, cause, and fix; end with one concrete next action.

Do not activate this mode automatically: the user must invoke `$i-have-adhd`.

## Demo interface prototype: [miro.com/app/board/uXjVHj5Bz8A=](https://miro.com/app/board/uXjVHj5Bz8A=/)/

# Part 0:

## UI Generation Rules

- Stack: React + Vite + TypeScript + Tailwind; interface in Vietnamese, locale vi-VN
- BEFORE writing any UI: read .agents/skills/web-design-guidelines/command.md and treat the rules as mandatory (ignore Hydration Safety and Content & Copy)
- Source of requirements: docs/PRD.md (UC-06, UC-07). Do not invent features outside the PRD; if something is missing in the PRD, note it in docs/open-questions.md
- After finishing each screen: run the web-design-guidelines skill on the newly created files, fix all errors before marking as complete
- Every screen must have all states: loading, empty, error, long data

# Part 1:

Read docs/PRD_Report.md sections 4.6 (UC-06) and 4.7 (UC-07), and read command.md of the web-design-guidelines skill.

Don't write any code yet. Create docs/ui-design.md including:

1. Sitemap + routes (URL reflects state: filter, report period, page → query params)
2. For each screen: purpose, components, states (loading/empty/error/long), main flow + corresponding A/E in PRD
3. Table mapping "PRD requirements → applied Vercel rules" (e.g., E2 highlights missing fields → inline error + focus first error)
4. List of shared components (Button, Field, StatusBadge, EmptyState, ConfirmDialog, AudioPlayer, DataTable)
5. PRD points that are unclear → docs/open-questions.md (don’t decide on your own)

Expected screens: LocationList, LocationForm (UC-06), AnalyticsFeedback (UC-07). Log in only if PRD requires it; currently PRD just mentions "Admin has been authenticated," so note it in open-questions.

# Part 2:

According to docs/ui-design.md, create the project framework and shared components in src/components/ui/.
You must follow Vercel rules:

- Button is <button></button>; navigation uses <a></a>/; don’t use div with onClick
- Field: <label></label> linked to control (htmlFor), show errors inline, have aria-live="polite" for errors/toast
- Focus-visible is clear on all interactive elements, don’t use plain outline-none
- Don’t use transition: all; respect prefers-reduced-motion
- Icon-only button has aria-label, decorative icons have aria-hidden
- ConfirmDialog: overscroll-behavior: contain; cancel/delete actions must confirm or have undo
- Format date/number/percent via Intl (vi-VN), put in src/lib/format.ts
- Include skip link and hierarchical headings in the main layout
  Only create files, don’t write the business screens. Then run a skill review on src/components/ui and fix the issues.

# Part 3:

Write src/pages/LocationForm.tsx (and child components) according to UC-06 and docs/ui-design.md.

Features (as per PRD):

- Info: name, category, latitude, longitude, geofence radius
- Scripts by (language × FULL/SHORT), tied to locationId + languageCode + scriptType
- Upload audio per language, validate format, with audio preview
- Two buttons: "Save Draft" (DRAFT, not mandatory to fill all fields) and "Submit for Review" (PENDING_REVIEW, validate required fields: name, category, coordinates, radius)
- Edit existing location: load data into form; allow changing audio for ONE language without affecting other data
- Clearly show: the approved version running on the app is retained until the replacement is approved (BR4)

Error handling (as per PRD):

- E2 missing required field: block submission, highlight field, inline error next to field, focus on first erroneous field
- E1 wrong audio format or upload error: show error, KEEP all form data, allow selecting a new file/try again

Vercel-specific rule:

- Coordinate input: proper type/inputmode (inputmode="decimal") name has meaning, autocomplete="off", spellCheck={false} - Don’t block paste; placeholder ends with "…" including an example (e.g., 10.7769…) - Send button is only disabled while the request is in progress, with a spinner and text "Sending…" - Warning when leaving the page with unsaved changes (beforeunload / router guard) - Upload: besides drag-and-drop, must have a file select button accessible via keyboard - Audio preview player: controls must be keyboard accessible - Long script textarea: layout shouldn’t break Use mock data in src/mocks/, don’t call real APIs yet. When done, run the web-design-guidelines skill on the files you just created and edited.

# Part 4:

Implement `src/pages/AnalyticsFeedback.tsx` in accordance with UC-07 and `docs/ui-design.md`.

Functionality (per PRD):

- Default load data for the last 30 days; reload when a different reporting period is selected (A1).
- 4 metrics: Total Playbacks, Top Locations, Language Distribution, Offline/Online Usage Ratio.
- Feedback list: category, content, rating, status; filter by category and status (A2).
- View feedback details; update status following the strict sequence NEW → IN_REVIEW → RESOLVED
  (no skipping steps) and display the update timestamp.
- "Export Report" button (.xlsx) for the current period (A3).
- E1 (No data): Display an "Empty State" instead of charts/records.
- E2 (Export error): Show error notification and a "Retry" button.

Specific Vercel rules to apply:

- Reporting period, filters, open feedback item, and page number → synchronize with URL query parameters.
- Numeric data: use `font-variant-numeric: tabular-nums`; format numbers/dates using `Intl` (vi-VN locale).
- Charts: provide a tabular alternative or description for screen readers; do not convey information solely through color.
- Long feedback lists: use truncation/line-clamp; flex children must have `min-w-0`; virtualize or paginate if exceeding 50 rows.
- Status changes: announce via `aria-live="polite"`.
- Export button: show "Exporting..." state; preserve component state on error.
  Use mock data; do not call the actual API yet. Upon completion, run the `web-design-guidelines` skill check and apply necessary fixes.

# Part 5: Review

Run the `web-design-guidelines` check (reading the local `command.md`) on the list of recently written files.

Cross-reference with `docs/ui-design.md` and the PRD:

1. Report errors in the format `file:line - issue`, grouped by priority (P0/P1/P2).
2. Fix P0 issues first, then P1, while preserving existing logic, props, and styles.
3. Re-run the check and report the status: resolved / remaining / newly introduced issues.
   Stop once all P0 and P1 issues are resolved and all PRD requirements for the screen are met. If P0 issues persist after the third round, stop and summarize the reasons.
