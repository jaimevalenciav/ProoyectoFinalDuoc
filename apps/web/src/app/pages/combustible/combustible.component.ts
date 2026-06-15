import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MsalService } from '@azure/msal-angular';
import { OperacionesService } from '@core/services/operaciones.service';
import { VehiculosService } from '@core/services/vehiculos.service';
import { DialogoService } from '@core/services/dialogo.service';
import { AlertaCombustible, CargaCombustible, Vehiculo } from '@core/models';

interface AlertaFormulario {
  tipo: 'error' | 'warning' | 'info';
  icono: string;
  mensaje: string;
}

const TIPOS_COMBUSTIBLE = [
  'Diesel', 'Ben97', 'Ben95', 'Ben93', 'Kerosene',
  'Carga Eléctrica', 'Hidrógeno', 'Gas', 'AdBlue',
] as const;

type TipoCombustible = typeof TIPOS_COMBUSTIBLE[number];

/** Unidad de volumen según tipo */
function unidadTipo(tipo: TipoCombustible): string {
  if (tipo === 'Carga Eléctrica') return 'kWh';
  if (tipo === 'Hidrógeno' || tipo === 'Gas') return 'kg';
  return 'L';
}

@Component({
  selector: 'app-combustible',
  standalone: true,
  imports: [
    CommonModule, FormsModule, ReactiveFormsModule,
    MatTableModule, MatButtonModule, MatIconModule,
    MatInputModule, MatSelectModule, MatProgressSpinnerModule,
    MatSnackBarModule, MatTooltipModule,
  ],
  template: `
    <div class="encabezado-pagina">
      <div>
        <h1>Combustible</h1>
        <p class="subtitulo-pagina">Registro de cargas y control de rendimiento</p>
      </div>
      <button mat-flat-button class="btn-principal" (click)="abrirFormulario()">
        <mat-icon>add</mat-icon> Registrar carga
      </button>
    </div>

    <!-- ══════════ PANEL: Alertas activas ══════════ -->
    @if (alertasActivas().length > 0) {
      <div class="panel-alertas">
        <div class="alertas-cabecera">
          <div style="display:flex;align-items:center;gap:8px">
            <mat-icon class="icono-alerta-panel">notifications_active</mat-icon>
            <span class="titulo-alertas">
              {{ alertasActivas().length }} alerta{{ alertasActivas().length > 1 ? 's' : '' }} pendiente{{ alertasActivas().length > 1 ? 's' : '' }}
            </span>
          </div>
          <button mat-button class="btn-historial" (click)="toggleHistorial()">
            <mat-icon>{{ mostrarHistorial() ? 'expand_less' : 'history' }}</mat-icon>
            {{ mostrarHistorial() ? 'Ocultar historial' : 'Ver historial (' + alertasHistorial().length + ')' }}
          </button>
        </div>

        <div class="lista-alertas-panel">
          @for (a of alertasActivas(); track a.id) {
            <div [class]="'tarjeta-alerta tarjeta-' + a.tipo">
              <div class="alerta-izq">
                <span [class]="'badge-tipo badge-tipo-' + a.tipo">
                  {{ a.tipo === 'error' ? 'ERROR' : a.tipo === 'warning' ? 'AVISO' : 'INFO' }}
                </span>
                <mat-icon class="alerta-icono-mat">{{ a.icono || iconoTipo(a.tipo) }}</mat-icon>
                <div class="alerta-cuerpo">
                  <span class="alerta-patente">{{ patenteVehiculo(a.vehiculoId) }}</span>
                  <span class="alerta-mensaje">{{ a.mensaje }}</span>
                  <span class="alerta-fecha">{{ a.createdAt | date:'dd/MM/yyyy HH:mm' }}</span>
                </div>
              </div>
              <button class="btn-leido" (click)="marcarLeida(a)" [disabled]="procesandoLeida()">
                <mat-icon>done</mat-icon>
                Leído
              </button>
            </div>
          }
        </div>
      </div>
    }

    <!-- ══════════ PANEL: Historial alertas ══════════ -->
    @if (mostrarHistorial()) {
      <div class="panel-historial">
        <div class="historial-cabecera">
          <mat-icon>history</mat-icon>
          <span>Historial de alertas</span>
        </div>
        @if (cargandoHistorial()) {
          <div class="spinner-centrado" style="padding:24px"><mat-spinner diameter="28" /></div>
        } @else if (alertasHistorial().length === 0) {
          <div style="padding:20px;text-align:center;color:var(--ink-soft);font-size:13px">Sin alertas en historial</div>
        } @else {
          <div class="tabla-historial">
            @for (a of alertasHistorial(); track a.id) {
              <div class="fila-historial">
                <span [class]="'badge-tipo badge-tipo-' + a.tipo + ' badge-tipo-leido'">
                  {{ a.tipo === 'warning' ? 'AVISO' : a.tipo.toUpperCase() }}
                </span>
                <span class="hist-patente">{{ patenteVehiculo(a.vehiculoId) }}</span>
                <span class="hist-mensaje">{{ a.mensaje }}</span>
                <span class="hist-leido-por">
                  <mat-icon style="font-size:13px;width:13px;height:13px">done_all</mat-icon>
                  {{ a.leidaPor }} · {{ a.leidaAt | date:'dd/MM/yy HH:mm' }}
                </span>
              </div>
            }
          </div>
        }
      </div>
    }

    <!-- Banner anomalías de consumo sin alerta en BD -->
    @if (anomaliasVisibles().length > 0) {
      <div class="panel-alertas panel-alertas-warning">
        <div class="alertas-cabecera alertas-cabecera-warning">
          <div style="display:flex;align-items:center;gap:8px">
            <mat-icon class="icono-alerta-panel">warning_amber</mat-icon>
            <span class="titulo-alertas">
              {{ anomaliasVisibles().length }} anomalía{{ anomaliasVisibles().length > 1 ? 's' : '' }} de consumo detectada{{ anomaliasVisibles().length > 1 ? 's' : '' }}
            </span>
          </div>
        </div>
        <div class="lista-alertas-panel">
          @for (a of anomaliasVisibles(); track a.id) {
            <div class="tarjeta-alerta tarjeta-warning">
              <div class="alerta-izq">
                <span class="badge-tipo badge-tipo-warning">AVISO</span>
                <mat-icon class="alerta-icono-mat">directions_bus</mat-icon>
                <div class="alerta-cuerpo">
                  <span class="alerta-patente">{{ patenteVehiculo(a.vehiculoId) }}</span>
                  <span class="alerta-mensaje">
                    Consumo anómalo:
                    <span [class]="claseConsumo(a.consumo100km!)">{{ a.consumo100km | number:'1.1-1' }} L/100km</span>
                    — {{ a.litros | number:'1.0-0' }} L · {{ a.kmVehiculo | number:'1.0-0' }} km
                  </span>
                  <span class="alerta-fecha">{{ a.fechaCarga | date:'dd/MM/yyyy' }}</span>
                </div>
              </div>
              <button class="btn-leido" (click)="descartarAnomalia(a)" [disabled]="procesandoLeida()">
                <mat-icon>done</mat-icon>
                Leído
              </button>
            </div>
          }
        </div>
      </div>
    }

    <!-- Filtros -->
    <div class="barra-filtros">
      <mat-form-field appearance="fill">
        <mat-label>Vehículo</mat-label>
        <mat-select [(ngModel)]="filtroVehiculo" (ngModelChange)="cargar()">
          <mat-option value="">Todos</mat-option>
          @for (v of vehiculos(); track v.id) {
            <mat-option [value]="v.id">{{ v.patente }} — {{ v.marca }} {{ v.modelo }}</mat-option>
          }
        </mat-select>
      </mat-form-field>
      <mat-form-field appearance="fill">
        <mat-label>Desde</mat-label>
        <input matInput type="date" [(ngModel)]="fechaDesde" (change)="cargar()" />
      </mat-form-field>
      <mat-form-field appearance="fill">
        <mat-label>Hasta</mat-label>
        <input matInput type="date" [(ngModel)]="fechaHasta" (change)="cargar()" />
      </mat-form-field>
    </div>

    @if (cargando()) {
      <div class="spinner-centrado"><mat-spinner diameter="40" /></div>
    } @else {
      <div class="superficie" style="padding:0;overflow:hidden">
        <table mat-table [dataSource]="cargas()">
          <ng-container matColumnDef="numDocumento">
            <th mat-header-cell *matHeaderCellDef>Comprobante</th>
            <td mat-cell *matCellDef="let c"><span class="texto-comprobante">{{ c.numDocumento ?? '—' }}</span></td>
          </ng-container>
          <ng-container matColumnDef="fechaCarga">
            <th mat-header-cell *matHeaderCellDef>Fecha</th>
            <td mat-cell *matCellDef="let c">{{ c.fechaCarga | date:'dd/MM/yyyy' }}</td>
          </ng-container>
          <ng-container matColumnDef="tipo">
            <th mat-header-cell *matHeaderCellDef>Tipo</th>
            <td mat-cell *matCellDef="let c">
              <span [class]="'chip-tipo chip-tipo-' + slugTipo(c.tipoCombustible)">
                {{ c.tipoCombustible || 'Diesel' }}
              </span>
            </td>
          </ng-container>
          <ng-container matColumnDef="vehiculo">
            <th mat-header-cell *matHeaderCellDef>Vehículo</th>
            <td mat-cell *matCellDef="let c">
              <span class="celda-patente">{{ patenteVehiculo(c.vehiculoId) }}</span>
              <span class="texto-atenuado" style="font-size:12px;display:block">{{ c.proveedor }}</span>
            </td>
          </ng-container>
          <ng-container matColumnDef="litros">
            <th mat-header-cell *matHeaderCellDef>Litros</th>
            <td mat-cell *matCellDef="let c"><strong>{{ c.litros | number:'1.1-1' }} L</strong></td>
          </ng-container>
          <ng-container matColumnDef="precioLitro">
            <th mat-header-cell *matHeaderCellDef>Precio/L</th>
            <td mat-cell *matCellDef="let c">{{ c.precioLitro | currency:'CLP':'$':'1.0-0' }}</td>
          </ng-container>
          <ng-container matColumnDef="costoTotal">
            <th mat-header-cell *matHeaderCellDef>Total</th>
            <td mat-cell *matCellDef="let c"><strong>{{ c.costoTotal | currency:'CLP':'$':'1.0-0' }}</strong></td>
          </ng-container>
          <ng-container matColumnDef="kmVehiculo">
            <th mat-header-cell *matHeaderCellDef>Km al cargar</th>
            <td mat-cell *matCellDef="let c">{{ c.kmVehiculo | number:'1.0-0' }} km</td>
          </ng-container>
          <ng-container matColumnDef="rendimiento">
            <th mat-header-cell *matHeaderCellDef>Consumo</th>
            <td mat-cell *matCellDef="let c">
              @if (c.consumo100km) {
                <span [class]="claseConsumo(c.consumo100km)">{{ c.consumo100km | number:'1.1-1' }} L/100km</span>
              } @else {
                <span class="texto-atenuado" style="font-size:12px">Sin dato anterior</span>
              }
            </td>
          </ng-container>
          <ng-container matColumnDef="acciones">
            <th mat-header-cell *matHeaderCellDef></th>
            <td mat-cell *matCellDef="let c">
              <button mat-icon-button color="warn" (click)="eliminar(c)" matTooltip="Eliminar">
                <mat-icon>delete_outline</mat-icon>
              </button>
            </td>
          </ng-container>
          <tr mat-header-row *matHeaderRowDef="columnas"></tr>
          <tr mat-row *matRowDef="let fila; columns: columnas;"></tr>
        </table>
        @if (cargas().length === 0) {
          <div class="estado-vacio-tabla">
            <mat-icon>local_gas_station</mat-icon>
            <p>No hay registros de carga.</p>
          </div>
        }
      </div>
    }

    <!-- ══════════ MODAL: Formulario carga ══════════ -->
    @if (mostrarFormulario()) {
      <div class="capa-modal" (click)="cerrarFormulario()">
        <div class="panel-modal" style="width:560px;max-height:92vh;overflow-y:auto" (click)="$event.stopPropagation()">
          <h2>Registrar carga de combustible</h2>

          <!-- ── Selector de tipo de combustible ── -->
          <div class="bloque-tipo-comb">
            <label class="etiqueta-tipo-comb">Tipo de combustible</label>
            <div class="grilla-tipos">
              @for (t of tiposCombustible; track t) {
                <button type="button"
                  [class.tipo-activo]="tipoCombustible() === t"
                  class="btn-tipo"
                  (click)="seleccionarTipo(t)">{{ t }}</button>
              }
            </div>
          </div>

          <form [formGroup]="formulario" (ngSubmit)="guardar()">

            <!-- Vehículo -->
            <mat-form-field appearance="fill" class="ancho-completo">
              <mat-label>Vehículo *</mat-label>
              <mat-select formControlName="vehiculoId" (selectionChange)="onVehiculoChange($event.value)">
                @for (v of vehiculos(); track v.id) {
                  <mat-option [value]="v.id">{{ v.patente }} — {{ v.marca }} {{ v.modelo }}</mat-option>
                }
              </mat-select>
            </mat-form-field>

            <!-- Resumen última carga -->
            @if (ultimaCarga()) {
              <div class="resumen-ultima-carga">
                <mat-icon>history</mat-icon>
                <div>
                  <span class="etiqueta-ultima">Última carga registrada</span>
                  <span>
                    {{ ultimaCarga()!.fechaCarga | date:'dd/MM/yyyy' }} —
                    <strong>{{ ultimaCarga()!.litros | number:'1.1-1' }} L</strong> —
                    {{ ultimaCarga()!.kmVehiculo | number:'1.0-0' }} km
                    @if (ultimaCarga()!.numDocumento) {
                      · Comprobante {{ ultimaCarga()!.numDocumento }}
                    }
                  </span>
                </div>
              </div>
            }

            <!-- ── Alertas tiempo real (estilo badge) ── -->
            @if (alertasFormulario().length > 0) {
              <div class="bloque-alertas">
                @for (a of alertasFormulario(); track a.mensaje) {
                  <div [class]="'alerta-badge alerta-badge-' + a.tipo">
                    <mat-icon>{{ a.icono }}</mat-icon>
                    <span>{{ a.mensaje }}</span>
                  </div>
                }
              </div>
            }

            <!-- Km y Nº comprobante -->
            <div class="dos-columnas">
              <mat-form-field appearance="fill">
                <mat-label>Km al cargar *</mat-label>
                <input matInput type="number" formControlName="kmVehiculo" (input)="recalcularAlertas()" />
                <mat-icon matSuffix style="font-size:18px;color:var(--ink-soft)">speed</mat-icon>
              </mat-form-field>
              <mat-form-field appearance="fill">
                <mat-label>Nº comprobante</mat-label>
                <input matInput formControlName="numDocumento" placeholder="Ej: 000123456" />
                <mat-icon matSuffix style="font-size:18px;color:var(--ink-soft)">receipt</mat-icon>
              </mat-form-field>
            </div>

            <!-- Litros y precio -->
            <div class="dos-columnas">
              <mat-form-field appearance="fill">
                <mat-label>{{ tipoCombustible() === 'Carga Eléctrica' ? 'kWh cargados' : tipoCombustible() === 'Hidrógeno' || tipoCombustible() === 'Gas' ? 'kg cargados' : 'Litros cargados' }} *</mat-label>
                <input matInput type="number" step="0.1" formControlName="litros" (input)="recalcularAlertas()" />
                <span matSuffix>{{ unidadActual() }}</span>
              </mat-form-field>
              <mat-form-field appearance="fill">
                <mat-label>Precio por litro (CLP) *</mat-label>
                <input matInput type="number" formControlName="precioLitro" />
                <span matPrefix>$&nbsp;</span>
              </mat-form-field>
            </div>

            <!-- Total estimado -->
            @if (totalEstimado() > 0) {
              <div class="total-estimado">
                <mat-icon>payments</mat-icon>
                <span>Total estimado: <strong>{{ totalEstimado() | currency:'CLP':'$':'1.0-0' }}</strong></span>
              </div>
            }

            <!-- Rendimiento proyectado -->
            @if (rendimientoProyectado() !== null) {
              <div class="total-estimado" [class.rend-malo]="rendimientoProyectado()! > 20" [class.rend-medio]="rendimientoProyectado()! > 15 && rendimientoProyectado()! <= 20">
                <mat-icon>{{ rendimientoProyectado()! > 20 ? 'trending_down' : rendimientoProyectado()! > 15 ? 'trending_flat' : 'trending_up' }}</mat-icon>
                <span>Rendimiento proyectado: <strong>{{ rendimientoProyectado() | number:'1.1-1' }} L/100km</strong></span>
              </div>
            }

            <!-- Proveedor / Estación -->
            <div class="dos-columnas">
              <mat-form-field appearance="fill">
                <mat-label>Proveedor *</mat-label>
                <input matInput formControlName="proveedor" placeholder="Ej: COPEC, SHELL, PETROBRAS" />
              </mat-form-field>
              <mat-form-field appearance="fill">
                <mat-label>Estación</mat-label>
                <input matInput formControlName="estacion" placeholder="Nombre o dirección" />
              </mat-form-field>
            </div>

            <!-- Fecha -->
            <mat-form-field appearance="fill" class="ancho-completo">
              <mat-label>Fecha de carga</mat-label>
              <input matInput type="date" formControlName="fechaCarga" />
            </mat-form-field>

            <div class="acciones-formulario">
              <button mat-button type="button" (click)="cerrarFormulario()">Cancelar</button>
              <button mat-flat-button class="btn-principal" type="submit"
                      [disabled]="formulario.invalid || guardando() || tieneErrores()">
                {{ guardando() ? 'Registrando…' : 'Registrar carga' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    }
  `,
  styles: [`
    .subtitulo-pagina { font-size: 13px; color: var(--ink-soft); margin: 2px 0 0; }

    /* ── Panel Alertas Activas ─────────────────────────────── */
    .panel-alertas {
      background: var(--surface);
      border: 1.5px solid #C25E01;
      border-radius: var(--radius-md);
      margin-bottom: 20px;
      overflow: hidden;
    }
    .alertas-cabecera {
      display: flex; justify-content: space-between; align-items: center;
      padding: 10px 16px;
      background: #C25E01;
      color: #fff;
    }
    .icono-alerta-panel { font-size: 20px; width: 20px; height: 20px; color: #fff; }
    .titulo-alertas { font-size: 13px; font-weight: 700; letter-spacing: .02em; }
    .btn-historial {
      color: #fff !important; font-size: 12px !important;
      opacity: .9;
      display: flex; align-items: center; gap: 4px;
      mat-icon { font-size: 16px; width: 16px; height: 16px; }
    }
    .lista-alertas-panel { display: flex; flex-direction: column; gap: 0; }

    /* Tarjetas de alerta en el panel */
    .tarjeta-alerta {
      display: flex; align-items: center; justify-content: space-between;
      padding: 10px 16px;
      border-bottom: 1px solid var(--slate-dark);
      gap: 12px;
      &:last-child { border-bottom: none; }
    }
    .tarjeta-warning { border-left: 4px solid #D97706; }
    .tarjeta-info    { border-left: 4px solid #007AF5; }
    .tarjeta-error   { border-left: 4px solid #C10A5A; }
    .panel-alertas-warning { border-color: #D97706; }
    .alertas-cabecera-warning { background: #D97706; }

    .alerta-izq { display: flex; align-items: flex-start; gap: 10px; flex: 1; }
    .alerta-icono-mat { font-size: 18px; width: 18px; height: 18px; color: var(--ink-soft); flex-shrink: 0; margin-top: 2px; }
    .alerta-cuerpo { display: flex; flex-direction: column; gap: 2px; }
    .alerta-patente { font-weight: 700; font-size: 13px; color: var(--ink); }
    .alerta-mensaje { font-size: 13px; color: var(--ink-mid); }
    .alerta-fecha   { font-size: 11px; color: var(--ink-soft); }

    /* Badge tipo en panel */
    .badge-tipo {
      display: inline-flex; align-items: center;
      padding: 2px 7px; border-radius: 4px;
      font-size: 9px; font-weight: 700; letter-spacing: .06em;
      white-space: nowrap; flex-shrink: 0; margin-top: 2px;
    }
    .badge-tipo-warning { background: #C25E01; color: #fff; }
    .badge-tipo-info    { background: #007AF5; color: #fff; }
    .badge-tipo-error   { background: #C10A5A; color: #fff; }
    .badge-tipo-leido   { opacity: .55; }

    /* Botón leído */
    .btn-leido {
      display: inline-flex; align-items: center; gap: 4px;
      padding: 5px 12px; border-radius: 4px; border: 1.5px solid #1B2C40;
      background: transparent; color: #1B2C40;
      font-size: 12px; font-weight: 600; cursor: pointer;
      white-space: nowrap; flex-shrink: 0; font-family: var(--fuente);
      transition: background .15s, color .15s;
      mat-icon { font-size: 16px; width: 16px; height: 16px; }
      &:hover:not(:disabled) { background: #1B2C40; color: #fff; }
      &:disabled { opacity: .4; cursor: not-allowed; }
    }

    /* ── Panel Historial ───────────────────────────────────── */
    .panel-historial {
      background: var(--slate);
      border: 1px solid var(--slate-dark);
      border-radius: var(--radius-md);
      margin-bottom: 16px;
      overflow: hidden;
    }
    .historial-cabecera {
      display: flex; align-items: center; gap: 8px;
      padding: 8px 16px;
      background: var(--slate-mid);
      font-size: 12px; font-weight: 700; color: var(--ink-mid);
      letter-spacing: .04em; text-transform: uppercase;
      mat-icon { font-size: 15px; width: 15px; height: 15px; }
    }
    .tabla-historial { display: flex; flex-direction: column; }
    .fila-historial {
      display: flex; align-items: center; gap: 10px;
      padding: 8px 16px;
      border-bottom: 1px solid var(--slate-dark);
      font-size: 12px; color: var(--ink-mid);
      &:last-child { border-bottom: none; }
    }
    .hist-patente  { font-weight: 700; color: var(--ink); min-width: 80px; }
    .hist-mensaje  { flex: 1; }
    .hist-leido-por {
      display: flex; align-items: center; gap: 3px;
      font-size: 11px; color: var(--ink-soft); white-space: nowrap;
      mat-icon { color: #16a34a; }
    }

    /* (banner-anomalias replaced by panel-alertas-warning) */

    /* ── Tabla ────────────────────────────────────────────── */
    .celda-patente { font-weight: 700; font-size: 14px; }
    .texto-comprobante { font-family: monospace; font-size: 13px; color: var(--ink-soft); }
    .estado-vacio-tabla {
      display: flex; flex-direction: column; align-items: center; padding: 48px;
      gap: 8px; color: var(--ink-soft);
      mat-icon { font-size: 48px; width: 48px; height: 48px; opacity: .3; }
    }

    /* ── Filtros ──────────────────────────────────────────── */
    .barra-filtros {
      display: flex; flex-wrap: wrap; gap: 12px; padding: 16px 0;
      mat-form-field { min-width: 150px; }
    }

    /* ── Formulario modal ─────────────────────────────────── */
    .resumen-ultima-carga {
      display: flex; align-items: flex-start; gap: 10px;
      background: var(--azul-50); border: 1px solid var(--azul-200);
      border-radius: var(--radius-sm); padding: 10px 14px;
      margin-bottom: 14px; font-size: 13px; color: var(--azul-700);
      mat-icon { color: var(--azul-500); flex-shrink: 0; margin-top: 2px; }
      div { display: flex; flex-direction: column; gap: 2px; }
      .etiqueta-ultima { font-size: 10px; font-weight: 700; text-transform: uppercase;
        letter-spacing: .6px; color: var(--azul-400); }
    }

    /* ── Alertas inline formulario (estilo badge sólido) ──── */
    .bloque-alertas { display: flex; flex-direction: column; gap: 6px; margin-bottom: 14px; }
    .alerta-badge {
      display: flex; align-items: center; gap: 10px;
      border-radius: var(--radius-sm); padding: 9px 14px;
      font-size: 13px; font-weight: 500; color: #fff;
      mat-icon { font-size: 18px; width: 18px; height: 18px; flex-shrink: 0; color: #fff; }
    }
    .alerta-badge-error   { background: #C10A5A; }
    .alerta-badge-warning { background: #C25E01; }
    .alerta-badge-info    { background: #1B2C40; }

    /* ── Total / rendimiento ──────────────────────────────── */
    .total-estimado {
      display: flex; align-items: center; gap: 8px;
      background: #F0FDF4; border: 1px solid #BBF7D0;
      border-radius: var(--radius-sm); padding: 8px 14px;
      font-size: 13px; color: #166534; margin-bottom: 14px;
      mat-icon { color: #16A34A; }
    }
    .total-estimado.rend-malo  { background: #FEF2F2; border-color: #FECACA; color: #991B1B; mat-icon { color: #DC2626; } }
    .total-estimado.rend-medio { background: #FFFBEB; border-color: #FDE68A; color: #92400E; mat-icon { color: #D97706; } }

    /* ── Selector tipo combustible ──────────────────────────── */
    .bloque-tipo-comb {
      margin-bottom: 18px;
    }
    .etiqueta-tipo-comb {
      display: block;
      font-size: 11px;
      font-weight: 700;
      color: var(--ink-soft);
      text-transform: uppercase;
      letter-spacing: .06em;
      margin-bottom: 8px;
    }
    .grilla-tipos {
      display: flex;
      flex-wrap: wrap;
      gap: 7px;
    }
    .btn-tipo {
      padding: 5px 13px;
      border-radius: 6px;
      border: 1.5px solid #1B2C40;
      background: #fff;
      color: #1B2C40;
      font-size: 13px;
      font-weight: 500;
      font-family: var(--fuente);
      cursor: pointer;
      transition: background .15s, color .15s, transform .1s;
      white-space: nowrap;
      &:hover:not(.tipo-activo) {
        background: rgba(27,44,64,.07);
      }
    }
    .tipo-activo {
      background: #1B2C40 !important;
      color: #fff !important;
      border-color: #1B2C40 !important;
    }

    /* ── Chip tipo en tabla ─────────────────────────────────── */
    .chip-tipo {
      display: inline-block;
      padding: 2px 8px;
      border-radius: 4px;
      font-size: 11px;
      font-weight: 600;
      white-space: nowrap;
      background: rgba(27,44,64,.1);
      color: #1B2C40;
    }
    .chip-tipo-adblue       { background: #EFF6FF; color: #1D4ED8; }
    .chip-tipo-carga-electrica { background: #F0FDF4; color: #166534; }
    .chip-tipo-hidrogeno    { background: #F0F9FF; color: #0369A1; }
    .chip-tipo-gas          { background: #FEFCE8; color: #92400E; }
    .chip-tipo-kerosene     { background: #FDF4FF; color: #7E22CE; }
    .chip-tipo-ben97        { background: #FFF7ED; color: #C2410C; }
    .chip-tipo-ben95        { background: #FFF7ED; color: #D97706; }
    .chip-tipo-ben93        { background: #FEF9C3; color: #854D0E; }
  `],
})
export class CombustibleComponent implements OnInit {
  private readonly svc          = inject(OperacionesService);
  private readonly svcVehiculos = inject(VehiculosService);
  private readonly msalSvc      = inject(MsalService);
  private readonly fb           = inject(FormBuilder);
  private readonly snack        = inject(MatSnackBar);
  private readonly dialogo      = inject(DialogoService);

  readonly tiposCombustible = TIPOS_COMBUSTIBLE;
  columnas = ['numDocumento', 'fechaCarga', 'tipo', 'vehiculo', 'litros', 'precioLitro', 'costoTotal', 'kmVehiculo', 'rendimiento', 'acciones'];

  tipoCombustible   = signal<TipoCombustible>('Diesel');
  cargando          = signal(true);
  guardando         = signal(false);
  procesandoLeida   = signal(false);
  mostrarFormulario = signal(false);
  mostrarHistorial  = signal(false);
  cargandoHistorial = signal(false);
  cargas            = signal<CargaCombustible[]>([]);
  anomalias         = signal<CargaCombustible[]>([]);
  vehiculos         = signal<Vehiculo[]>([]);
  ultimaCarga       = signal<CargaCombustible | null>(null);
  alertasFormulario = signal<AlertaFormulario[]>([]);
  alertasActivas    = signal<AlertaCombustible[]>([]);
  alertasHistorial  = signal<AlertaCombustible[]>([]);

  /** IDs de anomalías descartadas manualmente (persiste en localStorage) */
  private readonly DISMISSED_KEY = 'fm_anomalias_descartadas';
  dismissedIds = signal<Set<string>>(
    new Set<string>(JSON.parse(localStorage.getItem('fm_anomalias_descartadas') ?? '[]'))
  );

  /**
   * Anomalías que aún no han sido descartadas y no tienen alerta activa en BD
   * con el mismo cargaId (evita duplicados entre ambos paneles).
   */
  anomaliasVisibles = computed(() => {
    const dismissed    = this.dismissedIds();
    const alertaCargaIds = new Set(this.alertasActivas().map(a => a.cargaId));
    return this.anomalias().filter(a => !dismissed.has(a.id) && !alertaCargaIds.has(a.id));
  });

  // ID de la última carga recién guardada (para asociar alertas)
  private ultimaCargaId: string | null = null;
  // Alertas del formulario al momento de guardar (para persistir)
  private alertasAlGuardar: AlertaFormulario[] = [];

  filtroVehiculo = '';
  fechaDesde     = '';
  fechaHasta     = '';

  formulario = this.fb.group({
    vehiculoId:   ['', Validators.required],
    numDocumento: [''],
    kmVehiculo:   [null as number | null, [Validators.required, Validators.min(0)]],
    litros:       [null as number | null, [Validators.required, Validators.min(1)]],
    precioLitro:  [null as number | null, [Validators.required, Validators.min(1)]],
    proveedor:    ['', Validators.required],
    estacion:     [''],
    fechaCarga:   [new Date().toISOString().slice(0, 10)],
  });

  // ── Computados ──────────────────────────────────────────────

  totalEstimado = computed(() => {
    const v = this.formulario.value;
    return (Number(v.litros) || 0) * (Number(v.precioLitro) || 0);
  });

  rendimientoProyectado = computed((): number | null => {
    const tipo = this.tipoCombustible();
    if (tipo === 'AdBlue' || tipo === 'Carga Eléctrica' || tipo === 'Hidrógeno') return null;
    const ultima = this.ultimaCarga();
    if (!ultima) return null;
    const v = this.formulario.value;
    const km = Number(v.kmVehiculo) || 0;
    const litros = Number(v.litros) || 0;
    const kmRecorridos = km - (ultima.kmVehiculo ?? 0);
    if (kmRecorridos <= 0 || litros <= 0) return null;
    return (litros * 100) / kmRecorridos;
  });

  unidadActual = computed(() => unidadTipo(this.tipoCombustible()));

  tieneErrores = computed(() =>
    this.alertasFormulario().some(a => a.tipo === 'error')
  );

  // ── Lifecycle ───────────────────────────────────────────────

  ngOnInit() {
    this.svcVehiculos.getAll({ size: 200 }).subscribe(r => this.vehiculos.set(r.content));
    this.svc.getAnomalias().subscribe(a => this.anomalias.set(a));
    this.cargar();
    this.cargarAlertasActivas();
  }

  cargar() {
    this.cargando.set(true);
    this.svc.getCargas({
      vehiculoId: this.filtroVehiculo || undefined,
      desde:      this.fechaDesde    || undefined,
      hasta:      this.fechaHasta    || undefined,
    }).subscribe({
      next: r => { this.cargas.set(r.content); this.cargando.set(false); },
      error: () => this.cargando.set(false),
    });
  }

  cargarAlertasActivas() {
    this.svc.getAlertasCombustible(true).subscribe({
      next: a => this.alertasActivas.set(a),
      error: () => { /* silencioso */ },
    });
  }

  // ── Formulario ──────────────────────────────────────────────

  abrirFormulario() {
    const hoy = new Date().toISOString().slice(0, 10);
    this.formulario.reset({ fechaCarga: hoy });
    this.tipoCombustible.set('Diesel');
    this.ultimaCarga.set(null);
    this.alertasFormulario.set([]);
    this.ultimaCargaId = null;
    this.alertasAlGuardar = [];
    this.mostrarFormulario.set(true);
  }

  cerrarFormulario() { this.mostrarFormulario.set(false); }

  onVehiculoChange(vehiculoId: string) {
    this.ultimaCarga.set(null);
    this.alertasFormulario.set([]);
    if (!vehiculoId) return;
    const v = this.vehiculos().find(x => x.id === vehiculoId);
    if (v) this.formulario.patchValue({ kmVehiculo: v.kmActuales });
    this.svc.getUltimaCargaVehiculo(vehiculoId).subscribe({
      next: ultima => { this.ultimaCarga.set(ultima); this.recalcularAlertas(); },
    });
  }

  // ── Validaciones en tiempo real ─────────────────────────────

  recalcularAlertas() {
    const alertas: AlertaFormulario[] = [];
    const ultima = this.ultimaCarga();
    const v = this.formulario.value;
    const km = Number(v.kmVehiculo) || 0;
    const litros = Number(v.litros) || 0;

    if (ultima) {
      const kmAnterior = ultima.kmVehiculo ?? 0;
      const kmRecorridos = km - kmAnterior;

      if (km > 0 && km <= kmAnterior) {
        alertas.push({
          tipo: 'error', icono: 'error',
          mensaje: `El kilometraje ingresado (${km.toLocaleString('es-CL')} km) es menor o igual a la última carga registrada (${kmAnterior.toLocaleString('es-CL')} km). Verifique el odómetro.`,
        });
      } else if (km > 0 && kmRecorridos > 3000) {
        alertas.push({
          tipo: 'warning', icono: 'warning_amber',
          mensaje: `Recorrido inusualmente alto desde la última carga: ${kmRecorridos.toLocaleString('es-CL')} km. Confirme que el odómetro sea correcto.`,
        });
      }

      const rend = this.rendimientoProyectado();
      if (rend !== null) {
        if (rend > 35) {
          alertas.push({ tipo: 'error', icono: 'local_gas_station',
            mensaje: `Rendimiento proyectado extremadamente alto: ${rend.toFixed(1)} L/100km. Revise los datos ingresados.` });
        } else if (rend > 20) {
          alertas.push({ tipo: 'warning', icono: 'trending_down',
            mensaje: `Rendimiento proyectado alto: ${rend.toFixed(1)} L/100km (normal: 5–20 L/100km). Puede indicar un problema mecánico.` });
        } else if (rend < 3 && rend > 0) {
          alertas.push({ tipo: 'warning', icono: 'trending_up',
            mensaje: `Rendimiento proyectado inusualmente bajo: ${rend.toFixed(1)} L/100km. Verifique litros y km.` });
        }
      }

      if (litros > 0 && ultima.litros) {
        const varPct = Math.abs((litros - Number(ultima.litros)) / Number(ultima.litros)) * 100;
        if (varPct > 60) {
          alertas.push({ tipo: 'warning', icono: 'compare_arrows',
            mensaje: `Los litros cargados (${litros} L) difieren en un ${varPct.toFixed(0)}% respecto a la última carga (${ultima.litros} L).` });
        }
      }
    }

    const vehiculoId = this.formulario.value.vehiculoId;
    const vehiculo = this.vehiculos().find(x => x.id === vehiculoId);
    const limiteEstanque = vehiculo?.capacidadEstanque ?? 700;
    if (litros > limiteEstanque) {
      const origen = vehiculo?.capacidadEstanque
        ? `capacidad de estanque del vehículo (${limiteEstanque} L)`
        : `referencia estándar de camión (${limiteEstanque} L)`;
      alertas.push({ tipo: 'warning', icono: 'water_drop',
        mensaje: `${litros} litros supera la ${origen}. Confirme el dato antes de registrar.` });
    }

    if (!ultima && (km > 0 || litros > 0)) {
      alertas.push({ tipo: 'info', icono: 'info',
        mensaje: 'Primera carga registrada para este vehículo. El rendimiento se calculará desde la próxima carga.' });
    }

    this.alertasFormulario.set(alertas);
  }

  // ── Guardar carga + persistir alertas ───────────────────────

  guardar() {
    if (this.formulario.invalid || this.tieneErrores()) return;
    this.guardando.set(true);
    // Snapshot de alertas antes de limpiar el formulario
    this.alertasAlGuardar = this.alertasFormulario().filter(a => a.tipo !== 'error');
    const v = this.formulario.value;

    this.svc.registrarCarga({
      vehiculoId:       v.vehiculoId!,
      numDocumento:     v.numDocumento || undefined,
      kmVehiculo:       v.kmVehiculo!,
      litros:           v.litros!,
      precioLitro:      v.precioLitro!,
      proveedor:        v.proveedor!,
      estacion:         v.estacion || undefined,
      fechaCarga:       v.fechaCarga || undefined,
      tipoCombustible:  this.tipoCombustible(),
    } as any).subscribe({
      next: (carga) => {
        this.guardando.set(false);
        this.cerrarFormulario();
        this.cargar();
        this.svc.getAnomalias().subscribe(a => this.anomalias.set(a));

        // Persistir alertas warning/info asociadas a esta carga
        if (this.alertasAlGuardar.length > 0) {
          const vehiculoId = v.vehiculoId!;
          const dtos = this.alertasAlGuardar.map(a => ({
            cargaId:    carga.id,
            vehiculoId: vehiculoId,
            tipo:       a.tipo,
            icono:      a.icono,
            mensaje:    a.mensaje,
          }));
          this.svc.guardarAlertasCombustible(dtos).subscribe({
            next: () => this.cargarAlertasActivas(),
            error: () => this.cargarAlertasActivas(),
          });
          this.snack.open('Carga registrada con alertas pendientes', '', { duration: 3500 });
        } else {
          this.snack.open('Carga registrada correctamente', '', { duration: 3000 });
        }
      },
      error: () => {
        this.guardando.set(false);
        this.snack.open('Error al registrar la carga', 'Cerrar', { duration: 4000 });
      },
    });
  }

  async eliminar(c: CargaCombustible) {
    const ok = await this.dialogo.confirmarEliminar(
      `¿Eliminar carga del ${new Date(c.fechaCarga).toLocaleDateString('es-CL')}?`,
      `Vehículo: ${this.patenteVehiculo(c.vehiculoId)} · ${c.litros} L · ${c.proveedor}`
    );
    if (!ok) return;
    this.svc.eliminarCarga(c.id).subscribe({ next: () => this.cargar() });
  }

  // ── Alertas: Leído ──────────────────────────────────────────

  marcarLeida(alerta: AlertaCombustible) {
    const accounts = this.msalSvc.instance.getAllAccounts();
    const nombreUsuario = accounts[0]?.name ?? accounts[0]?.username ?? 'Usuario';
    this.procesandoLeida.set(true);
    this.svc.marcarAlertaLeida(alerta.id, nombreUsuario).subscribe({
      next: () => {
        this.procesandoLeida.set(false);
        this.cargarAlertasActivas();
        // Refrescar historial si está visible
        if (this.mostrarHistorial()) this.cargarHistorial();
        this.snack.open(`Leído por ${nombreUsuario}`, '', { duration: 2500 });
      },
      error: () => { this.procesandoLeida.set(false); },
    });
  }

  toggleHistorial() {
    if (!this.mostrarHistorial()) {
      this.cargarHistorial();
    }
    this.mostrarHistorial.update(v => !v);
  }

  cargarHistorial() {
    this.cargandoHistorial.set(true);
    this.svc.getAlertasCombustible(false).subscribe({
      next: a => { this.alertasHistorial.set(a); this.cargandoHistorial.set(false); },
      error: () => this.cargandoHistorial.set(false),
    });
  }

  /** Descarta una anomalía del banner amarillo (persiste en localStorage) */
  descartarAnomalia(carga: CargaCombustible) {
    const nuevo = new Set<string>(this.dismissedIds());
    nuevo.add(carga.id);
    try { localStorage.setItem(this.DISMISSED_KEY, JSON.stringify([...nuevo])); } catch { /* cuota llena */ }
    this.dismissedIds.set(nuevo);
  }

  // ── Helpers ─────────────────────────────────────────────────

  patenteVehiculo(vehiculoId: string): string {
    return this.vehiculos().find(v => v.id === vehiculoId)?.patente ?? vehiculoId.slice(0, 8);
  }

  claseConsumo(l100: number): string {
    if (l100 > 20) return 'insignia insignia-peligro';
    if (l100 > 15) return 'insignia insignia-advertencia';
    return 'insignia insignia-aprobado';
  }

  iconoTipo(tipo: string): string {
    if (tipo === 'error')   return 'error';
    if (tipo === 'warning') return 'warning_amber';
    return 'info';
  }

  seleccionarTipo(tipo: TipoCombustible) {
    this.tipoCombustible.set(tipo);
    this.recalcularAlertas();
  }

  slugTipo(tipo: string | null | undefined): string {
    if (!tipo) return 'diesel';
    return tipo.toLowerCase()
      .replace(/\s+/g, '-')
      .replace(/[éèê]/g, 'e')
      .replace(/[íì]/g, 'i')
      .replace(/[óò]/g, 'o')
      .replace(/[áà]/g, 'a')
      .replace(/[ú]/g, 'u');
  }
}
