import type { CheckboxType, SearchType } from "./types";

export const sampleSearchFilter: SearchType = {
  errorMessage: "test",
  validate: () => true,
  type: "search",
  chips: [],
  // eslint-disable-next-line @typescript-eslint/no-empty-function
  onRemoveChip: () => {},
  // eslint-disable-next-line @typescript-eslint/no-empty-function
  onRemoveGroup: () => {},
  // eslint-disable-next-line @typescript-eslint/no-empty-function
  onSearch: () => {},
};
export const sampleSearchFilterWithChips: SearchType = {
  ...sampleSearchFilter,
  chips: ["foo"],
};

export const sampleCheckboxFilter: CheckboxType<any> = {
  type: "checkbox",
  chips: [],
  // eslint-disable-next-line @typescript-eslint/no-empty-function
  onRemoveChip: () => {},
  // eslint-disable-next-line @typescript-eslint/no-empty-function
  onRemoveGroup: () => {},
  // eslint-disable-next-line @typescript-eslint/no-empty-function
  onToggle: () => {},
  options: {
    "opt-1": "Option 1",
    "opt-2": "Option 2",
  },
};
export const sampleCheckboxFilterWithChips: CheckboxType<any> = {
  ...sampleCheckboxFilter,
  chips: ["opt-2"],
};
