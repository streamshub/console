import { ViewedTopic } from "@/api/topics/actions";
import { ExpandableCard } from "@/components/ExpandableCard";
import { ExternalLink } from "@/components/Navigation/ExternalLink";
import {
  CardBody,
  EmptyState,
  EmptyStateActions,
  EmptyStateBody,
  EmptyStateFooter,
  EmptyStateHeader,
  Text,
  TextContent,
  Tooltip,
} from "@/libs/patternfly/react-core";
import { HelpIcon } from "@/libs/patternfly/react-icons";
import config from '@/utils/config';
import { useTranslations } from "next-intl";
import { TopicsTable } from "./components/TopicsTable";

export async function RecentTopicsCard({
  viewedTopics,
  isLoading,
}: {
  viewedTopics: ViewedTopic[];
  isLoading: boolean;
}) {
  const t = useTranslations();
  const productName = t("common.product");
  const showLearning = await config().then(cfg => cfg.showLearning);

  return (
    <ExpandableCard
      title={
        <TextContent>
          <b>
            {t("homepage.recently_viewed_topics_header")}{" "}
            <Tooltip
              content={t("homepage.recently_viewed_topics_header_popover", {
                product: productName,
              })}
            >
              <HelpIcon />
            </Tooltip>
          </b>
          <Text component={"small"}>
            {t("homepage.last_accessed_topics", {
              product: productName,
            })}
          </Text>
        </TextContent>
      }
      isCompact={true}
    >
      <CardBody>
        {isLoading ? (
          <>TODO</>
        ) : viewedTopics.length > 0 ? (
          <TopicsTable topics={viewedTopics} />
        ) : (
          <EmptyState variant={"xs"}>
            <EmptyStateHeader title={t("homepage.topics_empty_state_header")} />
            <EmptyStateBody>
              {t("homepage.empty_topics_description", { product: productName })}
            </EmptyStateBody>
            {showLearning && (
              <EmptyStateFooter>
                <EmptyStateActions className={"pf-v5-u-font-size-sm"}>
                  <ExternalLink
                    testId={"recent-topics-empty-state-link"}
                    href={t("learning.links.topicOperatorUse")}
                  >
                    {t("learning.labels.topicOperatorUse")}
                  </ExternalLink>
                </EmptyStateActions>
              </EmptyStateFooter>
            )}
          </EmptyState>
        )}
      </CardBody>
    </ExpandableCard>
  );
}
