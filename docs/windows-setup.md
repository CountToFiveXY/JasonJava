# JasonJava Windows setup and operations guide

This guide installs JasonJava on a Windows PC, connects it to Google Cloud
Pub/Sub, connects the two supported commands to Jitbit Macro Recorder files,
and keeps it running quietly whenever the automation user is logged in.

## 1. Understand the request path

```text
JasonApp -> JasonPython -> GCP topic -> pull subscription -> JasonJava -> task handler
```

JasonApp sends either `RESET` or `REFILL_ADS` to JasonPython. JasonPython
publishes a small JSON message to `asphalt_legend_topic`. JasonJava listens on
the `asphalt_legend_windows` pull subscription and launches only the local
script configured for that known task type.

JasonJava processes one task at a time. It acknowledges the Pub/Sub message
only when the script exits successfully. A timeout or non-zero exit allows
Pub/Sub to deliver the message again.

## 2. Important desktop automation limitation

Do not install JasonJava as a traditional Windows service with WinSW, NSSM, or
`sc.exe`. Windows services run in Session 0 and cannot safely control the mouse,
keyboard, game, or other windows in the signed-in user's desktop session.

Use Windows Task Scheduler and select **Run only when user is logged on**. The
automation account must remain signed in. Keep the PC awake and, while a macro
is running, unlocked with the expected screen resolution, scaling, and game
window position. Do not use the mouse or keyboard during playback.

The worker itself is idle while waiting for Pub/Sub. The final scheduled task
uses `javaw.exe` to avoid a console window and caps the JVM heap at 128 MB. It
writes rotating logs instead of holding a terminal open.

## 3. Install the required software

Install these on the Windows PC:

1. Git for Windows: <https://git-scm.com/download/win>
2. A Java 17 or newer JDK, such as Eclipse Temurin 17:
   <https://adoptium.net/temurin/releases/?version=17>
3. Apache Maven 3.9 or newer: <https://maven.apache.org/download.cgi>
4. Google Cloud CLI: <https://cloud.google.com/sdk/docs/install-sdk>
5. Jitbit Macro Recorder, the application that saves `.mcr` files:
   <https://www.jitbit.com/macro-recorder/>

For Maven, extract the binary ZIP to a stable directory such as
`C:\Tools\apache-maven`, then add its `bin` directory to the system `PATH`.

Open a new PowerShell window and verify the tools:

```powershell
git --version
java -version
javac -version
mvn --version
gcloud --version
```

All five commands must succeed before continuing.

## 4. Clone and build JasonJava

This guide uses `C:\JasonApp\JasonJava` as the installation directory. Choose a
different directory if needed, but use the same path consistently later.

```powershell
New-Item -ItemType Directory -Force C:\JasonApp | Out-Null
Set-Location C:\JasonApp
git clone https://github.com/CountToFiveXY/JasonJava.git
Set-Location C:\JasonApp\JasonJava
mvn clean verify
```

A successful build runs the tests and creates:

```text
C:\JasonApp\JasonJava\target\jasonjava-1.0.0-SNAPSHOT.jar
```

Create directories for credentials and rotating logs:

```powershell
New-Item -ItemType Directory -Force C:\JasonApp\secrets | Out-Null
New-Item -ItemType Directory -Force C:\JasonApp\JasonJava\logs | Out-Null
```

## 5. Create the Pub/Sub subscription

Sign in with a Google account that may administer the project:

```powershell
gcloud auth login
gcloud config set project jasonapp-xm0830
```

Create one pull subscription attached to the existing topic:

```powershell
gcloud pubsub subscriptions create asphalt_legend_windows `
  --topic=asphalt_legend_topic `
  --ack-deadline=600
```

If GCP reports that the subscription already exists, do not create another
one. Multiple workers using the same subscription share messages; only run one
JasonJava worker unless that behavior is intentional.

## 6. Create a least-privilege worker identity

Create a dedicated service account:

```powershell
gcloud iam service-accounts create jasonjava-windows `
  --display-name="JasonJava Windows PubSub consumer"
```

Allow it to consume only from the pull subscription:

```powershell
gcloud pubsub subscriptions add-iam-policy-binding asphalt_legend_windows `
  --member="serviceAccount:jasonjava-windows@jasonapp-xm0830.iam.gserviceaccount.com" `
  --role="roles/pubsub.subscriber"
```

Create its JSON key directly in the protected secrets directory:

```powershell
gcloud iam service-accounts keys create `
  C:\JasonApp\secrets\jasonjava-subscriber.json `
  --iam-account=jasonjava-windows@jasonapp-xm0830.iam.gserviceaccount.com
```

Restrict that directory to the Windows automation user and Administrators. Do
not place the key inside the Git repository or commit it.

## 7. Install the reset script and build the refill macro

`RESET` runs the existing PowerShell script directly. Confirm this file exists:

```text
C:\Users\ybao0\OneDrive\Desktop\Macro\reset.ps1
```

Test it from PowerShell before connecting it to JasonJava:

```powershell
powershell.exe -NoLogo -NoProfile -NonInteractive `
  -ExecutionPolicy Bypass `
  -File "C:\Users\ybao0\OneDrive\Desktop\Macro\reset.ps1"
```

`REFILL_ADS` uses the copied macro-generation code. Its source fragments live
under `macrofiles\`. Generate the runnable macro after building the project:

```powershell
Set-Location C:\JasonApp\JasonJava
java -cp target\jasonjava-1.0.0-SNAPSHOT.jar jasonjava.macro.RefillAds
```

This creates `generated-macros\refill-ads.mcr` by combining eight copies of
`macrofiles\ad.mcr`. The generated directory is intentionally ignored by Git.

The repository's `scripts\refill-ads.cmd` launcher uses Jitbit's supported
command-line form:

```text
MacroRecorder.exe "C:\path\macro.mcr" /a /c
```

`/a` starts playback immediately and `/c` closes Macro Recorder afterward.

Find the actual executable path on this PC. A common location is under
`C:\Program Files (x86)`, but use the path from your installation. Test it:

```powershell
& "C:\Program Files (x86)\Jitbit Macro Recorder\MacroRecorder.exe" `
  "C:\JasonApp\JasonJava\generated-macros\refill-ads.mcr" /a /c
```

Do not continue until the reset script and refill macro both work when launched
from PowerShell.

## 8. Configure the worker environment

Run these commands as the same Windows user that will run the scheduled task.
Update the Macro Recorder executable path if yours differs:

```powershell
setx GOOGLE_APPLICATION_CREDENTIALS "C:\JasonApp\secrets\jasonjava-subscriber.json"
setx JASON_GCP_PROJECT "jasonapp-xm0830"
setx JASON_PUBSUB_SUBSCRIPTION "asphalt_legend_windows"
setx JASON_RESET_SCRIPT "C:\Users\ybao0\OneDrive\Desktop\Macro\reset.ps1"
setx JASON_REFILL_ADS_SCRIPT "C:\JasonApp\JasonJava\scripts\refill-ads.cmd"
setx JASON_MACRO_RECORDER_EXE "C:\Program Files (x86)\Jitbit Macro Recorder\MacroRecorder.exe"
setx JASON_SCRIPT_TIMEOUT_SECONDS "600"
```

`setx` affects newly started programs, not the current PowerShell process. Sign
out and back in, or open a new PowerShell window before testing.

## 9. Test the complete worker interactively

In a new PowerShell window:

```powershell
Set-Location C:\JasonApp\JasonJava
java -Xms16m -Xmx128m -jar target\jasonjava-1.0.0-SNAPSHOT.jar
```

The worker should report that it is listening on:

```text
projects/jasonapp-xm0830/subscriptions/asphalt_legend_windows
```

From JasonApp, publish `REFILL_ADS` first. Confirm that:

1. JasonJava receives the message.
2. `scripts\refill-ads.cmd` launches Macro Recorder.
3. `generated-macros\refill-ads.mcr` plays once.
4. Macro Recorder closes.
5. JasonJava logs a successful completion.

Then publish `RESET` and confirm the configured `reset.ps1` runs directly.
Press `Ctrl+C` to stop the interactive worker before creating the scheduled
task.

## 10. Keep JasonJava running in the background

Open **Task Scheduler** and choose **Create Task** (not “Create Basic Task”).

### General

- Name: `JasonJava PubSub Worker`
- Select the Windows user that owns and tested the macros.
- Select **Run only when user is logged on**.
- Optionally select **Run with highest privileges** only if the target game
  requires elevated input. Otherwise leave it off.
- Select **Hidden**.

### Triggers

- New trigger: **At log on**.
- Choose the same automation user.
- Enable the trigger.

### Actions

Create one **Start a program** action:

- Program/script: the full path to `javaw.exe`, for example
  `C:\Program Files\Eclipse Adoptium\jdk-17.0.17.10-hotspot\bin\javaw.exe`
- Add arguments:

```text
-Xms16m -Xmx128m -Djava.util.logging.config.file=config\logging.properties -jar target\jasonjava-1.0.0-SNAPSHOT.jar
```

- Start in:

```text
C:\JasonApp\JasonJava
```

Do not put quotation marks around the **Start in** value.

### Conditions

- Clear **Start the task only if the computer is on AC power** if the PC may
  run on battery.
- Clear **Stop if the computer switches to battery power** if appropriate.
- Clear any idle-only requirement.
- Configure Windows Power settings separately so the PC does not sleep while
  the worker is expected to receive commands.

### Settings

- Select **Allow task to be run on demand**.
- Select **Run task as soon as possible after a scheduled start is missed**.
- Select **If the task fails, restart every 1 minute**.
- Set restart attempts to at least `10`.
- Clear **Stop the task if it runs longer than...**.
- For “If the task is already running,” select **Do not start a new instance**.

Save the task, right-click it, and select **Run**. Task Manager should show one
`javaw.exe` process. Task Scheduler should show the task as `Running`.

Logs rotate at 1 MB with five retained files:

```text
C:\JasonApp\JasonJava\logs\jasonjava-0.log
```

## 11. Verify resource usage

Open Task Manager, find `javaw.exe`, and verify that CPU usage settles near zero
while no message is arriving. The JVM heap is capped at 128 MB by `-Xmx128m`.
Actual process memory also includes the JVM and native gRPC libraries, so the
Task Manager value can be higher than 128 MB.

Do not run Macro Recorder continuously. JasonJava starts it only when a command
arrives and the `/c` option closes it after playback.

## 12. Normal operation

Useful Task Scheduler actions:

- **Run**: start the worker.
- **End**: stop the worker.
- **Disable**: prevent it from starting at the next logon.
- **Enable**: allow automatic startup again.

To inspect recent logs:

```powershell
Get-Content C:\JasonApp\JasonJava\logs\jasonjava-0.log -Tail 100
```

## 13. Update the application

Stop the scheduled task before replacing the jar:

```powershell
Set-Location C:\JasonApp\JasonJava
git status
git pull --ff-only
mvn clean verify
```

Do not continue if `git status` reports unexpected local source changes. Your
tracked `.mcr` files may also be affected by a pull, so back up recordings that
have not been committed.

After the build succeeds, use Task Scheduler to **Run** the task again and
inspect the log.

## 14. Troubleshooting

### Task starts and immediately stops

Check `logs\jasonjava-0.log` and Task Scheduler's **History** tab. Confirm the
`javaw.exe` path, the **Start in** directory, all environment variables, and
that the credential JSON exists.

### `PERMISSION_DENIED`

Confirm the JSON key belongs to `jasonjava-windows` and that the account has
`roles/pubsub.subscriber` on `asphalt_legend_windows`.

### `NOT_FOUND`

Confirm the project and subscription values:

```powershell
gcloud pubsub subscriptions describe asphalt_legend_windows `
  --project=jasonapp-xm0830
```

### Message arrives but the macro does not start

Run the corresponding `.cmd` file manually from a new PowerShell window. Check
`JASON_MACRO_RECORDER_EXE`, the `.mcr` filename, and Macro Recorder licensing.

### Macro starts but cannot control the game

Confirm the scheduled task uses **Run only when user is logged on**, the desktop
is unlocked, the game is visible, elevation levels match, and display scaling
and resolution match the recording environment.

### The same macro runs more than once

Pub/Sub provides at-least-once delivery. If the script finishes but the worker
cannot acknowledge the message, it may be delivered again. Design macros so a
repeat is safe, or add an application-level deduplication record before using
the worker for non-repeatable actions.

### Worker uses more memory than expected

Confirm the scheduled task includes `-Xms16m -Xmx128m` and only one worker is
running. Do not lower `-Xmx` until both Pub/Sub startup and real task playback
have been tested reliably.

## 15. Security checklist

- Use a dedicated subscriber service account; do not reuse the publisher key.
- Keep the JSON key outside the repository.
- Restrict filesystem access to the automation user and Administrators.
- Never accept a command or file path from Pub/Sub. JasonJava's task allowlist
  must remain the only way to select local scripts.
- Review `.mcr` changes before committing or pulling them.
- Revoke and replace the service-account key if it is copied or exposed.
