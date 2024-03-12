import {
  EmptyState,
  EmptyStateHeader,
  EmptyStateIcon,
  Spinner,
} from "@/libs/patternfly/react-core";
import { useTranslations } from "next-intl";

export function EmptyStateLoading() {
  const t = useTranslations();
  return (
    <EmptyState>
      <EmptyStateHeader
        titleText={t("Loading.loading")}
        headingLevel="h4"
        icon={<EmptyStateIcon icon={Spinner} />}
      />
    </EmptyState>
  );
}
