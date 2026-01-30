import type { Meta, StoryObj } from "@storybook/nextjs";
import { userEvent, within } from "storybook/test";
import { ButtonLink } from "./ButtonLink";

const handleInteraction = async ({ canvasElement }: { canvasElement: HTMLElement }) => {
  const canvas = within(canvasElement);
  const button = canvas.getByRole('link', { name: /click me/i });

  // Prevent navigation for all stories using this function
  button.addEventListener("click", (e) => e.preventDefault());
  
  await userEvent.click(button);
};

const meta: Meta<typeof ButtonLink> = {
  component: ButtonLink,
  args: {
    children: "Click Me",
    href: "/",
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
  play: handleInteraction
};

export const Secondary: Story = {
  args: {
    variant: "secondary",
  },
  play: handleInteraction
};

export const Danger: Story = {
  args: {
    variant: "danger",
  },
  play: handleInteraction
};

export const LinkVariant: Story = {
  args: {
    variant: "link",
  },
  play: handleInteraction
};
