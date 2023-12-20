"use client";
import {
  Button,
  EmptyState,
  EmptyStateBody,
  EmptyStateIcon,
  Title,
} from "@/libs/patternfly/react-core";
import { SearchIcon } from "@/libs/patternfly/react-icons";

export function NoResultsEmptyState({ onReset }: { onReset: () => void }) {
  return (
    <EmptyState variant={"lg"}>
      <EmptyStateIcon icon={SearchIcon} />
      <Title headingLevel="h4" size="lg">
        No results
      </Title>
      <EmptyStateBody>No partitions matching the filter.</EmptyStateBody>
      <Button variant={"link"} onClick={onReset}>
        Reset filters
      </Button>
    </EmptyState>
  );
}
