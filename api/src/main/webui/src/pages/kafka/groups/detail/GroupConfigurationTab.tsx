/**
 * Group Configuration Tab - Shows configuration settings for a consumer group
 */

import { useOutletContext } from 'react-router';
import { Group } from '@/api/types';
import { ConfigurationTable } from '@/components/common/ConfigurationTable';
interface GroupOutletContext {
  group: Group;
}

export function GroupConfigurationTab() {
  const { group } = useOutletContext<GroupOutletContext>();

  return (
    <ConfigurationTable
      configs={group.attributes.configs}
      ariaLabel="Group configuration table"
      showDocumentation
    />
  );
}
