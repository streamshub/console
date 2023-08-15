import {
  EmptyState,
  EmptyStateBody,
  EmptyStateIcon,
  Title,
} from "@/libs/patternfly/react-core";
import { CubesIcon } from "@/libs/patternfly/react-icons";
import { useTranslations } from "next-intl";

export function SelectTopicEmptyState() {
  const t = useTranslations("message-browser");
  return (
    <EmptyState variant={"lg"}>
      <EmptyStateIcon icon={CubesIcon} />
      <Title headingLevel="h4" size="lg">
        No Topic selected
      </Title>
      <EmptyStateBody>Select a topic to continue</EmptyStateBody>
    </EmptyState>
  );
}
