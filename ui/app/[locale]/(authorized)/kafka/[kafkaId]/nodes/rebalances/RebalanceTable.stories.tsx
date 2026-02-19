import type { Meta, StoryObj } from "@storybook/nextjs";
import { RebalanceTable, RebalanceTableColumns } from "./RebalanceTable";
import { fn } from "storybook/test";

const meta: Meta<typeof RebalanceTable> = {
  component: RebalanceTable,
};

export default meta;
type Story = StoryObj<typeof RebalanceTable>;

const fixedDate = new Date(
  Date.UTC(2024, 11, 31, 23, 59, 59, 999),
).toISOString();

export const RebalanceList: Story = {
  args: {
    rebalanceList: [
      {
        id: "geffs",
        meta: {
          allowedActions: ["approve", "refresh"],
        },
        attributes: {
          name: "kafak-rebalance-test1",
          status: "Ready",
          creationTimestamp: fixedDate,
          mode: "add-bokers",
          brokers: [1, 2, 3],
        },
      },
      {
        id: "zwrfts",
        meta: {
          allowedActions: ["refresh"],
        },
        attributes: {
          name: "kafak-rebalance-test2",
          status: "Stopped",
          creationTimestamp: fixedDate,
          brokers: [0, 1],
        },
      },
      {
        id: "zwerts",
        meta: {
          allowedActions: ["approve"],
        },
        attributes: {
          name: "kafak-rebalance-test3",
          status: "New",
          creationTimestamp: fixedDate,
          brokers: [],
        },
      },
      {
        id: "zw6gscd6w",
        meta: {
          allowedActions: ["approve", "refresh"],
        },
        attributes: {
          name: "kafak-rebalance-test4",
          status: "PendingProposal",
          creationTimestamp: fixedDate,
        },
      },
      {
        id: "werstys",
        attributes: {
          name: "kafak-rebalance-test5",
          status: "ProposalReady",
          creationTimestamp: fixedDate,
        },
      },
      {
        id: "zserts",
        meta: {
          allowedActions: [],
        },
        attributes: {
          name: "kafak-rebalance-test6",
          status: "ReconciliationPaused",
          creationTimestamp: fixedDate,
        },
      },
      {
        id: "asqwests",
        meta: {
          allowedActions: ["approve", "refresh"],
        },
        attributes: {
          name: "kafak-rebalance-test7",
          status: "NotReady",
          creationTimestamp: fixedDate,
        },
      },
      {
        id: "asqwests",
        meta: {
          allowedActions: [],
        },
        attributes: {
          name: "kafak-rebalance-test7",
          status: "Rebalancing",
          creationTimestamp: fixedDate,
        },
      },
    ],
    sortProvider: (column) => {
      const index = RebalanceTableColumns.indexOf(column);

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
};
