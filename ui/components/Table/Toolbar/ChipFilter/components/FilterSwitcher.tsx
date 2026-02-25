import type { SelectProps } from '@/libs/patternfly/react-core'
import {
  MenuToggle,
  Select,
  SelectList,
  SelectOption,
} from '@/libs/patternfly/react-core'
import { FilterIcon } from '@/libs/patternfly/react-icons'
import { useState } from 'react'

export function FilterSwitcher({
  options,
  value,
  onChange,
  ouiaId,
}: {
  options: string[]
  value: string
  onChange: (value: string) => void
  ouiaId: string
}) {
  const [isOpen, setIsOpen] = useState(false)

  const onSelect: SelectProps['onSelect'] = (_, value) => {
    onChange(value as string)
    setIsOpen(false)
  }

  return (
    <Select
      popperProps={{ appendTo: 'inline' }}
      toggle={(toggleRef) => (
        <MenuToggle
          ouiaId={'filter-switcher-toggle'}
          ref={toggleRef}
          onClick={() => setIsOpen((o) => !o)}
          isExpanded={isOpen}
          variant={'default'}
          icon={<FilterIcon />}
          style={{ minWidth: 'calc(0.875rem * 2.5 + 5.9375rem)' }}
        >
          {value}
        </MenuToggle>
      )}
      aria-label={'table:select_filter'}
      selected={value}
      isOpen={isOpen}
      onSelect={onSelect}
      ouiaId={ouiaId}
    >
      <SelectList>
        {options.map((option, index) => (
          <SelectOption value={option} key={index}>
            {option}
          </SelectOption>
        ))}
      </SelectList>
    </Select>
  )
}
