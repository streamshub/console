import { useTheme } from "./useTheme";

export const useColorTheme = () => {
  const { mode, setMode, modes, isDarkMode } = useTheme();

  const isLightMode = mode === modes.LIGHT;
  const isSystemMode = mode === modes.SYSTEM;

  const toggleDarkMode = (enableDark: boolean) => {
    setMode(enableDark ? modes.DARK : modes.LIGHT);
  };

  const setSystemMode = () => setMode(modes.SYSTEM);

  const cycleTheme = () => {
    if (isSystemMode) setMode(modes.LIGHT);
    else if (isLightMode) setMode(modes.DARK);
    else setMode(modes.SYSTEM);
  };

  return {
    mode,
    setMode,
    isDarkMode,
    isLightMode,
    isSystemMode,
    toggleDarkMode,
    setSystemMode,
    cycleTheme,
    modes,
  };
};
