import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { InvitacionesService, InvitacionResumen } from '@core/services/invitaciones.service';
import { PerfilService } from '@core/services/perfil.service';
import { ETIQUETA_ROL } from '@core/services/perfil.service';
import { RolUsuario } from '@core/models';

type Paso = 'ingreso' | 'confirmar' | 'exito' | 'error';

@Component({
  selector: 'app-onboarding',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule, MatInputModule, MatProgressSpinnerModule,
  ],
  template: `
    <div class="fondo-onboarding">
      <div class="tarjeta-onboarding">

        <!-- Logo -->
        <div class="logo-area">
          <div class="logotipo"><mat-icon>local_shipping</mat-icon></div>
          <h1>TruckManager Pro</h1>
        </div>

        <!-- ── Paso: ingresar código ── -->
        @if (paso() === 'ingreso') {
          <div class="contenido-paso">
            <div class="icono-paso"><mat-icon>vpn_key</mat-icon></div>
            <h2>Código de invitación</h2>
            <p class="subtitulo">
              Tu administrador te envió un código para unirte a la empresa.<br>
              Ingrésalo aquí para completar tu registro.
            </p>

            <div class="campo-codigo">
              <mat-form-field appearance="fill" class="ancho-completo">
                <mat-label>Código de invitación</mat-label>
                <input matInput [(ngModel)]="codigoIngresado" placeholder="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
                       (keyup.enter)="validarCodigo()" [disabled]="cargando()" />
                <mat-icon matSuffix>qr_code</mat-icon>
              </mat-form-field>
            </div>

            @if (mensajeError()) {
              <div class="alerta-error">
                <mat-icon>error_outline</mat-icon>
                {{ mensajeError() }}
              </div>
            }

            <button mat-flat-button class="btn-accion" (click)="validarCodigo()" [disabled]="!codigoIngresado.trim() || cargando()">
              @if (cargando()) { <mat-spinner diameter="20" /> }
              @else { <mat-icon>arrow_forward</mat-icon> Continuar }
            </button>
          </div>
        }

        <!-- ── Paso: confirmar invitación ── -->
        @if (paso() === 'confirmar') {
          <div class="contenido-paso">
            <div class="icono-paso confirmado"><mat-icon>verified</mat-icon></div>
            <h2>Invitación válida</h2>
            <p class="subtitulo">Revisa los detalles de tu acceso antes de confirmar.</p>

            <div class="detalle-invitacion">
              <div class="detalle-fila">
                <mat-icon>badge</mat-icon>
                <div>
                  <div class="detalle-etiqueta">Rol asignado</div>
                  <div class="detalle-valor">{{ etiquetaRol(invitacion()!.rol) }}</div>
                </div>
              </div>
              @if (invitacion()!.nota) {
                <div class="detalle-fila">
                  <mat-icon>notes</mat-icon>
                  <div>
                    <div class="detalle-etiqueta">Nota del administrador</div>
                    <div class="detalle-valor">{{ invitacion()!.nota }}</div>
                  </div>
                </div>
              }
              <div class="detalle-fila">
                <mat-icon>schedule</mat-icon>
                <div>
                  <div class="detalle-etiqueta">Válida hasta</div>
                  <div class="detalle-valor">{{ invitacion()!.expiresAt | date:'dd/MM/yyyy HH:mm' }}</div>
                </div>
              </div>
            </div>

            @if (mensajeError()) {
              <div class="alerta-error">
                <mat-icon>error_outline</mat-icon>
                {{ mensajeError() }}
              </div>
            }

            <div class="acciones-doble">
              <button mat-button (click)="volver()" [disabled]="cargando()">Cambiar código</button>
              <button mat-flat-button class="btn-accion" (click)="aceptarInvitacion()" [disabled]="cargando()">
                @if (cargando()) { <mat-spinner diameter="20" /> }
                @else { <mat-icon>check</mat-icon> Confirmar acceso }
              </button>
            </div>
          </div>
        }

        <!-- ── Paso: éxito ── -->
        @if (paso() === 'exito') {
          <div class="contenido-paso">
            <div class="icono-paso exito"><mat-icon>celebration</mat-icon></div>
            <h2>¡Listo! Ya tienes acceso</h2>
            <p class="subtitulo">Tu cuenta fue vinculada correctamente. Serás redirigido al sistema.</p>
            <div class="spinner-centrado" style="margin-top:24px"><mat-spinner diameter="32" /></div>
          </div>
        }

        <!-- ── Paso: error grave ── -->
        @if (paso() === 'error') {
          <div class="contenido-paso">
            <div class="icono-paso error"><mat-icon>error</mat-icon></div>
            <h2>Algo salió mal</h2>
            <p class="subtitulo">{{ mensajeError() }}</p>
            <button mat-flat-button class="btn-accion" (click)="reintentar()">
              <mat-icon>refresh</mat-icon> Intentar de nuevo
            </button>
          </div>
        }

      </div>
    </div>
  `,
  styles: [`
    .fondo-onboarding {
      min-height: 100vh;
      display: flex; align-items: center; justify-content: center;
      background: linear-gradient(145deg, var(--azul-50) 0%, #fff 60%, var(--azul-100) 100%);
    }
    .tarjeta-onboarding {
      background: #fff;
      border: 1px solid var(--azul-200);
      border-radius: var(--radio-xl);
      padding: 44px 40px;
      width: 480px;
      max-width: 92vw;
      box-shadow: 0 20px 60px rgba(27,44,64,.15), 0 4px 16px rgba(27,44,64,.08);
    }
    .logo-area {
      display: flex; align-items: center; gap: 12px; margin-bottom: 32px;
    }
    .logotipo {
      width: 44px; height: 44px;
      background: linear-gradient(135deg, var(--azul-600), var(--azul-400));
      border-radius: var(--radio-md);
      display: flex; align-items: center; justify-content: center;
      mat-icon { color: #fff; font-size: 22px; width: 22px; height: 22px; }
    }
    h1 { font-size: 18px; font-weight: 700; color: var(--azul-900); margin: 0; }
    .contenido-paso { display: flex; flex-direction: column; align-items: center; text-align: center; }
    .icono-paso {
      width: 64px; height: 64px; border-radius: 50%;
      background: var(--azul-50);
      display: flex; align-items: center; justify-content: center;
      margin-bottom: 20px;
      mat-icon { font-size: 32px; width: 32px; height: 32px; color: var(--azul-500); }
      &.confirmado { background: #d1fae5; mat-icon { color: #059669; } }
      &.exito      { background: #d1fae5; mat-icon { color: #059669; } }
      &.error      { background: #fee2e2; mat-icon { color: #dc2626; } }
    }
    h2 { font-size: 20px; font-weight: 700; color: var(--ink); margin: 0 0 8px; }
    .subtitulo { font-size: 14px; color: var(--ink-mid); line-height: 1.6; margin: 0 0 24px; }
    .campo-codigo { width: 100%; }
    .ancho-completo { width: 100%; }
    .alerta-error {
      display: flex; align-items: center; gap: 8px;
      background: #fee2e2; color: #dc2626;
      border-radius: var(--radius-sm); padding: 10px 14px;
      font-size: 13px; margin-bottom: 16px; width: 100%; text-align: left;
      mat-icon { font-size: 18px; width: 18px; height: 18px; flex-shrink: 0; }
    }
    .btn-accion {
      background: linear-gradient(135deg, var(--azul-600), var(--azul-500)) !important;
      color: #fff !important;
      border-radius: var(--radio-md) !important;
      height: 44px; padding: 0 24px !important;
      font-size: 14px !important; font-weight: 600 !important;
      display: flex; align-items: center; gap: 8px;
      mat-spinner { --mdc-circular-progress-active-indicator-color: #fff; }
    }
    .detalle-invitacion {
      width: 100%; background: var(--slate); border-radius: var(--radius-md);
      padding: 16px; display: flex; flex-direction: column; gap: 12px;
      margin-bottom: 24px; text-align: left;
    }
    .detalle-fila { display: flex; align-items: flex-start; gap: 12px;
      mat-icon { color: var(--azul-500); font-size: 20px; width: 20px; height: 20px; flex-shrink: 0; margin-top: 2px; }
    }
    .detalle-etiqueta { font-size: 11px; color: var(--ink-soft); font-weight: 600; text-transform: uppercase; letter-spacing: .05em; }
    .detalle-valor    { font-size: 14px; font-weight: 500; color: var(--ink); margin-top: 2px; }
    .acciones-doble { display: flex; gap: 12px; align-items: center; width: 100%; justify-content: flex-end; }
    .spinner-centrado { display: flex; justify-content: center; }
  `],
})
export class OnboardingComponent implements OnInit {
  private readonly invitacionesSvc = inject(InvitacionesService);
  private readonly perfilSvc       = inject(PerfilService);
  private readonly router          = inject(Router);
  private readonly route           = inject(ActivatedRoute);

  paso          = signal<Paso>('ingreso');
  cargando      = signal(false);
  mensajeError  = signal('');
  invitacion    = signal<InvitacionResumen | null>(null);
  codigoIngresado = '';

  ngOnInit(): void {
    // Si viene con ?invite=TOKEN en la URL, pre-rellenar y validar
    const tokenUrl = this.route.snapshot.queryParamMap.get('invite');
    if (tokenUrl) {
      this.codigoIngresado = tokenUrl;
      this.validarCodigo();
    }
  }

  validarCodigo(): void {
    const token = this.codigoIngresado.trim();
    if (!token) return;
    this.cargando.set(true);
    this.mensajeError.set('');

    this.invitacionesSvc.validar(token).subscribe({
      next: (inv) => {
        this.invitacion.set(inv);
        this.paso.set('confirmar');
        this.cargando.set(false);
      },
      error: (e) => {
        this.cargando.set(false);
        const msg = e?.error?.message ?? 'Código inválido o ya utilizado.';
        this.mensajeError.set(msg);
      },
    });
  }

  aceptarInvitacion(): void {
    const token = this.codigoIngresado.trim();
    this.cargando.set(true);
    this.mensajeError.set('');

    this.invitacionesSvc.aceptar(token).subscribe({
      next: (perfil) => {
        this.cargando.set(false);
        this.paso.set('exito');
        // Actualizar el servicio de perfil con los datos reales
        if (perfil.rol) this.perfilSvc.rolActual.set(perfil.rol as RolUsuario);
        // Redirigir al dashboard tras breve pausa
        setTimeout(() => this.router.navigate(['/dashboard']), 1800);
      },
      error: (e) => {
        this.cargando.set(false);
        const msg = e?.error?.message ?? 'Error al aceptar la invitación.';
        this.mensajeError.set(msg);
        this.paso.set('error');
      },
    });
  }

  volver(): void {
    this.paso.set('ingreso');
    this.mensajeError.set('');
  }

  reintentar(): void {
    this.paso.set('ingreso');
    this.codigoIngresado = '';
    this.mensajeError.set('');
  }

  etiquetaRol(rol: string): string {
    return ETIQUETA_ROL[rol as RolUsuario] ?? rol;
  }
}
