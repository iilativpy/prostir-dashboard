# Development guide

## Requirements

- JDK 17 or newer (JDK 21 works; the build targets Java 17).
- Android SDK with platform 35 and build-tools. Set `sdk.dir` in `local.properties`.
- An emulator or a device with Android 8.0 (API 26) or newer.
- The backend from `../prostir-backend` running locally.

## First run

1. Start the backend so `http://localhost:8000/api/admin/stats/occupancy` answers `401` without a token.
2. Create `local.properties` in the project root:

   ```
   sdk.dir=/Users/<you>/Library/Android/sdk
   API_BASE_URL=http://10.0.2.2:8000/
   ```

3. Start an emulator, then:

   ```bash
   ./gradlew :app:installDebug
   adb shell am start -n com.prostir.dashboard.debug/com.prostir.dashboard.MainActivity
   ```

4. Sign in with the login and password of the local backend (`ADMIN_LOGIN` and `ADMIN_PASSWORD` in its `.env`).

Debug builds use the application id suffix `.debug`, so they install next to a release build.

## Pointing the app at another backend

`API_BASE_URL` is read from `local.properties` at build time into `BuildConfig.API_BASE_URL`. It must end with `/`.

| Where the app runs | Value |
| --- | --- |
| Emulator, backend on the same Mac | `http://10.0.2.2:8000/` |
| Physical device on the same Wi-Fi | `http://<Mac LAN IP>:8000/` (the backend must listen on that interface) |
| Staging or production | `https://<host>/` |

Plain HTTP works only in debug builds (`app/src/debug/res/xml/network_security_config.xml`). Release builds block it.

## Tests

```bash
./gradlew :app:testDebugUnitTest
```

`LogicTest` covers rounding, the difference to last year, the three newest years, phone and date formatting, the "today" position on the chart axis and the house status line.

There are no UI tests yet. Check screens by hand on the emulator: light and dark theme (`adb shell cmd uimode night yes|no`), offline (`adb shell cmd connectivity airplane-mode enable|disable`), a house filter on Months and Arrivals, logout, and an expired token (revoke it on the backend, then pull to refresh).

## Adding or changing things

- A new API field: add it to `data/Models.kt` with `@SerialName` for snake_case names and a default for anything optional. The JSON parser ignores unknown keys.
- A new presentation rule: put it in `Logic.kt` and add a test in `LogicTest`.
- A new colour: add it to both `LightColors` and `DarkColors` in `ui/theme/Theme.kt`. Validate chart palettes for colour-blind safety first (see `design/spec-d.md`, "Перевірка палітри").
- A new icon: add a `Glyph` entry in `ui/components/ProIcon.kt`. Icons are SVG path data on a 24 by 24 grid drawn with a 2 px round stroke. Write arc flags with spaces (`a2 2 0 0 1 2-2`).
- User-visible text is Ukrainian and currently written in the composables. Moving it to `strings.xml` is the first step if a second language is ever needed.

## Troubleshooting

| Symptom | Cause and fix |
| --- | --- |
| "Немає з'єднання" right after login | The backend is not reachable from the emulator. Check `API_BASE_URL` and that the backend listens on port 8000. |
| Cleartext traffic error in Logcat | A release build is talking HTTP. Use a debug build or an HTTPS URL. |
| Login says wrong password but it is right | The backend reads `ADMIN_LOGIN` and `ADMIN_PASSWORD` from `.env`. Check them there. |
| "Забагато спроб" | The backend allows 5 login attempts per minute per IP. Wait for the countdown. |
| Guest names look like `Ð“Ñ–Ñ...` | The local database copy has an encoding problem. It is not an app bug. |
| Build fails on `sdk.dir` | Create `local.properties` (see above). |
| Emulator taps in scripts land late | Animations lag behind `adb shell input`; add a `sleep` between taps. |

## Fonts and licences

`app/src/main/res/font/onest.ttf` is Onest, SIL Open Font License 1.1 (https://github.com/simpals/onest).
