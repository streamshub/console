import {
  MenuToggle,
  MenuToggleElement,
  Select,
  SelectList,
  SelectOption,
  ToolbarItem,
} from '@/libs/patternfly/react-core'
import { useTranslations } from 'next-intl'
import { useState } from 'react'

export function FilterByBroker({
  selectedBroker,
  brokerList,
  onSetSelectedBroker,
  disableToolbar,
}: {
  selectedBroker: string | undefined
  brokerList: string[]
  onSetSelectedBroker: (value: string | undefined) => void
  disableToolbar: boolean
}) {
  const t = useTranslations('metrics')
  const [isBrokerSelectOpen, setIsBrokerSelectOpen] = useState(false)

  const onToggleClick = () => setIsBrokerSelectOpen((prev) => !prev)

  const onBrokerSelect = (
    _event: React.MouseEvent<Element, MouseEvent> | undefined,
    value: string | number | undefined,
  ) => {
    if (value === t('all_brokers')) {
      onSetSelectedBroker(undefined)
    } else {
      onSetSelectedBroker(value as string)
    }
    setIsBrokerSelectOpen(false)
  }

  // Define the toggle (new API pattern)
  const toggle = (toggleRef: React.Ref<MenuToggleElement>) => (
    <MenuToggle
      ouiaId={'filter-by-broker-toggle'}
      ref={toggleRef}
      onClick={onToggleClick}
      isExpanded={isBrokerSelectOpen}
      isDisabled={disableToolbar || brokerList.length === 0}
    >
      {selectedBroker || t('all_brokers')}
    </MenuToggle>
  )

  return (
    <ToolbarItem>
      <Select
        id="broker-select"
        isOpen={isBrokerSelectOpen}
        selected={selectedBroker || t('all_brokers')}
        onSelect={onBrokerSelect}
        onOpenChange={setIsBrokerSelectOpen}
        toggle={toggle}
        shouldFocusToggleOnSelect
      >
        <SelectList>
          <SelectOption value={t('all_brokers')}>
            {t('all_brokers')}
          </SelectOption>
          {brokerList.map((broker, index) => (
            <SelectOption key={`broker-${index}`} value={broker}>
              {broker}
            </SelectOption>
          ))}
        </SelectList>
      </Select>
    </ToolbarItem>
  )
}
