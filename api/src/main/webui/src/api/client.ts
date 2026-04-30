/**
 * API Client for StreamsHub Console
 * 
 * Handles all HTTP communication with the Quarkus backend API.
 * No authentication initially - will be added later.
 */

export class ApiError extends Error {
  constructor(
    public status: number,
    public statusText: string,
    public errors?: Array<{ status: string; title: string; detail?: string }>
  ) {
    super(`API Error: ${status} ${statusText}`);
    this.name = 'ApiError';
  }
}

class ApiClient {
  private baseUrl: string;

  constructor() {
    // In development, Vite proxy handles /api requests
    // In production, API will be served from same origin
    this.baseUrl = import.meta.env.VITE_API_URL || '';
  }

  /**
   * Generic fetch method with error handling
   */
  async fetch<T>(path: string, options?: RequestInit): Promise<T> {
    const url = `${this.baseUrl}${path}`;
    
    const response = await fetch(url, {
      ...options,
      headers: {
        'Content-Type': 'application/json',
        ...options?.headers,
      },
    });

    // Handle non-JSON responses
    const contentType = response.headers.get('content-type');
    if (!contentType?.includes('application/json')) {
      if (!response.ok) {
        throw new ApiError(response.status, response.statusText);
      }
      return {} as T;
    }

    return await response.json();
  }

  /**
   * GET request
   */
  get<T>(path: string, options?: RequestInit): Promise<T> {
    return this.fetch<T>(path, { ...options, method: 'GET' });
  }

  /**
   * POST request
   */
  post<T>(path: string, data: unknown, options?: RequestInit): Promise<T> {
    return this.fetch<T>(path, {
      ...options,
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  /**
   * PATCH request
   */
  patch<T>(path: string, data: unknown, options?: RequestInit): Promise<T> {
    return this.fetch<T>(path, {
      ...options,
      method: 'PATCH',
      body: JSON.stringify(data),
    });
  }

  /**
   * DELETE request
   */
  delete<T>(path: string, options?: RequestInit): Promise<T> {
    return this.fetch<T>(path, { ...options, method: 'DELETE' });
  }
}

// Export singleton instance
export const apiClient = new ApiClient();