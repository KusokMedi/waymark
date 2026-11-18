# AGENTS.md — Waymark

> This file defines the permanent engineering rules, project constraints, architecture principles, development standards, and autonomy boundaries for all AI agents working on Waymark.
>
> Every agent must read this file before making changes. These instructions apply to the entire repository unless a more specific instruction file overrides them without contradicting the project-wide requirements.

---

# 1. Project Identity

## 1.1. Basic information

- **Project name:** Waymark
- **Repository slug:** `waymark`
- **Project type:** Open-source Travel Advisory Dashboard
- **Repository visibility:** Public
- **License:** GNU General Public License v3.0 (GPL-3.0)
- **Primary language:** English
- **Primary data provider:** U.S. Department of State Travel Advisories
- **Hosting:** GitHub Pages
- **Backend:** None required
- **Infrastructure budget:** Zero-cost by default
- **Primary design direction:** Modern Android UI, inspired by Material Design 3 and Material You
- **Development environment:** OpenCode with autonomous AI-assisted engineering

## 1.2. Project purpose

Waymark is an open-source application that displays travel advisory information published by the U.S. Department of State.

Its primary purpose is to let users quickly understand the current advisory level for destinations, inspect the original advisory, and maintain a personal list of destinations they care about.

Waymark must be more than a simple static table. It should be a polished, reliable, responsive dashboard with:

- A global destination directory.
- Search and filtering.
- Multiple presentation modes.
- Advisory details.
- Regional risk indicators where supported by source data.
- Favorites and a personal watchlist.
- Automatic data updates.
- Manual refresh functionality where technically supported.
- Light, dark, and system themes.
- A separate project website.
- Public documentation and transparent data provenance.

## 1.3. Product positioning

Suggested tagline:

**Know before you go.**

Suggested short description:

**A modern, open-source dashboard for U.S. Department of State travel advisories.**

The project must be presented as an independent application, not as an official U.S. government product.

Never imply that Waymark is affiliated with, endorsed by, or operated by the U.S. Department of State.

---

# 2. Non-Negotiable Project Decisions

The following decisions are confirmed and must not be silently changed.

| Decision | Requirement |
|---|---|
| Project name | Waymark |
| Repository slug | waymark |
| Repository visibility | Public |
| License | GPL-3.0 |
| Interface language | English |
| Website language | English |
| Hosting | One GitHub Pages deployment |
| Website location | Repository Pages root |
| Application location | `/app/` |
| Default destinations | Finland, Estonia, Latvia, Lithuania |
| Design direction | Material 3-inspired modern Android UI |
| Theme behavior | System by default, with Light and Dark options |
| Hosting budget | Free |
| Backend requirement | No permanent backend |
| Initial application data | All destinations supported by the verified data source |

The GitHub Pages URLs must follow this structure:

```text
https://<github-username>.github.io/waymark/
https://<github-username>.github.io/waymark/app/
```

Do not hardcode a personal GitHub username or custom domain into application logic.

The application and website must work under the repository subpath. Do not assume that the application is hosted at `/`.

---

# 3. Agent Autonomy

## 3.1. General principle

Operate as an autonomous senior software engineer.

Do not behave as a passive assistant that merely suggests solutions, writes isolated snippets, or waits for instructions after every implementation step.

You are expected to investigate, plan, implement, test, debug, document, commit, and deploy the project.

Prefer completing a technically sound task independently over interrupting the user with unnecessary questions.

## 3.2. Authorized autonomous actions

You may independently:

- Inspect and modify the repository.
- Choose the technology stack.
- Design the application architecture.
- Create, edit, move, and remove project files when necessary.
- Install and update dependencies.
- Implement application features.
- Implement the project website.
- Write and run tests.
- Perform refactoring.
- Fix bugs and type errors.
- Configure formatting and linting.
- Create GitHub Actions workflows.
- Configure GitHub Pages.
- Create a public GitHub repository named `waymark`, provided the authenticated account is the intended destination and the repository name is available.
- Create Git commits.
- Push project changes.
- Prepare and publish GitHub releases.
- Maintain README, changelog, and developer documentation.
- Research official data sources and update data integration when necessary.

Use the available GitHub integration or authenticated GitHub CLI when appropriate.

Before creating a remote repository, check whether it already exists. Never overwrite or repurpose an unrelated repository.

## 3.3. Actions that are prohibited without explicit authorization

Never:

- Purchase services or subscriptions.
- Enable paid infrastructure.
- Create paid API usage.
- Introduce paid dependencies or external services that require billing.
- Publish private credentials or secrets.
- Publish personal information.
- Replace the selected GPL-3.0 license.
- Force-push or rewrite shared Git history.
- Delete the remote repository.
- Delete unrelated branches or tags.
- Run destructive commands against an unknown working directory.
- Use `git reset --hard`, `git clean -fd`, or equivalent destructive operations without first establishing that the affected changes are disposable and user-owned work will not be lost.
- Modify unrelated projects.
- Claim that a deployment, release, or repository creation succeeded unless it has been verified.

Full autonomy does not mean permission to spend money, destroy user data, or misrepresent results.

## 3.4. Handling blockers

When an external dependency or permission is unavailable:

1. Identify the exact blocker.
2. Complete every independent task that can still be completed.
3. Do not invent credentials, API responses, or deployment results.
4. Do not repeatedly ask the user for the same information.
5. Explain what remains blocked and what action is required.

A missing GitHub permission must not prevent local development, testing, or documentation.

---

# 4. Communication and Language

Use these language rules:

| Area | Language |
|---|---|
| Conversations with the user | Russian |
| Application interface | English |
| Website | English |
| README | English |
| Documentation | English |
| Code comments | English |
| Variable and function names | English |
| Commit messages | English |
| Release notes | English |

Do not translate application copy into Russian unless explicitly requested.

When reporting technical work to the user, explain important architectural decisions in Russian using clear language.

Do not send long progress messages after every small change. Report meaningful milestones, blockers, architectural changes, and completed phases.

---

# 5. Data Sources and Trustworthiness

## 5.1. Official source

The primary reference is the U.S. Department of State Travel Advisories website:

https://travel.state.gov/en/international-travel/travel-advisories.html

The official site describes Travel Advisories as assessments of risks and recommended precautions for U.S. citizens, nationals, and legal residents.

This distinction is important.

Waymark must display a clear explanation that the advisories are designed for U.S. travelers and do not necessarily represent the risk assessment applicable to travelers of every nationality.

The application must not claim to provide a universal or personalized safety assessment.

## 5.2. ArcGIS source candidate

The following ArcGIS FeatureServer is a known candidate for structured advisory data:

```text
https://services6.arcgis.com/R6wlO6UHmSzqm9Vs/ArcGIS/rest/services/Travel_Advisory_Levels__%282024%29_View_Layer/FeatureServer
```

A layer currently visible in this service is:

```text
Layer ID: 54
Layer name: Travel Advisory Levels (NEW)
```

Layer metadata currently exposes fields including:

- `OBJECTID`
- `NAME`
- `LEVEL_`
- `URL`
- `ADVDATE`
- `ISO_3`
- `GlobalID`
- `EditDate`
- `Editor`
- `AD_OD_Status`
- `AD_OD_Text`
- `AD_OD_Date`

These fields are a research starting point, not a guarantee that the schema will remain unchanged.

The agent must inspect the live service before relying on it.

Do not assume that a URL, field, layer ID, domain, or response format will remain available forever.

## 5.3. Other sources

Investigate whether the following source provides a suitable supported API:

https://cadataapi.state.gov/

Inspect available official documentation and determine whether it is appropriate for the application.

Do not introduce an unofficial scraping service when an official structured source is available.

## 5.4. Source selection requirements

Before implementing the production adapter, establish:

1. Whether the source is operated or maintained by the relevant official organization.
2. Whether the data actually represents the advisories shown on the official website.
3. Whether every destination can be retrieved.
4. Whether records contain stable identifiers.
5. Whether dates can be reliably interpreted.
6. Whether all known advisory codes are represented.
7. Whether there are multiple records per destination.
8. Whether country and subnational records can be distinguished.
9. Whether CORS allows direct browser access.
10. Whether request limits or usage restrictions apply.
11. Whether the source requires attribution.
12. Whether the source exposes changes or update timestamps.

Document the findings in:

`docs/DATA_SOURCES.md`

If there are several plausible sources, compare their reliability, completeness, accessibility, and maintenance characteristics.

Choose a primary source and document any fallback.

Never silently combine incompatible sources or make their records look interchangeable.

---

# 6. Advisory Data Model

## 6.1. Standard advisory levels

The familiar U.S. Department of State advisory levels are:

| Level | Official description |
|---:|---|
| 1 | Exercise Normal Precautions |
| 2 | Exercise Increased Caution |
| 3 | Reconsider Travel |
| 4 | Do Not Travel |

The ArcGIS data currently exposes the following coded values:

| Source code | Source description |
|---:|---|
| 1 | Exercise Normal Precautions |
| 10 | Exercise Normal Precautions - Contains Areas with Higher Security Risk |
| 2 | Exercise Increased Caution |
| 20 | Exercise Increased Caution - Contains Areas with Higher Security Risk |
| 3 | Reconsider Travel |
| 30 | Reconsider Travel - Contains Areas with Higher Security Risk |
| 4 | Do Not Travel |

Do not treat these seven codes as seven ordinary advisory levels.

Values `10`, `20`, and `30` are special coded values whose descriptions explicitly mention areas with higher security risk.

Preserve their additional meaning.

Do not rely on numeric sorting or blindly divide a code by ten.

Use an explicit mapping with tests.

## 6.2. Required normalized fields

Design a normalized internal model that can preserve at least:

```ts
type AdvisoryLevel = 1 | 2 | 3 | 4;

interface AdvisoryRecord {
  id: string;

  destinationName: string;
  isoAlpha3: string | null;
  isoAlpha2: string | null;

  rawLevelCode: number | null;
  level: AdvisoryLevel | null;

  containsHigherRiskAreas: boolean;
  officialLabel: string | null;

  advisoryUrl: string | null;
  advisoryDate: string | null;

  sourceName: string;
  sourceRecordId: string | null;

  recordKind: "country" | "region" | "unknown";
}
```

This is a conceptual baseline, not an instruction to copy the exact interface without examining the real data.

Expand or adjust it when the live source requires additional distinctions.

Preserve meaningful source fields, especially information related to regional risk and assisted or ordered departure.

## 6.3. Regional and subnational information

A source feature may represent a country, state, region, or other geographic record.

Never assume that every row is one independent country.

Investigate how records relate to one another using:

- `ISO_3`
- `NAME`
- `LEVEL_`
- Feature identifiers
- Geometry or other geographic relationships
- Additional source metadata

If regional warnings are represented separately, preserve them in the model and show them in the corresponding destination details.

If the source supplies only a country-level special code, show the special regional-risk indication without inventing names or locations for the affected areas.

Do not fabricate a country-to-region relationship.

If a safe country-level aggregation is not possible, show the source records distinctly and explain their classification.

## 6.4. Unknown and missing data

The following states must remain distinct:

- A valid Level 1 advisory.
- A valid Level 2 advisory.
- A valid Level 3 advisory.
- A valid Level 4 advisory.
- A special regional-risk advisory.
- No matching record.
- Unknown source code.
- Missing advisory date.
- Unavailable source.
- Stale cached data.
- Invalid source response.

Never convert missing data, `null`, an unknown code, or an API failure into Level 1.

If a record contains an unknown level code, preserve the raw value and show an understandable unknown or unsupported state.

Do not hide it, silently discard it, or invent a mapping.

## 6.5. Dates

Treat source dates carefully.

For ArcGIS date fields, inspect the actual returned JSON and determine whether the value is a Unix timestamp in milliseconds.

Use tested conversion logic.

Do not allow UTC-to-local-time conversion to accidentally move a date-only advisory to the previous or next calendar day.

Display:

- Advisory publication/update date.
- Last successful data synchronization time.
- Last attempted synchronization time, where relevant.

These values must not be confused with one another.

---

# 7. Country and Territory Identity

Country names are not sufficiently reliable as unique identifiers.

Prefer stable ISO codes when valid.

Requirements:

- Use ISO Alpha-3 from the source when available and valid.
- Resolve Alpha-3 to Alpha-2 using a maintained and tested mapping library.
- Never derive a flag from an unverified or malformed code.
- Provide a neutral fallback icon for destinations without a supported flag.
- Preserve source names.
- Handle duplicate or ambiguous names.
- Handle territories and destinations that do not have a standard country code.
- Search should support common country names and the official source name where possible.

Do not hardcode only the four initial countries.

Finland, Estonia, Latvia, and Lithuania are the initial dashboard destinations, not the complete supported directory.

---

# 8. Application Features

The application must include the following functional areas.

## 8.1. Dashboard

The initial screen should present the user's selected destinations.

Initial default destinations:

- Finland
- Estonia
- Latvia
- Lithuania

Users must be able to add and remove destinations.

The application should provide a clear overview without requiring the user to open every destination individually.

## 8.2. All destinations

Provide a complete directory of destinations available from the verified source.

Include:

- Search.
- Sorting.
- Filtering.
- Level indicators.
- Country flags where possible.
- A clear state for unknown or missing data.

## 8.3. Search

Support searching by destination name.

The search should be:

- Case-insensitive.
- Responsive.
- Able to handle minor differences in input.
- Consistent across list and card views.

Do not add fuzzy-search dependencies unless they provide a meaningful improvement.

## 8.4. Sorting and filtering

Support:

- Sorting alphabetically.
- Sorting by advisory severity.
- Filtering by levels 1–4.
- Filtering for entries with higher-risk-area indicators.
- Showing favorites only.

Severity ordering must use normalized advisory meaning, not raw numeric codes.

At the same level, a special regional-risk indication must remain distinguishable.

## 8.5. Presentation modes

Provide at least two views:

**List View**

A compact, information-dense view with flags, names, advisory levels, dates, and relevant indicators.

**Card View**

A more visual view with clearer surfaces, spacing, and a stronger information hierarchy.

Users must be able to switch between the views.

Persist the preference locally.

## 8.6. Destination details

A destination detail view should show available information such as:

- Destination name.
- Flag or fallback icon.
- Advisory level.
- Official advisory description.
- Higher-risk-area indicator, if applicable.
- Advisory date.
- Relevant regional records, if reliably available.
- Additional departure-related information, if supplied and verified by the source.
- Link to the original advisory.
- Source attribution.

Never fabricate descriptions or regional details.

## 8.7. Favorites

Users must be able to save destinations to a personal favorites list.

Use browser-local persistence.

Favorites must survive ordinary page reloads.

Handle malformed, outdated, or unavailable local data safely.

Do not introduce accounts or authentication.

## 8.8. Themes

Support:

- System
- Light
- Dark

System must be the default.

The selected explicit theme must remain selected after reload.

Avoid flashing the wrong theme before the application initializes.

---

# 9. Data Synchronization and Offline Resilience

## 9.1. Architecture

The preferred deployment is a static application.

There must be no mandatory application backend.

First investigate direct browser access to the source.

If browser CORS prevents reliable access, use a static data synchronization strategy powered by GitHub Actions.

Do not solve CORS by introducing an unknown public proxy.

## 9.2. Recommended fallback strategy

Where direct access is unavailable, implement a scheduled data update pipeline:

1. GitHub Actions periodically retrieves source data.
2. A validation script checks the response.
3. The normalized snapshot is generated.
4. The snapshot is committed only when its meaningful content changes.
5. The regular deployment workflow publishes the updated data with the application.
6. The application loads its same-origin snapshot.

The data pipeline must not require a secret API key when the official source does not require one.

The sync process must fail safely.

An unsuccessful fetch must never replace a valid snapshot with an empty file.

## 9.3. Refresh behavior

The UI must offer a way to refresh or recheck available data.

If the browser can directly refresh the official source, use that ability.

If the application only has access to a static snapshot, do not pretend that reloading the same file forces an upstream update.

Clearly distinguish:

- Reloading the currently published snapshot.
- Fetching new data from the source.
- Waiting for the scheduled synchronization pipeline.

## 9.4. Cache

Use a small, versioned local cache where useful.

Requirements:

- Store the last successful data response.
- Record when it was retrieved.
- Validate cached data before using it.
- Prefer fresh data.
- Allow stale cached data when the source is temporarily unavailable.
- Clearly indicate when displayed data is stale.
- Never mark stale data as freshly synchronized.
- Avoid uncontrolled growth of local storage.

A cache must never override a newer verified server snapshot without a clear reason.

## 9.5. Retry and rate limiting

Implement conservative request behavior:

- Reasonable request timeouts.
- Bounded retries.
- Exponential backoff when appropriate.
- Respect source rate limits.
- Avoid repeated duplicate requests.
- Avoid polling at unnecessarily high frequency.

Document the selected synchronization interval and why it is appropriate.

Do not advertise the application as an emergency-alerting service.

---

# 10. Design System and User Experience

## 10.1. Visual direction

The design must follow a modern Android-inspired approach.

Reference direction:

- Material Design 3.
- Material You.
- Modern Android application layouts.
- The general visual cleanliness and usability of applications such as Seal.

These are design references, not assets or interfaces to copy literally.

Do not duplicate third-party branding, logos, proprietary graphics, or complete layouts.

## 10.2. Visual principles

Use:

- Rounded surfaces.
- Clear hierarchy.
- Modern typography.
- Consistent spacing.
- Carefully selected accent colors.
- Subtle animation.
- Clear icons.
- Balanced density.
- Responsive layouts.
- Well-designed empty and error states.

Avoid:

- Generic dashboard templates.
- Excessive gradients.
- Excessive glassmorphism.
- Unnecessary shadows.
- Extremely large unused spaces.
- Excessive animation.
- Too many competing accent colors.
- Unnecessary borders around every element.
- Desktop-only assumptions.

## 10.3. Advisory color system

Use a consistent accessible visual language for Level 1–4.

Colors must remain understandable in both themes.

Never rely on color alone.

Every level must also have text or another visual indicator.

Special regional-risk codes need a separate visual indication that is not confused with a different ordinary advisory level.

## 10.4. Responsive behavior

Test at least:

- Small mobile screens.
- Typical mobile screens.
- Tablet layouts.
- Laptop screens.
- Wide desktop screens.

The app must remain functional without horizontal overflow.

Do not let responsive styling break the list/card switch, search, or destination details.

## 10.5. Accessibility

Implement:

- Semantic HTML.
- Keyboard accessibility.
- Visible focus states.
- Appropriate contrast.
- Accessible labels.
- Screen-reader-friendly controls.
- Reduced-motion support.
- Appropriate heading hierarchy.

Avoid custom interactive elements when semantic native controls are sufficient.

---

# 11. Technology Stack and Architecture

## 11.1. Stack autonomy

The agent is free to choose an appropriate stack.

Do not select technologies merely because they are fashionable.

Evaluate:

- Static hosting compatibility.
- GitHub Pages support.
- Maintainability.
- Build speed.
- Type safety.
- Testability.
- Accessibility.
- Bundle size.
- Dependency quality.
- Long-term maintenance cost.

A TypeScript-based stack with a static build tool is a reasonable candidate.

The choice must be justified in `docs/ARCHITECTURE.md`.

## 11.2. Architecture principles

Keep the following concerns separated:

1. Source API integration.
2. Raw response validation.
3. Data normalization.
4. Data persistence and caching.
5. Application state.
6. UI components.
7. Presentation and formatting.
8. Website.

Do not put all business logic into UI components.

Do not mix source-specific codes directly into random presentation components.

Use a dedicated mapping and normalization layer.

Keep interfaces small and understandable.

Prefer straightforward architecture over speculative abstractions.

Do not introduce a complex state-management library if simpler tools are sufficient.

## 11.3. Dependency policy

Every dependency should have a reason.

Before adding a library, consider:

- Whether native platform functionality is sufficient.
- Whether the library is maintained.
- Whether the license is compatible with GPL-3.0 distribution.
- Whether the package is disproportionately large.
- Whether it introduces security or maintenance concerns.

Avoid multiple libraries that solve the same problem.

Maintain a lockfile.

---

# 12. GitHub Pages and Repository Structure

## 12.1. Required routes

The intended deployment structure is:

```text
/waymark/       -> Project website
/waymark/app/   -> Application
```

The actual GitHub Pages hostname depends on the authenticated repository owner.

## 12.2. Base path requirements

Both builds must be configured correctly for the repository subpath.

The site must not assume `/` is the deployment root.

The application must not assume `/app/` exists at the hostname root.

Test:

- JavaScript assets.
- CSS assets.
- Icons and fonts.
- Images.
- Data files.
- Internal links.
- Application routes.
- Refreshing nested routes.
- Direct access to `/waymark/app/`.

Use a robust routing strategy compatible with GitHub Pages.

A hash-based application router or a correctly configured static fallback may be used, depending on the chosen framework.

## 12.3. Deployment workflow

Use GitHub Actions as the publishing source.

Follow the current official GitHub Pages workflow guidance:

https://docs.github.com/en/pages/getting-started-with-github-pages/configuring-a-publishing-source-for-your-github-pages-site

Use a secure workflow that:

- Builds the website.
- Builds the application.
- Combines both outputs.
- Uploads one Pages artifact.
- Deploys only from the intended branch.
- Does not deploy pull requests from untrusted contexts.
- Uses the minimum required workflow permissions.

Do not use deprecated deployment mechanisms when the official Actions-based method is available.

## 12.4. Repository structure

Choose a structure that fits the selected stack.

A reasonable starting point is:

```text
waymark/
├── .github/
│   ├── workflows/
│   ├── ISSUE_TEMPLATE/
│   └── dependabot.yml
├── app/
├── website/
├── data/
├── docs/
│   ├── ARCHITECTURE.md
│   ├── DATA_SOURCES.md
│   ├── DEVELOPMENT.md
│   └── RELEASES.md
├── screenshots/
├── AGENTS.md
├── README.md
├── CHANGELOG.md
├── CONTRIBUTING.md
├── LICENSE
└── package.json
```

Adapt this structure if necessary.

Do not create empty directories and unnecessary files simply to match a diagram.

---

# 13. Website Requirements

The project website is part of the deliverable, not an optional future idea.

It must include:

1. Home.
2. Features.
3. Screenshots.
4. Data Sources.
5. Documentation.
6. Changelog.
7. About.
8. A clear link to open the application.

## 13.1. Home

Explain:

- What Waymark does.
- Where its data comes from.
- Why the application exists.
- How to open the dashboard.

Use the tagline:

**Know before you go.**

## 13.2. Screenshots

The user will provide genuine application screenshots separately.

Do not generate fake screenshots or represent mockups as real product screenshots.

Before the user provides screenshots, create a deliberate placeholder state that can be replaced later.

## 13.3. Data Sources

Explain the source, the meaning of advisory levels, update behavior, and limitations.

Include the official source link.

## 13.4. About

State clearly that Waymark is an independent open-source project and is not an official U.S. Department of State product.

---

# 14. Documentation Requirements

Maintain the following files:

## README.md

Must include:

- Project description.
- Main features.
- Screenshots or clearly marked placeholders.
- Online application link when deployed.
- Website link when deployed.
- Data-source attribution.
- Technology overview.
- Development instructions.
- License.
- Disclaimer.

## CHANGELOG.md

Use a consistent versioned changelog.

Do not list features that are not actually implemented.

## CONTRIBUTING.md

Explain:

- How to set up the project.
- How to run it locally.
- How to run checks.
- Commit message expectations.
- Pull request expectations.
- General contribution guidelines.

## docs/ARCHITECTURE.md

Explain:

- Technology decisions.
- Application structure.
- Data flow.
- Normalization.
- Persistence.
- Static hosting approach.
- Deployment architecture.

## docs/DATA_SOURCES.md

Explain:

- Official sources.
- Source URLs.
- API endpoints.
- Verified schema.
- Known limitations.
- Advisory code mapping.
- Update frequency.
- Regional data handling.
- Attribution.

## docs/DEVELOPMENT.md

Explain setup, local development, build commands, tests, and troubleshooting.

## docs/RELEASES.md

Document the release process and versioning.

---

# 15. Licensing and Third-Party Assets

The project license is GPL-3.0.

Use the complete official license text, not a manually shortened summary.

Official source:

https://www.gnu.org/licenses/gpl-3.0.html

Check the licenses of:

- Dependencies.
- Icons.
- Fonts.
- Flag assets.
- Images.
- UI components.
- Build tools.

Do not copy assets from other projects without checking their licenses.

Keep license and attribution information where required.

Do not claim that the government's data, external maps, or third-party assets are automatically covered by the Waymark software license.

---

# 16. Testing and Quality Assurance

Testing is mandatory.

At minimum, implement tests for:

## 16.1. Data normalization

- Every known standard advisory code.
- Special regional-risk codes.
- Unknown codes.
- Null values.
- Invalid records.
- Duplicate records.
- Missing names.
- Missing ISO codes.
- Invalid advisory URLs.
- Invalid dates.
- Regional and country record relationships.
- Safe grouping and sorting.

## 16.2. Data integration

- Successful source responses.
- Empty responses.
- Unexpected response structures.
- Network errors.
- Timeout handling.
- Rate-limit responses.
- Stale cached data.
- Failed cache parsing.
- Successful replacement of cached data.

## 16.3. User functionality

- Search.
- Sorting.
- Filtering.
- Favorites.
- View switching.
- Theme persistence.
- Refresh behavior.
- Empty states.
- Error states.
- Destination details.

## 16.4. Build and deployment

- Production build.
- Correct application base path.
- Correct website base path.
- Data asset location.
- Asset URL resolution.
- Nested route handling.
- Deployment artifact structure.

Do not declare a task complete merely because it compiles.

Run the relevant tests after each meaningful implementation phase.

---

# 17. Security and Reliability

Follow least-privilege principles.

- Never expose tokens in frontend code.
- Never commit secrets.
- Never use `dangerouslySetInnerHTML` for untrusted data without an explicit, secure sanitization strategy.
- Treat all source responses as untrusted input.
- Validate and normalize source records.
- Validate external links.
- Prefer HTTPS.
- Avoid adding unnecessary browser permissions.
- Avoid unnecessary third-party scripts.
- Keep workflow permissions minimal.
- Keep dependencies updated responsibly.

Never let source data execute as HTML or JavaScript.

Never allow an invalid response to overwrite the last known valid snapshot.

---

# 18. Git and Commit Standards

Use Conventional Commits.

Examples:

```text
feat: add destination search
feat: implement advisory details
fix: preserve special regional risk codes
fix: resolve github pages base path
docs: document source data architecture
test: add advisory normalization coverage
refactor: separate source adapter from UI
chore: update dependencies
ci: add scheduled data synchronization
```

Rules:

- Commit logically related changes together.
- Avoid meaningless commits.
- Do not commit temporary logs or build artifacts.
- Do not commit secrets.
- Keep the working tree clean when practical.
- Do not amend or rewrite pushed commits unless the user explicitly asks.
- Inspect `git status` before destructive operations.

Create meaningful milestones in Git history.

---

# 19. Releases

Use Semantic Versioning:

```text
MAJOR.MINOR.PATCH
```

Suggested development progression:

- `v0.1.0` — Project foundation.
- `v0.2.0` — Data integration.
- `v0.3.0` — Core dashboard.
- `v0.4.0` — Destination details and regional information.
- `v0.5.0` — Favorites and preferences.
- `v0.6.0` — Website and documentation.
- `v0.8.0` — Feature-complete beta.
- `v1.0.0` — Stable public release.

These are suggestions, not mandatory release dates or a requirement to create every intermediate tag.

You are allowed to publish releases autonomously when the relevant quality gates are satisfied.

Never label an incomplete or unverified build as stable.

---

# 20. Definition of Done

A task is complete only when:

- The implementation exists.
- The relevant tests pass.
- Type checks pass where applicable.
- The production build succeeds.
- No known regression has been introduced.
- Documentation is updated when necessary.
- Relevant accessibility and responsive behavior have been checked.
- The change is committed when appropriate.

The project is ready for `v1.0.0` only when:

- Real source data is integrated.
- All known level codes are handled correctly.
- Missing data is not misrepresented.
- Regional-risk information is preserved.
- Search and filtering work.
- List and card views work.
- Destination details work.
- Favorites work.
- Theme selection works.
- Data synchronization is reliable.
- The static application works on GitHub Pages.
- The website works on GitHub Pages.
- CI and deployment workflows pass.
- Documentation is complete.
- Licensing is correct.
- The application includes a clear source disclaimer.

---

# 21. Mandatory Agent Workflow

Before implementation:

1. Read this file.
2. Inspect the repository and its current Git state.
3. Identify the existing environment and available tools.
4. Research the source data.
5. Write down uncertainties.
6. Select a suitable architecture.
7. Document significant technical decisions.

During implementation:

1. Work in small, coherent stages.
2. Keep business logic separated from UI.
3. Add tests with implementation.
4. Update documentation.
5. Run relevant checks.
6. Commit meaningful milestones.

Before publishing:

1. Inspect Git status.
2. Run tests.
3. Run type checks.
4. Run production builds.
5. Verify the generated deployment artifact.
6. Verify the application base path.
7. Verify data provenance and timestamps.
8. Verify no secrets or unintended files are included.
9. Verify the deployment target.

After publishing:

1. Verify the actual deployed URLs.
2. Verify the site loads.
3. Verify the application loads.
4. Verify data is accessible.
5. Verify browser console errors.
6. Verify the release information, if a release was created.
7. Report completed work and remaining limitations.

---

# 22. Final Engineering Principle

Build Waymark as a real, maintainable open-source product.

Prioritize:

1. Correct data.
2. Trustworthy presentation.
3. Reliability.
4. Accessibility.
5. Excellent user experience.
6. Maintainable code.
7. Simple infrastructure.
8. Clear documentation.

Do not sacrifice correctness for speed or visual polish.

Do not sacrifice maintainability for unnecessary complexity.

Do not invent functionality, data, sources, or successful deployment results.

When uncertain, investigate first, choose the safest technically sound approach, document the decision, and continue autonomously.
