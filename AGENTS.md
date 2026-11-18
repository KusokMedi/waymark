# AGENTS.md — Waymark

## 1. Project Overview

Waymark is an independent, local-first Android application for monitoring official U.S. Department of State Travel Advisories.

The project consists of:

- A native Android application.
- Real configurable Android home-screen widgets.
- A static project website hosted on GitHub Pages.
- Technical and user documentation.
- Automated build, testing, and release workflows.

**Android Application ID:** `com.kusokmedi.waymark`

**License:** GPL-3.0.

**Primary repository:** The existing Waymark GitHub repository.

**Primary language:** English.

The project must not require Waymark-operated backend infrastructure, user accounts, subscriptions, or paid services.

## 2. Core Principles

These principles are mandatory and take priority over implementation convenience.

### 2.1 Local-first architecture

All user-specific information must remain on the user's device.

This includes:

- Watchlists.
- Favorite countries.
- Application settings.
- Widget configurations.
- Notification preferences.
- Advisory history.
- Synchronization metadata.
- Locally generated data.

Never introduce:

- User accounts.
- Waymark backend services.
- Cloud synchronization.
- Remote analytics.
- Tracking or telemetry.
- Advertising SDKs.
- Third-party services that receive user-specific information.

The application may communicate directly with the official public data source to retrieve Travel Advisory information.

Do not use GitHub Actions, GitHub Pages, Vercel, or another hosting service as a proxy or backend for user data.

### 2.2 Data integrity

Never invent advisory information.

Never interpret missing, malformed, unavailable, or unknown data as Level 1.

Keep these states distinct:

- Valid current advisory.
- Cached advisory.
- Unknown advisory.
- Synchronization failure.
- No available advisory record.

When synchronization fails, preserve the last valid cached information and display its last successful update time.

If no valid information exists, display `Unknown`.

Never destroy previously downloaded information because a network request failed.

### 2.3 Official sources

The primary source is the official U.S. Department of State Travel Advisories website and its verified public data services.

Known research candidates:

- https://travel.state.gov/en/international-travel/travel-advisories.html
- https://services6.arcgis.com/R6wlO6UHmSzqm9Vs/ArcGIS/rest/services/Travel_Advisory_Levels__%282024%29_View_Layer/FeatureServer
- https://cadataapi.state.gov/

These are research starting points, not guaranteed permanent API contracts.

Verify current availability, endpoints, schema, pagination, rate limits, update behavior, and licensing before implementation.

Do not use unofficial fallback sources.

### 2.4 Preserve existing work

Before modifying the repository:

1. Inspect the entire repository.
2. Inspect the current Git state.
3. Read existing documentation and project instructions.
4. Identify the existing website, assets, workflows, and application code.
5. Identify useful components that can be reused.
6. Prepare a migration plan.

Do not blindly delete or replace existing project files.

Never rewrite Git history, delete user data, remove branches, force-push, or perform destructive Git operations without explicit authorization.

## 3. Technology and Architecture

Use native Android development.

Preferred technologies:

- Kotlin.
- Jetpack Compose.
- AndroidX.
- Room.
- DataStore.
- WorkManager.
- Kotlin Coroutines and Flow.
- A maintained HTTP client.
- Jetpack Glance or RemoteViews for home-screen widgets.

Select the best implementation after evaluating actual platform requirements.

Do not adopt a technology merely because it is familiar.

The application must support Android 8.0 (API 26) and later.

Use current stable compatible dependencies and avoid unnecessary libraries.

Keep dependency versions centralized where appropriate.

### Architecture requirements

Use a maintainable layered architecture.

Separate:

- Presentation.
- Domain logic.
- Data access.
- Network communication.
- Local persistence.
- Background synchronization.
- Widget configuration and rendering.
- Notification handling.

Keep business logic independent of Android UI components wherever practical.

Use dependency injection only if it provides meaningful architectural value.

Avoid unnecessary abstraction and overengineering.

## 4. Travel Advisory Data Model

The application must support all countries available in the verified official source.

The data model should accommodate:

- Country name.
- ISO identifiers.
- Flag asset mapping.
- Advisory level.
- Advisory title.
- Advisory summary.
- Official advisory URL.
- Official publication or update date.
- Regional warnings.
- Regional warning descriptions.
- Source metadata.
- Last successful synchronization timestamp.

Preserve the distinction between official advisory publication dates and local synchronization timestamps.

### Advisory level semantics

Known level codes include:

| Code | Meaning |
|---|---|
| 1 | Exercise Normal Precautions |
| 10 | Exercise Normal Precautions - Contains Areas with Higher Security Risk |
| 2 | Exercise Increased Caution |
| 20 | Exercise Increased Caution - Contains Areas with Higher Security Risk |
| 3 | Reconsider Travel |
| 30 | Reconsider Travel - Contains Areas with Higher Security Risk |
| 4 | Do Not Travel |

Codes 10, 20, and 30 are special semantic values. They must not be interpreted as ordinary numerical advisory levels.

Implement explicit, documented mapping logic.

Do not rely on naive numeric sorting.

Unknown future values must be handled safely and must not be silently converted into another level.

Regional information must retain its relationship to the relevant country and source record. Never invent geographic relationships.

## 5. Synchronization

Use WorkManager for persistent background synchronization.

Requirements:

- Synchronize when the application starts.
- Provide manual synchronization.
- Support background synchronization.
- Use any available network connection, including mobile data and metered connections.
- Retry recoverable failures with exponential backoff.
- Balance battery usage and data freshness.
- Respect Android background execution restrictions.

Do not promise exact background execution times.

Remember that Android may delay WorkManager jobs.

The first synchronization must:

1. Retrieve the available country catalogue.
2. Retrieve detailed information for selected countries.
3. Store valid records locally.
4. Establish a baseline for future change detection.
5. Avoid sending initial synchronization notifications.

Use incremental updates when supported by the source.

Do not assume that the source supports incremental updates. Verify this first.

Synchronization must be safe against duplicate execution and partial failures.

Use transactions where appropriate.

## 6. Local Storage and History

Use Room for structured local data and DataStore for application preferences.

Persist:

- Country catalogue.
- Selected countries.
- Advisory records.
- Regional warnings.
- Advisory history.
- Synchronization state.
- Notification preferences.
- Widget configurations.

History retention is indefinite.

Do not automatically remove historical records.

Database migrations must be implemented and tested.

Never introduce destructive database migrations that erase user data without a deliberate migration strategy.

### Backup and restore

Support manual backup and restore.

Required formats:

- JSON.
- ZIP.

Backups should contain the relevant local application data and a schema/version identifier.

Validate imported files before modifying the database.

Use safe, transactional import behavior.

Never silently overwrite existing information.

Provide clear error messages for invalid or incompatible backups.

## 7. Notifications

Notifications are optional and must be enabled through user consent.

Ask the user whether they want notifications during onboarding or the first appropriate setup flow.

Request the Android notification permission when required.

Support:

- Advisory worsening.
- Advisory improvement.
- New regional warnings.
- Significant advisory detail changes.

Requirements:

- Independent notification preferences per country.
- One notification per country and event.
- Short, understandable messages.
- Standard Android notification sound.
- Event-specific notification priority.
- No notifications during initial synchronization.
- No quiet-hours feature.

Implement reliable event deduplication.

Do not repeatedly notify about unchanged records.

Do not send notifications when the application cannot establish that a meaningful change occurred.

## 8. Android Home-Screen Widgets

Real Android App Widgets are a central Waymark feature.

Do not implement widgets as web pages, browser shortcuts, or simulated in-app components.

Support:

- Compact.
- Medium.
- Large.
- Customizable widget configurations.

Each widget instance must maintain independent configuration.

Users must be able to configure:

- Selected countries.
- Country order.
- Colors.
- Background.
- Visible date information.
- Layout.
- Displayed elements.

Provide two customization levels:

1. Preset layouts with configurable properties.
2. A more advanced widget builder that allows users to select available elements and arrange their presentation within Android widget limitations.

Do not promise arbitrary layouts or interactions that the Android launcher API cannot support.

Every widget should display:

- Country flag.
- Advisory level.
- Country name.
- Advisory date.
- Last update information.

Large widgets should display all selected countries when the available space permits.

Support:

- System theme.
- Launcher resizing.
- Automatic refresh.
- Manual refresh.
- Tapping a country to open its detail page.
- An appropriate empty state prompting the user to add countries.

Widget configurations must survive process restarts and device reboots.

Use the widget ID as the identity of each independent configuration.

Test widget behavior on different sizes and Android versions.

## 9. User Interface and Experience

The interface must be in English.

Design direction:

- Modern Seal-inspired visual language.
- Native Android experience.
- Jetpack Compose.
- System-controlled light and dark themes.
- Dynamic colors where supported.
- Bottom navigation.
- Comfortable information density.
- Custom flag assets.
- Rich but restrained animations.
- Simple settings screens.

Do not copy third-party logos, proprietary assets, or protected designs.

Create original Waymark branding and a coherent visual identity.

### Main navigation

The application should include appropriate sections for:

- Watchlist.
- Country catalogue / Search.
- History.
- Settings.

The primary screen is the user's watchlist.

### Country details

Provide a country dashboard containing:

- Current advisory.
- Advisory publication date.
- Summary.
- Official source link.
- Regional information.
- Last synchronization time.
- Historical changes.
- Charts.

Use accessible colors, typography, touch targets, and descriptions.

Never communicate risk using color alone.

## 10. Website

Keep the project website as part of the repository.

Deploy it through GitHub Pages.

The website must be a modern product presentation website with an interactive demonstration.

The website must not require a backend.

The demonstration must use clearly labeled sample data.

Never present sample information as real current travel advice.

The website should include:

- Project introduction.
- Features.
- Application screenshots.
- Interactive sample demonstration.
- Data sources.
- Privacy Policy.
- Documentation.
- Changelog.
- Download / Releases links.
- GitHub repository link.
- About section.

Use actual screenshots supplied by the project owner.

Do not fabricate screenshots or pretend that an unfinished application is complete.

Keep the website responsive and accessible.

## 11. Documentation

All project documentation must be written in English.

Maintain:

- README.md.
- Architecture documentation.
- Privacy Policy.
- User Guide.
- Contributing guide.
- Changelog.
- Data Sources / API documentation.

Documentation must describe actual implemented behavior.

Do not document planned features as completed.

Update documentation when architecture or behavior changes.

## 12. GitHub Actions and Releases

Use GitHub Actions for:

- Android build.
- Automated tests.
- Lint and code quality checks.
- Release builds.
- Release artifact publication.

Use Semantic Versioning.

A Git tag should trigger the appropriate release workflow.

Release APKs through GitHub Releases.

Keep the GitHub Pages deployment workflow separate where practical.

Do not introduce paid CI services.

Never expose secrets in logs or commit them to the repository.

Do not use CI as a runtime backend.

## 13. Testing and Quality

Aim for comprehensive automated testing.

Include:

- Unit tests.
- Repository and data-layer tests.
- Database tests.
- Migration tests.
- Synchronization tests.
- API parsing tests.
- Error-handling tests.
- Notification tests.
- Widget tests.
- UI tests.
- Backup and restore tests.
- Compatibility tests.
- Performance checks.

Test at least:

- No internet connection.
- Slow connections.
- HTTP errors.
- Malformed responses.
- Missing fields.
- Unknown advisory codes.
- Duplicate source records.
- Partial synchronization.
- Application process termination.
- Database migration.
- Widget recreation.
- Empty watchlists.
- Android notification permission denial.

Tests must not depend on live external APIs unless explicitly designed as separate integration checks.

Use fixtures and mocked network responses for deterministic tests.

Do not claim a test passed unless it actually ran successfully.

## 14. Privacy and Legal Accuracy

Waymark is independent and is not affiliated with or endorsed by the U.S. Department of State.

Clearly explain that U.S. Travel Advisories are designed for U.S. citizens and are not universal safety ratings for citizens of every country.

Link to official source material.

Avoid presenting the advisory level as a guarantee of safety or danger.

The Privacy Policy must accurately describe all network communication and local storage.

## 15. Development Workflow

Work autonomously within the project's established requirements.

For every substantial task:

1. Inspect relevant existing code.
2. Understand the current architecture.
3. Identify affected components.
4. Implement the smallest coherent solution.
5. Add or update tests.
6. Run relevant checks.
7. Review the result.
8. Update documentation.

Prefer complete, working features over placeholder implementations.

Do not leave important functionality as TODOs while claiming the project is finished.

Do not hide failures or weaken tests to make CI pass.

Do not introduce unnecessary dependencies.

Use clear naming and maintainable code.

Keep changes focused and avoid unrelated rewrites.

## 16. User Communication

The project owner expects high autonomy.

Do not request approval for every routine implementation decision.

Make reasonable technical decisions independently and document important trade-offs.

Ask for clarification only when a genuinely blocking ambiguity exists or when an action would be destructive, irreversible, costly, or outside the stated requirements.

Do not purchase services, create paid infrastructure, or incur costs.

Do not delete user data or rewrite Git history.

## 17. Definition of Done

A feature is not complete merely because its UI exists.

A feature is complete only when:

- Its behavior is implemented.
- Its data handling is correct.
- Relevant error cases are handled.
- Relevant tests exist and pass.
- Documentation is updated.
- It integrates correctly with the rest of the application.

The project is ready for release only when:

- The Android application builds successfully.
- The website builds successfully.
- Automated tests pass.
- Lint and quality checks pass.
- Widgets work on supported Android versions.
- Local-only requirements are verified.
- Backup and restore work.
- No critical TODOs or placeholder functionality remain.
- Release artifacts can be generated through the documented workflow.

**Primary objective: Build a reliable, polished, fully functional native Android application and preserve the Waymark website as a separate, useful project website.**
