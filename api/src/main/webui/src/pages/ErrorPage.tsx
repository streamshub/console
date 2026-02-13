/**
 * Error Page - Displayed when routing errors occur
 */

import { useRouteError, isRouteErrorResponse } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  Page,
  PageSection,
  EmptyState,
  EmptyStateBody,
  Button,
  Title,
} from '@patternfly/react-core';
import { ExclamationCircleIcon } from '@patternfly/react-icons';

export function ErrorPage() {
  const { t } = useTranslation();
  const error = useRouteError();

  let errorMessage = t('errors.generic');
  
  if (isRouteErrorResponse(error)) {
    errorMessage = error.statusText || error.data?.message || errorMessage;
  } else if (error instanceof Error) {
    errorMessage = error.message;
  }

  return (
    <Page>
      <PageSection>
        <EmptyState>
          <ExclamationCircleIcon />
          <Title headingLevel="h1" size="lg">
            {t('common.error')}
          </Title>
          <EmptyStateBody>{errorMessage}</EmptyStateBody>
          <Button variant="primary" onClick={() => window.location.href = '/'}>
            {t('common.back')}
          </Button>
        </EmptyState>
      </PageSection>
    </Page>
  );
}