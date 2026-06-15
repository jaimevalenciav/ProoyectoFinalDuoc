import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule, CurrencyPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatTabsModule } from '@angular/material/tabs';
import { MatChipsModule } from '@angular/material/chips';
import { OperacionesService } from '@core/services/operaciones.service';
import { FacturacionService } from '@core/services/facturacion.service';
import { DialogoService } from '@core/services/dialogo.service';
import { Servicio, Cliente, Factura } from '@core/models';

@Component({
  selector: 'app-facturacion',
  standalone: true,
  imports: [
    CommonModule, FormsModule, CurrencyPipe,
    MatTableModule, MatButtonModule, MatIconModule,
    MatInputModule, MatSelectModule, MatCheckboxModule,
    MatProgressSpinnerModule, MatSnackBarModule, MatTooltipModule,
    MatDatepickerModule, MatNativeDateModule, MatTabsModule, MatChipsModule,
  ],
  template: `
    <div class="encabezado-pagina">
      <h1>Facturación</h1>
      <div class="acciones-encabezado">
        <span class="texto-atenuado" style="font-size:13px">
          {{ facturas().length }} facturas emitidas
        </span>
      </div>
    </div>

    <!-- Tabs: Servicios pendientes / Facturas emitidas -->
    <div class="superficie tabs-contenedor">
      <div class="tabs-manual">
        <button class="tab-btn" [class.activo]="pestanaActiva() === 'servicios'" (click)="pestanaActiva.set('servicios')">
          <mat-icon>local_shipping</mat-icon> Servicios
        </button>
        <button class="tab-btn" [class.activo]="pestanaActiva() === 'facturas'" (click)="pestanaActiva.set('facturas')">
          <mat-icon>receipt</mat-icon> Facturas emitidas
        </button>
      </div>
    </div>

    <!-- ═══════════════════ TAB SERVICIOS ═══════════════════ -->
    @if (pestanaActiva() === 'servicios') {

      <!-- Filtros -->
      <div class="barra-filtros">
        <mat-form-field appearance="fill">
          <mat-label>Cliente</mat-label>
          <mat-select [(ngModel)]="filtroClienteId" (ngModelChange)="cargarServicios()">
            <mat-option value="">Todos</mat-option>
            @for (c of clientes(); track c.id) {
              <mat-option [value]="c.id">{{ c.razonSocial }}</mat-option>
            }
          </mat-select>
        </mat-form-field>

        <mat-form-field appearance="fill">
          <mat-label>Estado</mat-label>
          <mat-select [(ngModel)]="filtroEstado" (ngModelChange)="cargarServicios()">
            <mat-option value="">Todos</mat-option>
            <mat-option value="PENDIENTE">Pendiente</mat-option>
            <mat-option value="EN_CURSO">En curso</mat-option>
            <mat-option value="COMPLETADO">Completado</mat-option>
            <mat-option value="CANCELADO">Cancelado</mat-option>
          </mat-select>
        </mat-form-field>

        <mat-form-field appearance="fill">
          <mat-label>Facturado</mat-label>
          <mat-select [(ngModel)]="filtroFacturado" (ngModelChange)="cargarServicios()">
            <mat-option value="">Todos</mat-option>
            <mat-option value="0">Pendiente de facturar</mat-option>
            <mat-option value="1">Ya facturados</mat-option>
          </mat-select>
        </mat-form-field>

        <mat-form-field appearance="fill">
          <mat-label>Desde</mat-label>
          <input matInput type="date" [(ngModel)]="filtroDesde" (ngModelChange)="cargarServicios()" />
        </mat-form-field>

        <mat-form-field appearance="fill">
          <mat-label>Hasta</mat-label>
          <input matInput type="date" [(ngModel)]="filtroHasta" (ngModelChange)="cargarServicios()" />
        </mat-form-field>

        <button mat-stroked-button (click)="limpiarFiltros()">
          <mat-icon>filter_alt_off</mat-icon> Limpiar
        </button>
      </div>

      @if (cargandoServicios()) {
        <div class="spinner-centrado"><mat-spinner diameter="40" /></div>
      } @else {
        <div class="superficie" style="padding:0;overflow:hidden">
          <table mat-table [dataSource]="servicios()">

            <!-- Checkbox -->
            <ng-container matColumnDef="seleccion">
              <th mat-header-cell *matHeaderCellDef style="width:48px">
                <mat-checkbox
                  [checked]="todosSeleccionados()"
                  [indeterminate]="algunosSeleccionados()"
                  (change)="toggleTodos($event.checked)"
                />
              </th>
              <td mat-cell *matCellDef="let s" style="width:48px">
                <mat-checkbox
                  [checked]="estaSeleccionado(s.id)"
                  [disabled]="s.facturado === 1"
                  (change)="toggleSeleccion(s)"
                />
              </td>
            </ng-container>

            <!-- Número -->
            <ng-container matColumnDef="numero">
              <th mat-header-cell *matHeaderCellDef>#</th>
              <td mat-cell *matCellDef="let s">
                <strong class="texto-primario">{{ s.numServicio }}</strong>
              </td>
            </ng-container>

            <!-- Cliente -->
            <ng-container matColumnDef="cliente">
              <th mat-header-cell *matHeaderCellDef>Cliente</th>
              <td mat-cell *matCellDef="let s">
                <span class="nombre-cliente">{{ nombreCliente(s.clienteId) }}</span>
              </td>
            </ng-container>

            <!-- Ruta -->
            <ng-container matColumnDef="ruta">
              <th mat-header-cell *matHeaderCellDef>Origen → Destino</th>
              <td mat-cell *matCellDef="let s">
                {{ s.origen }}
                <mat-icon style="font-size:14px;vertical-align:middle;color:var(--color-texto-3)">arrow_forward</mat-icon>
                {{ s.destino }}
              </td>
            </ng-container>

            <!-- Fecha -->
            <ng-container matColumnDef="fecha">
              <th mat-header-cell *matHeaderCellDef>Fecha servicio</th>
              <td mat-cell *matCellDef="let s">{{ s.fechaServicio | date:'dd/MM/yyyy' }}</td>
            </ng-container>

            <!-- Valor -->
            <ng-container matColumnDef="valor">
              <th mat-header-cell *matHeaderCellDef>Valor total</th>
              <td mat-cell *matCellDef="let s">
                <strong>{{ s.valorTotal | currency:'CLP':'$':'1.0-0' }}</strong>
              </td>
            </ng-container>

            <!-- Estado -->
            <ng-container matColumnDef="estado">
              <th mat-header-cell *matHeaderCellDef>Estado</th>
              <td mat-cell *matCellDef="let s">
                <span [class]="insigniaEstado(s.estado)">{{ s.estado }}</span>
              </td>
            </ng-container>

            <!-- Facturado -->
            <ng-container matColumnDef="facturado">
              <th mat-header-cell *matHeaderCellDef>Facturación</th>
              <td mat-cell *matCellDef="let s">
                @if (s.facturado === 1) {
                  <span class="insignia insignia-exito">
                    <mat-icon style="font-size:12px;height:12px;width:12px;vertical-align:middle">check_circle</mat-icon>
                    Facturado
                  </span>
                } @else {
                  <span class="insignia insignia-advertencia">Pendiente</span>
                }
              </td>
            </ng-container>

            <tr mat-header-row *matHeaderRowDef="columnasServicios"></tr>
            <tr mat-row *matRowDef="let fila; columns: columnasServicios;"
                [class.fila-seleccionada]="estaSeleccionado(fila.id)"
                [class.fila-facturada]="fila.facturado === 1"></tr>
          </table>

          @if (servicios().length === 0) {
            <div class="estado-vacio-tabla">
              <mat-icon>receipt_long</mat-icon>
              <p>No hay servicios que coincidan con los filtros.</p>
            </div>
          }
        </div>
      }

      <!-- Panel flotante de selección -->
      @if (seleccionados().length > 0) {
        <div class="panel-flotante">
          <div class="info-seleccion">
            <mat-icon>check_box</mat-icon>
            <span><strong>{{ seleccionados().length }}</strong> servicio{{ seleccionados().length !== 1 ? 's' : '' }} seleccionado{{ seleccionados().length !== 1 ? 's' : '' }}</span>
            <span class="separador">|</span>
            <span class="total-seleccion">Total: <strong>{{ totalSeleccionado() | currency:'CLP':'$':'1.0-0' }}</strong></span>
          </div>

          @if (!mismoCiente()) {
            <span class="aviso-cliente">
              <mat-icon style="font-size:16px;vertical-align:middle">warning</mat-icon>
              Selecciona servicios del mismo cliente
            </span>
          }

          <div class="acciones-panel">
            <button mat-stroked-button (click)="limpiarSeleccion()">Cancelar</button>
            <button mat-flat-button class="btn-principal"
                    [disabled]="!mismoCiente() || facturando()"
                    (click)="abrirDialogoFacturar()">
              <mat-icon>receipt</mat-icon>
              {{ facturando() ? 'Facturando…' : 'Facturar seleccionados' }}
            </button>
          </div>
        </div>
      }
    }

    <!-- ═══════════════════ TAB FACTURAS ═══════════════════ -->
    @if (pestanaActiva() === 'facturas') {

      <div class="barra-filtros">
        <mat-form-field appearance="fill">
          <mat-label>Cliente</mat-label>
          <mat-select [(ngModel)]="filtroFacturasClienteId" (ngModelChange)="cargarFacturas()">
            <mat-option value="">Todos</mat-option>
            @for (c of clientes(); track c.id) {
              <mat-option [value]="c.id">{{ c.razonSocial }}</mat-option>
            }
          </mat-select>
        </mat-form-field>

        <mat-form-field appearance="fill">
          <mat-label>Estado</mat-label>
          <mat-select [(ngModel)]="filtroFacturasEstado" (ngModelChange)="cargarFacturas()">
            <mat-option value="">Todos</mat-option>
            <mat-option value="EMITIDA">Emitida</mat-option>
            <mat-option value="PAGADA">Pagada</mat-option>
            <mat-option value="ANULADA">Anulada</mat-option>
          </mat-select>
        </mat-form-field>
      </div>

      @if (cargandoFacturas()) {
        <div class="spinner-centrado"><mat-spinner diameter="40" /></div>
      } @else {
        <div class="superficie" style="padding:0;overflow:hidden">
          <table mat-table [dataSource]="facturas()">

            <ng-container matColumnDef="numFactura">
              <th mat-header-cell *matHeaderCellDef>N° Factura</th>
              <td mat-cell *matCellDef="let f">
                <strong class="texto-primario">{{ f.numFactura }}</strong>
              </td>
            </ng-container>

            <ng-container matColumnDef="cliente">
              <th mat-header-cell *matHeaderCellDef>Cliente</th>
              <td mat-cell *matCellDef="let f">{{ f.clienteRazonSocial ?? nombreCliente(f.clienteId) }}</td>
            </ng-container>

            <ng-container matColumnDef="fechaEmision">
              <th mat-header-cell *matHeaderCellDef>Fecha emisión</th>
              <td mat-cell *matCellDef="let f">{{ f.fechaEmision | date:'dd/MM/yyyy' }}</td>
            </ng-container>

            <ng-container matColumnDef="subtotal">
              <th mat-header-cell *matHeaderCellDef>Subtotal</th>
              <td mat-cell *matCellDef="let f">{{ f.subtotal | currency:'CLP':'$':'1.0-0' }}</td>
            </ng-container>

            <ng-container matColumnDef="iva">
              <th mat-header-cell *matHeaderCellDef>IVA (19%)</th>
              <td mat-cell *matCellDef="let f">{{ f.iva | currency:'CLP':'$':'1.0-0' }}</td>
            </ng-container>

            <ng-container matColumnDef="total">
              <th mat-header-cell *matHeaderCellDef>Total</th>
              <td mat-cell *matCellDef="let f">
                <strong>{{ f.total | currency:'CLP':'$':'1.0-0' }}</strong>
              </td>
            </ng-container>

            <ng-container matColumnDef="estado">
              <th mat-header-cell *matHeaderCellDef>Estado</th>
              <td mat-cell *matCellDef="let f">
                <span [class]="insigniaFactura(f.estado)">{{ f.estado }}</span>
              </td>
            </ng-container>

            <ng-container matColumnDef="acciones">
              <th mat-header-cell *matHeaderCellDef></th>
              <td mat-cell *matCellDef="let f" class="columna-acciones">
                <button mat-icon-button (click)="verPdf(f.id)" matTooltip="Ver / Imprimir PDF">
                  <mat-icon>picture_as_pdf</mat-icon>
                </button>
                @if (f.estado !== 'ANULADA') {
                  <button mat-icon-button color="warn" (click)="anularFactura(f)" matTooltip="Anular factura">
                    <mat-icon>cancel</mat-icon>
                  </button>
                }
              </td>
            </ng-container>

            <tr mat-header-row *matHeaderRowDef="columnasFacturas"></tr>
            <tr mat-row *matRowDef="let fila; columns: columnasFacturas;"
                [class.fila-anulada]="fila.estado === 'ANULADA'"></tr>
          </table>

          @if (facturas().length === 0) {
            <div class="estado-vacio-tabla">
              <mat-icon>receipt</mat-icon>
              <p>No hay facturas registradas.</p>
            </div>
          }
        </div>
      }
    }

    <!-- Modal confirmar facturación -->
    @if (mostrarDialogoFacturar()) {
      <div class="capa-modal" (click)="cerrarDialogoFacturar()">
        <div class="panel-modal" style="width:480px" (click)="$event.stopPropagation()">
          <h2>Confirmar facturación</h2>

          <div class="resumen-factura">
            <div class="linea-resumen">
              <span>Cliente</span>
              <strong>{{ nombreCliente(clienteIdSeleccionado()) }}</strong>
            </div>
            <div class="linea-resumen">
              <span>Servicios</span>
              <strong>{{ seleccionados().length }}</strong>
            </div>
            <div class="linea-resumen">
              <span>Subtotal (neto)</span>
              <strong>{{ subtotalSeleccionado() | currency:'CLP':'$':'1.0-0' }}</strong>
            </div>
            <div class="linea-resumen">
              <span>IVA (19%)</span>
              <strong>{{ ivaSeleccionado() | currency:'CLP':'$':'1.0-0' }}</strong>
            </div>
            <div class="linea-resumen total">
              <span>Total</span>
              <strong>{{ totalSeleccionado() | currency:'CLP':'$':'1.0-0' }}</strong>
            </div>
          </div>

          <mat-form-field appearance="fill" class="ancho-completo" style="margin-top:16px">
            <mat-label>Notas (opcional)</mat-label>
            <textarea matInput rows="3" [(ngModel)]="notasFactura" placeholder="Ej: Pago a 30 días..."></textarea>
          </mat-form-field>

          <div class="acciones-formulario">
            <button mat-button (click)="cerrarDialogoFacturar()">Cancelar</button>
            <button mat-flat-button class="btn-principal" [disabled]="facturando()" (click)="confirmarFacturar()">
              <mat-icon>receipt</mat-icon>
              {{ facturando() ? 'Procesando…' : 'Emitir factura' }}
            </button>
          </div>
        </div>
      </div>
    }
  `,
  styles: [`
    .tabs-contenedor { padding: 0 !important; border-radius: var(--radio-lg) var(--radio-lg) 0 0; margin-bottom: 0 !important; }
    .tabs-manual {
      display: flex;
      border-bottom: 2px solid var(--color-borde);
    }
    .tab-btn {
      display: flex; align-items: center; gap: 6px;
      padding: 14px 24px;
      background: none; border: none; cursor: pointer;
      font-size: 14px; font-weight: 500; font-family: var(--fuente);
      color: var(--color-texto-2);
      border-bottom: 3px solid transparent;
      margin-bottom: -2px;
      transition: all .15s;
      mat-icon { font-size: 18px; width: 18px; height: 18px; }
      &:hover { color: var(--azul-600); background: var(--azul-50, #EFF6FF); }
      &.activo { color: var(--azul-600); border-bottom-color: var(--azul-600); font-weight: 600; }
    }

    .barra-filtros {
      display: flex; flex-wrap: wrap; gap: 12px;
      padding: 16px 0;
      align-items: center;
      mat-form-field { min-width: 160px; }
    }

    .estado-vacio-tabla {
      display: flex; flex-direction: column; align-items: center;
      padding: 48px; gap: 8px; color: var(--color-texto-3);
      mat-icon { font-size: 48px; width: 48px; height: 48px; opacity: .3; }
    }

    .columna-acciones { white-space: nowrap; text-align: right; }

    .nombre-cliente { font-weight: 500; }

    .fila-seleccionada { background: var(--azul-50, #EFF6FF) !important; }
    .fila-facturada td { opacity: .6; }
    .fila-anulada td { opacity: .5; text-decoration: line-through; }

    /* Panel flotante */
    .panel-flotante {
      position: fixed;
      bottom: 32px;
      left: 50%;
      transform: translateX(-50%);
      background: var(--azul-900, #1B2C40);
      color: #fff;
      border-radius: var(--radio-lg);
      padding: 14px 24px;
      display: flex; align-items: center; gap: 20px;
      box-shadow: 0 8px 32px rgba(0,0,0,.35);
      z-index: 100;
      animation: slideUp .25s ease;
      flex-wrap: wrap;
    }
    @keyframes slideUp {
      from { opacity: 0; transform: translateX(-50%) translateY(20px); }
      to   { opacity: 1; transform: translateX(-50%) translateY(0); }
    }
    .info-seleccion {
      display: flex; align-items: center; gap: 10px;
      mat-icon { color: var(--color-exito, #22c55e); }
    }
    .separador { opacity: .4; }
    .total-seleccion { font-size: 15px; strong { font-size: 17px; } }
    .aviso-cliente { color: #fbbf24; font-size: 13px; display: flex; align-items: center; gap: 4px; }
    .acciones-panel { display: flex; gap: 8px; margin-left: auto; }

    /* Modal resumen factura */
    .resumen-factura {
      background: var(--color-fondo, #F8FAFC);
      border-radius: var(--radio-md);
      padding: 16px;
      display: flex; flex-direction: column; gap: 10px;
    }
    .linea-resumen {
      display: flex; justify-content: space-between; align-items: center;
      font-size: 14px; color: var(--color-texto-2);
      &.total {
        font-size: 16px; color: var(--color-texto-1);
        padding-top: 10px;
        border-top: 1px solid var(--color-borde);
        strong { color: var(--azul-600); font-size: 18px; }
      }
    }

    .acciones-encabezado { display: flex; align-items: center; gap: 12px; }
  `],
})
export class FacturacionComponent implements OnInit {
  private readonly operacionesSvc = inject(OperacionesService);
  private readonly facturacionSvc = inject(FacturacionService);
  private readonly snack           = inject(MatSnackBar);
  private readonly dialogo         = inject(DialogoService);

  // ── Estado ─────────────────────────────────────────────────
  pestanaActiva        = signal<'servicios' | 'facturas'>('servicios');
  cargandoServicios    = signal(true);
  cargandoFacturas     = signal(false);
  facturando           = signal(false);
  mostrarDialogoFacturar = signal(false);

  servicios  = signal<Servicio[]>([]);
  facturas   = signal<Factura[]>([]);
  clientes   = signal<Cliente[]>([]);
  seleccionados = signal<Servicio[]>([]);
  notasFactura  = '';

  // ── Filtros servicios ─────────────────────────────────────
  filtroClienteId  = '';
  filtroEstado     = '';
  filtroFacturado  = '';
  filtroDesde      = '';
  filtroHasta      = '';

  // ── Filtros facturas ──────────────────────────────────────
  filtroFacturasClienteId = '';
  filtroFacturasEstado    = '';

  // ── Columnas ──────────────────────────────────────────────
  columnasServicios = ['seleccion', 'numero', 'cliente', 'ruta', 'fecha', 'valor', 'estado', 'facturado'];
  columnasFacturas  = ['numFactura', 'cliente', 'fechaEmision', 'subtotal', 'iva', 'total', 'estado', 'acciones'];

  // ── Computed ──────────────────────────────────────────────
  totalSeleccionado = computed(() =>
    this.seleccionados().reduce((acc, s) => acc + (s.valorTotal ?? 0), 0)
  );

  subtotalSeleccionado = computed(() =>
    this.seleccionados().reduce((acc, s) => acc + (s.valorNeto ?? 0), 0)
  );

  ivaSeleccionado = computed(() =>
    this.seleccionados().reduce((acc, s) => acc + (s.iva ?? 0), 0)
  );

  mismoCiente = computed(() => {
    const ids = [...new Set(this.seleccionados().map(s => s.clienteId))];
    return ids.length <= 1;
  });

  clienteIdSeleccionado = computed(() =>
    this.seleccionados()[0]?.clienteId ?? ''
  );

  todosSeleccionados = computed(() => {
    const pendientes = this.servicios().filter(s => s.facturado !== 1);
    return pendientes.length > 0 && this.seleccionados().length === pendientes.length;
  });

  algunosSeleccionados = computed(() =>
    this.seleccionados().length > 0 && !this.todosSeleccionados()
  );

  // ── Ciclo de vida ─────────────────────────────────────────
  ngOnInit() {
    this.operacionesSvc.getClientes().subscribe(r => this.clientes.set(r.content));
    this.cargarServicios();
  }

  // ── Carga de datos ────────────────────────────────────────
  cargarServicios() {
    this.cargandoServicios.set(true);
    this.seleccionados.set([]);
    this.operacionesSvc.getServicios({
      clienteId: this.filtroClienteId  || undefined,
      estado:    this.filtroEstado     || undefined,
      desde:     this.filtroDesde      || undefined,
      hasta:     this.filtroHasta      || undefined,
      facturado: this.filtroFacturado !== '' ? Number(this.filtroFacturado) : undefined,
      tamano:    100,
    }).subscribe({
      next: r => { this.servicios.set(r.content); this.cargandoServicios.set(false); },
      error: () => this.cargandoServicios.set(false),
    });
  }

  cargarFacturas() {
    this.cargandoFacturas.set(true);
    this.facturacionSvc.getAll({
      clienteId: this.filtroFacturasClienteId || undefined,
      estado:    this.filtroFacturasEstado    || undefined,
      tamano:    100,
    }).subscribe({
      next: r => { this.facturas.set(r.content); this.cargandoFacturas.set(false); },
      error: () => this.cargandoFacturas.set(false),
    });
  }

  // ── Filtros ───────────────────────────────────────────────
  limpiarFiltros() {
    this.filtroClienteId = '';
    this.filtroEstado    = '';
    this.filtroFacturado = '';
    this.filtroDesde     = '';
    this.filtroHasta     = '';
    this.cargarServicios();
  }

  // ── Selección ─────────────────────────────────────────────
  estaSeleccionado(id: string): boolean {
    return this.seleccionados().some(s => s.id === id);
  }

  toggleSeleccion(servicio: Servicio) {
    if (servicio.facturado === 1) return;
    const actual = this.seleccionados();
    if (this.estaSeleccionado(servicio.id)) {
      this.seleccionados.set(actual.filter(s => s.id !== servicio.id));
    } else {
      this.seleccionados.set([...actual, servicio]);
    }
  }

  toggleTodos(checked: boolean) {
    if (checked) {
      this.seleccionados.set(this.servicios().filter(s => s.facturado !== 1));
    } else {
      this.seleccionados.set([]);
    }
  }

  limpiarSeleccion() {
    this.seleccionados.set([]);
  }

  // ── Facturación ───────────────────────────────────────────
  abrirDialogoFacturar() {
    if (!this.mismoCiente()) return;
    this.notasFactura = '';
    this.mostrarDialogoFacturar.set(true);
  }

  cerrarDialogoFacturar() {
    this.mostrarDialogoFacturar.set(false);
  }

  confirmarFacturar() {
    if (!this.mismoCiente() || this.seleccionados().length === 0) return;
    this.facturando.set(true);

    this.facturacionSvc.facturar({
      clienteId:   this.clienteIdSeleccionado(),
      servicioIds: this.seleccionados().map(s => s.id),
      notas:       this.notasFactura || undefined,
    }).subscribe({
      next: (f) => {
        this.facturando.set(false);
        this.cerrarDialogoFacturar();
        this.limpiarSeleccion();
        this.snack.open(`Factura ${f.numFactura} emitida correctamente`, 'Ver PDF', { duration: 6000 })
          .onAction().subscribe(() => this.verPdf(f.id));
        // Abrir PDF automáticamente en nueva pestaña
        this.verPdf(f.id);
        this.cargarServicios();
        this.pestanaActiva.set('facturas');
        this.cargarFacturas();
      },
      error: (err) => {
        this.facturando.set(false);
        const msg = err?.error?.message ?? 'Error al emitir la factura';
        this.snack.open(msg, 'Cerrar', { duration: 5000 });
      },
    });
  }

  // ── PDF ───────────────────────────────────────────────────
  verPdf(facturaId: string): void {
    window.open(`http://localhost:8080/api/v1/facturas/${facturaId}/pdf`, '_blank');
  }

  // ── Anular factura ────────────────────────────────────────
  async anularFactura(f: Factura) {
    const ok = await this.dialogo.confirmarCritico(
      `¿Anular la factura ${f.numFactura}?`,
      'Esta acción <strong>liberará los servicios asociados</strong> y no se puede deshacer.'
    );
    if (!ok) return;
    this.facturacionSvc.anular(f.id).subscribe({
      next: () => {
        this.snack.open('Factura anulada. Los servicios han sido liberados.', '', { duration: 4000 });
        this.cargarFacturas();
        this.cargarServicios();
      },
      error: (err) => {
        const msg = err?.error?.message ?? 'Error al anular la factura';
        this.snack.open(msg, 'Cerrar', { duration: 5000 });
      },
    });
  }

  // ── Helpers ───────────────────────────────────────────────
  nombreCliente(clienteId: string): string {
    return this.clientes().find(c => c.id === clienteId)?.razonSocial ?? clienteId;
  }

  insigniaEstado(estado: string): string {
    const mapa: Record<string, string> = {
      PENDIENTE:  'insignia insignia-info',
      EN_CURSO:   'insignia insignia-azul',
      COMPLETADO: 'insignia insignia-exito',
      CANCELADO:  'insignia insignia-peligro',
    };
    return mapa[estado] ?? 'insignia insignia-info';
  }

  insigniaFactura(estado: string): string {
    const mapa: Record<string, string> = {
      EMITIDA: 'insignia insignia-azul',
      PAGADA:  'insignia insignia-exito',
      ANULADA: 'insignia insignia-peligro',
    };
    return mapa[estado] ?? 'insignia insignia-info';
  }
}
