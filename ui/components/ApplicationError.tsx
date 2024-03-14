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
  const t = useTranslations();
  return (
    <EmptyState variant={"lg"}>
      <EmptyStateHeader
        titleText={t("ApplicationError.title")}
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
          <Text>{t("ApplicationError.body")}</Text>
        </TextContent>
      </EmptyStateBody>

      <EmptyStateFooter>
        <EmptyStateActions>
          <Button variant="primary" onClick={onReset}>
            {t("ApplicationError.retry")}
          </Button>
        </EmptyStateActions>
      </EmptyStateFooter>
      <EmptyStateFooter>
        <ExpandableSection
          initialExpanded={false}
          toggleText={t("ApplicationError.error_details")}
        >
          {error.digest && (
            <Title headingLevel={"h2"} className={"pf-v5-u-mb-lg"}>
              {error.digest}
            </Title>
          )}
          <DescriptionList>
            <DescriptionListGroup>
              <DescriptionListTerm>
                {t("ApplicationError.error")}
              </DescriptionListTerm>
              <DescriptionListDescription>
                {error.name}
              </DescriptionListDescription>
              <DescriptionListTerm>
                {t("ApplicationError.message")}
              </DescriptionListTerm>
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
