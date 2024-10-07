import type { Meta, StoryObj } from "@storybook/react";
import { EmptyStateNoKafkaRebalance } from "./EmptyStateNoKafkaRebalance";

const meta: Meta<typeof EmptyStateNoKafkaRebalance> = {
  component: EmptyStateNoKafkaRebalance,
};

export default meta;
type Story = StoryObj<typeof EmptyStateNoKafkaRebalance>;

export const NoKafkaClusterRebalances: Story = {};
