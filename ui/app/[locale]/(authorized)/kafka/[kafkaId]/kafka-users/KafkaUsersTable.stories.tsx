import type { Meta, StoryObj } from "@storybook/nextjs";
import { KafkaUsersTable } from "./KafkaUsersTable";

const fixedDate = new Date(Date.UTC(2025, 11, 11, 0, 0, 0, 0)).toISOString();

const mockUsers = [
  {
    id: "user1",
    type: "kafkaUsers",
    meta: {},
    attributes: {
      name: "alice",
      username: "alice",
      namespace: "kafka-namespace",
      authenticationType: "scram-sha-512",
      creationTimestamp: fixedDate,
    },
  },
  {
    id: "user2",
    type: "kafkaUsers",
    meta: {},
    attributes: {
      name: "bob",
      username: "bob",
      namespace: null,
      authenticationType: "tls",
      creationTimestamp: fixedDate,
    },
  },
];

export default {
  component: KafkaUsersTable,
  args: {
    kafkaUsers: mockUsers,
    kafkaUserCount: mockUsers.length,
    page: 1,
    perPage: 20,
    filterUsername: "",
  },
} satisfies Meta<typeof KafkaUsersTable>;

type Story = StoryObj<typeof KafkaUsersTable>;

export const Default: Story = {};
