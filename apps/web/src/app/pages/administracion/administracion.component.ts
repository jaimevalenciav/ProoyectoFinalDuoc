import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatTabsModule } from '@angular/material/tabs';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatChipsModule } from '@angular/material/chips';
import { AlmacenService } from '@core/services/almacen.service';
import { TareasDefinicionService } from '@core/services/tareas-definicion.service';
import { Repuesto, TareaDefinicion } from '@core/models';

@Component({
  selector: 'app-administracion',
  standalone: true,
  imports: [
    CommonModule, FormsModule, ReactiveFormsModule,
    MatTabsModule, MatButtonModule, MatIconModule, MatInputModule,
    MatSelectModule, MatTableModule, MatProgressSpinnerModule,
    MatSnackBarModule, MatChipsModule,
  ],
  template: `
    <div class="encabezado-pagina">
      <h1>Administración</h1>
    </div>

    <mat-tab-group animationDuration="200ms" class="tabs-admin">

      <!-- ══════════ TAB: Artículos Almacén ══════════ -->
      <mat-tab label="Artículos Almacén">
        <div class="tab-contenido">

          <div class="barra-acciones">
            <mat-form-field appearance="fill" style="width:280px">
              <mat-label>Buscar artículo</mat-label>
              <input matInput [(ngModel)]="busquedaRep" (ngModelChange)="cargarRepuestos()" />
              <mat-icon matSuffix>search</mat-icon>
            </mat-form-field>
            <mat-form-field appearance="fill" style="width:180px">
              <mat-label>Categoría</mat-label>
              <mat-select [(ngModel)]="categoriaRep" (ngModelChange)="cargarRepuestos()">
                <mat-option value="">Todas</mat-option>
                <mat-option value="LUBRICANTES">Lubricantes</mat-option>
                <mat-option value="FILTROS">Filtros</mat-option>
                <mat-option value="FRENOS">Frenos</mat-option>
                <mat-option value="ELECTRICO">Eléctrico</mat-option>
                <mat-option value="NEUMATICOS">Neumáticos</mat-option>
                <mat-option value="GENERAL">General</mat-option>
              </mat-select>
            </mat-form-field>
            <span style="flex:1"></span>
            <button class="btn-primary" (click)="abrirRepuesto()">
              <mat-icon>add</mat-icon> Nuevo artículo
            </button>
          </div>

          @if (cargandoRep()) {
            <div class="spinner-centrado"><mat-spinner diameter="36" /></div>
          } @else {
            <div class="superficie" style="padding:0;overflow:hidden">
              <table mat-table [dataSource]="repuestos()">
                <ng-container matColumnDef="codigo">
                  <th mat-header-cell *matHeaderCellDef>Código</th>
                  <td mat-cell *matCellDef="let r"><span class="badge-mono">{{ r.codigo }}</span></td>
                </ng-container>
                <ng-container matColumnDef="descripcion">
                  <th mat-header-cell *matHeaderCellDef>Descripción</th>
                  <td mat-cell *matCellDef="let r">
                    <div style="font-weight:500">{{ r.descripcion }}</div>
                    <div style="font-size:11px;color:var(--ink-soft)">{{ r.categoria }} · {{ r.unidad }}</div>
                  </td>
                </ng-container>
                <ng-container matColumnDef="stock">
                  <th mat-header-cell *matHeaderCellDef>Stock</th>
                  <td mat-cell *matCellDef="let r">
                    <span [class]="r.stockActual <= r.stockMinimo ? 'pill pill-fuera' : 'pill pill-activo'">
                      {{ r.stockActual }} / {{ r.stockMinimo }} min
                    </span>
                  </td>
                </ng-container>
                <ng-container matColumnDef="precio">
                  <th mat-header-cell *matHeaderCellDef>Precio unit.</th>
                  <td mat-cell *matCellDef="let r">{{ r.precioUnitario | currency:'CLP':'symbol':'1.0-0' }}</td>
                </ng-container>
                <ng-container matColumnDef="acciones">
                  <th mat-header-cell *matHeaderCellDef></th>
                  <td mat-cell *matCellDef="let r" style="white-space:nowrap">
                    <button mat-icon-button (click)="abrirAjuste(r)" matTooltip="Ajustar stock">
                      <mat-icon>inventory</mat-icon>
                    </button>
                    <button mat-icon-button (click)="abrirRepuesto(r)" matTooltip="Editar">
                      <mat-icon>edit</mat-icon>
                    </button>
                    <button mat-icon-button color="warn" (click)="eliminarRepuesto(r)" matTooltip="Eliminar">
                      <mat-icon>delete_outline</mat-icon>
                    </button>
                  </td>
                </ng-container>
                <tr mat-header-row *matHeaderRowDef="colsRepuesto"></tr>
                <tr mat-row *matRowDef="let r; columns: colsRepuesto;"></tr>
              </table>
              @if (repuestos().length === 0) {
                <div class="estado-vacio-tabla">
                  <mat-icon>inventory_2</mat-icon>
                  <p>No hay artículos registrados.</p>
                </div>
              }
            </div>
          }
        </div>
      </mat-tab>

      <!-- ══════════ TAB: Tareas OT ══════════ -->
      <mat-tab label="Tareas OT (Catálogo)">
        <div class="tab-contenido">
          <div class="barra-acciones">
            <span style="flex:1"></span>
            <button class="btn-primary" (click)="abrirTarea()">
              <mat-icon>add</mat-icon> Nueva tarea
            </button>
          </div>

          @if (cargandoTarea()) {
            <div class="spinner-centrado"><mat-spinner diameter="36" /></div>
          } @else {
            <div class="cuadricula-tareas">
              @for (td of tareasDef(); track td.id) {
                <div class="tarjeta-tarea superficie">
                  <div class="tarea-cabecera">
                    <div>
                      <div class="tarea-nombre">{{ td.nombre }}</div>
                      @if (td.tipoOt) {
                        <span class="pill pill-info" style="margin-top:4px;display:inline-flex">{{ td.tipoOt }}</span>
                      }
                    </div>
                    <div style="display:flex;gap:4px">
                      <button mat-icon-button (click)="abrirTarea(td)"><mat-icon>edit</mat-icon></button>
                      <button mat-icon-button color="warn" (click)="eliminarTarea(td)"><mat-icon>delete_outline</mat-icon></button>
                    </div>
                  </div>
                  @if (td.descripcion) {
                    <p class="tarea-desc">{{ td.descripcion }}</p>
                  }
                  @if (td.articulos?.length) {
                    <div class="tarea-articulos">
                      <div class="articulos-titulo">Artículos requeridos:</div>
                      @for (art of td.articulos; track art.id) {
                        <div class="art-fila">
                          <mat-icon style="font-size:14px;width:14px;height:14px;color:var(--ink-soft)">build</mat-icon>
                          <span>{{ art.repuestoNombre || art.repuestoId }}</span>
                          <span class="badge-mono">×{{ art.cantidad }}</span>
                          @if (art.repuestoUnidad) { <span style="color:var(--ink-soft);font-size:11px">{{ art.repuestoUnidad }}</span> }
                        </div>
                      }
                    </div>
                  } @else {
                    <p style="font-size:12px;color:var(--ink-soft);margin-top:8px">Sin artículos asociados</p>
                  }
                </div>
              }
              @if (tareasDef().length === 0) {
                <div class="estado-vacio-tabla" style="grid-column:1/-1">
                  <mat-icon>assignment</mat-icon>
                  <p>No hay tareas definidas.</p>
                </div>
              }
            </div>
          }
        </div>
      </mat-tab>

    </mat-tab-group>

    <!-- ══════════ MODAL: Artículo ══════════ -->
    @if (modalRepuesto()) {
      <div class="capa-modal" (click)="cerrarRepuesto()">
        <div class="panel-modal" (click)="$event.stopPropagation()">
          <h2>{{ idRepuesto() ? 'Editar' : 'Nuevo' }} artículo</h2>
          <form [formGroup]="formRepuesto" (ngSubmit)="guardarRepuesto()">
            <div class="dos-columnas">
              <mat-form-field appearance="fill" class="ancho-completo">
                <mat-label>Código</mat-label>
                <input matInput formControlName="codigo" />
              </mat-form-field>
              <mat-form-field appearance="fill">
                <mat-label>Categoría</mat-label>
                <mat-select formControlName="categoria">
                  <mat-option value="LUBRICANTES">Lubricantes</mat-option>
                  <mat-option value="FILTROS">Filtros</mat-option>
                  <mat-option value="FRENOS">Frenos</mat-option>
                  <mat-option value="ELECTRICO">Eléctrico</mat-option>
                  <mat-option value="NEUMATICOS">Neumáticos</mat-option>
                  <mat-option value="GENERAL">General</mat-option>
                </mat-select>
              </mat-form-field>
            </div>
            <mat-form-field appearance="fill" class="ancho-completo">
              <mat-label>Descripción</mat-label>
              <input matInput formControlName="descripcion" />
            </mat-form-field>
            <div class="dos-columnas">
              <mat-form-field appearance="fill">
                <mat-label>Unidad</mat-label>
                <mat-select formControlName="unidad">
                  <mat-option value="UN">Unidad</mat-option>
                  <mat-option value="LT">Litro</mat-option>
                  <mat-option value="KG">Kilogramo</mat-option>
                  <mat-option value="MT">Metro</mat-option>
                  <mat-option value="JGO">Juego</mat-option>
                </mat-select>
              </mat-form-field>
              <mat-form-field appearance="fill">
                <mat-label>Precio unitario</mat-label>
                <input matInput type="number" formControlName="precioUnitario" />
              </mat-form-field>
            </div>
            <div class="dos-columnas">
              <mat-form-field appearance="fill">
                <mat-label>Stock actual</mat-label>
                <input matInput type="number" formControlName="stockActual" />
              </mat-form-field>
              <mat-form-field appearance="fill">
                <mat-label>Stock mínimo</mat-label>
                <input matInput type="number" formControlName="stockMinimo" />
              </mat-form-field>
            </div>
            <mat-form-field appearance="fill" class="ancho-completo">
              <mat-label>Proveedor</mat-label>
              <input matInput formControlName="proveedor" />
            </mat-form-field>
            <div class="acciones-formulario">
              <button mat-button type="button" (click)="cerrarRepuesto()">Cancelar</button>
              <button mat-flat-button class="btn-principal" type="submit" [disabled]="formRepuesto.invalid || guardando()">Guardar</button>
            </div>
          </form>
        </div>
      </div>
    }

    <!-- ══════════ MODAL: Ajuste Stock ══════════ -->
    @if (modalAjuste()) {
      <div class="capa-modal" (click)="cerrarAjuste()">
        <div class="panel-modal" style="max-width:420px" (click)="$event.stopPropagation()">
          <h2>Ajustar stock — {{ repuestoAjuste()?.descripcion }}</h2>
          <form [formGroup]="formAjuste" (ngSubmit)="guardarAjuste()">
            <mat-form-field appearance="fill" class="ancho-completo">
              <mat-label>Tipo movimiento</mat-label>
              <mat-select formControlName="tipo">
                <mat-option value="ENTRADA">Entrada (compra/recepción)</mat-option>
                <mat-option value="SALIDA">Salida (uso/entrega)</mat-option>
                <mat-option value="AJUSTE">Ajuste de inventario</mat-option>
              </mat-select>
            </mat-form-field>
            <mat-form-field appearance="fill" class="ancho-completo">
              <mat-label>Cantidad</mat-label>
              <input matInput type="number" formControlName="cantidad" min="1" />
            </mat-form-field>
            <mat-form-field appearance="fill" class="ancho-completo">
              <mat-label>Referencia / documento</mat-label>
              <input matInput formControlName="referencia" placeholder="Ej: OC-2345, OT-1234" />
            </mat-form-field>
            <div class="acciones-formulario">
              <button mat-button type="button" (click)="cerrarAjuste()">Cancelar</button>
              <button mat-flat-button class="btn-principal" type="submit" [disabled]="formAjuste.invalid || guardando()">Registrar</button>
            </div>
          </form>
        </div>
      </div>
    }

    <!-- ══════════ MODAL: Tarea Definición ══════════ -->
    @if (modalTarea()) {
      <div class="capa-modal" (click)="cerrarTarea()">
        <div class="panel-modal" style="width:580px" (click)="$event.stopPropagation()">
          <h2>{{ idTarea() ? 'Editar' : 'Nueva' }} tarea</h2>
          <form [formGroup]="formTarea" (ngSubmit)="guardarTarea()">
            <mat-form-field appearance="fill" class="ancho-completo">
              <mat-label>Nombre</mat-label>
              <input matInput formControlName="nombre" />
            </mat-form-field>
            <div class="dos-columnas">
              <mat-form-field appearance="fill">
                <mat-label>Tipo OT asociado</mat-label>
                <mat-select formControlName="tipoOt">
                  <mat-option value="">Cualquiera</mat-option>
                  <mat-option value="PREVENTIVA">Preventiva</mat-option>
                  <mat-option value="CORRECTIVA">Correctiva</mat-option>
                  <mat-option value="NEUMATICOS">Neumáticos</mat-option>
                  <mat-option value="ELECTRICA">Eléctrica</mat-option>
                </mat-select>
              </mat-form-field>
              <mat-form-field appearance="fill">
                <mat-label>Descripción</mat-label>
                <input matInput formControlName="descripcion" />
              </mat-form-field>
            </div>

            <!-- Artículos requeridos -->
            <div class="seccion-articulos">
              <div class="seccion-titulo">Artículos requeridos</div>
              @for (art of articulosTarea; track $index) {
                <div class="fila-articulo-edit">
                  <mat-form-field appearance="fill" style="flex:1">
                    <mat-label>Artículo</mat-label>
                    <mat-select [(ngModel)]="art.repuestoId" [ngModelOptions]="{standalone:true}">
                      @for (r of repuestos(); track r.id) {
                        <mat-option [value]="r.id">{{ r.codigo }} — {{ r.descripcion }}</mat-option>
                      }
                    </mat-select>
                  </mat-form-field>
                  <mat-form-field appearance="fill" style="width:90px">
                    <mat-label>Cantidad</mat-label>
                    <input matInput type="number" [(ngModel)]="art.cantidad" [ngModelOptions]="{standalone:true}" min="1" />
                  </mat-form-field>
                  <button mat-icon-button type="button" color="warn" (click)="quitarArticuloTarea($index)">
                    <mat-icon>remove_circle_outline</mat-icon>
                  </button>
                </div>
              }
              <button mat-button type="button" class="btn-ghost" (click)="agregarArticuloTarea()">
                <mat-icon>add</mat-icon> Agregar artículo
              </button>
            </div>

            <div class="acciones-formulario">
              <button mat-button type="button" (click)="cerrarTarea()">Cancelar</button>
              <button mat-flat-button class="btn-principal" type="submit" [disabled]="formTarea.invalid || guardando()">Guardar</button>
            </div>
          </form>
        </div>
      </div>
    }
  `,
  styles: [`
    .tabs-admin { margin-top: -8px; }
    .tab-contenido { padding: 20px 0; }
    .barra-acciones { display:flex; gap:12px; align-items:flex-end; margin-bottom:16px; flex-wrap:wrap; }
    .estado-vacio-tabla {
      display:flex; flex-direction:column; align-items:center; padding:48px;
      gap:8px; color:var(--ink-soft);
      mat-icon { font-size:48px; width:48px; height:48px; opacity:.3; }
    }
    .cuadricula-tareas {
      display:grid; grid-template-columns:repeat(auto-fill,minmax(320px,1fr)); gap:16px;
    }
    .tarjeta-tarea { display:flex; flex-direction:column; gap:8px; }
    .tarea-cabecera { display:flex; justify-content:space-between; align-items:flex-start; }
    .tarea-nombre { font-weight:600; font-size:14px; color:var(--ink); }
    .tarea-desc { font-size:12px; color:var(--ink-mid); margin:0; }
    .tarea-articulos {
      background:var(--slate); border-radius:var(--radius-sm);
      padding:10px 12px; display:flex; flex-direction:column; gap:6px;
    }
    .articulos-titulo { font-size:11px; font-weight:700; color:var(--ink-soft); text-transform:uppercase; letter-spacing:.05em; margin-bottom:4px; }
    .art-fila { display:flex; align-items:center; gap:8px; font-size:12px; color:var(--ink); }
    .seccion-articulos { border:1px solid var(--slate-dark); border-radius:var(--radius-md); padding:12px 16px; display:flex; flex-direction:column; gap:8px; }
    .seccion-titulo { font-size:12px; font-weight:700; color:var(--ink-soft); text-transform:uppercase; letter-spacing:.05em; margin-bottom:4px; }
    .fila-articulo-edit { display:flex; align-items:center; gap:8px; }
  `],
})
export class AdministracionComponent implements OnInit {
  private readonly almacenSvc = inject(AlmacenService);
  private readonly tareasSvc  = inject(TareasDefinicionService);
  private readonly snack      = inject(MatSnackBar);
  private readonly fb         = inject(FormBuilder);

  // ── Repuestos ─────────────────────────────────────────────────
  repuestos    = signal<Repuesto[]>([]);
  cargandoRep  = signal(false);
  modalRepuesto = signal(false);
  modalAjuste   = signal(false);
  idRepuesto    = signal<string | null>(null);
  repuestoAjuste = signal<Repuesto | null>(null);
  busquedaRep  = '';
  categoriaRep = '';
  colsRepuesto = ['codigo', 'descripcion', 'stock', 'precio', 'acciones'];
  guardando    = signal(false);

  formRepuesto = this.fb.group({
    codigo:        ['', Validators.required],
    descripcion:   ['', Validators.required],
    categoria:     ['GENERAL', Validators.required],
    unidad:        ['UN', Validators.required],
    precioUnitario: [0],
    stockActual:   [0],
    stockMinimo:   [1],
    proveedor:     [''],
  });

  formAjuste = this.fb.group({
    tipo:      ['ENTRADA', Validators.required],
    cantidad:  [1, [Validators.required, Validators.min(1)]],
    referencia:[''],
  });

  // ── Tareas Definición ─────────────────────────────────────────
  tareasDef    = signal<TareaDefinicion[]>([]);
  cargandoTarea = signal(false);
  modalTarea   = signal(false);
  idTarea      = signal<string | null>(null);
  articulosTarea: { repuestoId: string; cantidad: number }[] = [];

  formTarea = this.fb.group({
    nombre:      ['', Validators.required],
    descripcion: [''],
    tipoOt:      [''],
  });

  ngOnInit() {
    this.cargarRepuestos();
    this.cargarTareas();
  }

  // ── Repuestos CRUD ────────────────────────────────────────────
  cargarRepuestos() {
    this.cargandoRep.set(true);
    this.almacenSvc.getAll({ search: this.busquedaRep || undefined, categoria: this.categoriaRep || undefined })
      .subscribe({ next: r => { this.repuestos.set(r.content ?? []); this.cargandoRep.set(false); }, error: () => this.cargandoRep.set(false) });
  }

  abrirRepuesto(r?: Repuesto) {
    this.idRepuesto.set(r?.id ?? null);
    if (r) this.formRepuesto.patchValue(r as any);
    else this.formRepuesto.reset({ categoria: 'GENERAL', unidad: 'UN', precioUnitario: 0, stockActual: 0, stockMinimo: 1 });
    this.modalRepuesto.set(true);
  }
  cerrarRepuesto() { this.modalRepuesto.set(false); }

  guardarRepuesto() {
    if (this.formRepuesto.invalid) return;
    this.guardando.set(true);
    const dto = this.formRepuesto.value as any;
    const op = this.idRepuesto() ? this.almacenSvc.update(this.idRepuesto()!, dto) : this.almacenSvc.create(dto);
    op.subscribe({
      next: () => { this.guardando.set(false); this.cerrarRepuesto(); this.cargarRepuestos(); this.snack.open('Guardado', '', { duration: 2500 }); },
      error: () => { this.guardando.set(false); this.snack.open('Error al guardar', '', { duration: 2500 }); },
    });
  }

  eliminarRepuesto(r: Repuesto) {
    if (!confirm(`¿Eliminar ${r.descripcion}?`)) return;
    this.almacenSvc.delete(r.id).subscribe({ next: () => this.cargarRepuestos() });
  }

  // ── Ajuste de Stock ───────────────────────────────────────────
  abrirAjuste(r: Repuesto) { this.repuestoAjuste.set(r); this.formAjuste.reset({ tipo: 'ENTRADA', cantidad: 1 }); this.modalAjuste.set(true); }
  cerrarAjuste() { this.modalAjuste.set(false); }

  guardarAjuste() {
    if (this.formAjuste.invalid) return;
    this.guardando.set(true);
    const dto = this.formAjuste.value as any;
    this.almacenSvc.ajustarStock(this.repuestoAjuste()!.id, dto).subscribe({
      next: () => { this.guardando.set(false); this.cerrarAjuste(); this.cargarRepuestos(); this.snack.open('Stock actualizado', '', { duration: 2500 }); },
      error: (e) => { this.guardando.set(false); this.snack.open(e?.error?.message || 'Error', '', { duration: 3000 }); },
    });
  }

  // ── Tareas Definición CRUD ────────────────────────────────────
  cargarTareas() {
    this.cargandoTarea.set(true);
    this.tareasSvc.getAll().subscribe({ next: r => { this.tareasDef.set(r.content ?? []); this.cargandoTarea.set(false); }, error: () => this.cargandoTarea.set(false) });
  }

  abrirTarea(td?: TareaDefinicion) {
    this.idTarea.set(td?.id ?? null);
    if (td) {
      this.formTarea.patchValue({ nombre: td.nombre, descripcion: td.descripcion, tipoOt: td.tipoOt });
      this.articulosTarea = (td.articulos ?? []).map(a => ({ repuestoId: a.repuestoId, cantidad: a.cantidad }));
    } else {
      this.formTarea.reset();
      this.articulosTarea = [];
    }
    this.modalTarea.set(true);
    if (!this.repuestos().length) this.almacenSvc.getAllActivos().subscribe(r => this.repuestos.set(r));
  }
  cerrarTarea() { this.modalTarea.set(false); }

  agregarArticuloTarea() { this.articulosTarea.push({ repuestoId: '', cantidad: 1 }); }
  quitarArticuloTarea(i: number) { this.articulosTarea.splice(i, 1); }

  guardarTarea() {
    if (this.formTarea.invalid) return;
    this.guardando.set(true);
    const dto = {
      ...this.formTarea.value,
      articulos: this.articulosTarea.filter(a => a.repuestoId),
    } as any;
    const op = this.idTarea() ? this.tareasSvc.update(this.idTarea()!, dto) : this.tareasSvc.create(dto);
    op.subscribe({
      next: () => { this.guardando.set(false); this.cerrarTarea(); this.cargarTareas(); this.snack.open('Guardado', '', { duration: 2500 }); },
      error: () => { this.guardando.set(false); this.snack.open('Error al guardar', '', { duration: 2500 }); },
    });
  }

  eliminarTarea(td: TareaDefinicion) {
    if (!confirm(`¿Eliminar tarea "${td.nombre}"?`)) return;
    this.tareasSvc.delete(td.id).subscribe({ next: () => this.cargarTareas() });
  }
}
