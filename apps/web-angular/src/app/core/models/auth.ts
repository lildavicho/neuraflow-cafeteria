export type AuthResponse = {
  token: string;
  refreshToken: string;
  userId: number;
  email: string;
  fullName: string;
  role: string;
  tenantCode?: string;
  platformAdmin?: boolean;
  commercialPlan?: string;
  enabledModules?: string[];
  requires2FA?: boolean;
  message?: string;
};

export type AuthUser = {
  userId: number;
  email: string;
  fullName: string;
  role: string;
  tenantCode?: string;
  platformAdmin?: boolean;
  commercialPlan?: string;
  enabledModules: string[];
};
