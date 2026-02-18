import React, { useState } from 'react'
import {
  Select,
  SelectOption,
  SelectProps,
  SelectList,
  MenuToggle,
  MenuToggleElement,
  TextInputGroup,
  TextInputGroupMain,
  TextInputGroupUtilities,
  Button,
} from '@/libs/patternfly/react-core'
import { TimesIcon } from '@/libs/patternfly/react-icons'

export function TypeaheadSelect({
  value,
  selectItems,
  onChange,
  placeholder,
}: {
  value: string | number
  selectItems: (string | number)[]
  onChange: (item: string | number) => void
  placeholder: string
}) {
  const [isOpen, setIsOpen] = useState<boolean>(false)
  const [inputValue, setInputValue] = useState<string>(value?.toString() ?? '')
  const [filterValue, setFilterValue] = useState<string>('')
  const [focusedItemIndex, setFocusedItemIndex] = useState<number | null>(null)
  const [activeItemId, setActiveItemId] = useState<string | null>(null)
  const textInputRef = React.useRef<HTMLInputElement>(null)

  const NO_RESULTS = 'no results'

  const uniqueItems = Array.from(new Set(selectItems))

  React.useEffect(() => {
    setInputValue(value?.toString() ?? '')
  }, [value])

  const onToggleClick = () => {
    setIsOpen((prev) => !prev)
    setFilterValue('')
    textInputRef.current?.focus()
  }

  const onSelect: SelectProps['onSelect'] = (_event, selection) => {
    if (selection !== NO_RESULTS) {
      const stringSelection = selection.toString()
      onChange(selection as string | number)
      setInputValue(stringSelection)
      setFilterValue('')
    }
    setIsOpen(false)
  }

  const filteredItems = filterValue
    ? uniqueItems.filter((item) =>
        item.toString().toLowerCase().includes(filterValue.toLowerCase()),
      )
    : uniqueItems

  const options = filteredItems.length
    ? filteredItems.map((item) => (
        <SelectOption key={item} value={item}>
          {item}
        </SelectOption>
      ))
    : [
        <SelectOption key={NO_RESULTS} value={NO_RESULTS} isAriaDisabled>
          {`No results found for "${filterValue}"`}
        </SelectOption>,
      ]

  const onInputChange = (
    _event: React.FormEvent<HTMLInputElement>,
    val: string,
  ) => {
    setInputValue(val)
    setFilterValue(val)
    setFocusedItemIndex(null)
    onChange(val)

    if (!isOpen && val !== '') {
      setIsOpen(true)
    }
  }

  const onInputKeyDown = (event: React.KeyboardEvent<HTMLInputElement>) => {
    if (event.key === 'Enter') {
      event.preventDefault()

      if (isOpen) {
        const itemToSelect =
          focusedItemIndex !== null
            ? filteredItems[focusedItemIndex]
            : filteredItems.length > 0
            ? filteredItems[0]
            : null

        if (itemToSelect && itemToSelect !== NO_RESULTS) {
          onChange(itemToSelect)
          setFilterValue(itemToSelect.toString())
        } else {
          onChange(filterValue)
        }
        setIsOpen(false)
      }
    }

    if (event.key === 'ArrowDown') {
      event.preventDefault()
      if (!isOpen) setIsOpen(true)
      setFocusedItemIndex((prev) =>
        prev === null || prev === filteredItems.length - 1 ? 0 : prev + 1,
      )
    }

    if (event.key === 'ArrowUp') {
      event.preventDefault()
      setFocusedItemIndex((prev) =>
        prev === null || prev === 0 ? filteredItems.length - 1 : prev - 1,
      )
    }
  }

  const onClearButtonClick = () => {
    setInputValue('')
    setFilterValue('')
    setFocusedItemIndex(null)
    setActiveItemId(null)
    onChange('')
    textInputRef.current?.focus()
  }
  const toggle = (toggleRef: React.Ref<MenuToggleElement>) => (
    <MenuToggle
      ouiaId={'type-ahead-select'}
      ref={toggleRef}
      variant="typeahead"
      aria-label="Typeahead menu toggle"
      onClick={onToggleClick}
      isExpanded={isOpen}
      isFullWidth
    >
      <TextInputGroup isPlain>
        <TextInputGroupMain
          value={inputValue}
          onClick={() => setIsOpen(true)}
          onChange={onInputChange}
          onKeyDown={onInputKeyDown}
          onBlur={() => {
            onChange(filterValue)
          }}
          id="typeahead-select-input"
          autoComplete="off"
          ref={textInputRef}
          placeholder={placeholder}
          {...(activeItemId && { 'aria-activedescendant': activeItemId })}
          role="combobox"
          isExpanded={isOpen}
          aria-controls="typeahead-select-listbox"
        />
        <TextInputGroupUtilities>
          {filterValue && (
            <Button
              ouiaId={'type-ahead-select-button'}
              icon={<TimesIcon aria-hidden />}
              variant="plain"
              onClick={onClearButtonClick}
              aria-label="Clear input value"
            />
          )}
        </TextInputGroupUtilities>
      </TextInputGroup>
    </MenuToggle>
  )

  return (
    <Select
      id="typeahead-select"
      isOpen={isOpen}
      selected={value ? [value] : []}
      onSelect={onSelect}
      onOpenChange={(isOpen) => setIsOpen(!isOpen)}
      toggle={toggle}
      shouldFocusFirstItemOnOpen={false}
    >
      <SelectList id="typeahead-select-listbox">{options}</SelectList>
    </Select>
  )
}
