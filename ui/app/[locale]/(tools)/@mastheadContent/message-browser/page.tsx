import { useTranslations } from "next-intl";

export default function Content() {
  const t = useTranslations("message-browser");
  return <span>{t("title")}</span>;
}
