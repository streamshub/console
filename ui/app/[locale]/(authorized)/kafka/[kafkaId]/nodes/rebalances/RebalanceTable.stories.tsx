import type { Meta, StoryObj } from "@storybook/react";
import { RebalanceTable } from "./RebalanceTable";

const meta: Meta<typeof RebalanceTable> = {
  component: RebalanceTable,
};

export default meta;
type Story = StoryObj<typeof RebalanceTable>;

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
          creationTimestamp: Date.now(),
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
          creationTimestamp: Date.now(),
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
          creationTimestamp: Date.now(),
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
          creationTimestamp: Date.now(),
        },
      },
      {
        id: "werstys",
        attributes: {
          name: "kafak-rebalance-test5",
          status: "ProposalReady",
          creationTimestamp: Date.now(),
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
          creationTimestamp: Date.now(),
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
          creationTimestamp: Date.now(),
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
          creationTimestamp: Date.now(),
        },
      },
    ],
  },
};
