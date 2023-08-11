import type { VoidFunctionComponent } from "react";
import { useTranslations } from "next-intl";
import { SearchInput } from "../../SearchInput";
import type { SearchType } from "../types";

export const FilterSearch: VoidFunctionComponent<
  Pick<SearchType, "onSearch" | "validate" | "errorMessage"> & { label: string }
> = ({ label, onSearch, validate, errorMessage }) => {
  const t = useTranslations();

  return (
    <SearchInput
      placeholder={t("common:search_hint", { label })}
      onSearch={onSearch}
      validate={validate}
      errorMessage={errorMessage}
    />
  );
};
