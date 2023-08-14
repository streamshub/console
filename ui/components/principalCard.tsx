"use client";
import { setContextPrincipal } from "@/api/setContextPrincipal";
import {
  Card,
  CardBody,
  CardFooter,
  CardHeader,
  CardTitle,
  Label,
} from "@/libs/patternfly/react-core";
import { useRouter } from "next/navigation";

export function PrincipalCard({ id, name, cluster, description }: Principal) {
  const router = useRouter();
  const cardId = `tool-card-${id}`;
  return (
    <Card
      isClickable={true}
      key={id}
      id={cardId}
      ouiaId={cardId}
      isCompact={true}
    >
      <CardHeader
        selectableActions={{
          onClickAction: () => {
            void setContextPrincipal(id);
            router.push("/tools");
          },
          selectableActionId: id,
          selectableActionAriaLabelledby: cardId,
        }}
      >
        <CardTitle>{name}</CardTitle>
      </CardHeader>
      <CardBody>{description}</CardBody>
      <CardFooter>
        <Label>{cluster}</Label>
      </CardFooter>
    </Card>
  );
}
