"use client";
import { BreadcrumbLink } from "@/components/BreadcrumbLink";
import { ButtonLink } from "@/components/buttonLink";
import {
  Breadcrumb,
  BreadcrumbItem,
  EmptyState,
  EmptyStateActions,
  EmptyStateBody,
  EmptyStateFooter,
  EmptyStateHeader,
  EmptyStateIcon,
  PageBreadcrumb,
  PageGroup,
  PageSection,
  Title,
} from "@/libs/patternfly/react-core";
import { PathMissingIcon } from "@/libs/patternfly/react-icons";

export default function NotFound() {
  return (
    <>
      <PageGroup>
        <PageBreadcrumb>
          <Breadcrumb>
            <BreadcrumbLink href="/kafka">Kafka</BreadcrumbLink>
            <BreadcrumbItem isActive>Cluster</BreadcrumbItem>
          </Breadcrumb>
        </PageBreadcrumb>
      </PageGroup>
      <PageSection variant={"light"}>
        <Title headingLevel={"h1"}>Cluster</Title>
      </PageSection>
      <PageSection padding={{ default: "noPadding" }} isFilled>
        <EmptyState variant={"full"}>
          <EmptyStateHeader
            titleText={"Kafka Cluster not found"}
            headingLevel={"h1"}
            icon={<EmptyStateIcon icon={PathMissingIcon} />}
          />
          <EmptyStateBody>
            The selected cluster is unavailable. Please check the connection and
            try again.
          </EmptyStateBody>
          <EmptyStateFooter>
            <EmptyStateActions>
              <ButtonLink href={"/resources"}>Check the connection</ButtonLink>
            </EmptyStateActions>
          </EmptyStateFooter>
        </EmptyState>
      </PageSection>
    </>
  );
}
