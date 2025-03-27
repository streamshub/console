import type { Meta, StoryObj } from "@storybook/react";

import { ConsumerGroupsTable as Comp } from "./ConsumerGroupsTable";

export default {
  component: Comp,
  args: {
    page: 1,
    perPage: 20,
    consumerGroups: [],
  },
} as Meta<typeof Comp>;
type Story = StoryObj<typeof Comp>;

const generateConsumerGroup = (
  id: string,
  state: string,
  lag1: number,
  lag2: number,
  lag3?: number,
) => ({
  id,
  attributes: {
    state,
    offsets:
      lag3 !== undefined
        ? [{ lag: lag1 }, { lag: lag2 }, { lag: lag3 }]
        : [{ lag: lag1 }, { lag: lag2 }],
    members: [
      {
        host: "localhost",
        memberId: "member-1",
        clientId: "client-1",
        groupInstanceId: "instance-1",
        assignments: [
          {
            topicName: `console_datagen_${id.split("-").pop()}-a`,
            topicId: "1",
          },
          {
            topicName: `console_datagen_${id.split("-").pop()}-b`,
            topicId: "2",
          },
        ],
      },
    ],
  },
});

export const ConsumerGroups: Story = {
  args: {
    perPage: 20,
    consumerGroups: (() => {
      const groups = Array.from({ length: 30 }, (_, i) =>
        generateConsumerGroup(
          `console-datagen-group-${i}`,
          ["STABLE", "EMPTY"][i % 2],
          Math.floor(Math.random() * 1000),
          Math.floor(Math.random() * 1000),
          i % 3 === 0 ? Math.floor(Math.random() * 1000) : undefined, // Add a third lag sometimes
        ),
      );
      return groups;
    })(),
    total: (() => {
      const groups = Array.from({ length: 30 }, (_, i) =>
        generateConsumerGroup(
          `console-datagen-group-${i}`,
          ["STABLE", "EMPTY"][i % 2],
          Math.floor(Math.random() * 1000),
          Math.floor(Math.random() * 1000),
          i % 3 === 0 ? Math.floor(Math.random() * 1000) : undefined, // Add a third lag sometimes
        ),
      );
      return groups.length;
    })(),
  },
};
