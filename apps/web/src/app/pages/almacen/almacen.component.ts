import { Component, OnInit, inject, signal } from '@angular/core';
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
import { AlmacenService, AjusteStockDto } from '@core/services/almacen.service';
import { Repuesto } from '@core/models';

@Component({
  selector: 'app-almacen',
  standalone: true,
  imports: [
    CommonModule, FormsModule, ReactiveFormsModule,
    MatTableModule, MatButtonModule, MatIconModule,
    MatInputModule, MatSelectModule, MatProgressSpinnerModule, MatSnackBarModule, MatTooltipModule,
  ],
  template: `
    <div class="encabezado-pagina">
      <h1>Almacén — Inventario</h1>
      <div class="acciones-encabezado">
        <button mat-stroked-button class="btn-secundario" (click)="alternarCriticos()">
          <mat-icon>warning_amber</mat-icon>
          {{ soloCriticos ? 'Ver todos' : 'Solo críticos' }}
        </button>
        <button mat-flat-button class="btn-principal" (click)="abrirFormulario()">
          <mat-icon>add</mat-icon> Nuevo repuesto
        </button>
      </div>
    </div>

    <div class="barra-busqueda">
      <mat-form-field appearance="fill">
        <mat-label>Buscar repuesto</mat-label>
        <input matInput [(ngModel)]="busqueda" (ngModelChange)="cargar()" />
        <mat-icon matSuffix>search</mat-icon>
      </mat-form-field>
    </div>

    @if (cargando()) {
      <div class="spinner-centrado"><mat-spinner diameter="40" /></div>
    } @else {
      <div class="superficie" style="padding:0;overflow:hidden">
        <table mat-table [dataSource]="repuestos()">
          <ng-container matColumnDef="codigo">
            <th mat-header-cell *matHeaderCellDef>Código</th>
            <td mat-cell *matCellDef="let r"><strong>{{ r.codigo }}</strong></td>
          </ng-container>
          <ng-container matColumnDef="descripcion">
            <th mat-header-cell *matHeaderCellDef>Descripción</th>
            <td mat-cell *matCellDef="let r">{{ r.descripcion }}</td>
          </ng-container>
          <ng-container matColumnDef="categoria">
            <th mat-header-cell *matHeaderCellDef>Categoría</th>
            <td mat-cell *matCellDef="let r">{{ r.categoria }}</td>
          </ng-container>
          <ng-container matColumnDef="stockActual">
            <th mat-header-cell *matHeaderCellDef>Stock</th>
            <td mat-cell *matCellDef="let r">
              <span [class]="insigniaStock(r)">{{ r.stockActual }} {{ r.unidad }}</span>
            </td>
          </ng-container>
          <ng-container matColumnDef="stockMinimo">
            <th mat-header-cell *matHeaderCellDef>Mínimo</th>
            <td mat-cell *matCellDef="let r">{{ r.stockMinimo }} {{ r.unidad }}</td>
          </ng-container>
          <ng-container matColumnDef="precioUnitario">
            <th mat-header-cell *matHeaderCellDef>Precio unitario</th>
            <td mat-cell *matCellDef="let r">{{ r.precioUnitario | currency:'CLP':'$':'1.0-0' }}</td>
          </ng-container>
          <ng-container matColumnDef="acciones">
            <th mat-header-cell *matHeaderCellDef></th>
            <td mat-cell *matCellDef="let r" class="columna-acciones">
              <button mat-icon-button (click)="registrarMovimiento(r)" matTooltip="Entrada/Salida" style="color:var(--azul-600)">
                <mat-icon>swap_horiz</mat-icon>
              </button>
              <button mat-icon-button (click)="abrirFormulario(r)" matTooltip="Editar">
                <mat-icon>edit</mat-icon>
              </button>
              <button mat-icon-button color="warn" (click)="eliminar(r)" matTooltip="Eliminar">
                <mat-icon>delete_outline</mat-icon>
              </button>
            </td>
          </ng-container>
          <tr mat-header-row *matHeaderRowDef="columnas"></tr>
          <tr mat-row *matRowDef="let fila; columns: columnas;" [class.fila-critica]="fila.stockActual <= fila.stockMinimo"></tr>
        </table>
        @if (repuestos().length === 0) {
          <div class="estado-vacio-tabla">
            <mat-icon>inventory_2</mat-icon><p>No hay repuestos en inventario.</p>
          </div>
        }
      </div>
    }

    <!-- Modal CRUD repuesto -->
    @if (mostrarFormulario()) {
      <div class="capa-modal" (click)="cerrarFormulario()">
        <div class="panel-modal" (click)="$event.stopPropagation()">
          <h2>{{ idEdicion() ? 'Editar' : 'Nuevo' }} repuesto</h2>
          <form [formGroup]="formulario" (ngSubmit)="guardar()">
            <div class="dos-columnas">
              <mat-form-field appearance="fill">
                <mat-label>Código</mat-label>
                <input matInput formControlName="codigo" />
              </mat-form-field>
              <mat-form-field appearance="fill">
                <mat-label>Categoría</mat-label>
                <input matInput formControlName="categoria" />
              </mat-form-field>
            </div>
            <mat-form-field appearance="fill" class="ancho-completo">
              <mat-label>Descripción del repuesto</mat-label>
              <input matInput formControlName="descripcion" />
            </mat-form-field>
            <div class="dos-columnas">
              <mat-form-field appearance="fill">
                <mat-label>Stock mínimo</mat-label>
                <input matInput type="number" formControlName="stockMinimo" />
              </mat-form-field>
              <mat-form-field appearance="fill">
                <mat-label>Unidad de medida</mat-label>
                <input matInput formControlName="unidad" placeholder="UN, LT, KG…" />
              </mat-form-field>
            </div>
            <mat-form-field appearance="fill" class="ancho-completo">
              <mat-label>Precio unitario (CLP)</mat-label>
              <input matInput type="number" formControlName="precioUnitario" />
            </mat-form-field>
            <div class="acciones-formulario">
              <button mat-button type="button" (click)="cerrarFormulario()">Cancelar</button>
              <button mat-flat-button class="btn-principal" type="submit" [disabled]="formulario.invalid || guardando()">
                {{ guardando() ? 'Guardando…' : 'Guardar' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    }

    <!-- Modal ajuste de stock -->
    @if (mostrarAjuste()) {
      <div class="capa-modal" (click)="cerrarAjuste()">
        <div class="panel-modal panel-ajuste" (click)="$event.stopPropagation()">
          <h2>Movimiento de stock</h2>
          <p class="subtitulo-ajuste">
            <strong>{{ repuestoAjuste()?.codigo }}</strong> — {{ repuestoAjuste()?.descripcion }}<br>
            <span class="texto-atenuado">Stock actual: <strong>{{ repuestoAjuste()?.stockActual }} {{ repuestoAjuste()?.unidad }}</strong></span>
          </p>
          <form [formGroup]="formularioAjuste" (ngSubmit)="confirmarAjuste()">
            <mat-form-field appearance="fill" class="ancho-completo">
              <mat-label>Tipo de movimiento</mat-label>
              <mat-select formControlName="tipo">
                <mat-option value="ENTRADA">Entrada (aumenta stock)</mat-option>
                <mat-option value="SALIDA">Salida (reduce stock)</mat-option>
                <mat-option value="AJUSTE">Ajuste manual</mat-option>
              </mat-select>
            </mat-form-field>
            <mat-form-field appearance="fill" class="ancho-completo">
              <mat-label>Cantidad</mat-label>
              <input matInput type="number" formControlName="cantidad" min="1" />
            </mat-form-field>
            <mat-form-field appearance="fill" class="ancho-completo">
              <mat-label>Referencia (opcional)</mat-label>
              <input matInput formControlName="referencia" placeholder="N° guía, orden, etc." />
            </mat-form-field>
            <mat-form-field appearance="fill" class="ancho-completo">
              <mat-label>Observación (opcional)</mat-label>
              <input matInput formControlName="observacion" />
            </mat-form-field>
            <div class="acciones-formulario">
              <button mat-button type="button" (click)="cerrarAjuste()">Cancelar</button>
              <button mat-flat-button class="btn-principal" type="submit" [disabled]="formularioAjuste.invalid || guardando()">
                {{ guardando() ? 'Procesando…' : 'Confirmar' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    }
  `,
  styles: [`
    .columna-acciones { white-space: nowrap; }
    .fila-critica td { background: rgba(220,38,38,.04) !important; }
    .estado-vacio-tabla {
      display: flex; flex-direction: column; align-items: center; padding: 48px;
      gap: 8px; color: var(--color-texto-3);
      mat-icon { font-size: 48px; width: 48px; height: 48px; opacity: .3; }
    }
    .panel-ajuste { max-width: 420px; }
    .subtitulo-ajuste { font-size: 13px; color: var(--color-texto-2); margin: -8px 0 16px; line-height: 1.6; }
  `],
})
export class AlmacenComponent implements OnInit {
  private readonly servicio     = inject(AlmacenService);
  private readonly fb           = inject(FormBuilder);
  private readonly notificacion = inject(MatSnackBar);

  columnas          = ['codigo', 'descripcion', 'categoria', 'stockActual', 'stockMinimo', 'precioUnitario', 'acciones'];
  cargando          = signal(true);
  guardando         = signal(false);
  mostrarFormulario = signal(false);
  mostrarAjuste     = signal(false);
  idEdicion         = signal<string | null>(null);
  repuestoAjuste    = signal<Repuesto | null>(null);
  repuestos         = signal<Repuesto[]>([]);
  busqueda          = '';
  soloCriticos      = false;

  formulario = this.fb.group({
    codigo:         ['', Validators.required],
    descripcion:    ['', Validators.required],
    categoria:      [''],
    stockMinimo:    [1, Validators.required],
    unidad:         ['UN'],
    precioUnitario: [0],
  });

  formularioAjuste = this.fb.group({
    tipo:        ['ENTRADA', Validators.required],
    cantidad:    [1, [Validators.required, Validators.min(1)]],
    referencia:  [''],
    observacion: [''],
  });

  ngOnInit() { this.cargar(); }

  cargar() {
    this.cargando.set(true);
    if (this.soloCriticos) {
      this.servicio.getBajoStock().subscribe({
        next: r => { this.repuestos.set(r); this.cargando.set(false); },
        error: () => this.cargando.set(false),
      });
    } else {
      this.servicio.getAll({ search: this.busqueda || undefined }).subscribe({
        next: r => { this.repuestos.set(r.content); this.cargando.set(false); },
        error: () => this.cargando.set(false),
      });
    }
  }

  alternarCriticos() { this.soloCriticos = !this.soloCriticos; this.cargar(); }

  abrirFormulario(r?: Repuesto) {
    this.idEdicion.set(r?.id ?? null);
    if (r) this.formulario.patchValue(r as any);
    else this.formulario.reset({ stockMinimo: 1, unidad: 'UN', precioUnitario: 0 });
    this.mostrarFormulario.set(true);
  }

  cerrarFormulario() { this.mostrarFormulario.set(false); }

  guardar() {
    if (this.formulario.invalid) return;
    this.guardando.set(true);
    const solicitud = this.formulario.value as any;
    const operacion = this.idEdicion() ? this.servicio.update(this.idEdicion()!, solicitud) : this.servicio.create(solicitud);
    operacion.subscribe({
      next: () => { this.guardando.set(false); this.cerrarFormulario(); this.cargar(); this.notificacion.open('Guardado', '', { duration: 3000 }); },
      error: () => { this.guardando.set(false); this.notificacion.open('Error al guardar', '', { duration: 3000 }); },
    });
  }

  registrarMovimiento(r: Repuesto) {
    this.repuestoAjuste.set(r);
    this.formularioAjuste.reset({ tipo: 'ENTRADA', cantidad: 1, referencia: '', observacion: '' });
    this.mostrarAjuste.set(true);
  }

  cerrarAjuste() { this.mostrarAjuste.set(false); this.repuestoAjuste.set(null); }

  confirmarAjuste() {
    const rep = this.repuestoAjuste();
    if (!rep || this.formularioAjuste.invalid) return;
    this.guardando.set(true);
    const v = this.formularioAjuste.value;
    const dto: AjusteStockDto = {
      tipo:        v.tipo as 'ENTRADA' | 'SALIDA' | 'AJUSTE',
      cantidad:    v.cantidad!,
      referencia:  v.referencia || undefined,
      observacion: v.observacion || undefined,
    };
    this.servicio.ajustarStock(rep.id, dto).subscribe({
      next: () => {
        this.guardando.set(false);
        this.cerrarAjuste();
        this.cargar();
        this.notificacion.open('Movimiento registrado', '', { duration: 3000 });
      },
      error: (err) => {
        this.guardando.set(false);
        const msg = err?.error?.message ?? 'Error al registrar movimiento';
        this.notificacion.open(msg, '', { duration: 4000 });
      },
    });
  }

  eliminar(r: Repuesto) {
    if (!confirm(`¿Eliminar el repuesto "${r.descripcion}"?`)) return;
    this.servicio.delete(r.id).subscribe({ next: () => this.cargar() });
  }

  insigniaStock(r: Repuesto): string {
    if (r.stockActual <= 0)              return 'insignia insignia-peligro';
    if (r.stockActual <= r.stockMinimo)  return 'insignia insignia-advertencia';
    return 'insignia insignia-exito';
  }
}
