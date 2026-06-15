import { Routes } from '@angular/router';
import { MsalGuard } from '@azure/msal-angular';
import { rolGuard } from '@core/auth/rol.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('@pages/login/login.component').then(m => m.LoginComponent),
  },
  {
    // Pantalla de onboarding: usuario autenticado en B2C pero sin empresa asignada aún.
    // Requiere sesión activa (MsalGuard) pero NO el shell/layout.
    path: 'onboarding',
    canActivate: [MsalGuard],
    loadComponent: () => import('@pages/onboarding/onboarding.component').then(m => m.OnboardingComponent),
  },
  {
    path: '',
    loadComponent: () => import('@layout/shell/shell.component').then(m => m.ShellComponent),
    canActivate: [MsalGuard],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      {
        path: 'dashboard',
        loadComponent: () => import('@pages/dashboard/dashboard.component').then(m => m.DashboardComponent),
      },
      {
        path: 'gps',
        canActivate: [rolGuard],
        loadComponent: () => import('@pages/gps/gps.component').then(m => m.GpsComponent),
      },
      {
        path: 'vehiculos',
        canActivate: [rolGuard],
        loadComponent: () => import('@pages/vehiculos/vehiculos.component').then(m => m.VehiculosComponent),
      },
      {
        path: 'conductores',
        canActivate: [rolGuard],
        loadComponent: () => import('@pages/conductores/conductores.component').then(m => m.ConductoresComponent),
      },
      {
        path: 'taller',
        canActivate: [rolGuard],
        loadComponent: () => import('@pages/taller/taller.component').then(m => m.TallerComponent),
      },
      {
        path: 'almacen',
        canActivate: [rolGuard],
        loadComponent: () => import('@pages/almacen/almacen.component').then(m => m.AlmacenComponent),
      },
      {
        path: 'combustible',
        canActivate: [rolGuard],
        loadComponent: () => import('@pages/combustible/combustible.component').then(m => m.CombustibleComponent),
      },
      {
        path: 'servicios',
        canActivate: [rolGuard],
        loadComponent: () => import('@pages/servicios/servicios.component').then(m => m.ServiciosComponent),
      },
      {
        path: 'clientes',
        canActivate: [rolGuard],
        loadComponent: () => import('@pages/clientes/clientes.component').then(m => m.ClientesComponent),
      },
      {
        path: 'facturacion',
        canActivate: [rolGuard],
        loadComponent: () => import('@pages/facturacion/facturacion.component').then(m => m.FacturacionComponent),
      },
      {
        path: 'reportes',
        canActivate: [rolGuard],
        loadComponent: () => import('@pages/reportes/reportes.component').then(m => m.ReportesComponent),
      },
      {
        path: 'administracion',
        canActivate: [rolGuard],
        loadComponent: () => import('@pages/administracion/administracion.component').then(m => m.AdministracionComponent),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
