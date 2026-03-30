/**
 * React Router configuration
 */

import { createBrowserRouter, Navigate } from 'react-router-dom';
import App from '../App';
import { HomePage } from '../pages/HomePage';
import { KafkaLayout } from '../pages/KafkaLayout';
import { KafkaOverview } from '../pages/KafkaOverview';
import { TopicsPage } from '../pages/TopicsPage';
import { TopicDetailPage } from '../pages/TopicDetailPage';
import { TopicMessagesTab } from '../pages/TopicMessagesTab';
import { TopicPartitionsTab } from '../pages/TopicPartitionsTab';
import { TopicGroupsTab } from '../pages/TopicGroupsTab';
import { TopicConfigurationTab } from '../pages/TopicConfigurationTab';
import { NodesPage } from '../pages/NodesPage';
import { NodesOverviewTab } from '../pages/NodesOverviewTab';
import { NodesRebalancesTab } from '../pages/NodesRebalancesTab';
import { GroupsPage } from '../pages/GroupsPage';
import { GroupDetailPage } from '../pages/GroupDetailPage';
import { GroupMembersTab } from '../pages/GroupMembersTab';
import { GroupConfigurationTab } from '../pages/GroupConfigurationTab';
import { ConnectPage } from '../pages/ConnectPage';
import { ConnectConnectorsTab } from '../pages/ConnectConnectorsTab';
import { ConnectClustersTab } from '../pages/ConnectClustersTab';
import { ConnectorDetailPage } from '../pages/ConnectorDetailPage';
import { ConnectClusterDetailPage } from '../pages/ConnectClusterDetailPage';
import { ErrorPage } from '../pages/ErrorPage';

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
            path: 'connect/clusters/:clusterId',
            element: <ConnectClusterDetailPage />,
          },
          // More routes will be added as pages are migrated
          // ... etc
        ],
      },
    ],
  },
]);