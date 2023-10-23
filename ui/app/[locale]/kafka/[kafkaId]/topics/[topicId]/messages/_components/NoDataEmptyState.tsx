"use client";
import { useFilterParams } from "@/utils/useFilterParams";
import {
  Button,
  EmptyState,
  EmptyStateBody,
  EmptyStateFooter,
  EmptyStateIcon,
  Title,
} from "@patternfly/react-core";
import { CubesIcon } from "@patternfly/react-icons";
import { useTranslations } from "next-intl";

export function NoDataEmptyState() {
  const t = useTranslations("message-browser");
  const updateUrl = useFilterParams({});

  function onRefresh() {
    updateUrl({
      _ts: Date.now(),
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
      <EmptyStateFooter>
        <Button onClick={onRefresh}>{t("no_data_refresh")}</Button>
      </EmptyStateFooter>
    </EmptyState>
  );
}
