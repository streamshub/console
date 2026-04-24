# AGENTS.md

This file provides guidance to agents when working with code in this repository.

## 🚨 CRITICAL: Deprecated UI Module

Ignore the `ui` directory *unless specifically requested*, that is the old Next.js application. The new React application is in `api/src/main/react`.

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

**UI development (with hot-reload):**
```bash
cd api && mvn quarkus:dev
```
This starts both the Quarkus API and the React UI with hot-reload enabled. Quinoa runs a Node server in the background and proxies UI resource requests.

**UI tests:**
```bash
cd api/src/main/webui && npm test
```

**UI standalone development (optional):**
```bash
cd api/src/main/webui && npm run dev
```
Note: API must be running separately on `http://localhost:8080` for this to work.

## Code Style (Non-Obvious)

**Java - Checkstyle enforces locale-specific string operations:**
- NEVER use `.toLowerCase()` or `.toUpperCase()` without Locale parameter
- Use `.toLowerCase(Locale.ROOT)` or `.toUpperCase(Locale.ROOT)`
- Checkstyle will fail build if violated

**Java - No @author tags:**
Project policy disallows `@author` in Javadoc (hard to maintain).

**UI - PatternFly packages optimization:**
`vite.config.ts` includes PatternFly packages in `optimizeDeps` for proper bundling and performance.

**UI - PatternFly version:**
- Project uses PatternFly v6 (check `api/src/main/webui/package.json` for exact version)
- Always verify PatternFly component APIs match v6 documentation
- Avoid version-specific class names or props when possible
- Reference existing components in codebase for correct usage patterns

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
- `api/` - Quarkus REST API (Java 21) with integrated React UI via Quinoa
- `api/src/main/webui/` - React + Vite frontend (PatternFly React components)
- `operator/` - Kubernetes operator (Quarkus Operator SDK)
- `systemtests/` - Integration tests

**UI Architecture:**
- Built with React, TypeScript, and Vite
- Integrated into Quarkus via Quinoa extension
- Uses PatternFly React components for UI
- TanStack Query for API state management
- React Router v6 for client-side routing
- i18next for internationalization

**Operator uses dependent resource workflow:**
Reconciler declares dependencies via `@Workflow` with `@Dependent` annotations defining resource creation order and preconditions.

**Container image versioning:**
Makefile converts Maven SNAPSHOT versions to lowercase for container tags (e.g., `0.12.1-SNAPSHOT` → `0.12.1-snapshot`).

**Quinoa Integration:**
- UI build is automatically triggered during Maven build (`mvn clean install`)
- During development (`mvn quarkus:dev`), Quinoa runs a Node server for hot-reload
- UI resources are served by the Quarkus API at runtime
- No separate UI container needed for deployment