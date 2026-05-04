export type ApiErrorPayload = {
  timestamp?: string;
  status?: number;
  error?: string;
  message?: string;
  code?: string;
  traceId?: string;
  path?: string;
  validationErrors?: Record<string, string>;
};
