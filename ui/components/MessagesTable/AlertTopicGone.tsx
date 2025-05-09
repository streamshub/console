import { Alert } from "@/libs/patternfly/react-core";
import { AlertActionLink } from "@/libs/patternfly/react-core";
import { useTranslations } from "next-intl";

export function AlertTopicGone({ onClick }: { onClick: () => void }) {
  const t = useTranslations();
  return (
    <Alert
      variant="danger"
      title={t("AlertTopicGone.topic_not_found")}
      ouiaId="topic-not-found"
      actionLinks={
        <AlertActionLink onClick={onClick}>
          {t("AlertTopicGone.go_back_to_the_list_of_topics")}
        </AlertActionLink>
      }
    >
      {t("AlertTopicGone.this_topic_was_deleted_or_you_donapost_have_the_co")}
    </Alert>
  );
}
