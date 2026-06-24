# Troubleshooting System Tests

This guide covers understanding test output, debugging failures, and resolving common issues.

## Table of Contents

- [Understanding Test Output](#understanding-test-output)
- [Finding Root Cause of Test Failures](#finding-root-cause-of-test-failures)

## Understanding Test Output

### Log Directory Structure

**Default location:** `TEST_LOG_DIR` (configured as `target/logs/` in config.yaml)

**Contents:**

```
target/logs/
├── config.yaml                    # Configuration snapshot from this test run
├── 2026-06-24-14-30-00/          # Timestamped test run directory
│   ├── test-execution.log        # Detailed test execution logs
│   ├── pod-logs/                 # Collected pod logs (on failure)
│   │   ├── console-operator-*.log
│   │   ├── strimzi-operator-*.log
│   │   ├── kafka-broker-*.log
│   │   └── console-api-*.log
│   └── yaml-dumps/               # Kubernetes resource YAMLs
│       ├── kafka-cluster.yaml
│       ├── console-instance.yaml
│       ├── configmaps/
│       ├── secrets/
│       └── deployments/
└── screenshots/                   # Playwright failure screenshots
    └── test-name-failure.png
```

### Log Levels and Output

**File logging** (`TEST_FILE_LOG_LEVEL`):
- `TRACE` - Most verbose, includes method entry/exit
- `DEBUG` - Detailed diagnostic information (default)
- `INFO` - General informational messages
- `WARN` - Warning messages
- `ERROR` - Error messages only

**Console logging** (`TEST_CONSOLE_LOG_LEVEL`):
- Default: `INFO` - Keeps console output clean
- Recommendation: Keep at `INFO`, rely on file logs for details

### Playwright Trace Files

**Location:** `tracing/` directory (relative to systemtests root)

**When generated:** Automatically when UI tests fail

**What's included:**
- Step-by-step timeline of browser actions
- Screenshots at each interaction
- Network requests and responses
- Browser console logs and errors
- Element selectors used
- Timing information
- Test video recording (if enabled)

#### Viewing Playwright Traces

**Step-by-step debugging process:**

1. **Locate trace file**
   ```bash
   cd systemtests
   ls -la tracing/
   # Find: trace-<test-name>-<timestamp>.zip
   ```

2. **Open Playwright Trace Viewer**
   - Navigate to https://trace.playwright.dev/
   - Drag and drop the ZIP file onto the page
   - No upload to external servers - runs entirely in browser

3. **Analyze the trace**
   
   **Timeline view:**
   - See every action in chronological order
   - Click any action to see page state at that moment
   - Red highlighted actions indicate failures
   
   **Screenshots:**
   - Before/after screenshots for each action
   - Highlight elements being interacted with
   - Zoom and inspect page details
   
   **Network tab:**
   - See all API calls made during test
   - Request/response headers and bodies
   - Timing information for each request
   
   **Console tab:**
   - Browser console logs and errors
   - JavaScript errors that occurred
   - Console.log output from application
   
   **Source tab:**
   - Test source code
   - See which line of test code corresponds to each action

4. **Find the failure point**
   - Look for red highlighted action in timeline
   - Review screenshot at failure moment
   - Check console for JavaScript errors
   - Verify network requests completed successfully

**Example usage:**

> Test fails with "Element not found" error. Trace shows:
> 1. Navigation to page succeeded
> 2. Click on "Topics" tab succeeded
> 3. Wait for "Create Topic" button failed
> 4. Screenshot shows button didn't load due to API error
> 5. Network tab shows 500 error from /api/topics endpoint
> 6. Root cause: Backend API issue, not UI test problem

## Finding Root Cause of Test Failures

**Systematic debugging process:**

### Step 1: Check Test Failure Message

```bash
# Maven output shows:
[ERROR] MessagesST.testSendMessage:123 Expected element not found
```

**What to look for:**
- Exception type (AssertionError, TimeoutException, etc.)
- Line number in test code
- Error message details

### Step 2: For UI Failures - Check Playwright Trace

```bash
ls -la systemtests/tracing/
# Open trace file at https://trace.playwright.dev/
```

**What to analyze:**
- Timeline of actions leading to failure
- Screenshot at failure point
- Network requests (check for API errors)
- Console errors (JavaScript issues)
- Element selectors (correct selector?)

### Step 3: Check Screenshots

```bash
ls -la systemtests/target/logs/screenshots/
open systemtests/target/logs/screenshots/test-name-failure.png
```

**What to look for:**
- Visual state at failure time
- Expected elements missing?
- Error messages displayed on page?
- Unexpected UI state?

### Step 4: Review Application Logs

```bash
cd systemtests/target/logs/<timestamp>/pod-logs/
cat console-api-*.log | grep ERROR
cat kafka-broker-*.log | grep ERROR
```

**What to check:**
- Application exceptions
- Backend API errors
- Database connection issues
- Kafka broker errors

### Step 5: Examine Resource YAML Files

```bash
cd systemtests/target/logs/<timestamp>/yaml-dumps/
cat kafka-cluster.yaml
cat console-instance.yaml
```

**What to verify:**
- Resource configurations are correct
- All required fields present
- Image names and versions correct
- Environment variables set properly

### Step 6: Verify Test Configuration

```bash
cat systemtests/target/logs/config.yaml
```

**What to confirm:**
- Test ran with expected configuration
- Environment overrides applied correctly
- Component versions match expectations

### Step 7: Inspect Live Cluster State

**If `CLEANUP_ENVIRONMENT: false`:**

```bash
# List all resources in test namespace
kubectl get all -n <test-namespace>

# Check pod status
kubectl describe pod <pod-name> -n <test-namespace>

# View live logs
kubectl logs <pod-name> -n <test-namespace>

# Check events
kubectl get events -n <test-namespace> --sort-by='.lastTimestamp'
```

**Common issues revealed:**
- Pods stuck in Pending (resource constraints)
- ImagePullBackOff (image not found)
- CrashLoopBackOff (application error)
- Resource conflicts

### Common Failure Patterns

| Symptom | Likely Cause | Where to Look |
|---------|-------------|---------------|
| Timeout waiting for element | UI slow to load, API error, incorrect selector | Playwright trace (network tab, screenshots, DOM), console logs |
| Element not found | Incorrect selector, page structure changed | Playwright trace (screenshots, DOM) |
| Test timeout | Resource not becoming ready | Pod logs, kubectl describe pod |
| ImagePullBackOff | Wrong image name, registry auth | kubectl describe pod, config.yaml |
| 500 API errors | Backend application error | Console API logs, Kafka logs |
| Authentication failures | Keycloak config, credentials | Keycloak logs, Console API logs |
| Kafka connection errors | Kafka not ready, network issue | Kafka broker logs, Strimzi logs |

---

**See also:**
- [README.md](README.md) - Getting started guide
- [CONFIGURATION.md](CONFIGURATION.md) - Configuration reference
- [RUNNING_TESTS.md](RUNNING_TESTS.md) - How to run tests
