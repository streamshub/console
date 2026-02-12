import type { Meta, StoryObj } from "@storybook/nextjs";
import { userEvent, within } from "storybook/test";
import { ButtonLink } from "./ButtonLink";

const meta: Meta<typeof ButtonLink> = {
  component: ButtonLink,
  args: {
    children: "Click Me",
    href: "#",
  },
  argTypes: {
    variant: {
      control: { type: "select" },
      options: ["primary", "secondary", "danger", "link"],
    },
    href: { control: "text" },
    children: { control: "text" },
  },
};

export default meta;
type Story = StoryObj<typeof ButtonLink>;

export const Primary: Story = {
  args: {
    variant: "primary",
  },
  play: async ({ canvasElement }) => {
    const button = within(canvasElement).getByText("Click Me");
    await userEvent.click(button);
    // Add assertions or additional interactions here if needed
  },
};

export const Secondary: Story = {
  args: {
    variant: "secondary",
  },
  play: async ({ canvasElement }) => {
    const button = within(canvasElement).getByText("Click Me");
    await userEvent.click(button);
  },
};

export const Danger: Story = {
  args: {
    variant: "danger",
  },
  play: async ({ canvasElement }) => {
    const button = within(canvasElement).getByText("Click Me");
    await userEvent.click(button);
  },
};

export const LinkVariant: Story = {
  args: {
    variant: "link",
  },
  play: async ({ canvasElement }) => {
    const button = within(canvasElement).getByText("Click Me");
    await userEvent.click(button);
  },
};
