import {
  Button,
  EmptyState,
  EmptyStateBody,
  EmptyStateIcon,
  Title,
} from "@/libs/patternfly/react-core";
import { CubesIcon } from "@/libs/patternfly/react-icons";
import { useFilterParams } from "@/utils/useFilterParams";
import { useTranslations } from "next-intl";

export function NoDataEmptyState() {
  const t = useTranslations("message-browser");
  const updateUrl = useFilterParams({});

  function onRefresh() {
    updateUrl({
      "filter[offset]": undefined,
      "filter[timestamp]": undefined,
      "filter[epoch]": undefined,
    });
  }

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
