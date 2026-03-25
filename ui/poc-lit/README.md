# StreamsHub Console - Proof of Concept (Lit + Web Components)

This is a proof-of-concept demonstrating the migration from Next.js/React to Lit + Web Components for the StreamsHub Console, now with **full application routing** and page structure.

## What's Included

### Complete Application Structure

**Home Page:**
- List of all Kafka clusters
- Search functionality
- Click to navigate to cluster details

**Kafka Cluster Pages (with sidebar navigation):**
- **Overview** - Cluster health and statistics (placeholder)
- **Topics** - Full topics list with search, sort, pagination (✅ Complete)
- **Nodes** - Broker nodes (placeholder)
- **Kafka Connect** - Connect clusters and connectors (placeholder)
- **Kafka Users** - User management (placeholder)
- **Groups** - Consumer/Share/Streams groups (placeholder)

### Architecture Highlights
- ✅ **Vaadin Router** for client-side routing
- ✅ **Lit 3.x** for Web Components
- ✅ **@lit/task** for async data fetching
- ✅ **PatternFly CSS** for styling
- ✅ **Vite** for build tooling
- ✅ **TypeScript** for type safety
- ✅ **Layout system** with sidebar navigation
- ✅ **Custom API client** (no server actions)

## Setup

### Prerequisites
- Node.js 18+ 
- Backend API running on `http://localhost:8080`

### Installation

```bash
cd ui/poc-lit
npm install
```

### Development

```bash
npm run dev
```

Open http://localhost:3006 in your browser.

## Application Routes

### Public Routes
- `/` - Home page (list of Kafka clusters)

### Kafka Cluster Routes
All cluster routes follow the pattern `/kafka/{kafkaId}/...`:

- `/kafka/{kafkaId}` - Redirects to overview
- `/kafka/{kafkaId}/overview` - Cluster overview dashboard
- `/kafka/{kafkaId}/topics` - Topics list (fully functional)
- `/kafka/{kafkaId}/nodes` - Broker nodes
- `/kafka/{kafkaId}/kafka-connect` - Kafka Connect
- `/kafka/{kafkaId}/kafka-users` - Kafka users
- `/kafka/{kafkaId}/groups` - Groups (consumer, share, streams)

### Example URLs
```
http://localhost:3006/
http://localhost:3006/kafka/1/overview
http://localhost:3006/kafka/1/topics
http://localhost:3006/kafka/my-cluster/topics
```

## Project Structure

```
poc-lit/
├── src/
│   ├── api/
│   │   ├── client.ts          # API client
│   │   └── topics.ts          # Topics API
│   ├── components/
│   │   ├── topics-page.ts     # Topics page component
│   │   └── topics-table.ts    # Table component
│   ├── layouts/
│   │   └── kafka-layout.ts    # Kafka cluster layout with sidebar
│   ├── pages/
│   │   ├── home-page.ts       # Home page (cluster list)
│   │   ├── overview-page.ts   # Cluster overview
│   │   ├── nodes-page.ts      # Nodes page (placeholder)
│   │   ├── kafka-connect-page.ts
│   │   ├── kafka-users-page.ts
│   │   ├── groups-page.ts
│   │   └── not-found-page.ts  # 404 page
│   ├── router/
│   │   └── routes.ts          # Route configuration
│   ├── utils/
│   │   └── router.ts          # URL parameter extraction
│   └── main.ts                # App entry point
├── index.html                 # HTML shell
├── package.json
├── tsconfig.json
├── vite.config.ts
└── README.md
```

## Key Features Implemented

### 1. Client-Side Routing
- Vaadin Router for SPA navigation
- Lazy-loaded route components
- Nested routes for cluster pages
- 404 handling

### 2. Layout System
- Shared masthead across all pages
- Sidebar navigation for cluster pages
- Active route highlighting
- Responsive design

### 3. Home Page
- Lists all Kafka clusters
- Search functionality
- Click to navigate to cluster
- Loading and error states

### 4. Kafka Layout
- Fetches cluster details
- Renders sidebar navigation
- Highlights active page
- Shows cluster name and type (virtual/regular)

### 5. Topics Page (Fully Functional)
- Custom sortable table
- Search by topic name
- Pagination
- Loading/error/empty states
- Status indicators

## Navigation Labels

The sidebar uses these labels (matching the React app):
- **Cluster overview** (not "Overview")
- **Topics**
- **Kafka nodes** (not "Brokers" or "Nodes")
- **Kafka Connect**
- **Kafka users**
- **Groups** (not "Consumer Groups")

## Key Validations

This PoC validates:

1. ✅ **Lit + PatternFly CSS Integration**
2. ✅ **Custom Table Component**
3. ✅ **API Client Architecture**
4. ✅ **Async Data Loading**
5. ✅ **Bundle Size**
6. ✅ **TypeScript Support**
7. ✅ **URL-based Routing**
8. ✅ **Client-side Router (Vaadin Router)**
9. ✅ **Layout System with Sidebar**
10. ✅ **Multi-page Application Structure**

## Implementation Status

| Page | Status | Notes |
|------|--------|-------|
| Home | ✅ Complete | Cluster list with search |
| Kafka Layout | ✅ Complete | Sidebar navigation |
| Overview | 🟡 Placeholder | Structure in place |
| Topics | ✅ Complete | Full functionality |
| Nodes | 🟡 Placeholder | Structure in place |
| Kafka Connect | ✅ Complete | Tabs for connectors and connect clusters |
| Kafka Users | 🟡 Placeholder | Structure in place |
| Groups | 🟡 Placeholder | Structure in place |

## Next Steps

To complete the PoC:

1. **Implement Overview Page**
   - Cluster health cards
   - Metrics charts (Chart.js)
   - Recent activity

2. **Implement Groups Page**
   - Similar to Topics (reuse table component)
   - Group types (Consumer, Share, Streams)

3. **Add More Features**
   - Topic details page
   - Group details page
   - Create topic form

4. **Polish**
   - Better error handling
   - Loading skeletons
   - Accessibility improvements

## Bundle Size Comparison

### Current Next.js App
```
Total: ~2.5MB (uncompressed)
- Next.js: ~500KB
- React: ~150KB
- PatternFly React: ~1.5MB
- Other: ~350KB
```

### This PoC (estimated with routing)
```
Total: ~150-200KB (uncompressed)
- Lit: ~15KB
- @lit/task: ~5KB
- @vaadin/router: ~20KB
- PatternFly CSS: ~50KB
- App code: ~60-110KB
```

**Reduction: ~92% smaller!**

## Development Tips

### Adding a New Page

1. Create page component in `src/pages/`:
```typescript
import { LitElement, html } from 'lit';
import { customElement } from 'lit/decorators.js';

@customElement('my-page')
export class MyPage extends LitElement {
  render() {
    return html`<h1>My Page</h1>`;
  }
}
```

2. Add route in `src/router/routes.ts`:
```typescript
{
  path: 'my-page',
  component: 'my-page',
  action: async () => {
    await import('../pages/my-page');
  },
}
```

3. Add navigation link in `src/layouts/kafka-layout.ts`

### Debugging Router

Open browser console to see router initialization:
```
StreamsHub Console initialized with Vaadin Router
```

Use browser DevTools to inspect route changes.

## Known Limitations

This PoC doesn't include:
- ❌ Authentication (OIDC/SCRAM-SHA)
- ❌ Internationalization (i18n)
- ❌ Charts/metrics visualization
- ❌ Form validation
- ❌ Error boundaries
- ❌ Unit tests
- ❌ E2E tests
- ❌ Accessibility audit
- ❌ Cross-browser testing

These will be added in subsequent phases.

## Resources

- [Lit Documentation](https://lit.dev/)
- [Vaadin Router](https://vaadin.com/router)
- [PatternFly CSS](https://www.patternfly.org/)
- [Vite Documentation](https://vitejs.dev/)
- [Web Components](https://developer.mozilla.org/en-US/docs/Web/Web_Components)

## Feedback

After testing, consider:

1. **Navigation** - Does the routing feel natural?
2. **Performance** - How does page navigation feel?
3. **Layout** - Is the sidebar navigation intuitive?
4. **Developer Experience** - Is adding new pages straightforward?
5. **Bundle Size** - Check actual build output
6. **Blockers** - Any issues preventing full migration?