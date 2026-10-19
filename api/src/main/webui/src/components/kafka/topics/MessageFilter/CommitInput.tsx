import { useState } from 'react';
import { TextInput, TextInputProps } from '@patternfly/react-core';

interface CommitInputProps
  extends Omit<TextInputProps, 'value' | 'onChange' | 'onBlur' | 'onKeyDown'> {
  value: string;
  onCommit: (value: string) => void;
  isValid?: (value: string) => boolean;
  sanitize?: (raw: string) => string;
}

export function CommitInput({
  value,
  onCommit,
  isValid = () => true,
  sanitize,
  ...props
}: CommitInputProps) {
  const [draft, setDraft] = useState(value);
  const [prevValue, setPrevValue] = useState(value);
  if (value !== prevValue) {
    setPrevValue(value);
    setDraft(value);
  }

  const valid = isValid(draft);

  // Leaving the box without changing anything is not a new search
  const commit = () => {
    if (valid && draft !== value) onCommit(draft);
  };

  return (
    <TextInput
      {...props}
      value={draft}
      validated={valid ? 'default' : 'error'}
      onChange={(_, next) => setDraft(sanitize ? sanitize(next) : next)}
      onBlur={commit}
      onKeyDown={(e) => {
        if (e.key === 'Enter') commit();
      }}
    />
  );
}
