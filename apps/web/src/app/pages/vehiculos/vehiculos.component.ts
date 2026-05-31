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
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { VehiculosService } from '@core/services/vehiculos.service';
import { Vehiculo } from '@core/models';
import { VehiculoQrPrintComponent } from './vehiculo-qr-print.component';

@Component({
  selector: 'app-vehiculos',
  standalone: true,
  imports: [
    CommonModule, FormsModule, ReactiveFormsModule,
    MatTableModule, MatButtonModule, MatIconModule,
    MatInputModule, MatSelectModule, MatProgressSpinnerModule,
    MatSnackBarModule, MatDialogModule, MatTooltipModule,
  ],
  template: `
    <div class="encabezado-pagina">
      <h1>Vehículos</h1>
      <button mat-flat-button class="btn-principal" (click)="abrirFormulario()">
        <mat-icon>add</mat-icon> Nuevo vehículo
      </button>
    </div>

    <div class="barra-busqueda">
      <mat-form-field appearance="fill">
        <mat-label>Buscar placa o modelo</mat-label>
        <input matInput [(ngModel)]="busqueda" (ngModelChange)="cargar()" />
        <mat-icon matSuffix>search</mat-icon>
      </mat-form-field>
      <mat-form-field appearance="fill">
        <mat-label>Estado</mat-label>
        <mat-select [(ngModel)]="filtroEstado" (ngModelChange)="cargar()">
          <mat-option value="">Todos</mat-option>
          <mat-option value="OPERATIVO">Operativo</mat-option>
          <mat-option value="EN_TALLER">En taller</mat-option>
          <mat-option value="FUERA_SERVICIO">Fuera de servicio</mat-option>
        </mat-select>
      </mat-form-field>
    </div>

    @if (cargando()) {
      <div class="spinner-centrado"><mat-spinner diameter="40" /></div>
    } @else {
      <div class="superficie" style="padding:0;overflow:hidden">
        <table mat-table [dataSource]="vehiculos()">
          <ng-container matColumnDef="patente">
            <th mat-header-cell *matHeaderCellDef>Patente</th>
            <td mat-cell *matCellDef="let v">
              <span class="celda-placa">{{ v.patente }}</span>
            </td>
          </ng-container>
          <ng-container matColumnDef="marcaModelo">
            <th mat-header-cell *matHeaderCellDef>Marca / Modelo</th>
            <td mat-cell *matCellDef="let v">{{ v.marca }} {{ v.modelo }} <span class="texto-atenuado">({{ v.anio }})</span></td>
          </ng-container>
          <ng-container matColumnDef="tipo">
            <th mat-header-cell *matHeaderCellDef>Tipo</th>
            <td mat-cell *matCellDef="let v">{{ v.tipo }}</td>
          </ng-container>
          <ng-container matColumnDef="estado">
            <th mat-header-cell *matHeaderCellDef>Estado</th>
            <td mat-cell *matCellDef="let v">
              <span [class]="insigniaEstado(v.estado)">{{ v.estado }}</span>
            </td>
          </ng-container>
          <ng-container matColumnDef="kilometraje">
            <th mat-header-cell *matHeaderCellDef>Km actuales</th>
            <td mat-cell *matCellDef="let v">{{ v.kmActuales | number:'1.0-0' }} km</td>
          </ng-container>
          <ng-container matColumnDef="acciones">
            <th mat-header-cell *matHeaderCellDef></th>
            <td mat-cell *matCellDef="let v" class="columna-acciones">
              <button mat-icon-button (click)="imprimirQr(v)" matTooltip="Imprimir QR" style="color:#007AF5">
                <mat-icon>qr_code_2</mat-icon>
              </button>
              <button mat-icon-button (click)="abrirFormulario(v)" matTooltip="Editar">
                <mat-icon>edit</mat-icon>
              </button>
              <button mat-icon-button color="warn" (click)="eliminar(v)" matTooltip="Eliminar">
                <mat-icon>delete_outline</mat-icon>
              </button>
            </td>
          </ng-container>
          <tr mat-header-row *matHeaderRowDef="columnas"></tr>
          <tr mat-row *matRowDef="let fila; columns: columnas;"></tr>
        </table>
        @if (vehiculos().length === 0) {
          <div class="estado-vacio-tabla">
            <mat-icon>directions_bus</mat-icon>
            <p>No hay vehículos registrados.</p>
          </div>
        }
      </div>
    }

    @if (mostrarFormulario()) {
      <div class="capa-modal" (click)="cerrarFormulario()">
        <div class="panel-modal" (click)="$event.stopPropagation()">
          <h2>{{ idEdicion() ? 'Editar' : 'Nuevo' }} vehículo</h2>
          <form [formGroup]="formulario" (ngSubmit)="guardar()">
            <mat-form-field appearance="fill" class="ancho-completo">
              <mat-label>Patente</mat-label>
              <input matInput formControlName="patente" placeholder="ABCD-12" />
            </mat-form-field>
            <div class="dos-columnas">
              <mat-form-field appearance="fill">
                <mat-label>Marca</mat-label>
                <input matInput formControlName="marca" />
              </mat-form-field>
              <mat-form-field appearance="fill">
                <mat-label>Modelo</mat-label>
                <input matInput formControlName="modelo" />
              </mat-form-field>
            </div>
            <div class="dos-columnas">
              <mat-form-field appearance="fill">
                <mat-label>Año</mat-label>
                <input matInput type="number" formControlName="anio" />
              </mat-form-field>
              <mat-form-field appearance="fill">
                <mat-label>Tipo</mat-label>
                <mat-select formControlName="tipo">
                  <mat-option value="CAMION">Camión</mat-option>
                  <mat-option value="BUS">Bus</mat-option>
                  <mat-option value="FURGON">Furgón</mat-option>
                  <mat-option value="CAMIONETA">Camioneta</mat-option>
                  <mat-option value="OTRO">Otro</mat-option>
                </mat-select>
              </mat-form-field>
            </div>
            <mat-form-field appearance="fill" class="ancho-completo">
              <mat-label>Km actuales</mat-label>
              <input matInput type="number" formControlName="kmActuales" />
            </mat-form-field>

            <h3 style="font-size:13px;font-weight:600;color:var(--color-texto-2);margin:16px 0 8px;text-transform:uppercase;letter-spacing:.5px">
              Especificaciones técnicas
            </h3>
            <div class="dos-columnas">
              <mat-form-field appearance="fill">
                <mat-label>Capacidad estanque (L)</mat-label>
                <input matInput type="number" formControlName="capacidadEstanque" placeholder="Ej: 400" />
                <span matSuffix>L</span>
              </mat-form-field>
              <mat-form-field appearance="fill">
                <mat-label>Tara (kg)</mat-label>
                <input matInput type="number" formControlName="taraKg" placeholder="Peso vacío" />
                <span matSuffix>kg</span>
              </mat-form-field>
            </div>
            <mat-form-field appearance="fill" class="ancho-completo">
              <mat-label>Capacidad de carga (kg)</mat-label>
              <input matInput type="number" formControlName="capacidadCargaKg" placeholder="Carga máxima permitida" />
              <span matSuffix>kg</span>
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
  `,
  styles: [`
    .celda-placa { font-weight: 700; color: var(--azul-800); font-size: 14px; }
    .columna-acciones { white-space: nowrap; }
    .estado-vacio-tabla {
      display: flex; flex-direction: column; align-items: center; padding: 48px;
      gap: 8px; color: var(--color-texto-3);
      mat-icon { font-size: 48px; width: 48px; height: 48px; opacity: .3; }
    }
  `],
})
export class VehiculosComponent implements OnInit {
  private readonly servicio     = inject(VehiculosService);
  private readonly constructor_ = inject(FormBuilder);
  private readonly notificacion = inject(MatSnackBar);
  private readonly dialog       = inject(MatDialog);

  columnas       = ['patente', 'marcaModelo', 'tipo', 'estado', 'kilometraje', 'acciones'];
  cargando       = signal(true);
  guardando      = signal(false);
  mostrarFormulario = signal(false);
  idEdicion      = signal<string | null>(null);
  vehiculos      = signal<Vehiculo[]>([]);
  busqueda       = '';
  filtroEstado   = '';

  formulario = this.constructor_.group({
    patente:            ['', Validators.required],
    marca:              ['', Validators.required],
    modelo:             ['', Validators.required],
    anio:               [new Date().getFullYear(), Validators.required],
    tipo:               ['CAMION', Validators.required],
    kmActuales:         [0],
    capacidadEstanque:  [null as number | null],
    taraKg:             [null as number | null],
    capacidadCargaKg:   [null as number | null],
  });

  ngOnInit() {
    this.cargar();
    // Auto-mayúsculas en FormControl para que el valor enviado al backend sea uppercase
    (['patente', 'marca', 'modelo'] as const).forEach(campo => {
      this.formulario.get(campo)?.valueChanges.subscribe(v => {
        if (v && v !== v.toUpperCase()) {
          this.formulario.get(campo)?.setValue(v.toUpperCase(), { emitEvent: false });
        }
      });
    });
  }

  cargar() {
    this.cargando.set(true);
    this.servicio.getAll({
      search: this.busqueda  || undefined,
      estado: this.filtroEstado || undefined,
    }).subscribe({
      next: r => { this.vehiculos.set(r.content); this.cargando.set(false); },
      error: () => this.cargando.set(false),
    });
  }

  abrirFormulario(v?: Vehiculo) {
    this.idEdicion.set(v?.id ?? null);
    if (v) this.formulario.patchValue(v as any);
    else this.formulario.reset({ anio: new Date().getFullYear(), tipo: 'CAMION', kmActuales: 0, capacidadEstanque: null, taraKg: null, capacidadCargaKg: null });
    this.mostrarFormulario.set(true);
  }

  cerrarFormulario() { this.mostrarFormulario.set(false); }

  guardar() {
    if (this.formulario.invalid) return;
    this.guardando.set(true);
    const solicitud = this.formulario.value as any;
    const operacion = this.idEdicion()
      ? this.servicio.update(this.idEdicion()!, solicitud)
      : this.servicio.create(solicitud);
    operacion.subscribe({
      next: () => { this.guardando.set(false); this.cerrarFormulario(); this.cargar(); this.notificacion.open('Guardado correctamente', '', { duration: 3000 }); },
      error: () => { this.guardando.set(false); this.notificacion.open('Error al guardar', '', { duration: 3000 }); },
    });
  }

  imprimirQr(v: Vehiculo) {
    this.dialog.open(VehiculoQrPrintComponent, {
      data:        { vehiculo: v },
      width:       '240mm',
      maxWidth:    '98vw',
      maxHeight:   '98vh',
      panelClass:  'dlg-qr-print',
    });
  }

  eliminar(v: Vehiculo) {
    if (!confirm(`¿Eliminar el vehículo ${v.patente}?`)) return;
    this.servicio.delete(v.id).subscribe({ next: () => this.cargar() });
  }

  insigniaEstado(estado: string): string {
    const mapa: Record<string, string> = {
      OPERATIVO:          'insignia insignia-exito',
      EN_TALLER:          'insignia insignia-advertencia',
      FUERA_SERVICIO:     'insignia insignia-peligro',
    };
    return mapa[estado] ?? 'insignia insignia-info';
  }
}
