# Plan Mode Rules (Non-Obvious Only)

## Architecture Constraints

**Multi-module Maven structure:**
- `common/` - Shared config models (dependency for api/operator)
- `api/` - Quarkus REST API (Java 21, depends on common)
- `operator/` - Kubernetes operator (Quarkus Operator SDK, depends on common)
- `ui/` - Next.js 14 frontend (standalone, separate build)
- `systemtests/` - Integration tests (requires Docker 2GB+ RAM)

**Operator dependent resource workflow:**
- Uses `@Workflow` with `@Dependent` annotations
- Resources have explicit creation order via `dependsOn`
- Preconditions control when resources are created
- Example: Prometheus resources only created when metrics source is "private"
- See `operator/src/main/java/com/github/streamshub/console/ConsoleReconciler.java`

**UI architecture constraints:**
- Next.js standalone output mode (required for containerization)
- PatternFly packages MUST be transpiled (listed in next.config.js)
- Middleware handles OIDC with special `/api/schema` bypass
- Cannot use standard localStorage or browser APIs (webview restrictions)

## Testing Architecture

**System tests use @TestBucket pattern:**
- Custom extension for resource grouping
- Resources created once per bucket, shared across tests
- Tests in same bucket run together (randomized between buckets)
- Reduces test setup overhead significantly
- Implementation: `systemtests/src/main/java/com/github/streamshub/systemtests/interfaces/TestBucketExtension.java`

**Test lifecycle:**
- `AbstractST` provides `@BeforeAll` for operator setup
- `@BeforeEach` checks cluster health before each test
- Requires Kubernetes cluster with Strimzi operator

## Build Constraints

**Container image versioning:**
- Makefile converts Maven SNAPSHOT to lowercase for container tags
- Example: `0.12.1-SNAPSHOT` → `0.12.1-snapshot`
- Required for container registry compatibility

**Local development:**
- Requires `console-config.yaml` in project root for compose
- UI and API run as separate containers
- See `compose.yaml` and `examples/console-config.yaml`

## Code Style Constraints

**Checkstyle enforces locale-specific operations:**
- Regex prevents `.toLowerCase()` or `.toUpperCase()` without Locale
- Must use `Locale.ROOT` parameter
- Build will fail if violated

**Project policies:**
- No `@author` tags in Javadoc
- Field injection via `@Inject` is standard pattern