import { useTranslations } from "next-intl";

export type NoDataCellProps = {
  columnLabel: string;
};
export function NoDataCell({ columnLabel }: NoDataCellProps) {
  const t = useTranslations("message-browser");
  return (
    <span className="pf-u-color-400">
      {t("no_data", { column: columnLabel })}
    </span>
  );
}
