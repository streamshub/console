import type { Meta, StoryObj } from "@storybook/nextjs";
import { expect, fn, userEvent, within } from "storybook/test";
import { ApplicationError as Comp } from "./ApplicationError";

const meta: Meta<typeof Comp> = {
  component: Comp,
  args: {
    error: {
      digest: "Some description of the error",
      message: "Some message",
      name: "Some name",
    },
    onReset: fn(),
  },
};

export default meta;
type Story = StoryObj<typeof Comp>;

export const ApplicationError: Story = {
  play: async ({ canvasElement, args }) => {
    const canvas = within(canvasElement);
    await userEvent.click(canvas.getByText("Retry"));
    await expect(args.onReset).toHaveBeenCalled();
  },
};
