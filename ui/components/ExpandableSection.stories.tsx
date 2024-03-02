import type { Meta, StoryObj } from "@storybook/react";
import { expect, userEvent, waitFor, within } from "@storybook/test";
import { ExpandableSection } from "./ExpandableSection";

const meta: Meta<typeof ExpandableSection> = {
  component: ExpandableSection,
  args: {
    toggleContent: "Expandable section title",
    children: "This is the content of the expandable section.",
  },
  argTypes: {
    initialExpanded: {
      control: { type: "boolean" },
      description: "Controls the initial expanded state of the section",
    },
  },
};

export default meta;
type Story = StoryObj<typeof ExpandableSection>;

export const Default: Story = {
  args: {
    initialExpanded: false,
  },
  play: async ({ args, canvasElement }) => {
    const canvas = within(canvasElement);
    // Initially, the section should not be expanded based on the `initialExpanded` prop
    await waitFor(() =>
      expect(canvas.queryByText(args.children)).not.toBeVisible(),
    );

    // Simulate clicking the toggle button to expand the section
    await userEvent.click(canvas.getByRole("button"));

    // Verify the section is now expanded and the content is visible
    await waitFor(() => expect(canvas.getByText(args.children)).toBeVisible());

    // Restore the previous state
    await userEvent.click(canvas.getByRole("button"));
  },
};

export const InitiallyExpanded: Story = {
  args: {
    initialExpanded: true,
  },
  play: async ({ args, canvasElement }) => {
    const canvas = within(canvasElement);
    // Initially, the section should be expanded
    await waitFor(() => expect(canvas.getByText(args.children)).toBeVisible());

    // Simulate clicking the toggle button to collapse the section
    await userEvent.click(canvas.getByRole("button"));

    // Verify the section is now collapsed and the content is not visible
    await waitFor(() =>
      expect(canvas.queryByText(args.children)).not.toBeVisible(),
    );

    // Restore the previous state
    await userEvent.click(canvas.getByRole("button"));
  },
};
