import { KafkaRecord } from '@/api/types';

export type SearchWhere = 'key' | 'headers' | 'value' | undefined;

/**
 * Applies the search term to the records that were already fetched
 * (the first `limit` records), looking in the place chosen in the
 * Search dropdown. undefined means Anywhere. Matching is case-insensitive.
 */
export function filterMessages(
  messages: KafkaRecord[],
  query: string | undefined,
  where: SearchWhere
): KafkaRecord[] {
  const term = query?.trim().toLowerCase();
  if (!term) {
    return messages;
  }

  const matches = (value: unknown): boolean =>
    value !== null &&
    value !== undefined &&
    String(value).toLowerCase().includes(term);

  return messages.filter((message) => {
    const { key, value, headers } = message.attributes;

    const inKey = () => matches(key);
    const inValue = () => matches(value);
    const inHeaders = () =>
      Object.entries(headers ?? {}).some(
        ([name, headerValue]) => matches(name) || matches(headerValue)
      );

    switch (where) {
      case 'key':
        return inKey();
      case 'value':
        return inValue();
      case 'headers':
        return inHeaders();
      default:
        return inKey() || inValue() || inHeaders();
    }
  });
}
