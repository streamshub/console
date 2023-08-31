"use client";
import { AuthProfile } from "@/api/getAuthProfiles";
import {
    Card,
    CardBody,
    CardHeader,
    CardTitle
} from "@/libs/patternfly/react-core";
import { useRouter } from "next/navigation";

export function AuthProfileCard({ id, attributes: { name, bootstrapServers } }: AuthProfile) {
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
                        router.push(`/${id}/tools`);
                    },
                    selectableActionId: id,
                    selectableActionAriaLabelledby: cardId,
                }}
            >
                <CardTitle>{name}</CardTitle>
            </CardHeader>
            <CardBody>{bootstrapServers}</CardBody>
        </Card>
    );
}
