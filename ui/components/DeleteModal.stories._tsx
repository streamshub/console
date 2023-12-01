import { Alert } from "@patternfly/react-core";
import type { PlayFunction } from "@storybook/csf";
import type { ComponentMeta, ComponentStory } from "@storybook/react";
import type { ReactFramework } from "@storybook/react/types-6-0";
import { userEvent, within } from "@storybook/testing-library";
import { useMachine } from "@xstate/react";
import { createMachine } from "xstate";
import type { DeleteModalProps } from "./DeleteModal";
import { DeleteModal, DeleteModalConfirmation } from "./DeleteModal";

export default {
  component: DeleteModal,
  subcomponents: { DeleteModalConfirmation },
  args: {
    title: "Delete?",
    children: "You are deleting something.",
    disableFocusTrap: true,
  },
  argTypes: {
    ouiaId: { table: { category: "Tracking" } },
    appendTo: { table: { category: "Functional" } },
    onDelete: { table: { category: "Events" } },
    onCancel: { table: { category: "Events" } },
    children: { table: { category: "Appearance" } },
    disableFocusTrap: { table: { category: "Development" } },
    confirmationValue: { table: { category: "Appearance" } },
    isModalOpen: {
      control: {
        type: null,
      },
      table: { category: "States" },
    },
    isDeleting: {
      control: {
        type: null,
      },
      table: { category: "States" },
    },
  },
} as ComponentMeta<typeof DeleteModal>;

const onDelete = [
  {
    cond: (context: { isAsync: boolean }) => context.isAsync,
    target: "deleting",
  },
  {
    cond: (context: { willFail: boolean }) => context.willFail,
    target: "withError",
  },
  { target: "closed" },
];
const makeModalStoryMachine = ({
  id,
  initial = "open",
  isAsync,
  willFail,
}: {
  id: string;
  initial?: string;
  isAsync: boolean;
  willFail: boolean;
}) =>
  createMachine({
    initial,
    id,
    context: {
      isAsync,
      willFail,
    },
    states: {
      closed: {
        on: {
          OPEN: "open",
        },
      },
      open: {
        on: {
          DELETE: onDelete,
        },
      },
      deleting: {
        after: {
          1000: [
            { cond: (context) => context.willFail, target: "withError" },
            { cond: () => initial !== "deleting", target: "closed" },
          ],
        },
        on: {
          CLOSE: "deleting",
        },
      },
      withError: {
        on: {
          DELETE: onDelete,
        },
      },
    },
    on: {
      CLOSE: "closed",
    },
  });

const Template: ComponentStory<typeof DeleteModal> = (
  { onDelete: onDeleteAction, onCancel: onCancelAction, children, ...args },
  { id, parameters }
) => {
  const [state, send] = useMachine(
    () =>
      makeModalStoryMachine({
        id,
        // eslint-disable-next-line @typescript-eslint/no-unsafe-assignment
        initial: parameters.initialState,
        // eslint-disable-next-line @typescript-eslint/no-unsafe-assignment
        isAsync: parameters.isDeleteAsync,
        // eslint-disable-next-line @typescript-eslint/no-unsafe-assignment
        willFail: parameters.willDeleteFail,
      }),
    { devTools: true }
  );

  const onDelete = () => {
    onDeleteAction && onDeleteAction();
    send("DELETE");
  };
  const onCancel = () => {
    onCancelAction && onCancelAction();
    send("CLOSE");
  };

  return (
    <div style={{ minHeight: 500, height: "100%" }}>
      <DeleteModal
        {...args}
        isDeleting={state.value === "deleting"}
        isModalOpen={state.value !== "closed"}
        appendTo={() => {
          return (
            document.getElementById(`story--${id}`) ||
            document.getElementById("root") ||
            document.body
          );
        }}
        onCancel={onCancel}
        onDelete={onDelete}
      >
        {state.value === "withError" && (
          <Alert variant="danger" title="Danger alert title" isInline>
            <p>This should tell the user more information about the alert.</p>
          </Alert>
        )}
        {children}
      </DeleteModal>
      <button onClick={() => send("OPEN")}>Open modal</button>
    </div>
  );
};

const fillConfirmation: PlayFunction<
  ReactFramework,
  DeleteModalProps
> = async ({ canvasElement, args }) => {
  const story = within(canvasElement);
  const confirmationValue = args.confirmationValue || "digit this";
  await userEvent.type(
    await story.findByLabelText(`Type ${confirmationValue} to confirm`),
    confirmationValue
  );
  await userEvent.click(await story.findByText("Delete"));
};

export const SynchronousDelete = Template.bind({});

export const SynchronousDeleteWithError = Template.bind({});
SynchronousDeleteWithError.args = SynchronousDelete.args;
SynchronousDeleteWithError.parameters = {
  willDeleteFail: true,
  initialState: "withError",
  docs: {
    description: {
      story: `If the delete action fails, it's possible to show an inline error.
The user can then try again or cancel the action. In this demo the delete will
always fail.
      `,
    },
  },
};

export const SynchronousDeleteWithConfirmation = Template.bind({});
SynchronousDeleteWithConfirmation.args = {
  confirmationValue: "digit this",
  ...SynchronousDelete.args,
};
SynchronousDeleteWithConfirmation.parameters = {
  docs: {
    description: {
      story: `It is possible to ask the user to type something to enable the
disable button. In this demo you should be typing \`digit this\`.
      `,
    },
  },
};
SynchronousDeleteWithConfirmation.play = fillConfirmation;

export const SynchronousDeleteWithConfirmationAndError = Template.bind({});
SynchronousDeleteWithConfirmationAndError.args =
  SynchronousDeleteWithConfirmation.args;
SynchronousDeleteWithConfirmationAndError.parameters = {
  willDeleteFail: true,
  initialState: "withError",

  docs: {
    description: {
      story: `It is possible to ask the user to type something to enable the
disable button. In this demo you should be typing \`digit this\`.

If the delete action fails, it's possible to show an inline error. The user can
then try again or cancel the action. In this demo the delete will always fail.
      `,
    },
  },
};

export const AsynchronousDelete = Template.bind({});
AsynchronousDelete.args = SynchronousDelete.args;
AsynchronousDelete.parameters = {
  isDeleteAsync: true,
  initialState: "deleting",
  docs: {
    description: {
      story: `For asyncronous deletes, after the user clicks Delete all the
buttons gets disabled, the X button to close the modal is removed, and the
Delete button shows a spinner to indicate that the delete process is
ongoing.`,
    },
  },
};

export const AsynchronousDeleteWithError = Template.bind({});
AsynchronousDeleteWithError.args = SynchronousDeleteWithError.args;
AsynchronousDeleteWithError.parameters = {
  isDeleteAsync: true,
  willDeleteFail: true,
  initialState: "withError",

  docs: {
    description: {
      story: `For asyncronous deletes, after the user clicks Delete all the
buttons gets disabled, the X button to close the modal is removed, and the
Delete button shows a spinner to indicate that the delete process is
ongoing.

If the delete action fails, it's possible to show an inline error. The user can
then try again or cancel the action. In this demo the delete will always fail.
      `,
    },
  },
};

export const AsynchronousDeleteWithConfirmation = Template.bind({});
AsynchronousDeleteWithConfirmation.args =
  SynchronousDeleteWithConfirmation.args;
AsynchronousDeleteWithConfirmation.parameters = {
  isDeleteAsync: true,
  docs: {
    description: {
      story: `It is possible to ask the user to type something to enable the
disable button. In this demo you should be typing \`digit this\`.
      `,
    },
  },
};
AsynchronousDeleteWithConfirmation.play = fillConfirmation;

export const AsynchronousDeleteWithConfirmationAndError = Template.bind({});
AsynchronousDeleteWithConfirmationAndError.args =
  SynchronousDeleteWithConfirmationAndError.args;
AsynchronousDeleteWithConfirmationAndError.parameters = {
  isDeleteAsync: true,
  willDeleteFail: true,
  initialState: "withError",

  docs: {
    description: {
      story: `It is possible to ask the user to type something to enable the
disable button. In this demo you should be typing \`digit this\`.

If the delete action fails, it's possible to show an inline error.
The user can then try again or cancel the action. In this demo the delete will
always fail.

In this example a single wrapper will be applied.
      `,
    },
  },
};

export const CustomConfirmationPlacement = Template.bind({});
CustomConfirmationPlacement.args = {
  children: (
    <div>
      <p>You are deleting something.</p>
      <p>⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️</p>
      <DeleteModalConfirmation requiredConfirmationValue="digit this" />
      <p>⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️</p>
      <p>This goes after the confirmation</p>
    </div>
  ),
};
CustomConfirmationPlacement.parameters = {
  docs: {
    description: {
      story: `If needed it's possible to opt-out of the default placement of the
confirmation field using the \`DeleteModalConfirmation\` component
directly.

Each node in the children property gets automatically wrapped in a ModalBoxBody
component to apply proper spacing.
      `,
    },
  },
};
CustomConfirmationPlacement.play = fillConfirmation;

export const CustomConfirmationPlacementWithAutomaticSpacing = Template.bind(
  {}
);
CustomConfirmationPlacementWithAutomaticSpacing.args = {
  children: [
    <p key={0}>You are deleting something.</p>,
    <p key={1}>⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️</p>,
    <DeleteModalConfirmation key={2} requiredConfirmationValue="digit this" />,
    <p key={3}>⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️</p>,
    <p key={4}>This goes after the confirmation</p>,
  ],
};
CustomConfirmationPlacementWithAutomaticSpacing.parameters = {
  docs: {
    description: {
      story: `In this example a wrapper around each node will be applied since
an array of elements is being passed to the \`children\` property.`,
    },
  },
};
CustomConfirmationPlacementWithAutomaticSpacing.play = fillConfirmation;

export const AsynchronousCustomConfirmationPlacement = Template.bind({});
AsynchronousCustomConfirmationPlacement.args = CustomConfirmationPlacement.args;
AsynchronousCustomConfirmationPlacement.parameters = {
  isDeleteAsync: true,
};
AsynchronousCustomConfirmationPlacement.play = fillConfirmation;

export const AsynchronousCustomConfirmationPlacementAndError = Template.bind(
  {}
);
AsynchronousCustomConfirmationPlacementAndError.args =
  CustomConfirmationPlacement.args;
AsynchronousCustomConfirmationPlacementAndError.parameters = {
  isDeleteAsync: true,
  willDeleteFail: true,
  initialState: "withError",
};
