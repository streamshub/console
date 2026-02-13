/**
 * CLI Command Display Component
 * Shows the equivalent kafka-consumer-groups CLI command
 */

import { useTranslation } from 'react-i18next';
import {
  CodeBlock,
  CodeBlockCode,
  CodeBlockAction,
  ClipboardCopyButton,
  ExpandableSection,
} from '@patternfly/react-core';
import { Fragment, useState } from 'react';

interface CliCommandDisplayProps {
  command: string;
  isExpanded?: boolean;
}

export function CliCommandDisplay({
  command,
  isExpanded = false,
}: CliCommandDisplayProps) {
  const { t } = useTranslation();
  const [isOpen, setIsOpen] = useState(isExpanded);
  const [copied, setCopied] = useState(false);
  const clipboardCopyFunc = (text: string) => {
    navigator.clipboard.writeText(text.toString());
  };

  const onClick = (text: string) => {
    clipboardCopyFunc(text);
    setCopied(true);
  };
  return (
    <ExpandableSection
      toggleText={t('groups.resetOffset.cliCommand')}
      onToggle={(_event, expanded) => setIsOpen(expanded)}
      isExpanded={isOpen}
    >
      <CodeBlock
        actions={
          <Fragment>
            <CodeBlockAction>
              <ClipboardCopyButton
                id="basic-copy-button"
                aria-label="Copy to clipboard basic example code block"
                onClick={_ => onClick(command)}
                exitDelay={copied ? 1500 : 600}
                maxWidth="110px"
                variant="plain"
                onTooltipHidden={() => setCopied(false)}
              >
                {copied ? t('groups.resetOffset.commandCopied') : t('groups.resetOffset.copyCommand')}
              </ClipboardCopyButton>
            </CodeBlockAction>
          </Fragment>
        }
      >
        <CodeBlockCode>{command}</CodeBlockCode>
      </CodeBlock>
    </ExpandableSection>
  );
}