"use client";
import {
  Button,
  EmptyState,
  EmptyStateBody,
  EmptyStateIcon,
  Title,
} from "@/libs/patternfly/react-core";
import { SearchIcon } from "@/libs/patternfly/react-icons";
import { useFilterParams } from "@/utils/useFilterParams";
import { useTranslations } from "next-intl";
import { startTransition } from "react";

export function NoResultsEmptyState() {
  const t = useTranslations("message-browser");
  const updateUrl = useFilterParams({});

  function onReset() {
    startTransition(() =>
      updateUrl({
        "filter[offset]": undefined,
        "filter[timestamp]": undefined,
        "filter[epoch]": undefined,
      }),
    );
  }

  return (
    <EmptyState variant={"lg"}>
      <EmptyStateIcon icon={SearchIcon} />
      <Title headingLevel="h4" size="lg">
        {t("no_results_title")}
      </Title>
      <EmptyStateBody>{t("no_results_body")}</EmptyStateBody>
      <Button variant={"link"} onClick={onReset}>
        {t("no_results_reset")}
      </Button>
    </EmptyState>
  );
}
