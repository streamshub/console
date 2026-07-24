/**
 * React Router configuration
 */

import { createBrowserRouter, Navigate } from 'react-router-dom';
import type { TFunction } from 'i18next';
import App from '../App';

// Root pages
import { HomePage } from '@/pages/HomePage';
import { ErrorPage } from '@/pages/ErrorPage';

// Kafka layout and overview
import { KafkaLayout } from '@/pages/kafka/KafkaLayout';
import { KafkaOverview } from '@/pages/kafka/overview/KafkaOverview';

// Topics pages
import { TopicsPage } from '@/pages/kafka/topics/TopicsPage';
import { TopicDetailPage } from '@/pages/kafka/topics/detail/TopicDetailPage';
import { TopicMessagesTab } from '@/pages/kafka/topics/detail/TopicMessagesTab';
import { TopicPartitionsTab } from '@/pages/kafka/topics/detail/TopicPartitionsTab';
import { TopicGroupsTab } from '@/pages/kafka/topics/detail/TopicGroupsTab';
import { TopicConfigurationTab } from '@/pages/kafka/topics/detail/TopicConfigurationTab';

// Nodes pages
import { NodesPage } from '@/pages/kafka/nodes/NodesPage';
import { NodesOverviewTab } from '@/pages/kafka/nodes/NodesOverviewTab';
import { NodesRebalancesTab } from '@/pages/kafka/nodes/NodesRebalancesTab';
import { NodeDetailPage } from '@/pages/kafka/nodes/detail/NodeDetailPage';
import { NodeConfigurationTab } from '@/pages/kafka/nodes/detail/NodeConfigurationTab';

// Groups pages
import { GroupsPage } from '@/pages/kafka/groups/GroupsPage';
import { GroupDetailPage } from '@/pages/kafka/groups/detail/GroupDetailPage';
import { GroupMembersTab } from '@/pages/kafka/groups/detail/GroupMembersTab';
import { GroupConfigurationTab } from '@/pages/kafka/groups/detail/GroupConfigurationTab';

// Connect pages
import { ConnectPage } from '@/pages/kafka/connect/ConnectPage';
import { ConnectConnectorsTab } from '@/pages/kafka/connect/ConnectConnectorsTab';
import { ConnectClustersTab } from '@/pages/kafka/connect/ConnectClustersTab';
import { ConnectorDetailPage } from '@/pages/kafka/connect/detail/ConnectorDetailPage';
import { ConnectClusterDetailPage } from '@/pages/kafka/connect/detail/ConnectClusterDetailPage';

// Users pages
import { UsersPage } from '@/pages/kafka/users/UsersPage';
import { UserDetailPage } from '@/pages/kafka/users/detail/UserDetailPage';

// Route handle type — each route may declare a page title factory.
export interface RouteHandle {
  title?: (t: TFunction) => string;
}

export const router = createBrowserRouter([
  {
    path: '/',
    element: <App />,
    errorElement: <ErrorPage />,
    children: [
      {
        index: true,
        element: <HomePage />,
      },
      {
        path: 'kafka/:kafkaId',
        element: <KafkaLayout />,
        children: [
          {
            index: true,
            element: <Navigate to="overview" replace />,
          },
          {
            path: 'overview',
            handle: { title: (t: TFunction) => t('kafka.overview') } satisfies RouteHandle,
            element: <KafkaOverview />,
          },
          {
            path: 'topics',
            handle: { title: (t: TFunction) => t('kafka.topics') } satisfies RouteHandle,
            element: <TopicsPage />,
          },
          {
            path: 'topics/:topicId',
            // Title is dynamic (fetched topic name) — set by TopicDetailPage via usePageTitle
            element: <TopicDetailPage />,
            children: [
              {
                index: true,
                element: <Navigate to="messages" replace />,
              },
              {
                path: 'messages',
                element: <TopicMessagesTab />,
              },
              {
                path: 'partitions',
                element: <TopicPartitionsTab />,
              },
              {
                path: 'groups',
                element: <TopicGroupsTab />,
              },
              {
                path: 'configuration',
                element: <TopicConfigurationTab />,
              },
            ],
          },
          {
            path: 'nodes',
            handle: { title: (t: TFunction) => t('nodes.title') } satisfies RouteHandle,
            element: <NodesPage />,
            children: [
              {
                index: true,
                element: <Navigate to="overview" replace />,
              },
              {
                path: 'overview',
                handle: { title: (t: TFunction) => t('nodes.title') } satisfies RouteHandle,
                element: <NodesOverviewTab />,
              },
              {
                path: 'rebalances',
                handle: { title: (t: TFunction) => t('nodes.tabs.rebalances') } satisfies RouteHandle,
                element: <NodesRebalancesTab />,
              },
            ],
          },
          {
            path: 'nodes/:nodeId',
            // Title is dynamic (broker ID) — set by NodeDetailPage via usePageTitle
            element: <NodeDetailPage />,
            children: [
              {
                index: true,
                element: <Navigate to="configuration" replace />,
              },
              {
                path: 'configuration',
                element: <NodeConfigurationTab />,
              },
            ],
          },
          {
            path: 'groups',
            handle: { title: (t: TFunction) => t('groups.title') } satisfies RouteHandle,
            element: <GroupsPage />,
          },
          {
            path: 'groups/:groupId',
            // Title is dynamic (fetched group ID) — set by GroupDetailPage via usePageTitle
            element: <GroupDetailPage />,
            children: [
              {
                index: true,
                element: <Navigate to="members" replace />,
              },
              {
                path: 'members',
                element: <GroupMembersTab />,
              },
              {
                path: 'configuration',
                element: <GroupConfigurationTab />,
              },
            ],
          },
          {
            path: 'connect',
            handle: { title: (t: TFunction) => t('kafka.connect.title') } satisfies RouteHandle,
            element: <ConnectPage />,
            children: [
              {
                index: true,
                element: <Navigate to="connectors" replace />,
              },
              {
                path: 'connectors',
                handle: { title: (t: TFunction) => t('kafka.connect.connectors') } satisfies RouteHandle,
                element: <ConnectConnectorsTab />,
              },
              {
                path: 'clusters',
                handle: { title: (t: TFunction) => t('kafka.connect.connectClustersTitle') } satisfies RouteHandle,
                element: <ConnectClustersTab />,
              },
            ],
          },
          {
            path: 'connect/connectors/:connectorId',
            // Title is dynamic (fetched connector name) — set by ConnectorDetailPage via usePageTitle
            element: <ConnectorDetailPage />,
          },
          {
            path: 'connect/clusters/:connectClusterId',
            // Title is dynamic (fetched cluster name) — set by ConnectClusterDetailPage via usePageTitle
            element: <ConnectClusterDetailPage />,
          },
          {
            path: 'users',
            handle: { title: (t: TFunction) => t('kafka.users') } satisfies RouteHandle,
            element: <UsersPage />,
          },
          {
            path: 'users/:userId',
            // Title is dynamic (fetched username) — set by UserDetailPage via usePageTitle
            element: <UserDetailPage />,
          },
          // More routes will be added as pages are migrated
          // ... etc
        ],
      },
    ],
  },
]);