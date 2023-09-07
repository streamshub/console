import { getBookmarks } from "@/api/bookmarks";
import { BookmarkCard } from "@/components/bookmarkCard";
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
  const bookmarks = await getBookmarks();

  return (
    <>
      <PageSection variant={"light"}>
        <Split>
          <SplitItem isFilled>
            <TextContent>
              <Title headingLevel="h1">Bookmarks</Title>
              <Text>
                Brief description of what a Bookmark is and how it works.
              </Text>
            </TextContent>
          </SplitItem>
          {bookmarks.length > 0 && (
            <SplitItem>
              <ButtonLink href={"create"}>Add a Bookmark</ButtonLink>
            </SplitItem>
          )}
        </Split>
      </PageSection>
      <PageSection isFilled>
        <Gallery hasGutter aria-label={"Authorization Profiles"}>
          {bookmarks.map((ap) => (
            <BookmarkCard key={ap.id} {...ap} />
          ))}
        </Gallery>
        {bookmarks.length === 0 && (
          <EmptyState variant={"lg"}>
            <EmptyStateIcon icon={PlusCircleIcon} />
            <Title headingLevel="h2" size="lg">
              No Bookmarks
            </Title>
            <EmptyStateBody>To get started, create a Bookmark</EmptyStateBody>
            <EmptyStateFooter>
              <EmptyStateActions>
                <ButtonLink variant="primary" href={"create"}>
                  Create a Bookmark
                </ButtonLink>
              </EmptyStateActions>
            </EmptyStateFooter>
          </EmptyState>
        )}
      </PageSection>
    </>
  );
}
