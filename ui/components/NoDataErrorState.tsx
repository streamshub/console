"use client";
import { signOut } from "next-auth/react";
import {
  EmptyState,
  EmptyStateBody,
  Title,
} from "@/libs/patternfly/react-core";
import { ErrorCircleOIcon, BanIcon } from "@/libs/patternfly/react-icons";
import { ApiError } from "@/api/api";
import { useEffect, useRef } from "react";
import React from "react";

export function NoDataErrorState({ errors }: { errors: ApiError[] }) {
  const errorIcon = errors[0].status === "403" ? BanIcon : ErrorCircleOIcon;
  const hasSignedOut = useRef(false);

  useEffect(() => {
    if (errors[0].status === "401" && !hasSignedOut.current) {
      hasSignedOut.current = true;
      signOut();
    }
  }, [errors]);
  return (
    <EmptyState
      titleText={errors[0].title}
      headingLevel={"h4"}
      icon={errorIcon}
      variant={"lg"}
    >
      <EmptyStateBody>
        <>
          {errors.map((err) => (
            <React.Fragment key={err.id || err.title}>
              {err.title}: {err.detail} {err.code && <>({err.code})</>}
            </React.Fragment>
          ))}
        </>
      </EmptyStateBody>
    </EmptyState>
  );
}
