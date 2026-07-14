# Troubleshooting System Tests

## Table of Contents

- [Understanding Test Output](#understanding-test-output)
- [Finding Root Cause of Test Failures](#finding-root-cause-of-test-failures)

## Understanding Test Output

### Output Directory Structure

Test output is split across three directories under `systemtests/` — only `target/` is removed by `mvn clean`, so screenshots and traces survive a clean build for comparing across runs:

```
systemtests/
├── target/logs/                    # TEST_LOG_DIR — wiped by `mvn clean`
│   ├── config.yaml                 # configuration snapshot for this run
│   └── 2026-06-24-14-30-00/        # timestamped test run
│       ├── test-execution.log
│       ├── pod-logs/               # console-operator-*.log, kafka-broker-*.log, ...
│       └── yaml-dumps/             # kafka-cluster.yaml, console-instance.yaml, ...
├── screenshots/                    # SCREENSHOTS_DIR_PATH — survives `mvn clean`
│   └── 2026-06-24__14-30-00/
│       └── test-name-failure.png
└── tracing/                        # TRACING_DIR_PATH — survives `mvn clean`
    └── 2026-06-24__14-30-00/
        └── trace-<test-name>.zip
```

### Log Levels

**File logging** (`TEST_FILE_LOG_LEVEL`): `TRACE` > `DEBUG` (default) > `INFO` > `WARN` > `ERROR`.

**Console logging** (`TEST_CONSOLE_LOG_LEVEL`): default `INFO` — keep it there and rely on file logs for detail.

### Playwright Trace Files

**Location:** `systemtests/tracing/<run-timestamp>/`, generated automatically when a UI test fails.

Each trace includes a step-by-step timeline of browser actions, before/after screenshots per action, network requests/responses, browser console logs, element selectors, and timing — optionally video, if enabled.

**Viewing a trace:**
1. `ls systemtests/tracing/` to find `trace-<test-name>-<timestamp>.zip`
2. Drag it onto https://trace.playwright.dev/ (runs entirely in-browser, nothing is uploaded)
3. Use the Timeline tab to find the red (failed) action, then check Network and Console tabs for the underlying cause

**Example:** a test fails with "Element not found". The trace shows navigation succeeded, the "Topics" tab click succeeded, but waiting for "Create Topic" timed out — the Network tab shows a 500 from `/api/topics`. Root cause: a backend error, not a broken selector.

## Finding Root Cause of Test Failures

1. **Read the failure message**
   ```
   [ERROR] MessagesST.testSendMessage:123 Expected element not found
   ```
   Note the exception type, line number, and message.

2. **UI failures — check the Playwright trace** (see above) for the timeline, screenshot, network, and console state at the point of failure.

3. **Check the failure screenshot directly**
   ```bash
   ls systemtests/screenshots/<run-timestamp>/
   open systemtests/screenshots/<run-timestamp>/test-name-failure.png
   ```
   Look for missing elements, on-page error messages, or unexpected UI state.

4. **Review application logs**
   ```bash
   cd systemtests/target/logs/<timestamp>/pod-logs/
   grep ERROR console-api-*.log kafka-broker-*.log
   ```

5. **Examine the resource YAML dumps**
   ```bash
   cd systemtests/target/logs/<timestamp>/yaml-dumps/
   cat kafka-cluster.yaml console-instance.yaml
   ```
   Confirm images, versions, and env vars are as expected.

6. **Verify the test configuration**
   ```bash
   cat systemtests/target/logs/config.yaml
   ```

7. **Inspect the live cluster**, if `CLEANUP_ENVIRONMENT: false`:
   ```bash
   kubectl get all -n <test-namespace>
   kubectl describe pod <pod-name> -n <test-namespace>
   kubectl logs <pod-name> -n <test-namespace>
   kubectl get events -n <test-namespace> --sort-by='.lastTimestamp'
   ```

### Common Failure Patterns

| Symptom | Likely Cause | Where to Look |
|---------|-------------|---------------|
| Timeout waiting for element | UI slow to load, API error, wrong selector | Trace (network, screenshots, DOM), console logs |
| Element not found | Wrong selector, changed page structure | Trace (screenshots, DOM) |
| Test timeout | Resource not becoming ready | Pod logs, `kubectl describe pod` |
| ImagePullBackOff | Wrong image name, registry auth | `kubectl describe pod`, `config.yaml` |
| 500 API errors | Backend application error | Console API logs, Kafka logs |
| Authentication failures | Keycloak config, credentials | Keycloak logs, Console API logs |
| Kafka connection errors | Kafka not ready, network issue | Kafka broker logs, Strimzi logs |

---

**See also:** [README.md](README.md) · [CONFIGURATION.md](CONFIGURATION.md) · [RUNNING_TESTS.md](RUNNING_TESTS.md)
