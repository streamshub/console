import type { Meta, StoryObj } from "@storybook/nextjs";
import { expect, userEvent, waitFor, within } from "storybook/test";
import { ExternalLink } from "./ExternalLink";

const meta: Meta<typeof ExternalLink> = {
  component: ExternalLink,
  args: {
    testId: "external-link",
    href: "https://example.com",
    children: "Example Link",
  },
  argTypes: {
    target: {
      control: "text",
      description: "Where to open the linked document",
      defaultValue: "_blank",
      table: {
        type: { summary: "string" },
        defaultValue: { summary: "_blank" },
      },
    },
    href: {
      control: "text",
      description: "The URL the link points to",
      table: {
        type: { summary: "string" },
      },
    },
    className: {
      control: "text",
      description: "CSS class for the icon",
    },
    ouiaId: {
      control: "text",
      description: "OUIA ID for testing",
    },
  },
};

export default meta;
type Story = StoryObj<typeof ExternalLink>;

export const Default: Story = {};

export const WithCustomTarget: Story = {
  args: {
    target: "_self",
  },
};

export const InteractionTest: Story = {
  args: {
    href: "#example",
    children: "Visit Example",
    target: "_self",
  },
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement);
    await waitFor(() =>
      expect(canvas.getByTestId("external-link")).toBeInTheDocument(),
    );
    await waitFor(() =>
      expect(canvas.getByText("Visit Example")).toBeInTheDocument(),
    );

    await userEvent.click(canvas.getByTestId("external-link"));
  },
};
