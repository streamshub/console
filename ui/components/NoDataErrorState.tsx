"use client";
import {
  EmptyState,
  EmptyStateBody,
  EmptyStateIcon,
  Title,
} from "@patternfly/react-core";
import {
  ErrorCircleOIcon,
  BanIcon
} from "@patternfly/react-icons";
import { ApiError } from '@/api/api';

export function NoDataErrorState({ errors }: { errors: ApiError[] }) {
  let errorIcon;

  switch (errors[0].status ?? '400') {
    case '401':
    case '403':
      errorIcon = BanIcon;
      break;
    default:
      errorIcon = ErrorCircleOIcon;
      break;
  }

  return (
    <EmptyState variant={"lg"}>
      <EmptyStateIcon icon={ errorIcon } />
      <Title headingLevel="h4" size="lg">
        { errors[0].title }
      </Title>
      <EmptyStateBody>
        <>
        { errors.map(err => {
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
