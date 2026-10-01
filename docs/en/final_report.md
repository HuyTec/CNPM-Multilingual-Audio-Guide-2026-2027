# Overall Assessment

major_changes. The MVP scope is coherent and the diagrams are structurally usable, but three end-to-end boundaries must be resolved before this becomes the implementation baseline: approved-content publication, UC-05 notification behavior, and the UC-05 to UC-02 handoff.

# What Is Already Good

The seven use cases cover the tourist journey and essential administration without obvious scope creep. Fallbacks are concrete, business rules protect important content and update boundaries, and all seven activity graphs have logical start-to-end paths with no broken XML edge references.

# Critical Issues

None classified as critical. No source corruption or unrecoverable graph defect was found.

# Important Improvements

1. Define the owner and contract for approval, publication, and synchronization of APPROVED content. Publish/install only an atomic approved package containing matched script and audio assets.
2. Retain UC-05's written MVP boundary: selecting a notification/banner opens Location Details only. Remove UC-05 as an automatic UC-03 playback trigger.
3. Let UC-02 start from any valid Location Details navigation carrying `locationId`, including UC-05 notification selection.
4. Repair UC-02 approval/fallback ordering and UC-03 pause/device-disconnect paths.
5. Choose one offline first-run baseline and state how the location database is available.

# Cross-Use-Case Consistency

UC-02 requires APPROVED content but UC-06 ends at DRAFT/PENDING_REVIEW; UC-04 must therefore be restricted to approved published packages. UC-05 supplies a valid `locationId` but UC-02 currently only names UC-01 as its source. Current language, feedback submission, and analytics events are also assumed external dependencies and should be assigned an owner/contract without expanding this MVP.

# UML / Activity Diagram Issues

Do not add `<<include>>` or `<<extend>>` among UC-01, UC-02, and UC-03; these are data/state handoffs. Add a system boundary to the use-case diagrams.

UC-02 must decide approval before loading text and model Selected Language -> EN -> VI explicitly. UC-03 should use a compact event loop for playback controls and interruptions; `Remain paused` must reach a final node, and device disconnect must not auto-resume. Add UC-01 radius expansion, UC-04 delete cancellation, UC-06 approved-version protection, and an independent optional-flow layout for UC-07.

# Documentation Issues

Normalize UC IDs, activity references, step numbering, business-rule IDs, actor labels, and terminology. Correct the PRD title/opening, separate metadata tables from flow tables, repair UC-03 formatting, replace diagram authoring notes with final references, and align the README scope and PRD link with the actual product.

# XML Issues

The XML has no broken edge references or logical disconnected nodes. Required fixes are semantic: UC3's non-final `Remain paused` endpoint and disconnect-to-playback loop, UC1's missing radius-expansion path, UC4's delete-cancellation path, and UC7's no-data route that currently proceeds to feedback records.

# Recommended Minimal Changes

1. Add one approval/publication/package-version dependency note with an owner.
2. Align UC-03/UC-05 to notification -> Location Details -> UC-02 only.
3. Correct UC2/UC3 activity paths.
4. Make the offline baseline explicit.
5. Perform one terminology, numbering, and README cleanup pass.

# Optimized Activity Flows

- UC-02: Location Details -> resolve language/mode -> lookup APPROVED content -> selected language, EN, VI fallback -> load/display or unavailable outcome.
- UC-03: Play -> matching audio or TTS -> playing event loop -> explicit focus-restored resume or paused final; disconnect always pauses.
- UC-04: Update verifies then atomically replaces an approved package; every cancel/failure preserves current content. Delete has its own confirm/cancel branch.
- UC-05: Geofence checks -> notification/banner -> selection opens Location Details -> UC-02.
- UC-06/UC-07: Preserve the published version while edits await review; make dashboard actions independently optional.

# Files That Should Be Updated

- `docs/PRODUCT REQUIRMENTS DOCUMENT.md` — lifecycle, handoff, offline contracts, terminology, and structure.
- `docs/Software Engineer.drawio.xml` — corrected activity paths, labels, and use-case system boundaries.
- `README.md` — actual product scope and authoritative PRD link.
