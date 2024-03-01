import { Breadcrumb } from "@/libs/patternfly/react-core";
import { expect } from "@storybook/jest";
import type { Meta, StoryObj } from "@storybook/react";
import { waitFor, within } from "@storybook/testing-library";
import { BreadcrumbLink } from "./BreadcrumbLink";

const meta: Meta<typeof BreadcrumbLink> = {
  component: BreadcrumbLink,
  args: {
    // Default args for all stories
    children: "Breadcrumb Item",
  },
  argTypes: {
    href: { control: "text" },
    isActive: { control: "boolean" },
  },
  decorators: (Story) => <Breadcrumb>{Story()}</Breadcrumb>,
};

export default meta;
type Story = StoryObj<typeof BreadcrumbLink>;

export const Default: Story = {
  args: {
    href: "#",
    isActive: false,
  },
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement);
    await waitFor(() =>
      expect(canvas.getByText("Breadcrumb Item")).toBeInTheDocument(),
    );
  },
};

export const Active: Story = {
  args: {
    ...Default.args,
    isActive: true,
  },
};
