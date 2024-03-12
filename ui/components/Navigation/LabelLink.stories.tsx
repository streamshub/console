import type { Meta, StoryObj } from "@storybook/react";
import { userEvent, within } from "@storybook/test";
import { LabelLink } from "./LabelLink";

const meta: Meta<typeof LabelLink> = {
  component: LabelLink,
  args: {
    children: "Clickable Label",
    href: "#",
  },
  argTypes: {
    color: {
      control: "select",
      options: [
        "blue",
        "cyan",
        "green",
        "orange",
        "purple",
        "red",
        "grey",
        "gold",
      ],
    },
    href: {
      control: "text",
    },
  },
};

export default meta;
type Story = StoryObj<typeof LabelLink>;

export const Default: Story = {};

export const Blue: Story = {
  args: {
    color: "blue",
  },
};

export const Cyan: Story = {
  args: {
    color: "cyan",
  },
};

export const Green: Story = {
  args: {
    color: "green",
  },
};

export const Orange: Story = {
  args: {
    color: "orange",
  },
};

export const Purple: Story = {
  args: {
    color: "purple",
  },
};

export const Red: Story = {
  args: {
    color: "red",
  },
};

export const Grey: Story = {
  args: {
    color: "grey",
  },
};

export const Gold: Story = {
  args: {
    color: "gold",
  },
};

// Example of an interaction test using play function for the Default story
Default.play = async ({ canvasElement, args }) => {
  const canvas = within(canvasElement);
  await userEvent.click(canvas.getByText(args.children));
};
