import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { MsalService } from '@azure/msal-angular';
import { PerfilUsuario, RolUsuario } from '@core/models';

// ── Etiquetas legibles ─────────────────────────────────────────
export const ETIQUETA_ROL: Record<RolUsuario, string> = {
  ADMIN:             'Administrador',
  SUPERVISOR_TALLER: 'Supervisor de Taller',
  MECANICO_TALLER:   'Mecánico de Taller',
  COMERCIAL:         'Comercial',
  CONTABILIDAD:      'Contabilidad',
};

// ── Rutas accesibles por rol ───────────────────────────────────
const ACCESO: Record<RolUsuario, string[]> = {
  ADMIN: [
    'dashboard','gps','vehiculos','conductores',
    'taller','almacen','combustible',
    'servicios','clientes','facturacion',
    'reportes','administracion',
  ],
  SUPERVISOR_TALLER: [
    'dashboard','gps','vehiculos','conductores',
    'taller','almacen','combustible','reportes',
  ],
  MECANICO_TALLER: [
    'dashboard','taller','almacen','vehiculos',
  ],
  COMERCIAL: [
    'dashboard','gps','servicios','clientes','reportes',
  ],
  CONTABILIDAD: [
    'dashboard','clientes','facturacion','reportes',
  ],
};

// ── Rutas donde el rol puede crear / editar / eliminar ─────────
const ESCRITURA: Record<RolUsuario, string[]> = {
  ADMIN:             ['vehiculos','conductores','taller','almacen','combustible','servicios','clientes','facturacion','administracion'],
  SUPERVISOR_TALLER: ['vehiculos','conductores','taller','almacen','combustible'],
  MECANICO_TALLER:   ['taller'],
  COMERCIAL:         ['servicios','clientes'],
  CONTABILIDAD:      ['facturacion'],
};

const ROLES_VALIDOS: RolUsuario[] = [
  'ADMIN','SUPERVISOR_TALLER','MECANICO_TALLER','COMERCIAL','CONTABILIDAD',
];
const LS_DEV_ROL = 'dev_rol';

@Injectable({ providedIn: 'root' })
export class PerfilService {
  private readonly msal   = inject(MsalService);
  private readonly http   = inject(HttpClient);
  private readonly router = inject(Router);

  /** Rol activo (signal para reactividad) */
  readonly rolActual = signal<RolUsuario>(this.detectarRol());

  /** Etiqueta legible del rol */
  readonly etiquetaRol = computed(() => ETIQUETA_ROL[this.rolActual()]);

  /** Nombre del usuario (del backend) */
  readonly nombre = signal<string>('');

  /** ID del usuario en BD */
  readonly usuarioId = signal<string | null>(null);

  /** Correo del usuario autenticado */
  readonly correo = signal(
    this.msal.instance.getAllAccounts()[0]?.username ?? ''
  );

  /** Inicial para el avatar */
  readonly inicial = computed(() => {
    const n = this.nombre();
    const c = this.correo();
    return ((n || c)[0] ?? 'U').toUpperCase();
  });

  /**
   * Llama al backend para obtener/crear el perfil del usuario autenticado.
   * Actualiza rolActual con el rol almacenado en la BD.
   * Debe llamarse una sola vez al iniciar la app (en AppComponent o en el guard de auth).
   */
  sincronizarPerfil(): void {
    // No sincronizar si estamos en modo dev-rol (localStorage override)
    const devRol = localStorage.getItem(LS_DEV_ROL);
    if (devRol && ROLES_VALIDOS.includes(devRol as RolUsuario)) return;

    this.http.get<PerfilUsuario>('/api/v1/usuarios/perfil-actual').subscribe({
      next: (p) => {
        this.nombre.set(p.nombre ?? '');
        if (p.email) this.correo.set(p.email);

        if (!p.empresaId || !p.rol) {
          // Usuario autenticado en Azure B2C pero sin empresa asignada → onboarding
          this.router.navigate(['/onboarding']);
          return;
        }

        this.usuarioId.set(p.id);
        if (p.rol && ROLES_VALIDOS.includes(p.rol)) {
          this.rolActual.set(p.rol);
        }
      },
      error: () => {
        // Sin conexión al backend → mantiene rol detectado localmente
      },
    });
  }

  // ── Consultas ────────────────────────────────────────────────

  tieneAcceso(ruta: string): boolean {
    return ACCESO[this.rolActual()].includes(ruta);
  }

  puedeEscribir(ruta: string): boolean {
    return ESCRITURA[this.rolActual()].includes(ruta);
  }

  esAdmin(): boolean {
    return this.rolActual() === 'ADMIN';
  }

  /** Rutas visibles para construir el menú */
  rutasVisibles(): string[] {
    return ACCESO[this.rolActual()];
  }

  // ── Desarrollo: override manual del rol ──────────────────────

  /** Para desarrollo: guarda un rol en localStorage y recarga la señal */
  setRolDesarrollo(rol: RolUsuario | null) {
    if (rol === null) {
      localStorage.removeItem(LS_DEV_ROL);
    } else {
      localStorage.setItem(LS_DEV_ROL, rol);
    }
    this.rolActual.set(this.detectarRol());
  }

  // ── Detección del rol ────────────────────────────────────────

  private detectarRol(): RolUsuario {
    // 1. Override de desarrollo (localStorage)
    const devRol = localStorage.getItem(LS_DEV_ROL);
    if (devRol && ROLES_VALIDOS.includes(devRol as RolUsuario)) {
      return devRol as RolUsuario;
    }

    // 2. Claim del token Azure AD B2C
    const account = this.msal.instance.getAllAccounts()[0];
    const claims = account?.idTokenClaims as Record<string, unknown> | undefined;
    // Azure B2C custom attribute → extension_rol
    // O atributo estándar roles[] (App Roles)
    const claimRol =
      (claims?.['extension_rol'] as string | undefined) ??
      (Array.isArray(claims?.['roles']) ? (claims!['roles'] as string[])[0] : undefined);

    if (claimRol && ROLES_VALIDOS.includes(claimRol as RolUsuario)) {
      return claimRol as RolUsuario;
    }

    // 3. Fallback — por ahora ADMIN para no romper la app
    return 'ADMIN';
  }
}
