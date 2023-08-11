import {
  Button,
  EmptyState,
  EmptyStateBody,
  EmptyStateIcon,
  Title,
} from "@/libs/patternfly/react-core";
import { CubesIcon } from "@/libs/patternfly/react-icons";
import { useTranslations } from "next-intl";

export function NoDataEmptyState({ onRefresh }: { onRefresh: () => void }) {
  const t = useTranslations("message-browser");
  return (
    <EmptyState variant={"lg"}>
      <EmptyStateIcon icon={CubesIcon} />
      <Title headingLevel="h4" size="lg">
        {t("no_data_title")}
      </Title>
      <EmptyStateBody>{t("no_data_body")}</EmptyStateBody>
      <Button onClick={onRefresh}>{t("no_data_refresh")}</Button>
    </EmptyState>
  );
}
