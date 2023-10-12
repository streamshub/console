"use client";
import { KafkaResource } from "@/api/resources";
import {
  Card,
  CardBody,
  CardHeader,
  CardTitle,
  DescriptionList,
  DescriptionListDescription,
  DescriptionListGroup,
  DescriptionListTerm,
  Divider,
  Truncate,
} from "@/libs/patternfly/react-core";
import { CardFooter } from "@patternfly/react-core";
import { Route } from "next";
import Link from "next-intl/link";
import { useRouter } from "next/navigation";

export function ResourceCard<T extends string>({
  id,
  attributes: { name, mechanism, principal, bootstrapServer },
  href,
}: KafkaResource & { href: Route<T> | URL }) {
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
      <CardHeader>
        <CardTitle>{name}</CardTitle>
      </CardHeader>
      <CardBody>
        <DescriptionList>
          <DescriptionListGroup>
            <DescriptionListTerm>Cluster Address</DescriptionListTerm>
            <DescriptionListDescription>
              <Truncate content={bootstrapServer} position={"middle"} />
            </DescriptionListDescription>
          </DescriptionListGroup>
          <DescriptionListGroup>
            <DescriptionListTerm>Principal</DescriptionListTerm>
            <DescriptionListDescription>{principal}</DescriptionListDescription>
          </DescriptionListGroup>
          <DescriptionListGroup>
            <DescriptionListTerm>SASL Mechanism</DescriptionListTerm>
            <DescriptionListDescription>{mechanism}</DescriptionListDescription>
          </DescriptionListGroup>
        </DescriptionList>
      </CardBody>
      <Divider />
      <CardFooter>
        <Link href={href}>Connect to this cluster</Link>
      </CardFooter>
    </Card>
  );
}
