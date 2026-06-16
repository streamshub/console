/* eslint-disable */
// @ts-nocheck
// This file is adapted from PatternFly with minimal changes
/**
 * This component was copied from @patternfly/react-data-view and modified to support arbitrary children,
 * e.g. to allow filters to render icons. The original component uses JSON.stringify to generate the childrenHash,
 * which breaks with a cyclic object error for some React component children.
 * 
 * This component should be kept in sync with the original component and removed if and when the original no longer
 * uses JSON.stringify.
 */
import { Children, isValidElement, cloneElement, useMemo, useState, useRef, useEffect, ReactElement, ReactNode } from 'react';
import {
  Menu, MenuContent, MenuItem, MenuList, MenuToggle, Popper, ToolbarGroup, ToolbarToggleGroup, ToolbarToggleGroupProps,
} from '@patternfly/react-core';
import { FilterIcon } from '@patternfly/react-icons';

export interface DataViewFilterOption {
  /** Filter option label */
  label: ReactNode;
  /** Filter option value */
  value: string;
}

// helper interface to generate attribute menu
interface DataViewFilterIdentifiers {
  filterId: string;
  title: string;
}

/** extends ToolbarToggleGroupProps */
export interface DataViewFiltersProps<T extends object> extends Omit<ToolbarToggleGroupProps, 'toggleIcon' | 'breakpoint' | 'onChange'> {
  /** Content rendered inside the data view */
  children: React.ReactNode;
  /** Optional onChange callback shared across filters */
  onChange?: (key: string, newValues: Partial<T>) => void;
  /** Optional values shared across filters */
  values?: T;
  /** Icon for the toolbar toggle group */
  toggleIcon?: ToolbarToggleGroupProps['toggleIcon'];
  /** Breakpoint for the toolbar toggle group */
  breakpoint?: ToolbarToggleGroupProps['breakpoint'];
  /** Custom OUIA ID */
  ouiaId?: string;
};


export const DataViewFilters = <T extends object>({
  children,
  ouiaId = 'DataViewFilters',
  toggleIcon = <FilterIcon />,
  breakpoint = 'xl',
  onChange,
  values,
  ...props
}: DataViewFiltersProps<T>) => {
  const [ activeAttributeMenu, setActiveAttributeMenu ] = useState<string>('');
  const [ isAttributeMenuOpen, setIsAttributeMenuOpen ] = useState(false);
  const attributeToggleRef = useRef<HTMLButtonElement>(null);
  const attributeMenuRef = useRef<HTMLDivElement>(null);
  const attributeContainerRef = useRef<HTMLDivElement>(null);

  const childrenHash = useMemo(() =>
    Children.map(children, (child) =>
      isValidElement(child) ? { type: child.type, key: child.key, props: child.props } : child
    )
  , [ children ]);

  const filterItems: DataViewFilterIdentifiers[] = useMemo(() => Children.toArray(children)
    .map(child =>
      isValidElement(child) ? { filterId: String((child.props as any).filterId), title: String((child.props as any).title) } : undefined
    ).filter((item): item is DataViewFilterIdentifiers => !!item), [ childrenHash ]);

  useEffect(() => {
    filterItems.length > 0 && setActiveAttributeMenu(filterItems[0].title);
  }, [ filterItems ]);

  const handleClickOutside = (event: MouseEvent) => 
    isAttributeMenuOpen &&
    !attributeMenuRef.current?.contains(event.target as Node) &&
    !attributeToggleRef.current?.contains(event.target as Node)
    && setIsAttributeMenuOpen(false);

  useEffect(() => {
    window.addEventListener('click', handleClickOutside);
    return () => {
      window.removeEventListener('click', handleClickOutside);
    };
  }, [ isAttributeMenuOpen ]);

  const attributeToggle = (
    <MenuToggle
      ref={attributeToggleRef}
      onClick={() => setIsAttributeMenuOpen(!isAttributeMenuOpen)}
      isExpanded={isAttributeMenuOpen}
      icon={toggleIcon}
    >
      {activeAttributeMenu}
    </MenuToggle>
  );

  const attributeMenu = (
    <Menu
      ref={attributeMenuRef}
      onSelect={(_ev, itemId) => {
        const selectedItem = filterItems.find(item => item.filterId === itemId);
        selectedItem && setActiveAttributeMenu(selectedItem.title);
        setIsAttributeMenuOpen(false);
      }}
    >
      <MenuContent>
        <MenuList>
          {filterItems.map(item => (
            <MenuItem key={item.filterId} itemId={item.filterId}>
              {item.title}
            </MenuItem>
          ))}
        </MenuList>
      </MenuContent>
    </Menu>
  );

  return (
    <ToolbarToggleGroup data-ouia-component-id={ouiaId} toggleIcon={toggleIcon} breakpoint={breakpoint} {...props}>
      <ToolbarGroup variant="filter-group">
        <div ref={attributeContainerRef}>
          <Popper
            trigger={attributeToggle}
            triggerRef={attributeToggleRef}
            popper={attributeMenu}
            popperRef={attributeMenuRef}
            appendTo={attributeContainerRef.current || undefined}
            isVisible={isAttributeMenuOpen}
          />
        </div>
        {Children.map(children, (child) =>
          isValidElement(child)
            ? cloneElement(child as ReactElement<{
              showToolbarItem: boolean;
              onChange: (_e: unknown, values: unknown) => void;
              value: unknown;
            }>, {
              showToolbarItem: activeAttributeMenu === (child.props as any).title,
              onChange: (event, value) => onChange?.((child.props as any).filterId, { [(child.props as any).filterId]: value } as Partial<T>),
              value: values?.[(child.props as any).filterId],
              ...(child.props as any)
            })
            : child
        )}
      </ToolbarGroup>
    </ToolbarToggleGroup>
  );
};

export default DataViewFilters;
