import { createContext, useContext, useEffect, useState, PropsWithChildren } from 'react';

type ThemeMode = 'system' | 'light' | 'dark';

interface ThemeContextType {
  mode: ThemeMode;
  setMode: (mode: ThemeMode) => void;
  isDarkMode: boolean;
  brandLogo: string;
}

const ThemeContext = createContext<ThemeContextType | undefined>(undefined);

const THEME_STORAGE_KEY = 'pf-theme-mode';

function getSystemTheme(): 'light' | 'dark' {
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
}

function getEffectiveTheme(mode: ThemeMode): 'light' | 'dark' {
  return mode === 'system' ? getSystemTheme() : mode;
}

export function ThemeProvider({ children }: PropsWithChildren) {
  const [mode, setModeState] = useState<ThemeMode>(() => {
    const stored = localStorage.getItem(THEME_STORAGE_KEY);
    if (stored === 'light' || stored === 'dark' || stored === 'system') {
      return stored;
    }
    return 'system';
  });

  const [effectiveTheme, setEffectiveTheme] = useState<'light' | 'dark'>(() =>
    getEffectiveTheme(mode)
  );

  const setMode = (newMode: ThemeMode) => {
    setModeState(newMode);
    localStorage.setItem(THEME_STORAGE_KEY, newMode);
  };

  useEffect(() => {
    const updateTheme = () => {
      const theme = getEffectiveTheme(mode);
      setEffectiveTheme(theme);
      
      const root = document.documentElement;
      
      // Remove all theme classes
      root.classList.remove('pf-v6-theme-light', 'pf-v6-theme-dark');
      
      // Add current theme class
      root.classList.add(`pf-v6-theme-${theme}`);
    };

    updateTheme();

    // Listen for system theme changes when in system mode
    if (mode === 'system') {
      const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
      const handler = () => updateTheme();
      mediaQuery.addEventListener('change', handler);
      return () => mediaQuery.removeEventListener('change', handler);
    }
  }, [mode]);

  const brandLogo = effectiveTheme === 'dark'
    ? '/full_logo_hori_reverse.svg'
    : '/full_logo_hori_default.svg';

  return (
    <ThemeContext.Provider value={{ mode, setMode, isDarkMode: effectiveTheme === 'dark', brandLogo }}>
      {children}
    </ThemeContext.Provider>
  );
}

export function useTheme() {
  const context = useContext(ThemeContext);
  if (!context) {
    throw new Error('useTheme must be used within ThemeProvider');
  }
  return context;
}