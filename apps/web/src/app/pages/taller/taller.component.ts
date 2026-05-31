import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { TallerService } from '@core/services/taller.service';
import { VehiculosService } from '@core/services/vehiculos.service';
import { TareasDefinicionService } from '@core/services/tareas-definicion.service';
import { DialogoService } from '@core/services/dialogo.service';
import { OrdenTrabajo, Vehiculo, TareaDefinicion } from '@core/models';
import { TallerOtDialogComponent } from './taller-ot-dialog.component';

interface MaterialOt {
  repuestoId: string;
  repuestoCodigo?: string;
  repuestoDescripcion?: string;
  repuestoUnidad?: string;
  cantidad: number;
}

@Component({
  selector: 'app-taller',
  standalone: true,
  imports: [
    CommonModule, FormsModule, ReactiveFormsModule,
    MatTableModule, MatButtonModule, MatIconModule,
    MatInputModule, MatFormFieldModule, MatSelectModule, MatProgressSpinnerModule,
    MatProgressBarModule, MatSnackBarModule, MatTooltipModule, MatDialogModule,
  ],
  template: `
<div class="pagina-taller">

  <div class="encabezado-pagina">
    <h1>Taller — Órdenes de trabajo</h1>
    <button mat-flat-button class="btn-principal" (click)="abrirFormulario()">
      <mat-icon>add</mat-icon> Nueva OT
    </button>
  </div>

  <div class="barra-filtros">
    <mat-form-field appearance="fill">
      <mat-label>Estado</mat-label>
      <mat-select [(ngModel)]="filtroEstado" (ngModelChange)="cargar()">
        <mat-option value="">Todos</mat-option>
        <mat-option value="PENDIENTE">Pendiente</mat-option>
        <mat-option value="EN_EJECUCION">En ejecución</mat-option>
        <mat-option value="CERRADA">Cerrada</mat-option>
      </mat-select>
    </mat-form-field>
    <mat-form-field appearance="fill">
      <mat-label>Tipo</mat-label>
      <mat-select [(ngModel)]="filtroTipo" (ngModelChange)="cargar()">
        <mat-option value="">Todos</mat-option>
        <mat-option value="PREVENTIVA">Preventivo</mat-option>
        <mat-option value="CORRECTIVA">Correctivo</mat-option>
        <mat-option value="NEUMATICOS">Neumáticos</mat-option>
        <mat-option value="ELECTRICA">Eléctrica</mat-option>
      </mat-select>
    </mat-form-field>
  </div>

  @if (cargando()) {
    <div class="spinner-centrado"><mat-spinner diameter="40" /></div>
  } @else {
    <div class="superficie" style="padding:0;overflow:hidden">
      <table mat-table [dataSource]="ordenes()">

        <ng-container matColumnDef="numero">
          <th mat-header-cell *matHeaderCellDef>#</th>
          <td mat-cell *matCellDef="let ot">
            <span class="numero-ot">{{ ot.numero }}</span>
          </td>
        </ng-container>

        <ng-container matColumnDef="vehiculo">
          <th mat-header-cell *matHeaderCellDef>Vehículo</th>
          <td mat-cell *matCellDef="let ot">{{ ot.vehiculoPatente || ot.vehiculoId }}</td>
        </ng-container>

        <ng-container matColumnDef="tipo">
          <th mat-header-cell *matHeaderCellDef>Tipo</th>
          <td mat-cell *matCellDef="let ot">
            <span class="badge {{ badgeTipo(ot.tipo) }}">{{ labelTipo(ot.tipo) }}</span>
          </td>
        </ng-container>

        <ng-container matColumnDef="descripcion">
          <th mat-header-cell *matHeaderCellDef>Trabajo</th>
          <td mat-cell *matCellDef="let ot" class="celda-descripcion">{{ ot.descripcion }}</td>
        </ng-container>

        <ng-container matColumnDef="avance">
          <th mat-header-cell *matHeaderCellDef>Avance</th>
          <td mat-cell *matCellDef="let ot">
            <div class="celda-avance">
              <mat-progress-bar mode="determinate" [value]="ot.avance" [class]="colorAvance(ot.avance)" />
              <span class="pct-avance">{{ ot.avance }}%</span>
            </div>
          </td>
        </ng-container>

        <ng-container matColumnDef="estado">
          <th mat-header-cell *matHeaderCellDef>Estado</th>
          <td mat-cell *matCellDef="let ot">
            <span class="badge {{ badgeEstado(ot.estado) }}">{{ labelEstado(ot.estado) }}</span>
          </td>
        </ng-container>

        <ng-container matColumnDef="acciones">
          <th mat-header-cell *matHeaderCellDef></th>
          <td mat-cell *matCellDef="let ot" class="acciones-fila">
            <button mat-icon-button (click)="seleccionar(ot); $event.stopPropagation()" matTooltip="Ver detalle">
              <mat-icon>checklist</mat-icon>
            </button>
            @if (ot.estado !== 'CERRADA') {
              <button mat-icon-button (click)="abrirEdicion(ot); $event.stopPropagation()" matTooltip="Editar">
                <mat-icon>edit</mat-icon>
              </button>
            }
            <button mat-icon-button (click)="confirmarEliminar(ot); $event.stopPropagation()"
                    matTooltip="Eliminar" [disabled]="eliminando() === ot.id" class="btn-danger-icon">
              @if (eliminando() === ot.id) {
                <mat-spinner diameter="16" />
              } @else {
                <mat-icon>delete_outline</mat-icon>
              }
            </button>
          </td>
        </ng-container>

        <tr mat-header-row *matHeaderRowDef="columnas"></tr>
        <tr mat-row *matRowDef="let row; columns: columnas;"
            (click)="seleccionar(row)" style="cursor:pointer"></tr>
      </table>
    </div>
  }
</div>

<!-- ── Formulario nueva OT ───────────────────────────────────────────────────── -->
@if (mostrarFormulario()) {
  <div class="capa-modal" (click)="cerrarFormulario()">
    <div class="panel-modal-ot" (click)="$event.stopPropagation()">
      <div class="panel-encabezado">
        <h2>{{ otEditando() ? 'Editar OT ' + otEditando()!.numero : 'Nueva orden de trabajo' }}</h2>
        <button mat-icon-button (click)="cerrarFormulario()"><mat-icon>close</mat-icon></button>
      </div>

      <form [formGroup]="form" (ngSubmit)="guardar()" class="formulario-grid">
        <mat-form-field appearance="outline">
          <mat-label>Vehículo</mat-label>
          <mat-select formControlName="vehiculoId">
            @for (v of vehiculos(); track v.id) {
              <mat-option [value]="v.id">{{ v.patente }} — {{ v.marca }} {{ v.modelo }}</mat-option>
            }
          </mat-select>
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Tipo de mantenimiento</mat-label>
          <mat-select formControlName="tipo">
            <mat-option value="PREVENTIVA">Preventivo</mat-option>
            <mat-option value="CORRECTIVA">Correctivo</mat-option>
            <mat-option value="NEUMATICOS">Neumáticos</mat-option>
            <mat-option value="ELECTRICA">Eléctrica</mat-option>
          </mat-select>
        </mat-form-field>

        <mat-form-field appearance="outline" style="grid-column:1/-1">
          <mat-label>Nombre del trabajo</mat-label>
          <input matInput formControlName="descripcion" placeholder="Ej: Cambio de aceite y filtros" />
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Mecánico responsable</mat-label>
          <input matInput formControlName="mecanicoResponsable" />
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Fecha cierre estimada</mat-label>
          <input matInput type="date" formControlName="fechaCierreEst" />
        </mat-form-field>

        <!-- Tareas iniciales (solo en nueva OT) -->
        @if (!otEditando()) {
        <div style="grid-column:1/-1">
          <p style="font-weight:600;margin-bottom:8px;color:var(--azul-700)">Tareas del trabajo</p>

          <!-- Selector catálogo -->
          @if (tareasDefinicion().length > 0) {
            <div class="fila-nueva-tarea" style="margin-bottom:8px">
              <mat-form-field appearance="outline" style="flex:1">
                <mat-label>Agregar del catálogo</mat-label>
                <mat-select [(ngModel)]="tareaDefSeleccionada" [ngModelOptions]="{standalone:true}">
                  @for (td of tareasDefinicion(); track td.id) {
                    <mat-option [value]="td">{{ td.nombre }}{{ td.tipoOt ? ' (' + td.tipoOt + ')' : '' }}</mat-option>
                  }
                </mat-select>
              </mat-form-field>
              <button mat-stroked-button type="button" class="btn-secundario" (click)="agregarDelCatalogo()" [disabled]="!tareaDefSeleccionada">
                <mat-icon>library_add</mat-icon>
              </button>
            </div>
          }

          @for (tarea of tareasFormulario; track $index) {
            <div class="fila-tarea-form">
              <mat-icon style="color:var(--azul-300)">drag_indicator</mat-icon>
              <span style="flex:1;font-size:13px">{{ tarea }}</span>
              <button mat-icon-button type="button" (click)="quitarTareaFormulario($index)">
                <mat-icon>close</mat-icon>
              </button>
            </div>
          }
          <div class="fila-nueva-tarea">
            <mat-form-field appearance="outline" style="flex:1">
              <mat-label>Agregar tarea manualmente</mat-label>
              <input matInput [(ngModel)]="nuevaTareaFormulario" [ngModelOptions]="{standalone:true}"
                     (keyup.enter)="agregarTareaFormulario()" placeholder="Ej: Cambio de filtro de aceite" />
            </mat-form-field>
            <button mat-flat-button type="button" class="btn-principal" (click)="agregarTareaFormulario()">
              <mat-icon>add</mat-icon>
            </button>
          </div>

          <!-- Materiales requeridos (acumulados del catálogo) -->
          @if (materialesRequeridos.length > 0) {
            <div class="bloque-materiales-form">
              <div class="materiales-header">
                <mat-icon style="font-size:16px;color:var(--azul-500)">inventory_2</mat-icon>
                <span>Materiales a solicitar al almacén</span>
              </div>
              @for (mat of materialesRequeridos; track mat.repuestoId) {
                <div class="fila-material-form">
                  <span class="mat-cod-form">{{ mat.repuestoCodigo || '—' }}</span>
                  <span style="flex:1;font-size:13px">{{ mat.repuestoDescripcion || mat.repuestoId }}</span>
                  <span class="mat-cant-form">{{ mat.cantidad }} {{ mat.repuestoUnidad || 'UN' }}</span>
                </div>
              }
            </div>
          }
        </div>

        } <!-- /!otEditando -->

        <div class="acciones-form" style="grid-column:1/-1">
          <button mat-button type="button" (click)="cerrarFormulario()">Cancelar</button>
          <button mat-flat-button class="btn-principal" type="submit" [disabled]="form.invalid || guardando()">
            @if (guardando()) { <mat-spinner diameter="18" /> }
            @else { {{ otEditando() ? 'Guardar cambios' : 'Crear OT' }} }
          </button>
        </div>
      </form>
    </div>
  </div>
}
  `,
  styles: [`
    .pagina-taller { display: flex; flex-direction: column; }
    .acciones-fila { white-space: nowrap; }
    .btn-danger-icon { color: #C10A5A !important; }

    .numero-ot { font-weight:700; font-family:monospace; color:var(--azul-600); }
    .celda-descripcion { max-width:220px; white-space:nowrap; overflow:hidden; text-overflow:ellipsis; }
    .celda-avance { display:flex; align-items:center; gap:8px; min-width:120px; }
    .pct-avance { font-size:12px; font-weight:600; color:var(--azul-600); min-width:32px; }

    ::ng-deep .avance-bajo  .mdc-linear-progress__bar-inner { border-color:#C10A5A !important; }
    ::ng-deep .avance-medio .mdc-linear-progress__bar-inner { border-color:#C25E01 !important; }
    ::ng-deep .avance-alto  .mdc-linear-progress__bar-inner { border-color:#007AF5 !important; }

    /* ── Modal centrado ── */
    .capa-modal {
      position: fixed; inset: 0;
      background: rgba(15,23,42,.45);
      display: flex; align-items: center; justify-content: center;
      z-index: 1000;
      padding: 16px;
    }
    .panel-modal-ot {
      background: var(--color-superficie, #fff);
      border-radius: var(--radio-lg, 12px);
      width: min(600px, 96vw);
      max-height: 90vh;
      overflow-y: auto;
      padding: 28px 28px 24px;
      display: flex; flex-direction: column; gap: 20px;
      box-shadow: 0 20px 60px rgba(0,0,0,.22);
    }
    .panel-encabezado { display:flex; justify-content:space-between; align-items:flex-start; }
    .formulario-grid { display:grid; grid-template-columns:1fr 1fr; gap:16px; }
    .acciones-form { display:flex; justify-content:flex-end; gap:8px; }
    .fila-nueva-tarea { display:flex; align-items:center; gap:8px; margin-top:8px; }
    .fila-tarea-form { display:flex; align-items:center; gap:8px; padding:6px 8px; background:var(--azul-50); border-radius:var(--radio-sm); margin-bottom:6px; }

    .bloque-materiales-form { margin-top:12px; border:1px solid var(--azul-100); border-radius:var(--radio-md); overflow:hidden; }
    .materiales-header { display:flex; align-items:center; gap:6px; padding:8px 12px; background:var(--azul-50); font-size:13px; font-weight:600; color:var(--azul-700); border-bottom:1px solid var(--azul-100); }
    .fila-material-form { display:flex; align-items:center; gap:8px; padding:7px 12px; border-bottom:1px solid var(--azul-50); font-size:13px; }
    .fila-material-form:last-child { border-bottom:none; }
    .mat-cod-form { font-family:monospace; font-size:11px; color:var(--azul-500); min-width:60px; }
    .mat-cant-form { font-weight:600; color:var(--azul-700); min-width:50px; text-align:right; }
  `],
})
export class TallerComponent implements OnInit {
  private readonly svc       = inject(TallerService);
  private readonly vehSvc    = inject(VehiculosService);
  private readonly tareasSvc = inject(TareasDefinicionService);
  private readonly fb        = inject(FormBuilder);
  private readonly snack     = inject(MatSnackBar);
  private readonly dialog    = inject(MatDialog);
  private readonly dialogo   = inject(DialogoService);

  ordenes           = signal<OrdenTrabajo[]>([]);
  vehiculos         = signal<Vehiculo[]>([]);
  tareasDefinicion  = signal<TareaDefinicion[]>([]);
  cargando          = signal(false);
  guardando         = signal(false);
  eliminando        = signal<string | null>(null);
  mostrarFormulario = signal(false);
  otEditando        = signal<OrdenTrabajo | null>(null);

  filtroEstado = '';
  filtroTipo   = '';
  nuevaTareaFormulario  = '';
  tareasFormulario: string[] = [];
  tareaDefSeleccionada: TareaDefinicion | null = null;
  materialesRequeridos: MaterialOt[] = [];

  columnas = ['numero', 'vehiculo', 'tipo', 'descripcion', 'avance', 'estado', 'acciones'];

  form = this.fb.group({
    vehiculoId:          ['', Validators.required],
    tipo:                ['', Validators.required],
    descripcion:         ['', Validators.required],
    mecanicoResponsable: [''],
    fechaCierreEst:      [''],
  });

  ngOnInit() {
    this.cargar();
    this.vehSvc.getAll().subscribe(r => this.vehiculos.set(r.content ?? []));
    this.tareasSvc.getAllActivos().subscribe(list => this.tareasDefinicion.set(list));
  }

  cargar() {
    this.cargando.set(true);
    this.svc.getAll({ estado: this.filtroEstado || undefined, tipo: this.filtroTipo || undefined })
      .subscribe({
        next: r => { this.ordenes.set(r.content ?? []); this.cargando.set(false); },
        error: () => this.cargando.set(false),
      });
  }

  seleccionar(ot: OrdenTrabajo) {
    this.svc.getById(ot.id).subscribe(detalle => {
      const ref = this.dialog.open(TallerOtDialogComponent, {
        width: '900px',
        maxWidth: '96vw',
        maxHeight: '90vh',
        panelClass: 'dlg-ot',
        data: {
          ot: detalle,
          vehiculos: this.vehiculos(),
          tareasDefinicion: this.tareasDefinicion(),
        },
      });
      ref.afterClosed().subscribe(actualizada => {
        if (actualizada) {
          this.ordenes.update(list => list.map(o => o.id === actualizada.id ? actualizada : o));
        }
      });
    });
  }

  abrirFormulario()  { this.otEditando.set(null); this.mostrarFormulario.set(true); }

  abrirEdicion(ot: OrdenTrabajo) {
    this.otEditando.set(ot);
    this.form.patchValue({
      vehiculoId:          ot.vehiculoId,
      tipo:                ot.tipo,
      descripcion:         ot.descripcion,
      mecanicoResponsable: ot.mecanicoResponsable ?? '',
      fechaCierreEst:      ot.fechaCierreEst ?? '',
    });
    this.mostrarFormulario.set(true);
  }

  cerrarFormulario() {
    this.mostrarFormulario.set(false);
    this.otEditando.set(null);
    this.form.reset();
    this.tareasFormulario = [];
    this.materialesRequeridos = [];
    this.tareaDefSeleccionada = null;
  }

  async confirmarEliminar(ot: OrdenTrabajo) {
    const ok = await this.dialogo.confirmarEliminar(
      `¿Eliminar la OT ${ot.numero}?`,
      `${ot.tipo} · ${ot.descripcion?.slice(0, 60) ?? ''}`
    );
    if (!ok) return;
    this.eliminando.set(ot.id);
    this.svc.delete(ot.id).subscribe({
      next: () => {
        this.eliminando.set(null);
        this.ordenes.update(list => list.filter(o => o.id !== ot.id));
        this.snack.open(`OT ${ot.numero} eliminada`, '', { duration: 2500 });
      },
      error: () => {
        this.eliminando.set(null);
        this.snack.open('Error al eliminar la OT', '', { duration: 3000 });
      },
    });
  }

  agregarTareaFormulario() {
    const t = this.nuevaTareaFormulario.trim();
    if (!t) return;
    this.tareasFormulario.push(t);
    this.nuevaTareaFormulario = '';
  }

  quitarTareaFormulario(i: number) { this.tareasFormulario.splice(i, 1); }

  agregarDelCatalogo() {
    const td = this.tareaDefSeleccionada;
    if (!td) return;
    this.tareasFormulario.push(td.nombre);
    for (const art of td.articulos ?? []) {
      const existing = this.materialesRequeridos.find(m => m.repuestoId === art.repuestoId);
      if (existing) {
        existing.cantidad += art.cantidad;
      } else {
        this.materialesRequeridos.push({
          repuestoId:          art.repuestoId,
          repuestoCodigo:      art.repuestoCodigo,
          repuestoDescripcion: art.repuestoNombre,
          repuestoUnidad:      art.repuestoUnidad,
          cantidad:            art.cantidad,
        });
      }
    }
    this.tareaDefSeleccionada = null;
  }

  guardar() {
    if (this.form.invalid) return;
    this.guardando.set(true);
    const v  = this.form.value;
    const ed = this.otEditando();

    if (ed) {
      // ── Edición ──────────────────────────────────────────────────────────────
      this.svc.update(ed.id, {
        vehiculoId:          v.vehiculoId!,
        tipo:                v.tipo as any,
        descripcion:         v.descripcion!,
        mecanicoResponsable: v.mecanicoResponsable || undefined,
        fechaCierreEst:      v.fechaCierreEst || undefined,
      }).subscribe({
        next: actualizada => {
          this.guardando.set(false);
          this.cerrarFormulario();
          this.ordenes.update(list => list.map(o => o.id === actualizada.id ? actualizada : o));
          this.snack.open('OT actualizada', '', { duration: 2500 });
        },
        error: () => this.guardando.set(false),
      });
    } else {
      // ── Creación ─────────────────────────────────────────────────────────────
      this.svc.create({
        vehiculoId:          v.vehiculoId!,
        tipo:                v.tipo as any,
        descripcion:         v.descripcion!,
        mecanicoResponsable: v.mecanicoResponsable || undefined,
        fechaCierreEst:      v.fechaCierreEst || undefined,
        tareas: this.tareasFormulario.map((d, i) => ({ descripcion: d, orden: i })),
      }).subscribe({
        next: ot => {
          this.guardando.set(false);
          this.cerrarFormulario();
          this.cargar();
          this.seleccionar(ot);
          this.snack.open('OT creada', '', { duration: 2500 });
        },
        error: () => this.guardando.set(false),
      });
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
