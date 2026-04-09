/**
 * CLI command generator for kafka-consumer-groups commands
 */

import { ResetOffsetFormState } from '../api/types';

/**
 * Generate kafka-consumer-groups CLI command for reset offset operation
 */
export function generateCliCommand(
  groupId: string,
  state: ResetOffsetFormState,
  topicName?: string
): string {
  let command = `$ kafka-consumer-groups --bootstrap-server \${BOOTSTRAP_SERVER} --group '${groupId}' `;

  // Add reset-offsets or delete-offsets flag
  if (state.offsetValue === 'delete') {
    command += '--delete-offsets ';
  } else {
    command += '--reset-offsets ';
  }

  // Add topic/partition specification
  if (state.topicSelection === 'allTopics') {
    command += '--all-topics';
  } else if (topicName) {
    // Add partition if specific partition is selected
    const partition =
      state.partitionSelection === 'allPartitions'
        ? ''
        : `:${state.selectedPartition}`;

    command += `--topic ${topicName}${partition}`;
  }

  // Add offset specification (not for delete operation)
  if (state.offsetValue !== 'delete') {
    if (state.offsetValue === 'custom') {
      command += ` --to-offset ${state.customOffset ?? 0}`;
    } else if (state.offsetValue === 'specificDateTime') {
      const dateTimeValue =
        state.dateTimeFormat === 'Epoch' && state.dateTime
          ? new Date(parseInt(state.dateTime, 10)).toISOString()
          : state.dateTime;
      command += ` --to-datetime ${dateTimeValue}`;
    } else {
      // earliest or latest
      command += ` --to-${state.offsetValue}`;
    }
  }

  // Always add dry-run flag for preview
  command += ' --dry-run';

  return command;
}

/**
 * Generate CLI command for execution (without dry-run flag)
 */
export function generateExecutionCommand(
  groupId: string,
  state: ResetOffsetFormState,
  topicName?: string
): string {
  const dryRunCommand = generateCliCommand(groupId, state, topicName);
  // Remove the --dry-run flag for actual execution
  return dryRunCommand.replace(' --dry-run', ' --execute');
}

/**
 * Format CLI command for display with syntax highlighting hints
 */
export function formatCliCommand(command: string): string {
  // Add line breaks for better readability
  return command
    .replace(/--bootstrap-server/g, '\\\n  --bootstrap-server')
    .replace(/--group/g, '\\\n  --group')
    .replace(/--reset-offsets/g, '\\\n  --reset-offsets')
    .replace(/--delete-offsets/g, '\\\n  --delete-offsets')
    .replace(/--all-topics/g, '\\\n  --all-topics')
    .replace(/--topic/g, '\\\n  --topic')
    .replace(/--to-offset/g, '\\\n  --to-offset')
    .replace(/--to-datetime/g, '\\\n  --to-datetime')
    .replace(/--to-earliest/g, '\\\n  --to-earliest')
    .replace(/--to-latest/g, '\\\n  --to-latest')
    .replace(/--dry-run/g, '\\\n  --dry-run')
    .replace(/--execute/g, '\\\n  --execute');
}