import type { FunctionComponent } from "react";
import { SearchInput } from "../../SearchInput";
import type { SearchType } from "../types";

export const FilterSearch: FunctionComponent<
  Pick<SearchType, "onSearch" | "validate" | "errorMessage"> & { label: string }
> = ({ label, onSearch, validate, errorMessage }) => {
  return (
    <SearchInput
      placeholder={`Search for ${label}`}
      onSearch={onSearch}
      validate={validate}
      errorMessage={errorMessage}
    />
  );
};
