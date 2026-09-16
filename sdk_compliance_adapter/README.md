# KMP JVM compliance profile

This is **partial KMP coverage**, through the compiled public `com.posthog.kmp.PostHog`
JVM facade. Its delegate is stateful `com.posthog:posthog:6.33.7`, not the Java server SDK.
The project dependency builds the current checkout, without exporting internals or replacing the delegate.

## Run

Requirements: Java 17, the repository's Gradle/Android build setup, and Python 3 (standard library only).

```sh
./gradlew :sdk_compliance_adapter:installDist :sdk_compliance_adapter:test
python3 -m unittest discover -s sdk_compliance_adapter -p 'test_*.py'
python3 sdk_compliance_adapter/adapter.py
```

The controller listens on loopback `PORT` (default 18320), its JVM worker on
`WORKER_PORT` (18321), and the passive wire proxy on `PROXY_PORT` (19321).
Use distinct free ports. The mock must be HTTP on loopback. In Linux CI, the harness
container uses host networking to reach these listeners:

```sh
docker run --rm --network host -v "$PWD/report:/report" \
  ghcr.io/posthog/sdk-test-harness:1.0.0 \
  run --adapter-url http://127.0.0.1:18320 \
  --mock-port 19320 --mock-url http://127.0.0.1:19320 \
  --sdk-type server --suite capture --suite feature_flags \
  --report /report/kmp-jvm.json
python3 sdk_compliance_adapter/check_report.py report/kmp-jvm.json
```

Create `report/` first. `server` describes the V0 `/batch` wire, not a stateless product.
CI pins the 1.0.0 image digest, retains advisory assertion results, uploads a JVM-specific
artifact, and requires the exact **30 capture + 17 flags = 47** IDs in `inventory.json`.
Missing, empty, duplicate, wrong-profile or inconsistent reports fail the inventory gate.
No individual failing cases are filtered out.

## Public mappings and limitations

- Each `/init` starts a new owned JVM. `/reset` kills and waits for only that worker,
  then deletes its private home/temp directory. The SDK's public `close()` schedules a
  flush and is not a quiescence barrier; process isolation prevents late requests or
  preferences from leaking into the next test. No private queue access is used.
- `api_key`, `host`, `flush_at` and positive whole-second `flush_interval_ms` map to
  existing configuration. Omitted controls retain KMP defaults (including a 30-second
  flush timer). Subsecond intervals cannot be represented and are reported unsupported,
  with the default unchanged. Retry limits, compression toggles, GeoIP and historical
  migration controls are also reported unsupported rather than changing the delegate.
  This profile uses native default gzip and the delegate's default retry limit of three.
- Automatic lifecycle/screen/deep-link/autocapture and flag preload are disabled in this
  configured profile. This is not default-lifecycle coverage. SDK-generated identity
  events and reloads remain enabled.
- `/capture` uses public `identify` when identity changes, then public `capture`.
  The real `$identify` event and any resulting flag reload are retained. There is no
  per-event identity override. RFC3339 timestamps are parsed to the existing epoch-millis
  `CaptureOptions.timestamp`; the SDK owns serialization to UTC. The ordinary properties
  are not date-normalized.
- KMP capture returns `Unit` and its public before-send event hides UUID. `/capture`
  therefore omits UUID. It does not manufacture one or force an early flush to learn it.
- `/get_feature_flag` uses public identity/person/group setters, awaits public reload,
  then reads the public cached getter. The SDK parses the response and emits its own
  `$feature_flag_called` event. Every action reloads, including `force_remote`; this is
  not proof that ordinary cached getters fetch remotely. Per-call GeoIP and singleton
  flag-key scope are unavailable. Setter-triggered reloads can consume mock fixtures
  before the explicit reload.
- The proxy forwards SDK-produced bytes and upstream responses without delivery retries,
  observing a decompressed copy only for UUID/status telemetry. Before-send is observational;
  the pinned core can invoke it twice for one capture, so it is not an event-count signal.
  `pending_events` and `total_events_captured` are `null`; raw hook invocation counts are
  separate. `total_events_sent` counts unique SDK UUIDs observed in successful responses;
  retries count capture requests containing previously observed UUIDs. `requests_made`
  covers `/batch` only, as stated by `requests_scope`. No internal queue/drop
  state is claimed.
- `/flush` invokes the existing public method once, observes wire traffic for ten seconds,
  then returns `success: false`, `flush_invoked: true` and the newly observed successful
  UUID count. KMP has no public completion signal, so successful completion cannot be
  certified, even when all observed traffic succeeded. The adapter never schedules SDK
  flush retries or clears unresolved state on a timeout. Harness 1.0.0's current YAML
  assertions inspect mock wire traffic rather than requiring this response to be true.

## Expected disagreements

The full inventory intentionally exposes incompatible assumptions. Identify-triggered
`/flags` requests can precede `/batch`; harness 1.0.0 capture assertions often inspect
or count all requests, and the shared mock response sequence can be consumed by flags.
Thus a passing capture assertion may not actually isolate capture retries or schema.
`$identify` also increases event counts. Focused public-entry tests separately verify
the named capture's UTC/UUID/metadata and the real multi-event retry path with a supported
one-second timer, preserving identify events and SDK-owned batch UUIDs/timestamps. The 30-second timer can miss retry YAML windows; a bounded observation
window is not a queue-timing override. Multi-event HTTP 413 handling can split a batch
containing identify and capture. Mobile-style reloads, startup remote configuration,
per-call controls and cached flag side effects differ from stateless server assertions.
A green advisory CI job is not a compliance verdict; inspect all failures in the JSON artifact.

## Deferred runtime profiles

| Runtime | Exact existing delegate | Starting inventory | Status |
| --- | --- | --- | --- |
| Android | Android 3.60.7 / core 6.33.7 | 30 capture + 17 flags | No device controller host |
| iOS | iOS 3.64.1 | 30 capture + 17 flags | No simulator controller host |
| JS | browser 1.398.7 | 27 client capture | No compiled KMP browser controller host |
| Wasm | browser 1.398.7 | 27 client capture | No compiled KMP browser controller host |

JVM results do not certify these delegates. In particular, the existing JS/Wasm wrapper
always passes capture options, bypassing normal browser batching: the client
`capture.deduplication.preserves_uuid_and_timestamp_on_batch_retry` remains a priority
for real browser hosts. Their pinned 408/429 and retry-budget behavior also needs runtime
coverage. Client-wire flags are not selected by harness 1.0.0. V1, dedicated AI and
selectable extra codecs are unsupported by this facade, not passing profiles.
