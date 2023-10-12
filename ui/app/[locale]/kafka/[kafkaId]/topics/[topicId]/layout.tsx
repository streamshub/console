import { getKafkaCluster, getKafkaClusters } from "@/api/kafka";
import { getTopic } from "@/api/topics";
import { KafkaTopicParams } from "@/app/[locale]/kafka/[kafkaId]/topics/kafkaTopic.params";
import { BreadcrumbLink } from "@/components/BreadcrumbLink";
import { Loading } from "@/components/Loading";
import { NavItemLink } from "@/components/NavItemLink";
import { Number } from "@/components/Number";
import {
  Breadcrumb,
  BreadcrumbItem,
  Divider,
  Label,
  Nav,
  NavList,
  PageBreadcrumb,
  PageGroup,
  PageNavigation,
  PageSection,
  Title,
} from "@/libs/patternfly/react-core";
import { notFound } from "next/navigation";
import { PropsWithChildren, Suspense } from "react";
import { KafkaBreadcrumbItem } from "./KafkaBreadcrumbItem";

export default async function KafkaLayout({
  children,
  params: { kafkaId, topicId: topicId },
}: PropsWithChildren<{ params: KafkaTopicParams }>) {
  const cluster = await getKafkaCluster(kafkaId);
  if (!cluster) {
    notFound();
  }
  const clusters = await getKafkaClusters();
  const topic = await getTopic(cluster.id, topicId);

  return (
    <>
      <PageGroup>
        <PageBreadcrumb>
          <Breadcrumb>
            <BreadcrumbLink href="/kafka">Kafka</BreadcrumbLink>
            <BreadcrumbItem>
              <KafkaBreadcrumbItem selected={cluster} clusters={clusters} />
            </BreadcrumbItem>
            <BreadcrumbLink href={`/kafka/${kafkaId}/topics`}>
              Topics
            </BreadcrumbLink>
            <BreadcrumbItem isActive={true}>
              {topic.attributes.name}
            </BreadcrumbItem>
          </Breadcrumb>
        </PageBreadcrumb>
        <PageSection
          variant={"light"}
          padding={{ default: "noPadding" }}
          className={"pf-v5-u-px-lg pf-v5-u-pt-sm"}
        >
          <Title headingLevel={"h1"} className={"pf-v5-u-pb-sm"}>
            {topic.attributes.name}
          </Title>
        </PageSection>
        <PageNavigation>
          <Nav aria-label="Group section navigation" variant="tertiary">
            <NavList>
              <NavItemLink url={`/kafka/${kafkaId}/topics/${topicId}/messages`}>
                Messages&nbsp;
                <Label isCompact={true}>
                  <Number value={topic.attributes.recordCount} />
                </Label>
              </NavItemLink>
              <NavItemLink
                url={`/kafka/${kafkaId}/topics/${topicId}/consumer-groups`}
              >
                Consumer groups&nbsp;
                <Label isCompact={true}>
                  <Number value={0} />
                </Label>
              </NavItemLink>
              <NavItemLink
                url={`/kafka/${kafkaId}/topics/${topicId}/partitions`}
              >
                Partitions&nbsp;
                <Label isCompact={true}>
                  <Number value={topic.attributes.partitions.length} />
                </Label>
              </NavItemLink>
              <NavItemLink url={`/kafka/${kafkaId}/topics/${topicId}/schema`}>
                Schema
              </NavItemLink>
              <NavItemLink
                url={`/kafka/${kafkaId}/topics/${topicId}/configuration`}
              >
                Configuration
              </NavItemLink>
            </NavList>
          </Nav>
        </PageNavigation>
      </PageGroup>
      <Divider />
      <Suspense fallback={<Loading />}>{children}</Suspense>
    </>
  );
}
