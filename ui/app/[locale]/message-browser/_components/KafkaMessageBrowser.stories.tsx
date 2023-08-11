import type { ComponentMeta, ComponentStory } from "@storybook/react";
import { userEvent, within } from "@storybook/testing-library";
import type { Message } from "ui-models/src/models/message";
import type { DateIsoString } from "../../../../ui-models/src/types";
import { apiError, fakeApi } from "../storiesHelpers";
import { KafkaMessageBrowser } from "./KafkaMessageBrowser";

export default {
  component: KafkaMessageBrowser,
} as ComponentMeta<typeof KafkaMessageBrowser>;

const Template: ComponentStory<typeof KafkaMessageBrowser> = (args) => (
  <KafkaMessageBrowser {...args}>todo</KafkaMessageBrowser>
);

export const Example = Template.bind({});
Example.args = {
  getMessages: sampleData,
};

export const InitialLoading = Template.bind({});
InitialLoading.args = {
  getMessages: () => new Promise(() => false),
};

export const NoData = Template.bind({});
NoData.args = {
  getMessages: () =>
    fakeApi<{ messages: Message[]; partitions: number }>(
      { messages: [], partitions: 0 },
      500
    ),
};

export const ApiError = Template.bind({});
ApiError.args = {
  getMessages: () => apiError<{ messages: Message[]; partitions: number }>(),
};

export const NoMatch = Template.bind({});
NoMatch.args = {
  getMessages: (args) => {
    return args.offset === undefined &&
      args.partition === undefined &&
      args.timestamp === undefined
      ? sampleData(args)
      : fakeApi<{ messages: Message[]; partitions: number }>(
          { messages: [], partitions: 0 },
          500
        );
  },
};
NoMatch.play = async ({ canvasElement }) => {
  const container = within(canvasElement);
  await userEvent.click(await container.findByLabelText("Show Filters"));
  await userEvent.type(
    await container.findByLabelText("Specify partition value"),
    "1337"
  );
  await userEvent.keyboard("[ArrowDown][Enter]");
  await userEvent.click(await container.findByText("Latest messages"));
  await userEvent.click((await container.findAllByText("Offset"))[0]);
  await userEvent.type(
    await container.findByLabelText("Specify offset"),
    "1337"
  );
  await userEvent.click(await container.findByLabelText("Search"));
};

function sampleData(args: {
  partition?: number;
  offset?: number;
  timestamp?: DateIsoString;
  limit: number;
}) {
  const numberOfMessages = args.limit;
  const messages: Message[] = [
    {
      partition: 0,
      offset: 0,
      timestamp: "2022-03-15T14:11:57.102Z",
      headers: {
        random: `${Math.random()}`,
      },
      value:
        '{"order":{"address":{"street":"123 any st","city":"Austin","state":"TX","zip":"78626"},"contact":{"firstName":"james","lastName":"smith","phone":"512-123-1234"},"orderId":"123","customerName":""},"primitives":{"stringPrimitive":"some value","booleanPrimitive":true,"numberPrimitive":24},"addressList":[{"street":"123 any st","city":"Austin","state":"TX","zip":"78626"},{"street":"123 any st","city":"Austin","state":"TX","zip":"78626"},{"street":"123 any st","city":"Austin","state":"TX","zip":"78626"},{"street":"123 any st","city":"Austin","state":"TX","zip":"78626"}]}',
    },
    {
      key: "this-is-a-very-long-key-that-might-cause-some-trouble-figuring-out-column-widths",
      partition: 4,
      offset: 16,
      timestamp: "2022-03-15T14:11:57.103Z",
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
    },
    {
      partition: 2,
      offset: 21,
      timestamp: "2022-03-15T14:10:57.103Z",
      headers: {
        never: `change`,
      },
      value: '{"foo": "bar", "baz": "???"}',
    },
    {
      partition: 3,
      offset: 3,
      timestamp: "2022-03-15T14:10:57.103Z",
      headers: {},
      value: "random string",
    },
    {
      partition: 5,
      offset: 44,
      timestamp: "2022-03-15T14:10:57.103Z",
      headers: {},
      value: "",
    },
  ];
  return fakeApi<{ messages: Message[]; partitions: number }>(
    {
      messages: new Array(Math.ceil(numberOfMessages / messages.length))
        .fill(0)
        .flatMap((_, i) =>
          messages.map((m, j) => ({
            ...m,
            offset:
              args.partition !== undefined
                ? (args.offset || 0) + i * messages.length + j
                : m.offset,
            partition: args.partition || m.partition,
            _: `${i}-${j}`,
          }))
        )
        .slice(0, numberOfMessages),
      partitions: 10000,
    },
    500
  );
}
