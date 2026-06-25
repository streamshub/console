/**
 * Root App component
 * Provides the main layout and outlet for child routes
 */

import { Outlet } from 'react-router-dom';
import { AppLayoutProvider } from '@/components/app/AppLayoutProvider';
import { ThemeProvider } from '@/components/app/ThemeProvider';
import { KafkaAuthProvider } from '@/components/auth/KafkaAuthProvider';

function App() {
  return (
    <ThemeProvider>
      <AppLayoutProvider>
        <KafkaAuthProvider>
          <Outlet />
        </KafkaAuthProvider>
      </AppLayoutProvider>
    </ThemeProvider>
  );
}

export default App;