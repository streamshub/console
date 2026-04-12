# Ask Mode Rules (Non-Obvious Only)

## Project Structure Context

**Multi-module Maven project:**
- `common/` - Shared configuration models (not source code)
- `api/` - Quarkus REST API backend (Java 21)
- `operator/` - Kubernetes operator (Quarkus Operator SDK)
- `ui/` - Next.js 14 frontend with PatternFly
- `systemtests/` - Integration tests with custom @TestBucket pattern

**UI architecture:**
- Next.js 14 with standalone output mode
- PatternFly components require explicit transpilation in next.config.js
- Middleware handles OIDC authentication with special bypass for `/api/schema`

## Testing Documentation

**System tests use @TestBucket annotation:**
- Non-standard pattern for resource grouping
- Resources created once per bucket, shared across tests
- Tests in same bucket run together (randomized order between buckets)
- Implementation: `systemtests/src/main/java/com/github/streamshub/systemtests/interfaces/TestBucketExtension.java`

**Test execution patterns:**
- Single test: `mvn clean verify -Dit.test=ClassName#methodName`
- Remote debugging: `mvn clean verify -Ddebug=5005`
- UI tests: `cd ui && npm run build && npm run test`

## Code Style Context

**Checkstyle enforces locale-specific operations:**
- Regex check prevents `.toLowerCase()` or `.toUpperCase()` without Locale parameter
- Must use `Locale.ROOT` parameter
- See `.checkstyle/checkstyle.xml:77-81`

**Project policies:**
- No `@author` tags in Javadoc (maintenance burden)
- Field injection via `@Inject` is standard (Sonar S6813 ignored)

## Architecture Context

**Operator uses dependent resource workflow:**
- `@Workflow` annotation with `@Dependent` resources
- Declares creation order and preconditions
- See `operator/src/main/java/com/github/streamshub/console/ConsoleReconciler.java`

**Container versioning:**
- Makefile converts SNAPSHOT to lowercase for container tags
- Example: `0.12.1-SNAPSHOT` → `0.12.1-snapshot`