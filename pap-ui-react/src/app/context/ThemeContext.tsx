// Theme provider for MUI with light/dark mode support

import React, { createContext, useContext, useMemo, useState, useEffect } from 'react';
import {
  ThemeProvider as MuiThemeProvider,
  createTheme,
  type Theme,
  CssBaseline,
} from '@mui/material';
import { settingsApi } from '../api/settings';
import { getAccessToken } from '../api/client';

interface AppSettings { theme: 'light' | 'dark'; colorScheme: string; }

interface ThemeContextType {
  settings: AppSettings;
  toggleTheme: () => void;
  setColorScheme: (scheme: string) => void;
}

const ThemeContext = createContext<ThemeContextType | undefined>(undefined);

const colorSchemes: Record<string, { primary: string; secondary: string }> = {
  blue: {
    primary: '#1976d2',
    secondary: '#dc004e',
  },
  green: {
    primary: '#2e7d32',
    secondary: '#f57c00',
  },
  purple: {
    primary: '#7b1fa2',
    secondary: '#0288d1',
  },
  teal: {
    primary: '#00796b',
    secondary: '#c2185b',
  },
};

export function ThemeProvider({ children }: { children: React.ReactNode }) {
  const [settings, setSettings] = useState<AppSettings>(() => ({
    theme: (localStorage.getItem('pap_theme') as 'light' | 'dark') ?? 'light',
    colorScheme: localStorage.getItem('pap_colorScheme') ?? 'blue',
  }));

  // Sync to backend when authenticated
  useEffect(() => {
    localStorage.setItem('pap_theme', settings.theme);
    localStorage.setItem('pap_colorScheme', settings.colorScheme);
    if (getAccessToken()) {
      settingsApi.update({ theme: settings.theme, colorScheme: settings.colorScheme }).catch(() => {});
    }
  }, [settings]);

  const theme = useMemo<Theme>(() => {
    const colors = colorSchemes[settings.colorScheme] || colorSchemes.blue;

    return createTheme({
      palette: {
        mode: settings.theme,
        primary: {
          main: colors.primary,
        },
        secondary: {
          main: colors.secondary,
        },
      },
      typography: {
        fontFamily: '"Inter", "Roboto", "Helvetica", "Arial", sans-serif',
      },
      components: {
        MuiButton: {
          styleOverrides: {
            root: {
              textTransform: 'none',
            },
          },
        },
        MuiTab: {
          styleOverrides: {
            root: {
              textTransform: 'none',
            },
          },
        },
      },
    });
  }, [settings.theme, settings.colorScheme]);

  const toggleTheme = () => {
    setSettings(prev => ({
      ...prev,
      theme: prev.theme === 'light' ? 'dark' : 'light',
    }));
  };

  const setColorScheme = (scheme: string) => {
    setSettings(prev => ({
      ...prev,
      colorScheme: scheme,
    }));
  };

  return (
    <ThemeContext.Provider value={{ settings, toggleTheme, setColorScheme }}>
      <MuiThemeProvider theme={theme}>
        <CssBaseline />
        {children}
      </MuiThemeProvider>
    </ThemeContext.Provider>
  );
}

export function useThemeSettings() {
  const context = useContext(ThemeContext);
  if (!context) {
    throw new Error('useThemeSettings must be used within ThemeProvider');
  }
  return context;
}
