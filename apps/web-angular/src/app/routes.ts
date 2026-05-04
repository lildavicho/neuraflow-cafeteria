import { Routes } from '@angular/router';
import { authGuard, guestGuard, platformAdminGuard } from './core/guards/auth';
import { moduleAccessGuard } from './core/guards/module-access';

export const routes: Routes = [
  {
    path: 'landing',
    redirectTo: '',
    pathMatch: 'full',
  },
  {
    path: 'portal',
    redirectTo: 'pos',
    pathMatch: 'full',
  },
  {
    path: 'unauthorized',
    redirectTo: 'login',
    pathMatch: 'full',
  },
  {
    path: '',
    pathMatch: 'full',
    loadComponent: () => import('./features/public/public-site').then((m) => m.PublicSite),
  },
  {
    path: 'precios',
    loadComponent: () => import('./features/public/public-site').then((m) => m.PublicSite),
  },
  {
    path: 'demo',
    loadComponent: () => import('./features/public/public-site').then((m) => m.PublicSite),
  },
  {
    path: 'contacto',
    loadComponent: () => import('./features/public/public-site').then((m) => m.PublicSite),
  },
  {
    path: 'funcionalidades',
    loadComponent: () => import('./features/public/public-site').then((m) => m.PublicSite),
  },
  {
    path: 'vision-ai',
    loadComponent: () => import('./features/public/public-site').then((m) => m.PublicSite),
  },
  {
    path: 'facturacion-electronica',
    loadComponent: () => import('./features/public/public-site').then((m) => m.PublicSite),
  },
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login/login').then((m) => m.Login),
    canActivate: [guestGuard],
  },
  {
    path: 'register',
    redirectTo: 'demo',
    pathMatch: 'full',
  },
  {
    path: 'forgot-password',
    loadComponent: () => import('./features/auth/forgot-password/forgot-password').then((m) => m.ForgotPassword),
    canActivate: [guestGuard],
  },
  {
    path: '',
    loadComponent: () => import('./features/shell/shell').then((m) => m.Shell),
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'pos' },
      {
        path: 'pos',
        loadComponent: () => import('./features/pos/pos').then((m) => m.Pos),
        canActivate: [moduleAccessGuard],
        data: { requiredModule: 'pos' },
      },
      {
        path: 'inventario',
        loadComponent: () => import('./features/inventory/inventory').then((m) => m.Inventory),
        canActivate: [moduleAccessGuard],
        data: { requiredModule: 'inventory' },
      },
      {
        path: 'clientes',
        loadComponent: () => import('./features/customers/customers').then((m) => m.Customers),
        canActivate: [moduleAccessGuard],
        data: { requiredModule: 'customers' },
      },
      {
        path: 'reportes',
        loadComponent: () => import('./features/reports/reports').then((m) => m.Reports),
        canActivate: [moduleAccessGuard],
        data: { requiredModule: 'basic-reports' },
      },
      {
        path: 'dashboard-ejecutivo',
        loadComponent: () =>
          import('./features/executive-dashboard/executive-dashboard').then((m) => m.ExecutiveDashboard),
        canActivate: [moduleAccessGuard],
        data: { requiredModule: 'executive-dashboard' },
      },
      {
        path: 'compras',
        loadComponent: () => import('./features/purchases/purchases').then((m) => m.Purchases),
        canActivate: [moduleAccessGuard],
        data: { requiredModule: 'purchases' },
      },
      {
        path: 'insights',
        loadComponent: () => import('./features/insights/insights').then((m) => m.Insights),
        canActivate: [moduleAccessGuard],
        data: { requiredModule: 'insights' },
      },
      {
        path: 'contabilidad',
        loadComponent: () => import('./features/accounting/accounting').then((m) => m.Accounting),
        canActivate: [moduleAccessGuard],
        data: { requiredModule: 'accounting' },
      },
      {
        path: 'sri',
        loadComponent: () => import('./features/sri/sri').then((m) => m.Sri),
        canActivate: [moduleAccessGuard],
        data: { requiredModule: 'sri' },
      },
      {
        path: 'onboarding',
        loadComponent: () => import('./features/onboarding/onboarding').then((m) => m.Onboarding),
      },
      {
        path: 'vision',
        loadComponent: () => import('./features/vision-ai/vision-ai').then((m) => m.VisionAi),
        canActivate: [moduleAccessGuard],
        data: { requiredModule: 'vision-ai' },
      },
      {
        path: 'sucursales',
        loadComponent: () => import('./features/branches/branches').then((m) => m.Branches),
        canActivate: [moduleAccessGuard],
        data: { requiredModule: 'branches' },
      },
      {
        path: 'bodegas',
        loadComponent: () => import('./features/warehouses/warehouses').then((m) => m.Warehouses),
        canActivate: [moduleAccessGuard],
        data: { requiredModule: 'warehouses' },
      },
      {
        path: 'contactos',
        loadComponent: () => import('./features/parties/parties').then((m) => m.Parties),
        canActivate: [moduleAccessGuard],
        data: { requiredModule: 'parties' },
      },
      {
        path: 'aprobaciones',
        loadComponent: () => import('./features/approvals/approvals').then((m) => m.Approvals),
        canActivate: [moduleAccessGuard],
        data: { requiredModule: 'approvals' },
      },
      {
        path: 'secuencias',
        loadComponent: () => import('./features/sequences/sequences').then((m) => m.Sequences),
        canActivate: [moduleAccessGuard],
        data: { requiredModule: 'branches' },
      },
      {
        path: 'admin/negocios',
        loadComponent: () => import('./features/admin-tenants/admin-tenants').then((m) => m.AdminTenants),
        canActivate: [platformAdminGuard],
      },
      {
        path: 'perfil',
        loadComponent: () => import('./features/profile/profile').then((m) => m.Profile),
      },
      {
        path: 'notificaciones',
        loadComponent: () => import('./features/notifications/notifications').then((m) => m.Notifications),
      },
      {
        path: 'ajustes',
        loadComponent: () => import('./features/settings/settings').then((m) => m.Settings),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
