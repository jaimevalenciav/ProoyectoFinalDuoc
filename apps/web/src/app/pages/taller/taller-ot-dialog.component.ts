import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  MatDialogModule, MatDialogRef, MAT_DIALOG_DATA,
} from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TallerService } from '@core/services/taller.service';
import { AlmacenService, AjusteStockDto } from '@core/services/almacen.service';
import { OrdenTrabajo, TareaOT, Vehiculo, TareaDefinicion } from '@core/models';

interface MaterialOt {
  repuestoId: string;
  repuestoCodigo?: string;
  repuestoDescripcion?: string;
  repuestoUnidad?: string;
  cantidad: number;
}

export interface TallerOtDialogData {
  ot: OrdenTrabajo;
  vehiculos: Vehiculo[];
  tareasDefinicion: TareaDefinicion[];
}

@Component({
  selector: 'app-taller-ot-dialog',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatDialogModule, MatButtonModule, MatIconModule,
    MatCheckboxModule, MatProgressBarModule, MatProgressSpinnerModule,
    MatSelectModule, MatFormFieldModule, MatSnackBarModule, MatTooltipModule,
  ],
  template: `
    <!-- ── Header ─────────────────────────────────────────────── -->
    <div class="dlg-header">
      <div class="dlg-header-left">
        <span class="ot-num">{{ ot().numero }}</span>
        <span class="ot-veh">{{ vehiculoLabel() }}</span>
        <span class="badge {{ badgeTipo(ot().tipo) }}">{{ labelTipo(ot().tipo) }}</span>
        <span class="badge {{ badgeEstado(ot().estado) }}">{{ labelEstado(ot().estado) }}</span>
      </div>
      <button mat-icon-button (click)="cerrar()" class="btn-x"><mat-icon>close</mat-icon></button>
    </div>

    <!-- ── Body ───────────────────────────────────────────────── -->
    <div class="dlg-body">

      <!-- Progreso -->
      <div class="bloque-prog">
        <div class="prog-row">
          <span class="prog-lbl">Avance del trabajo</span>
          <span class="prog-pct">{{ ot().avance }}%</span>
        </div>
        <mat-progress-bar mode="determinate" [value]="ot().avance"
          [class]="colorAvance(ot().avance)" class="barra-avance" />
        <div class="prog-stats">
          <span>{{ tareasCompletadas() }} de {{ ot().tareas?.length ?? 0 }} tareas completadas</span>
          <span class="texto-desc">{{ ot().descripcion }}</span>
        </div>
      </div>

      <!-- Grid: tareas | detalle -->
      <div class="dlg-grid">

        <!-- Columna tareas -->
        <section class="bloque-tareas">
          <h3 class="bloque-titulo"><mat-icon class="titulo-ico">checklist</mat-icon> Tareas</h3>

          @if (!ot().tareas?.length) {
            <p class="txt-vacio">Sin tareas registradas.</p>
          }
          @for (tarea of ot().tareas; track tarea.id) {
            <div class="fila-tarea" [class.done]="tarea.completada === 1"
                 [class.saving]="guardandoTarea() === tarea.id">
              @if (guardandoTarea() === tarea.id) {
                <mat-spinner diameter="18" class="spin-tarea" />
              } @else {
                <mat-checkbox
                  [checked]="tarea.completada === 1"
                  [disabled]="ot().estado === 'CERRADA'"
                  (change)="toggleTarea(tarea)"
                  color="primary">
                </mat-checkbox>
              }
              <span class="tarea-txt">{{ tarea.descripcion }}</span>
              @if (ot().estado !== 'CERRADA' && guardandoTarea() !== tarea.id) {
                <button mat-icon-button class="btn-del-tarea" (click)="eliminarTarea(tarea)" matTooltip="Eliminar">
                  <mat-icon>delete_outline</mat-icon>
                </button>
              }
            </div>
          }

          @if (ot().estado !== 'CERRADA') {
            <div class="nueva-tarea-row">
              <input class="input-tarea" [(ngModel)]="nuevaTarea"
                     (keyup.enter)="agregarTarea()"
                     placeholder="Nueva tarea…" />
              <button class="btn-principal" (click)="agregarTarea()" [disabled]="!nuevaTarea.trim()">
                <mat-icon>add</mat-icon>
              </button>
            </div>
          }
        </section>

        <!-- Columna detalle -->
        <section class="bloque-detalle">

          <!-- Info -->
          <div class="bloque-info">
            <h3 class="bloque-titulo"><mat-icon class="titulo-ico">info_outline</mat-icon> Información</h3>
            <div class="info-grid">
              <div class="info-item">
                <span class="info-lbl">Vehículo</span>
                <span class="info-val">{{ ot().vehiculoPatente || '—' }}</span>
              </div>
              <div class="info-item">
                <span class="info-lbl">Mecánico</span>
                <span class="info-val">{{ ot().mecanicoResponsable || '—' }}</span>
              </div>
              <div class="info-item">
                <span class="info-lbl">Apertura</span>
                <span class="info-val">{{ ot().fechaApertura | date:'dd/MM/yyyy' }}</span>
              </div>
              <div class="info-item">
                <span class="info-lbl">Cierre est.</span>
                <span class="info-val">{{ (ot().fechaCierreEst | date:'dd/MM/yyyy') || '—' }}</span>
              </div>
              <div class="info-item">
                <span class="info-lbl">Costo MO</span>
                <span class="info-val">{{ ot().costoManoObra | currency:'CLP':'$':'1.0-0' }}</span>
              </div>
              <div class="info-item">
                <span class="info-lbl">Costo repuestos</span>
                <span class="info-val">{{ ot().costoRepuestos | currency:'CLP':'$':'1.0-0' }}</span>
              </div>
              <div class="info-item info-total-row">
                <span class="info-lbl">Total</span>
                <span class="info-total">{{ ot().costoTotal | currency:'CLP':'$':'1.0-0' }}</span>
              </div>
            </div>
          </div>

          <!-- Materiales -->
          @if (tareasDefinicionData.length > 0) {
            <div class="bloque-mat">
              <h3 class="bloque-titulo"><mat-icon class="titulo-ico">inventory_2</mat-icon> Materiales almacén</h3>
              @if (ot().estado !== 'CERRADA') {
                <div class="mat-selector-row">
                  <mat-form-field appearance="outline" class="mat-campo">
                    <mat-label>Tarea del catálogo</mat-label>
                    <mat-select [(ngModel)]="tareaDefEntrega">
                      @for (td of tareasDefinicionData; track td.id) {
                        <mat-option [value]="td">{{ td.nombre }}</mat-option>
                      }
                    </mat-select>
                  </mat-form-field>
                  <button class="btn-secundario" (click)="agregarMateriales()" [disabled]="!tareaDefEntrega">
                    <mat-icon>add</mat-icon>
                  </button>
                </div>
              }
              @if (materialesOt.length > 0) {
                <div class="lista-mat">
                  @for (mat of materialesOt; track mat.repuestoId) {
                    <div class="mat-row">
                      <div class="mat-info">
                        <span class="mat-cod">{{ mat.repuestoCodigo || '—' }}</span>
                        <span class="mat-desc-txt">{{ mat.repuestoDescripcion || mat.repuestoId }}</span>
                      </div>
                      <span class="mat-cant">{{ mat.cantidad }} {{ mat.repuestoUnidad || 'UN' }}</span>
                      @if (ot().estado !== 'CERRADA') {
                        <button mat-icon-button class="btn-rm-mat" (click)="quitarMaterial(mat.repuestoId)">
                          <mat-icon>close</mat-icon>
                        </button>
                      }
                    </div>
                  }
                </div>
                @if (ot().estado !== 'CERRADA') {
                  <button class="btn-entregar" (click)="entregarMateriales()" [disabled]="entregando()">
                    <mat-icon>output</mat-icon>
                    {{ entregando() ? 'Procesando…' : 'Entregar al taller' }}
                  </button>
                }
              } @else if (ot().estado !== 'CERRADA') {
                <p class="txt-vacio">Selecciona del catálogo para ver los materiales requeridos.</p>
              }
            </div>
          }

        </section>
      </div>
    </div>

    <!-- ── Footer ─────────────────────────────────────────────── -->
    <div class="dlg-footer">
      <button class="btn-secundario" (click)="cerrar()">Cerrar</button>
      @if (ot().estado !== 'CERRADA') {
        <button class="btn-cerrar-ot" (click)="cerrarOT()" [disabled]="cerrando()">
          <mat-icon>lock</mat-icon>
          {{ cerrando() ? 'Cerrando…' : 'Cerrar orden de trabajo' }}
        </button>
      }
    </div>
  `,
  styles: [`
    :host { display: flex; flex-direction: column; height: 100%; }

    /* Header */
    .dlg-header {
      display: flex; align-items: center; justify-content: space-between;
      padding: 16px 20px 12px;
      border-bottom: 1.5px solid #e4eaf3;
      background: #fff;
      flex-shrink: 0;
    }
    .dlg-header-left { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
    .ot-num { font-size: 18px; font-weight: 700; color: #1a2540; font-family: monospace; }
    .ot-veh { font-size: 13px; color: #4a5878; }
    .btn-x { color: #8492b0 !important; }

    /* Body */
    .dlg-body { flex: 1; overflow-y: auto; padding: 20px; display: flex; flex-direction: column; gap: 20px; }

    /* Progreso */
    .bloque-prog {
      background: #f0f4f9; border-radius: 10px; padding: 16px;
      border: 1px solid #e4eaf3;
    }
    .prog-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
    .prog-lbl { font-size: 13px; font-weight: 600; color: #1a2540; }
    .prog-pct { font-size: 24px; font-weight: 700; color: #007AF5; }
    .barra-avance { height: 10px !important; border-radius: 5px !important; }
    .prog-stats { display: flex; justify-content: space-between; align-items: center; margin-top: 8px; font-size: 12px; color: #4a5878; }
    .texto-desc { font-style: italic; }
    ::ng-deep .avance-bajo .mdc-linear-progress__bar-inner  { border-color: #C10A5A !important; }
    ::ng-deep .avance-medio .mdc-linear-progress__bar-inner { border-color: #C25E01 !important; }
    ::ng-deep .avance-alto .mdc-linear-progress__bar-inner  { border-color: #007AF5 !important; }

    /* Grid 2 columnas */
    .dlg-grid {
      display: grid;
      grid-template-columns: 1fr 340px;
      gap: 20px;
      align-items: start;
    }

    /* Bloques */
    .bloque-tareas, .bloque-detalle { display: flex; flex-direction: column; gap: 12px; }
    .bloque-info, .bloque-mat {
      background: #fff; border: 1px solid #e4eaf3; border-radius: 10px; padding: 16px;
    }
    .bloque-titulo {
      display: flex; align-items: center; gap: 6px;
      font-size: 13px; font-weight: 700; color: #1a2540; margin-bottom: 12px;
    }
    .titulo-ico { font-size: 16px; width: 16px; height: 16px; color: #007AF5; }
    .txt-vacio { font-size: 12px; color: #8492b0; padding: 4px 0; }

    /* Tareas */
    .fila-tarea {
      display: flex; align-items: center; gap: 8px;
      padding: 8px 10px; border-radius: 7px;
      background: #f0f4f9; transition: background .15s;
      position: relative;
    }
    .fila-tarea:hover { background: #e4eaf3; }
    .fila-tarea.done { opacity: .6; }
    .fila-tarea.done .tarea-txt { text-decoration: line-through; }
    .fila-tarea.saving { opacity: .75; pointer-events: none; }
    .tarea-txt { flex: 1; font-size: 13px; color: #1a2540; }
    .spin-tarea { flex-shrink: 0; width: 18px; height: 18px; }
    .btn-del-tarea { opacity: 0; transition: opacity .15s; color: #8492b0 !important; width: 28px !important; height: 28px !important; }
    .fila-tarea:hover .btn-del-tarea { opacity: 1; }
    .nueva-tarea-row { display: flex; align-items: center; gap: 8px; margin-top: 4px; }
    .input-tarea {
      flex: 1; height: 36px; padding: 0 10px;
      border: 1.5px solid #ccd5e6; border-radius: 7px;
      font-size: 13px; font-family: inherit; outline: none;
      transition: border-color .15s;
      &:focus { border-color: #007AF5; }
    }

    /* Info */
    .info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
    .info-item { display: flex; flex-direction: column; gap: 2px; padding: 8px 10px; background: #f0f4f9; border-radius: 7px; }
    .info-lbl { font-size: 10px; font-weight: 700; color: #8492b0; text-transform: uppercase; letter-spacing: .05em; }
    .info-val { font-size: 13px; font-weight: 500; color: #1a2540; }
    .info-total-row { grid-column: 1/-1; }
    .info-total { font-size: 16px; font-weight: 700; color: #007AF5; }

    /* Materiales */
    .mat-selector-row { display: flex; align-items: flex-start; gap: 8px; margin-bottom: 8px; }
    .mat-campo { flex: 1; }
    ::ng-deep .mat-campo .mat-mdc-form-field-infix { min-height: 40px; padding-top: 8px; padding-bottom: 8px; }
    .lista-mat { border: 1px solid #e4eaf3; border-radius: 8px; overflow: hidden; margin-bottom: 10px; }
    .mat-row {
      display: flex; align-items: center; gap: 8px;
      padding: 8px 10px; border-bottom: 1px solid #f0f4f9;
      &:last-child { border-bottom: none; }
    }
    .mat-info { display: flex; flex-direction: column; flex: 1; min-width: 0; }
    .mat-cod { font-family: monospace; font-size: 10px; color: #8492b0; }
    .mat-desc-txt { font-size: 12px; color: #1a2540; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .mat-cant { font-weight: 700; font-size: 12px; color: #1B2C40; white-space: nowrap; flex-shrink: 0; }
    .btn-rm-mat { color: #8492b0 !important; width: 24px !important; height: 24px !important; flex-shrink: 0; }
    .btn-entregar {
      display: flex; align-items: center; gap: 6px;
      width: 100%; height: 34px; padding: 0 14px;
      background: #1B2C40; color: #FEFF04;
      border: none; border-radius: 7px;
      font-size: 12px; font-weight: 700; font-family: inherit;
      cursor: pointer; transition: opacity .15s;
      &:hover { opacity: .85; }
      &:disabled { opacity: .5; cursor: default; }
      mat-icon { font-size: 16px; width: 16px; height: 16px; }
    }

    /* Footer */
    .dlg-footer {
      display: flex; align-items: center; justify-content: flex-end; gap: 10px;
      padding: 12px 20px;
      border-top: 1.5px solid #e4eaf3;
      background: #f0f4f9;
      flex-shrink: 0;
    }
    .btn-cerrar-ot {
      display: flex; align-items: center; gap: 6px;
      height: 34px; padding: 0 16px;
      background: #007AF5; color: #fff;
      border: none; border-radius: 7px;
      font-size: 12px; font-weight: 700; font-family: inherit;
      cursor: pointer; transition: opacity .15s;
      &:hover { opacity: .85; }
      &:disabled { opacity: .5; cursor: default; }
      mat-icon { font-size: 16px; width: 16px; height: 16px; }
    }
  `],
})
export class TallerOtDialogComponent {
  private readonly ref    = inject(MatDialogRef<TallerOtDialogComponent>);
  private readonly data   = inject<TallerOtDialogData>(MAT_DIALOG_DATA);
  private readonly svc    = inject(TallerService);
  private readonly almSvc = inject(AlmacenService);
  private readonly snack  = inject(MatSnackBar);

  ot               = signal<OrdenTrabajo>(this.data.ot);
  vehiculosData    = this.data.vehiculos;
  tareasDefinicionData = this.data.tareasDefinicion;

  guardandoTarea   = signal<string | null>(null);
  cerrando         = signal(false);
  entregando       = signal(false);
  nuevaTarea       = '';
  tareaDefEntrega: TareaDefinicion | null = null;
  materialesOt: MaterialOt[] = [];

  tareasCompletadas() { return this.ot().tareas?.filter(t => t.completada === 1).length ?? 0; }

  vehiculoLabel(): string {
    const v = this.vehiculosData.find(x => x.id === this.ot().vehiculoId);
    return v ? `${v.patente} — ${v.marca}` : (this.ot().vehiculoPatente || this.ot().vehiculoId);
  }

  cerrar() { this.ref.close(this.ot()); }

  toggleTarea(tarea: TareaOT) {
    const otId    = this.ot().id;
    const tareaId = tarea.id;
    const otPrev  = this.ot();

    // ── Actualización optimista ───────────────────────────────────────────────
    // Cambiamos el signal ANTES de la llamada HTTP para que la UI responda
    // inmediatamente sin depender de la respuesta del servidor.
    const nuevaCompletada = tarea.completada === 1 ? 0 : 1;
    const tareasOpt = otPrev.tareas.map(t =>
      t.id === tareaId ? { ...t, completada: nuevaCompletada } : t
    );
    const total      = tareasOpt.length;
    const completadas = tareasOpt.filter(t => t.completada === 1).length;
    const nuevoAvance = total > 0 ? Math.round((completadas / total) * 100) : 0;
    this.ot.set({ ...otPrev, tareas: tareasOpt, avance: nuevoAvance });

    // ── Persistencia al servidor ──────────────────────────────────────────────
    this.guardandoTarea.set(tareaId);
    this.svc.completarTarea(otId, tareaId).subscribe({
      next: actualizada => {
        // Confirmar con la respuesta real del servidor
        this.ot.set(actualizada);
        this.guardandoTarea.set(null);
      },
      error: () => {
        // Revertir la actualización optimista si el servidor falla
        this.ot.set(otPrev);
        this.guardandoTarea.set(null);
        this.snack.open('Error al guardar — se revirtió el cambio', '', { duration: 3000 });
      },
    });
  }

  agregarTarea() {
    const desc = this.nuevaTarea.trim();
    if (!desc) return;
    this.svc.agregarTarea(this.ot().id, desc).subscribe(() => {
      this.nuevaTarea = '';
      this.svc.getById(this.ot().id).subscribe(actualizada => this.ot.set(actualizada));
    });
  }

  eliminarTarea(tarea: TareaOT) {
    this.svc.eliminarTarea(this.ot().id, tarea.id).subscribe(() => {
      this.svc.getById(this.ot().id).subscribe(actualizada => this.ot.set(actualizada));
    });
  }

  cerrarOT() {
    this.cerrando.set(true);
    this.svc.cerrar(this.ot().id, { costoManoObra: this.ot().costoManoObra }).subscribe({
      next: actualizada => {
        this.ot.set(actualizada);
        this.cerrando.set(false);
        this.snack.open('Orden de trabajo cerrada', '', { duration: 2500 });
      },
      error: () => this.cerrando.set(false),
    });
  }

  agregarMateriales() {
    const td = this.tareaDefEntrega;
    if (!td) return;
    for (const art of td.articulos ?? []) {
      const existing = this.materialesOt.find(m => m.repuestoId === art.repuestoId);
      if (existing) { existing.cantidad += art.cantidad; }
      else {
        this.materialesOt.push({
          repuestoId:          art.repuestoId,
          repuestoCodigo:      art.repuestoCodigo,
          repuestoDescripcion: art.repuestoNombre,
          repuestoUnidad:      art.repuestoUnidad,
          cantidad:            art.cantidad,
        });
      }
    }
    this.tareaDefEntrega = null;
  }

  quitarMaterial(repuestoId: string) {
    this.materialesOt = this.materialesOt.filter(m => m.repuestoId !== repuestoId);
  }

  entregarMateriales() {
    if (!this.materialesOt.length) return;
    this.entregando.set(true);
    let pendientes = this.materialesOt.length;
    let errores = 0;

    for (const mat of this.materialesOt) {
      const dto: AjusteStockDto = { tipo: 'SALIDA', cantidad: mat.cantidad, otId: this.ot().id };
      this.almSvc.ajustarStock(mat.repuestoId, dto).subscribe({
        next:  () => { if (--pendientes === 0) this.finalizarEntrega(errores); },
        error: () => { errores++; if (--pendientes === 0) this.finalizarEntrega(errores); },
      });
    }
  }

  private finalizarEntrega(errores: number) {
    this.entregando.set(false);
    if (errores === 0) {
      this.materialesOt = [];
      this.snack.open('Materiales entregados — stock descontado', '', { duration: 3000 });
    } else {
      this.snack.open(`${errores} error(es) al descontar stock. Verifique disponibilidad.`, '', { duration: 5000 });
    }
  }

  badgeTipo(tipo: string): string {
    const m: Record<string, string> = {
      PREVENTIVA: 'badge-preventiva', CORRECTIVA: 'badge-correctiva',
      NEUMATICOS: 'badge-neumaticos', ELECTRICA: 'badge-electrica',
    };
    return m[tipo] ?? '';
  }
  badgeEstado(estado: string): string {
    const m: Record<string, string> = {
      PENDIENTE: 'badge-pendiente', EN_EJECUCION: 'badge-ejecucion',
      BLOQUEADA: 'badge-bloqueada', CERRADA: 'badge-cerrada',
    };
    return m[estado] ?? '';
  }
  labelTipo(tipo: string): string {
    const m: Record<string, string> = { PREVENTIVA:'Preventivo', CORRECTIVA:'Correctivo', NEUMATICOS:'Neumáticos', ELECTRICA:'Eléctrica' };
    return m[tipo] ?? tipo;
  }
  labelEstado(estado: string): string {
    const m: Record<string, string> = { PENDIENTE:'Pendiente', EN_EJECUCION:'En ejecución', BLOQUEADA:'Bloqueada', CERRADA:'Cerrada' };
    return m[estado] ?? estado;
  }
  colorAvance(avance: number): string {
    if (avance >= 80) return 'avance-alto';
    if (avance >= 40) return 'avance-medio';
    return 'avance-bajo';
  }
}
