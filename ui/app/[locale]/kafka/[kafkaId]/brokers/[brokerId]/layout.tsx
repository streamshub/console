import { getKafkaCluster } from "@/api/kafka";
import { getResources } from "@/api/resources";
import { KafkaBrokerParams } from "@/app/[locale]/kafka/[kafkaId]/brokers/kafkaBroker.params";
import { BreadcrumbLink } from "@/components/BreadcrumbLink";
import { Loading } from "@/components/Loading";
import { NavItemLink } from "@/components/NavItemLink";
import {
  Breadcrumb,
  BreadcrumbItem,
  ClipboardCopy,
  Divider,
  Label,
  Nav,
  NavList,
  PageBreadcrumb,
  PageGroup,
  PageNavigation,
  PageSection,
  Split,
  SplitItem,
  Title,
} from "@/libs/patternfly/react-core";
import { notFound } from "next/navigation";
import { PropsWithChildren, Suspense } from "react";
import { KafkaBreadcrumbItem } from "./KafkaBreadcrumbItem";

export default async function BrokerDetails({
  children,
  params: { kafkaId, brokerId },
}: PropsWithChildren<{
  params: KafkaBrokerParams;
}>) {
  const cluster = await getKafkaCluster(kafkaId);
  if (!cluster) {
    notFound();
  }
  const clusters = await getResources("kafka");
  const broker = cluster.attributes.nodes.find((n) => `${n.id}` === brokerId);
  if (!broker) {
    notFound();
  }
  return (
    <>
      <PageGroup>
        <PageBreadcrumb>
          <Breadcrumb>
            <BreadcrumbLink href="/kafka">Kafka</BreadcrumbLink>
            <BreadcrumbItem>
              <KafkaBreadcrumbItem selected={cluster} clusters={clusters} />
            </BreadcrumbItem>
            <BreadcrumbLink href={`/kafka/${kafkaId}/brokers`}>
              Brokers
            </BreadcrumbLink>
            <BreadcrumbItem isActive={true}>Broker {broker.id}</BreadcrumbItem>
          </Breadcrumb>
        </PageBreadcrumb>
        <PageSection
          variant={"light"}
          padding={{ default: "noPadding" }}
          className={"pf-v5-u-px-lg pf-v5-u-pt-sm"}
        >
          <Split hasGutter={true}>
            <SplitItem>
              <Title headingLevel={"h1"} className={"pf-v5-u-pb-sm"}>
                Broker {broker.id}
              </Title>
            </SplitItem>
            {cluster.attributes.controller.id === broker.id && (
              <SplitItem>
                <Label color={"purple"}>Controller</Label>
              </SplitItem>
            )}
          </Split>

          <ClipboardCopy
            hoverTip="Copy"
            clickTip="Copied"
            variant="inline-compact"
            isBlock={true}
          >
            {broker.host}:{broker.port}
          </ClipboardCopy>
        </PageSection>
        <PageNavigation>
          <Nav aria-label="Group section navigation" variant="tertiary">
            <NavList>
              <NavItemLink
                url={`/kafka/${kafkaId}/brokers/${brokerId}/configuration`}
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
