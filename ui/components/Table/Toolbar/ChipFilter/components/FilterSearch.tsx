import { useTranslations } from "next-intl";
import type { FunctionComponent } from "react";
import { SearchInput } from "../../SearchInput";
import type { SearchType } from "../types";

export const FilterSearch: FunctionComponent<
  Pick<SearchType, "onSearch" | "validate" | "errorMessage"> & { label: string }
> = ({ label, onSearch, validate, errorMessage }) => {
  const t = useTranslations("common");

  return (
    <SearchInput
      placeholder={t("search_hint", { label: label.toLocaleLowerCase() })}
      onSearch={onSearch}
      validate={validate}
      errorMessage={errorMessage}
    />
  );
};
