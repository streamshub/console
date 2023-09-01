import { getAuthProfiles } from "@/api/auth";
import { AuthProfileCard } from "@/components/authProfileCard";
import { ButtonLink } from "@/components/buttonLink";
import {
  EmptyState,
  EmptyStateActions,
  EmptyStateBody,
  EmptyStateFooter,
  EmptyStateIcon,
  Gallery,
  PageSection,
  Split,
  SplitItem,
  Text,
  TextContent,
  Title,
} from "@/libs/patternfly/react-core";
import { PlusCircleIcon } from "@/libs/patternfly/react-icons";

export default async function Home() {
  const authProfiles = await getAuthProfiles();

  return (
    <>
      <PageSection variant={"light"}>
        <Split>
          <SplitItem isFilled>
            <TextContent>
              <Title headingLevel="h1">Authorization Profiles</Title>
              <Text>
                Select an Authorization Profile to access a Cluster from a list
                of previously created ones. Clusters can have more than one
                Authorization Profile.
              </Text>
            </TextContent>
          </SplitItem>
          {authProfiles.length > 0 && (
            <SplitItem>
              <ButtonLink href={"new-auth-profile"}>
                Add a new Authorization Profile
              </ButtonLink>
            </SplitItem>
          )}
        </Split>
      </PageSection>
      <PageSection isFilled>
        <Gallery hasGutter aria-label={"Authorization Profiles"}>
          {authProfiles.map((ap) => (
            <AuthProfileCard key={ap.id} {...ap} />
          ))}
        </Gallery>
        {authProfiles.length === 0 && (
          <EmptyState variant={"lg"}>
            <EmptyStateIcon icon={PlusCircleIcon} />
            <Title headingLevel="h2" size="lg">
              No Authorization Profile
            </Title>
            <EmptyStateBody>
              To get started, create an Authorization Profile
            </EmptyStateBody>
            <EmptyStateFooter>
              <EmptyStateActions>
                <ButtonLink variant="primary" href={"new-auth-profile"}>
                  Create a new Authorization Profile
                </ButtonLink>
              </EmptyStateActions>
            </EmptyStateFooter>
          </EmptyState>
        )}
      </PageSection>
    </>
  );
}
