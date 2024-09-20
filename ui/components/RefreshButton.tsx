"use client";
import { Button, Spinner, Tooltip } from "@/libs/patternfly/react-core";
import { SyncAltIcon } from "@/libs/patternfly/react-icons";
import { useRouter } from "@/navigation";
import { useTranslations } from "next-intl";
import { useState } from "react";

export type RefreshButtonProps = {
  lastRefresh: Date;
  tooltip?: string;
  ariaLabel?: string;
  onClick?: () => void;
};

export function RefreshButton({
  lastRefresh,
  ariaLabel,
  onClick,
  tooltip,
}: RefreshButtonProps) {
  const t = useTranslations();
  const router = useRouter();
  const [refreshTs, setRefreshTs] = useState<Date | undefined>();
  const handleClick =
    onClick ??
    (() => {
      setRefreshTs(new Date());
      router.refresh();
    });

  const isRefreshing = refreshTs !== undefined && refreshTs >= lastRefresh;

  const defaultTooltip = isRefreshing
    ? t("RefreshButton.refreshing_tooltip")
    : t("RefreshButton.refresh_description");
  return (
    <Tooltip content={tooltip || defaultTooltip}>
      <Button
        className="pf-m-hoverable"
        variant="plain"
        aria-label={ariaLabel || t("RefreshButton.refresh_button_label")}
        isDisabled={isRefreshing}
        onClick={isRefreshing === true ? undefined : handleClick}
        icon={
          isRefreshing === false ? <SyncAltIcon /> : <Spinner size={"md"} />
        }
      />
    </Tooltip>
  );
}
