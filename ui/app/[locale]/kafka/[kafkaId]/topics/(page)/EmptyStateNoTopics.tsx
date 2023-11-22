"use client";
import { ButtonLink } from "@/components/ButtonLink";
import { useHelp } from "@/components/Quickstarts/HelpContainer";
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
  const openHelp = useHelp();
  return (
    <EmptyState>
      <EmptyStateHeader
        titleText="No topics"
        headingLevel="h4"
        icon={<EmptyStateIcon icon={PlusCircleIcon} />}
      />
      <EmptyStateBody>
        For help getting started, access the{" "}
        <a
          onClick={() => {
            openHelp("connect-to-cluster"); // TODO: change this
          }}
        >
          quick start guide
        </a>
      </EmptyStateBody>
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
