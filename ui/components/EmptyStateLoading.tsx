import {
  EmptyState,
  EmptyStateHeader,
  EmptyStateIcon,
  Spinner,
} from "@/libs/patternfly/react-core";

export function EmptyStateLoading() {
  return (
    <EmptyState>
      <EmptyStateHeader
        titleText={"Loading"}
        headingLevel="h4"
        icon={<EmptyStateIcon icon={Spinner} />}
      />
    </EmptyState>
  );
}
