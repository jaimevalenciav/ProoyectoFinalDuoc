import { Component, signal, inject } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MsalService } from '@azure/msal-angular';

interface ElementoNavegacion { etiqueta: string; icono: string; ruta: string; }

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [
    RouterOutlet, RouterLink, RouterLinkActive,
    MatToolbarModule, MatSidenavModule, MatListModule,
    MatIconModule, MatButtonModule, MatTooltipModule,
  ],
  template: `
    <mat-sidenav-container class="contenedor-shell">
      <mat-sidenav class="barra-lateral" mode="side" [opened]="barraLateralAbierta()">

        <div class="marca">
          <div class="icono-marca">
            <mat-icon>local_shipping</mat-icon>
          </div>
          <div class="texto-marca">
            <span class="nombre-marca">TruckManager</span>
            <span class="subtitulo-marca">Pro — Gestión de Flota</span>
          </div>
        </div>

        <div class="seccion-nav">
          <span class="etiqueta-seccion">Principal</span>
          <mat-nav-list>
            @for (elemento of elementosPrincipales; track elemento.ruta) {
              <a mat-list-item [routerLink]="elemento.ruta" routerLinkActive="enlace-activo"
                 [routerLinkActiveOptions]="{ exact: elemento.ruta === 'dashboard' }">
                <mat-icon matListItemIcon>{{ elemento.icono }}</mat-icon>
                <span matListItemTitle>{{ elemento.etiqueta }}</span>
              </a>
            }
          </mat-nav-list>
        </div>

        <div class="seccion-nav">
          <span class="etiqueta-seccion">Operaciones</span>
          <mat-nav-list>
            @for (elemento of elementosOperaciones; track elemento.ruta) {
              <a mat-list-item [routerLink]="elemento.ruta" routerLinkActive="enlace-activo">
                <mat-icon matListItemIcon>{{ elemento.icono }}</mat-icon>
                <span matListItemTitle>{{ elemento.etiqueta }}</span>
              </a>
            }
          </mat-nav-list>
        </div>

        <div class="seccion-nav">
          <span class="etiqueta-seccion">Sistema</span>
          <mat-nav-list>
            @for (elemento of elementosSistema; track elemento.ruta) {
              <a mat-list-item [routerLink]="elemento.ruta" routerLinkActive="enlace-activo">
                <mat-icon matListItemIcon>{{ elemento.icono }}</mat-icon>
                <span matListItemTitle>{{ elemento.etiqueta }}</span>
              </a>
            }
          </mat-nav-list>
        </div>

        <div class="pie-barra-lateral">
          <div class="info-usuario">
            <div class="avatar-usuario">{{ inicialUsuario() }}</div>
            <div class="datos-usuario">
              <span class="correo-usuario">{{ correoUsuario() }}</span>
              <span class="rol-usuario">Administrador</span>
            </div>
          </div>
          <button mat-icon-button (click)="cerrarSesion()" matTooltip="Cerrar sesión" class="boton-salir">
            <mat-icon>logout</mat-icon>
          </button>
        </div>
      </mat-sidenav>

      <mat-sidenav-content class="contenido-principal">
        <mat-toolbar class="barra-herramientas">
          <button mat-icon-button (click)="alternarBarra()" class="boton-menu">
            <mat-icon>menu</mat-icon>
          </button>
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

    /* ── Barra lateral ──────────────────────────── */
    .barra-lateral {
      width: var(--ancho-sidebar);
      background: #1B2C40;
      border-right: none;
      display: flex;
      flex-direction: column;
      box-shadow: 2px 0 16px rgba(0,0,0,.25);
    }

    .marca {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 20px 16px 16px;
      border-bottom: 1px solid rgba(255,255,255,.1);
    }
    .icono-marca {
      width: 40px; height: 40px;
      border-radius: var(--radio-md);
      background: rgba(255,255,255,.15);
      display: flex; align-items: center; justify-content: center;
      color: #fff;
      flex-shrink: 0;
    }
    .nombre-marca { display: block; font-size: 16px; font-weight: 700; color: #fff; line-height: 1.2; }
    .subtitulo-marca { display: block; font-size: 11px; color: rgba(255,255,255,.5); font-weight: 500; letter-spacing: .5px; text-transform: uppercase; }

    .seccion-nav { padding: 12px 0 4px; }
    .etiqueta-seccion {
      display: block;
      font-size: 10px;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 1px;
      color: rgba(255,255,255,.4);
      padding: 0 20px 6px;
    }

    a[mat-list-item] {
      border-radius: var(--radio-sm) !important;
      margin: 1px 8px !important;
      height: 42px !important;
      color: rgba(255,255,255,.75) !important;
      font-weight: 500 !important;
      font-family: var(--fuente) !important;
      transition: all .15s !important;

      /* Clases internas de Angular Material MDC */
      .mdc-list-item__primary-text,
      .mat-mdc-list-item-unscoped-content,
      span[matlistitemtitle] {
        color: rgba(255,255,255,.75) !important;
        font-family: var(--fuente) !important;
      }
      mat-icon,
      .mat-icon { color: rgba(255,255,255,.55) !important; font-size: 20px !important; }

      &:hover {
        background: rgba(255,255,255,.1) !important;
        color: #fff !important;
        .mdc-list-item__primary-text,
        .mat-mdc-list-item-unscoped-content,
        span[matlistitemtitle] { color: #fff !important; }
        mat-icon, .mat-icon { color: #fff !important; }
      }
      &.enlace-activo {
        background: rgba(255,255,255,.18) !important;
        color: #fff !important;
        font-weight: 600 !important;
        .mdc-list-item__primary-text,
        .mat-mdc-list-item-unscoped-content,
        span[matlistitemtitle] { color: #fff !important; font-weight: 600 !important; }
        mat-icon, .mat-icon { color: #fff !important; }
      }
    }

    .pie-barra-lateral {
      margin-top: auto;
      padding: 12px 12px 16px;
      border-top: 1px solid rgba(255,255,255,.1);
      display: flex;
      align-items: center;
      gap: 8px;
    }
    .info-usuario { display: flex; align-items: center; gap: 10px; flex: 1; min-width: 0; }
    .avatar-usuario {
      width: 34px; height: 34px; border-radius: 50%;
      background: rgba(255,255,255,.2);
      color: #fff; font-weight: 700; font-size: 14px;
      display: flex; align-items: center; justify-content: center;
      flex-shrink: 0;
    }
    .datos-usuario { display: flex; flex-direction: column; min-width: 0; }
    .correo-usuario { font-size: 12px; font-weight: 500; color: #fff; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .rol-usuario { font-size: 11px; color: rgba(255,255,255,.5); }
    .boton-salir { color: rgba(255,255,255,.6) !important; }

    /* ── Barra de herramientas ───────────────────── */
    .barra-herramientas {
      background: #fff !important;
      border-bottom: 1px solid var(--color-borde);
      box-shadow: 0 2px 8px rgba(27,44,64,.07);
      position: sticky;
      top: 0;
      z-index: 10;
      height: 60px;
      padding: 0 20px;
    }
    .boton-menu { color: var(--azul-600) !important; }
    .ruta-activa {
      display: flex; align-items: center; gap: 8px;
      color: var(--azul-900); font-weight: 600; font-size: 15px;
      margin-left: 8px;
    }
    .icono-ruta { color: var(--azul-500); font-size: 20px; }
    .espaciador { flex: 1; }
    .boton-accion-barra { color: var(--color-texto-2) !important; }

    /* ── Contenido principal ────────────────────── */
    .contenido-principal { background: var(--color-fondo); }
    .envoltura-pagina { padding: 28px 32px; }
  `],
})
export class ShellComponent {
  private readonly servicioAuth = inject(MsalService);

  barraLateralAbierta = signal(true);
  correoUsuario = signal(this.servicioAuth.instance.getAllAccounts()[0]?.username ?? '');
  inicialUsuario = signal((this.servicioAuth.instance.getAllAccounts()[0]?.username?.[0] ?? 'U').toUpperCase());

  elementosPrincipales: ElementoNavegacion[] = [
    { etiqueta: 'Dashboard',   icono: 'dashboard',         ruta: 'dashboard' },
    { etiqueta: 'GPS / Mapa',  icono: 'map',               ruta: 'gps' },
    { etiqueta: 'Vehículos',   icono: 'directions_bus',    ruta: 'vehiculos' },
    { etiqueta: 'Conductores', icono: 'badge',             ruta: 'conductores' },
  ];

  elementosOperaciones: ElementoNavegacion[] = [
    { etiqueta: 'Taller',       icono: 'build',             ruta: 'taller' },
    { etiqueta: 'Almacén',      icono: 'inventory_2',       ruta: 'almacen' },
    { etiqueta: 'Combustible',  icono: 'local_gas_station', ruta: 'combustible' },
    { etiqueta: 'AdBlue',       icono: 'opacity',           ruta: 'adblue' },
    { etiqueta: 'Servicios',    icono: 'local_shipping',    ruta: 'servicios' },
    { etiqueta: 'Clientes',     icono: 'people',            ruta: 'clientes' },
    { etiqueta: 'Facturación',  icono: 'receipt',           ruta: 'facturacion' },
    { etiqueta: 'Reportes',     icono: 'bar_chart',         ruta: 'reportes' },
  ];

  elementosSistema: ElementoNavegacion[] = [
    { etiqueta: 'Administración', icono: 'admin_panel_settings', ruta: 'administracion' },
  ];

  private readonly mapaIconos: Record<string, string> = {
    dashboard: 'dashboard', gps: 'map', vehiculos: 'directions_bus',
    conductores: 'badge', taller: 'build', almacen: 'inventory_2',
    combustible: 'local_gas_station', adblue: 'opacity', servicios: 'local_shipping',
    clientes: 'people', facturacion: 'receipt', reportes: 'bar_chart',
    administracion: 'admin_panel_settings',
  };
  private readonly mapaEtiquetas: Record<string, string> = {
    dashboard: 'Dashboard', gps: 'GPS / Mapa', vehiculos: 'Vehículos',
    conductores: 'Conductores', taller: 'Taller', almacen: 'Almacén',
    combustible: 'Combustible', adblue: 'AdBlue', servicios: 'Servicios',
    clientes: 'Clientes', facturacion: 'Facturación', reportes: 'Reportes',
    administracion: 'Administración',
  };

  iconoRutaActual(): string {
    const segmento = window.location.pathname.split('/')[1] ?? '';
    return this.mapaIconos[segmento] ?? 'circle';
  }

  etiquetaRutaActual(): string {
    const segmento = window.location.pathname.split('/')[1] ?? '';
    return this.mapaEtiquetas[segmento] ?? '';
  }

  alternarBarra(): void { this.barraLateralAbierta.update(v => !v); }
  cerrarSesion(): void { this.servicioAuth.logoutRedirect(); }
}
