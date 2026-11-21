# Releases

Waymark uses Semantic Versioning. Version numbers are defined in
`android/app/build.gradle.kts` (`versionCode`, `versionName`).

## Versioning

- `MAJOR`, incompatible storage or behavior changes.
- `MINOR`, new user-facing features.
- `PATCH`, fixes and documentation.

A Git tag of the form `vX.Y.Z` triggers the release workflow.

## Release checklist

1. Update `versionCode` and `versionName`.
2. Update `CHANGELOG.md`.
3. Run locally:

   ```bash
   cd android
   ./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleRelease
   ```

4. Commit the release preparation.
5. Tag and push:

   ```bash
   git tag -a v1.0.0 -m "Waymark 1.0.0"
   git push origin v1.0.0
   ```

6. The workflow builds a signed release APK, creates a GitHub Release and
   attaches the APK.

## Signing

Signing credentials are never committed. Create `android/keystore.properties`
locally (ignored by Git):

```properties
storeFile=/absolute/path/to/waymark-release.jks
storePassword=…
keyAlias=waymark
keyPassword=…
```

`app/build.gradle.kts` reads this file when it exists. In CI the workflow
reconstructs the keystore and property file from repository secrets:

| Secret | Content |
|---|---|
| `KEYSTORE_BASE64` | Base64-encoded release keystore |
| `KEYSTORE_PASSWORD` | Keystore password |
| `KEY_ALIAS` | Key alias |
| `KEY_PASSWORD` | Key password |

If the secrets are absent, the release workflow fails rather than publishing an
unsigned APK.

## Artifacts

- `app-release.apk`, installable release build.
- `app-release.aab`, optional Play-style bundle (not published automatically).
- GitHub Pages deployment is independent and only publishes the website.

## Database compatibility

Release builds must never erase user data. Schema migrations are explicit;
downgrading the app is not supported. Backups (JSON/ZIP) are the supported way
to move data between installations.
