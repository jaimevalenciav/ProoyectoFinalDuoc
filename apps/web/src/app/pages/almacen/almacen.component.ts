import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import {
  AlmacenService, AjusteStockDto, IngresoFacturaDto, IngresoFacturaLineaDto,
} from '@core/services/almacen.service';
import { DialogoService } from '@core/services/dialogo.service';
import { PerfilService } from '@core/services/perfil.service';
import { Repuesto, MovimientoStock } from '@core/models';
import { validarRut, procesarInputRut } from '@core/utils/rut.utils';

interface LineaFactura {
  repuestoId:    string;
  codigo:        string;
  descripcion:   string;
  unidad:        string;
  precioUnit:    number;
  cantidad:      number;
  total:         number;
}

@Component({
  selector: 'app-almacen',
  standalone: true,
  imports: [
    CommonModule, FormsModule, ReactiveFormsModule,
    MatTabsModule, MatTableModule, MatButtonModule, MatIconModule,
    MatInputModule, MatSelectModule, MatAutocompleteModule,
    MatProgressSpinnerModule, MatSnackBarModule, MatTooltipModule,
  ],
  template: `
    <div class="encabezado-pagina">
      <h1>Almacén — Inventario</h1>
    </div>

    <mat-tab-group animationDuration="200ms" class="tabs-almacen">

      <!-- ════════ TAB 1: Inventario ════════ -->
      <mat-tab label="Inventario">
        <div class="tab-contenido">
          <div class="barra-acciones">
            <mat-form-field appearance="fill" style="width:280px">
              <mat-label>Buscar repuesto</mat-label>
              <input matInput [(ngModel)]="busqueda" (ngModelChange)="cargar()" />
              <mat-icon matSuffix>search</mat-icon>
            </mat-form-field>
            <button mat-stroked-button class="btn-secundario" (click)="alternarCriticos()">
              <mat-icon>warning_amber</mat-icon>
              {{ soloCriticos ? 'Ver todos' : 'Solo críticos' }}
            </button>
            <span style="flex:1"></span>
            @if (puedeEscribir()) {
              <button mat-flat-button class="btn-principal" (click)="abrirFormulario()">
                <mat-icon>add</mat-icon> Nuevo artículo
              </button>
            }
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
                  <td mat-cell *matCellDef="let r">
                    <div style="font-weight:500">{{ r.descripcion }}</div>
                    <div style="font-size:11px;color:var(--ink-soft)">{{ r.categoria }} · {{ r.unidad }}</div>
                  </td>
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
                    @if (puedeEscribir()) {
                      <button mat-icon-button (click)="abrirAjuste(r)" matTooltip="Ajuste rápido" style="color:var(--azul-600)">
                        <mat-icon>swap_horiz</mat-icon>
                      </button>
                      <button mat-icon-button (click)="abrirFormulario(r)" matTooltip="Editar">
                        <mat-icon>edit</mat-icon>
                      </button>
                      <button mat-icon-button color="warn" (click)="eliminar(r)" matTooltip="Eliminar">
                        <mat-icon>delete_outline</mat-icon>
                      </button>
                    }
                  </td>
                </ng-container>
                <tr mat-header-row *matHeaderRowDef="columnas"></tr>
                <tr mat-row *matRowDef="let fila; columns: columnas;"
                    [class.fila-critica]="fila.stockActual <= fila.stockMinimo"></tr>
              </table>
              @if (repuestos().length === 0) {
                <div class="estado-vacio-tabla">
                  <mat-icon>inventory_2</mat-icon><p>No hay repuestos en inventario.</p>
                </div>
              }
            </div>
          }
        </div>
      </mat-tab>

      <!-- ════════ TAB 2: Ingreso Factura / Guía (solo escritura) ════════ -->
      @if (puedeEscribir()) {
      <mat-tab label="Entrada por Documento">
        <div class="tab-contenido">
          <div class="doc-encabezado superficie">
            <div class="doc-titulo">
              <mat-icon>receipt_long</mat-icon>
              <span>Ingreso de stock — Factura / Guía de despacho</span>
            </div>

            <!-- ── Encabezado del documento ── -->
            <div class="tres-columnas">
              <mat-form-field appearance="fill">
                <mat-label>Tipo documento</mat-label>
                <mat-select [(ngModel)]="docTipo">
                  <mat-option value="FACTURA">Factura</mat-option>
                  <mat-option value="GUIA_DESPACHO">Guía de despacho</mat-option>
                </mat-select>
              </mat-form-field>
              <mat-form-field appearance="fill">
                <mat-label>N° Documento</mat-label>
                <input matInput [(ngModel)]="docNumero" placeholder="Ej: 45231" />
              </mat-form-field>
              <mat-form-field appearance="fill">
                <mat-label>Fecha documento</mat-label>
                <input matInput type="date" [(ngModel)]="docFecha" />
              </mat-form-field>
            </div>
            <div class="dos-columnas">
              <mat-form-field appearance="fill">
                <mat-label>Proveedor</mat-label>
                <input matInput [(ngModel)]="docProveedor" placeholder="Nombre del proveedor" />
              </mat-form-field>
              <mat-form-field appearance="fill">
                <mat-label>RUT Proveedor</mat-label>
                <input matInput [(ngModel)]="docRutProveedor" placeholder="76.543.210-K"
                       (input)="onRutProveedorInput($event)" />
                <mat-icon matSuffix style="font-size:16px;opacity:.5">badge</mat-icon>
                @if (rutProveedorInvalido()) {
                  <mat-error>RUT inválido — revise el dígito verificador</mat-error>
                }
              </mat-form-field>
            </div>
          </div>

          <!-- ── Líneas de detalle ── -->
          <div class="superficie doc-detalle">
            <div class="detalle-cabecera">
              <span class="detalle-titulo">Detalle de artículos</span>
              <button mat-stroked-button class="btn-secundario" (click)="agregarLinea()">
                <mat-icon>add</mat-icon> Agregar artículo
              </button>
            </div>

            <!-- Tabla de líneas -->
            <div class="tabla-lineas-wrapper">
              <table class="tabla-lineas">
                <thead>
                  <tr>
                    <th style="min-width:260px">Artículo</th>
                    <th>Descripción seleccionada</th>
                    <th style="width:80px">Unidad</th>
                    <th style="width:120px;text-align:right">Precio unit.</th>
                    <th style="width:90px;text-align:right">Cantidad</th>
                    <th style="width:130px;text-align:right">Total ítem</th>
                    <th style="width:48px"></th>
                  </tr>
                </thead>
                <tbody>
                  @for (linea of lineasFactura; track $index) {
                    <tr class="fila-linea">
                      <td class="col-busqueda-rep">
                        <mat-form-field appearance="fill" class="campo-linea campo-busqueda-rep">
                          <input matInput
                                 [(ngModel)]="busquedasLinea[$index]"
                                 [matAutocomplete]="autoRep"
                                 placeholder="Buscar código o descripción…"
                                 autocomplete="off" />
                          <mat-icon matSuffix style="font-size:16px;opacity:.45">search</mat-icon>
                          <mat-autocomplete #autoRep="matAutocomplete"
                                            (optionSelected)="onRepuestoAutoSeleccionado($index, $event.option.value)">
                            @for (r of filtrarCatalogo(busquedasLinea[$index]); track r.id) {
                              <mat-option [value]="repuestoDisplayText(r)">
                                <span class="opt-codigo">[{{ r.codigo }}]</span>
                                {{ r.descripcion }}
                                <small class="opt-unidad"> · {{ r.unidad }}</small>
                              </mat-option>
                            }
                          </mat-autocomplete>
                        </mat-form-field>
                      </td>
                      <td class="col-descripcion">
                        @if (linea.codigo) {
                          <span class="badge-mono" style="font-size:11px;margin-right:6px">{{ linea.codigo }}</span>
                        }
                        {{ linea.descripcion || '—' }}
                      </td>
                      <td>{{ linea.unidad || '—' }}</td>
                      <td>
                        <mat-form-field appearance="fill" class="campo-linea campo-numero">
                          <input matInput type="number" [(ngModel)]="linea.precioUnit" min="0"
                                 (ngModelChange)="recalcularLinea($index)" />
                        </mat-form-field>
                      </td>
                      <td>
                        <mat-form-field appearance="fill" class="campo-linea campo-numero">
                          <input matInput type="number" [(ngModel)]="linea.cantidad" min="0.01"
                                 (ngModelChange)="recalcularLinea($index)" />
                        </mat-form-field>
                      </td>
                      <td class="col-total">{{ linea.total | currency:'CLP':'$':'1.0-0' }}</td>
                      <td>
                        <button mat-icon-button color="warn" (click)="quitarLinea($index)"
                                matTooltip="Quitar línea">
                          <mat-icon>remove_circle_outline</mat-icon>
                        </button>
                      </td>
                    </tr>
                  }
                  @if (lineasFactura.length === 0) {
                    <tr>
                      <td colspan="7" class="sin-lineas">
                        <mat-icon>add_shopping_cart</mat-icon>
                        <span>Agrega artículos con el botón "Agregar artículo"</span>
                      </td>
                    </tr>
                  }
                </tbody>
                <tfoot>
                  <tr class="fila-total">
                    <td colspan="5" style="text-align:right;padding-right:12px">
                      <strong>TOTAL DOCUMENTO</strong>
                    </td>
                    <td class="col-total" style="font-size:15px;font-weight:700">
                      {{ totalDocumento() | currency:'CLP':'$':'1.0-0' }}
                    </td>
                    <td></td>
                  </tr>
                </tfoot>
              </table>
            </div>

            <div class="doc-acciones">
              <button mat-button type="button" (click)="limpiarFactura()">
                <mat-icon>clear_all</mat-icon> Limpiar
              </button>
              <button mat-flat-button class="btn-principal"
                      [disabled]="!puedeGuardarFactura() || guardandoFactura()"
                      (click)="confirmarIngresoFactura()">
                <mat-icon>save</mat-icon>
                {{ guardandoFactura() ? 'Procesando…' : 'Confirmar ingreso' }}
              </button>
            </div>
          </div>
        </div>
      </mat-tab>
      } <!-- fin @if puedeEscribir -->

      <!-- ════════ TAB 3: Movimientos de Stock ════════ -->
      <mat-tab label="Movimientos">
        <div class="tab-contenido">
          <!-- Filtros -->
          <div class="superficie filtros-mov">
            <div class="filtros-fila">
              <mat-form-field appearance="fill" style="flex:1;min-width:200px">
                <mat-label>Artículo</mat-label>
                <mat-select [(ngModel)]="filtroRepuestoId" (ngModelChange)="cargarMovimientos()">
                  <mat-option value="">Todos</mat-option>
                  @for (r of catalogoRepuestos(); track r.id) {
                    <mat-option [value]="r.id">{{ r.codigo }} — {{ r.descripcion }}</mat-option>
                  }
                </mat-select>
              </mat-form-field>
              <mat-form-field appearance="fill" style="width:140px">
                <mat-label>Tipo</mat-label>
                <mat-select [(ngModel)]="filtroTipo" (ngModelChange)="cargarMovimientos()">
                  <mat-option value="">Todos</mat-option>
                  <mat-option value="ENTRADA">Entrada</mat-option>
                  <mat-option value="SALIDA">Salida</mat-option>
                  <mat-option value="AJUSTE">Ajuste</mat-option>
                </mat-select>
              </mat-form-field>
              <mat-form-field appearance="fill" style="width:160px">
                <mat-label>Documento</mat-label>
                <input matInput [(ngModel)]="filtroDocumento" (ngModelChange)="cargarMovimientos()" placeholder="N° FAC / Guía" />
              </mat-form-field>
              <mat-form-field appearance="fill" style="width:150px">
                <mat-label>Desde</mat-label>
                <input matInput type="date" [(ngModel)]="filtroDesde" (ngModelChange)="cargarMovimientos()" />
              </mat-form-field>
              <mat-form-field appearance="fill" style="width:150px">
                <mat-label>Hasta</mat-label>
                <input matInput type="date" [(ngModel)]="filtroHasta" (ngModelChange)="cargarMovimientos()" />
              </mat-form-field>
              <button mat-icon-button (click)="limpiarFiltrosMov()" matTooltip="Limpiar filtros">
                <mat-icon>filter_alt_off</mat-icon>
              </button>
            </div>
          </div>

          @if (cargandoMov()) {
            <div class="spinner-centrado"><mat-spinner diameter="40" /></div>
          } @else {
            <div class="superficie" style="padding:0;overflow:hidden">
              <table mat-table [dataSource]="movimientos()">
                <ng-container matColumnDef="createdAt">
                  <th mat-header-cell *matHeaderCellDef>Fecha</th>
                  <td mat-cell *matCellDef="let m">{{ m.createdAt | date:'dd/MM/yyyy HH:mm' }}</td>
                </ng-container>
                <ng-container matColumnDef="tipo">
                  <th mat-header-cell *matHeaderCellDef>Tipo</th>
                  <td mat-cell *matCellDef="let m">
                    <span [class]="chipTipo(m.tipo)">{{ m.tipo }}</span>
                  </td>
                </ng-container>
                <ng-container matColumnDef="repuesto">
                  <th mat-header-cell *matHeaderCellDef>Artículo</th>
                  <td mat-cell *matCellDef="let m">
                    <div style="font-weight:500">{{ m.repuestoCodigo || m.repuestoId }}</div>
                    @if (m.repuestoDescripcion) {
                      <div style="font-size:11px;color:var(--ink-soft)">{{ m.repuestoDescripcion }}</div>
                    }
                  </td>
                </ng-container>
                <ng-container matColumnDef="cantidad">
                  <th mat-header-cell *matHeaderCellDef style="text-align:right">Cantidad</th>
                  <td mat-cell *matCellDef="let m" style="text-align:right">
                    <span [style.color]="m.tipo==='SALIDA' ? '#dc2626' : m.tipo==='ENTRADA' ? '#16a34a' : '#6b7280'">
                      {{ m.tipo === 'SALIDA' ? '-' : '+' }}{{ m.cantidad }}
                    </span>
                  </td>
                </ng-container>
                <ng-container matColumnDef="stock">
                  <th mat-header-cell *matHeaderCellDef style="text-align:right">Stock anterior → nuevo</th>
                  <td mat-cell *matCellDef="let m" style="text-align:right;white-space:nowrap">
                    {{ m.stockAnterior }} → <strong>{{ m.stockNuevo }}</strong>
                  </td>
                </ng-container>
                <ng-container matColumnDef="costoTotal">
                  <th mat-header-cell *matHeaderCellDef style="text-align:right">Monto</th>
                  <td mat-cell *matCellDef="let m" style="text-align:right">
                    @if (m.costoTotal) { {{ m.costoTotal | currency:'CLP':'$':'1.0-0' }} }
                  </td>
                </ng-container>
                <ng-container matColumnDef="documento">
                  <th mat-header-cell *matHeaderCellDef>Documento</th>
                  <td mat-cell *matCellDef="let m">
                    @if (m.documento) {
                      <span class="badge-mono" style="font-size:11px">{{ m.documento }}</span>
                    }
                    @if (m.referencia && m.referencia !== m.documento) {
                      <div style="font-size:11px;color:var(--ink-soft)">{{ m.referencia }}</div>
                    }
                  </td>
                </ng-container>
                <tr mat-header-row *matHeaderRowDef="columnasMov"></tr>
                <tr mat-row *matRowDef="let fila; columns: columnasMov;"></tr>
              </table>
              @if (movimientos().length === 0) {
                <div class="estado-vacio-tabla">
                  <mat-icon>swap_horiz</mat-icon>
                  <p>No hay movimientos con los filtros aplicados.</p>
                </div>
              }
            </div>

            <!-- Paginación simple -->
            @if (totalMovPages() > 1) {
              <div class="paginacion">
                <button mat-button [disabled]="paginaMov === 0" (click)="cambiarPaginaMov(-1)">
                  <mat-icon>chevron_left</mat-icon>
                </button>
                <span>Pág. {{ paginaMov + 1 }} / {{ totalMovPages() }}</span>
                <button mat-button [disabled]="paginaMov >= totalMovPages() - 1" (click)="cambiarPaginaMov(1)">
                  <mat-icon>chevron_right</mat-icon>
                </button>
              </div>
            }
          }
        </div>
      </mat-tab>

    </mat-tab-group>

    <!-- ══════════ MODAL: CRUD Repuesto ══════════ -->
    @if (mostrarFormulario()) {
      <div class="capa-modal" (click)="cerrarFormulario()">
        <div class="panel-modal" (click)="$event.stopPropagation()">
          <h2>{{ idEdicion() ? 'Editar' : 'Nuevo' }} artículo</h2>
          <form [formGroup]="formulario" (ngSubmit)="guardar()">
            <div class="dos-columnas">
              <mat-form-field appearance="fill">
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
              <mat-label>Descripción del artículo</mat-label>
              <input matInput formControlName="descripcion" />
            </mat-form-field>
            <div class="dos-columnas">
              <mat-form-field appearance="fill">
                <mat-label>Unidad de medida</mat-label>
                <mat-select formControlName="unidad">
                  <mat-option value="UN">Unidad</mat-option>
                  <mat-option value="LT">Litro</mat-option>
                  <mat-option value="KG">Kilogramo</mat-option>
                  <mat-option value="MT">Metro</mat-option>
                  <mat-option value="JGO">Juego</mat-option>
                  <mat-option value="CAJA">Caja</mat-option>
                </mat-select>
              </mat-form-field>
              <mat-form-field appearance="fill">
                <mat-label>Stock mínimo</mat-label>
                <input matInput type="number" formControlName="stockMinimo" min="0" />
              </mat-form-field>
            </div>
            <div class="dos-columnas">
              <mat-form-field appearance="fill">
                <mat-label>Stock inicial</mat-label>
                <input matInput type="number" formControlName="stockActual" min="0" />
                <span matSuffix style="font-size:12px;color:var(--ink-soft)">unid.</span>
              </mat-form-field>
              <mat-form-field appearance="fill">
                <mat-label>Precio unitario (CLP)</mat-label>
                <input matInput type="number" formControlName="precioUnitario" min="0" />
                <span matPrefix>$&nbsp;</span>
              </mat-form-field>
            </div>
            <mat-form-field appearance="fill" class="ancho-completo">
              <mat-label>Proveedor (referencia)</mat-label>
              <input matInput formControlName="proveedor" />
            </mat-form-field>
            <div class="acciones-formulario">
              <button mat-button type="button" (click)="cerrarFormulario()">Cancelar</button>
              <button mat-flat-button class="btn-principal" type="submit"
                      [disabled]="formulario.invalid || guardando()">
                {{ guardando() ? 'Guardando…' : 'Guardar' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    }

    <!-- ══════════ MODAL: Ajuste rápido de stock ══════════ -->
    @if (mostrarAjuste()) {
      <div class="capa-modal" (click)="cerrarAjuste()">
        <div class="panel-modal panel-ajuste" (click)="$event.stopPropagation()">
          <h2>Ajuste de stock</h2>
          <p class="subtitulo-ajuste">
            <strong>{{ repuestoAjuste()?.codigo }}</strong> — {{ repuestoAjuste()?.descripcion }}<br>
            <span class="texto-atenuado">
              Stock actual: <strong>{{ repuestoAjuste()?.stockActual }} {{ repuestoAjuste()?.unidad }}</strong>
              · Precio unitario ref.: <strong>{{ repuestoAjuste()?.precioUnitario | currency:'CLP':'$':'1.0-0' }}</strong>
            </span>
          </p>
          <form [formGroup]="formularioAjuste" (ngSubmit)="confirmarAjuste()">
            <mat-form-field appearance="fill" class="ancho-completo">
              <mat-label>Tipo de movimiento</mat-label>
              <mat-select formControlName="tipo">
                <mat-option value="SALIDA">Salida (reduce stock)</mat-option>
                <mat-option value="AJUSTE">Ajuste de inventario</mat-option>
              </mat-select>
            </mat-form-field>
            <div class="dos-columnas">
              <mat-form-field appearance="fill">
                <mat-label>Cantidad</mat-label>
                <input matInput type="number" formControlName="cantidad" min="0.01" />
                <span matSuffix style="font-size:12px;color:var(--ink-soft)">{{ repuestoAjuste()?.unidad }}</span>
              </mat-form-field>
              <mat-form-field appearance="fill">
                <mat-label>Precio unitario (CLP)</mat-label>
                <input matInput type="number" formControlName="precioUnit" min="0"
                       [placeholder]="(repuestoAjuste()?.precioUnitario ?? 0).toString()" />
                <span matPrefix>$&nbsp;</span>
              </mat-form-field>
            </div>
            @if (totalAjuste() > 0) {
              <div class="total-ajuste-preview">
                <mat-icon>calculate</mat-icon>
                Costo total movimiento:
                <strong>{{ totalAjuste() | currency:'CLP':'$':'1.0-0' }}</strong>
              </div>
            }
            <mat-form-field appearance="fill" class="ancho-completo">
              <mat-label>Motivo del ajuste</mat-label>
              <input matInput formControlName="motivo" placeholder="Ej: Consumo en OT-235, merma, vencimiento" />
            </mat-form-field>
            <mat-form-field appearance="fill" class="ancho-completo">
              <mat-label>Referencia / documento</mat-label>
              <input matInput formControlName="referencia" placeholder="N° OT, guía, acta de inventario" />
            </mat-form-field>
            <div class="acciones-formulario">
              <button mat-button type="button" (click)="cerrarAjuste()">Cancelar</button>
              <button mat-flat-button class="btn-principal" type="submit"
                      [disabled]="formularioAjuste.invalid || guardando()">
                {{ guardando() ? 'Procesando…' : 'Confirmar ajuste' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    }
  `,
  styles: [`
    .tabs-almacen { margin-top: -8px; }
    .tab-contenido { padding: 20px 0; display: flex; flex-direction: column; gap: 16px; }
    .barra-acciones { display: flex; gap: 12px; align-items: flex-end; flex-wrap: wrap; }
    .columna-acciones { white-space: nowrap; }
    .fila-critica td { background: rgba(220,38,38,.04) !important; }
    .estado-vacio-tabla {
      display: flex; flex-direction: column; align-items: center; padding: 48px;
      gap: 8px; color: var(--color-texto-3);
      mat-icon { font-size: 48px; width: 48px; height: 48px; opacity: .3; }
    }
    .panel-ajuste { max-width: 480px; }
    .dos-columnas { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
    .total-ajuste-preview {
      display: flex; align-items: center; gap: 8px;
      background: #f0fdf4; border: 1px solid #bbf7d0;
      border-radius: var(--radius-sm); padding: 8px 14px;
      font-size: 13px; color: #166534; margin-bottom: 12px;
      mat-icon { color: #16a34a; font-size: 18px; width: 18px; height: 18px; }
    }
    .subtitulo-ajuste {
      font-size: 13px; color: var(--color-texto-2); margin: -8px 0 16px; line-height: 1.6;
    }

    /* ── Encabezado documento ── */
    .doc-encabezado {
      display: flex; flex-direction: column; gap: 14px;
    }
    .doc-titulo {
      display: flex; align-items: center; gap: 10px;
      font-weight: 600; font-size: 15px; color: var(--azul-800);
      mat-icon { color: var(--azul-500); }
    }
    .tres-columnas {
      display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px;
    }

    /* ── Tabla de líneas ── */
    .doc-detalle { display: flex; flex-direction: column; gap: 16px; }
    .detalle-cabecera {
      display: flex; justify-content: space-between; align-items: center;
    }
    .detalle-titulo { font-weight: 600; font-size: 13px; color: var(--ink-soft); text-transform: uppercase; letter-spacing: .05em; }
    .tabla-lineas-wrapper { overflow-x: auto; }
    .tabla-lineas {
      width: 100%; border-collapse: collapse;
    }
    .tabla-lineas th {
      background: var(--slate); padding: 8px 10px; font-size: 11px;
      font-weight: 700; color: var(--ink-soft); text-transform: uppercase;
      letter-spacing: .04em; border-bottom: 1px solid var(--slate-dark);
      white-space: nowrap;
    }
    .tabla-lineas td {
      padding: 6px 10px; border-bottom: 1px solid var(--slate);
      font-size: 13px; vertical-align: middle;
    }
    .fila-linea:hover td { background: var(--azul-50); }
    .col-descripcion { color: var(--ink-mid); }
    .col-total { text-align: right; font-weight: 600; white-space: nowrap; }
    .fila-total td { background: var(--slate); padding: 10px; border-top: 2px solid var(--slate-dark); }
    .sin-lineas {
      text-align: center; padding: 32px !important;
      color: var(--ink-soft); font-style: italic;
      display: flex; align-items: center; justify-content: center; gap: 8px;
      mat-icon { font-size: 20px; width: 20px; height: 20px; opacity: .5; }
    }
    .campo-linea { width: 100%; margin: 0; }
    .campo-linea .mat-mdc-form-field-subscript-wrapper { display: none; }
    .campo-numero { max-width: 110px; }
    .col-busqueda-rep { min-width: 260px; }
    .campo-busqueda-rep { min-width: 240px; }
    .opt-codigo { font-weight: 700; color: var(--azul-700, #1d4ed8); font-family: monospace; font-size: 12px; }
    .opt-unidad { color: var(--ink-soft); font-size: 11px; }
    .doc-acciones {
      display: flex; justify-content: flex-end; gap: 12px; padding-top: 4px;
    }

    /* ── Filtros movimientos ── */
    .filtros-mov { }
    .filtros-fila { display: flex; gap: 12px; align-items: flex-end; flex-wrap: wrap; }
    .paginacion {
      display: flex; align-items: center; justify-content: center;
      gap: 12px; padding: 12px 0;
      span { font-size: 13px; color: var(--ink-mid); }
    }

    /* ── Chips de tipo de movimiento ── */
    .chip-entrada { background: #dcfce7; color: #166534; padding: 2px 8px; border-radius: 999px; font-size: 11px; font-weight: 600; }
    .chip-salida  { background: #fee2e2; color: #991b1b; padding: 2px 8px; border-radius: 999px; font-size: 11px; font-weight: 600; }
    .chip-ajuste  { background: #e0e7ff; color: #3730a3; padding: 2px 8px; border-radius: 999px; font-size: 11px; font-weight: 600; }
  `],
})
export class AlmacenComponent implements OnInit {
  private readonly servicio     = inject(AlmacenService);
  private readonly fb           = inject(FormBuilder);
  private readonly notificacion = inject(MatSnackBar);
  private readonly dialogo      = inject(DialogoService);
  readonly perfil               = inject(PerfilService);

  /** true si el rol puede crear/editar/eliminar en almacén */
  readonly puedeEscribir = computed(() => this.perfil.puedeEscribir('almacen'));

  // ── Tab 1: Inventario ────────────────────────────────────────
  columnas          = ['codigo', 'descripcion', 'stockActual', 'stockMinimo', 'precioUnitario', 'acciones'];
  cargando          = signal(true);
  guardando         = signal(false);
  mostrarFormulario = signal(false);
  mostrarAjuste     = signal(false);
  idEdicion         = signal<string | null>(null);
  repuestoAjuste    = signal<Repuesto | null>(null);
  repuestos         = signal<Repuesto[]>([]);
  catalogoRepuestos = signal<Repuesto[]>([]);
  busqueda          = '';
  soloCriticos      = false;

  formulario = this.fb.group({
    codigo:         ['', Validators.required],
    descripcion:    ['', Validators.required],
    categoria:      ['GENERAL'],
    unidad:         ['UN'],
    stockActual:    [0, Validators.min(0)],
    stockMinimo:    [1],
    precioUnitario: [0],
    proveedor:      [''],
  });

  formularioAjuste = this.fb.group({
    tipo:        ['SALIDA', Validators.required],
    cantidad:    [1, [Validators.required, Validators.min(0.01)]],
    precioUnit:  [0, Validators.min(0)],
    referencia:  [''],
    motivo:      [''],
  });

  /** Costo total del ajuste actual */
  readonly totalAjuste = computed(() => {
    const v = this.formularioAjuste.value;
    return (Number(v.cantidad) || 0) * (Number(v.precioUnit) || 0);
  });

  // ── Tab 2: Ingreso por Factura ───────────────────────────────
  docTipo         = 'FACTURA';
  docNumero       = '';
  docProveedor    = '';
  docRutProveedor = '';
  docFecha        = new Date().toISOString().substring(0, 10);
  lineasFactura: LineaFactura[] = [];
  /** Texto de búsqueda por fila del autocomplete (sincronizado con lineasFactura) */
  busquedasLinea: string[] = [];
  guardandoFactura = signal(false);

  totalDocumento(): number {
    return this.lineasFactura.reduce((s, l) => s + (l.total || 0), 0);
  }

  puedeGuardarFactura(): boolean {
    return !!(
      this.docNumero.trim() &&
      this.docProveedor.trim() &&
      this.docFecha &&
      this.lineasFactura.length > 0 &&
      this.lineasFactura.every(l => l.repuestoId && l.cantidad > 0)
    );
  }

  // ── Tab 3: Movimientos ───────────────────────────────────────
  columnasMov       = ['createdAt', 'tipo', 'repuesto', 'cantidad', 'stock', 'costoTotal', 'documento'];
  movimientos       = signal<MovimientoStock[]>([]);
  cargandoMov       = signal(false);
  paginaMov         = 0;
  totalMovPages     = signal(0);
  filtroRepuestoId  = '';
  filtroTipo        = '';
  filtroDocumento   = '';
  filtroDesde       = '';
  filtroHasta       = '';

  ngOnInit() {
    this.cargar();
    this.cargarCatalogo();
    this.cargarMovimientos();
  }

  // ── Inventario ───────────────────────────────────────────────
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

  cargarCatalogo() {
    this.servicio.getAllActivos().subscribe({
      next: r => this.catalogoRepuestos.set(r),
    });
  }

  alternarCriticos() { this.soloCriticos = !this.soloCriticos; this.cargar(); }

  abrirFormulario(r?: Repuesto) {
    this.idEdicion.set(r?.id ?? null);
    if (r) this.formulario.patchValue(r as any);
    else this.formulario.reset({ categoria: 'GENERAL', unidad: 'UN', stockActual: 0, stockMinimo: 1, precioUnitario: 0 });
    this.mostrarFormulario.set(true);
  }

  cerrarFormulario() { this.mostrarFormulario.set(false); }

  guardar() {
    if (this.formulario.invalid) return;
    this.guardando.set(true);
    const dto = this.formulario.value as any;
    const op = this.idEdicion()
      ? this.servicio.update(this.idEdicion()!, dto)
      : this.servicio.create(dto);
    op.subscribe({
      next: () => {
        this.guardando.set(false);
        this.cerrarFormulario();
        this.cargar();
        this.cargarCatalogo();
        this.notificacion.open('Guardado', '', { duration: 3000 });
      },
      error: (e) => {
        this.guardando.set(false);
        const msg = e?.error?.message ?? e?.error?.detail ?? 'Error al guardar';
        this.notificacion.open(msg, 'OK', { duration: 5000 });
      },
    });
  }

  abrirAjuste(r: Repuesto) {
    this.repuestoAjuste.set(r);
    this.formularioAjuste.reset({ tipo: 'SALIDA', cantidad: 1 });
    this.mostrarAjuste.set(true);
  }

  cerrarAjuste() { this.mostrarAjuste.set(false); this.repuestoAjuste.set(null); }

  confirmarAjuste() {
    const rep = this.repuestoAjuste();
    if (!rep || this.formularioAjuste.invalid) return;
    this.guardando.set(true);
    const v = this.formularioAjuste.value;
    const dto: AjusteStockDto = {
      tipo:       v.tipo as 'SALIDA' | 'AJUSTE',
      cantidad:   v.cantidad!,
      precioUnit: v.precioUnit ? Number(v.precioUnit) : (rep.precioUnitario ?? 0),
      motivo:     v.motivo || undefined,
      referencia: v.referencia || undefined,
    };
    const repSnapshot = { ...rep };
    const dtoSnapshot = { ...dto };
    this.servicio.ajustarStock(rep.id, dto).subscribe({
      next: (repActualizado) => {
        this.guardando.set(false);
        this.cerrarAjuste();
        this.cargar();
        this.cargarMovimientos();
        const total = (dtoSnapshot.cantidad ?? 0) * (dtoSnapshot.precioUnit ?? 0);
        this.notificacion.open(
          `Ajuste registrado — ${dtoSnapshot.cantidad} ${repSnapshot.unidad} · ${total > 0 ? '$' + total.toLocaleString('es-CL') : 'sin valorizar'}`,
          'Imprimir',
          { duration: 6000 }
        ).onAction().subscribe(() =>
          this.imprimirReporteAjuste(repSnapshot, dtoSnapshot, repActualizado)
        );
      },
      error: (err) => {
        this.guardando.set(false);
        this.notificacion.open(err?.error?.message ?? 'Error', '', { duration: 4000 });
      },
    });
  }

  async eliminar(r: Repuesto) {
    const ok = await this.dialogo.confirmarEliminar(
      `¿Eliminar repuesto "${r.descripcion}"?`,
      `Código: ${r.codigo} · Stock actual: ${r.stockActual} ${r.unidad}`
    );
    if (!ok) return;
    this.servicio.delete(r.id).subscribe({
      next: () => { this.cargar(); this.cargarCatalogo(); },
    });
  }

  // ── Ingreso por Factura ──────────────────────────────────────
  agregarLinea() {
    this.lineasFactura.push({
      repuestoId: '', codigo: '', descripcion: '', unidad: 'UN',
      precioUnit: 0, cantidad: 1, total: 0,
    });
    this.busquedasLinea.push('');
  }

  quitarLinea(i: number) {
    this.lineasFactura.splice(i, 1);
    this.busquedasLinea.splice(i, 1);
  }

  onRepuestoSeleccionado(index: number, repuestoId: string) {
    const rep = this.catalogoRepuestos().find(r => r.id === repuestoId);
    if (rep) {
      const l = this.lineasFactura[index];
      l.codigo      = rep.codigo;
      l.descripcion = rep.descripcion;
      l.unidad      = rep.unidad ?? 'UN';
      l.precioUnit  = rep.precioUnitario ?? 0;
      this.recalcularLinea(index);
    }
  }

  /** Texto a mostrar en el input del autocomplete */
  repuestoDisplayText(r: Repuesto): string {
    return `[${r.codigo}] ${r.descripcion}`;
  }

  /** Filtra el catálogo por código o descripción (máx. 15 resultados) */
  filtrarCatalogo(texto: string): Repuesto[] {
    const q = (texto || '').trim().toLowerCase();
    // Si el texto ya coincide con un item seleccionado (es el displayText completo),
    // mostramos todos para no "bloquear" el dropdown
    const exacto = this.catalogoRepuestos().find(r => this.repuestoDisplayText(r) === texto);
    if (!q || exacto) return this.catalogoRepuestos().slice(0, 15);
    return this.catalogoRepuestos()
      .filter(r =>
        r.codigo.toLowerCase().includes(q) ||
        r.descripcion.toLowerCase().includes(q)
      )
      .slice(0, 15);
  }

  /** Callback del mat-autocomplete: el value del option es el display text */
  onRepuestoAutoSeleccionado(index: number, displayText: string): void {
    const rep = this.catalogoRepuestos().find(r => this.repuestoDisplayText(r) === displayText);
    if (!rep) return;
    const l = this.lineasFactura[index];
    l.repuestoId  = rep.id;
    l.codigo      = rep.codigo;
    l.descripcion = rep.descripcion;
    l.unidad      = rep.unidad ?? 'UN';
    l.precioUnit  = rep.precioUnitario ?? 0;
    this.busquedasLinea[index] = displayText;
    this.recalcularLinea(index);
  }

  recalcularLinea(index: number) {
    const l = this.lineasFactura[index];
    l.total = (l.precioUnit || 0) * (l.cantidad || 0);
  }

  limpiarFactura() {
    this.docNumero        = '';
    this.docProveedor     = '';
    this.docRutProveedor  = '';
    this.docFecha         = new Date().toISOString().substring(0, 10);
    this.docTipo          = 'FACTURA';
    this.lineasFactura    = [];
    this.busquedasLinea   = [];
  }

  async confirmarIngresoFactura() {
    const ok = await this.dialogo.confirmar(
      `¿Confirmar ingreso ${this.docTipo === 'FACTURA' ? 'Factura' : 'Guía'} N° ${this.docNumero}?`,
      `${this.lineasFactura.length} artículos · Total: $${this.totalDocumento().toLocaleString('es-CL')}`
    );
    if (!ok) return;
    this.guardandoFactura.set(true);
    const dto: IngresoFacturaDto = {
      tipoDocumento:   this.docTipo as 'FACTURA' | 'GUIA_DESPACHO',
      numDocumento:    this.docNumero,
      proveedor:       this.docProveedor,
      rutProveedor:    this.docRutProveedor || undefined,
      fechaDocumento:  this.docFecha,
      lineas: this.lineasFactura.map(l => ({
        repuestoId: l.repuestoId,
        cantidad:   l.cantidad,
        precioUnit: l.precioUnit,
      })),
    };
    this.servicio.ingresoFactura(dto).subscribe({
      next: (res) => {
        this.guardandoFactura.set(false);
        this.limpiarFactura();
        this.cargar();
        this.cargarMovimientos();
        this.notificacion.open(
          `Ingreso procesado: ${res.movimientos} artículos — Total $${Number(res.totalCLP).toLocaleString('es-CL')}`,
          '',
          { duration: 5000 }
        );
      },
      error: (e) => {
        this.guardandoFactura.set(false);
        this.notificacion.open(e?.error?.message ?? 'Error al procesar ingreso', 'OK', { duration: 5000 });
      },
    });
  }

  // ── Movimientos ──────────────────────────────────────────────
  cargarMovimientos() {
    this.cargandoMov.set(true);
    this.servicio.getMovimientos({
      repuestoId: this.filtroRepuestoId  || undefined,
      tipo:       this.filtroTipo        || undefined,
      documento:  this.filtroDocumento   || undefined,
      desde:      this.filtroDesde       || undefined,
      hasta:      this.filtroHasta       || undefined,
      pagina:     this.paginaMov,
      tamano:     30,
    }).subscribe({
      next: r => {
        this.movimientos.set(r.content);
        this.totalMovPages.set(r.totalPages);
        this.cargandoMov.set(false);
      },
      error: () => this.cargandoMov.set(false),
    });
  }

  limpiarFiltrosMov() {
    this.filtroRepuestoId = '';
    this.filtroTipo       = '';
    this.filtroDocumento  = '';
    this.filtroDesde      = '';
    this.filtroHasta      = '';
    this.paginaMov        = 0;
    this.cargarMovimientos();
  }

  cambiarPaginaMov(delta: number) {
    this.paginaMov = Math.max(0, Math.min(this.totalMovPages() - 1, this.paginaMov + delta));
    this.cargarMovimientos();
  }

  // ── RUT proveedor factura ────────────────────────────────────
  rutProveedorInvalido(): boolean {
    if (!this.docRutProveedor?.trim()) return false;
    return !validarRut(this.docRutProveedor);
  }

  onRutProveedorInput(event: Event): void {
    const formatted = procesarInputRut(event);
    this.docRutProveedor = formatted;
  }

  // ── Helpers ──────────────────────────────────────────────────
  insigniaStock(r: Repuesto): string {
    if (r.stockActual <= 0)             return 'insignia insignia-peligro';
    if (r.stockActual <= r.stockMinimo) return 'insignia insignia-advertencia';
    return 'insignia insignia-exito';
  }

  chipTipo(tipo: string): string {
    return tipo === 'ENTRADA' ? 'chip-entrada' : tipo === 'SALIDA' ? 'chip-salida' : 'chip-ajuste';
  }

  // ── Reporte de ajuste (ventana de impresión) ─────────────────
  imprimirReporteAjuste(rep: Repuesto, dto: AjusteStockDto, repActualizado: Repuesto): void {
    const total = (dto.cantidad ?? 0) * (dto.precioUnit ?? 0);
    const fecha = new Date().toLocaleDateString('es-CL', {
      year: 'numeric', month: 'long', day: 'numeric', hour: '2-digit', minute: '2-digit',
    });
    const html = `<!DOCTYPE html>
<html lang="es">
<head>
  <meta charset="UTF-8">
  <title>Reporte Ajuste de Inventario</title>
  <style>
    body { font-family: Arial, sans-serif; margin: 30px; color: #1a1a1a; font-size: 13px; }
    h1 { font-size: 20px; margin-bottom: 4px; }
    .subtitulo { color: #666; margin-bottom: 24px; font-size: 12px; }
    table { width: 100%; border-collapse: collapse; margin-top: 16px; }
    th { background: #1B2C40; color: #fff; padding: 8px 12px; text-align: left; font-size: 11px; text-transform: uppercase; }
    td { padding: 8px 12px; border-bottom: 1px solid #e5e7eb; }
    .valor { font-weight: 700; }
    .total-row td { background: #f1f5f9; font-weight: 700; font-size: 14px; }
    .firma { margin-top: 60px; display: flex; justify-content: space-around; }
    .firma-bloque { text-align: center; }
    .firma-linea { width: 200px; border-top: 1px solid #999; margin: 0 auto 6px; }
    @media print { button { display: none; } }
  </style>
</head>
<body>
  <h1>Reporte de Ajuste de Inventario</h1>
  <div class="subtitulo">TruckManager Pro · Almacén · Generado: ${fecha}</div>

  <table>
    <tr><th colspan="2">Información del Movimiento</th></tr>
    <tr><td>Tipo</td><td class="valor">${dto.tipo}</td></tr>
    <tr><td>Artículo</td><td class="valor">${rep.codigo} — ${rep.descripcion}</td></tr>
    <tr><td>Categoría</td><td>${rep.categoria ?? '—'}</td></tr>
    <tr><td>Cantidad ajustada</td><td class="valor">${dto.cantidad} ${rep.unidad}</td></tr>
    <tr><td>Precio unitario</td><td class="valor">$${(dto.precioUnit ?? 0).toLocaleString('es-CL')}</td></tr>
    <tr class="total-row"><td>Costo total movimiento</td><td>$${total.toLocaleString('es-CL')}</td></tr>
    <tr><td>Stock anterior</td><td>${rep.stockActual} ${rep.unidad}</td></tr>
    <tr><td>Stock resultante</td><td class="valor">${repActualizado.stockActual} ${rep.unidad}</td></tr>
    <tr><td>Motivo</td><td>${dto.motivo ?? '—'}</td></tr>
    <tr><td>Referencia</td><td>${dto.referencia ?? '—'}</td></tr>
    <tr><td>Fecha</td><td>${fecha}</td></tr>
  </table>

  <div class="firma">
    <div class="firma-bloque">
      <div class="firma-linea"></div>
      <div>Supervisor de Taller</div>
    </div>
    <div class="firma-bloque">
      <div class="firma-linea"></div>
      <div>Jefe de Bodega</div>
    </div>
  </div>

  <script>window.print();<\/script>
</body>
</html>`;

    const ventana = window.open('', '_blank', 'width=800,height=600');
    ventana?.document.write(html);
    ventana?.document.close();
  }
}
