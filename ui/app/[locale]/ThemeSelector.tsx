"use client";
import {
  Select,
  SelectGroup,
  SelectList,
  SelectOption,
  MenuToggle,
  Icon,
} from "@/libs/patternfly/react-core";
import { DesktopIcon, MoonIcon, SunIcon } from "@/libs/patternfly/react-icons";
import { useState } from "react";
import { useColorTheme } from "@/app/[locale]/useColorTheme";

export function ThemeSelector() {
  const { mode, setMode, modes } = useColorTheme();
  const [isOpen, setIsOpen] = useState(false);

  const getThemeIcon = (themeMode: string) => {
    switch (themeMode) {
      case modes.LIGHT:
        return <SunIcon />;
      case modes.DARK:
        return <MoonIcon />;
      default:
        return <DesktopIcon />;
    }
  };

  const getThemeLabel = (themeMode: string) => {
    switch (themeMode) {
      case modes.LIGHT:
        return "Light";
      case modes.DARK:
        return "Dark";
      default:
        return "System";
    }
  };

  return (
    <Select
      id="theme-select"
      isOpen={isOpen}
      selected={mode}
      onSelect={(_e, selected) => {
        setMode(selected as any);
        setIsOpen(false);
      }}
      onOpenChange={setIsOpen}
      toggle={(toggleRef) => (
        <MenuToggle
          ref={toggleRef}
          onClick={() => setIsOpen(!isOpen)}
          isExpanded={isOpen}
          icon={<Icon size="lg">{getThemeIcon(mode)}</Icon>}
          aria-label={`Theme selection, current: ${getThemeLabel(mode)}`}
        />
      )}
    >
      <SelectGroup>
        <SelectList aria-label="Theme switcher">
          <SelectOption
            value={modes.SYSTEM}
            icon={<DesktopIcon />}
            description="Follow system preference"
          >
            System
          </SelectOption>
          <SelectOption
            value={modes.LIGHT}
            icon={<SunIcon />}
            description="Always use light mode"
          >
            Light
          </SelectOption>
          <SelectOption
            value={modes.DARK}
            icon={<MoonIcon />}
            description="Always use dark mode"
          >
            Dark
          </SelectOption>
        </SelectList>
      </SelectGroup>
    </Select>
  );
}
