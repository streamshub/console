import type { Meta, StoryObj } from "@storybook/react";
import { expect, fn, within } from "@storybook/test";
import { UnknownValuePreview as Comp } from "./UnknownValuePreview";

const meta: Meta<typeof Comp> = {
  component: Comp,
  args: {
    onClick: fn(),
  },
};

export default meta;
type Story = StoryObj<typeof Comp>;

export const JSONValue: Story = {
  args: {
    value:
      '{"timestamp":"2024-03-07T08:33:16.885023940Z","user":{"lastName":"Wilkinson","firstName":"Tonette","birthDate":"1996-09-03T02:11:51.527Z","address":{"number":"501","street":"Miller Cove","city":"Port Deandrefort","region":"New York","postalCode":"81142"},"favoriteBeers":[{"name":"Two Hearted Ale","style":"Stout"},{"name":"St. Bernardus Abt 12","style":"Wood-aged Beer"}]},"payload":"ATylLUiU8i8JgQ8lalwNt/oYcGPC4SUe5AS05Yi8YmsG8CkBPGrrvPLIrKQW3fvy+tCjvz+YIBMyMw8FdmT0UY0J5IgO9VkW460psXWbTt3iHTaInvd0WzZkRtj3fVfKZjbpreU/yVxElpDn07Go7twWJ4ZXwhs0QbhmxfuHGKa18loGraeaB/LQUsTga0YWpLJA5OfkUFBcy4UYFGV0aXJ/KsqUz+xpva9JHzCPRPIOppMGUTKl57/3qbb3q/tx5yj1No6lvBzrcdAruzNYUX6SQYiXMNtIW2iIi/+HH4M23bLv0nZyVbP+QPldLHnlboTuvUX14PlR+wLpqilKVtESMQJbFE0kw29d71f/QL3swXPM5xF5ZXn0j0Iax2QeAP+TwWkHXdK7a3DpzG4AUBnprfyPkGTX3H/1CZQs7UhfHFiS/dUBG7BQLmJFkc2mq16AdlAAIBA6BAP2BN38KFwBjhlgVxvJqi6hmS56DHNrDGj2xZGFCYVFMqd4wuOivYZK/pwYYVQlkqm9VnI0sbhvN2KLQ+adZFdacaFuOGEYPoWRAdGRd5Qe3uRzwNPMA/jVWsIebKOxX94MVet/llo43z2i5mxc2fjTGpwBFBlRtRwKkIPXtfwoAhUPG8/QQRwMltYCqObPARrUzRFB17xWKnU="}',
  },
  play: async ({ canvasElement, args }) => {
    const canvas = within(canvasElement);
    expect(canvas.getByText(args.value));
  },
};

export const JSONValueWithHighlight: Story = {
  args: {
    value:
      '{"timestamp":"2024-03-07T08:33:16.885023940Z","user":{"lastName":"Wilkinson","firstName":"Tonette","birthDate":"1996-09-03T02:11:51.527Z","address":{"number":"501","street":"Miller Cove","city":"Port Deandrefort","region":"New York","postalCode":"81142"},"favoriteBeers":[{"name":"Two Hearted Ale","style":"Stout"},{"name":"St. Bernardus Abt 12","style":"Wood-aged Beer"}]},"payload":"ATylLUiU8i8JgQ8lalwNt/oYcGPC4SUe5AS05Yi8YmsG8CkBPGrrvPLIrKQW3fvy+tCjvz+YIBMyMw8FdmT0UY0J5IgO9VkW460psXWbTt3iHTaInvd0WzZkRtj3fVfKZjbpreU/yVxElpDn07Go7twWJ4ZXwhs0QbhmxfuHGKa18loGraeaB/LQUsTga0YWpLJA5OfkUFBcy4UYFGV0aXJ/KsqUz+xpva9JHzCPRPIOppMGUTKl57/3qbb3q/tx5yj1No6lvBzrcdAruzNYUX6SQYiXMNtIW2iIi/+HH4M23bLv0nZyVbP+QPldLHnlboTuvUX14PlR+wLpqilKVtESMQJbFE0kw29d71f/QL3swXPM5xF5ZXn0j0Iax2QeAP+TwWkHXdK7a3DpzG4AUBnprfyPkGTX3H/1CZQs7UhfHFiS/dUBG7BQLmJFkc2mq16AdlAAIBA6BAP2BN38KFwBjhlgVxvJqi6hmS56DHNrDGj2xZGFCYVFMqd4wuOivYZK/pwYYVQlkqm9VnI0sbhvN2KLQ+adZFdacaFuOGEYPoWRAdGRd5Qe3uRzwNPMA/jVWsIebKOxX94MVet/llo43z2i5mxc2fjTGpwBFBlRtRwKkIPXtfwoAhUPG8/QQRwMltYCqObPARrUzRFB17xWKnU="}',
    highlight: "Wilki",
  },
  play: async ({ canvasElement, args }) => {
    await expect(canvasElement.querySelectorAll("mark").length).toBeGreaterThan(
      0,
    );
  },
};

export const WithBinaryValue: Story = {
  args: {
    value: "SGVsbG8sIFdvcmxkIQ==",
  },
  play: async ({ canvasElement, args }) => {
    const canvas = within(canvasElement);
    expect(canvas.getByText(args.value));
  },
};
