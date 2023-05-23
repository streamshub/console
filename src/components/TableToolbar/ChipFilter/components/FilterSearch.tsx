import type { VoidFunctionComponent } from "react";
import { SearchInput } from "../../SearchInput";
import type { SearchType } from "../types";

export const FilterSearch: VoidFunctionComponent<
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
