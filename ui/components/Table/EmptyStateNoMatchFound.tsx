import {
  Button,
  EmptyState,
  EmptyStateActions,
  EmptyStateBody,
  EmptyStateFooter,
  EmptyStateHeader,
  EmptyStateIcon,
} from "@/libs/patternfly/react-core";
import { SearchIcon } from "@/libs/patternfly/react-icons";

export function EmptyStateNoMatchFound({ onClear }: { onClear: () => void }) {
  return (
    <EmptyState>
      <EmptyStateHeader
        titleText="No results found"
        headingLevel="h4"
        icon={<EmptyStateIcon icon={SearchIcon} />}
      />
      <EmptyStateBody>
        No results match the filter criteria. Clear all filters and try again.
      </EmptyStateBody>
      <EmptyStateFooter>
        <EmptyStateActions>
          <Button variant="link" onClick={onClear}>
            Clear all filters
          </Button>
        </EmptyStateActions>
      </EmptyStateFooter>
    </EmptyState>
  );
}
