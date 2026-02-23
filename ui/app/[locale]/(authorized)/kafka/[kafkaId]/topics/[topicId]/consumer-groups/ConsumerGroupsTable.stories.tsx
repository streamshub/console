import type { Meta, StoryObj } from "@storybook/nextjs";
import { ConsumerGroupsTable } from "./ConsumerGroupsTable";

const allStates = [
  "STABLE",
  "EMPTY",
  "UNKNOWN",
  "PREPARING_REBALANCE",
  "ASSIGNING",
  "COMPLETING_REBALANCE",
  "RECONCILING",
  "DEAD",
] as const;

type ConsumerGroupMock = {
  id: string;
  type: "group";
  attributes: {
    groupId: string;
    state: (typeof allStates)[number];
    members: Array<{
      clientId: string;
      memberId: string;
      assignments?: Array<{
        topicName: string;
        topicId: string;
        partition: number;
      }>;
    }>;
    offsets?: Array<{
      topic: string;
      partition: number;
      lag: number;
    }>;
  };
};

const createMockConsumerGroup = (
  id: string,
  state: ConsumerGroupMock["attributes"]["state"],
): ConsumerGroupMock => ({
  id,
  type: "group",
  attributes: {
    groupId: `group-${id}`,
    state,
    members: [
      {
        clientId: `client-${id}`,
        memberId: `member-${id}`,
        assignments: [
          {
            topicName: "topic-A",
            topicId: "topic-1",
            partition: 0,
          },
        ],
      },
    ],
    offsets: [
      {
        topic: "topic-A",
        partition: 0,
        lag: 42,
      },
    ],
  },
});

const mockConsumerGroups = allStates.map((state, index) =>
  createMockConsumerGroup(`${index}`, state),
);

const meta: Meta<typeof ConsumerGroupsTable> = {
  component: ConsumerGroupsTable,
  args: {
    kafkaId: "mock-kafka-id",
    page: 1,
    total: mockConsumerGroups.length,
    consumerGroups: mockConsumerGroups,
  },
};

export default meta;
type Story = StoryObj<typeof ConsumerGroupsTable>;

export const AllStates: Story = {};
