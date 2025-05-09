"use client";
import {
  EmptyState,
  EmptyStateBody,
  Title,
} from "@/libs/patternfly/react-core";
import { ErrorCircleOIcon, BanIcon } from "@/libs/patternfly/react-icons";
import { ApiError } from "@/api/api";

export function NoDataErrorState({ errors }: { errors: ApiError[] }) {
  let errorIcon;

  switch (errors[0].status ?? "400") {
    case "401":
    case "403":
      errorIcon = BanIcon;
      break;
    default:
      errorIcon = ErrorCircleOIcon;
      break;
  }

  return (
    <EmptyState
      titleText={
        <Title headingLevel="h4" size="lg">
          {errors[0].title}
        </Title>
      }
      icon={errorIcon}
      variant={"lg"}
    >
      <EmptyStateBody>
        <>
          {errors.map((err) => {
            return (
              <>
                {err.title}: {err.detail} {err.code && <>({err.code})</>}
              </>
            );
          })}
        </>
      </EmptyStateBody>
    </EmptyState>
  );
}
