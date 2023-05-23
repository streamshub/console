import { InputGroup } from "@patternfly/react-core";
import type { ComponentStory, ComponentMeta } from "@storybook/react";
import { within, userEvent } from "@storybook/testing-library";

import { SearchInput } from "./SearchInput";

export default {
  component: SearchInput,
  args: {
    validate: (v) => v !== "error",
  },
} as ComponentMeta<typeof SearchInput>;

const Template: ComponentStory<typeof SearchInput> = (args) => (
  <InputGroup>
    <SearchInput {...args} />
  </InputGroup>
);

export const EmptyState = Template.bind({});

export const SomeValidInput = Template.bind({});
SomeValidInput.args = {
  placeholder: "Searching something",
};

SomeValidInput.play = async ({ canvasElement }) => {
  const story = within(canvasElement);
  await userEvent.type(
    await story.findByLabelText("Searching something"),
    "something"
  );
  await userEvent.click(await story.findByLabelText("Search"));
};
SomeValidInput.parameters = {
  docs: {
    description: {
      story: `A user can type some valid search text and click the search button`,
    },
  },
};

export const TypeErrorForInvalid = Template.bind({});
TypeErrorForInvalid.args = {
  placeholder: "Searching something",
  errorMessage: "Why did you type error???",
};
TypeErrorForInvalid.play = async ({ canvasElement }) => {
  const story = within(canvasElement);
  await userEvent.type(
    await story.findByLabelText("Searching something"),
    "error"
  );
};
TypeErrorForInvalid.parameters = {
  docs: {
    description: {
      story: `When user enter some invalid search text, Tooltip text will be displayed describing why the text entered is invalid. Search is also disabled.`,
    },
  },
};
