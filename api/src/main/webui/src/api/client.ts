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

  // Trigger login (full page redirect)
  login() {
    // Extract only the path, search, and hash (no origin) to comply with security requirements
    const redirectUri = encodeURIComponent(
      window.location.pathname + window.location.search + window.location.hash
    );
    window.location.href = `/api/session/login?redirect_uri=${redirectUri}`;
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
        /*
         * Quarkus uses X-Requested-With for JavaScript requests
         * to use HTTP status 499 when a session is required.
         */
        'X-Requested-With': 'JavaScript',
        ...options?.headers,
      },
    });

    if (response.status === 499 && response.headers.get('WWW-Authenticate') === 'OIDC') {
      // Quarkus uses status 499 with 'WWW-Authenticate: OIDC' to indicate login required.
      this.login(); // Redirect to login
      return {} as T;
    }

    // Handle non-JSON responses
    const contentType = response.headers.get('content-type');
    if (!contentType?.includes('application/json')) {
      if (!response.ok) {
        throw new ApiError(response.status, response.statusText);
      }
      return {} as T;
    }

    const data = await response.json();

    if (!response.ok) {
      throw new ApiError(
        response.status,
        response.statusText,
        data.errors
      );
    }

    return data;
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