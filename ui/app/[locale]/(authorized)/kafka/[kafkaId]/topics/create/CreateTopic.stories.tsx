import { Meta, StoryObj } from "@storybook/nextjs";
import { CreateTopic } from "./CreateTopic";;

const meta: Meta<typeof CreateTopic> = {
  component: CreateTopic,
};

export default meta;
type Story = StoryObj<typeof CreateTopic>;


export const Default: Story = {
  args: {
    kafkaId: 'kafka1',
    maxReplicas: 3,
    initialOptions: {},
    onSave: async (name, partitions, replicas, options, validateOnly) => {
      return new Promise((resolve) => {
        setTimeout(() => {
          if (validateOnly) {
            resolve({ data: { id: 'topic1' } });
          } else {
            resolve({ data: { id: 'topic1' } });
          }
        }, 1000);
      });
    },

  }
}

export const InvalidState: Story = {
  args: {
    kafkaId: 'example-kafka-id',
    maxReplicas: 3,
    initialOptions: {},
    onSave: async (name, partitions, replicas, options, validateOnly) => {
      return new Promise((resolve, reject) => {
        setTimeout(() => {
          resolve({
            errors: [
              {
                field: "name",
                error: "Invalid topic name",
              },
              {
                field: "numPartitions",
                error: "Invalid number of partitions",
              },
              {
                field: "replicationFactor",
                error: "Invalid replication factor",
              },
            ],
          });
        }, 1000);
      });
    },
  }
}

