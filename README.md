# Prostir Booking Dashboard (Android)

Read-only statistics app for the owner of Prostir: occupancy of the current month, booking pace, occupancy by month, check-ins and check-outs.
Kotlin, Jetpack Compose, design direction D "Будинки". Design files and spec are in `design/` (`spec-d.md`, `prototype-d.html`).

## Documentation

- [docs/development.md](docs/development.md): setup, pointing the app at a backend, tests, troubleshooting.
- [docs/architecture.md](docs/architecture.md): layers, state and error handling, authentication, which screen reads which endpoint.
- [design/spec-d.md](design/spec-d.md): design specification. [design/prototype-d.html](design/prototype-d.html): visual prototype.
- Backend API: "Admin API (mobile app)" in `../prostir-backend/README.md`.

## Run against the local backend

1. Start the backend from `../prostir-backend` so that `http://localhost:8000/api/admin/...` answers (see "Admin API (mobile app)" in its README).
2. Create `local.properties` (not committed):

   ```
   sdk.dir=/Users/<you>/Library/Android/sdk
   API_BASE_URL=http://10.0.2.2:8000/
   ```

   `10.0.2.2` is the host machine as seen from the Android emulator. For a physical device use the host's LAN IP, for example `http://192.168.1.20:8000/`.
3. Start an emulator and install:

   ```bash
   ./gradlew :app:installDebug
   ```

   Sign in with the admin login and password of the local backend (`ADMIN_LOGIN` / `ADMIN_PASSWORD` in its `.env`).

Debug builds allow plain HTTP (`app/src/debug/res/xml/network_security_config.xml`). Release builds do not, so a release `API_BASE_URL` must be HTTPS.

## Tests

```bash
./gradlew :app:testDebugUnitTest
```

Unit tests cover the presentation rules in `Logic.kt` (rounding, the difference to last year, phone and date formatting, house status).

## Structure

```
app/src/main/java/com/prostir/dashboard/
  data/        Admin API: models, Retrofit interface, errors, token store, repository
  Logic.kt     presentation rules from the spec (no Android types)
  ui/theme/    design tokens (light and dark), Onest font
  ui/components/  cards, chips, house badge, filter, ring, skeleton, error banner, bottom navigation, line icons
  ui/charts/   pace chart, month bars, heatmap table, occupancy matrix
  ui/screens/  login, overview, pace, months, arrivals
  ui/          ViewModels, Loadable state, AppRoot (login or the four tabs)
```

How it behaves:

- The token (30 days) is kept in `EncryptedSharedPreferences`. The password is never stored. Any `401` after login clears the token and returns to the login screen.
- Every screen keeps the last good data on screen when a refresh fails and shows "Показуємо дані станом на HH:mm".
- There are no push messages: screens refresh on open, on return from the background, every 60 seconds while visible, and by pull-to-refresh.
- The house filter is shared by "Місяці" and "Заїзди" and is reset on logout.

## Fonts

`app/src/main/res/font/onest.ttf` is Onest (SIL Open Font License 1.1, https://github.com/simpals/onest).
