/**
 * Simple API client for fetching data from the backend
 */

export interface ApiResponse<T> {
  data?: T;
  errors?: ApiError[];
}

export interface ApiError {
  status: number;
  code: string;
  title: string;
  detail?: string;
}

export class ApiClient {
  private baseUrl: string;

  constructor(baseUrl: string = '/api') {
    this.baseUrl = baseUrl;
  }

  async fetch<T>(path: string, options: RequestInit = {}): Promise<ApiResponse<T>> {
    try {
      const response = await fetch(`${this.baseUrl}${path}`, {
        ...options,
        headers: {
          'Content-Type': 'application/json',
          ...options.headers,
        },
      });

      if (!response.ok) {
        return {
          errors: [{
            status: response.status,
            code: 'HTTP_ERROR',
            title: response.statusText,
          }],
        };
      }

      const data = await response.json();
      return { data };
    } catch (error) {
      return {
        errors: [{
          status: 0,
          code: 'NETWORK_ERROR',
          title: 'Network error',
          detail: error instanceof Error ? error.message : 'Unknown error',
        }],
      };
    }
  }

  async get<T>(path: string): Promise<ApiResponse<T>> {
    return this.fetch<T>(path, { method: 'GET' });
  }

  async post<T>(path: string, body: unknown): Promise<ApiResponse<T>> {
    return this.fetch<T>(path, {
      method: 'POST',
      body: JSON.stringify(body),
    });
  }

  async patch<T>(path: string, body: unknown): Promise<ApiResponse<T>> {
    return this.fetch<T>(path, {
      method: 'PATCH',
      body: JSON.stringify(body),
    });
  }

  async delete<T>(path: string): Promise<ApiResponse<T>> {
    return this.fetch<T>(path, { method: 'DELETE' });
  }
}

// Singleton instance
export const apiClient = new ApiClient();

// Made with Bob
