import { getKafkaCluster } from "@/api/kafka/actions";
import { getTopic } from "@/api/topics/actions";
import { KafkaTopicParams } from "@/app/[locale]/kafka/[kafkaId]/topics/kafkaTopic.params";
import { AppHeader } from "@/components/AppHeader";
import { NavItemLink } from "@/components/NavItemLink";
import { Number } from "@/components/Number";
import {
  Label,
  Nav,
  NavList,
  PageNavigation,
  Spinner,
} from "@/libs/patternfly/react-core";
import { Skeleton } from "@patternfly/react-core";
import { notFound } from "next/navigation";
import { ReactNode, Suspense } from "react";

export const fetchCache = "force-cache";

export function TopicHeader({
  params: { kafkaId, topicId },
}: {
  params: KafkaTopicParams;
}) {
  const portal = <div key={"topic-header-portal"} id={"topic-header-portal"} />;
  return (
    <Suspense
      fallback={
        <AppHeader
          title={<Skeleton width="35%" />}
          navigation={
            <PageNavigation>
              <Nav aria-label="Group section navigation" variant="tertiary">
                <NavList>
                  <NavItemLink
                    url={`/kafka/${kafkaId}/topics/${topicId}/messages`}
                  >
                    Messages&nbsp;
                    <Label isCompact={true}>
                      <Spinner size="sm" />
                    </Label>
                  </NavItemLink>
                  <NavItemLink
                    url={`/kafka/${kafkaId}/topics/${topicId}/partitions`}
                  >
                    Partitions&nbsp;
                    <Label isCompact={true}>
                      <Spinner size="sm" />
                    </Label>
                  </NavItemLink>
                  <NavItemLink
                    url={`/kafka/${kafkaId}/topics/${topicId}/consumer-groups`}
                  >
                    Consumer groups&nbsp;
                    <Label isCompact={true}>
                      <Spinner size="sm" />
                    </Label>
                  </NavItemLink>
                  {/*
                  <NavItemLink
                    url={`/kafka/${kafkaId}/topics/${topicId}/schema-registry`}
                  >
                    Schema
                  </NavItemLink>
*/}
                  <NavItemLink
                    url={`/kafka/${kafkaId}/topics/${topicId}/configuration`}
                  >
                    Configuration
                  </NavItemLink>
                </NavList>
              </Nav>
            </PageNavigation>
          }
          actions={[portal]}
        />
      }
    >
      <ConnectedTopicHeader params={{ kafkaId, topicId }} portal={portal} />
    </Suspense>
  );
}

async function ConnectedTopicHeader({
  params: { kafkaId, topicId },
  portal,
}: {
  params: KafkaTopicParams;
  portal: ReactNode;
}) {
  const cluster = await getKafkaCluster(kafkaId);
  if (!cluster) {
    notFound();
  }
  const topic = await getTopic(cluster.id, topicId);
  return (
    <AppHeader
      title={topic.attributes.name}
      navigation={
        <PageNavigation>
          <Nav aria-label="Group section navigation" variant="tertiary">
            <NavList>
              <NavItemLink url={`/kafka/${kafkaId}/topics/${topicId}/messages`}>
                Messages&nbsp;
              </NavItemLink>
              <NavItemLink
                url={`/kafka/${kafkaId}/topics/${topicId}/partitions`}
              >
                Partitions&nbsp;
                <Label isCompact={true}>
                  <Suspense fallback={<Spinner size="sm" />}>
                    <Number value={topic.attributes.numPartitions} />
                  </Suspense>
                </Label>
              </NavItemLink>
              <NavItemLink
                url={`/kafka/${kafkaId}/topics/${topicId}/consumer-groups`}
              >
                Consumer groups&nbsp;
                <Label isCompact={true}>
                  <Number
                    value={topic.relationships.consumerGroups.data.length}
                  />
                </Label>
              </NavItemLink>
              {/*
              <NavItemLink
                url={`/kafka/${kafkaId}/topics/${topicId}/schema-registry`}
              >
                Schema
              </NavItemLink>
              */}
              <NavItemLink
                url={`/kafka/${kafkaId}/topics/${topicId}/configuration`}
              >
                Configuration
              </NavItemLink>
            </NavList>
          </Nav>
        </PageNavigation>
      }
      actions={[portal]}
    />
  );
}
