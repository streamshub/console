import type { Meta, StoryObj } from "@storybook/nextjs";
import { KafkaUserDetails } from "./KafkaUserDetails";
import { KafkaUser } from "@/api/kafkaUsers/schema";

const mockUser: KafkaUser = {
  id: "user1",
  type: "kafkaUsers",
  meta: {
    privileges: ["GET", "UPDATE"],
  },
  attributes: {
    name: "alice",
    username: "alice",
    namespace: "kafka-namespace",
    authenticationType: "scram-sha-512",
    creationTimestamp: new Date().toISOString(),
    authorization: {
      accessControls: [
        {
          type: "topic",
          patternType: "literal",
          resourceName: "payments",
          operations: ["Read", "Write"],
          permissionType: "allow",
        },
      ],
    },
  },
  relationships: {},
};

export default {
  component: KafkaUserDetails,
  args: {
    kafkaUser: mockUser,
  },
} satisfies Meta<typeof KafkaUserDetails>;

type Story = StoryObj<typeof KafkaUserDetails>;

export const Default: Story = {};
