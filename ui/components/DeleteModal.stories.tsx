import { expect } from "@storybook/jest";
import type { Meta, StoryObj } from "@storybook/react";
import { userEvent, waitFor, within } from "@storybook/testing-library";
import { DeleteModal } from "./DeleteModal";

const meta: Meta<typeof DeleteModal> = {
  component: DeleteModal,
  args: {
    isModalOpen: true,
    title: "Confirm Delete",
    isDeleting: false,
    disableFocusTrap: true,
    onDelete: () => {},
  },
  argTypes: {
    onDelete: { action: "deleted" },
    ouiaId: {
      control: "text",
      description: "OUIA component id",
    },
    confirmationValue: {
      control: "text",
      description: "Value to enable the delete button",
    },
    disableFocusTrap: {
      control: "boolean",
      description: "Set true to disable focus trap",
    },
    appendTo: {
      table: {
        disable: true,
      },
    },
  },
  decorators: (Story) => <div id={"modal"}>{Story()}</div>,
};

export default meta;
type Story = StoryObj<typeof DeleteModal>;

export const Default: Story = {};

export const WithConfirmation: Story = {
  args: {
    confirmationValue: "CONFIRM",
  },
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement.parentElement);
    await userEvent.type(
      canvas.getByLabelText("Type CONFIRM to confirm"),
      "CONFIRM",
    );
    await waitFor(() =>
      expect(canvas.getByRole("button", { name: /delete/i })).toBeEnabled(),
    );
  },
};

export const DeletingState: Story = {
  args: {
    isDeleting: true,
  },
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement.parentElement);
    await waitFor(() =>
      expect(canvas.getByRole("button", { name: /delete/i })).toBeDisabled(),
    );
  },
};
