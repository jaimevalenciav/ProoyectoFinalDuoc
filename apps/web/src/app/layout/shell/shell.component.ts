import { Component, signal, inject, computed } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSelectModule } from '@angular/material/select';
import { MsalService } from '@azure/msal-angular';
import { PerfilService, ETIQUETA_ROL } from '@core/services/perfil.service';
import { RolUsuario } from '@core/models';

interface ElementoNavegacion { etiqueta: string; icono: string; ruta: string; }

const TODOS_PRINCIPALES: ElementoNavegacion[] = [
  { etiqueta: 'Dashboard',   icono: 'dashboard',         ruta: 'dashboard' },
  { etiqueta: 'GPS / Mapa',  icono: 'map',               ruta: 'gps' },
  { etiqueta: 'Vehículos',   icono: 'directions_bus',    ruta: 'vehiculos' },
  { etiqueta: 'Conductores', icono: 'badge',             ruta: 'conductores' },
];

const TODOS_OPERACIONES: ElementoNavegacion[] = [
  { etiqueta: 'Taller',      icono: 'build',             ruta: 'taller' },
  { etiqueta: 'Almacén',     icono: 'inventory_2',       ruta: 'almacen' },
  { etiqueta: 'Combustible', icono: 'local_gas_station', ruta: 'combustible' },
  { etiqueta: 'Servicios',   icono: 'local_shipping',    ruta: 'servicios' },
  { etiqueta: 'Clientes',    icono: 'people',            ruta: 'clientes' },
  { etiqueta: 'Facturación', icono: 'receipt',           ruta: 'facturacion' },
  { etiqueta: 'Reportes',    icono: 'bar_chart',         ruta: 'reportes' },
];

const TODOS_SISTEMA: ElementoNavegacion[] = [
  { etiqueta: 'Administración', icono: 'admin_panel_settings', ruta: 'administracion' },
];

const MAPA_ICONOS: Record<string, string> = {
  dashboard: 'dashboard', gps: 'map', vehiculos: 'directions_bus',
  conductores: 'badge', taller: 'build', almacen: 'inventory_2',
  combustible: 'local_gas_station', servicios: 'local_shipping',
  clientes: 'people', facturacion: 'receipt', reportes: 'bar_chart',
  administracion: 'admin_panel_settings',
};
const MAPA_ETIQUETAS: Record<string, string> = {
  dashboard: 'Dashboard', gps: 'GPS / Mapa', vehiculos: 'Vehículos',
  conductores: 'Conductores', taller: 'Taller', almacen: 'Almacén',
  combustible: 'Combustible', servicios: 'Servicios',
  clientes: 'Clientes', facturacion: 'Facturación', reportes: 'Reportes',
  administracion: 'Administración',
};

const ROLES_DEV: { valor: RolUsuario; etiqueta: string }[] = [
  { valor: 'ADMIN',             etiqueta: 'Administrador' },
  { valor: 'SUPERVISOR_TALLER', etiqueta: 'Supervisor Taller' },
  { valor: 'MECANICO_TALLER',   etiqueta: 'Mecánico Taller' },
  { valor: 'COMERCIAL',         etiqueta: 'Comercial' },
  { valor: 'CONTABILIDAD',      etiqueta: 'Contabilidad' },
];

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    RouterOutlet, RouterLink, RouterLinkActive,
    MatToolbarModule, MatSidenavModule, MatListModule,
    MatIconModule, MatButtonModule, MatTooltipModule, MatSelectModule,
  ],
  template: `
    <mat-sidenav-container class="contenedor-shell" [class.sidebar-colapsada]="colapsado()">

      <!-- ═══ BARRA LATERAL ═══ -->
      <mat-sidenav class="barra-lateral" mode="side" opened="true"
                   [class.colapsada]="colapsado()">

        <!-- Marca + botón colapsar -->
        <div class="marca">
          <div class="icono-marca"><mat-icon>local_shipping</mat-icon></div>
          @if (!colapsado()) {
            <div class="texto-marca">
              <span class="nombre-marca">TruckManager</span>
              <span class="subtitulo-marca">Pro — Gestión de Flota</span>
            </div>
          }
          <button mat-icon-button class="boton-colapsar"
                  (click)="alternarColapso()"
                  [matTooltip]="colapsado() ? 'Expandir menú' : 'Colapsar menú'"
                  matTooltipPosition="right">
            <mat-icon>{{ colapsado() ? 'chevron_right' : 'chevron_left' }}</mat-icon>
          </button>
        </div>

        <!-- ── Sección Principal ── -->
        @if (elementosPrincipales().length) {
          <div class="seccion-nav">
            @if (!colapsado()) { <span class="etiqueta-seccion">Principal</span> }
            <mat-nav-list>
              @for (el of elementosPrincipales(); track el.ruta) {
                <a mat-list-item [routerLink]="el.ruta" routerLinkActive="enlace-activo"
                   [routerLinkActiveOptions]="{ exact: el.ruta === 'dashboard' }"
                   [matTooltip]="colapsado() ? el.etiqueta : ''"
                   matTooltipPosition="right">
                  <mat-icon matListItemIcon>{{ el.icono }}</mat-icon>
                  @if (!colapsado()) {
                    <span matListItemTitle>{{ el.etiqueta }}</span>
                  }
                </a>
              }
            </mat-nav-list>
          </div>
        }

        <!-- ── Sección Operaciones ── -->
        @if (elementosOperaciones().length) {
          <div class="seccion-nav">
            @if (!colapsado()) { <span class="etiqueta-seccion">Operaciones</span> }
            <mat-nav-list>
              @for (el of elementosOperaciones(); track el.ruta) {
                <a mat-list-item [routerLink]="el.ruta" routerLinkActive="enlace-activo"
                   [matTooltip]="colapsado() ? el.etiqueta : ''"
                   matTooltipPosition="right">
                  <mat-icon matListItemIcon>{{ el.icono }}</mat-icon>
                  @if (!colapsado()) {
                    <span matListItemTitle>{{ el.etiqueta }}</span>
                  }
                </a>
              }
            </mat-nav-list>
          </div>
        }

        <!-- ── Sección Sistema (solo ADMIN) ── -->
        @if (elementosSistema().length) {
          <div class="seccion-nav">
            @if (!colapsado()) { <span class="etiqueta-seccion">Sistema</span> }
            <mat-nav-list>
              @for (el of elementosSistema(); track el.ruta) {
                <a mat-list-item [routerLink]="el.ruta" routerLinkActive="enlace-activo"
                   [matTooltip]="colapsado() ? el.etiqueta : ''"
                   matTooltipPosition="right">
                  <mat-icon matListItemIcon>{{ el.icono }}</mat-icon>
                  @if (!colapsado()) {
                    <span matListItemTitle>{{ el.etiqueta }}</span>
                  }
                </a>
              }
            </mat-nav-list>
          </div>
        }

        <!-- ── Pie: usuario + rol ── -->
        <div class="pie-barra-lateral" [class.pie-colapsado]="colapsado()">
          @if (!colapsado()) {
            <div class="info-usuario">
              <div class="avatar-usuario">{{ perfil.inicial() }}</div>
              <div class="datos-usuario">
                <span class="correo-usuario">{{ perfil.correo() }}</span>
                <span class="rol-usuario">{{ perfil.etiquetaRol() }}</span>
              </div>
            </div>
          } @else {
            <div class="avatar-usuario" [matTooltip]="perfil.correo()" matTooltipPosition="right">
              {{ perfil.inicial() }}
            </div>
          }
          <button mat-icon-button (click)="cerrarSesion()"
                  matTooltip="Cerrar sesión" matTooltipPosition="right"
                  class="boton-salir">
            <mat-icon>logout</mat-icon>
          </button>
        </div>

        <!-- ── Selector de rol dev ── -->
        @if (modoDesarrollo && !colapsado()) {
          <div class="selector-rol-dev">
            <span class="dev-label">🛠 Simular rol</span>
            <mat-select [ngModel]="perfil.rolActual()"
                        (ngModelChange)="cambiarRolDev($event)"
                        class="select-dev">
              @for (r of rolesDisponibles; track r.valor) {
                <mat-option [value]="r.valor">{{ r.etiqueta }}</mat-option>
              }
            </mat-select>
          </div>
        }

      </mat-sidenav>

      <!-- ═══ CONTENIDO PRINCIPAL ═══ -->
      <mat-sidenav-content class="contenido-principal">
        <mat-toolbar class="barra-herramientas">
          <div class="ruta-activa">
            <mat-icon class="icono-ruta">{{ iconoRutaActual() }}</mat-icon>
            <span>{{ etiquetaRutaActual() }}</span>
          </div>
          <span class="espaciador"></span>
          <div class="acciones-barra">
            <button mat-icon-button matTooltip="Notificaciones" class="boton-accion-barra">
              <mat-icon>notifications_none</mat-icon>
            </button>
          </div>
        </mat-toolbar>
        <div class="envoltura-pagina">
          <router-outlet />
        </div>
      </mat-sidenav-content>

    </mat-sidenav-container>
  `,
  styles: [`
    .contenedor-shell { height: 100vh; }

    /* ── Barra lateral ── */
    .barra-lateral {
      width: var(--ancho-sidebar, 240px);
      background: #1B2C40;
      border-right: none;
      display: flex;
      flex-direction: column;
      box-shadow: 2px 0 16px rgba(0,0,0,.25);
      overflow: hidden;
      transition: width .22s cubic-bezier(.4,0,.2,1);
    }
    .barra-lateral.colapsada {
      width: 64px;
    }

    /* ── Marca ── */
    .marca {
      display: flex; align-items: center; gap: 10px;
      padding: 14px 10px 14px 12px;
      border-bottom: 1px solid rgba(255,255,255,.1);
      min-height: 64px;
      position: relative;
    }
    .icono-marca {
      width: 36px; height: 36px; border-radius: 8px;
      background: rgba(255,255,255,.15);
      display: flex; align-items: center; justify-content: center;
      color: #fff; flex-shrink: 0;
      mat-icon { font-size: 20px; width: 20px; height: 20px; }
    }
    .texto-marca { flex: 1; min-width: 0; overflow: hidden; }
    .nombre-marca    { display: block; font-size: 15px; font-weight: 700; color: #fff; line-height: 1.2; white-space: nowrap; }
    .subtitulo-marca { display: block; font-size: 10px; color: rgba(255,255,255,.45); font-weight: 500; letter-spacing: .5px; text-transform: uppercase; white-space: nowrap; }

    .boton-colapsar {
      color: rgba(255,255,255,.5) !important;
      flex-shrink: 0;
      width: 32px !important; height: 32px !important;
      &:hover { color: #fff !important; background: rgba(255,255,255,.1) !important; }
    }
    .barra-lateral.colapsada .marca {
      justify-content: center;
      padding: 14px 6px;
      .boton-colapsar { margin: 0; }
    }

    /* ── Secciones ── */
    .seccion-nav { padding: 10px 0 2px; }
    .etiqueta-seccion {
      display: block; font-size: 10px; font-weight: 700;
      text-transform: uppercase; letter-spacing: 1px;
      color: rgba(255,255,255,.35); padding: 0 18px 4px;
      white-space: nowrap; overflow: hidden;
    }

    /* ── Items de navegación ── */
    a[mat-list-item] {
      border-radius: 6px !important;
      margin: 1px 6px !important;
      height: 40px !important;
      color: rgba(255,255,255,.75) !important;
      font-weight: 500 !important;
      font-family: var(--fuente) !important;
      transition: background .13s, color .13s !important;
      white-space: nowrap;
      overflow: hidden;

      .mdc-list-item__primary-text,
      .mat-mdc-list-item-unscoped-content,
      span[matlistitemtitle] {
        color: rgba(255,255,255,.75) !important;
        font-family: var(--fuente) !important;
        white-space: nowrap;
        overflow: hidden;
      }
      mat-icon, .mat-icon { color: rgba(255,255,255,.55) !important; font-size: 20px !important; flex-shrink: 0; }

      &:hover {
        background: rgba(255,255,255,.1) !important; color: #fff !important;
        .mdc-list-item__primary-text, .mat-mdc-list-item-unscoped-content,
        span[matlistitemtitle] { color: #fff !important; }
        mat-icon, .mat-icon { color: #fff !important; }
      }
      &.enlace-activo {
        background: rgba(255,255,255,.16) !important; color: #fff !important; font-weight: 600 !important;
        border-left: 3px solid rgba(255,255,255,.7);
        .mdc-list-item__primary-text, .mat-mdc-list-item-unscoped-content,
        span[matlistitemtitle] { color: #fff !important; font-weight: 600 !important; }
        mat-icon, .mat-icon { color: #fff !important; }
      }
    }

    /* Modo colapsado: centrar íconos */
    .barra-lateral.colapsada a[mat-list-item] {
      justify-content: center !important;
      margin: 1px 4px !important;
      padding: 0 !important;
      .mat-mdc-list-item-content { justify-content: center !important; padding: 0 !important; }
    }

    /* ── Pie ── */
    .pie-barra-lateral {
      margin-top: auto; padding: 10px 10px 14px;
      border-top: 1px solid rgba(255,255,255,.1);
      display: flex; align-items: center; gap: 8px;
    }
    .pie-colapsado { justify-content: center; flex-direction: column; gap: 6px; padding: 10px 6px 14px; }
    .info-usuario { display: flex; align-items: center; gap: 10px; flex: 1; min-width: 0; overflow: hidden; }
    .avatar-usuario {
      width: 32px; height: 32px; border-radius: 50%;
      background: rgba(255,255,255,.2); color: #fff;
      font-weight: 700; font-size: 13px;
      display: flex; align-items: center; justify-content: center; flex-shrink: 0;
      cursor: default;
    }
    .datos-usuario { display: flex; flex-direction: column; min-width: 0; overflow: hidden; }
    .correo-usuario { font-size: 11px; font-weight: 500; color: #fff; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .rol-usuario    { font-size: 10px; color: rgba(255,255,255,.5); white-space: nowrap; }
    .boton-salir    { color: rgba(255,255,255,.55) !important; flex-shrink: 0; }

    /* ── Selector dev ── */
    .selector-rol-dev {
      padding: 6px 12px 10px;
      border-top: 1px dashed rgba(255,255,255,.12);
      display: flex; flex-direction: column; gap: 4px;
    }
    .dev-label { font-size: 10px; color: rgba(255,200,0,.65); text-transform: uppercase; letter-spacing: .05em; }
    .select-dev {
      --mat-select-trigger-text-size: 12px;
      color: rgba(255,255,255,.8) !important;
      .mat-mdc-select-value { color: rgba(255,255,255,.8) !important; }
      .mat-mdc-select-arrow { color: rgba(255,255,255,.45) !important; }
    }

    /* ── Barra de herramientas ── */
    .barra-herramientas {
      background: #fff !important; border-bottom: 1px solid var(--color-borde);
      box-shadow: 0 2px 8px rgba(27,44,64,.07);
      position: sticky; top: 0; z-index: 10; height: 60px; padding: 0 20px;
    }
    .ruta-activa { display: flex; align-items: center; gap: 8px; color: var(--azul-900); font-weight: 600; font-size: 15px; }
    .icono-ruta  { color: var(--azul-500); font-size: 20px; }
    .espaciador  { flex: 1; }
    .boton-accion-barra { color: var(--color-texto-2) !important; }

    /* ── Contenido principal ── */
    .contenido-principal { background: var(--color-fondo); }
    .envoltura-pagina    { padding: 28px 32px; }

    /*
     * Angular Material fija un margin-left inline en .mat-drawer-content al abrir
     * el sidenav, pero no lo actualiza cuando el width cambia vía CSS transition.
     * Forzamos el margen correcto con !important y animamos al mismo ritmo.
     */
    :host ::ng-deep .contenedor-shell .mat-drawer-content {
      margin-left: 240px !important;
      transition: margin-left .22s cubic-bezier(.4,0,.2,1) !important;
    }
    :host ::ng-deep .contenedor-shell.sidebar-colapsada .mat-drawer-content {
      margin-left: 64px !important;
    }
  `],
})
export class ShellComponent {
  private readonly servicioAuth = inject(MsalService);
  readonly perfil = inject(PerfilService);

  /** false = expandido (default) · true = modo mini (solo íconos) */
  colapsado = signal(false);

  // ── Modo desarrollo ──────────────────────────────────────────
  readonly modoDesarrollo = !this.perfil.esAdmin()
    ? false
    : !(this.servicioAuth.instance.getAllAccounts()[0]?.idTokenClaims as any)?.['extension_rol'];

  readonly rolesDisponibles = ROLES_DEV;

  // ── Menú filtrado por rol ────────────────────────────────────
  readonly elementosPrincipales = computed(() =>
    TODOS_PRINCIPALES.filter(e => this.perfil.tieneAcceso(e.ruta))
  );
  readonly elementosOperaciones = computed(() =>
    TODOS_OPERACIONES.filter(e => this.perfil.tieneAcceso(e.ruta))
  );
  readonly elementosSistema = computed(() =>
    TODOS_SISTEMA.filter(e => this.perfil.tieneAcceso(e.ruta))
  );

  // ── Helpers toolbar ──────────────────────────────────────────
  iconoRutaActual(): string {
    const seg = window.location.pathname.split('/')[1] ?? '';
    return MAPA_ICONOS[seg] ?? 'circle';
  }
  etiquetaRutaActual(): string {
    const seg = window.location.pathname.split('/')[1] ?? '';
    return MAPA_ETIQUETAS[seg] ?? '';
  }

  // ── Acciones ─────────────────────────────────────────────────
  alternarColapso() { this.colapsado.update(v => !v); }
  cerrarSesion()    { this.servicioAuth.logoutRedirect(); }
  cambiarRolDev(rol: RolUsuario) { this.perfil.setRolDesarrollo(rol); }
}
