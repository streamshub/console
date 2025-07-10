import { Meta, StoryObj } from "@storybook/nextjs";
import { FieldReplicas } from "./FieldReplicas";;

const meta: Meta<typeof FieldReplicas> = {
  component: FieldReplicas,
};

export default meta;
type Story = StoryObj<typeof FieldReplicas>;


export const Default: Story = {
  args: {
    replicas: 3,
    maxReplicas: 5,
    showErrors: false,
    backendError: false,
  }
}

export const InvalidState: Story = {
  args: {
    replicas: 6,
    maxReplicas: 5,
    showErrors: true,
    backendError: "Exceeded maximum replicas",
  }
}

