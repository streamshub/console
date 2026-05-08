// In a toolbar or header component
import { useQueryClient } from '@tanstack/react-query';
import { Button, ButtonProps, Tooltip } from '@patternfly/react-core';
import { SyncIcon } from '@patternfly/react-icons';
import { useState, useTransition } from 'react';
import { formatDateTime } from '@/utils/dateTime';
import { Trans } from 'react-i18next';

export function RefreshAllButton({ staticRefresh} : { staticRefresh?: Date; }) {
  const queryClient = useQueryClient();
  const [isRefreshing, startTransition] = useTransition();
  const now = new Date();
  const [lastRefresh, setLastRefresh] = useState(staticRefresh ?? now);

  const handleRefresh: ButtonProps["onClick"] = async (e) => {
    e.preventDefault();
    startTransition(async () => {
      await queryClient.refetchQueries({ type: 'active' });
      setLastRefresh(new Date());
    });
  };

  return (
    <Tooltip content={
      <Trans 
        i18nKey="common.refreshDataTooltip"
        components={{
          br: <br/>,
        }}
        values={{
          lastRefresh: formatDateTime({ value: lastRefresh })
        }}
        />
    }>
      <Button
        variant="plain"
        onClick={handleRefresh}
        isLoading={isRefreshing}
        icon={<SyncIcon />}
      />
    </Tooltip>
  );
}
