import { ButtonLink } from "@/components/Navigation/ButtonLink";
import {
  Button,
  EmptyState,
  EmptyStateActions,
  EmptyStateBody,
  EmptyStateFooter,
  EmptyStateHeader,
  EmptyStateIcon,
} from "@/libs/patternfly/react-core";
import { PlusCircleIcon } from "@/libs/patternfly/react-icons";
import { useTranslations } from "next-intl";

export function EmptyStateNoTopics({
  canCreate,
  createHref,
  onShowHiddenTopics,
}: {
  canCreate: boolean;
  createHref: string;
  onShowHiddenTopics: () => void;
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
      <EmptyStateFooter>
        <EmptyStateActions>
          {canCreate && (
            <ButtonLink variant="primary" href={createHref}>
              {t("EmptyStateNoTopics.create_a_topic")}
            </ButtonLink>
          )}
          <Button variant="secondary" onClick={onShowHiddenTopics}>
            {t("EmptyStateNoTopics.show_internal_topics")}
          </Button>
        </EmptyStateActions>
      </EmptyStateFooter>
    </EmptyState>
  );
}
