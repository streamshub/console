import type { Meta, StoryObj } from "@storybook/nextjs";
import { expect, fn, userEvent, within } from "storybook/test";
import { ColumnsModal } from "./ColumnsModal";

const meta: Meta<typeof ColumnsModal> = {
  component: ColumnsModal,
  args: {
    chosenColumns: ["timestampUTC", "size", "value"], // Example initial selected columns
    onCancel: fn(),
    onConfirm: fn(),
  },
  argTypes: {
    onConfirm: { action: "confirmed" },
    onCancel: { action: "cancelled" },
  },
};

export default meta;
type Story = StoryObj<typeof ColumnsModal>;

export const Default: Story = {
  play: async ({ canvasElement, args }) => {
    const canvas = within(canvasElement.parentElement);
    const key = canvas.getByText("Key");

    await userEvent.click(key);
    await userEvent.click(canvas.getByRole("button", { name: "Save" }));

    await expect(args.onConfirm).toHaveBeenCalledWith([
      "timestampUTC",
      "size",
      "value",
      "key",
    ]);
  },
};

// export const DragAndDropColumn: Story = {
//   play: async ({ canvasElement, args }) => {
//     const canvas = within(canvasElement.parentElement);
//     const dragButtons = canvas.getAllByRole("button", { name: "Drag button" });
//
//     const startButton = dragButtons[0]; // The button we'll start the drag from
//     const endButton = dragButtons[2]; // The button where we'll end the drag
//
//     // Start drag by simulating mouse down on the first drag button
//     await userEvent.pointer([
//       { target: startButton, keys: "[MouseLeft>]" },
//       {
//         coords: endButton.getBoundingClientRect(),
//       },
//       { keys: "[/MouseLeft]" },
//     ]);
//
//     await userEvent.click(canvas.getByRole("button", { name: "Save" }));
//
//     await expect(args.onConfirm).toHaveBeenCalledWith([
//       "size",
//       "timestampUTC",
//       "value",
//     ]);
//   },
// };
