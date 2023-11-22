import { ButtonLink } from "@/components/ButtonLink";
import {
  EmptyState,
  EmptyStateActions,
  EmptyStateBody,
  EmptyStateFooter,
  EmptyStateHeader,
  EmptyStateIcon,
} from "@/libs/patternfly/react-core";
import { PlusCircleIcon } from "@patternfly/react-icons";

export function EmptyStateNoTopics({ createHref }: { createHref: string }) {
  return (
    <EmptyState>
      <EmptyStateHeader
        titleText="No topics"
        headingLevel="h4"
        icon={<EmptyStateIcon icon={PlusCircleIcon} />}
      />
      <EmptyStateBody>To get started, create your first topic </EmptyStateBody>
      <EmptyStateFooter>
        <EmptyStateActions>
          <ButtonLink variant="primary" href={createHref}>
            Create a topic
          </ButtonLink>
        </EmptyStateActions>
      </EmptyStateFooter>
    </EmptyState>
  );
}
