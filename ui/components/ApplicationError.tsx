import { ExpandableSection } from "@/components/ExpandableSection";
import {
  Button,
  DescriptionList,
  DescriptionListDescription,
  DescriptionListGroup,
  DescriptionListTerm,
  EmptyState,
  EmptyStateActions,
  EmptyStateBody,
  EmptyStateFooter,
  EmptyStateHeader,
  EmptyStateIcon,
  Text,
  TextContent,
  Title,
} from "@/libs/patternfly/react-core";
import { ExclamationCircleIcon } from "@/libs/patternfly/react-icons";
import { global_danger_color_100 } from "@/libs/patternfly/react-tokens";
import { useTranslations } from "next-intl";

export function ApplicationError({
  error,
  onReset,
}: {
  error: Error & { digest?: string };
  onReset: () => void;
}) {
  return (
    <EmptyState variant={"lg"}>
      <EmptyStateHeader
        titleText={"This page is temporarily unavailable"}
        headingLevel="h1"
        icon={
          <EmptyStateIcon
            icon={ExclamationCircleIcon}
            color={global_danger_color_100.var}
          />
        }
      />
      <EmptyStateBody>
        <TextContent>
          <Text>
            Try clicking the button below, or refreshing the page. If the
            problem persists, contact your organization administrator.
          </Text>
        </TextContent>
      </EmptyStateBody>

      <EmptyStateFooter>
        <EmptyStateActions>
          <Button variant="primary" onClick={onReset}>
            Retry
          </Button>
        </EmptyStateActions>
      </EmptyStateFooter>
      <EmptyStateFooter>
        <ExpandableSection initialExpanded={false} toggleText={"Show more"}>
          {error.digest && (
            <Title headingLevel={"h2"} className={"pf-v5-u-mb-lg"}>
              {error.digest}
            </Title>
          )}
          <DescriptionList>
            <DescriptionListGroup>
              <DescriptionListTerm>Error</DescriptionListTerm>
              <DescriptionListDescription>
                {error.name}
              </DescriptionListDescription>
              <DescriptionListTerm>Message</DescriptionListTerm>
              <DescriptionListDescription>
                {error.message}
              </DescriptionListDescription>
            </DescriptionListGroup>
          </DescriptionList>
        </ExpandableSection>
      </EmptyStateFooter>
    </EmptyState>
  );
}
