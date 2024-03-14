import { ButtonLink } from "@/components/Navigation/ButtonLink";
import {
  EmptyState,
  EmptyStateActions,
  EmptyStateBody,
  EmptyStateFooter,
  EmptyStateHeader,
  EmptyStateIcon,
} from "@/libs/patternfly/react-core";
import { PlusCircleIcon } from "@patternfly/react-icons";
import { useTranslations } from "next-intl";

export function EmptyStateNoTopics({
  canCreate,
  createHref,
}: {
  canCreate: boolean;
  createHref: string;
}) {
  const t = useTranslations();
  return (
    <EmptyState>
      <EmptyStateHeader
        titleText="No topics"
        headingLevel="h4"
        icon={<EmptyStateIcon icon={PlusCircleIcon} />}
      />
      <EmptyStateBody>
        {t("EmptyStateNoTopics.to_get_started_create_your_first_topic")}{" "}
      </EmptyStateBody>
      {canCreate && (
        <EmptyStateFooter>
          <EmptyStateActions>
            <ButtonLink variant="primary" href={createHref}>
              {t("EmptyStateNoTopics.create_a_topic")}
            </ButtonLink>
          </EmptyStateActions>
        </EmptyStateFooter>
      )}
    </EmptyState>
  );
}
