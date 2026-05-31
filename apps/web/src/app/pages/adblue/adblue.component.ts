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
import { OperacionesService } from '@core/services/operaciones.service';
import { VehiculosService } from '@core/services/vehiculos.service';
import { CargaAdBlue, Vehiculo } from '@core/models';

@Component({
  selector: 'app-adblue',
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
        <h1>AdBlue</h1>
        <p class="subtitulo-pagina">Registro de recargas de reductor de emisiones (SCR / Euro V–VI)</p>
      </div>
      <button mat-flat-button class="btn-principal"
              (click)="abrirFormulario()"
              [disabled]="vehiculosAdBlue().length === 0">
        <mat-icon>add</mat-icon> Registrar recarga
      </button>
    </div>

    <!-- Banner informativo si no hay vehículos con AdBlue -->
    @if (!cargando() && vehiculosAdBlue().length === 0) {
      <div class="banner-info">
        <mat-icon>info</mat-icon>
        <div>
          <strong>No hay vehículos con AdBlue habilitado</strong>
          <span>Para registrar recargas, active el campo <em>Usa AdBlue</em> en el mantenedor de vehículos (módulo Administración → Vehículos).</span>
        </div>
      </div>
    }

    <!-- Banner anomalías de % diésel -->
    @if (anomalias().length > 0) {
      <div class="banner-anomalias">
        <div class="banner-cabecera">
          <mat-icon>warning_amber</mat-icon>
          <strong>{{ anomalias().length }} anomalía{{ anomalias().length > 1 ? 's' : '' }} de consumo AdBlue detectada{{ anomalias().length > 1 ? 's' : '' }}</strong>
        </div>
        <div class="lista-anomalias">
          @for (a of anomalias(); track a.id) {
            <div class="item-anomalia">
              <mat-icon class="icono-anomalia">opacity</mat-icon>
              <span>
                <strong>{{ patenteVehiculo(a.vehiculoId) }}</strong> —
                {{ a.fechaCarga | date:'dd/MM/yyyy' }} —
                <strong>{{ a.litros | number:'1.1-1' }} L</strong>
                @if (a.pctDiesel != null) {
                  — <span class="pct-anomalia">{{ a.pctDiesel | number:'1.1-1' }}% del diésel</span>
                }
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
          @for (v of vehiculosAdBlue(); track v.id) {
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

    <!-- Tabla -->
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
            <td mat-cell *matCellDef="let c">
              <strong class="chip-adblue">{{ c.litros | number:'1.1-1' }} L</strong>
            </td>
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
            <th mat-header-cell *matHeaderCellDef>Km al recargar</th>
            <td mat-cell *matCellDef="let c">{{ c.kmVehiculo | number:'1.0-0' }} km</td>
          </ng-container>

          <ng-container matColumnDef="pctDiesel">
            <th mat-header-cell *matHeaderCellDef>% Diésel</th>
            <td mat-cell *matCellDef="let c">
              @if (c.pctDiesel != null) {
                <span [class]="clasePct(c.pctDiesel)">{{ c.pctDiesel | number:'1.1-1' }}%</span>
              } @else {
                <span class="texto-atenuado" style="font-size:12px">—</span>
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
            <mat-icon>opacity</mat-icon>
            <p>No hay registros de recarga AdBlue.</p>
          </div>
        }
      </div>
    }

    <!-- ══════════ MODAL: Formulario recarga ══════════ -->
    @if (mostrarFormulario()) {
      <div class="capa-modal" (click)="cerrarFormulario()">
        <div class="panel-modal" style="width:560px;max-height:92vh;overflow-y:auto" (click)="$event.stopPropagation()">
          <h2 style="display:flex;align-items:center;gap:8px">
            <mat-icon style="color:#007AF5">opacity</mat-icon>
            Registrar recarga AdBlue
          </h2>

          <form [formGroup]="formulario" (ngSubmit)="guardar()">

            <!-- Vehículo -->
            <mat-form-field appearance="fill" class="ancho-completo">
              <mat-label>Vehículo *</mat-label>
              <mat-select formControlName="vehiculoId" (selectionChange)="onVehiculoChange($event.value)">
                @for (v of vehiculosAdBlue(); track v.id) {
                  <mat-option [value]="v.id">
                    {{ v.patente }} — {{ v.marca }} {{ v.modelo }}
                    @if (v.normaEuro) { <span class="chip-norma">{{ v.normaEuro.replace('_',' ') }}</span> }
                  </mat-option>
                }
              </mat-select>
            </mat-form-field>

            <!-- Resumen última recarga -->
            @if (ultimaCarga()) {
              <div class="resumen-ultima-carga">
                <mat-icon>history</mat-icon>
                <div>
                  <span class="etiqueta-ultima">Última recarga AdBlue</span>
                  <span>
                    {{ ultimaCarga()!.fechaCarga | date:'dd/MM/yyyy' }} —
                    <strong>{{ ultimaCarga()!.litros | number:'1.1-1' }} L</strong> —
                    {{ ultimaCarga()!.kmVehiculo | number:'1.0-0' }} km
                  </span>
                </div>
              </div>
            }

            <!-- Alerta litros excesivos -->
            @if (alertaLitros()) {
              <div class="alerta-badge alerta-badge-warning">
                <mat-icon>warning_amber</mat-icon>
                <span>{{ alertaLitros() }}</span>
              </div>
            }

            <!-- Km y comprobante -->
            <div class="dos-columnas">
              <mat-form-field appearance="fill">
                <mat-label>Km al recargar *</mat-label>
                <input matInput type="number" formControlName="kmVehiculo" />
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
                <mat-label>Litros recargados *</mat-label>
                <input matInput type="number" step="0.1" formControlName="litros" />
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

            <!-- Proveedor / Estación -->
            <div class="dos-columnas">
              <mat-form-field appearance="fill">
                <mat-label>Proveedor *</mat-label>
                <input matInput formControlName="proveedor" placeholder="Ej: TOTAL, CUMMINS, SHELL" />
              </mat-form-field>
              <mat-form-field appearance="fill">
                <mat-label>Estación / Punto de recarga</mat-label>
                <input matInput formControlName="estacion" placeholder="Nombre o dirección" />
              </mat-form-field>
            </div>

            <!-- Fecha -->
            <mat-form-field appearance="fill" class="ancho-completo">
              <mat-label>Fecha de recarga</mat-label>
              <input matInput type="date" formControlName="fechaCarga" />
            </mat-form-field>

            <div class="acciones-formulario">
              <button mat-button type="button" (click)="cerrarFormulario()">Cancelar</button>
              <button mat-flat-button class="btn-principal" type="submit"
                      [disabled]="formulario.invalid || guardando()">
                {{ guardando() ? 'Registrando…' : 'Registrar recarga' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    }
  `,
  styles: [`
    .subtitulo-pagina { font-size: 13px; color: var(--ink-soft); margin: 2px 0 0; }

    /* Banner sin vehículos */
    .banner-info {
      display: flex; align-items: flex-start; gap: 12px;
      background: #EFF6FF; border: 1px solid #BFDBFE;
      border-radius: var(--radius-md); padding: 14px 18px; margin-bottom: 20px;
      font-size: 13px; color: #1E40AF;
      mat-icon { color: #3B82F6; flex-shrink: 0; margin-top: 2px; }
      div { display: flex; flex-direction: column; gap: 4px; }
      em { font-style: normal; font-weight: 600; }
    }

    /* Banner anomalías */
    .banner-anomalias {
      background: #FFFBEB; border: 1px solid #F59E0B;
      border-radius: var(--radius-md); padding: 14px 16px; margin-bottom: 20px;
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
    .pct-anomalia { font-weight: 700; color: #92400E; }

    /* Filtros */
    .barra-filtros {
      display: flex; flex-wrap: wrap; gap: 12px; padding: 16px 0;
      mat-form-field { min-width: 150px; }
    }

    /* Tabla */
    .celda-patente { font-weight: 700; font-size: 14px; }
    .texto-comprobante { font-family: monospace; font-size: 13px; color: var(--ink-soft); }
    .chip-adblue {
      background: #EFF6FF; color: #1D4ED8; border-radius: 4px;
      padding: 2px 8px; font-size: 13px;
    }
    .estado-vacio-tabla {
      display: flex; flex-direction: column; align-items: center; padding: 48px;
      gap: 8px; color: var(--ink-soft);
      mat-icon { font-size: 48px; width: 48px; height: 48px; opacity: .3; }
    }

    /* % Diésel badge */
    .pct-ok      { color: #166534; font-weight: 700; }
    .pct-bajo    { color: #C25E01; font-weight: 700; }
    .pct-alto    { color: #C10A5A; font-weight: 700; }

    /* Formulario modal */
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

    .alerta-badge {
      display: flex; align-items: center; gap: 10px;
      border-radius: var(--radius-sm); padding: 9px 14px;
      font-size: 13px; font-weight: 500; color: #fff;
      margin-bottom: 10px;
      mat-icon { font-size: 18px; width: 18px; height: 18px; flex-shrink: 0; color: #fff; }
    }
    .alerta-badge-warning { background: #C25E01; }

    .total-estimado {
      display: flex; align-items: center; gap: 8px;
      background: #EFF6FF; border: 1px solid #BFDBFE;
      border-radius: var(--radius-sm); padding: 8px 14px;
      font-size: 13px; color: #1E40AF; margin-bottom: 14px;
      mat-icon { color: #3B82F6; }
    }

    .chip-norma {
      font-size: 10px; font-weight: 700;
      background: #DBEAFE; color: #1E40AF;
      border-radius: 3px; padding: 1px 5px; margin-left: 4px;
    }
  `],
})
export class AdBlueComponent implements OnInit {
  private readonly svc          = inject(OperacionesService);
  private readonly svcVehiculos = inject(VehiculosService);
  private readonly fb           = inject(FormBuilder);
  private readonly snack        = inject(MatSnackBar);

  columnas = ['numDocumento', 'fechaCarga', 'vehiculo', 'litros', 'precioLitro', 'costoTotal', 'kmVehiculo', 'pctDiesel', 'acciones'];

  cargando          = signal(true);
  guardando         = signal(false);
  mostrarFormulario = signal(false);
  cargas            = signal<CargaAdBlue[]>([]);
  anomalias         = signal<CargaAdBlue[]>([]);
  todosVehiculos    = signal<Vehiculo[]>([]);
  ultimaCarga       = signal<CargaAdBlue | null>(null);

  filtroVehiculo = '';
  fechaDesde     = '';
  fechaHasta     = '';

  formulario = this.fb.group({
    vehiculoId:   ['', Validators.required],
    numDocumento: [''],
    kmVehiculo:   [null as number | null, [Validators.required, Validators.min(0)]],
    litros:       [null as number | null, [Validators.required, Validators.min(0.1)]],
    precioLitro:  [null as number | null, [Validators.required, Validators.min(1)]],
    proveedor:    ['', Validators.required],
    estacion:     [''],
    fechaCarga:   [new Date().toISOString().slice(0, 10)],
  });

  // Solo vehículos con AdBlue habilitado
  vehiculosAdBlue = computed(() =>
    this.todosVehiculos().filter(v => v.usaAdBlue)
  );

  totalEstimado = computed(() => {
    const v = this.formulario.value;
    return (Number(v.litros) || 0) * (Number(v.precioLitro) || 0);
  });

  /** Alerta si litros > 200 (un contenedor de AdBlue típico es 1000 L en camión; 200 L es una bandera) */
  alertaLitros = computed((): string | null => {
    const litros = Number(this.formulario.value.litros) || 0;
    if (litros > 200) {
      return `${litros} litros parece inusualmente alto para AdBlue. Verifique el dato antes de registrar.`;
    }
    return null;
  });

  ngOnInit() {
    this.svcVehiculos.getAll({ size: 200 }).subscribe(r => this.todosVehiculos.set(r.content));
    this.svc.getAnomaliasAdBlue().subscribe({ next: a => this.anomalias.set(a), error: () => {} });
    this.cargar();
  }

  cargar() {
    this.cargando.set(true);
    this.svc.getCargasAdBlue({
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
    this.mostrarFormulario.set(true);
  }

  cerrarFormulario() { this.mostrarFormulario.set(false); }

  onVehiculoChange(vehiculoId: string) {
    this.ultimaCarga.set(null);
    if (!vehiculoId) return;
    const v = this.todosVehiculos().find(x => x.id === vehiculoId);
    if (v) this.formulario.patchValue({ kmVehiculo: v.kmActuales });
    this.svc.getUltimaCargaAdBlueVehiculo(vehiculoId).subscribe({
      next: ultima => this.ultimaCarga.set(ultima),
      error: () => {},
    });
  }

  guardar() {
    if (this.formulario.invalid) return;
    this.guardando.set(true);
    const v = this.formulario.value;
    this.svc.registrarCargaAdBlue({
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
        this.svc.getAnomaliasAdBlue().subscribe({ next: a => this.anomalias.set(a), error: () => {} });
        this.snack.open('Recarga AdBlue registrada', '', { duration: 3000 });
      },
      error: () => {
        this.guardando.set(false);
        this.snack.open('Error al registrar la recarga', 'Cerrar', { duration: 4000 });
      },
    });
  }

  eliminar(c: CargaAdBlue) {
    if (!confirm(`¿Eliminar recarga del ${new Date(c.fechaCarga).toLocaleDateString('es-CL')}?`)) return;
    this.svc.eliminarCargaAdBlue(c.id).subscribe({ next: () => this.cargar() });
  }

  patenteVehiculo(vehiculoId: string): string {
    return this.todosVehiculos().find(v => v.id === vehiculoId)?.patente ?? vehiculoId.slice(0, 8);
  }

  clasePct(pct: number): string {
    if (pct > 8 || pct < 2) return 'pct-alto';
    if (pct > 6 || pct < 3) return 'pct-bajo';
    return 'pct-ok';
  }
}
