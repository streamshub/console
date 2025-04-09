import { getTopic } from "@/api/topics/actions";
import { KafkaTopicParams } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/topics/kafkaTopic.params";
import { AppHeader } from "@/components/AppHeader";
import { Number } from "@/components/Format/Number";
import { ManagedTopicLabel } from "@/components/ManagedTopicLabel";
import { NavTabLink } from "@/components/Navigation/NavTabLink";
import {
  Label,
  Nav,
  NavList,
  PageSection,
  Skeleton,
  Spinner,
} from "@/libs/patternfly/react-core";
import { useTranslations } from "next-intl";
import { ReactNode, Suspense } from "react";

export type TopicHeaderProps = {
  params: KafkaTopicParams;
  showRefresh?: boolean;
};

export function TopicHeader({
  params: { kafkaId, topicId },
  showRefresh,
}: TopicHeaderProps) {
  const t = useTranslations("topic-header");

  const tabs = [
    {
      key: 0,
      title: (
        <>
          {t("messages")}&nbsp;
          <Label isCompact={true}>
            <Spinner size="sm" />
          </Label>
        </>
      ),
      url: `/kafka/${kafkaId}/topics/${topicId}/messages`,
    },
    {
      key: 1,
      title: (
        <>
          {t("partitions")}&nbsp;
          <Label isCompact={true}>
            <Spinner size="sm" />
          </Label>
        </>
      ),
      url: `/kafka/${kafkaId}/topics/${topicId}/partitions`,
    },
    {
      key: 2,
      title: (
        <>
          {t("consumer_groups")}&nbsp;
          <Label isCompact={true}>
            <Spinner size="sm" />
          </Label>
        </>
      ),
      url: `/kafka/${kafkaId}/topics/${topicId}/consumer-groups`,
    },
    {
      key: 3,
      title: t("configuration"),
      url: `/kafka/${kafkaId}/topics/${topicId}/configuration`,
    },
  ];

  const portal = <div key={"topic-header-portal"} id={"topic-header-portal"} />;
  return (
    <Suspense
      fallback={
        <AppHeader
          title={<Skeleton width="35%" />}
          showRefresh={showRefresh}
          navigation={
            <PageSection className="pf-v6-u-px-sm" type="subnav">
              <NavTabLink tabs={tabs} />
            </PageSection>
          }
          actions={[portal]}
        />
      }
    >
      <ConnectedTopicHeader
        params={{ kafkaId, topicId }}
        portal={portal}
        showRefresh={showRefresh}
      />
    </Suspense>
  );
}

async function ConnectedTopicHeader({
  params: { kafkaId, topicId },
  showRefresh,
  portal,
}: {
  params: KafkaTopicParams;
  showRefresh?: boolean;
  portal: ReactNode;
}) {
  const t = useTranslations("topic-header");

  const response = await getTopic(kafkaId, topicId);

  if (response.errors) {
    return <AppHeader title={`Topic ${topicId}`} />;
  }

  const topic = response.payload;

  const tabs = [
    {
      key: 0,
      title: t("messages"),
      url: `/kafka/${kafkaId}/topics/${topicId}/messages`,
    },
    {
      key: 1,
      title: (
        <>
          {t("partitions")}&nbsp;
          <Label isCompact={true}>
            <Suspense fallback={<Spinner size="sm" />}>
              <Number value={topic?.attributes.numPartitions} />
            </Suspense>
          </Label>
        </>
      ),
      url: `/kafka/${kafkaId}/topics/${topicId}/partitions`,
    },
    {
      key: 2,
      title: (
        <>
          {t("consumer_groups")}&nbsp;
          <Label isCompact={true}>
            <Number
              value={topic?.relationships.consumerGroups?.data.length ?? 0}
            />
          </Label>
        </>
      ),
      url: `/kafka/${kafkaId}/topics/${topicId}/consumer-groups`,
    },
    {
      key: 3,
      title: t("configuration"),
      url: `/kafka/${kafkaId}/topics/${topicId}/configuration`,
    },
  ];

  return (
    <AppHeader
      title={
        <>
          {topic?.attributes.name}
          {topic?.meta?.managed === true && <ManagedTopicLabel />}
        </>
      }
      showRefresh={showRefresh}
      navigation={
        <PageSection className="pf-v6-u-px-sm" type="subnav">
          <NavTabLink tabs={tabs} />
        </PageSection>
      }
      actions={[portal]}
    />
  );
}
