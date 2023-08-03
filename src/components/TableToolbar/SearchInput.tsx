import {
  Button,
  ButtonVariant,
  InputGroup,
  TextInput,
  Tooltip,
  ValidatedOptions,
} from "@patternfly/react-core";
import { ArrowRightIcon } from "@patternfly/react-icons";
import type { KeyboardEvent, FunctionComponent } from "react";
import { useCallback, useRef, useState } from "react";

export type SearchInputProps = {
  placeholder: string;
  errorMessage: string;
  validate: (value: string) => boolean;
  onSearch: (value: string) => void;
};

export const SearchInput: FunctionComponent<SearchInputProps> = ({
  placeholder,
  errorMessage,
  validate,
  onSearch,
}) => {
  const [value, setValue] = useState("");
  const inputRef = useRef<HTMLInputElement>(null);

  const isEmpty = value.length === 0;
  const isValid = !isEmpty && validate(value);
  const showErrorFeedback = !isEmpty && !isValid;
  const canSearch = !isEmpty && isValid;

  const handleSearch = useCallback(() => {
    if (canSearch) {
      onSearch(value);
      setValue("");
    }
  }, [canSearch, onSearch, value]);

  const onKeyPress = useCallback(
    (event: KeyboardEvent) => {
      if (event.key === "Enter") {
        handleSearch();
      }
    },
    [handleSearch]
  );

  return (
    <InputGroup>
      <TextInput
        name="search"
        id="search"
        type="search"
        aria-label={placeholder}
        placeholder={placeholder}
        validated={
          showErrorFeedback ? ValidatedOptions.error : ValidatedOptions.default
        }
        value={value}
        onChange={setValue}
        onKeyPress={onKeyPress}
        ref={inputRef}
      />
      <Button
        isDisabled={!canSearch}
        variant={ButtonVariant.control}
        onClick={handleSearch}
        aria-label={"Search"}
      >
        <ArrowRightIcon />
      </Button>
      <Tooltip
        trigger="manual"
        isVisible={showErrorFeedback}
        content={errorMessage}
        reference={inputRef}
      />
    </InputGroup>
  );
};
