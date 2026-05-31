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
import { MatCheckboxModule } from '@angular/material/checkbox';
import { OperacionesService } from '@core/services/operaciones.service';
import { VehiculosService } from '@core/services/vehiculos.service';
import { ConductoresService } from '@core/services/conductores.service';
import { DialogoService } from '@core/services/dialogo.service';
import { Servicio, Vehiculo, Conductor, Cliente, TipoServicio } from '@core/models';

@Component({
  selector: 'app-servicios',
  standalone: true,
  imports: [
    CommonModule, FormsModule, ReactiveFormsModule,
    MatTableModule, MatButtonModule, MatIconModule,
    MatInputModule, MatSelectModule, MatProgressSpinnerModule,
    MatSnackBarModule, MatTooltipModule, MatCheckboxModule,
  ],
  template: `
    <div class="encabezado-pagina">
      <div>
        <h1>Solicitudes de Servicio</h1>
        <p class="subtitulo-pagina">Registro y seguimiento de órdenes de transporte</p>
      </div>
      <button mat-flat-button class="btn-principal" (click)="abrirFormulario()">
        <mat-icon>add</mat-icon> Nueva solicitud
      </button>
    </div>

    <!-- Filtros -->
    <div class="barra-filtros">
      <mat-form-field appearance="fill">
        <mat-label>Estado</mat-label>
        <mat-select [(ngModel)]="filtroEstado" (ngModelChange)="cargar()">
          <mat-option value="">Todos</mat-option>
          <mat-option value="BORRADOR">Borrador</mat-option>
          <mat-option value="PENDIENTE">Pendiente</mat-option>
          <mat-option value="APROBADO">Aprobado</mat-option>
          <mat-option value="EN_CURSO">En curso</mat-option>
          <mat-option value="COMPLETADO">Completado</mat-option>
          <mat-option value="CANCELADO">Cancelado</mat-option>
        </mat-select>
      </mat-form-field>

      <mat-form-field appearance="fill">
        <mat-label>Cliente</mat-label>
        <mat-select [(ngModel)]="filtroClienteId" (ngModelChange)="cargar()">
          <mat-option value="">Todos</mat-option>
          @for (c of clientes(); track c.id) {
            <mat-option [value]="c.id">{{ c.razonSocial }}</mat-option>
          }
        </mat-select>
      </mat-form-field>

      <mat-form-field appearance="fill">
        <mat-label>Desde</mat-label>
        <input matInput type="date" [(ngModel)]="filtroDesde" (ngModelChange)="cargar()" />
      </mat-form-field>

      <mat-form-field appearance="fill">
        <mat-label>Hasta</mat-label>
        <input matInput type="date" [(ngModel)]="filtroHasta" (ngModelChange)="cargar()" />
      </mat-form-field>
    </div>

    @if (cargando()) {
      <div class="spinner-centrado"><mat-spinner diameter="40" /></div>
    } @else {
      <div class="superficie" style="padding:0;overflow:hidden">
        <table mat-table [dataSource]="servicios()">

          <ng-container matColumnDef="numero">
            <th mat-header-cell *matHeaderCellDef>#</th>
            <td mat-cell *matCellDef="let s">
              <strong class="texto-primario">{{ s.numServicio }}</strong>
            </td>
          </ng-container>

          <ng-container matColumnDef="cliente">
            <th mat-header-cell *matHeaderCellDef>Cliente</th>
            <td mat-cell *matCellDef="let s">
              <span style="font-weight:500">{{ s.clienteRazonSocial ?? nombreCliente(s.clienteId) }}</span>
            </td>
          </ng-container>

          <ng-container matColumnDef="tipoServicio">
            <th mat-header-cell *matHeaderCellDef>Tipo</th>
            <td mat-cell *matCellDef="let s">
              <span class="texto-atenuado" style="font-size:13px">{{ nombreTipo(s.tipoServicioId) }}</span>
            </td>
          </ng-container>

          <ng-container matColumnDef="origen">
            <th mat-header-cell *matHeaderCellDef>Origen → Destino</th>
            <td mat-cell *matCellDef="let s">
              {{ s.origen }}
              <mat-icon style="font-size:14px;vertical-align:middle;color:var(--color-texto-3)">arrow_forward</mat-icon>
              {{ s.destino }}
              @if (s.idaVuelta === 1) {
                <mat-icon style="font-size:14px;vertical-align:middle;color:var(--azul-500)" matTooltip="Ida y vuelta">sync_alt</mat-icon>
              }
            </td>
          </ng-container>

          <ng-container matColumnDef="asignacion">
            <th mat-header-cell *matHeaderCellDef>Vehículo / Conductor</th>
            <td mat-cell *matCellDef="let s" class="celda-asignacion">
              @if (s.vehiculoId) {
                <span class="chip-asignacion"><mat-icon style="font-size:14px">directions_bus</mat-icon> {{ patenteVehiculo(s.vehiculoId) }}</span>
              }
              @if (s.conductorId) {
                <span class="chip-asignacion"><mat-icon style="font-size:14px">person</mat-icon> {{ nombreConductor(s.conductorId) }}</span>
              }
              @if (!s.vehiculoId && !s.conductorId) {
                <span class="texto-atenuado" style="font-size:12px">Sin asignar</span>
              }
            </td>
          </ng-container>

          <ng-container matColumnDef="fecha">
            <th mat-header-cell *matHeaderCellDef>Fecha</th>
            <td mat-cell *matCellDef="let s">{{ s.fechaServicio | date:'dd/MM/yyyy' }}</td>
          </ng-container>

          <ng-container matColumnDef="estado">
            <th mat-header-cell *matHeaderCellDef>Estado</th>
            <td mat-cell *matCellDef="let s">
              <span [class]="insigniaEstado(s.estado)">{{ etiquetaEstado(s.estado) }}</span>
            </td>
          </ng-container>

          <ng-container matColumnDef="valorTotal">
            <th mat-header-cell *matHeaderCellDef>Total</th>
            <td mat-cell *matCellDef="let s">
              <strong>{{ s.valorTotal | currency:'CLP':'$':'1.0-0' }}</strong>
            </td>
          </ng-container>

          <ng-container matColumnDef="acciones">
            <th mat-header-cell *matHeaderCellDef></th>
            <td mat-cell *matCellDef="let s" class="columna-acciones">

              <!-- Aprobar: BORRADOR o PENDIENTE -->
              @if (s.estado === 'BORRADOR' || s.estado === 'PENDIENTE') {
                <button mat-icon-button matTooltip="Aprobar servicio"
                        style="color:#007AF5" (click)="aprobar(s)"
                        [disabled]="accionando() === s.id">
                  <mat-icon>thumb_up</mat-icon>
                </button>
              }

              <!-- Asignar: APROBADO -->
              @if (s.estado === 'APROBADO') {
                <button mat-icon-button matTooltip="Asignar vehículo y conductor"
                        style="color:#C25E01" (click)="abrirAsignacion(s)"
                        [disabled]="accionando() === s.id">
                  <mat-icon>assignment_ind</mat-icon>
                </button>
              }

              <!-- Iniciar: APROBADO (con asignación) -->
              @if (s.estado === 'APROBADO') {
                <button mat-icon-button matTooltip="Iniciar viaje"
                        style="color:#1B2C40" (click)="iniciar(s)"
                        [disabled]="accionando() === s.id">
                  <mat-icon>play_circle</mat-icon>
                </button>
              }

              <!-- Completar: EN_CURSO -->
              @if (s.estado === 'EN_CURSO') {
                <button mat-icon-button matTooltip="Marcar como completado"
                        style="color:#16a34a" (click)="completar(s)"
                        [disabled]="accionando() === s.id">
                  <mat-icon>check_circle</mat-icon>
                </button>
              }

              <!-- Cancelar: estados no terminales -->
              @if (['BORRADOR','PENDIENTE','APROBADO','EN_CURSO'].includes(s.estado)) {
                <button mat-icon-button matTooltip="Cancelar"
                        style="color:#C10A5A" (click)="cancelar(s)"
                        [disabled]="accionando() === s.id">
                  <mat-icon>cancel</mat-icon>
                </button>
              }

              <!-- Editar siempre -->
              <button mat-icon-button matTooltip="Editar" (click)="abrirFormulario(s)"
                      [disabled]="accionando() === s.id">
                <mat-icon>edit</mat-icon>
              </button>

              <!-- Eliminar siempre -->
              <button mat-icon-button matTooltip="Eliminar" color="warn" (click)="eliminar(s)"
                      [disabled]="accionando() === s.id">
                <mat-icon>delete_outline</mat-icon>
              </button>

              @if (accionando() === s.id) {
                <mat-spinner diameter="20" style="display:inline-block;vertical-align:middle" />
              }
            </td>
          </ng-container>

          <tr mat-header-row *matHeaderRowDef="columnas"></tr>
          <tr mat-row *matRowDef="let fila; columns: columnas;"></tr>
        </table>

        @if (servicios().length === 0) {
          <div class="estado-vacio-tabla">
            <mat-icon>local_shipping</mat-icon>
            <p>No hay solicitudes de servicio registradas.</p>
          </div>
        }
      </div>
    }

    <!-- Modal: Asignación de vehículo y conductor -->
    @if (mostrarAsignacion()) {
      <div class="capa-modal" (click)="cerrarAsignacion()">
        <div class="panel-modal" style="width:420px" (click)="$event.stopPropagation()">
          <h2>Asignar vehículo y conductor</h2>
          <p class="subtitulo-pagina" style="margin-bottom:16px">
            Servicio <strong>{{ servicioAsignando()?.numServicio }}</strong>
          </p>

          <form [formGroup]="frmAsignacion" (ngSubmit)="guardarAsignacion()">
            <mat-form-field appearance="fill" class="ancho-completo">
              <mat-label>Vehículo</mat-label>
              <mat-select formControlName="vehiculoId">
                <mat-option value="">Sin asignar</mat-option>
                @for (v of vehiculos(); track v.id) {
                  <mat-option [value]="v.id">{{ v.patente }} — {{ v.marca }} {{ v.modelo }}</mat-option>
                }
              </mat-select>
            </mat-form-field>

            <mat-form-field appearance="fill" class="ancho-completo">
              <mat-label>Conductor</mat-label>
              <mat-select formControlName="conductorId">
                <mat-option value="">Sin asignar</mat-option>
                @for (c of conductores(); track c.id) {
                  <mat-option [value]="c.id">{{ c.nombre }}</mat-option>
                }
              </mat-select>
            </mat-form-field>

            <div class="acciones-formulario">
              <button mat-button type="button" (click)="cerrarAsignacion()">Cancelar</button>
              <button mat-flat-button style="background:#C25E01;color:#fff" type="submit"
                      [disabled]="guardando()">
                {{ guardando() ? 'Guardando…' : 'Asignar' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    }

    <!-- Modal formulario -->
    @if (mostrarFormulario()) {
      <div class="capa-modal" (click)="cerrarFormulario()">
        <div class="panel-modal" style="width:640px;max-height:90vh;overflow-y:auto" (click)="$event.stopPropagation()">
          <h2>{{ idEdicion() ? 'Editar' : 'Nueva' }} Solicitud de Servicio</h2>

          <form [formGroup]="formulario" (ngSubmit)="guardar()">

            <!-- Sección: Cliente y Tipo -->
            <div class="seccion-form">
              <h3 class="titulo-seccion">Datos del servicio</h3>
              <div class="dos-columnas">
                <mat-form-field appearance="fill">
                  <mat-label>Cliente *</mat-label>
                  <mat-select formControlName="clienteId">
                    @for (c of clientes(); track c.id) {
                      <mat-option [value]="c.id">{{ c.razonSocial }}</mat-option>
                    }
                  </mat-select>
                </mat-form-field>

                <mat-form-field appearance="fill">
                  <mat-label>Tipo de servicio</mat-label>
                  <mat-select formControlName="tipoServicioId">
                    <mat-option value="">Sin especificar</mat-option>
                    @for (t of tiposServicio(); track t.id) {
                      <mat-option [value]="t.id">{{ t.nombre }}</mat-option>
                    }
                  </mat-select>
                </mat-form-field>
              </div>
            </div>

            <!-- Sección: Ruta -->
            <div class="seccion-form">
              <h3 class="titulo-seccion">Ruta</h3>
              <div class="dos-columnas">
                <mat-form-field appearance="fill">
                  <mat-label>Origen *</mat-label>
                  <input matInput formControlName="origen" placeholder="Ciudad, dirección..." />
                </mat-form-field>

                <mat-form-field appearance="fill">
                  <mat-label>Destino *</mat-label>
                  <input matInput formControlName="destino" placeholder="Ciudad, dirección..." />
                </mat-form-field>
              </div>

              <div class="dos-columnas">
                <mat-form-field appearance="fill">
                  <mat-label>Km recorridos</mat-label>
                  <input matInput type="number" formControlName="kmsRecorrido" />
                </mat-form-field>

                <div class="campo-checkbox">
                  <mat-checkbox formControlName="idaVuelta">Ida y vuelta</mat-checkbox>
                </div>
              </div>
            </div>

            <!-- Sección: Fechas y Estado -->
            <div class="seccion-form">
              <h3 class="titulo-seccion">Fechas y estado</h3>
              <div class="dos-columnas">
                <mat-form-field appearance="fill">
                  <mat-label>Fecha de servicio *</mat-label>
                  <input matInput type="date" formControlName="fechaServicio" />
                </mat-form-field>

                <mat-form-field appearance="fill">
                  <mat-label>Fecha de término</mat-label>
                  <input matInput type="date" formControlName="fechaTermino" />
                </mat-form-field>
              </div>

              <mat-form-field appearance="fill" class="ancho-completo">
                <mat-label>Estado</mat-label>
                <mat-select formControlName="estado">
                  <mat-option value="BORRADOR">Borrador</mat-option>
                  <mat-option value="PENDIENTE">Pendiente</mat-option>
                  <mat-option value="APROBADO">Aprobado</mat-option>
                  <mat-option value="EN_CURSO">En curso</mat-option>
                  <mat-option value="COMPLETADO">Completado</mat-option>
                  <mat-option value="CANCELADO">Cancelado</mat-option>
                </mat-select>
              </mat-form-field>
            </div>

            <!-- Sección: Valores -->
            <div class="seccion-form">
              <h3 class="titulo-seccion">Valores</h3>
              <div class="dos-columnas">
                <mat-form-field appearance="fill">
                  <mat-label>Valor neto (CLP) *</mat-label>
                  <input matInput type="number" formControlName="valorNeto" />
                  <span matPrefix>$&nbsp;</span>
                </mat-form-field>

                <mat-form-field appearance="fill">
                  <mat-label>Tipo de documento</mat-label>
                  <mat-select formControlName="tipoDocumento">
                    <mat-option value="FACTURA">Factura</mat-option>
                    <mat-option value="BOLETA">Boleta</mat-option>
                    <mat-option value="GUIA_DESPACHO">Guía de despacho</mat-option>
                  </mat-select>
                </mat-form-field>
              </div>
            </div>

            <!-- Notas -->
            <mat-form-field appearance="fill" class="ancho-completo">
              <mat-label>Notas</mat-label>
              <textarea matInput rows="3" formControlName="notas" placeholder="Observaciones, instrucciones especiales..."></textarea>
            </mat-form-field>

            <div class="acciones-formulario">
              <button mat-button type="button" (click)="cerrarFormulario()">Cancelar</button>
              <button mat-flat-button class="btn-principal" type="submit"
                      [disabled]="formulario.invalid || guardando()">
                {{ guardando() ? 'Guardando…' : (idEdicion() ? 'Actualizar' : 'Crear solicitud') }}
              </button>
            </div>
          </form>
        </div>
      </div>
    }
  `,
  styles: [`
    .subtitulo-pagina { font-size: 13px; color: var(--color-texto-3); margin: 2px 0 0; }
    .columna-acciones { white-space: nowrap; text-align: right; }
    .celda-asignacion { font-size: 13px; }
    .chip-asignacion {
      display: inline-flex; align-items: center; gap: 3px;
      background: var(--color-superficie-2, #f0f4f8);
      border-radius: 4px; padding: 2px 6px; margin-right: 4px;
      font-size: 12px; color: var(--color-texto-2);
      mat-icon { font-size: 14px; width: 14px; height: 14px; }
    }
    .barra-filtros {
      display: flex; flex-wrap: wrap; gap: 12px; padding: 16px 0;
      mat-form-field { min-width: 150px; }
    }
    .estado-vacio-tabla {
      display: flex; flex-direction: column; align-items: center;
      padding: 48px; gap: 8px; color: var(--color-texto-3);
      mat-icon { font-size: 48px; width: 48px; height: 48px; opacity: .3; }
    }
    .seccion-form { margin-bottom: 8px; }
    .titulo-seccion {
      font-size: 12px; font-weight: 700; text-transform: uppercase;
      letter-spacing: .8px; color: var(--color-texto-3);
      margin: 16px 0 8px; padding-bottom: 6px;
      border-bottom: 1px solid var(--color-borde);
    }
    .campo-checkbox {
      display: flex; align-items: center; padding-top: 18px;
      mat-checkbox { font-size: 14px; }
    }
  `],
})
export class ServiciosComponent implements OnInit {
  private readonly svcOperaciones    = inject(OperacionesService);
  private readonly svcVehiculos      = inject(VehiculosService);
  private readonly svcConductores    = inject(ConductoresService);
  private readonly fb                = inject(FormBuilder);
  private readonly snack             = inject(MatSnackBar);
  private readonly dialogo           = inject(DialogoService);

  columnas          = ['numero', 'cliente', 'tipoServicio', 'origen', 'asignacion', 'fecha', 'estado', 'valorTotal', 'acciones'];
  cargando          = signal(true);
  guardando         = signal(false);
  accionando        = signal<string | null>(null);
  mostrarFormulario = signal(false);
  mostrarAsignacion = signal(false);
  idEdicion         = signal<string | null>(null);
  servicioAsignando = signal<Servicio | null>(null);
  servicios         = signal<Servicio[]>([]);
  vehiculos         = signal<Vehiculo[]>([]);
  conductores       = signal<Conductor[]>([]);
  clientes          = signal<Cliente[]>([]);
  tiposServicio     = signal<TipoServicio[]>([]);

  filtroEstado    = '';
  filtroClienteId = '';
  filtroDesde     = '';
  filtroHasta     = '';

  formulario = this.fb.group({
    clienteId:      ['', Validators.required],
    tipoServicioId: [''],
    origen:         ['', Validators.required],
    destino:        ['', Validators.required],
    kmsRecorrido:   [null as number | null],
    idaVuelta:      [false],
    fechaServicio:  ['', Validators.required],
    fechaTermino:   [''],
    estado:         ['BORRADOR'],
    valorNeto:      [0, [Validators.required, Validators.min(0)]],
    tipoDocumento:  ['FACTURA'],
    notas:          [''],
  });

  frmAsignacion = this.fb.group({
    vehiculoId:  [''],
    conductorId: [''],
  });

  ngOnInit() {
    this.svcVehiculos.getAll({ size: 200 }).subscribe(r => this.vehiculos.set(r.content));
    this.svcConductores.getAll({ size: 200 } as any).subscribe(r => this.conductores.set(r.content));
    this.svcOperaciones.getClientes().subscribe(r => this.clientes.set(r.content));
    this.svcOperaciones.getTiposServicio().subscribe(ts => this.tiposServicio.set(ts));
    this.cargar();
  }

  cargar() {
    this.cargando.set(true);
    this.svcOperaciones.getServicios({
      estado:    this.filtroEstado    || undefined,
      clienteId: this.filtroClienteId || undefined,
      desde:     this.filtroDesde     || undefined,
      hasta:     this.filtroHasta     || undefined,
      tamano:    100,
    }).subscribe({
      next: r => { this.servicios.set(r.content); this.cargando.set(false); },
      error: () => this.cargando.set(false),
    });
  }

  // ── Formulario principal ───────────────────────────────────

  abrirFormulario(s?: Servicio) {
    this.idEdicion.set(s?.id ?? null);
    if (s) {
      this.formulario.patchValue({
        ...s as any,
        idaVuelta: s.idaVuelta === 1,
      });
    } else {
      this.formulario.reset({
        estado: 'BORRADOR',
        tipoDocumento: 'FACTURA',
        valorNeto: 0,
        idaVuelta: false,
      });
    }
    this.mostrarFormulario.set(true);
  }

  cerrarFormulario() { this.mostrarFormulario.set(false); }

  guardar() {
    if (this.formulario.invalid) return;
    this.guardando.set(true);
    const v = this.formulario.value;
    const solicitud = {
      ...v,
      idaVuelta: v.idaVuelta ? 1 : 0,
      fechaTermino: v.fechaTermino || undefined,
      tipoServicioId: v.tipoServicioId || undefined,
    } as any;

    const op = this.idEdicion()
      ? this.svcOperaciones.updateServicio(this.idEdicion()!, solicitud)
      : this.svcOperaciones.createServicio(solicitud);

    op.subscribe({
      next: () => {
        this.guardando.set(false);
        this.cerrarFormulario();
        this.cargar();
        this.snack.open('Solicitud guardada correctamente', '', { duration: 3000 });
      },
      error: (err) => {
        this.guardando.set(false);
        const msg = err?.error?.message ?? 'Error al guardar';
        this.snack.open(msg, 'Cerrar', { duration: 4000 });
      },
    });
  }

  // ── Panel de asignación ────────────────────────────────────

  abrirAsignacion(s: Servicio) {
    this.servicioAsignando.set(s);
    this.frmAsignacion.patchValue({
      vehiculoId:  s.vehiculoId  ?? '',
      conductorId: s.conductorId ?? '',
    });
    this.mostrarAsignacion.set(true);
  }

  cerrarAsignacion() { this.mostrarAsignacion.set(false); this.servicioAsignando.set(null); }

  guardarAsignacion() {
    const s = this.servicioAsignando();
    if (!s) return;
    this.guardando.set(true);
    const v = this.frmAsignacion.value;
    this.svcOperaciones.asignarServicio(s.id, {
      vehiculoId:  v.vehiculoId  || undefined,
      conductorId: v.conductorId || undefined,
    }).subscribe({
      next: (actualizado) => {
        this.servicios.update(lista => lista.map(x => x.id === actualizado.id ? actualizado : x));
        this.guardando.set(false);
        this.cerrarAsignacion();
        this.snack.open('Asignación guardada', '', { duration: 3000 });
      },
      error: (err) => {
        this.guardando.set(false);
        this.snack.open(err?.error?.message ?? 'Error al asignar', 'Cerrar', { duration: 4000 });
      },
    });
  }

  // ── Acciones de estado ─────────────────────────────────────

  aprobar(s: Servicio) {
    this.ejecutarAccion(s.id, this.svcOperaciones.aprobarServicio(s.id), 'Servicio aprobado');
  }

  iniciar(s: Servicio) {
    this.ejecutarAccion(s.id, this.svcOperaciones.iniciarServicio(s.id), 'Viaje iniciado');
  }

  completar(s: Servicio) {
    this.ejecutarAccion(s.id, this.svcOperaciones.completarServicio(s.id), 'Viaje completado');
  }

  async cancelar(s: Servicio) {
    const ok = await this.dialogo.confirmar(
      `¿Cancelar la solicitud ${s.numServicio}?`,
      `${s.origen} → ${s.destino}`,
      'Sí, cancelar'
    );
    if (!ok) return;
    this.ejecutarAccion(s.id, this.svcOperaciones.cancelarServicio(s.id), 'Servicio cancelado');
  }

  private ejecutarAccion(id: string, op: any, mensaje: string) {
    this.accionando.set(id);
    op.subscribe({
      next: (actualizado: Servicio) => {
        this.servicios.update(lista => lista.map(x => x.id === actualizado.id ? actualizado : x));
        this.accionando.set(null);
        this.snack.open(mensaje, '', { duration: 3000 });
      },
      error: (err: any) => {
        this.accionando.set(null);
        this.snack.open(err?.error?.message ?? 'Error al ejecutar acción', 'Cerrar', { duration: 4000 });
      },
    });
  }

  // ── Eliminar ───────────────────────────────────────────────

  async eliminar(s: Servicio) {
    const ok = await this.dialogo.confirmarEliminar(
      `¿Eliminar la solicitud ${s.numServicio}?`,
      `${s.origen} → ${s.destino}`
    );
    if (!ok) return;
    this.svcOperaciones.deleteServicio(s.id).subscribe({ next: () => this.cargar() });
  }

  // ── Helpers ────────────────────────────────────────────────

  nombreCliente(clienteId: string): string {
    return this.clientes().find(c => c.id === clienteId)?.razonSocial ?? '';
  }

  nombreTipo(tipoId?: string): string {
    if (!tipoId) return '';
    return this.tiposServicio().find(t => t.id === tipoId)?.nombre ?? '';
  }

  patenteVehiculo(vehiculoId?: string): string {
    if (!vehiculoId) return '';
    const v = this.vehiculos().find(x => x.id === vehiculoId);
    return v ? v.patente : vehiculoId.slice(0, 8);
  }

  nombreConductor(conductorId?: string): string {
    if (!conductorId) return '';
    const c = this.conductores().find(x => x.id === conductorId);
    return c ? c.nombre : conductorId.slice(0, 8);
  }

  etiquetaEstado(estado: string): string {
    const mapa: Record<string, string> = {
      BORRADOR:   'Borrador',
      PENDIENTE:  'Pendiente',
      APROBADO:   'Aprobado',
      EN_CURSO:   'En curso',
      COMPLETADO: 'Completado',
      CANCELADO:  'Cancelado',
    };
    return mapa[estado] ?? estado;
  }

  insigniaEstado(estado: string): string {
    const mapa: Record<string, string> = {
      BORRADOR:   'insignia insignia-neutro',
      PENDIENTE:  'insignia insignia-info',
      APROBADO:   'insignia insignia-aprobado',
      EN_CURSO:   'insignia insignia-azul',
      COMPLETADO: 'insignia insignia-exito',
      CANCELADO:  'insignia insignia-peligro',
    };
    return mapa[estado] ?? 'insignia insignia-info';
  }
}
