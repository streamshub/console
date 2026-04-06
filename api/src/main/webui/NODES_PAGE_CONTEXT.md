# Nodes Page Implementation Context

This document provides complete context for the nodes page implementation in the ui/poc-react project. Use this when continuing work on Phases 4 and 5.

## Overview

The nodes page displays Kafka cluster nodes with two tabs:
1. **Overview Tab** - Shows node statistics, partition distribution chart, and nodes table
2. **Rebalances Tab** - Shows Kafka rebalances with actions (approve, stop, refresh)

## Completed Phases (1-3)

### Phase 1: API Layer ✅

**Files Created:**
- `src/api/hooks/useNodes.ts` - TanStack Query hook for fetching nodes
- `src/api/hooks/useRebalances.ts` - TanStack Query hooks for rebalances
- `src/api/types.ts` (lines 230-485) - Type definitions

**API Hooks Usage:**

```typescript
// Fetch nodes with filtering
const { data, isLoading, error } = useNodes(kafkaId, {
  pageSize: 20,
  pageCursor: 'after:cursor_value',
  sort: 'name',
  sortDir: 'asc',
  nodePool: ['pool1', 'pool2'],
  roles: ['broker', 'controller'],
  brokerStatus: ['Running', 'Starting'],
  controllerStatus: ['QuorumLeader'],
  fields: ['host', 'port', 'rack'],
});

// Fetch rebalances
const { data } = useRebalances(kafkaId, {
  pageSize: 20,
  name: 'search-term',
  status: ['ProposalReady', 'Rebalancing'],
  mode: ['full', 'add-brokers'],
});

// Patch rebalance (approve, stop, refresh)
const { mutate } = usePatchRebalance(kafkaId);
mutate({ rebalanceId: 'id', action: 'approve' });
```

**Type Definitions:**
- `Node` - Node resource with attributes (host, port, rack, roles, broker, controller, storage)
- `NodesResponse` - Paginated response with meta (summary, page) and links
- `NodePools` - Record of node pool names to metadata
- `Statuses` - Status counts for brokers, controllers, combined
- `BrokerStatus` - Union type: 'Running' | 'Starting' | 'ShuttingDown' | etc.
- `ControllerStatus` - Union type: 'QuorumLeader' | 'QuorumFollower' | etc.
- `NodeRoles` - Union type: 'broker' | 'controller'
- `Rebalance` - Rebalance resource with status, mode, brokers, optimizationResult
- `RebalancesResponse` - Paginated rebalance list

### Phase 2: Page Structure ✅

**Files Created:**
- `src/pages/NodesPage.tsx` - Main page with tab navigation
- `src/routes/index.tsx` - Updated with nodes routes
- `src/i18n/messages/en.json` (lines 248-318) - All translations

**Routing Structure:**
```
/kafka/:kafkaId/nodes
  ├── /overview (default) - NodesOverviewTab
  └── /rebalances - NodesRebalancesTab
```

**Navigation:**
- Sidebar already includes "Nodes" link (see `src/components/KafkaClusterSidebar.tsx`)
- Tab switching handled by React Router

### Phase 3: Overview Tab ✅

**Files Created:**
- `src/pages/NodesOverviewTab.tsx` - Complete overview implementation
- `src/pages/NodesRebalancesTab.tsx` - Placeholder for Phase 5

**Overview Tab Features:**
1. **Statistics Card** (left column):
   - Total nodes count with tooltip
   - Controller role count with status icon (warning if issues)
   - Broker role count with status icon (warning if issues)
   - Lead controller ID

2. **Distribution Chart** (right column):
   - Horizontal stacked bar chart showing partition distribution
   - Toggle buttons: All / Leaders / Followers
   - Legend with broker counts and percentages
   - Theme-aware styling (works in dark mode)
   - Responsive width using `useRef` and `useEffect`

3. **Placeholder for Nodes Table** (Phase 4):
   - Currently shows "Nodes table coming in Phase 4" message

**Key Implementation Details:**
- Uses `@patternfly/react-charts/victory` for charts
- Chart has `themeColor={ChartThemeColor.multiOrdered}` for theme support
- Legend uses custom `ChartLegend` with CSS variable for text color:
  ```typescript
  legendComponent={
    <ChartLegend
      style={{
        labels: { fill: 'var(--pf-t--global--text--color--regular)' }
      }}
    />
  }
  ```
- Calculates node counts from `data.meta.summary.statuses`
- Builds distribution data from broker nodes only

## Remaining Work

### Phase 4: Nodes Table (PENDING)

**Requirements:**
Implement a comprehensive table in `NodesOverviewTab.tsx` to replace the placeholder.

**Columns:**
1. **ID** - Node ID with link to configuration page (if broker), "Lead controller" label if applicable
2. **Roles** - Display broker/controller roles
3. **Status** - Show broker and controller status with icons and popovers
4. **Replicas** - Total replicas (leaders + followers) for brokers
5. **Rack** - Rack ID with tooltip
6. **Node Pool** - Node pool name

**Expandable Row Content:**
- Host name (with ClipboardCopy component)
- Disk usage (donut chart showing used/available capacity)
- Kafka version

**Filters:**
1. **Node Pool** (checkbox filter):
   - Options from `data.meta.summary.nodePools`
   - Show count next to each pool name
   - Show pool roles in description

2. **Role** (checkbox filter):
   - Options: broker, controller
   - Show count next to each role

3. **Status** (grouped checkbox filter):
   - Group 1: Broker statuses (Running, Starting, ShuttingDown, etc.)
   - Group 2: Controller statuses (QuorumLeader, QuorumFollower, etc.)
   - Show count next to each status

**Features:**
- Pagination (20 per page default)
- Sorting by all columns
- Empty state when no results match filters
- Loading state while fetching

**Reference Implementation:**
- Original: `ui/app/[locale]/(authorized)/kafka/[kafkaId]/nodes/NodesTable.tsx`
- Status labels: `ui/app/[locale]/(authorized)/kafka/[kafkaId]/nodes/NodesLabel.tsx`

**Status Label Requirements:**
Each status needs an icon and popover with description:
- **Broker Statuses:**
  - Running: CheckCircleIcon (success)
  - Starting: InProgressIcon
  - ShuttingDown: PendingIcon
  - PendingControlledShutdown: ExclamationTriangleIcon (warning)
  - Recovery: NewProcessIcon
  - NotRunning: ExclamationCircleIcon (danger)
  - Unknown: ExclamationCircleIcon (danger)

- **Controller Statuses:**
  - QuorumLeader: CheckCircleIcon (success)
  - QuorumFollower: CheckCircleIcon (success)
  - QuorumFollowerLagged: ExclamationTriangleIcon (warning)
  - Unknown: ExclamationCircleIcon (danger)

**Translation Keys Needed:**
Add to `src/i18n/messages/en.json`:
```json
{
  "nodes": {
    "node_roles": {
      "broker": "Broker",
      "controller": "Controller"
    },
    "broker_status": {
      "running": { "label": "Running", "popover_text": "..." },
      "starting": { "label": "Starting", "popover_text": "..." },
      // ... etc
    },
    "controller_status": {
      "quorum_leader": "Quorum Leader",
      "quorum_follower": "Quorum Follower",
      // ... etc
    }
  }
}
```

### Phase 5: Rebalances Table (PENDING)

**Requirements:**
Implement the rebalances table in `NodesRebalancesTab.tsx`.

**Columns:**
1. **Name** - Rebalance name with "CR" badge and link to detail page
2. **Status** - Status with icon and tooltip
3. **Last Updated** - DateTime of last status change

**Expandable Row Content:**
- Auto-approval enabled (true/false)
- Mode (Full/Add brokers/Remove brokers) with popover explaining each mode
- Broker list (if mode is add-brokers or remove-brokers)

**Actions Menu:**
- Approve (enabled only if status is ProposalReady)
- Refresh (enabled based on allowedActions)
- Stop (enabled based on allowedActions)

**Filters:**
1. **Name** (search filter) - Text search with validation
2. **Status** (checkbox filter) - All rebalance statuses with counts
3. **Mode** (checkbox filter) - full, add-brokers, remove-brokers

**Features:**
- Pagination
- Sorting by name, status, lastUpdated
- Empty state when no rebalances exist
- Empty state when no results match filters

**Reference Implementation:**
- Original: `ui/app/[locale]/(authorized)/kafka/[kafkaId]/nodes/rebalances/RebalanceTable.tsx`
- Empty state: `ui/app/[locale]/(authorized)/kafka/[kafkaId]/nodes/rebalances/EmptyStateNoKafkaRebalance.tsx`

**Status Icons:**
- New: ExclamationCircleIcon
- PendingProposal: PendingIcon
- ProposalReady: CheckIcon
- Stopped: Stop icon (custom SVG)
- Rebalancing: PendingIcon
- NotReady: OutlinedClockIcon
- Ready: CheckIcon
- ReconciliationPaused: PauseCircleIcon

## File Structure

```
ui/poc-react/
├── src/
│   ├── api/
│   │   ├── hooks/
│   │   │   ├── useNodes.ts ✅
│   │   │   └── useRebalances.ts ✅
│   │   └── types.ts ✅ (updated)
│   ├── pages/
│   │   ├── NodesPage.tsx ✅
│   │   ├── NodesOverviewTab.tsx ✅
│   │   └── NodesRebalancesTab.tsx ⏳ (placeholder)
│   ├── routes/
│   │   └── index.tsx ✅ (updated)
│   └── i18n/
│       └── messages/
│           └── en.json ✅ (updated)
└── NODES_PAGE_CONTEXT.md ✅ (this file)
```

## Testing the Implementation

1. **Start the API:**
   ```bash
   cd api
   mvn quarkus:dev
   ```

2. **Start the POC:**
   ```bash
   cd ui/poc-react
   npm run dev
   ```

3. **Navigate to:**
   - http://localhost:5173/kafka/{kafkaId}/nodes
   - Should see Overview tab with distribution chart
   - Rebalances tab shows placeholder

## Next Steps for Phases 4 & 5

1. **Phase 4 - Nodes Table:**
   - Create status label components (similar to NodesLabel.tsx)
   - Implement table with all columns
   - Add expandable rows with disk usage chart
   - Implement all three filter types
   - Add pagination and sorting

2. **Phase 5 - Rebalances Table:**
   - Implement table with columns
   - Add expandable rows with mode details
   - Implement actions menu with mutations
   - Add all filters
   - Handle empty states

## Important Notes

- **PatternFly Charts:** Import from `@patternfly/react-charts/victory`
- **Dark Mode:** Use CSS variables like `var(--pf-t--global--text--color--regular)`
- **Pagination:** Use cursor-based pagination (after:/before: prefixes)
- **Sorting:** API expects sort param with optional `-` prefix for desc
- **Filters:** API uses `in,value1,value2` format for array filters

## Reference Links

- Original Next.js implementation: `ui/app/[locale]/(authorized)/kafka/[kafkaId]/nodes/`
- API schema: `ui/api/nodes/schema.ts` and `ui/api/rebalance/schema.ts`
- API actions: `ui/api/nodes/actions.ts` and `ui/api/rebalance/actions.ts`