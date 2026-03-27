# Migration Plan: Next.js to React SPA

## Overview
This document outlines the plan to migrate the StreamsHub Console UI from Next.js to a traditional client-side React application.

## Architecture

### Current (Next.js)
- Next.js 14 App Router with server components
- Server actions for API communication
- NextAuth for authentication
- Server-side rendering and routing

### Target (React SPA)
- Vite + React + TypeScript
- React Router v6 for routing
- TanStack Query for API state management
- Client-side rendering
- Authentication layer added later

## Migration Strategy

### Phase 1: Foundation Setup (Week 1)
- [x] Create migration plan document
- [ ] Set up Vite configuration
- [ ] Install core dependencies
- [ ] Create basic project structure
- [ ] Set up API client (no auth)
- [ ] Configure routing
- [ ] Configure i18n

### Phase 2: Core Infrastructure (Week 1-2)
- [ ] Create API client layer
- [ ] Create TanStack Query hooks
- [ ] Set up routing structure
- [ ] Configure i18n with react-i18next

### Phase 3: Component Migration (Weeks 2-5)
**Week 2: Foundation Components**
- [ ] AppLayout
- [ ] AppHeader
- [ ] AppSidebar
- [ ] ResponsiveTable
- [ ] AlertProvider

**Week 3: Simple Pages**
- [ ] Home page
- [ ] Kafka Overview
- [ ] Nodes List
- [ ] Node Details

**Week 4: Medium Complexity**
- [ ] Topics List
- [ ] Topic Details
- [ ] Consumer Groups List
- [ ] Consumer Group Details

**Week 5: Complex Pages**
- [ ] Create Topic wizard
- [ ] Reset Offsets
- [ ] Kafka Connect
- [ ] Schema Registry

### Phase 4: Testing & Validation (Week 6)
- [ ] Functional testing
- [ ] Performance testing
- [ ] Cross-browser testing

### Phase 5: Authentication Layer (Week 7 - Optional)
- [ ] Add OIDC support
- [ ] Add protected routes
- [ ] Update API client for auth

## Technology Stack

- **Build Tool:** Vite
- **Routing:** React Router v6
- **State Management:** TanStack Query + Zustand
- **i18n:** react-i18next
- **UI Components:** PatternFly React (existing)
- **Authentication:** To be added later

## Development Workflow

```bash
# Terminal 1: Start Quarkus API
cd api
mvn quarkus:dev

# Terminal 2: Start React dev server
cd ui/poc-react
npm run dev
```

## Key Decisions

1. **Authentication Later:** Focus on core functionality first, add auth as a layer
2. **No CORS Initially:** Use Vite proxy for development
3. **Incremental Migration:** Migrate components one at a time
4. **Keep Next.js:** Keep existing Next.js code for reference during migration

## Success Criteria

- All pages functional and accessible
- Data loads from API correctly
- Navigation works properly
- Forms and tables work
- Ready to add authentication layer