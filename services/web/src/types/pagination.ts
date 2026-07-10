/** Generic page wrapper shape shared by admin, platform-operations, and audit list endpoints. */
export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
}
