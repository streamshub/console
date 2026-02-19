import type { Meta, StoryObj } from "@storybook/nextjs";

import {
  ConsumerGroupsTable as Comp,
  ConsumerGroupColumns,
} from "./ConsumerGroupsTable";
import { fn } from "storybook/test";

export default {
  component: Comp,
  args: {
    page: 1,
    perPage: 20,
    consumerGroups: [],
    sortProvider: (column) => {
      const index = ConsumerGroupColumns.indexOf(column);

      return {
        sortBy: {
          index: 0,
          direction: "asc",
        },
        onSort: fn(),
        columnIndex: index,
      };
    },
  },
} as Meta<typeof Comp>;
type Story = StoryObj<typeof Comp>;

const generateConsumerGroup = (
  id: string,
  groupId: string,
  state: string,
  lag1: number,
  lag2: number,
  lag3?: number,
) => ({
  id,
  attributes: {
    groupId,
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
      const groups = Array.from({ length: 21 }, (_, i) =>
        generateConsumerGroup(
          `${i}`,
          `console-datagen-group-${i}`,
          ["STABLE", "EMPTY"][i % 2],
          i * 1000,
          i * 1000,
          i % 3 === 0 ? i * 1000 : undefined,
        ),
      );
      return groups;
    })(),
    total: 21,
  },
};
