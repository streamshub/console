import { useTranslations } from "next-intl";

export function NoData() {
  const t = useTranslations("message-browser");
  return <span className="pf-u-color-400">{t("no_data")}</span>;
}
