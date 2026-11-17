# Start JasonJava on Windows

The normal setup is simple:

1. Save the Google credential and environment variable.
2. Clone the repository.
3. Double-click `start-worker.cmd`.

## 1. One-time prerequisites

Install these once:

- [Git for Windows](https://git-scm.com/download/win)
- [Java JDK 17](https://adoptium.net/temurin/releases/?version=17)
- [Apache Maven 3.9+](https://maven.apache.org/download.cgi)

## 2. Get repository access

Generate an SSH key in PowerShell:

```powershell
ssh-keygen -t ed25519 -C "jasonjava-windows-pc" `
  -f "$env:USERPROFILE\.ssh\jasonjava_github"
```

Send the contents of this public-key file to the repository admin:

```powershell
Get-Content "$env:USERPROFILE\.ssh\jasonjava_github.pub"
```

Ask the repository admin to add it under:

```text
GitHub repository → Settings → Deploy keys → Add deploy key
```

The admin should leave **Allow write access** cleared.

Create `$env:USERPROFILE\.ssh\config` with:

```text
Host github-jasonjava
    HostName github.com
    User git
    IdentityFile ~/.ssh/jasonjava_github
    IdentitiesOnly yes
```

## 3. Add the Google credential

Ask the Google Cloud project admin to provide the Windows worker's subscriber
JSON key.

The project admin is responsible for:

1. Creating the `jasonjava-windows` service account.
2. Granting it only `roles/pubsub.subscriber` on that subscription.
3. Providing its JSON key securely.

Save the key on the PC with this exact path:

```text
C:\JasonApp\secrets\jasonjava-subscriber.json
```

Save its location as a user environment variable once:

```powershell
setx GOOGLE_APPLICATION_CREDENTIALS "C:\JasonApp\secrets\jasonjava-subscriber.json"
```

The value is available to newly started programs. You do not need to run this
command again unless the key location changes.

## 4. Clone the repository

```powershell
New-Item -ItemType Directory -Force C:\JasonApp | Out-Null
Set-Location C:\JasonApp
git clone git@github-jasonjava:CountToFiveXY/JasonJava.git
```

## 5. Start the worker

Open:

```text
C:\JasonApp\JasonJava
```

Double-click:

```text
start-worker.cmd
```

That single launcher builds and starts the worker. No manual Maven commands or
per-run configuration are required.

Keep the launcher window open while the worker is running.

Task logic belongs in:

```text
scripts\reset.ps1
scripts\refill-ads.ps1
```

## 6. Test it

From Google Cloud Console, publish this message to
`asphalt_legend_topic`:

```json
{"type":"RESET"}
```

The worker should run `scripts\reset.ps1`.

The second task can be tested with:

```json
{"type":"REFILL_ADS","repeat":8}
```

This runs `scripts\refill-ads.ps1` once with `-Repeat 8`.

Start game can be tested with:

```json
{"type":"START_GAME"}
```

This runs `scripts\start-game.ps1`, which launches `scripts\exe\pre_ad.exe` once.

Close game can be tested with:

```json
{"type":"CLOSE_GAME"}
```

This runs `scripts\close-game.ps1`, which reuses the close and verification
stages from `scripts\reset.ps1`.

## 7. Check logs

Logs are written automatically to:

```text
%USERPROFILE%\Documents\JasonApp\Logs\jasonjava-0.log
```

Read the latest entries with:

```powershell
Get-Content "$env:USERPROFILE\Documents\JasonApp\Logs\jasonjava-0.log" -Tail 100
```

Look for `Running RESET` followed by `Completed RESET`.
