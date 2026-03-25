import { Route } from '@vaadin/router';

export const routes: Route[] = [
  {
    path: '/',
    component: 'home-page',
    action: async () => {
      await import('../pages/home-page');
    },
  },
  {
    path: '/kafka/:kafkaId',
    component: 'kafka-layout',
    action: async () => {
      await import('../layouts/kafka-layout');
    },
    children: [
      {
        path: '',
        redirect: '/kafka/:kafkaId/overview',
      },
      {
        path: 'overview',
        component: 'overview-page',
        action: async () => {
          await import('../pages/overview-page');
        },
      },
      {
        path: 'topics',
        component: 'topics-page',
        action: async () => {
          await import('../pages/topics-page');
        },
      },
      {
        path: 'topics/:topicId',
        component: 'topic-layout',
        action: async () => {
          await import('../layouts/topic-layout');
        },
        children: [
          {
            path: '',
            redirect: '/kafka/:kafkaId/topics/:topicId/messages',
          },
          {
            path: 'messages',
            component: 'topic-messages-page',
            action: async () => {
              await import('../pages/topic-messages-page');
            },
          },
          {
            path: 'partitions',
            component: 'topic-partitions-page',
            action: async () => {
              await import('../pages/topic-partitions-page');
            },
          },
          {
            path: 'groups',
            component: 'topic-groups-page',
            action: async () => {
              await import('../pages/topic-groups-page');
            },
          },
          {
            path: 'configuration',
            component: 'topic-configuration-page',
            action: async () => {
              await import('../pages/topic-configuration-page');
            },
          },
        ],
      },
      {
        path: 'nodes',
        component: 'nodes-layout',
        action: async () => {
          await import('../layouts/nodes-layout');
        },
        children: [
          {
            path: '',
            component: 'nodes-page',
            action: async () => {
              await import('../pages/nodes-page');
            },
          },
          {
            path: 'rebalances',
            component: 'rebalances-page',
            action: async () => {
              await import('../pages/rebalances-page');
            },
          },
        ],
      },
      {
        path: 'kafka-connect',
        component: 'kafka-connect-page',
        action: async () => {
          await import('../pages/kafka-connect-page');
        },
      },
      {
        path: 'kafka-users',
        component: 'kafka-users-page',
        action: async () => {
          await import('../pages/kafka-users-page');
        },
      },
      {
        path: 'groups',
        component: 'groups-page',
        action: async () => {
          await import('../pages/groups-page');
        },
      },
      {
        path: 'groups/:groupId',
        component: 'group-layout',
        action: async () => {
          await import('../layouts/group-layout');
        },
        children: [
          {
            path: '',
            redirect: '/kafka/:kafkaId/groups/:groupId/members',
          },
          {
            path: 'members',
            component: 'group-members-page',
            action: async () => {
              await import('../pages/group-members-page');
            },
          },
          {
            path: 'configuration',
            component: 'group-configuration-page',
            action: async () => {
              await import('../pages/group-configuration-page');
            },
          },
          {
            path: 'reset-offset',
            component: 'group-reset-offset-page',
            action: async () => {
              await import('../pages/group-reset-offset-page');
            },
          },
        ],
      },
    ],
  },
  {
    path: '(.*)',
    component: 'not-found-page',
    action: async () => {
      await import('../pages/not-found-page');
    },
  },
];

// Made with Bob
