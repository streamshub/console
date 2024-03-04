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
  const t = useTranslations("RefreshButton");

  const defaultTooltip = isRefreshing
    ? t("refreshing_tooltip")
    : t("refresh_description");
  return (
    <Tooltip content={tooltip || defaultTooltip}>
      <Button
        className="pf-m-hoverable"
        variant="plain"
        aria-label={ariaLabel || t("refresh_button_label")}
        isDisabled={isDisabled}
        onClick={isDisabled === true ? undefined : onClick}
        icon={
          isRefreshing === false ? <SyncAltIcon /> : <Spinner size={"md"} />
        }
      />
    </Tooltip>
  );
}
