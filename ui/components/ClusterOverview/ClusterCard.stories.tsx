import type { Meta, StoryObj } from "@storybook/nextjs";

import { ClusterCard } from "./ClusterCard";
import { ReconciliationContext } from "../ReconciliationContext";

const meta: Meta<typeof ClusterCard> = {
  component: ClusterCard,
  decorators: [
    (Story) => (
      <ReconciliationContext.Provider
        value={{
          isReconciliationPaused: false,
          setReconciliationPaused: () => {},
        }}
      >
        <Story />
      </ReconciliationContext.Provider>
    ),
  ],
};

export default meta;
type Story = StoryObj<typeof ClusterCard>;

export const WithData: Story = {
  args: {
    isLoading: false,
    kafkaDetail: {
      attributes: {
        name: "my-kafka-cluster",
        status: "ready",
        kafkaVersion: "3.5.6",
      }
    },
    brokersTotal: 9999,
    brokersOnline: 9999,
    consumerGroups: 9999,
    messages: [
      {
        variant: "danger",
        subject: { type: "broker", name: "Broker 1", id: "1" },
        message:
          "Lorem ipsum dolor sit amet, consectetur adipisicing elit. Ab, accusantium amet consequuntur delectus dolor excepturi hic illum iure labore magnam nemo nobis non obcaecati odit officia qui quo saepe sapiente",
        date: "2023-12-09T13:54:17.687Z",
      },
      {
        variant: "warning",
        subject: { type: "topic", name: "Pet-sales", id: "1" },
        message:
          "Lorem ipsum dolor sit amet, consectetur adipisicing elit. Ab, accusantium amet consequuntur delectus dolor excepturi hic illum iure labore magnam nemo nobis non obcaecati odit officia qui quo saepe sapiente",
        date: "2023-12-10T13:54:17.687Z",
      },
      {
        variant: "warning",
        subject: { type: "topic", name: "night-orders", id: "1" },
        message:
          "Lorem ipsum dolor sit amet, consectetur adipisicing elit. Ab, accusantium amet consequuntur delectus dolor excepturi hic illum iure labore magnam nemo nobis non obcaecati odit officia qui quo saepe sapiente",
        date: "2023-12-11T13:54:17.687Z",
      },
      {
        variant: "danger",
        subject: { type: "topic", name: "night-orders", id: "1" },
        message:
          "Lorem ipsum dolor sit amet, consectetur adipisicing elit. Ab, accusantium amet consequuntur delectus dolor excepturi hic illum iure labore magnam nemo nobis non obcaecati odit officia qui quo saepe sapiente",
        date: "2023-12-12T13:54:17.687Z",
      },
      {
        variant: "danger",
        subject: {
          type: "topic",
          name: "very-very-very-long-name-that-will-cause-problems",
          id: "1",
        },
        message:
          "Lorem ipsum dolor sit amet, consectetur adipisicing elit. Ab, accusantium amet consequuntur delectus dolor excepturi hic illum iure labore magnam nemo nobis non obcaecati odit officia qui quo saepe sapiente",
        date: "2023-12-13T13:54:17.687Z",
      },
    ],
  },
};

export const NoMessages: Story = {
  args: {
    isLoading: false,
    kafkaDetail: {
      attributes: {
        name: "my-kafka-cluster",
        status: "ready",
        kafkaVersion: "3.5.6",
      }
    },
    brokersTotal: 9999,
    brokersOnline: 9999,
    consumerGroups: 9999,
    messages: [],
  },
};
export const Loading: Story = {
  args: {
    isLoading: true,
  },
};
