/**
 * Root App component
 * Provides the main layout and outlet for child routes
 */

import { Outlet } from 'react-router-dom';
import { AppLayoutProvider } from '@/components/app/AppLayoutProvider';
import { ThemeProvider } from '@/components/app/ThemeProvider';

function App() {
  return (
    <ThemeProvider>
      <AppLayoutProvider>
        <Outlet />
      </AppLayoutProvider>
    </ThemeProvider>
  );
}

export default App;