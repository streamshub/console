import { ButtonLink } from "@/components/buttonLink";
import {
  EmptyState,
  EmptyStateActions,
  EmptyStateBody,
  EmptyStateFooter,
  EmptyStateHeader,
  EmptyStateIcon,
  PageSection,
} from "@/libs/patternfly/react-core";
import { PathMissingIcon } from "@/libs/patternfly/react-icons";

export default function NotFound() {
  return (
    <PageSection padding={{ default: "noPadding" }} isFilled>
      <EmptyState variant={"full"}>
        <EmptyStateHeader
          titleText={"Page not found"}
          headingLevel={"h1"}
          icon={<EmptyStateIcon icon={PathMissingIcon} />}
        />
        <EmptyStateBody>
          Lorem ipsum dolor sit amet, consectetur adipisicing elit. Amet aperiam
          cum, cupiditate debitis dignissimos doloremque eius excepturi mollitia
          nam nesciunt, pariatur ratione, repudiandae sint sit tempora tenetur
          vel velit voluptatem?
        </EmptyStateBody>
        <EmptyStateFooter>
          <EmptyStateActions>
            <ButtonLink href={"/"}>Return to the home page</ButtonLink>
          </EmptyStateActions>
        </EmptyStateFooter>
      </EmptyState>
    </PageSection>
  );
}
