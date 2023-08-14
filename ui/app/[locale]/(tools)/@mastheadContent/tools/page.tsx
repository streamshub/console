import { useTranslations } from "next-intl";

export default function Content() {
  const t = useTranslations("common");
  return <span>{t("title")}</span>;
}
