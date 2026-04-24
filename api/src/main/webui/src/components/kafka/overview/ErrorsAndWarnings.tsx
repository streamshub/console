/**
 * Errors and Warnings Component
 * 
 * Displays cluster errors and warnings in an expandable section.
 * Shows error/warning counts with icons and lists individual messages.
 */

import * as React from 'react';
import { useTranslation } from 'react-i18next';
import {
  ExpandableSection,
  Label,
  LabelGroup,
  Title,
  Tooltip,
} from '@patternfly/react-core';
import {
  ExclamationCircleIcon,
  ExclamationTriangleIcon,
  HelpIcon,
} from '@patternfly/react-icons';

export interface ErrorsAndWarningsProps {
  warnings: number;
  dangers: number;
  children?: React.ReactNode;
}

export function ErrorsAndWarnings({
  warnings,
  dangers,
  children,
}: ErrorsAndWarningsProps) {
  const { t } = useTranslation();
  const [isExpanded, setIsExpanded] = React.useState(warnings + dangers > 0);

  return (
    <ExpandableSection
      isExpanded={isExpanded}
      onToggle={(_event, isOpen) => setIsExpanded(isOpen)}
      toggleContent={
        <Title headingLevel="h3" className="pf-v6-u-font-size-sm">
          {t('ClusterCard.cluster_errors_and_warnings')}{' '}
          <Tooltip content={t('ClusterOverview.ErrorsAndWarnings.tooltip')}>
            <HelpIcon />
          </Tooltip>{' '}
          <LabelGroup>
            {dangers > 0 && (
              <Label color="red" isCompact>
                <ExclamationCircleIcon /> {dangers}
              </Label>
            )}
            {warnings > 0 && (
              <Label color="orange" isCompact>
                <ExclamationTriangleIcon /> {warnings}
              </Label>
            )}
          </LabelGroup>
        </Title>
      }
    >
      {children}
    </ExpandableSection>
  );
}