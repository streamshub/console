import { Button, Spinner, Tooltip } from "@patternfly/react-core";
import { SyncAltIcon } from "@patternfly/react-icons";
import { useTranslations } from "next-intl";

export type RefreshButtonProps = {
  isDisabled?: boolean;
  tooltip?: string;
  isRefreshing: boolean;
  ariaLabel?: string;
  onClick: () => void;
};

export function RefreshButton({
  ariaLabel,
  onClick,
  isDisabled,
  tooltip,
  isRefreshing,
}: RefreshButtonProps) {
  const t = useTranslations();

  const defaultTooltip = isRefreshing
    ? t("RefreshButton.refreshing_tooltip")
    : t("RefreshButton.refresh_description");
  return (
    <Tooltip content={tooltip || defaultTooltip}>
      <Button
        className="pf-m-hoverable"
        variant="plain"
        aria-label={ariaLabel || t("RefreshButton.refresh_button_label")}
        isDisabled={isDisabled}
        onClick={isDisabled === true ? undefined : onClick}
        icon={
          isRefreshing === false ? <SyncAltIcon /> : <Spinner size={"md"} />
        }
      />
    </Tooltip>
  );
}
