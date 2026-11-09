# Architecture

The app is a thin read-only client of the Admin API (`prostir-backend`, section "Admin API (mobile app)" in its README). It has no database and no background work. All numbers are computed by the backend; the app only formats them.

## Layers

```
UI (Compose screens)  ->  ViewModels  ->  AdminRepository  ->  Retrofit / OkHttp  ->  Admin API
        ^                     |                  |
   design tokens         Loader<T>          TokenStore (encrypted)
   (ui/theme)            Loadable<T>
```

| Package | Role |
| --- | --- |
| `data/` | API models (`Models.kt`), Retrofit interface (`AdminApi.kt`), error mapping (`ApiError.kt`), encrypted token storage (`TokenStore.kt`), `AdminRepository` |
| `Logic.kt` | Presentation rules from `design/spec-d.md`, free of Android types so they are unit-tested (`LogicTest`) |
| `ProstirApp.kt` | `AppContainer`: hand-wired dependencies, OkHttp auth interceptor, shared house filter |
| `ui/Loadable.kt` | `Loadable<T>` (data, loading, error, updatedAt) and `Loader<T>` that runs one request |
| `ui/ViewModels.kt` | One ViewModel per screen and `VmFactory` |
| `ui/AppRoot.kt` | Chooses login or the four tabs, handles logout and session expiry |
| `ui/theme/` | Colour tokens (light, dark), Onest font, `Pro.c` accessor |
| `ui/components/` | Cards, chips, house badge, filter, ring, skeleton, error banner, bottom navigation, line icons |
| `ui/charts/` | Pace chart, month bars, heatmap table, occupancy matrix (Canvas) |
| `ui/screens/` | Login, overview, pace, months, arrivals |

There is no dependency injection framework on purpose: five ViewModels and one repository do not justify the build time.

## Request flow and state

Each request has one `Loader<T>` exposing `StateFlow<Loadable<T>>`:

- `loading` is true while a request runs; `data` keeps the previous answer.
- On failure `data` stays and `error` is set. Screens show the old data with a slim banner ("Показуємо дані станом на HH:mm").
- A silent load (the 60 second poll) does not set `loading`, so the pull-to-refresh indicator stays quiet.
- Changing the month or year calls `load(clear = true)` because the old data belongs to something else.

Polling is `PollWhileVisible` in `ui/screens/Common.kt`: it refreshes on return from the background and every 60 seconds while the screen is resumed. The API has no push, so this is the whole "real time" story.

## Authentication

1. `LoginViewModel` calls `AdminRepository.login`. On success `TokenStore` saves the token and its `expires_at` in `EncryptedSharedPreferences`. The password is never stored.
2. The OkHttp interceptor in `AppContainer` adds `Authorization: Bearer <token>` and `Accept: application/json` to every request.
3. A `401` on a request that carried a token clears the token and emits `sessionExpired`. `AppRoot` collects it, resets the house filter and shows the login screen with the "Потрібно увійти знову" card. A `401` on the login request itself is just wrong credentials.
4. Logout calls `POST /api/admin/logout` (best effort) and always forgets the token locally.
5. ViewModels are keyed by a session counter in `AppRoot`, so a new login never shows the previous user's data.

## Errors

`ApiError` is a sealed type mapped in `toApiError()`:

| Case | Source | Shown as |
| --- | --- | --- |
| `InvalidCredentials` | `401` on login | red field borders and message |
| `TooManyAttempts` | `429` on login (`Retry-After`, default 60 s) | message and a countdown on the button |
| `Validation` | `422` | "Введіть логін і пароль." |
| `SessionExpired` | `401` elsewhere | back to login |
| `Network` | `IOException` | "Немає з'єднання" banner |
| `Server` | `5xx` | "Сервер тимчасово недоступний" banner |
| `Unknown` | anything else, bad JSON | generic message |

## Which screen reads what

| Screen | Requests |
| --- | --- |
| Overview | `occupancy?year=<this year>`, `booking-pace?month=<this month>`, `arrivals?from=today&to=today+62` (the last also gives each house its next check-in) |
| Pace | `booking-pace?month=<selected>` |
| Months | `occupancy?year=<selected>` |
| Arrivals | `arrivals` for today, tomorrow, or today..today+6, plus `occupancy` once if the house list is empty |

The Overview requests run in parallel and fail independently.

## Houses: colour and number

Houses come from `occupancy.houses`. The position in that list is the slot of the house: colour `houses[i]` from the theme and number `i + 1`. `AdminRepository.houses` caches the last list so the arrivals screen can colour its cards. The palette has four colours; a fifth house needs a new colour in `ui/theme/Theme.kt` (and a palette check, see `design/spec-d.md`).

## Charts

Charts are drawn on `Canvas` and use no chart library.

- `PaceChart`: up to three newest years; the newest is a thick solid line, the previous a solid line, the oldest dashed. Tap or horizontal drag moves a crosshair and shows a tooltip. "Today" is placed by interpolating the day offset between the API's `offsets`.
- `MonthBars`: tap a bar for details. Ticks mark the same month of the previous year, only for "all houses" because the API compares years in total.
- `HeatCell`, `PaceTable`, `OccupancyMatrix`: one-colour scale, text flips colour on dark cells (`hotThreshold` in the theme).

## Design source of truth

`design/spec-d.md` is the written spec, `design/prototype-d.html` the visual prototype. When the app and the spec disagree, fix one of them and note it in the spec's "Припущення й відкриті питання".
