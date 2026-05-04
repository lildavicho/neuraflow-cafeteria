export type UiFeedbackTone = 'success' | 'error' | 'warning' | 'info';

export type UiFeedback = {
  tone: UiFeedbackTone;
  message: string;
  traceId?: string;
  code?: string;
  status?: number;
};
