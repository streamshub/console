import { getResource, getResources } from "@/api/resources";
import { BreadcrumbLink } from "@/components/BreadcrumbLink";
import { Loading } from "@/components/Loading";
import { NavItemLink } from "@/components/NavItemLink";
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
import { KafkaSelectorBreadcrumbItem } from "./KafkaSelectorBreadcrumbItem";

export default async function KafkaLayout({
  children,
  params: { kafkaId },
}: PropsWithChildren<{ params: { kafkaId: string } }>) {
  const cluster = await getResource(kafkaId, "kafka");
  if (!cluster || !cluster.attributes.cluster) {
    notFound();
  }
  const clusters = await getResources("kafka");

  return (
    <>
      <PageGroup>
        <PageBreadcrumb>
          <Breadcrumb>
            <BreadcrumbLink href="/kafka">Kafka</BreadcrumbLink>
            <BreadcrumbItem isActive>
              <Suspense fallback={"Loading clusters..."}>
                <KafkaSelectorBreadcrumbItem
                  selected={cluster}
                  clusters={clusters}
                  isActive={true}
                />
              </Suspense>
            </BreadcrumbItem>
          </Breadcrumb>
        </PageBreadcrumb>
        <PageSection
          variant={"light"}
          padding={{ default: "noPadding" }}
          className={"pf-v5-u-px-lg pf-v5-u-pt-sm"}
        >
          <Title headingLevel={"h1"}>{cluster.attributes.name}</Title>
          <TextContent>
            <Text
              component={"small"}
            >{`${cluster.attributes.principal}@${cluster.attributes.bootstrapServer}`}</Text>
          </TextContent>
        </PageSection>
        <PageNavigation>
          <Nav aria-label="Group section navigation" variant="tertiary">
            <NavList>
              <NavItemLink url={`/kafka/${kafkaId}/overview`}>
                Overview
              </NavItemLink>
              <NavItemLink url={`/kafka/${kafkaId}/brokers`}>
                Brokers
              </NavItemLink>
              <NavItemLink url={`/kafka/${kafkaId}/topics`}>Topics</NavItemLink>
              <NavItemLink url={`/kafka/${kafkaId}/schema-registry`}>
                Schema registry
              </NavItemLink>
              <NavItemLink url={`/kafka/${kafkaId}/consumer-groups`}>
                Consumer groups
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
