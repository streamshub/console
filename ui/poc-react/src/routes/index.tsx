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
            element: <KafkaOverview />,
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
          // More routes will be added as pages are migrated
          // ... etc
        ],
      },
    ],
  },
]);