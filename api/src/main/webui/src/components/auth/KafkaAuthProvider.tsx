import { createContext, useContext, useState, useCallback, ReactNode } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQueryClient } from '@tanstack/react-query';
import { KafkaLoginModal } from './KafkaLoginModal';
import { QueryErrorHandler } from './QueryErrorHandler';
import { useKafkaAuth } from '@/api/hooks/useKafkaAuth';
import { ApiError } from '@/api/client';

interface AuthState {
  isModalOpen: boolean;
  clusterId: string;
  clusterName: string;
  authMethod?: string;
  error?: string;
  pendingNavigation?: string;
}

const initialAuthState = {
  isModalOpen: false,
  clusterId: '',
  clusterName: '',
  authMethod: undefined,
  error: undefined,
  pendingNavigation: undefined,
};

export type KafkaAuthShowLoginModalType = (
  clusterId: string,
  clusterName: string,
  authMethod?: string,
  pendingNavigation?: string
) => void;

interface KafkaAuthContextType {
  showLoginModal: (clusterId: string, clusterName: string, authMethod?: string, pendingNavigation?: string) => void;
  hideLoginModal: () => void;
  handleAuthError: (error: unknown, clusterId?: string) => boolean;
}

const KafkaAuthContext = createContext<KafkaAuthContextType | undefined>(undefined);

export function useKafkaAuthContext() {
  const context = useContext(KafkaAuthContext);
  if (!context) {
    throw new Error('useKafkaAuthContext must be used within KafkaAuthProvider');
  }
  return context;
}

interface KafkaAuthProviderProps {
  children: ReactNode;
}

export function KafkaAuthProvider({ children }: KafkaAuthProviderProps) {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { kafkaId } = useParams<{ kafkaId?: string }>();
  
  const [authState, setAuthState] = useState<AuthState>(initialAuthState);

  const { mutateAsync: authenticate } = useKafkaAuth(authState.clusterId);

  const showLoginModal = useCallback((clusterId: string, clusterName: string, authMethod?: string, pendingNavigation?: string) => {
    setAuthState({
      isModalOpen: true,
      clusterId,
      clusterName,
      authMethod,
      error: undefined,
      pendingNavigation,
    });
  }, []);

  const hideLoginModal = useCallback(() => {
    setAuthState(initialAuthState);
  }, []);

  const handleAuthError = useCallback((error: unknown, clusterId?: string): boolean => {
    // Check if this is a 401 error
    if (error instanceof ApiError && error.status === 401) {
      const targetClusterId = clusterId || kafkaId;
      
      if (targetClusterId) {
        // Get cluster name from the error context or use clusterId as fallback
        const clusterName = targetClusterId;
        showLoginModal(targetClusterId, clusterName, window.location.pathname);
        return true; // Indicates the error was handled
      }
    }
    return false; // Indicates the error was not handled
  }, [kafkaId, showLoginModal]);

  const handleLogin = async (username: string, password: string) => {
    try {
      setAuthState(prev => ({ ...prev, error: undefined }));
      await authenticate({ username, password });
      
      // Invalidate all queries for this cluster to refresh data after successful login
      // This will refetch any data that was showing 401 errors
      await queryClient.invalidateQueries({
        predicate: (query) => {
          // Invalidate queries that have the clusterId in their query key
          const queryKey = query.queryKey;
          if (Array.isArray(queryKey)) {
            // Check if any element in the query key matches the cluster ID
            return queryKey.some(key => key === authState.clusterId);
          }
          return false;
        },
      });
      
      // On success, navigate to pending location or close modal
      if (authState.pendingNavigation) {
        navigate(authState.pendingNavigation);
      }
      
      hideLoginModal();
    } catch (error) {
      // Set error to display in modal
      if (error instanceof ApiError) {
        setAuthState(prev => ({
          ...prev,
          error: error.errors?.[0]?.detail ||
                 error.errors?.[0]?.title ||
                 'Invalid username or password',
        }));
      } else {
        setAuthState(prev => ({
          ...prev,
          error: 'Invalid username or password',
        }));
      }
      throw error; // Re-throw to let the modal handle loading state
    }
  };

  return (
    <KafkaAuthContext.Provider value={{ showLoginModal, hideLoginModal, handleAuthError }}>
      <QueryErrorHandler />
      {children}
      <KafkaLoginModal
        // key forces a clean state when the modal is re-opened
        key={authState.isModalOpen ? 'open' : 'closed'}
        isOpen={authState.isModalOpen}
        clusterName={authState.clusterName}
        authMethod={authState.authMethod}
        onClose={hideLoginModal}
        onLogin={handleLogin}
        error={authState.error}
      />
    </KafkaAuthContext.Provider>
  );
}