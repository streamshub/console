import type { Meta, StoryObj } from "@storybook/react";
import { expect, fn, userEvent, within } from "@storybook/test";
import { MessagesTableToolbar as Comp } from "./MessagesTableToolbar";

const meta: Meta<typeof Comp> = {
  component: Comp,
  args: {
    onColumnManagement: fn(),
    messages: [],
    topicName: "Example",
  },
};

export default meta;
type Story = StoryObj<typeof Comp>;

export const MessagesTableToolbar: Story = {
  play: async ({ args, canvasElement }) => {
    const canvas = within(canvasElement);
    // Assuming the button to manage columns can be identified by its aria-label or text
    const columnsButton = canvas.getByRole("button", {
      name: "Columns management",
    });
    await userEvent.click(columnsButton);
    // This checks if the onColumnManagement prop function is called upon clicking the button
    await expect(args.onColumnManagement).toHaveBeenCalled();
  },
};
