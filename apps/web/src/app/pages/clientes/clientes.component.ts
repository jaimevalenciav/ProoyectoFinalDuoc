import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { OperacionesService } from '@core/services/operaciones.service';
import { DialogoService } from '@core/services/dialogo.service';
import { Cliente } from '@core/models';

@Component({
  selector: 'app-clientes',
  standalone: true,
  imports: [
    CommonModule, FormsModule, ReactiveFormsModule,
    MatTableModule, MatButtonModule, MatIconModule,
    MatInputModule, MatProgressSpinnerModule, MatSnackBarModule, MatTooltipModule,
  ],
  template: `
    <div class="encabezado-pagina">
      <h1>Clientes</h1>
      <button mat-flat-button class="btn-principal" (click)="abrirFormulario()">
        <mat-icon>add</mat-icon> Nuevo cliente
      </button>
    </div>

    <div class="barra-busqueda">
      <mat-form-field appearance="fill">
        <mat-label>Buscar RUT o razón social</mat-label>
        <input matInput [(ngModel)]="busqueda" (ngModelChange)="cargar()" />
        <mat-icon matSuffix>search</mat-icon>
      </mat-form-field>
    </div>

    @if (cargando()) {
      <div class="spinner-centrado"><mat-spinner diameter="40" /></div>
    } @else {
      <div class="superficie" style="padding:0;overflow:hidden">
        <table mat-table [dataSource]="clientes()">
          <ng-container matColumnDef="rut">
            <th mat-header-cell *matHeaderCellDef>RUT</th>
            <td mat-cell *matCellDef="let c"><strong>{{ c.rut }}</strong></td>
          </ng-container>
          <ng-container matColumnDef="razonSocial">
            <th mat-header-cell *matHeaderCellDef>Razón social</th>
            <td mat-cell *matCellDef="let c">{{ c.razonSocial }}</td>
          </ng-container>
          <ng-container matColumnDef="repLegalNombre">
            <th mat-header-cell *matHeaderCellDef>Rep. legal</th>
            <td mat-cell *matCellDef="let c">{{ c.repLegalNombre }}</td>
          </ng-container>
          <ng-container matColumnDef="email">
            <th mat-header-cell *matHeaderCellDef>Email</th>
            <td mat-cell *matCellDef="let c">{{ c.email }}</td>
          </ng-container>
          <ng-container matColumnDef="telefono">
            <th mat-header-cell *matHeaderCellDef>Teléfono</th>
            <td mat-cell *matCellDef="let c">{{ c.telefono }}</td>
          </ng-container>
          <ng-container matColumnDef="acciones">
            <th mat-header-cell *matHeaderCellDef></th>
            <td mat-cell *matCellDef="let c" class="columna-acciones">
              <button mat-icon-button (click)="verFacturacion(c)" matTooltip="Ver facturación" style="color:var(--azul-600)">
                <mat-icon>receipt_long</mat-icon>
              </button>
              <button mat-icon-button (click)="abrirFormulario(c)" matTooltip="Editar">
                <mat-icon>edit</mat-icon>
              </button>
              <button mat-icon-button color="warn" (click)="eliminar(c)" matTooltip="Eliminar">
                <mat-icon>delete_outline</mat-icon>
              </button>
            </td>
          </ng-container>
          <tr mat-header-row *matHeaderRowDef="columnas"></tr>
          <tr mat-row *matRowDef="let fila; columns: columnas;"></tr>
        </table>
        @if (clientes().length === 0) {
          <div class="estado-vacio-tabla">
            <mat-icon>people</mat-icon><p>No hay clientes registrados.</p>
          </div>
        }
      </div>

      <!-- Panel de facturación -->
      @if (datosFacturacion()) {
        <div class="panel-facturacion superficie">
          <div class="encabezado-facturacion">
            <h3>Facturación — {{ datosFacturacion()?.razonSocial }}</h3>
            <button mat-icon-button (click)="datosFacturacion.set(null)">
              <mat-icon>close</mat-icon>
            </button>
          </div>
          <div class="cuadricula-kpi-mini">
            <div class="tarjeta-kpi">
              <div class="etiqueta-kpi">Total servicios</div>
              <div class="valor-kpi">{{ datosFacturacion()?.totalServicios }}</div>
            </div>
            <div class="tarjeta-kpi">
              <div class="etiqueta-kpi">Monto total</div>
              <div class="valor-kpi texto-primario">{{ datosFacturacion()?.montoTotal | currency:'CLP':'$':'1.0-0' }}</div>
            </div>
            <div class="tarjeta-kpi">
              <div class="etiqueta-kpi">Pendiente de cobro</div>
              <div class="valor-kpi" style="color:var(--color-peligro)">{{ datosFacturacion()?.pendienteCobro | currency:'CLP':'$':'1.0-0' }}</div>
            </div>
          </div>
        </div>
      }
    }

    @if (mostrarFormulario()) {
      <div class="capa-modal" (click)="cerrarFormulario()">
        <div class="panel-modal" (click)="$event.stopPropagation()">
          <h2>{{ idEdicion() ? 'Editar' : 'Nuevo' }} cliente</h2>
          <form [formGroup]="formulario" (ngSubmit)="guardar()">
            <div class="dos-columnas">
              <mat-form-field appearance="fill">
                <mat-label>RUT empresa</mat-label>
                <input matInput formControlName="rut" placeholder="76.123.456-7" />
              </mat-form-field>
              <mat-form-field appearance="fill">
                <mat-label>Razón social</mat-label>
                <input matInput formControlName="razonSocial" />
              </mat-form-field>
            </div>
            <mat-form-field appearance="fill" class="ancho-completo">
              <mat-label>Dirección</mat-label>
              <input matInput formControlName="direccion" />
            </mat-form-field>
            <div class="dos-columnas">
              <mat-form-field appearance="fill">
                <mat-label>Rep. legal</mat-label>
                <input matInput formControlName="repLegalNombre" />
              </mat-form-field>
              <mat-form-field appearance="fill">
                <mat-label>Teléfono</mat-label>
                <input matInput formControlName="telefono" />
              </mat-form-field>
            </div>
            <mat-form-field appearance="fill" class="ancho-completo">
              <mat-label>Email</mat-label>
              <input matInput type="email" formControlName="email" />
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
    .columna-acciones { white-space: nowrap; }
    .estado-vacio-tabla {
      display: flex; flex-direction: column; align-items: center; padding: 48px;
      gap: 8px; color: var(--color-texto-3);
      mat-icon { font-size: 48px; width: 48px; height: 48px; opacity: .3; }
    }
    .panel-facturacion { margin-top: 20px; }
    .encabezado-facturacion {
      display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px;
      h3 { font-size: 16px; font-weight: 600; color: var(--azul-900); }
    }
    .cuadricula-kpi-mini {
      display: grid; grid-template-columns: repeat(3,1fr); gap: 16px;
    }
  `],
})
export class ClientesComponent implements OnInit {
  private readonly servicio     = inject(OperacionesService);
  private readonly constructor_ = inject(FormBuilder);
  private readonly notificacion = inject(MatSnackBar);
  private readonly dialogo      = inject(DialogoService);

  columnas          = ['rut', 'razonSocial', 'repLegalNombre', 'email', 'telefono', 'acciones'];
  cargando          = signal(true);
  guardando         = signal(false);
  mostrarFormulario = signal(false);
  idEdicion         = signal<string | null>(null);
  clientes          = signal<Cliente[]>([]);
  datosFacturacion  = signal<any | null>(null);
  busqueda          = '';

  formulario = this.constructor_.group({
    rut:            ['', Validators.required],
    razonSocial:    ['', Validators.required],
    direccion:      [''],
    repLegalNombre: [''],
    telefono:       [''],
    email:          ['', Validators.email],
  });

  ngOnInit() {
    this.cargar();
    // RUT y razón social en mayúsculas para consistencia de datos
    (['rut', 'razonSocial', 'repLegalNombre'] as const).forEach(campo => {
      this.formulario.get(campo)?.valueChanges.subscribe(v => {
        if (v && v !== v.toUpperCase()) {
          this.formulario.get(campo)?.setValue(v.toUpperCase(), { emitEvent: false });
        }
      });
    });
  }

  cargar() {
    this.cargando.set(true);
    this.servicio.getClientes(this.busqueda || undefined).subscribe({
      next: r => { this.clientes.set(r.content); this.cargando.set(false); },
      error: () => this.cargando.set(false),
    });
  }

  abrirFormulario(c?: Cliente) {
    this.idEdicion.set(c?.id ?? null);
    if (c) this.formulario.patchValue(c as any); else this.formulario.reset();
    this.mostrarFormulario.set(true);
  }

  cerrarFormulario() { this.mostrarFormulario.set(false); }

  guardar() {
    if (this.formulario.invalid) return;
    this.guardando.set(true);
    const solicitud = this.formulario.value as any;
    const operacion = this.idEdicion()
      ? this.servicio.updateCliente(this.idEdicion()!, solicitud)
      : this.servicio.createCliente(solicitud);
    operacion.subscribe({
      next: () => { this.guardando.set(false); this.cerrarFormulario(); this.cargar(); this.notificacion.open('Guardado', '', { duration: 3000 }); },
      error: () => { this.guardando.set(false); this.notificacion.open('Error', '', { duration: 3000 }); },
    });
  }

  verFacturacion(c: Cliente) {
    this.servicio.getFacturacionCliente(c.id).subscribe(f =>
      this.datosFacturacion.set({ ...f, razonSocial: c.razonSocial })
    );
  }

  async eliminar(c: Cliente) {
    const ok = await this.dialogo.confirmarEliminar(
      `¿Eliminar cliente ${c.razonSocial}?`,
      `RUT: ${c.rut}`
    );
    if (!ok) return;
    this.servicio.deleteCliente(c.id).subscribe({ next: () => this.cargar() });
  }
}
