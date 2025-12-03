import type { Meta, StoryObj } from "@storybook/nextjs";
import { KafkaUsersTable } from "./KafkaUsersTable";

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
      creationTimestamp: new Date().toISOString(),
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
      creationTimestamp: new Date().toISOString(),
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
    isColumnSortable: () => undefined,
    onPageChange: () => {},
    onClearAllFilters: () => {},
    onFilterUsernameChange: () => {},
  },
} satisfies Meta<typeof KafkaUsersTable>;

type Story = StoryObj<typeof KafkaUsersTable>;

export const Default: Story = {};
