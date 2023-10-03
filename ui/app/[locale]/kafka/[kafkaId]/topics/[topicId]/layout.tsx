import { getKafkaCluster } from "@/api/kafka";
import { getResources } from "@/api/resources";
import { getTopic } from "@/api/topics";
import { KafkaSelectorBreadcrumbItem } from "@/app/[locale]/kafka/[kafkaId]/(root)/KafkaSelectorBreadcrumbItem";
import { KafkaTopicParams } from "@/app/[locale]/kafka/[kafkaId]/topics/kafkaTopic.params";
import { BreadcrumbLink } from "@/components/BreadcrumbLink";
import { Bytes } from "@/components/Bytes";
import { Loading } from "@/components/Loading";
import { NavItemLink } from "@/components/NavItemLink";
import { Number } from "@/components/Number";
import {
  Breadcrumb,
  BreadcrumbItem,
  Divider,
  Label,
  LabelGroup,
  Nav,
  NavList,
  PageBreadcrumb,
  PageGroup,
  PageNavigation,
  PageSection,
  Title,
} from "@/libs/patternfly/react-core";
import {
  CodeBranchIcon,
  HddIcon,
  ListIcon,
} from "@/libs/patternfly/react-icons";
import { notFound } from "next/navigation";
import { PropsWithChildren, Suspense } from "react";

export default async function KafkaLayout({
  children,
  params: { kafkaId, topicId: topicId },
}: PropsWithChildren<{ params: KafkaTopicParams }>) {
  const cluster = await getKafkaCluster(kafkaId);
  if (!cluster) {
    notFound();
  }
  const clusters = await getResources("kafka");
  const topic = await getTopic(cluster.id, topicId);

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
          <Title headingLevel={"h1"} className={"pf-v5-u-pb-sm"}>
            {topic.attributes.name}
          </Title>
          <LabelGroup>
            <Label color={"blue"} icon={<CodeBranchIcon />}>
              <Number value={topic.attributes.partitions.length} />
            </Label>
            <Label color={"green"} icon={<ListIcon />}>
              <Number value={topic.attributes.recordCount} />
            </Label>
            <Label color={"cyan"} icon={<HddIcon />}>
              <Bytes value={topic.attributes.totalLeaderLogBytes} />
            </Label>
          </LabelGroup>
        </PageSection>
        <PageNavigation>
          <Nav aria-label="Group section navigation" variant="tertiary">
            <NavList>
              <NavItemLink
                url={`/kafka/${kafkaId}/topics/${topicId}/partitions`}
              >
                Partitions
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
