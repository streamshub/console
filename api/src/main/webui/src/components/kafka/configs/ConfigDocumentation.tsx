/**
 * Shared expandable documentation row for configuration tables.
 */

import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Title } from '@patternfly/react-core';
import {
  ExpandableRowContent,
  Tr,
  Td,
} from '@patternfly/react-table';

export function useExpandedConfigRows() {
  const [expandedRows, setExpandedRows] = useState<Set<string>>(new Set());

  const toggleRow = (name: string) => {
    setExpandedRows((prev) => {
      const next = new Set(prev);
      if (next.has(name)) {
        next.delete(name);
      } else {
        next.add(name);
      }
      return next;
    });
  };

  return { expandedRows, toggleRow };
}

interface ConfigDocumentationProps {
  documentation?: string;
  colSpan: number;
  isExpanded: boolean;
}

export function ConfigDocumentation({
  documentation,
  colSpan,
  isExpanded,
}: ConfigDocumentationProps) {
  const { t } = useTranslation();
  const content = documentation?.trim();

  return (
    <Tr isExpanded={isExpanded}>
      <Td colSpan={colSpan}>
        <ExpandableRowContent>
          <div
            style={{ padding: '1rem' }}
            data-testid="config-documentation-content"
          >
            <Title
              headingLevel="h4"
              size="md"
              style={{ marginBottom: '0.5rem' }}
            >
              {t('topics.configuration.documentation')}
            </Title>
            <div style={{ whiteSpace: 'pre-wrap' }}>
              {content || t('topics.configuration.noDocumentation')}
            </div>
          </div>
        </ExpandableRowContent>
      </Td>
    </Tr>
  );
}
