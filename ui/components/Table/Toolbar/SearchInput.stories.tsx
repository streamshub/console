import { InputGroup } from "@patternfly/react-core";
import type { Meta, StoryObj } from "@storybook/react";
import { expect, fn, userEvent, within } from "@storybook/test";

import { SearchInput } from "./SearchInput";

export default {
  component: SearchInput,
  args: {
    validate: (v) => v !== "error",
    onSearch: fn(),
  },
  decorators: (Story) => <InputGroup>{Story()}</InputGroup>,
} as Meta<typeof SearchInput>;

type Story = StoryObj<typeof SearchInput>;

export const EmptyState: Story = {};

export const SomeValidInput: Story = {
  args: {
    placeholder: "Searching something",
  },

  play: async ({ canvasElement, args }) => {
    const canvas = within(canvasElement);
    await userEvent.type(
      await canvas.findByLabelText("Searching something"),
      "something",
    );
    await userEvent.click(await canvas.findByLabelText("Search"));
    expect(args.onSearch).toBeCalledWith("something");
  },

  parameters: {
    docs: {
      description: {
        story: `A user can type some valid search text and click the search button`,
      },
    },
  },
};

export const TypeErrorForInvalid: Story = {
  args: {
    placeholder: "Searching something",
    errorMessage: "Why did you type error???",
  },
  play: async ({ canvasElement }) => {
    const story = within(canvasElement);
    await userEvent.type(
      await story.findByLabelText("Searching something"),
      "error",
    );
  },
  parameters: {
    docs: {
      description: {
        story: `When user enter some invalid search text, Tooltip text will be displayed describing why the text entered is invalid. Search is also disabled.`,
      },
    },
  },
};
