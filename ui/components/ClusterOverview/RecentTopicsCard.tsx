import { ViewedTopic } from "@/api/topics/actions";
import { ExpandableCard } from "@/components/ExpandableCard";
import { ExternalLink } from "@/components/Navigation/ExternalLink";
import {
  CardBody,
  EmptyState,
  EmptyStateActions,
  EmptyStateBody,
  EmptyStateFooter,
  Content,
  Tooltip,
} from "@/libs/patternfly/react-core";
import { HelpIcon } from "@/libs/patternfly/react-icons";
import { clientConfig as config } from "@/utils/config";
import { getTranslations } from "next-intl/server";
import { TopicsTable } from "./components/TopicsTable";

export async function RecentTopicsCard({
  viewedTopics,
  isLoading,
}: {
  viewedTopics: ViewedTopic[];
  isLoading: boolean;
}) {
  const t = await getTranslations();
  const productName = t("common.product");
  const showLearning = await config().then((cfg) => cfg.showLearning);

  return (
    <ExpandableCard
      title={
        <Content>
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
          <Content component={"small"}>
            {t("homepage.last_accessed_topics", {
              product: productName,
            })}
          </Content>
        </Content>
      }
      isCompact={true}
    >
      <CardBody>
        {isLoading ? (
          <>TODO</>
        ) : viewedTopics.length > 0 ? (
          <TopicsTable topics={viewedTopics} />
        ) : (
          <EmptyState
            variant={"xs"}
            title={t("homepage.topics_empty_state_header")}
          >
            <EmptyStateBody>
              {t("homepage.empty_topics_description", { product: productName })}
            </EmptyStateBody>
            {showLearning && (
              <EmptyStateFooter>
                <EmptyStateActions className={"pf-v6-u-font-size-sm"}>
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
