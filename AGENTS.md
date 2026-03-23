# AGENTS.md

This file provides guidance to agents when working with code in this repository.

## Build & Test Commands

**Single test execution (API/Operator):**
```bash
mvn clean verify -Dit.test=ClassName#methodName
```

**Remote debugging tests:**
```bash
mvn clean verify -Ddebug=5005
```
Attach debugger to port 5005 after test breakpoint is hit.

**UI tests:**
```bash
cd ui && npm run build && npm run test
```

**Local development with compose:**
Requires `console-config.yaml` in project root (see examples/console-config.yaml).

## Code Style (Non-Obvious)

**Java - Checkstyle enforces locale-specific string operations:**
- NEVER use `.toLowerCase()` or `.toUpperCase()` without Locale parameter
- Use `.toLowerCase(Locale.ROOT)` or `.toUpperCase(Locale.ROOT)`
- Checkstyle will fail build if violated

**Java - No @author tags:**
Project policy disallows `@author` in Javadoc (hard to maintain).

**UI - PatternFly packages must be transpiled:**
`next.config.js` explicitly transpiles all `@patternfly/*` packages - required for proper bundling.

**UI - Middleware authentication bypass:**
`/api/schema` endpoint bypasses OIDC when `content` and `schemaname` query params present (see middleware.ts:60-71).

## Testing Patterns

**System tests use @TestBucket annotation:**
- Groups tests sharing resources (e.g., `@TestBucket(VARIOUS_TOPIC_TYPES_BUCKET)`)
- Resources created once per bucket, not per test
- Tests in same bucket run together (randomized order between buckets)
- See `systemtests/src/main/java/com/github/streamshub/systemtests/interfaces/TestBucketExtension.java`

**Test lifecycle:**
- `AbstractST` base class provides `@BeforeAll` setup for operators
- `@BeforeEach` checks cluster health before each test
- System tests require Docker with 2GB+ RAM

## Architecture Notes

**Multi-module Maven project:**
- `common/` - Shared config models
- `api/` - Quarkus REST API (Java 21)
- `operator/` - Kubernetes operator (Quarkus Operator SDK)
- `ui/` - Next.js 14 frontend (standalone output)
- `systemtests/` - Integration tests

**Operator uses dependent resource workflow:**
Reconciler declares dependencies via `@Workflow` with `@Dependent` annotations defining resource creation order and preconditions.

**Container image versioning:**
Makefile converts Maven SNAPSHOT versions to lowercase for container tags (e.g., `0.12.1-SNAPSHOT` → `0.12.1-snapshot`).