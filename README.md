# JasonJava

JasonJava is the small Windows worker for JasonApp. It pulls one-shot commands
from Google Cloud Pub/Sub and runs a configured local script for each supported
task.

Additional setup, integration, and operational documentation belongs in
`docs/`. Start with the complete [Windows setup and operations
guide](docs/windows-setup.md).

The message contract matches the JasonPython `/v1/asphalt-tasks` endpoint:

```json
{"type":"RESET"}
```

The worker supports `RESET` and `REFILL_ADS`. The message never supplies a path
or command; each task maps to a repository PowerShell script in `WorkerConfig`.

## Code layout

- `jasonjava.config` contains the fixed Windows worker settings.
- `jasonjava.polling` contains the Pub/Sub message parser and receiver.
- `jasonjava.handler` maps allowed task types to local actions.
- `jasonjava.execution` launches supported Windows scripts and executables.

Edit `scripts/reset.ps1` and `scripts/refill-ads.ps1` to add the task logic.

## Prerequisites

- Windows 10/11 with Java 17 or newer.
- A Pub/Sub pull subscription attached to `asphalt_legend_topic`. The default
  subscription name is `asphalt_legend_windows`.
- A dedicated Google service account with only
  `roles/pubsub.subscriber` on that subscription.
- A service-account JSON key stored outside this repository.

Create the subscription once (from a machine with the Google Cloud CLI and
permission to administer Pub/Sub):

```powershell
gcloud pubsub subscriptions create asphalt_legend_windows `
  --topic=asphalt_legend_topic `
  --project=jasonapp-xm0830 `
  --ack-deadline=600

gcloud pubsub subscriptions add-iam-policy-binding asphalt_legend_windows `
  --project=jasonapp-xm0830 `
  --member="serviceAccount:YOUR_WORKER_ACCOUNT@jasonapp-xm0830.iam.gserviceaccount.com" `
  --role="roles/pubsub.subscriber"
```

The 600-second acknowledgement deadline matches the worker's default script
timeout. Configure a dead-letter topic and retry policy in GCP if repeatedly
failing scripts should stop redelivering after a fixed number of attempts.

## Build

Install Maven 3.9+ and run:

```powershell
mvn clean package
```

The runnable jar is `target/jasonjava-1.0.0-SNAPSHOT.jar`.

## Run on Windows

Place the subscriber key at
`C:\JasonApp\secrets\jasonjava-subscriber.json`, save that location once with
`setx GOOGLE_APPLICATION_CREDENTIALS`, then double-click `start-worker.cmd` in
the repository root. It builds and starts the worker.

Each script runs with the `scripts` directory as its working directory, and its
output is written to the worker's console.

The worker processes only one message at a time. It acknowledges a message
after the script exits with code `0`; a timeout or non-zero exit is negatively
acknowledged and can be delivered again. Because Pub/Sub is at-least-once,
scripts should be safe to retry. Invalid JSON and unsupported task types are
logged and acknowledged without execution.

## Run continuously

Use Windows Task Scheduler to launch the worker at logon. The [Windows
guide](docs/windows-setup.md) shows how to run `javaw.exe` hidden, limit its
heap, rotate logs, and restart it automatically. Do not commit the
service-account key or place it beside the jar in a shared directory.
