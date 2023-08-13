import type { ComponentMeta, ComponentStory } from "@storybook/react";
import { UnknownValuePreview } from "./UnknownValuePreview";

export default {
  component: UnknownValuePreview,
  args: {
    value:
      "Lorem ipsum dolor sit amet, consectetur adipisicing elit. Autem deleniti eligendi vero voluptatibus. Amet animi, deserunt dignissimos, eveniet non nulla numquam obcaecati perferendis sapiente temporibus tenetur vero. Incidunt, libero, vel?",
  },
} as ComponentMeta<typeof UnknownValuePreview>;

const Template: ComponentStory<typeof UnknownValuePreview> = (args) => (
  <UnknownValuePreview {...args}>todo</UnknownValuePreview>
);

export const LongTextIsTruncated = Template.bind({});
LongTextIsTruncated.args = {};

export const ShortTextIsNotTruncated = Template.bind({});
ShortTextIsNotTruncated.args = {
  value: "Lorem ipsum dolor sit amet, consectetur adipisicing elit",
};
