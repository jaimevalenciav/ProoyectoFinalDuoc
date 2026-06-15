import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { PerfilService } from '@core/services/perfil.service';

/**
 * Guard de rol.
 * Lee el path del route y consulta PerfilService.tieneAcceso().
 * Si no tiene acceso, redirige al dashboard.
 */
export const rolGuard: CanActivateFn = (route) => {
  const perfil = inject(PerfilService);
  const router = inject(Router);

  // route.routeConfig?.path es el segmento: 'vehiculos', 'taller', etc.
  const ruta = route.routeConfig?.path ?? '';

  if (perfil.tieneAcceso(ruta)) return true;

  // Sin acceso → al dashboard (silencioso, sin mensaje de error)
  return router.createUrlTree(['dashboard']);
};
