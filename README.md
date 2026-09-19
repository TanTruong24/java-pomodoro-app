# Pomodoro

A personal Pomodoro desktop application built with Java 21 and JavaFX. It combines a configurable timer, focus history, daily goals, and a calendar-style heatmap in a responsive light or dark interface.

## Features

- Configurable Focus, Short Break, and Long Break durations
- Configurable number of focus sessions before a long break
- Accurate countdown based on the current clock, including pause and resume
- Automatic break start after a completed focus session
- Audio notifications when sessions finish
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
