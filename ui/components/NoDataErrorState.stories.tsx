import type { Meta, StoryObj } from "@storybook/nextjs";
import { NoDataErrorState } from "./NoDataErrorState";

const meta: Meta<typeof NoDataErrorState> = {
  component: NoDataErrorState,
};

export default meta;
type Story = StoryObj<typeof NoDataErrorState>;

export const Default: Story = {
  args: {
    errors: [
      {
        id: "1",
        status: "500",
        title: "Something went wrong",
        detail: "Unable to fetch topics",
        code: "TOPIC_FETCH_FAILED",
      },
    ],
  },
};
