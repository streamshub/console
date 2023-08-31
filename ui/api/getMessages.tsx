import { fakeApi } from "@/api/fakeApi";

export function getMessages(args: {
  partition?: number;
  offset?: number;
  timestamp?: DateIsoString;
  limit: number;
}): Promise<MessageApiResponse> {
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
  return fakeApi<MessageApiResponse>({
    lastUpdated: new Date(),
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
        })),
      )
      .slice(0, numberOfMessages),
    partitions: 10000,
    offsetMin: 0,
    offsetMax: 10,

    filter: {
      partition: 0,
      offset: 0,
      timestamp: undefined,
      limit: undefined,
      epoch: undefined,
    },
  });
}
