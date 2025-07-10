import {
  Drawer,
  DrawerContent,
  DrawerContentBody,
} from "@patternfly/react-core";
import type { Meta, StoryObj } from "@storybook/nextjs";
import { fn } from "storybook/test";
import { MessageDetails } from "./MessageDetails";

export default {
  component: MessageDetails,
  args: {
    message: {
      attributes: {
        key: "this-is-a-very-long-key-that-might-cause-some-trouble-figuring-out-column-widths",
        partition: 4,
        offset: 16,
        size: 1234,
        timestamp: new Date(
          Date.UTC(2024, 11, 31, 23, 59, 59, 999),
        ).toISOString(),
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
      relationships: {
        keySchema: null,
        valueSchema: {
          meta: {
            artifactType: "AVRO",
            name: "com.example.price",
          },
          data: {
            type: "schemas",
            id: "eyJnbG9iYWxJZCI6MX0=",
          },
          links: {
            content:
              "/api/registries/my-apicurio-registry/schemas/eyJnbG9iYWxJZCI6MX0=",
          },
        },
      },
    },
    defaultTab: "value",
    onClose: fn(),
  },
  decorators: (Story) => (
    <Drawer isExpanded={true}>
      <DrawerContent panelContent={Story()}>
        <DrawerContentBody />
      </DrawerContent>
    </Drawer>
  ),
} as Meta<typeof MessageDetails>;

type Story = StoryObj<typeof MessageDetails>;

export const Example: Story = {};

export const StartWithHeaders: Story = {
  args: {
    defaultTab: "headers",
  },
};

export const StartWithKey: Story = {
  args: {
    defaultTab: "key",
  },
};
