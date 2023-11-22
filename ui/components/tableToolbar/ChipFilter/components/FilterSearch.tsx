import { useTranslations } from "next-intl";
import type { VoidFunctionComponent } from "react";
import { SearchInput } from "../../SearchInput";
import type { SearchType } from "../types";

export const FilterSearch: VoidFunctionComponent<
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
