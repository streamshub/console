import { Message } from "@/api/messages/schema";
import type { Meta, StoryObj } from "@storybook/nextjs";
import { expect, fn, userEvent, within } from "storybook/test";
import { subSeconds } from "date-fns";
import { MessagesTable, MessagesTableProps } from "./MessagesTable";

export default {
  component: MessagesTable,
  args: {
    lastUpdated: new Date(Date.UTC(2024, 11, 31, 23, 59, 59, 999)),
    partitions: 5,
    messages: [],
    topicName: "Example",
    onSearch: fn(),
    onSelectMessage: fn(),
    onDeselectMessage: fn(),
    filterLimit: 50,
  },
  render: (props) => <MessagesTable {...props} messages={sampleData(props)} />,
} as Meta<typeof MessagesTable>;

type Story = StoryObj<typeof MessagesTable>;

export const Example: Story = {
  play: async ({ canvasElement, args }) => {
    const canvas = within(canvasElement);
    const messages = sampleData(args);

    expect(messages.length).toBeGreaterThan(0);
    const rows = await canvas.findAllByRole("row");

    const targetRow = rows.find((r) =>
      r.textContent?.includes("this-is-a-very-long-key"),
    );

    if (!targetRow) {
      throw new Error(`Target row not found. Available rows: ${rows.length}`);
    }
    await userEvent.click(targetRow);
    await expect(args.onSelectMessage).toHaveBeenCalledWith(messages[1]);

    const search = await canvas.findByDisplayValue(/messages=latest/i);

    await userEvent.clear(search);
    await userEvent.type(search, "messages=latest retrieve=50 foo bar");
    await userEvent.keyboard("[Enter]");

    await expect(args.onSearch).toBeCalledWith(
      expect.objectContaining({
        query: {
          value: "foo bar",
          where: "everywhere",
        },
        limit: 50,
      }),
    );
  },
};

export const SearchWithMatches: Story = {
  args: {
    filterQuery: "foo",
    filterWhere: "value",
  },
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement);

    const highlighted = await canvas.findAllByText(/foo/i);
    expect(highlighted.length).toBeGreaterThan(0);
  },
};

export const SearchWithoutMatches: Story = {
  args: {
    filterQuery: "lorem dolor",
  },
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement);
    const emptyState = await canvas.findByText("No messages data");
    expect(emptyState).toBeInTheDocument();
    expect(canvasElement.querySelectorAll("mark").length).toBe(0);
  },
};
// export const AdvancedSearch: Story = {
//   play: async ({ canvasElement }) => {
//     const container = within(canvasElement);
//     await userEvent.click(
//       await container.findByLabelText("Open advanced search"),
//     );
//     await userEvent.click(container.queryAllByText("All partitions")[0]);
//     await userEvent.click(await container.findByText("2"));
//     await userEvent.click(await container.findByText("Latest messages"));
//     await userEvent.click((await container.findAllByText("Offset"))[0]);
//     await userEvent.type(
//       await container.findByLabelText("Specify offset"),
//       "1337",
//     );
//     await userEvent.click(await container.findByLabelText("Search"));
//   },
// };

function sampleData({
  filterEpoch,
  filterLimit,
  filterOffset,
  filterPartition,
  filterQuery,
  filterTimestamp,
  filterWhere,
}: Pick<
  MessagesTableProps,
  | "filterEpoch"
  | "filterLimit"
  | "filterOffset"
  | "filterPartition"
  | "filterQuery"
  | "filterTimestamp"
  | "filterWhere"
>) {
  const messages: Message[] = [
    {
      attributes: {
        partition: 0,
        offset: 0,
        timestamp: "2022-03-15T14:10:57.105Z",
        headers: {
          "key.globalId": "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0002",
          "key.encoding": "BINARY",
        },
        value:
          '{"order":{"address":{"street":"123 any st","city":"Austin","state":"TX","zip":"78626"},"contact":{"firstName":"james","lastName":"smith","phone":"512-123-1234"},"orderId":"123","customerName":""},"primitives":{"stringPrimitive":"some value","booleanPrimitive":true,"numberPrimitive":24},"addressList":[{"street":"123 any st","city":"Austin","state":"TX","zip":"78626"},{"street":"123 any st","city":"Austin","state":"TX","zip":"78626"},{"street":"123 any st","city":"Austin","state":"TX","zip":"78626"},{"street":"123 any st","city":"Austin","state":"TX","zip":"78626"}]}',
        size: 1234,
      },
      relationships: {
        valueSchema: {
          meta: {
            artifactType: "AVRO",
          },
        },
      },
    },
    {
      attributes: {
        key: "this-is-a-very-long-key-that-might-cause-some-trouble-figuring-out-column-widths",
        partition: 4,
        offset: 16,
        timestamp: "2022-03-15T14:10:57.104Z",
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
        size: 1234,
      },
      relationships: {
        keySchema: {
          meta: {
            artifactType: "PROTOBUF",
          },
        },
      },
    },
    {
      attributes: {
        partition: 2,
        offset: 21,
        timestamp: "2022-03-15T14:10:57.103Z",
        headers: {
          never: `change`,
        },
        value: '{"foo": "bar", "baz": "???"}',
        size: 432,
      },
      relationships: {},
    },
    {
      attributes: {
        partition: 3,
        offset: 3,
        timestamp: "2022-03-15T14:10:57.102Z",
        headers: {},
        value: "random string",
        size: 532,
      },
      relationships: {},
    },
    {
      attributes: {
        partition: 5,
        offset: 44,
        timestamp: "2022-03-15T14:10:57.101Z",
        headers: {},
        value: "",
        size: 0,
      },
      relationships: {},
    },
  ];
  const numberOfMessages =
    filterLimit === "continuously" ? 50 : (filterLimit ?? 50);
  return new Array(Math.ceil(numberOfMessages / messages.length))
    .fill(0)
    .flatMap((_, i) =>
      messages
        .map<Message>((m, j) => ({
          attributes: {
            ...m.attributes,
            timestamp: subSeconds(m.attributes.timestamp, i),
            offset:
              (filterOffset ?? 0) + numberOfMessages - messages.length * i - j,
            partition: filterPartition || m.attributes.partition,
          },
          relationships: {
            ...m.relationships,
          },
        }))
        .filter((m) => {
          if (filterQuery) {
            switch (filterWhere) {
              case "key":
                return m.attributes.key.includes(filterQuery);
              case "value":
                return m.attributes.value.includes(filterQuery);
              case "headers":
                return JSON.stringify(m.attributes.headers || {}).includes(
                  filterQuery,
                );
              default:
                return JSON.stringify(m).includes(filterQuery);
            }
          }
          return true;
        })
        .slice(0, numberOfMessages),
    );
}
