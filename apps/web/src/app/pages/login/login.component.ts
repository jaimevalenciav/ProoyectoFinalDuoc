import { Component, inject, OnInit, signal, HostBinding } from '@angular/core';
import { Router } from '@angular/router';
import { MsalService } from '@azure/msal-angular';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { CommonModule } from '@angular/common';

const LS_KEY = 'login_wallpaper';

interface Wallpaper {
  id: number;
  label: string;
  /** CSS color para el botón selector */
  color: string;
  /** URL de imagen (null → gradiente por defecto) */
  imagen: string | null;
  /** Color de texto del botón selector */
  colorTexto: string;
}

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule, MatTooltipModule],
  template: `
    <!-- Fondo dinámico -->
    <div class="fondo-login" [class.con-imagen]="wallpaperActivo().imagen"
         [style.background-image]="wallpaperActivo().imagen ? 'url(' + wallpaperActivo().imagen + ')' : null">

      <!-- Overlay oscuro solo cuando hay imagen -->
      @if (wallpaperActivo().imagen) {
        <div class="overlay-imagen"></div>
      } @else {
        <div class="fondo-decorativo">
          <div class="circulo circulo-1"></div>
          <div class="circulo circulo-2"></div>
          <div class="circulo circulo-3"></div>
        </div>
      }

      <!-- Tarjeta centrada -->
      <div class="tarjeta-login" [class.sobre-imagen]="wallpaperActivo().imagen">
        <!-- Logo -->
        <div class="encabezado-login">
          <div class="logotipo">
            <mat-icon>local_shipping</mat-icon>
          </div>
          <h1>TruckManager Pro</h1>
          <p class="descripcion">Sistema de gestión de flota para empresas de transporte</p>
        </div>

        <!-- Características -->
        <div class="lista-caracteristicas">
          @for (item of caracteristicas; track item.icono) {
            <div class="caracteristica">
              <div class="icono-caracteristica">
                <mat-icon>{{ item.icono }}</mat-icon>
              </div>
              <span>{{ item.texto }}</span>
            </div>
          }
        </div>

        <hr class="divisor" />

        <!-- Botón login -->
        <button mat-flat-button class="boton-iniciar-sesion" (click)="iniciarSesion()">
          <mat-icon>login</mat-icon>
          Iniciar sesión con Azure B2C
        </button>

        <p class="nota-pie">Acceso exclusivo para empresas registradas</p>
      </div>

      <!-- ── Selector de wallpaper ── -->
      <div class="selector-wallpaper">
        <span class="etiqueta-selector">Fondo</span>
        <div class="carrusel-fondos">
          @for (wp of wallpapers; track wp.id) {
            <button
              class="boton-fondo"
              [class.activo]="wallpaperActivo().id === wp.id"
              [style.background]="wp.color"
              [matTooltip]="wp.label"
              matTooltipPosition="above"
              (click)="seleccionarWallpaper(wp)"
              [attr.aria-label]="wp.label">
              @if (wallpaperActivo().id === wp.id) {
                <mat-icon class="icono-check">check</mat-icon>
              }
            </button>
          }
        </div>
      </div>
    </div>
  `,
  styles: [`
    /* ── Fondo ── */
    .fondo-login {
      min-height: 100vh;
      display: flex;
      align-items: center;
      justify-content: center;
      background: linear-gradient(145deg, var(--azul-50) 0%, #fff 60%, var(--azul-100) 100%);
      position: relative;
      overflow: hidden;
      transition: background-image .5s ease;
    }
    .fondo-login.con-imagen {
      background-size: cover;
      background-position: center;
      background-repeat: no-repeat;
    }

    /* Overlay para imágenes */
    .overlay-imagen {
      position: absolute;
      inset: 0;
      background: linear-gradient(
        135deg,
        rgba(10, 18, 30, .72) 0%,
        rgba(10, 18, 30, .50) 50%,
        rgba(10, 18, 30, .65) 100%
      );
      backdrop-filter: blur(1px);
    }

    /* Círculos decorativos (solo fondo default) */
    .fondo-decorativo { position: absolute; inset: 0; pointer-events: none; }
    .circulo {
      position: absolute; border-radius: 50%;
      opacity: .12; background: var(--azul-500);
    }
    .circulo-1 { width: 400px; height: 400px; top: -120px; right: -80px; }
    .circulo-2 { width: 240px; height: 240px; bottom: -60px; left: -40px; opacity: .08; }
    .circulo-3 { width: 160px; height: 160px; top: 40%; right: 10%; opacity: .06; }

    /* ── Tarjeta ── */
    .tarjeta-login {
      position: relative;
      z-index: 2;
      background: #fff;
      border: 1px solid var(--azul-200);
      border-radius: var(--radio-xl);
      padding: 44px 40px;
      width: 420px;
      max-width: 92vw;
      box-shadow: 0 20px 60px rgba(27,44,64,.15), 0 4px 16px rgba(27,44,64,.08);
      transition: box-shadow .3s;
    }
    .tarjeta-login.sobre-imagen {
      background: rgba(255,255,255,.93);
      border-color: rgba(255,255,255,.4);
      box-shadow: 0 32px 80px rgba(0,0,0,.4), 0 8px 24px rgba(0,0,0,.25);
    }

    /* Encabezado */
    .encabezado-login { text-align: center; margin-bottom: 28px; }
    .logotipo {
      width: 64px; height: 64px;
      background: linear-gradient(135deg, var(--azul-600), var(--azul-400));
      border-radius: var(--radio-lg);
      display: flex; align-items: center; justify-content: center;
      margin: 0 auto 16px;
      box-shadow: 0 8px 20px rgba(27,44,64,.3);
      mat-icon { color: #fff; font-size: 32px; width: 32px; height: 32px; }
    }
    h1 {
      font-size: 26px; font-weight: 700; color: var(--azul-900);
      margin-bottom: 6px; letter-spacing: -.3px; font-family: var(--fuente);
    }
    .descripcion { font-size: 14px; color: var(--color-texto-2); line-height: 1.5; }

    /* Características */
    .lista-caracteristicas { display: flex; flex-direction: column; gap: 10px; margin-bottom: 24px; }
    .caracteristica { display: flex; align-items: center; gap: 12px; font-size: 14px; color: var(--color-texto); }
    .icono-caracteristica {
      width: 32px; height: 32px; flex-shrink: 0;
      background: var(--azul-50);
      border-radius: var(--radio-sm);
      display: flex; align-items: center; justify-content: center;
      mat-icon { color: var(--azul-600); font-size: 18px; width: 18px; height: 18px; }
    }

    .divisor { border: none; border-top: 1px solid var(--azul-100); margin-bottom: 24px; }

    /* Botón login */
    .boton-iniciar-sesion {
      width: 100%; height: 48px;
      background: linear-gradient(135deg, var(--azul-600), var(--azul-500)) !important;
      color: #fff !important;
      border-radius: var(--radio-md) !important;
      font-size: 15px !important; font-weight: 600 !important;
      font-family: var(--fuente) !important;
      box-shadow: 0 4px 14px rgba(27,44,64,.35) !important;
      display: flex; align-items: center; gap: 8px; justify-content: center;
      transition: transform .15s, box-shadow .15s !important;
      &:hover { transform: translateY(-1px); box-shadow: 0 6px 20px rgba(27,44,64,.4) !important; }
      mat-icon { font-size: 20px; }
    }
    .nota-pie { text-align: center; font-size: 12px; color: var(--color-texto-3); margin-top: 16px; }

    /* ── Selector de wallpaper ── */
    .selector-wallpaper {
      position: fixed;
      bottom: 16px;
      left: 50%;
      transform: translateX(-50%);
      z-index: 10;
      display: flex;
      align-items: center;
      gap: 7px;
      background: rgba(10, 18, 30, .60);
      border: 1px solid rgba(255,255,255,.15);
      border-radius: 32px;
      padding: 5px 12px;
      backdrop-filter: blur(12px);
      box-shadow: 0 4px 16px rgba(0,0,0,.3);
    }
    .etiqueta-selector {
      font-size: 9px;
      font-weight: 700;
      color: rgba(255,255,255,.55);
      text-transform: uppercase;
      letter-spacing: 1px;
      white-space: nowrap;
      padding-right: 2px;
    }
    .carrusel-fondos {
      display: flex;
      align-items: center;
      gap: 5px;
    }

    .boton-fondo {
      width: 20px; height: 20px;
      border-radius: 50%;
      border: 1.5px solid rgba(255,255,255,.28);
      cursor: pointer;
      position: relative;
      flex-shrink: 0;
      transition: transform .16s, border-color .16s, box-shadow .16s;
      display: flex; align-items: center; justify-content: center;
      overflow: hidden;

      &:hover {
        transform: scale(1.2);
        border-color: rgba(255,255,255,.75);
        box-shadow: 0 0 0 2px rgba(255,255,255,.18);
      }
      &.activo {
        border-color: #fff;
        transform: scale(1.25);
        box-shadow: 0 0 0 2px rgba(255,255,255,.3), 0 2px 8px rgba(0,0,0,.4);
      }

      .icono-check {
        font-size: 11px; width: 11px; height: 11px;
        color: #fff;
        filter: drop-shadow(0 0 2px rgba(0,0,0,.9));
      }
    }
  `],
})
export class LoginComponent implements OnInit {
  private readonly servicioAuth = inject(MsalService);
  private readonly enrutador    = inject(Router);

  readonly wallpapers: Wallpaper[] = [
    {
      id: 0,
      label: 'Predeterminado',
      color: 'linear-gradient(135deg, #E8F0FE 0%, #ffffff 60%, #C7D7F8 100%)',
      imagen: null,
      colorTexto: '#1B2C40',
    },
    {
      id: 1,
      label: 'Volvo FH16',
      color: '#2c3e50',
      imagen: 'wallpapers/bg-1.jpg',
      colorTexto: '#ffffff',
    },
    {
      id: 2,
      label: 'MAN TGX',
      color: '#7f8c8d',
      imagen: 'wallpapers/bg-2.jpg',
      colorTexto: '#ffffff',
    },
    {
      id: 3,
      label: 'Scania S530',
      color: '#27ae60',
      imagen: 'wallpapers/bg-3.jpg',
      colorTexto: '#ffffff',
    },
    {
      id: 4,
      label: 'Mercedes Actros',
      color: '#2980b9',
      imagen: 'wallpapers/bg-4.jpg',
      colorTexto: '#ffffff',
    },
    {
      id: 5,
      label: 'Flota multimarc',
      color: '#1a1a2e',
      imagen: 'wallpapers/bg-5.jpg',
      colorTexto: '#ffffff',
    },
    {
      id: 6,
      label: 'Noche en ruta',
      color: '#0d1b2a',
      imagen: 'wallpapers/bg-6.avif',
      colorTexto: '#ffffff',
    },
  ];

  wallpaperActivo = signal<Wallpaper>(this.wallpapers[0]);

  caracteristicas = [
    { icono: 'map',               texto: 'Seguimiento GPS en tiempo real' },
    { icono: 'build',             texto: 'Gestión de taller y mantenimiento' },
    { icono: 'local_gas_station', texto: 'Control de combustible y anomalías' },
    { icono: 'bar_chart',         texto: 'Reportes y exportación de datos' },
  ];

  ngOnInit(): void {
    if (this.servicioAuth.instance.getAllAccounts().length > 0) {
      this.enrutador.navigate(['/']);
    }
    // Restore from localStorage
    const guardado = localStorage.getItem(LS_KEY);
    if (guardado !== null) {
      const id = parseInt(guardado, 10);
      const wp = this.wallpapers.find(w => w.id === id);
      if (wp) this.wallpaperActivo.set(wp);
    }
  }

  seleccionarWallpaper(wp: Wallpaper): void {
    this.wallpaperActivo.set(wp);
    localStorage.setItem(LS_KEY, String(wp.id));
  }

  iniciarSesion(): void { this.servicioAuth.loginRedirect(); }
}
