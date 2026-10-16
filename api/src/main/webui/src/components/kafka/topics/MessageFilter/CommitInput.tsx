import { useState } from 'react';
import { TextInput, TextInputProps } from '@patternfly/react-core';

interface CommitInputProps
  extends Omit<TextInputProps, 'value' | 'onChange' | 'onBlur' | 'onKeyDown'> {
  value: string;
  onCommit: (value: string) => void;
  isValid?: (value: string) => boolean;
}

export function CommitInput({
  value,
  onCommit,
  isValid = () => true,
  ...props
}: CommitInputProps) {
  const [draft, setDraft] = useState(value);
  const [prevValue, setPrevValue] = useState(value);
  if (value !== prevValue) {
    setPrevValue(value);
    setDraft(value);
  }

  const valid = isValid(draft);
  const commit = () => {
    if (valid) onCommit(draft);
  };

  return (
    <TextInput
      {...props}
      value={draft}
      validated={valid ? 'default' : 'error'}
      onChange={(_, next) => setDraft(next)}
      onBlur={commit}
      onKeyDown={(e) => {
        if (e.key === 'Enter') commit();
      }}
    />
  );
}
