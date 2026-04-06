# Authentication Security Review: JWT/JWE Cookie Approach

**Date:** 2026-03-29
**Reviewer:** Bob (Plan Mode)
**Scope:** poc-react application authentication architecture
**Status:** ⚠️ CRITICAL SECURITY CONCERNS IDENTIFIED

---

## Executive Summary

The proposed stateless JWT/JWE cookie-based authentication approach for storing Kafka credentials has **significant security risks** that outweigh its benefits. While the approach attempts to solve the stateless requirement, it introduces critical vulnerabilities that could lead to credential exposure and compromise.

**Key Context:** The poc-react application will be a **pure client-side React SPA** served as static files from the Quarkus API server. There will be **no Node.js server-side component**, meaning server-side session management (like NextAuth.js) is not possible.

**Recommendation:** ❌ **DO NOT IMPLEMENT** as proposed. See Alternative Approaches section.

---

## Proposed Architecture Analysis

### What's Being Proposed

1. **Encrypt Kafka credentials** (SCRAM username/password or OAuth client secrets) into a JWE token
2. **Store in HTTP-only cookie** with Secure, SameSite=Strict flags
3. **Stateless backend** - decrypt on each request, build Kafka client, discard credentials
4. **Sliding expiration** - reissue token on each request if near expiry (~30 min lifetime)
5. **No refresh tokens** - cookie-based session handles renewal

### Current Implementation Context

#### Backend: [`ConsoleAuthenticationMechanism.java`](api/src/main/java/com/github/streamshub/console/api/security/ConsoleAuthenticationMechanism.java)

- **Supported auth methods:**
  - OIDC (via Quarkus OIDC extension)
  - Per-request Basic Auth (SCRAM/PLAIN)
  - Per-request Bearer tokens (OAuth)
  
- **Backend credential flow:**
  - Credentials provided in `Authorization` header on each request
  - Parsed and used to create Kafka Admin/Consumer/Producer clients
  - No server-side credential storage in backend
  - Credentials discarded after request completes

#### Frontend: Next.js Application (Current)

**⚠️ IMPORTANT:** The existing Next.js application **already implements** a similar approach to what's being proposed:

From [`ui/utils/session.ts`](ui/utils/session.ts) and [`ui/app/api/auth/[...nextauth]/scram.ts`](ui/app/api/auth/[...nextauth]/scram.ts):

- **NextAuth.js session management:**
  - Uses `iron-session` library for encryption
  - Stores encrypted credentials in HTTP-only cookies
  - Encryption key: `NEXTAUTH_SECRET` environment variable
  - Cookie format: `{username}:{scope}` (e.g., `user1:recent-topics`)

- **Credential storage flow:**
  1. User authenticates via NextAuth providers (SCRAM, OAuth, OIDC)
  2. Credentials encoded as Base64 Authorization header value
  3. Stored in NextAuth session via JWT callback
  4. Session encrypted with `iron-session` using `NEXTAUTH_SECRET`
  5. Encrypted session stored in HTTP-only cookie
  6. On each request, session decrypted and Authorization header reconstructed

- **Example from SCRAM provider:**
```typescript
// ui/app/api/auth/[...nextauth]/scram.ts
const basicAuth = bytesToBase64(
  new TextEncoder().encode(`${username}:${password}`)
);
return {
  id: "1",
  name: credentials!.username,
  authorization: `Basic ${basicAuth}`, // Stored in session
};
```

- **Session encryption/decryption:**
```typescript
// ui/utils/session.ts
const encryptedSession = await sealData(session, {
  password: process.env.NEXTAUTH_SECRET, // Single encryption key
});

const rawSession = await unsealData(encryptedSession, {
  password: process.env.NEXTAUTH_SECRET,
});
```

**Key Point:** The proposed approach is essentially **what Next.js already does**, just moving from `iron-session` to JWE and from NextAuth session to custom implementation.

**⚠️ CRITICAL ARCHITECTURAL DIFFERENCE:**

The Next.js implementation uses **server-side session management** with NextAuth.js running on a Node.js server. The poc-react application will be a **pure client-side SPA** with no server-side component:

- **Next.js (current):** Node.js server → NextAuth.js → iron-session → HTTP-only cookies
- **poc-react (proposed):** Static files served by Quarkus → All React code runs in browser → JWE cookies managed by Quarkus API

**This means:**
- No server-side session state possible in poc-react
- No NextAuth.js or similar server-side auth framework
- All authentication logic must be in Quarkus backend or browser
- Cannot port NextAuth session logic to poc-react (no server to run it on)

---

## Critical Security Risks

### 🔴 RISK 1: Credential Exposure via Token Theft

**Severity:** CRITICAL  
**Likelihood:** HIGH

**Problem:**
- JWE tokens containing plaintext credentials stored in cookies can be stolen via:
  - XSS attacks (despite HttpOnly, other vulnerabilities exist)
  - Browser extensions with cookie access
  - Malware on client machine
  - Network interception if HTTPS is misconfigured
  - Browser history/cache in some edge cases

**Impact:**
- Attacker gains **long-lived Kafka credentials** (30 min minimum)
- Can authenticate to Kafka cluster directly
- Can perform any operation the user is authorized for
- Credentials remain valid even after token expiry (if SCRAM/OAuth client secret)

**Why This Is Worse Than Current Approach:**
- Current: Credentials only in memory during request, never stored
- Proposed: Credentials stored in browser for 30 minutes minimum

### 🔴 RISK 2: Encryption Key Management Complexity

**Severity:** HIGH
**Likelihood:** MEDIUM

**Problem:**
- JWE encryption requires secure key management:
  - Where is the encryption key stored?
  - How is it rotated?
  - What happens if key is compromised?
  - How is key distributed in multi-instance deployments?

**Impact:**
- Key compromise = all stored credentials compromised
- Key rotation = all active sessions invalidated
- Key management errors = security vulnerabilities

**Current Next.js Implementation:**
- Uses single `NEXTAUTH_SECRET` environment variable for all encryption
- **No key rotation mechanism** - changing key invalidates all sessions
- Same key used across all application instances
- Key stored in environment variables (potential exposure risk)
- If key leaks, all historical and current sessions compromised

**⚠️ This risk already exists in the current Next.js application!**

**Proposed Implementation:**
- Would use similar approach (single encryption key)
- Same key management challenges
- **No improvement over current situation**

### 🔴 RISK 3: Credential Lifetime Extension

**Severity:** HIGH  
**Likelihood:** HIGH

**Problem:**
- Sliding expiration means credentials are continuously renewed
- User could stay authenticated indefinitely with active usage
- No forced re-authentication with actual credentials
- Credentials in token may become stale (password changed, OAuth token revoked)

**Impact:**
- Compromised token provides extended access window
- Stale credentials not detected until token expires
- No way to force immediate credential refresh

### 🟡 RISK 4: Token Size and Performance

**Severity:** MEDIUM  
**Likelihood:** HIGH

**Problem:**
- JWE tokens are large (especially with nested encryption)
- Sent on every request (cookie overhead)
- Contains full JAAS config or OAuth credentials
- Multiple Kafka clusters = multiple credential sets

**Impact:**
- Increased bandwidth usage
- Slower request processing
- Cookie size limits (4KB typical)
- May not fit multiple cluster credentials

### 🟡 RISK 5: CSRF Despite SameSite

**Severity:** MEDIUM  
**Likelihood:** LOW

**Problem:**
- SameSite=Strict helps but isn't foolproof
- Browser bugs and edge cases exist
- Not all browsers fully support SameSite
- Subdomain attacks possible

**Impact:**
- Potential for cross-site request forgery
- Attacker could trigger authenticated requests

### 🟡 RISK 6: Compliance and Audit Concerns

**Severity:** MEDIUM  
**Likelihood:** MEDIUM

**Problem:**
- Storing credentials (even encrypted) in browser may violate:
  - PCI DSS requirements
  - SOC 2 controls
  - Industry-specific regulations
  - Corporate security policies

**Impact:**
- Failed security audits
- Compliance violations
- Inability to deploy in regulated environments

---

## Security Best Practices Violations

### ❌ Principle of Least Privilege
Credentials stored longer than necessary (30 min vs. per-request)

### ❌ Defense in Depth
Single point of failure (encryption key compromise)

### ❌ Minimize Attack Surface
Increases attack surface by storing credentials client-side

### ❌ Secure by Default
Requires perfect implementation to be secure

### ❌ Fail Securely
Key compromise or encryption failure exposes all credentials

---

## Comparison: Next.js Current vs. Proposed vs. Backend-Only

| Aspect | Next.js Current (iron-session) | Proposed (JWE Cookie) | Backend Per-Request Auth |
|--------|-------------------------------|----------------------|--------------------------|
| **Credential Storage** | Browser cookie (NextAuth session) | Browser cookie (30 min) | None (Authorization header) |
| **Encryption Library** | iron-session | SmallRye JWT (JWE) | N/A |
| **Encryption Key** | `NEXTAUTH_SECRET` env var | Custom key (TBD) | Not required |

---

## NEW OPTION EVALUATION: Server-Side Session Cache with Opaque Tokens

### ✅ OPTION 6: Session Cache with Opaque Tokens (RECOMMENDED - BEST BALANCE)

**Description:**
- User provides credentials on login
- Quarkus encrypts and stores credentials in cache (local or Redis)
- Returns opaque session token to browser
- Token sent on each request (HTTP-only cookie or Authorization header)
- Quarkus retrieves and decrypts credentials for Kafka operations
- Session TTL: 60 min inactivity timeout, 24 hour absolute maximum

**Architecture:**
```
Login Flow:
User → Credentials → Quarkus API
                        ↓
                   Encrypt credentials
                        ↓
                   Store in cache (TTL: 60m/24h)
                        ↓
                   Generate opaque token (UUID)
                        ↓
                   Return token → Browser (HTTP-only cookie)

Request Flow:
Browser → Token (cookie) → Quarkus API
                              ↓
                         Lookup in cache
                              ↓
                         Decrypt credentials
                              ↓
                         Use with Kafka
                              ↓
                         Extend TTL (if < 24h)
```

### Security Analysis

**✅ Significant Security Improvements:**

1. **No credentials in browser**
   - Only opaque token stored client-side
   - Token is meaningless without cache access
   - Stolen token cannot reveal credentials

2. **Server-side credential storage**
   - Credentials encrypted at rest in cache
   - Encryption key never leaves server
   - Can use hardware security modules (HSM)

3. **Session lifecycle control**
   - Server can invalidate sessions immediately
   - Automatic expiration after inactivity
   - Absolute maximum lifetime enforced

4. **Better key management**
   - Encryption key only on server
   - Can rotate keys without invalidating sessions
   - Separate key per session possible

5. **Audit and monitoring**
   - All credential access logged server-side
   - Can detect unusual patterns
   - Session activity tracking

**⚠️ Considerations:**

1. **Requires session store**
   - Local cache (Caffeine) for single instance
   - Redis/Infinispan for multi-instance
   - Adds infrastructure dependency

2. **Not truly stateless**
   - Violates original stateless requirement
   - But much more secure than client-side storage

3. **Cache availability**
   - Cache failure = all sessions lost
   - Need cache persistence/replication for HA

### Implementation Details

**Session Token Format:**
```java
// Opaque, cryptographically random token
String sessionToken = UUID.randomUUID().toString();
// Or use SecureRandom for better entropy
```

**Cache Entry Structure:**
```java
class SessionData {
    String encryptedCredentials;  // AES-256 encrypted
    String username;
    Instant createdAt;
    Instant lastAccessedAt;
    Instant expiresAt;
}
```

**Token Delivery: HTTP-only Cookie vs. Authorization Header**

**✅ RECOMMENDED: HTTP-only Cookie**

**Pros:**
- ✅ Automatic inclusion on requests
- ✅ Cannot be accessed by JavaScript (XSS protection)
- ✅ SameSite=Strict prevents CSRF
- ✅ Secure flag ensures HTTPS only
- ✅ Standard session management pattern

**Cons:**
- ⚠️ Requires CSRF protection for state-changing operations
- ⚠️ Cookie size limits (not an issue for opaque token)

**Cookie Configuration:**
```java
Cookie sessionCookie = Cookie.cookie("session", sessionToken)
    .setHttpOnly(true)
    .setSecure(true)
    .setSameSite(CookieSameSite.STRICT)
    .setMaxAge(86400)  // 24 hours
    .setPath("/api");
```

**⚠️ Alternative: Authorization Header**

**Pros:**
- ✅ No CSRF concerns
- ✅ Works with CORS
- ✅ Explicit per-request

**Cons:**
- ❌ Must be stored in browser (localStorage/sessionStorage)
- ❌ Accessible to JavaScript (XSS risk)
- ❌ Manual inclusion on each request
- ❌ Lost on page refresh unless persisted

**Verdict: Use HTTP-only Cookie for better security**

### Session Lifecycle

**Login:**
```java
@POST
@Path("/auth/login")
public Response login(Credentials creds) {
    // Validate credentials against Kafka
    if (validateKafkaCredentials(creds)) {
        // Encrypt credentials
        String encrypted = encrypt(creds);
        
        // Generate session token
        String token = generateSecureToken();
        
        // Store in cache with TTL
        SessionData session = new SessionData(
            encrypted,
            creds.username,
            Instant.now(),
            Instant.now(),
            Instant.now().plus(24, ChronoUnit.HOURS)
        );
        cache.put(token, session, 60, TimeUnit.MINUTES);
        
        // Return cookie
        return Response.ok()
            .cookie(createSessionCookie(token))
            .build();
    }
    return Response.status(401).build();
}
```

**Request Processing:**
```java
@GET
@Path("/api/kafkas/{id}/topics")
public Response getTopics(@PathParam("id") String clusterId,
                         @CookieParam("session") String sessionToken) {
    // Lookup session
    SessionData session = cache.get(sessionToken);
    if (session == null || session.isExpired()) {
        return Response.status(401).build();
    }
    
    // Decrypt credentials
    Credentials creds = decrypt(session.encryptedCredentials);
    
    // Use with Kafka
    List<Topic> topics = kafkaClient.getTopics(clusterId, creds);
    
    // Extend TTL if not at max lifetime
    if (session.canExtend()) {
        cache.put(sessionToken, session.updateLastAccessed(), 
                 60, TimeUnit.MINUTES);
    }
    
    return Response.ok(topics).build();
}
```

**Logout:**
```java
@POST
@Path("/auth/logout")
public Response logout(@CookieParam("session") String sessionToken) {
    // Remove from cache
    cache.remove(sessionToken);
    
    // Clear cookie
    return Response.ok()
        .cookie(createExpiredSessionCookie())
        .build();
}
```

### Cache Options

**Option A: Local Cache (Caffeine)**
```java
@Inject
@CacheName("sessions")
Cache<String, SessionData> sessionCache;
```

**Pros:**
- ✅ Simple, no external dependencies
- ✅ Fast (in-memory)
- ✅ Good for single instance

**Cons:**
- ❌ Lost on restart
- ❌ Not shared across instances
- ❌ No persistence

**Option B: Redis**
```java
@Inject
RedisClient redis;

// Store session
redis.setex(sessionToken, 3600, serialize(session));

// Retrieve session
SessionData session = deserialize(redis.get(sessionToken));
```

**Pros:**
- ✅ Shared across instances
- ✅ Persistence options
- ✅ High availability
- ✅ TTL built-in

**Cons:**
- ❌ External dependency
- ❌ Network latency
- ❌ More complex setup

**Recommendation:** Start with Caffeine, migrate to Redis for production HA

### Risks of Opaque Token Theft

**⚠️ IMPORTANT: Token theft is still a risk, but the impact is significantly reduced compared to JWE cookies.**

#### What Happens If Token Is Stolen?

**Attack Scenario:**
1. Attacker obtains session token via XSS, network interception, or malware
2. Attacker uses token to make API requests
3. Quarkus retrieves credentials from cache and processes requests
4. Attacker gains access to Kafka operations **for the session lifetime**

**Impact Analysis:**

| Aspect | Opaque Token Theft | JWE Cookie Theft |
|--------|-------------------|------------------|
| **Immediate Impact** | 🟡 Session access | 🔴 Session access |
| **Credential Exposure** | ✅ No - credentials stay on server | ❌ Yes - attacker can decrypt |
| **Attack Duration** | 🟡 Until session expires (max 24h) | 🔴 Until token expires + credential lifetime |
| **Offline Attack** | ✅ No - requires API access | ❌ Yes - can decrypt offline |
| **Credential Reuse** | ✅ No - token is session-specific | ❌ Yes - credentials work elsewhere |
| **Detection** | 🟢 Easier - unusual API patterns | 🔴 Harder - looks like normal usage |
| **Mitigation** | ✅ Invalidate session immediately | ⚠️ Cannot invalidate until expiry |
| **Long-term Risk** | 🟢 Low - session expires | 🔴 High - credentials may be long-lived |

#### Detailed Risk Breakdown

**1. Session Hijacking (Both Approaches)**
- **Risk:** Attacker can impersonate user for session lifetime
- **Duration:** Up to 24 hours (or until inactivity timeout)
- **Mitigation:** See "Token Theft Mitigations" below

**2. Credential Exposure (JWE Only)**
- **Risk:** Attacker obtains actual Kafka credentials
- **Duration:** Until credentials are rotated (could be months/years)
- **Impact:** Can authenticate directly to Kafka, bypassing console
- **Mitigation:** Not possible with JWE approach

**3. Offline Attacks (JWE Only)**
- **Risk:** Attacker can decrypt JWE offline without API access
- **Impact:** Credentials exposed even if API is secured
- **Mitigation:** Not possible with JWE approach

**4. Credential Reuse (JWE Only)**
- **Risk:** Stolen credentials work on other systems
- **Impact:** Broader compromise beyond console
- **Mitigation:** Not possible with JWE approach

#### Token Theft Mitigations

**Required Security Controls:**

1. **Token Binding**
```java
class SessionData {
    String encryptedCredentials;
    String username;
    String ipAddress;        // Bind to IP
    String userAgent;        // Bind to user agent
    Instant createdAt;
    Instant lastAccessedAt;
}

// Validate on each request
if (!session.ipAddress.equals(request.getRemoteAddr())) {
    // Potential token theft - invalidate session
    cache.remove(sessionToken);
    return Response.status(401).build();
}
```

**Pros:**
- ✅ Detects token theft if used from different location
- ✅ Automatic protection

**Cons:**
- ⚠️ Breaks legitimate use cases (VPN changes, mobile switching networks)
- ⚠️ Can be bypassed if attacker proxies through victim's network

**Recommendation:** Use for high-security environments, make optional for others

2. **Anomaly Detection**
```java
// Track request patterns
class SessionActivity {
    List<RequestLog> recentRequests;
    
    boolean isAnomalous(Request current) {
        // Detect unusual patterns:
        // - Sudden geographic change
        // - Unusual request frequency
        // - Access to resources not previously accessed
        // - Different browser fingerprint
    }
}

// On anomalous activity
if (sessionActivity.isAnomalous(request)) {
    // Challenge user (MFA, re-auth)
    // Or invalidate session
}
```

3. **Short Session Lifetime**
```java
// Aggressive timeouts
int INACTIVITY_TIMEOUT = 15; // minutes (not 60)
int ABSOLUTE_TIMEOUT = 8;    // hours (not 24)

// Force re-authentication more frequently
```

**Trade-off:** Better security vs. user convenience

4. **Activity Logging**
```java
// Log all session activity
logger.info("Session {} accessed from {} at {} for resource {}",
    sessionToken, ipAddress, timestamp, resource);

// Enable security monitoring
// Alert on suspicious patterns
```

5. **Secure Cookie Flags (Already Recommended)**
```java
Cookie sessionCookie = Cookie.cookie("session", sessionToken)
    .setHttpOnly(true)      // Prevents JavaScript access (XSS)
    .setSecure(true)        // HTTPS only
    .setSameSite(STRICT)    // Prevents CSRF
    .setPath("/api");       // Limit scope
```

6. **Rate Limiting**
```java
// Limit requests per session
@RateLimit(value = 100, window = "1m")
public Response getTopics(@CookieParam("session") String token) {
    // Prevents abuse of stolen token
}
```

7. **Explicit Logout**
```java
// Provide logout endpoint
@POST
@Path("/auth/logout")
public Response logout(@CookieParam("session") String token) {
    cache.remove(token);  // Immediate invalidation
    return Response.ok()
        .cookie(createExpiredCookie())
        .build();
}
```

8. **Session Invalidation API**
```java
// Allow users to view/revoke active sessions
@GET
@Path("/auth/sessions")
public List<SessionInfo> getActiveSessions() {
    // Show user their active sessions
}

@DELETE
@Path("/auth/sessions/{sessionId}")
public Response revokeSession(@PathParam("sessionId") String id) {
    cache.remove(id);
    return Response.ok().build();
}
```

#### Comparison: Token Theft Impact

**Scenario: Attacker steals token via XSS**

**With Opaque Token (Option 6):**
1. Attacker gets session token (UUID)
2. Can make API requests for session lifetime (max 24h)
3. **Cannot** extract Kafka credentials
4. **Cannot** use credentials elsewhere
5. Session can be invalidated immediately if detected
6. Attack ends when session expires
7. **Credentials remain secure on server**

**With JWE Cookie (Option 5):**
1. Attacker gets JWE token
2. Can make API requests for token lifetime (30 min)
3. **Can** decrypt and extract Kafka credentials
4. **Can** use credentials directly with Kafka (bypassing console)
5. **Cannot** invalidate token (stateless)
6. Attack continues until credentials are rotated
7. **Credentials are compromised**

#### Real-World Attack Scenarios

**Scenario 1: XSS Attack**
- **Opaque Token:** Session hijacked, but credentials safe
- **JWE Cookie:** Credentials stolen, can be used indefinitely

**Scenario 2: Malware on User's Machine**
- **Opaque Token:** Session access until detected/expired
- **JWE Cookie:** Credentials exfiltrated, used elsewhere

**Scenario 3: Network Interception (HTTPS misconfigured)**
- **Opaque Token:** Session compromised, limited window
- **JWE Cookie:** Credentials compromised, unlimited window

**Scenario 4: Insider Threat (Employee with API access)**
- **Opaque Token:** Can access during session, logged
- **JWE Cookie:** Can extract credentials, use offline

#### Risk Acceptance

**With Opaque Tokens, you accept:**
- ✅ Session hijacking is possible (standard web app risk)
- ✅ Attacker can impersonate user temporarily
- ✅ Requires monitoring and detection
- ✅ Need logout and session management features

**You avoid:**
- ✅ Credential exposure
- ✅ Long-term compromise
- ✅ Offline attacks
- ✅ Credential reuse attacks

#### Recommendation

**Implement comprehensive defense-in-depth:**

1. **Prevent theft:**
   - HTTPS everywhere
   - Secure cookie flags
   - XSS protection (CSP, input validation)
   - Regular security audits

2. **Detect theft:**
   - Token binding (IP, user agent)
   - Anomaly detection
   - Activity logging
   - Security monitoring

3. **Limit impact:**
   - Short session lifetimes
   - Rate limiting
   - Scope limitations
   - Immediate invalidation capability

4. **Enable response:**
   - Session management UI
   - Logout functionality
   - Audit logs
   - Incident response procedures

**Bottom Line:** Token theft is a risk with any session-based authentication, but opaque tokens significantly limit the damage compared to storing credentials in cookies. The key is implementing proper security controls and monitoring.


### Security Comparison

| Aspect | JWE Cookie (Option 5) | Session Cache (Option 6) |
|--------|----------------------|--------------------------|
| **Credentials in Browser** | ❌ Yes (encrypted) | ✅ No (opaque token only) |
| **Token Theft Impact** | 🔴 Credentials exposed | 🟢 Token useless without cache |
| **Session Invalidation** | ❌ Not possible | ✅ Immediate |
| **Key Management** | ⚠️ Client-side risk | ✅ Server-side only |
| **Credential Rotation** | ❌ Requires re-login | ✅ Update cache entry |
| **Audit Trail** | ⚠️ Limited | ✅ Complete server-side |
| **Compliance** | ⚠️ May violate | ✅ Standard practice |
| **Stateless** | ✅ Yes | ❌ No (requires cache) |
| **Horizontal Scaling** | ✅ Easy | ⚠️ Needs shared cache |
| **Implementation Complexity** | 🟡 Medium | 🟡 Medium |

### Recommendation

**✅ This is the BEST option for poc-react** because it:

1. **Significantly more secure** than JWE cookies
2. **Familiar UX** - credentials persist across page refresh
3. **Standard pattern** - widely used and understood
4. **Flexible** - can start with local cache, scale to Redis
5. **Compliant** - meets most security standards
6. **Manageable** - server controls session lifecycle

**Trade-offs Accepted:**
- Not truly stateless (requires session cache)
- Adds cache infrastructure dependency
- Slightly more complex than in-memory only

**This is essentially Option 3 (Session-Based Authentication) but with explicit evaluation for poc-react context.**

| **Key Rotation** | ❌ Not supported | ❌ Not supported | N/A |
| **Credential Lifetime** | Session duration (configurable) | 30 minutes minimum | Single request (~seconds) |
| **Attack Window** | Session duration | 30+ minutes | Milliseconds |
| **Key Management** | ⚠️ Single env var | ⚠️ Complex, critical | ✅ Not required |
| **Token Theft Impact** | Session compromised | 30 min of access | Single request compromised |
| **Credential Staleness** | Delayed until session expiry | Delayed until expiry | Immediate detection |
| **Compliance** | ⚠️ May violate policies | ⚠️ May violate policies | ✅ Standard practice |
| **Implementation Complexity** | Medium (NextAuth) | High (custom) | Low |
| **Security Audit** | ⚠️ Requires justification | ⚠️ Requires justification | ✅ Straightforward |
| **Already Implemented** | ✅ Yes (Next.js) | ❌ No | ✅ Yes (Backend) |

**Key Insight:** The proposed approach has **similar security properties** to the current Next.js implementation, with the main difference being the encryption library used (iron-session vs. JWE).

---

## Alternative Approaches (Recommended)

### ✅ OPTION 1: Keep Current Per-Request Authentication (RECOMMENDED FOR SECURITY)

**Description:**
- Continue using Authorization header on each request
- React app stores credentials in memory only (React state/context)
- Never persist credentials to localStorage/cookies
- Re-authenticate on page refresh

**Pros:**
- ✅ **Significantly more secure** than cookie-based storage
- ✅ Minimal attack surface
- ✅ No credential storage
- ✅ Simple implementation
- ✅ Compliant with security standards
- ✅ Already implemented in backend
- ✅ No encryption key management needed

**Cons:**
- ❌ User must re-enter credentials on page refresh
- ❌ No "remember me" functionality
- ❌ **Different UX than current Next.js app**

**Migration Note:**
This would be a **security improvement** over the current Next.js implementation, but requires accepting the UX trade-off of re-authentication on page refresh.

**Implementation:**
```typescript
// React Context
const AuthContext = createContext<{
  credentials: KafkaCredentials | null;
  setCredentials: (creds: KafkaCredentials) => void;
}>();

// API Client
const apiClient = {
  async fetchTopics(clusterId: string, credentials: KafkaCredentials) {
    const headers = {
      'Authorization': credentials.type === 'basic' 
        ? `Basic ${btoa(`${credentials.username}:${credentials.password}`)}`
        : `Bearer ${credentials.token}`
    };
    return fetch(`/api/kafkas/${clusterId}/topics`, { headers });
  }
};
```

**Security Properties:**
- Credentials only in memory
- Cleared on page refresh/close
- No persistent storage
- Minimal attack window

---

### ✅ OPTION 2: OIDC with Backend Credential Proxy (RECOMMENDED FOR PRODUCTION)

**Description:**
- Use OIDC for user authentication (already supported)
- Backend stores Kafka credentials securely (encrypted at rest)
- Backend maps OIDC user to Kafka credentials
- React app only handles OIDC tokens

**Pros:**
- ✅ No credentials in browser
- ✅ Centralized credential management
- ✅ Credential rotation without user action
- ✅ Audit trail
- ✅ Supports SSO

**Cons:**
- ❌ Requires backend state (credential store)
- ❌ More complex infrastructure

**Architecture:**
```
User → OIDC Provider → React App (OIDC token)
                          ↓
                    Quarkus API (validates OIDC)
                          ↓
                    Credential Store (encrypted)
                          ↓
                    Kafka Cluster (SCRAM/OAuth)
```

**Implementation Notes:**
- Store credentials in Kubernetes Secrets or HashiCorp Vault
- Map OIDC subject/groups to Kafka credentials
- Backend handles all Kafka authentication
- Already partially implemented in [`ConsoleAuthenticationMechanism.java`](api/src/main/java/com/github/streamshub/console/api/security/ConsoleAuthenticationMechanism.java:108-110)

---

### ⚠️ OPTION 3: Session-Based Authentication (ACCEPTABLE)

**Description:**
- Traditional server-side sessions
- Session ID in HTTP-only cookie
- Credentials stored server-side (encrypted)
- Session expires after inactivity

**Pros:**
- ✅ No credentials in browser
- ✅ Server controls session lifecycle
- ✅ Can force logout
- ✅ Standard pattern

**Cons:**
- ❌ Requires server-side state (violates stateless requirement)
- ❌ Session store needed (Redis, database)
- ❌ Horizontal scaling complexity

**When to Use:**
- If stateless requirement is flexible
- If session store infrastructure exists
- If credential rotation is needed

---

### ❌ OPTION 4: Keep Current Next.js Approach (iron-session) - NOT POSSIBLE

**Description:**
- Continue using NextAuth.js with iron-session encryption
- Maintain current UX (credentials persist across page refreshes)

**Why This Won't Work for poc-react:**
- ❌ **Requires Node.js server** - NextAuth.js is server-side
- ❌ **poc-react is pure client-side SPA** - no server component
- ❌ **Static files served by Quarkus** - no Node.js runtime
- ❌ **Cannot run server-side session management** in browser

**Architectural Constraint:**
The poc-react application will be served as static files from the Quarkus API server. All React code runs in the browser. There is no Node.js server to run NextAuth.js or iron-session encryption.

**This option is included for comparison only - it cannot be implemented in poc-react.**

---

### ❌ OPTION 5: Modified JWE Approach (NOT RECOMMENDED)

If you must use JWE cookies despite risks, implement these mitigations:

**Required Mitigations:**
1. **Short token lifetime** (5 min max, not 30 min)
2. **No sliding expiration** (force re-authentication)
3. **Secure key management** (HSM or key management service)
4. **Key rotation** (automated, frequent)
5. **Token binding** (bind to IP, user agent)
6. **Audit logging** (all token issuance/usage)
7. **Anomaly detection** (unusual usage patterns)
8. **Explicit user consent** (inform about credential storage)

**Still Problematic:**
- Doesn't eliminate core risks
- Adds significant complexity
- May still fail compliance audits

---

## Analysis: Is Proposed Approach Better Than Current?

### Security Comparison

**Current Next.js (iron-session):**
- ✅ Mature, audited library
- ✅ Used by thousands of applications
- ⚠️ Single encryption key
- ⚠️ No key rotation
- ⚠️ Credentials in browser

**Proposed (JWE with SmallRye JWT):**
- ✅ Standard JWE format
- ✅ Quarkus-native library
- ⚠️ Single encryption key (same issue)
- ⚠️ No key rotation (same issue)
- ⚠️ Credentials in browser (same issue)
- ❌ Custom implementation (more risk)
- ❌ Less battle-tested for this use case

### Verdict

The proposed approach is **not significantly better** than the current Next.js implementation from a security perspective. Both approaches:
- Store credentials in encrypted cookies
- Use a single encryption key
- Lack key rotation mechanisms
- Have similar attack surfaces

**The main difference is the encryption library used, not the fundamental security model.**

### Recommendation for poc-react Migration

**Critical Constraint:** poc-react is a pure client-side SPA with no Node.js server, so server-side session management with NextAuth (Option 4) is **not possible**.

**🎯 RECOMMENDED: Option 6 - Session Cache with Opaque Tokens**

This is the **best balance** of security, UX, and implementation complexity:
- ✅ Credentials never in browser (only opaque token)
- ✅ UX continuity (no re-auth on refresh)
- ✅ Server controls session lifecycle
- ✅ Standard, well-understood pattern
- ✅ Meets most compliance requirements
- ⚠️ Requires session cache (Caffeine or Redis)

**See detailed evaluation above in "NEW OPTION EVALUATION" section.**

**Alternative Options:**

1. **Best Security (Option 1):** Per-request auth with in-memory storage
   - ✅ Most secure
   - ❌ Re-auth on page refresh
   
2. **Credential Persistence (Option 5):** JWE cookie approach
   - ✅ Maintains UX (no re-auth on refresh)
   - ⚠️ Same security risks as Next.js
   - ⚠️ **Not recommended** - use Option 6 instead

3. **Production Ready (Option 2):** OIDC + backend credential proxy
   - ✅ Best long-term solution
   - ❌ Most complex

**Bottom Line:** Option 6 (session cache) is significantly more secure than Option 5 (JWE cookies) while maintaining the same UX. The trade-off is accepting server-side state, which is standard practice for authentication.

---

## Specific Concerns for Kafka Credentials

### SCRAM Credentials
- **Problem:** Username/password are long-lived
- **Risk:** Stolen token = permanent access until password changed
- **Mitigation:** Very short token lifetime (5 min max)

### OAuth Client Credentials
- **Problem:** Client secret is long-lived
- **Risk:** Stolen token = access until secret rotated
- **Mitigation:** Use OAuth token flow instead (access token in header)

### OAuth Access Tokens
- **Less Risky:** Access tokens are already short-lived
- **Better Approach:** Store in memory, pass in Authorization header
- **Current Implementation:** Already supported in [`ConsoleAuthenticationMechanism.java`](api/src/main/java/com/github/streamshub/console/api/security/ConsoleAuthenticationMechanism.java:297-317)

---

## Implementation Recommendations

### For poc-react Application

**Phase 1: MVP (Recommended)**
```typescript
// 1. In-memory credential storage
interface AuthState {
  credentials: {
    [clusterId: string]: {
      type: 'basic' | 'bearer';
      username?: string;
      password?: string;
      token?: string;
    }
  };
}

// 2. React Context for auth state
const useAuth = () => {
  const [auth, setAuth] = useState<AuthState>({ credentials: {} });
  
  // Credentials cleared on unmount/refresh
  return { auth, setAuth };
};

// 3. API client with per-request auth
const apiClient = {
  setCredentials(clusterId: string, creds: Credentials) {
    // Store in memory only
  },
  
  async request(url: string, clusterId: string) {
    const creds = getCredentials(clusterId);
    const headers = buildAuthHeader(creds);
    return fetch(url, { headers });
  }
};
```

**Phase 2: Production (If Needed)**
- Implement OIDC integration
- Backend credential proxy
- Vault integration for credential storage

---

## Security Checklist

Before implementing any authentication approach:

- [ ] Threat model completed
- [ ] Security review by security team
- [ ] Compliance requirements verified
- [ ] Penetration testing planned
- [ ] Incident response plan updated
- [ ] User documentation includes security guidance
- [ ] Audit logging implemented
- [ ] Monitoring and alerting configured

---

## Conclusion

The proposed JWT/JWE cookie approach for storing Kafka credentials **does not improve security** over the current Next.js implementation and introduces additional complexity.

### Key Takeaways

1. **Current Next.js already uses encrypted cookies** - via iron-session with `NEXTAUTH_SECRET`
2. **Proposed approach has same security risks** - just different encryption library
3. **Both approaches share key management problems** - single key, no rotation
4. **Backend per-request auth is more secure** - but different UX
5. **Encryption key rotation is already a problem** - in current Next.js app

### Critical Insight

**The proposed approach is essentially reimplementing what Next.js already does**, just with a different encryption library (JWE instead of iron-session). It does not address the fundamental security concerns of storing credentials in browser cookies.

### Recommended Path Forward

**For poc-react Migration:**

1. **If UX continuity is priority:**
   - ⚠️ **Cannot use iron-session approach** (requires Node.js server)
   - Must use JWE cookies managed by Quarkus backend (Option 5)
   - Accept security trade-offs of credential storage
   - **Same security risks as Next.js, but different implementation**

2. **If security improvement is priority:**
   - Use in-memory credential storage (Option 1)
   - Per-request Authorization headers
   - Accept page refresh requires re-auth
   - **Significant security improvement, different UX**

3. **If production-ready security needed:**
   - Implement OIDC + backend credential proxy (Option 2)
   - Centralized credential management
   - Proper audit logging
   - **Best security, most complex**

4. **If you must store credentials (despite risks):**
   - Implement JWE cookie approach (Option 5) with all mitigations
   - **This is the only way to persist credentials in poc-react**
   - Accept that it has same security risks as Next.js
   - Implement comprehensive security controls (see Option 5 mitigations)

**Critical Decision Point:**

Since poc-react has **no server-side component**, you must choose between:
- **Option 1:** In-memory only (secure, but re-auth on refresh)
- **Option 5:** JWE cookies (same risks as Next.js, but only option for persistence)

**Option 4 (iron-session) is not possible** without a Node.js server.

---

## References

- [OWASP Authentication Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html)
- [OWASP Session Management Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html)
- [RFC 7519 - JSON Web Token (JWT)](https://tools.ietf.org/html/rfc7519)
- [RFC 7516 - JSON Web Encryption (JWE)](https://tools.ietf.org/html/rfc7516)
- Current implementation: [`ConsoleAuthenticationMechanism.java`](api/src/main/java/com/github/streamshub/console/api/security/ConsoleAuthenticationMechanism.java)

---

## Questions for Discussion

1. **Is current Next.js security posture acceptable?** If yes, keep iron-session approach
2. **What is the threat model?** Who are we protecting against?
3. **What are compliance requirements?** PCI, SOC2, etc.?
4. **Is UX continuity critical?** Can users accept re-auth on refresh?
5. **Is OIDC available for production?** Can we use backend credential proxy?
6. **What is the encryption key rotation strategy?** Currently none exists
7. **Has current implementation been security audited?** What were findings?

---

**Next Steps:**
1. Review this document with security team
2. Discuss alternative approaches
3. Define threat model and requirements
4. Choose appropriate authentication strategy
5. Create detailed implementation plan