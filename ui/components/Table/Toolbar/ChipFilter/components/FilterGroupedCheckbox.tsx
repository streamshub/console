import React, { useState } from 'react'
import {
  MenuToggle,
  Select,
  SelectOption,
  SelectList,
  SelectGroup,
  Divider,
} from '@/libs/patternfly/react-core'
import { useTranslations } from 'next-intl'
import { GroupedCheckboxType } from '../types'

export function FilterGroupedCheckbox<T extends string | number>({
  label,
  chips,
  options,
  onToggle,
  placeholder,
}: Pick<
  GroupedCheckboxType<any>,
  'chips' | 'options' | 'onToggle' | 'placeholder'
> & {
  label: string
}) {
  const t = useTranslations()
  const [isOpen, setIsOpen] = useState(false)

  return (
    <Select
      aria-label={label}
      popperProps={{
        appendTo: () => document.body,
        placement: "bottom-start",
        enableFlip: true,
        flipBehavior: ["bottom"],
        preventOverflow: true,
      }}
      onSelect={(_, value) => {
        onToggle(value as T)
        setIsOpen(false)
      }}
      selected={chips}
      isOpen={isOpen}
      toggle={(toggleRef) => (
        <MenuToggle
          ouiaId={'filter-group-checkbox-toggle'}
          ref={toggleRef}
          onClick={() => setIsOpen((o) => !o)}
          isExpanded={isOpen}
          style={{ width: '200px' } as React.CSSProperties}
        >
          {placeholder
            ? placeholder
            : t('common.search_hint', { label: label.toLocaleLowerCase() })}
        </MenuToggle>
      )}
    >
      {options.map((group, index) => (
        <React.Fragment key={group.groupLabel}>
          <SelectGroup label={group.groupLabel}>
            <SelectList>
              {Object.entries(group.groupOptions).map(([key, node]) => (
                <SelectOption
                  key={key}
                  value={key}
                  hasCheckbox={true}
                  isSelected={chips.includes(key)}
                >
                  {node}
                </SelectOption>
              ))}
            </SelectList>
          </SelectGroup>
          {index < options.length - 1 && <Divider />}
        </React.Fragment>
      ))}
    </Select>
  )
}
