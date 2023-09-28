import { getResource, getResources } from "@/api/resources";
import { getTopic } from "@/api/topics";
import { KafkaSelectorBreadcrumbItem } from "@/app/[locale]/kafka/[kafkaId]/(root)/KafkaSelectorBreadcrumbItem";
import { BreadcrumbLink } from "@/components/BreadcrumbLink";
import { Loading } from "@/components/Loading";
import { NavItemLink } from "@/components/NavItemLink";
import { Number } from "@/components/Number";
import {
  Breadcrumb,
  BreadcrumbItem,
  Divider,
  Nav,
  NavList,
  PageBreadcrumb,
  PageGroup,
  PageNavigation,
  PageSection,
  Text,
  TextContent,
  Title,
} from "@/libs/patternfly/react-core";
import { notFound } from "next/navigation";
import { PropsWithChildren, Suspense } from "react";

export default async function KafkaLayout({
  children,
  params: { kafkaId, topic: topicId },
}: PropsWithChildren<{ params: { kafkaId: string; topic: string } }>) {
  const cluster = await getResource(kafkaId, "kafka");
  if (!cluster || !cluster.attributes.cluster) {
    notFound();
  }
  const clusters = await getResources("kafka");
  const topic = await getTopic(cluster.attributes.cluster.id, topicId);

  return (
    <>
      <PageGroup>
        <PageBreadcrumb>
          <Breadcrumb>
            <BreadcrumbLink href="/kafka">Kafka</BreadcrumbLink>
            <BreadcrumbItem>
              <KafkaSelectorBreadcrumbItem
                selected={cluster}
                clusters={clusters}
              />
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
          <Title headingLevel={"h1"}>{topic.attributes.name}</Title>
          <TextContent>
            <Text component={"small"}>
              <Number value={topic.attributes.recordCount || 0} /> messages
            </Text>
          </TextContent>
        </PageSection>
        <PageNavigation>
          <Nav aria-label="Group section navigation" variant="tertiary">
            <NavList>
              <NavItemLink url={`/kafka/${kafkaId}/topics/${topicId}/overview`}>
                Overview
              </NavItemLink>
              <NavItemLink url={`/kafka/${kafkaId}/topics/${topicId}/messages`}>
                Messages
              </NavItemLink>
              <NavItemLink url={`/kafka/${kafkaId}/topics/${topicId}/schema`}>
                Schema
              </NavItemLink>
              <NavItemLink
                url={`/kafka/${kafkaId}/topics/${topicId}/consumer-groups`}
              >
                Consumer groups
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
