/**
 * React Router configuration
 */

import { createBrowserRouter, Navigate } from 'react-router-dom';
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
            element: <KafkaOverview />,
          },
          {
            path: 'topics',
            element: <TopicsPage />,
          },
          {
            path: 'topics/:topicId',
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
            element: <NodesPage />,
            children: [
              {
                index: true,
                element: <Navigate to="overview" replace />,
              },
              {
                path: 'overview',
                element: <NodesOverviewTab />,
              },
              {
                path: 'rebalances',
                element: <NodesRebalancesTab />,
              },
            ],
          },
          {
            path: 'nodes/:nodeId',
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
            element: <GroupsPage />,
          },
          {
            path: 'groups/:groupId',
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
            element: <ConnectPage />,
            children: [
              {
                index: true,
                element: <Navigate to="connectors" replace />,
              },
              {
                path: 'connectors',
                element: <ConnectConnectorsTab />,
              },
              {
                path: 'clusters',
                element: <ConnectClustersTab />,
              },
            ],
          },
          {
            path: 'connect/connectors/:connectorId',
            element: <ConnectorDetailPage />,
          },
          {
            path: 'connect/clusters/:connectClusterId',
            element: <ConnectClusterDetailPage />,
          },
          {
            path: 'users',
            element: <UsersPage />,
          },
          {
            path: 'users/:userId',
            element: <UserDetailPage />,
          },
          // More routes will be added as pages are migrated
          // ... etc
        ],
      },
    ],
  },
]);