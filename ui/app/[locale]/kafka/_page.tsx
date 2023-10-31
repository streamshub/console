import { getResources } from "@/api/resources";
import { ButtonLink } from "@/components/ButtonLink";
import {
  EmptyState,
  EmptyStateActions,
  EmptyStateBody,
  EmptyStateFooter,
  EmptyStateIcon,
  PageSection,
  Split,
  SplitItem,
  TextContent,
  Title,
} from "@/libs/patternfly/react-core";
import { PlusCircleIcon } from "@/libs/patternfly/react-icons";
import { ClustersTable } from "./ClustersTable";

export default async function Home() {
  const clusters = await getResources("kafka");
  const isEmpty = clusters.length === 0;
  return (
    <>
      <PageSection variant={"light"}>
        <Split>
          <SplitItem isFilled>
            <TextContent>
              <Title headingLevel="h1" ouiaId={"page-title"}>
                Kafka
              </Title>
            </TextContent>
          </SplitItem>
          {!isEmpty && (
            <SplitItem>
              <ButtonLink
                href={"/resources/create/kafka"}
                ouiaId={"add-resource-primary"}
              >
                Add a Kafka Cluster Resource
              </ButtonLink>
            </SplitItem>
          )}
        </Split>
      </PageSection>
      <PageSection isFilled>
        <ClustersTable clusters={clusters} />
        {isEmpty && (
          <EmptyState variant={"lg"}>
            <EmptyStateIcon icon={PlusCircleIcon} />
            <Title headingLevel="h2" size="lg">
              No Kafka Cluster Resources
            </Title>
            <EmptyStateBody>
              To get started, create a Kafka Cluster Resource
            </EmptyStateBody>
            <EmptyStateFooter>
              <EmptyStateActions>
                <ButtonLink variant="primary" href={"/resources/create/kafka"}>
                  Create a Kafka Cluster Resource
                </ButtonLink>
              </EmptyStateActions>
            </EmptyStateFooter>
          </EmptyState>
        )}
      </PageSection>
    </>
  );
}
