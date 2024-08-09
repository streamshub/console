import type { Meta, StoryObj } from "@storybook/react";

import { ConsumerGroupsTable as Comp } from "./ConsumerGroupsTable";

export default {
  component: Comp,
  args: {
    page: 1,
    perPage: 20,
    consumerGroups: [],
  },
} as Meta<typeof Comp>;
type Story = StoryObj<typeof Comp>;

export const ConsumerGroups: Story = {
  args: {
    consumerGroups: [
      {
        id: "console-datagen-group-0",
        attributes: {
          state: "STABLE",
          offsets: [{
            lag: 200
          }, {
            lag: 1000
          }],
          members: [
            {
              host: "localhost",
              memberId: "member-1",
              clientId: "client-1",
              groupInstanceId: "instance-1",
              assignments: [
                {
                  topicName: "console_datagen_000-a",
                  topicId: "1",
                },
                {
                  topicName: "console_datagen_000-b",
                  topicId: "2",
                },
              ],
            },
          ],
        },
      },
      {
        id: "console-datagen-group-1",
        attributes: {
          state: "EMPTY",
          offsets: [{
            lag: 400
          }, {
            lag: 240
          }],
          members: [
            {
              host: "localhost",
              memberId: "member-1",
              clientId: "client-1",
              groupInstanceId: "instance-1",
              assignments: [
                {
                  topicName: "console_datagen_001-a",
                  topicId: "1",
                },
                {
                  topicName: "console_datagen_001-b",
                  topicId: "2",
                },
              ],
            },
          ],
        },
      },
      {
        id: "console-datagen-group-2",
        attributes: {
          state: "STABLE",
          offsets: [{
            lag: 200
          }, {
            lag: 400
          }, {
            lag: 400
          }],
          members: [
            {
              host: "localhost",
              memberId: "member-1",
              clientId: "client-1",
              groupInstanceId: "instance-1",
              assignments: [
                {
                  topicName: "console_datagen_002-a",
                  topicId: "1",
                },
                {
                  topicName: "console_datagen_002-b",
                  topicId: "2",
                },
              ],
            },
          ],
        },
      },
      {
        id: "console-datagen-group-2",
        attributes: {
          state: "STABLE",
          offsets: [{
            lag: 200
          }, {
            lag: 400
          }, {
            lag: 400
          }],
          members: [
            {
              host: "localhost",
              memberId: "member-1",
              clientId: "client-1",
              groupInstanceId: "instance-1",
              assignments: [
                {
                  topicName: "console_datagen_002-a",
                  topicId: "1",
                },
                {
                  topicName: "console_datagen_002-b",
                  topicId: "2",
                },
              ],
            },
          ],
        },
      },
      {
        id: "console-datagen-group-2",
        attributes: {
          state: "STABLE",
          offsets: [{
            lag: 200
          }, {
            lag: 400
          }, {
            lag: 400
          }],
          members: [
            {
              host: "localhost",
              memberId: "member-1",
              clientId: "client-1",
              groupInstanceId: "instance-1",
              assignments: [
                {
                  topicName: "console_datagen_002-a",
                  topicId: "1",
                },
                {
                  topicName: "console_datagen_002-b",
                  topicId: "2",
                },
              ],
            },
          ],
        },
      },
      {
        id: "console-datagen-group-2",
        attributes: {
          state: "STABLE",
          offsets: [{
            lag: 200
          }, {
            lag: 400
          }, {
            lag: 400
          }],
          members: [
            {
              host: "localhost",
              memberId: "member-1",
              clientId: "client-1",
              groupInstanceId: "instance-1",
              assignments: [
                {
                  topicName: "console_datagen_002-a",
                  topicId: "1",
                },
                {
                  topicName: "console_datagen_002-b",
                  topicId: "2",
                },
              ],
            },
          ],
        },
      },
      {
        id: "console-datagen-group-2",
        attributes: {
          state: "STABLE",
          offsets: [{
            lag: 200
          }, {
            lag: 400
          }, {
            lag: 400
          }],
          members: [
            {
              host: "localhost",
              memberId: "member-1",
              clientId: "client-1",
              groupInstanceId: "instance-1",
              assignments: [
                {
                  topicName: "console_datagen_002-a",
                  topicId: "1",
                },
                {
                  topicName: "console_datagen_002-b",
                  topicId: "2",
                },
              ],
            },
          ],
        },
      },
      {
        id: "console-datagen-group-2",
        attributes: {
          state: "STABLE",
          offsets: [{
            lag: 200
          }, {
            lag: 400
          }, {
            lag: 400
          }],
          members: [
            {
              host: "localhost",
              memberId: "member-1",
              clientId: "client-1",
              groupInstanceId: "instance-1",
              assignments: [
                {
                  topicName: "console_datagen_002-a",
                  topicId: "1",
                },
                {
                  topicName: "console_datagen_002-b",
                  topicId: "2",
                },
              ],
            },
          ],
        },
      },
      {
        id: "console-datagen-group-2",
        attributes: {
          state: "STABLE",
          offsets: [{
            lag: 200
          }, {
            lag: 400
          }, {
            lag: 400
          }],
          members: [
            {
              host: "localhost",
              memberId: "member-1",
              clientId: "client-1",
              groupInstanceId: "instance-1",
              assignments: [
                {
                  topicName: "console_datagen_002-a",
                  topicId: "1",
                },
                {
                  topicName: "console_datagen_002-b",
                  topicId: "2",
                },
              ],
            },
          ],
        },
      },
      {
        id: "console-datagen-group-2",
        attributes: {
          state: "STABLE",
          offsets: [{
            lag: 200
          }, {
            lag: 400
          }, {
            lag: 400
          }],
          members: [
            {
              host: "localhost",
              memberId: "member-1",
              clientId: "client-1",
              groupInstanceId: "instance-1",
              assignments: [
                {
                  topicName: "console_datagen_002-a",
                  topicId: "1",
                },
                {
                  topicName: "console_datagen_002-b",
                  topicId: "2",
                },
              ],
            },
          ],
        },
      },
      {
        id: "console-datagen-group-2",
        attributes: {
          state: "STABLE",
          offsets: [{
            lag: 200
          }, {
            lag: 400
          }, {
            lag: 400
          }],
          members: [
            {
              host: "localhost",
              memberId: "member-1",
              clientId: "client-1",
              groupInstanceId: "instance-1",
              assignments: [
                {
                  topicName: "console_datagen_002-a",
                  topicId: "1",
                },
                {
                  topicName: "console_datagen_002-b",
                  topicId: "2",
                },
              ],
            },
          ],
        },
      },
      {
        id: "console-datagen-group-2",
        attributes: {
          state: "STABLE",
          offsets: [{
            lag: 200
          }, {
            lag: 400
          }, {
            lag: 400
          }],
          members: [
            {
              host: "localhost",
              memberId: "member-1",
              clientId: "client-1",
              groupInstanceId: "instance-1",
              assignments: [
                {
                  topicName: "console_datagen_002-a",
                  topicId: "1",
                },
                {
                  topicName: "console_datagen_002-b",
                  topicId: "2",
                },
              ],
            },
          ],
        },
      },
      {
        id: "console-datagen-group-2",
        attributes: {
          state: "STABLE",
          offsets: [{
            lag: 200
          }, {
            lag: 400
          }, {
            lag: 400
          }],
          members: [
            {
              host: "localhost",
              memberId: "member-1",
              clientId: "client-1",
              groupInstanceId: "instance-1",
              assignments: [
                {
                  topicName: "console_datagen_002-a",
                  topicId: "1",
                },
                {
                  topicName: "console_datagen_002-b",
                  topicId: "2",
                },
              ],
            },
          ],
        },
      },
      {
        id: "console-datagen-group-2",
        attributes: {
          state: "STABLE",
          offsets: [{
            lag: 200
          }, {
            lag: 400
          }, {
            lag: 400
          }],
          members: [
            {
              host: "localhost",
              memberId: "member-1",
              clientId: "client-1",
              groupInstanceId: "instance-1",
              assignments: [
                {
                  topicName: "console_datagen_002-a",
                  topicId: "1",
                },
                {
                  topicName: "console_datagen_002-b",
                  topicId: "2",
                },
              ],
            },
          ],
        },
      },
      {
        id: "console-datagen-group-2",
        attributes: {
          state: "STABLE",
          offsets: [{
            lag: 200
          }, {
            lag: 400
          }, {
            lag: 400
          }],
          members: [
            {
              host: "localhost",
              memberId: "member-1",
              clientId: "client-1",
              groupInstanceId: "instance-1",
              assignments: [
                {
                  topicName: "console_datagen_002-a",
                  topicId: "1",
                },
                {
                  topicName: "console_datagen_002-b",
                  topicId: "2",
                },
              ],
            },
          ],
        },
      },
      {
        id: "console-datagen-group-2",
        attributes: {
          state: "STABLE",
          offsets: [{
            lag: 200
          }, {
            lag: 400
          }, {
            lag: 400
          }],
          members: [
            {
              host: "localhost",
              memberId: "member-1",
              clientId: "client-1",
              groupInstanceId: "instance-1",
              assignments: [
                {
                  topicName: "console_datagen_002-a",
                  topicId: "1",
                },
                {
                  topicName: "console_datagen_002-b",
                  topicId: "2",
                },
              ],
            },
          ],
        },
      },
      {
        id: "console-datagen-group-2",
        attributes: {
          state: "STABLE",
          offsets: [{
            lag: 200
          }, {
            lag: 400
          }, {
            lag: 400
          }],
          members: [
            {
              host: "localhost",
              memberId: "member-1",
              clientId: "client-1",
              groupInstanceId: "instance-1",
              assignments: [
                {
                  topicName: "console_datagen_002-a",
                  topicId: "1",
                },
                {
                  topicName: "console_datagen_002-b",
                  topicId: "2",
                },
              ],
            },
          ],
        },
      },
      {
        id: "console-datagen-group-2",
        attributes: {
          state: "STABLE",
          offsets: [{
            lag: 200
          }, {
            lag: 400
          }, {
            lag: 400
          }],
          members: [
            {
              host: "localhost",
              memberId: "member-1",
              clientId: "client-1",
              groupInstanceId: "instance-1",
              assignments: [
                {
                  topicName: "console_datagen_002-a",
                  topicId: "1",
                },
                {
                  topicName: "console_datagen_002-b",
                  topicId: "2",
                },
              ],
            },
          ],
        },
      },
      {
        id: "console-datagen-group-2",
        attributes: {
          state: "STABLE",
          offsets: [{
            lag: 200
          }, {
            lag: 400
          }, {
            lag: 400
          }],
          members: [
            {
              host: "localhost",
              memberId: "member-1",
              clientId: "client-1",
              groupInstanceId: "instance-1",
              assignments: [
                {
                  topicName: "console_datagen_002-a",
                  topicId: "1",
                },
                {
                  topicName: "console_datagen_002-b",
                  topicId: "2",
                },
              ],
            },
          ],
        },
      },
      {
        id: "console-datagen-group-2",
        attributes: {
          state: "STABLE",
          offsets: [{
            lag: 200
          }, {
            lag: 400
          }, {
            lag: 400
          }],
          members: [
            {
              host: "localhost",
              memberId: "member-1",
              clientId: "client-1",
              groupInstanceId: "instance-1",
              assignments: [
                {
                  topicName: "console_datagen_002-a",
                  topicId: "1",
                },
                {
                  topicName: "console_datagen_002-b",
                  topicId: "2",
                },
              ],
            },
          ],
        },
      },
      {
        id: "console-datagen-group-2",
        attributes: {
          state: "STABLE",
          offsets: [{
            lag: 200
          }, {
            lag: 400
          }, {
            lag: 400
          }],
          members: [
            {
              host: "localhost",
              memberId: "member-1",
              clientId: "client-1",
              groupInstanceId: "instance-1",
              assignments: [
                {
                  topicName: "console_datagen_002-a",
                  topicId: "1",
                },
                {
                  topicName: "console_datagen_002-b",
                  topicId: "2",
                },
              ],
            },
          ],
        },
      },
      {
        id: "console-datagen-group-2",
        attributes: {
          state: "STABLE",
          offsets: [{
            lag: 200
          }, {
            lag: 400
          }, {
            lag: 400
          }],
          members: [
            {
              host: "localhost",
              memberId: "member-1",
              clientId: "client-1",
              groupInstanceId: "instance-1",
              assignments: [
                {
                  topicName: "console_datagen_002-a",
                  topicId: "1",
                },
                {
                  topicName: "console_datagen_002-b",
                  topicId: "2",
                },
              ],
            },
          ],
        },
      },
      {
        id: "console-datagen-group-2",
        attributes: {
          state: "STABLE",
          offsets: [{
            lag: 200
          }, {
            lag: 400
          }, {
            lag: 400
          }],
          members: [
            {
              host: "localhost",
              memberId: "member-1",
              clientId: "client-1",
              groupInstanceId: "instance-1",
              assignments: [
                {
                  topicName: "console_datagen_002-a",
                  topicId: "1",
                },
                {
                  topicName: "console_datagen_002-b",
                  topicId: "2",
                },
              ],
            },
          ],
        },
      },
      {
        id: "console-datagen-group-2",
        attributes: {
          state: "STABLE",
          offsets: [{
            lag: 200
          }, {
            lag: 400
          }, {
            lag: 400
          }],
          members: [
            {
              host: "localhost",
              memberId: "member-1",
              clientId: "client-1",
              groupInstanceId: "instance-1",
              assignments: [
                {
                  topicName: "console_datagen_002-a",
                  topicId: "1",
                },
                {
                  topicName: "console_datagen_002-b",
                  topicId: "2",
                },
              ],
            },
          ],
        },
      },
      {
        id: "console-datagen-group-2",
        attributes: {
          state: "STABLE",
          offsets: [{
            lag: 200
          }, {
            lag: 400
          }, {
            lag: 400
          }],
          members: [
            {
              host: "localhost",
              memberId: "member-1",
              clientId: "client-1",
              groupInstanceId: "instance-1",
              assignments: [
                {
                  topicName: "console_datagen_002-a",
                  topicId: "1",
                },
                {
                  topicName: "console_datagen_002-b",
                  topicId: "2",
                },
              ],
            },
          ],
        },
      },
      {
        id: "console-datagen-group-2",
        attributes: {
          state: "STABLE",
          offsets: [{
            lag: 200
          }, {
            lag: 400
          }, {
            lag: 400
          }],
          members: [
            {
              host: "localhost",
              memberId: "member-1",
              clientId: "client-1",
              groupInstanceId: "instance-1",
              assignments: [
                {
                  topicName: "console_datagen_002-a",
                  topicId: "1",
                },
                {
                  topicName: "console_datagen_002-b",
                  topicId: "2",
                },
              ],
            },
          ],
        },
      },
      {
        id: "console-datagen-group-2",
        attributes: {
          state: "STABLE",
          offsets: [{
            lag: 200
          }, {
            lag: 400
          }, {
            lag: 400
          }],
          members: [
            {
              host: "localhost",
              memberId: "member-1",
              clientId: "client-1",
              groupInstanceId: "instance-1",
              assignments: [
                {
                  topicName: "console_datagen_002-a",
                  topicId: "1",
                },
                {
                  topicName: "console_datagen_002-b",
                  topicId: "2",
                },
              ],
            },
          ],
        },
      },
      {
        id: "console-datagen-group-2",
        attributes: {
          state: "STABLE",
          offsets: [{
            lag: 200
          }, {
            lag: 400
          }, {
            lag: 400
          }],
          members: [
            {
              host: "localhost",
              memberId: "member-1",
              clientId: "client-1",
              groupInstanceId: "instance-1",
              assignments: [
                {
                  topicName: "console_datagen_002-a",
                  topicId: "1",
                },
                {
                  topicName: "console_datagen_002-b",
                  topicId: "2",
                },
              ],
            },
          ],
        },
      },
      {
        id: "console-datagen-group-2",
        attributes: {
          state: "STABLE",
          offsets: [{
            lag: 200
          }, {
            lag: 400
          }, {
            lag: 400
          }],
          members: [
            {
              host: "localhost",
              memberId: "member-1",
              clientId: "client-1",
              groupInstanceId: "instance-1",
              assignments: [
                {
                  topicName: "console_datagen_002-a",
                  topicId: "1",
                },
                {
                  topicName: "console_datagen_002-b",
                  topicId: "2",
                },
              ],
            },
          ],
        },
      },
      {
        id: "console-datagen-group-2",
        attributes: {
          state: "STABLE",
          offsets: [{
            lag: 200
          }, {
            lag: 400
          }, {
            lag: 400
          }],
          members: [
            {
              host: "localhost",
              memberId: "member-1",
              clientId: "client-1",
              groupInstanceId: "instance-1",
              assignments: [
                {
                  topicName: "console_datagen_002-a",
                  topicId: "1",
                },
                {
                  topicName: "console_datagen_002-b",
                  topicId: "2",
                },
              ],
            },
          ],
        },
      },
      {
        id: "console-datagen-group-2",
        attributes: {
          state: "STABLE",
          offsets: [{
            lag: 200
          }, {
            lag: 400
          }, {
            lag: 400
          }],
          members: [
            {
              host: "localhost",
              memberId: "member-1",
              clientId: "client-1",
              groupInstanceId: "instance-1",
              assignments: [
                {
                  topicName: "console_datagen_002-a",
                  topicId: "1",
                },
                {
                  topicName: "console_datagen_002-b",
                  topicId: "2",
                },
              ],
            },
          ],
        },
      },
      {
        id: "console-datagen-group-2",
        attributes: {
          state: "STABLE",
          offsets: [{
            lag: 200
          }, {
            lag: 400
          }, {
            lag: 400
          }],
          members: [
            {
              host: "localhost",
              memberId: "member-1",
              clientId: "client-1",
              groupInstanceId: "instance-1",
              assignments: [
                {
                  topicName: "console_datagen_002-a",
                  topicId: "1",
                },
                {
                  topicName: "console_datagen_002-b",
                  topicId: "2",
                },
              ],
            },
          ],
        },
      },
      {
        id: "console-datagen-group-2",
        attributes: {
          state: "STABLE",
          offsets: [{
            lag: 200
          }, {
            lag: 400
          }, {
            lag: 400
          }],
          members: [
            {
              host: "localhost",
              memberId: "member-1",
              clientId: "client-1",
              groupInstanceId: "instance-1",
              assignments: [
                {
                  topicName: "console_datagen_002-a",
                  topicId: "1",
                },
                {
                  topicName: "console_datagen_002-b",
                  topicId: "2",
                },
              ],
            },
          ],
        },
      },
      {
        id: "console-datagen-group-2",
        attributes: {
          state: "STABLE",
          offsets: [{
            lag: 200
          }, {
            lag: 400
          }, {
            lag: 400
          }],
          members: [
            {
              host: "localhost",
              memberId: "member-1",
              clientId: "client-1",
              groupInstanceId: "instance-1",
              assignments: [
                {
                  topicName: "console_datagen_002-a",
                  topicId: "1",
                },
                {
                  topicName: "console_datagen_002-b",
                  topicId: "2",
                },
              ],
            },
          ],
        },
      },
      {
        id: "console-datagen-group-2",
        attributes: {
          state: "STABLE",
          offsets: [{
            lag: 200
          }, {
            lag: 400
          }, {
            lag: 400
          }],
          members: [
            {
              host: "localhost",
              memberId: "member-1",
              clientId: "client-1",
              groupInstanceId: "instance-1",
              assignments: [
                {
                  topicName: "console_datagen_002-a",
                  topicId: "1",
                },
                {
                  topicName: "console_datagen_002-b",
                  topicId: "2",
                },
              ],
            },
          ],
        },
      },
      {
        id: "console-datagen-group-2",
        attributes: {
          state: "STABLE",
          offsets: [{
            lag: 200
          }, {
            lag: 400
          }, {
            lag: 400
          }],
          members: [
            {
              host: "localhost",
              memberId: "member-1",
              clientId: "client-1",
              groupInstanceId: "instance-1",
              assignments: [
                {
                  topicName: "console_datagen_002-a",
                  topicId: "1",
                },
                {
                  topicName: "console_datagen_002-b",
                  topicId: "2",
                },
              ],
            },
          ],
        },
      },
      {
        id: "console-datagen-group-2",
        attributes: {
          state: "STABLE",
          offsets: [{
            lag: 200
          }, {
            lag: 400
          }, {
            lag: 400
          }],
          members: [
            {
              host: "localhost",
              memberId: "member-1",
              clientId: "client-1",
              groupInstanceId: "instance-1",
              assignments: [
                {
                  topicName: "console_datagen_002-a",
                  topicId: "1",
                },
                {
                  topicName: "console_datagen_002-b",
                  topicId: "2",
                },
              ],
            },
          ],
        },
      },
      {
        id: "console-datagen-group-2",
        attributes: {
          state: "STABLE",
          offsets: [{
            lag: 200
          }, {
            lag: 400
          }, {
            lag: 400
          }],
          members: [
            {
              host: "localhost",
              memberId: "member-1",
              clientId: "client-1",
              groupInstanceId: "instance-1",
              assignments: [
                {
                  topicName: "console_datagen_002-a",
                  topicId: "1",
                },
                {
                  topicName: "console_datagen_002-b",
                  topicId: "2",
                },
              ],
            },
          ],
        },
      },
      {
        id: "console-datagen-group-2",
        attributes: {
          state: "STABLE",
          offsets: [{
            lag: 200
          }, {
            lag: 400
          }, {
            lag: 400
          }],
          members: [
            {
              host: "localhost",
              memberId: "member-1",
              clientId: "client-1",
              groupInstanceId: "instance-1",
              assignments: [
                {
                  topicName: "console_datagen_002-a",
                  topicId: "1",
                },
                {
                  topicName: "console_datagen_002-b",
                  topicId: "2",
                },
              ],
            },
          ],
        },
      },
      {
        id: "console-datagen-group-2",
        attributes: {
          state: "STABLE",
          offsets: [{
            lag: 200
          }, {
            lag: 400
          }, {
            lag: 400
          }],
          members: [
            {
              host: "localhost",
              memberId: "member-1",
              clientId: "client-1",
              groupInstanceId: "instance-1",
              assignments: [
                {
                  topicName: "console_datagen_002-a",
                  topicId: "1",
                },
                {
                  topicName: "console_datagen_002-b",
                  topicId: "2",
                },
              ],
            },
          ],
        },
      },
      {
        id: "console-datagen-group-2",
        attributes: {
          state: "STABLE",
          offsets: [{
            lag: 200
          }, {
            lag: 400
          }, {
            lag: 400
          }],
          members: [
            {
              host: "localhost",
              memberId: "member-1",
              clientId: "client-1",
              groupInstanceId: "instance-1",
              assignments: [
                {
                  topicName: "console_datagen_002-a",
                  topicId: "1",
                },
                {
                  topicName: "console_datagen_002-b",
                  topicId: "2",
                },
              ],
            },
          ],
        },
      },
      {
        id: "console-datagen-group-2",
        attributes: {
          state: "STABLE",
          offsets: [{
            lag: 200
          }, {
            lag: 400
          }, {
            lag: 400
          }],
          members: [
            {
              host: "localhost",
              memberId: "member-1",
              clientId: "client-1",
              groupInstanceId: "instance-1",
              assignments: [
                {
                  topicName: "console_datagen_002-a",
                  topicId: "1",
                },
                {
                  topicName: "console_datagen_002-b",
                  topicId: "2",
                },
              ],
            },
          ],
        },
      },
      {
        id: "console-datagen-group-2",
        attributes: {
          state: "STABLE",
          offsets: [{
            lag: 200
          }, {
            lag: 400
          }, {
            lag: 400
          }],
          members: [
            {
              host: "localhost",
              memberId: "member-1",
              clientId: "client-1",
              groupInstanceId: "instance-1",
              assignments: [
                {
                  topicName: "console_datagen_002-a",
                  topicId: "1",
                },
                {
                  topicName: "console_datagen_002-b",
                  topicId: "2",
                },
              ],
            },
          ],
        },
      },
      {
        id: "console-datagen-group-2",
        attributes: {
          state: "STABLE",
          offsets: [{
            lag: 200
          }, {
            lag: 400
          }, {
            lag: 400
          }],
          members: [
            {
              host: "localhost",
              memberId: "member-1",
              clientId: "client-1",
              groupInstanceId: "instance-1",
              assignments: [
                {
                  topicName: "console_datagen_002-a",
                  topicId: "1",
                },
                {
                  topicName: "console_datagen_002-b",
                  topicId: "2",
                },
              ],
            },
          ],
        },
      },
      {
        id: "console-datagen-group-2",
        attributes: {
          state: "STABLE",
          offsets: [{
            lag: 200
          }, {
            lag: 400
          }, {
            lag: 400
          }],
          members: [
            {
              host: "localhost",
              memberId: "member-1",
              clientId: "client-1",
              groupInstanceId: "instance-1",
              assignments: [
                {
                  topicName: "console_datagen_002-a",
                  topicId: "1",
                },
                {
                  topicName: "console_datagen_002-b",
                  topicId: "2",
                },
              ],
            },
          ],
        },
      },
      {
        id: "console-datagen-group-2",
        attributes: {
          state: "STABLE",
          offsets: [{
            lag: 200
          }, {
            lag: 400
          }, {
            lag: 400
          }],
          members: [
            {
              host: "localhost",
              memberId: "member-1",
              clientId: "client-1",
              groupInstanceId: "instance-1",
              assignments: [
                {
                  topicName: "console_datagen_002-a",
                  topicId: "1",
                },
                {
                  topicName: "console_datagen_002-b",
                  topicId: "2",
                },
              ],
            },
          ],
        },
      },
      {
        id: "console-datagen-group-2",
        attributes: {
          state: "STABLE",
          offsets: [{
            lag: 200
          }, {
            lag: 400
          }, {
            lag: 400
          }],
          members: [
            {
              host: "localhost",
              memberId: "member-1",
              clientId: "client-1",
              groupInstanceId: "instance-1",
              assignments: [
                {
                  topicName: "console_datagen_002-a",
                  topicId: "1",
                },
                {
                  topicName: "console_datagen_002-b",
                  topicId: "2",
                },
              ],
            },
          ],
        },
      },
      {
        id: "console-datagen-group-2",
        attributes: {
          state: "STABLE",
          offsets: [{
            lag: 200
          }, {
            lag: 400
          }, {
            lag: 400
          }],
          members: [
            {
              host: "localhost",
              memberId: "member-1",
              clientId: "client-1",
              groupInstanceId: "instance-1",
              assignments: [
                {
                  topicName: "console_datagen_002-a",
                  topicId: "1",
                },
                {
                  topicName: "console_datagen_002-b",
                  topicId: "2",
                },
              ],
            },
          ],
        },
      },
      {
        id: "console-datagen-group-2",
        attributes: {
          state: "STABLE",
          offsets: [{
            lag: 200
          }, {
            lag: 400
          }, {
            lag: 400
          }],
          members: [
            {
              host: "localhost",
              memberId: "member-1",
              clientId: "client-1",
              groupInstanceId: "instance-1",
              assignments: [
                {
                  topicName: "console_datagen_002-a",
                  topicId: "1",
                },
                {
                  topicName: "console_datagen_002-b",
                  topicId: "2",
                },
              ],
            },
          ],
        },
      },
      {
        id: "console-datagen-group-2",
        attributes: {
          state: "EMPTY",
          offsets: [{
            lag: 200
          }, {
            lag: 400
          }, {
            lag: 400
          }],
          members: [
            {
              host: "localhost",
              memberId: "member-1",
              clientId: "client-1",
              groupInstanceId: "instance-1",
              assignments: [
                {
                  topicName: "console_datagen_002-a",
                  topicId: "1",
                },
                {
                  topicName: "console_datagen_002-b",
                  topicId: "2",
                },
              ],
            },
          ],
        },
      },
    ]
  }
};
