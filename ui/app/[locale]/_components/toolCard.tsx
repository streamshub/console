"use client";
import { Tool } from "@/app/[locale]/_api/getTools";
import {
  Card,
  CardBody,
  CardHeader,
  CardTitle,
  Icon,
} from "@/libs/patternfly/react-core";
import { useTranslations } from "next-intl";
import { useRouter } from "next/navigation";

export function ToolCard({ id, url, icon, title, description }: Tool) {
  const router = useRouter();
  const t = useTranslations();
  const cardId = `tool-card-${id}`;
  return (
    <Card isClickable={true} key={id} id={cardId} ouiaId={cardId}>
      <CardHeader
        selectableActions={{
          onClickAction: () => void router.push(url),
          selectableActionId: id,
          selectableActionAriaLabelledby: cardId,
        }}
      >
        <Icon size={"xl"}>{icon}</Icon>
      </CardHeader>
      <CardTitle>{t(title)}</CardTitle>
      <CardBody>{t(description)}</CardBody>
    </Card>
  );
}
