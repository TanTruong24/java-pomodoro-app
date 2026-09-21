# Pomodoro

A personal Pomodoro desktop application built with Java 21 and JavaFX. It combines a configurable timer, focus history, daily goals, and a calendar-style heatmap in a responsive light or dark interface.

## Features

- Configurable Focus, Short Break, and Long Break durations
- Configurable number of focus sessions before a long break
- Accurate countdown based on the current clock, including pause and resume
- Automatic break start after a completed focus session
- Audio and top-right popup notifications when sessions finish
- SQLite history for completed Focus and Short Break sessions
- History filters by day, week, month, or custom date range
- List and heatmap history views
- Daily target tracking with progress percentage
- Responsive layout for different window sizes
- Persistent light and dark themes
- Collapsible timer settings panel

## Requirements

- JDK 21
- Windows, macOS, or Linux with JavaFX media support

Verify the installed Java version:

```shell
java -version
```

## Run the application

### Windows

```powershell
.\mvnw.cmd clean javafx:run
```

### macOS or Linux

```shell
./mvnw clean javafx:run
```

You can also run `org.example.pomodoro.MainApplication` directly from IntelliJ IDEA using JDK 21.

## Build a portable Windows application

Make sure the Windows icon exists at:

```text
src/main/resources/icons/app-icon.ico
```

Run the following commands from the project root in PowerShell, in this order:

```powershell
.\mvnw.cmd clean package

.\mvnw.cmd dependency:copy-dependencies `
  -DoutputDirectory=target/dependency

if (Test-Path dist) {
  Remove-Item -Recurse -Force dist
}

jpackage `
  --type app-image `
  --name Pomodoro `
  --app-version 1.0.0 `
  --module-path "target\pomodoro-1.0-SNAPSHOT.jar;target\dependency" `
  --module "org.example.pomodoro/org.example.pomodoro.MainApplication" `
  --add-modules jdk.crypto.ec `
  --icon "src\main\resources\icons\app-icon.ico" `
  --dest dist
```

The portable application is generated at:

```text
dist/Pomodoro/
```

Launch it with `dist\Pomodoro\Pomodoro.exe`. Keep the complete `Pomodoro` directory together when moving the application because the launcher depends on its bundled `app` and `runtime` directories.

The `jdk.crypto.ec` module is needed for HTTPS connections to Supabase. When rebuilding, replace the entire old `dist\Pomodoro` folder with the new app image. You can verify the bundled module in `dist\Pomodoro\runtime\release`.

## Default timer settings

| Session | Default |
| --- | ---: |
| Focus | 25 minutes |
| Short Break | 5 minutes |
| Long Break | 10 minutes |
| Sessions before Long Break | 4 |

Open the timer settings with the gear button in the application header. Saving new values resets the timer to the first Focus session.

## History and daily target

Only completed sessions are stored:

- Focus sessions appear in the history list and heatmap.
- Focus and Short Break sessions count toward the daily target.
- Long Break, skipped, and reset sessions are not counted.

The heatmap uses darker cells for days with more focus time. Hover over a cell to see its date, total focus time, and completed session count.

The daily target defaults to 4 hours and can be set from 0.5 to 24 hours.

## Application data

The app stores its SQLite database in the current user's application-data directory:

- Windows: `%LOCALAPPDATA%\Pomodoro\pomodoro.db`
- Linux with `XDG_DATA_HOME`: `$XDG_DATA_HOME/pomodoro/pomodoro.db`
- Fallback: `<user-home>/.pomodoro/pomodoro.db`

Older databases located at `data/pomodoro.db` are copied to the user-data directory automatically when no destination database exists.

## Sync across computers

The app can sync completed sessions and saved settings through Supabase while keeping SQLite as its local data store. Setup instructions and the required SQL script are in [`.docs/supabase/README.md`](.docs/supabase/README.md). Sync is optional; without configuration, the app remains local-only.

## Application icon

Place a PNG icon at:

```text
src/main/resources/icons/app-icon.png
```

The application still starts normally when the icon is missing.

## Project structure

```text
src/main/java/org/example/pomodoro/
├── config/       Dependency wiring
├── controller/   Timer and history controllers
├── database/     SQLite configuration and schema initialization
├── model/        Application data models
├── repository/   Persistence interfaces and SQLite implementations
├── service/      Timer, history, target, theme, and sound services
└── view/         Reusable JavaFX components such as the heatmap

src/main/resources/
├── css/          Shared, dark, and light theme styling
├── icons/        Application icon
├── sounds/       Session notification audio
└── view/         FXML layouts
```

## Technology

- Java 21
- JavaFX 21
- Maven
- SQLite
