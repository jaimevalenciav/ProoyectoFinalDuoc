import { Component, OnInit, inject, signal, computed, effect } from '@angular/core';
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
import { OperacionesService } from '@core/services/operaciones.service';
import { VehiculosService } from '@core/services/vehiculos.service';
import { CargaCombustible, Vehiculo } from '@core/models';

interface AlertaFormulario {
  tipo: 'error' | 'warning' | 'info';
  icono: string;
  mensaje: string;
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

    <!-- Banner anomalías globales -->
    @if (anomalias().length > 0) {
      <div class="banner-anomalias">
        <div class="banner-cabecera">
          <mat-icon>warning_amber</mat-icon>
          <strong>{{ anomalias().length }} anomalía{{ anomalias().length > 1 ? 's' : '' }} de consumo detectada{{ anomalias().length > 1 ? 's' : '' }}</strong>
        </div>
        <div class="lista-anomalias">
          @for (a of anomalias(); track a.id) {
            <div class="item-anomalia">
              <mat-icon class="icono-anomalia">directions_bus</mat-icon>
              <span>
                <strong>{{ patenteVehiculo(a.vehiculoId) }}</strong> —
                {{ a.fechaCarga | date:'dd/MM/yyyy' }} —
                <span [class]="claseConsumo(a.consumo100km!)">{{ a.consumo100km | number:'1.1-1' }} L/100km</span>
                ({{ a.litros | number:'1.0-0' }} L · {{ a.kmVehiculo | number:'1.0-0' }} km)
              </span>
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
            <td mat-cell *matCellDef="let c">
              <span class="texto-comprobante">{{ c.numDocumento ?? '—' }}</span>
            </td>
          </ng-container>

          <ng-container matColumnDef="fechaCarga">
            <th mat-header-cell *matHeaderCellDef>Fecha</th>
            <td mat-cell *matCellDef="let c">{{ c.fechaCarga | date:'dd/MM/yyyy' }}</td>
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
                <span [class]="claseConsumo(c.consumo100km)">
                  {{ c.consumo100km | number:'1.1-1' }} L/100km
                </span>
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

    <!-- Modal formulario -->
    @if (mostrarFormulario()) {
      <div class="capa-modal" (click)="cerrarFormulario()">
        <div class="panel-modal" style="width:560px;max-height:92vh;overflow-y:auto" (click)="$event.stopPropagation()">
          <h2>Registrar carga de combustible</h2>

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

            <!-- Alertas en tiempo real -->
            @if (alertasFormulario().length > 0) {
              <div class="bloque-alertas">
                @for (a of alertasFormulario(); track a.mensaje) {
                  <div [class]="'alerta-linea alerta-' + a.tipo">
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
                <mat-icon matSuffix style="font-size:18px;color:var(--color-texto-3)">speed</mat-icon>
              </mat-form-field>
              <mat-form-field appearance="fill">
                <mat-label>Nº comprobante</mat-label>
                <input matInput formControlName="numDocumento" placeholder="Ej: 000123456" />
                <mat-icon matSuffix style="font-size:18px;color:var(--color-texto-3)">receipt</mat-icon>
              </mat-form-field>
            </div>

            <!-- Litros y precio -->
            <div class="dos-columnas">
              <mat-form-field appearance="fill">
                <mat-label>Litros cargados *</mat-label>
                <input matInput type="number" step="0.1" formControlName="litros" (input)="recalcularAlertas()" />
                <span matSuffix>L</span>
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
              <div class="total-estimado" [style.background]="rendimientoProyectado()! > 20 ? '#FEF2F2' : rendimientoProyectado()! > 15 ? '#FFFBEB' : '#F0FDF4'">
                <mat-icon [style.color]="rendimientoProyectado()! > 20 ? '#DC2626' : rendimientoProyectado()! > 15 ? '#D97706' : '#16A34A'">
                  {{ rendimientoProyectado()! > 20 ? 'trending_down' : rendimientoProyectado()! > 15 ? 'trending_flat' : 'trending_up' }}
                </mat-icon>
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
    .subtitulo-pagina { font-size: 13px; color: var(--color-texto-3); margin: 2px 0 0; }

    /* Banner anomalías */
    .banner-anomalias {
      background: #FFFBEB; border: 1px solid #F59E0B;
      border-radius: var(--radio-md); padding: 14px 16px;
      margin-bottom: 20px;
    }
    .banner-cabecera {
      display: flex; align-items: center; gap: 8px;
      color: #92400E; font-size: 14px; margin-bottom: 10px;
      mat-icon { color: #D97706; }
    }
    .lista-anomalias { display: flex; flex-direction: column; gap: 6px; }
    .item-anomalia {
      display: flex; align-items: center; gap: 8px;
      font-size: 13px; color: #78350F;
      padding: 4px 0 4px 4px;
      border-top: 1px solid rgba(245,158,11,.2);
    }
    .icono-anomalia { font-size: 16px; width: 16px; height: 16px; color: #D97706; flex-shrink: 0; }

    /* Tabla */
    .celda-patente { font-weight: 700; color: var(--azul-800); font-size: 14px; }
    .texto-comprobante { font-family: monospace; font-size: 13px; color: var(--color-texto-2); }
    .estado-vacio-tabla {
      display: flex; flex-direction: column; align-items: center; padding: 48px;
      gap: 8px; color: var(--color-texto-3);
      mat-icon { font-size: 48px; width: 48px; height: 48px; opacity: .3; }
    }

    /* Filtros */
    .barra-filtros {
      display: flex; flex-wrap: wrap; gap: 12px; padding: 16px 0;
      mat-form-field { min-width: 150px; }
    }

    /* Formulario — resumen última carga */
    .resumen-ultima-carga {
      display: flex; align-items: flex-start; gap: 10px;
      background: var(--azul-50); border: 1px solid var(--azul-200);
      border-radius: var(--radio-sm); padding: 10px 14px;
      margin-bottom: 14px; font-size: 13px; color: var(--azul-700);
      mat-icon { color: var(--azul-500); flex-shrink: 0; margin-top: 2px; }
      div { display: flex; flex-direction: column; gap: 2px; }
      .etiqueta-ultima { font-size: 10px; font-weight: 700; text-transform: uppercase;
        letter-spacing: .6px; color: var(--azul-400); }
    }

    /* Alertas en tiempo real */
    .bloque-alertas { display: flex; flex-direction: column; gap: 6px; margin-bottom: 14px; }
    .alerta-linea {
      display: flex; align-items: center; gap: 8px;
      border-radius: var(--radio-sm); padding: 8px 12px;
      font-size: 13px; font-weight: 500;
      mat-icon { font-size: 18px; width: 18px; height: 18px; flex-shrink: 0; }
    }
    .alerta-error   { background: #FEF2F2; color: #991B1B; border: 1px solid #FECACA; mat-icon { color: #DC2626; } }
    .alerta-warning { background: #FFFBEB; color: #92400E; border: 1px solid #FDE68A; mat-icon { color: #D97706; } }
    .alerta-info    { background: var(--azul-50); color: var(--azul-700); border: 1px solid var(--azul-200); mat-icon { color: var(--azul-500); } }

    /* Total estimado / rendimiento */
    .total-estimado {
      display: flex; align-items: center; gap: 8px;
      background: #F0FDF4; border: 1px solid #BBF7D0;
      border-radius: var(--radio-sm); padding: 8px 14px;
      font-size: 13px; color: #166534; margin-bottom: 14px;
      mat-icon { color: #16A34A; }
    }
  `],
})
export class CombustibleComponent implements OnInit {
  private readonly svc          = inject(OperacionesService);
  private readonly svcVehiculos = inject(VehiculosService);
  private readonly fb           = inject(FormBuilder);
  private readonly snack        = inject(MatSnackBar);

  columnas = ['numDocumento', 'fechaCarga', 'vehiculo', 'litros', 'precioLitro', 'costoTotal', 'kmVehiculo', 'rendimiento', 'acciones'];

  cargando          = signal(true);
  guardando         = signal(false);
  mostrarFormulario = signal(false);
  cargas            = signal<CargaCombustible[]>([]);
  anomalias         = signal<CargaCombustible[]>([]);
  vehiculos         = signal<Vehiculo[]>([]);
  ultimaCarga       = signal<CargaCombustible | null>(null);
  alertasFormulario = signal<AlertaFormulario[]>([]);

  filtroVehiculo = '';
  fechaDesde     = '';
  fechaHasta     = '';

  formulario = this.fb.group({
    vehiculoId:  ['', Validators.required],
    numDocumento:[''],
    kmVehiculo:  [null as number | null, [Validators.required, Validators.min(0)]],
    litros:      [null as number | null, [Validators.required, Validators.min(1)]],
    precioLitro: [null as number | null, [Validators.required, Validators.min(1)]],
    proveedor:   ['', Validators.required],
    estacion:    [''],
    fechaCarga:  [new Date().toISOString().slice(0, 10)],
  });

  // ── Computados ─────────────────────────────────────────────

  totalEstimado = computed(() => {
    const v = this.formulario.value;
    const l = Number(v.litros) || 0;
    const p = Number(v.precioLitro) || 0;
    return l * p;
  });

  rendimientoProyectado = computed((): number | null => {
    const ultima = this.ultimaCarga();
    if (!ultima) return null;
    const v = this.formulario.value;
    const km = Number(v.kmVehiculo) || 0;
    const litros = Number(v.litros) || 0;
    const kmRecorridos = km - (ultima.kmVehiculo ?? 0);
    if (kmRecorridos <= 0 || litros <= 0) return null;
    return (litros * 100) / kmRecorridos;
  });

  tieneErrores = computed(() =>
    this.alertasFormulario().some(a => a.tipo === 'error')
  );

  ngOnInit() {
    this.svcVehiculos.getAll({ size: 200 }).subscribe(r => this.vehiculos.set(r.content));
    this.svc.getAnomalias().subscribe(a => this.anomalias.set(a));
    this.cargar();
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

  abrirFormulario() {
    const hoy = new Date().toISOString().slice(0, 10);
    this.formulario.reset({ fechaCarga: hoy });
    this.ultimaCarga.set(null);
    this.alertasFormulario.set([]);
    this.mostrarFormulario.set(true);
  }

  cerrarFormulario() { this.mostrarFormulario.set(false); }

  // ── Cuando cambia el vehículo seleccionado ─────────────────

  onVehiculoChange(vehiculoId: string) {
    this.ultimaCarga.set(null);
    this.alertasFormulario.set([]);
    if (!vehiculoId) return;

    // Pre-rellenar km con los km actuales del vehículo
    const v = this.vehiculos().find(x => x.id === vehiculoId);
    if (v) this.formulario.patchValue({ kmVehiculo: v.kmActuales });

    // Cargar última carga del vehículo
    this.svc.getUltimaCargaVehiculo(vehiculoId).subscribe({
      next: ultima => {
        this.ultimaCarga.set(ultima);
        this.recalcularAlertas();
      },
    });
  }

  // ── Validaciones en tiempo real ────────────────────────────

  recalcularAlertas() {
    const alertas: AlertaFormulario[] = [];
    const ultima = this.ultimaCarga();
    const v = this.formulario.value;
    const km = Number(v.kmVehiculo) || 0;
    const litros = Number(v.litros) || 0;

    if (ultima) {
      const kmAnterior = ultima.kmVehiculo ?? 0;
      const kmRecorridos = km - kmAnterior;

      // ── KM menor o igual al anterior ──────────────────────
      if (km > 0 && km <= kmAnterior) {
        alertas.push({
          tipo: 'error',
          icono: 'error',
          mensaje: `El kilometraje ingresado (${km.toLocaleString('es-CL')} km) es menor o igual a la última carga registrada (${kmAnterior.toLocaleString('es-CL')} km). Verifique el odómetro.`,
        });
      }

      // ── KM retroceso muy grande ────────────────────────────
      else if (km > 0 && kmRecorridos > 3000) {
        alertas.push({
          tipo: 'warning',
          icono: 'warning_amber',
          mensaje: `Recorrido inusualmente alto desde la última carga: ${kmRecorridos.toLocaleString('es-CL')} km. Confirme que el odómetro sea correcto.`,
        });
      }

      // ── Rendimiento proyectado anómalo ────────────────────
      const rend = this.rendimientoProyectado();
      if (rend !== null) {
        if (rend > 35) {
          alertas.push({
            tipo: 'error',
            icono: 'local_gas_station',
            mensaje: `Rendimiento proyectado extremadamente alto: ${rend.toFixed(1)} L/100km. Revise los datos ingresados.`,
          });
        } else if (rend > 20) {
          alertas.push({
            tipo: 'warning',
            icono: 'trending_down',
            mensaje: `Rendimiento proyectado alto: ${rend.toFixed(1)} L/100km (normal: 5–20 L/100km). Puede indicar un problema mecánico.`,
          });
        } else if (rend < 3 && rend > 0) {
          alertas.push({
            tipo: 'warning',
            icono: 'trending_up',
            mensaje: `Rendimiento proyectado inusualmente bajo: ${rend.toFixed(1)} L/100km. Verifique litros y km.`,
          });
        }
      }

      // ── Variación grande respecto a última carga ──────────
      if (litros > 0 && ultima.litros) {
        const varPct = Math.abs((litros - Number(ultima.litros)) / Number(ultima.litros)) * 100;
        if (varPct > 60) {
          alertas.push({
            tipo: 'warning',
            icono: 'compare_arrows',
            mensaje: `Los litros cargados (${litros} L) difieren en un ${varPct.toFixed(0)}% respecto a la última carga (${ultima.litros} L).`,
          });
        }
      }
    }

    // ── Litros excesivos (supera capacidad del estanque) ──────
    const vehiculoId = this.formulario.value.vehiculoId;
    const vehiculo   = this.vehiculos().find(x => x.id === vehiculoId);
    const limiteEstanque = vehiculo?.capacidadEstanque ?? 700;
    if (litros > limiteEstanque) {
      const origen = vehiculo?.capacidadEstanque
        ? `capacidad de estanque del vehículo (${limiteEstanque} L)`
        : `referencia estándar de camión (${limiteEstanque} L)`;
      alertas.push({
        tipo: 'warning',
        icono: 'water_drop',
        mensaje: `${litros} litros supera la ${origen}. Confirme el dato antes de registrar.`,
      });
    }

    // ── Sin carga anterior: aviso informativo ─────────────────
    if (!ultima && (km > 0 || litros > 0)) {
      alertas.push({
        tipo: 'info',
        icono: 'info',
        mensaje: 'Primera carga registrada para este vehículo. El rendimiento se calculará desde la próxima carga.',
      });
    }

    this.alertasFormulario.set(alertas);
  }

  // ── Guardar ────────────────────────────────────────────────

  guardar() {
    if (this.formulario.invalid || this.tieneErrores()) return;
    this.guardando.set(true);
    const v = this.formulario.value;
    this.svc.registrarCarga({
      vehiculoId:   v.vehiculoId!,
      numDocumento: v.numDocumento || undefined,
      kmVehiculo:   v.kmVehiculo!,
      litros:       v.litros!,
      precioLitro:  v.precioLitro!,
      proveedor:    v.proveedor!,
      estacion:     v.estacion || undefined,
      fechaCarga:   v.fechaCarga || undefined,
    } as any).subscribe({
      next: () => {
        this.guardando.set(false);
        this.cerrarFormulario();
        this.cargar();
        this.svc.getAnomalias().subscribe(a => this.anomalias.set(a));
        this.snack.open('Carga registrada correctamente', '', { duration: 3000 });
      },
      error: () => {
        this.guardando.set(false);
        this.snack.open('Error al registrar la carga', 'Cerrar', { duration: 4000 });
      },
    });
  }

  eliminar(c: CargaCombustible) {
    if (!confirm(`¿Eliminar carga del ${new Date(c.fechaCarga).toLocaleDateString('es-CL')}?`)) return;
    this.svc.eliminarCarga(c.id).subscribe({ next: () => this.cargar() });
  }

  // ── Helpers ────────────────────────────────────────────────

  patenteVehiculo(vehiculoId: string): string {
    return this.vehiculos().find(v => v.id === vehiculoId)?.patente ?? vehiculoId.slice(0, 8);
  }

  claseConsumo(l100: number): string {
    if (l100 > 20) return 'insignia insignia-peligro';
    if (l100 > 15) return 'insignia insignia-advertencia';
    return 'insignia insignia-aprobado';
  }
}
