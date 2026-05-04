/**
 * Representación de una página Spring Data recibida desde el backend.
 * El backend devuelve estructuras similares a `org.springframework.data.domain.Page`.
 */
export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
  numberOfElements: number;
  empty: boolean;
}

export const EMPTY_PAGE: PageResponse<never> = Object.freeze({
  content: [],
  totalElements: 0,
  totalPages: 0,
  number: 0,
  size: 0,
  first: true,
  last: true,
  numberOfElements: 0,
  empty: true,
});

export function emptyPage<T>(): PageResponse<T> {
  return EMPTY_PAGE as unknown as PageResponse<T>;
}
