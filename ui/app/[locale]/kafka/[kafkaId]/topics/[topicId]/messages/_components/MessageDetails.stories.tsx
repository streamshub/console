import type { ComponentMeta, ComponentStory } from "@storybook/react";
import { MessageDetailsBody } from "./MessageDetails";

export default {
  component: MessageDetailsBody,
  args: {
    messageKey:
      "this-is-a-very-long-key-that-might-cause-some-trouble-figuring-out-column-widths",
    partition: 4,
    offset: 16,
    timestamp: new Date("2022-03-15T14:11:57.103Z").toISOString(),
    headers: {
      "post-office-box": "string",
      "extended-address": "string",
      "street-address": "string",
      locality: "string",
      region: "LATAM",
      "postal-code": "string",
      "country-name": "string",
    },
    value:
      '{"order":{"address":{"street":"123 any st","city":"Austin","state":"TX","zip":"78626"},"contact":{"firstName":"james","lastName":"smith","phone":"512-123-1234"},"orderId":"123"},"primitives":{"stringPrimitive":"some value","booleanPrimitive":true,"numberPrimitive":24},"addressList":[{"street":"123 any st","city":"Austin","state":"TX","zip":"78626"},{"street":"123 any st","city":"Austin","state":"TX","zip":"78626"},{"street":"123 any st","city":"Austin","state":"TX","zip":"78626"},{"street":"123 any st","city":"Austin","state":"TX","zip":"78626"}]}',
    defaultTab: "value",
  },
} as ComponentMeta<typeof MessageDetailsBody>;

const Template: ComponentStory<typeof MessageDetailsBody> = (args) => (
  <MessageDetailsBody {...args} />
);

export const Example = Template.bind({});
Example.args = {};

export const StartWithHeaders = Template.bind({});
StartWithHeaders.args = {
  defaultTab: "headers",
};
