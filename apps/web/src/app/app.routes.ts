import { Routes } from '@angular/router';
import { MsalGuard } from '@azure/msal-angular';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('@pages/login/login.component').then(m => m.LoginComponent),
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
        loadComponent: () => import('@pages/gps/gps.component').then(m => m.GpsComponent),
      },
      {
        path: 'vehiculos',
        loadComponent: () => import('@pages/vehiculos/vehiculos.component').then(m => m.VehiculosComponent),
      },
      {
        path: 'conductores',
        loadComponent: () => import('@pages/conductores/conductores.component').then(m => m.ConductoresComponent),
      },
      {
        path: 'taller',
        loadComponent: () => import('@pages/taller/taller.component').then(m => m.TallerComponent),
      },
      {
        path: 'almacen',
        loadComponent: () => import('@pages/almacen/almacen.component').then(m => m.AlmacenComponent),
      },
      {
        path: 'combustible',
        loadComponent: () => import('@pages/combustible/combustible.component').then(m => m.CombustibleComponent),
      },
      {
        path: 'servicios',
        loadComponent: () => import('@pages/servicios/servicios.component').then(m => m.ServiciosComponent),
      },
      {
        path: 'clientes',
        loadComponent: () => import('@pages/clientes/clientes.component').then(m => m.ClientesComponent),
      },
      {
        path: 'facturacion',
        loadComponent: () => import('@pages/facturacion/facturacion.component').then(m => m.FacturacionComponent),
      },
      {
        path: 'reportes',
        loadComponent: () => import('@pages/reportes/reportes.component').then(m => m.ReportesComponent),
      },
      {
        path: 'administracion',
        loadComponent: () => import('@pages/administracion/administracion.component').then(m => m.AdministracionComponent),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
