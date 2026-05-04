export type CommercialPlanSnapshot = {
  tenantCode: string;
  commercialPlan: 'START' | 'PRO' | 'VISION_AI' | string;
  enabledModules: string[];
  availablePlans: string[];
};

export type NavigationModule = {
  path: string;
  label: string;
  requiredModule?: string;
  platformOnly?: boolean;
};
