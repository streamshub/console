'use client'

import type { ToolbarToggleGroupProps } from '@/libs/patternfly/react-core'
import {
  InputGroup,
  ToolbarFilter,
  ToolbarGroup,
  ToolbarItem,
  ToolbarToggleGroup,
} from '@/libs/patternfly/react-core'
import { FilterIcon } from '@/libs/patternfly/react-icons'
import { CSSProperties, useState } from 'react'
import {
  FilterCheckbox,
  FilterSearch,
  FilterSelect,
  FilterSwitcher,
  FilterGroupedCheckbox,
} from './components'
import type { CheckboxType, FilterType, GroupedCheckboxType } from './types'

export type ChipFilterProps = {
  filters: { [label: string]: FilterType }
  breakpoint?: ToolbarToggleGroupProps['breakpoint']
}

export function ChipFilter({ filters, breakpoint = 'md' }: ChipFilterProps) {
  const options = Object.keys(filters)
  const [selectedOption, setSelectedOption] = useState<string>(options[0])

  const getFilterComponent = (label: string, f: FilterType) => {
    switch (f.type) {
      case 'search':
        return (
          <FilterSearch
            onSearch={f.onSearch}
            label={label}
            validate={f.validate}
            errorMessage={f.errorMessage}
          />
        )
      case 'checkbox':
        return (
          <FilterCheckbox
            chips={f.chips}
            options={f.options}
            onToggle={f.onToggle}
            label={label}
          />
        )
      case 'select':
        return (
          <FilterSelect
            chips={f.chips}
            options={f.options}
            onToggle={f.onToggle}
            label={label}
          />
        )
      case 'groupedCheckbox':
        return (
          <FilterGroupedCheckbox
            chips={f.chips}
            options={f.options}
            onToggle={f.onToggle}
            label={label}
          />
        )
    }
  }

  const getToolbarChips = (f: FilterType) => {
    if ('options' in f) {
      if (f.type === 'groupedCheckbox') {
        const groupedFilter = f as GroupedCheckboxType<any>
        const allOptions = groupedFilter.options.flatMap((group) =>
          Object.entries(group.groupOptions),
        )
        return groupedFilter.chips.map((c) => {
          const found = allOptions.find(([key]) => key === c)
          return {
            key: c,
            node: found ? found[1] : c,
          }
        })
      }
      const checkboxFilter = f as CheckboxType<any> // Type assertion
      return checkboxFilter.chips.map((c) => ({
        key: c,
        node: checkboxFilter.options[c]?.label ?? c,
      }))
    }
    return f.chips.map((chip) => ({ key: chip, node: chip }))
  }

  return (
    <>
      <ToolbarItem
        variant={'label'}
        visibility={{ default: 'hidden', [breakpoint]: 'visible' }}
        data-testid={'large-viewport-toolbar'}
        style={
          {
            '--pf-v6-c-toolbar__item--Width': '400px',
          } as CSSProperties
        }
      >
        <InputGroup>
          {options.length > 1 && (
            <FilterSwitcher
              options={options}
              value={selectedOption}
              onChange={setSelectedOption}
              ouiaId={'chip-filter-selector-large-viewport'}
            />
          )}
          {getFilterComponent(selectedOption, filters[selectedOption])}
        </InputGroup>
      </ToolbarItem>
      <ToolbarToggleGroup
        toggleIcon={<FilterIcon />}
        breakpoint={breakpoint}
        visibility={{ default: 'visible', [breakpoint]: 'hidden' }}
      >
        <ToolbarGroup variant="filter-group">
          {options.length > 1 && (
            <FilterSwitcher
              options={options}
              value={selectedOption}
              onChange={setSelectedOption}
              ouiaId={'chip-filter-selector-small-viewport'}
            />
          )}
          {Object.entries(filters).map(([label, f], index) => (
            <ToolbarFilter
              key={index}
              labels={getToolbarChips(f)}
              deleteLabel={(_, chip) =>
                f.onRemoveChip(typeof chip === 'string' ? chip : chip.key)
              }
              deleteLabelGroup={f.onRemoveGroup}
              categoryName={label}
              showToolbarItem={label === selectedOption}
            >
              {label === selectedOption && getFilterComponent(label, f)}
            </ToolbarFilter>
          ))}
        </ToolbarGroup>
      </ToolbarToggleGroup>
    </>
  )
}
