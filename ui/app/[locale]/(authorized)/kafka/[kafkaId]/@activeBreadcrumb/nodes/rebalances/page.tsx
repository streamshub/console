import { BreadcrumbItem } from "@/libs/patternfly/react-core";
import { useTranslations } from "next-intl";

export default function RebalanceActiveBreadcrumb() {
  const t = useTranslations();
  return <BreadcrumbItem showDivider={true}>{t("nodes.title")}</BreadcrumbItem>;
}
