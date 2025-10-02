import type { Meta, StoryObj } from "@storybook/nextjs";

import { TopicsTable as Comp } from "./TopicsTable";

export default {
  component: Comp,
  args: {
    baseurl: "/kafka/abc",
    page: 1,
    perPage: 20,
    topics: {
      data: [],
    },
    includeHidden: false,
    topicsCount: 0,
    showLearningLinks: true,
  },
} as Meta<typeof Comp>;
type Story = StoryObj<typeof Comp>;

export const NoTopics: Story = {};
export const WithTopics: Story = {
  args: {
    topics: {
      data: [
        {
          id: 1,
          attributes: {
            name: "foo",
            numPartitions: 2,
            status: "FullyReplicated",
            totalLeaderLogBytes: 1234,
          },
          relationships: {
            consumerGroups: {
              data: [{}],
            },
          },
        },
        {
          id: 2,
          attributes: {
            name: "bar",
            numPartitions: 5,
            status: "UnderReplicated",
            totalLeaderLogBytes: 4212,
          },
          relationships: {
            consumerGroups: {
              data: [{}, {}],
            },
          },
        },
        {
          id: 3,
          attributes: {
            name: "baz",
            numPartitions: 2,
            status: "PartiallyOffline",
            totalLeaderLogBytes: 355312,
          },
          relationships: {
            consumerGroups: {
              data: [],
            },
          },
        },
        {
          id: 4,
          attributes: {
            name: "zod",
            numPartitions: 1,
            status: "Offline",
            totalLeaderLogBytes: 0,
          },
          relationships: {
            consumerGroups: {
              data: [],
            },
          },
        },
        {
          id: 5,
          attributes: {
            name: "averylongnamethatcancauseproblemsunlessitgetstruncated",
            numPartitions: 9999999,
            status: "FullyReplicated",
            totalLeaderLogBytes: 99999999999999,
          },
          relationships: {
            consumerGroups: {
              data: new Array(99999).fill({}),
            },
          },
        },
      ]
    },
  },
};
