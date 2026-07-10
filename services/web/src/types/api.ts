/**
 * Shared response shapes that mirror the BachatSetu backend's actual REST conventions
 * (see services/backend docs/application/*.md). Keeping these centrally typed means every
 * feature's API layer speaks the same contract instead of re-declaring it per endpoint.
 */

/** Matches every paginated list endpoint's `PageResponse<T>` wrapper. */
export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
}

/** Matches the RFC 7807 problem-detail body every backend error handler returns. */
export interface ApiProblemDetail {
  type: string;
  title: string;
  status: number;
  detail: string;
  instance: string;
  code: string;
  timestamp: string;
  violations?: { field: string; message: string }[];
}
