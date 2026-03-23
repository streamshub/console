# Advance Mode Rules (Non-Obvious Only)

## Java Coding Rules

**Locale-specific string operations are enforced:**
- NEVER use `.toLowerCase()` or `.toUpperCase()` without `Locale.ROOT` parameter
- Checkstyle regex check will fail build: `\.to(Lower|Upper)Case\(\)`
- Example: `string.toLowerCase(Locale.ROOT)` not `string.toLowerCase()`

**No @author tags allowed:**
- Project policy: `@author` tags in Javadoc are forbidden (hard to maintain)
- See api/CONTRIBUTING.md

**Dependency injection pattern:**
- Use `@Inject` for field injection (Sonar rule java:S6813 is ignored project-wide)
- Services are `@ApplicationScoped`, utilities are `@Singleton`

## UI Coding Rules

**PatternFly transpilation required:**
- All `@patternfly/*` packages MUST be listed in `next.config.js` `transpilePackages`
- Missing packages will cause build failures

**Middleware authentication bypass:**
- `/api/schema` endpoint bypasses OIDC when both `content` and `schemaname` query params present
- See `ui/middleware.ts:60-71` for implementation

**Next.js standalone output:**
- UI uses `output: "standalone"` in next.config.js
- Required for container deployment

## Testing

**Single test execution:**
```bash
mvn clean verify -Dit.test=ClassName#methodName
```

**Remote debugging:**
```bash
mvn clean verify -Ddebug=5005
```

**System tests use @TestBucket:**
- Resources shared across tests in same bucket
- See `systemtests/src/main/java/com/github/streamshub/systemtests/interfaces/TestBucketExtension.java`

## Browser & MCP Tools Available

This mode has access to browser automation and MCP tools for enhanced capabilities.