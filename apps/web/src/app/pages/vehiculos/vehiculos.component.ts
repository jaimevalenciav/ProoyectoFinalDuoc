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
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { forkJoin } from 'rxjs';
import { VehiculosService } from '@core/services/vehiculos.service';
import { VehiculosMaestrosService } from '@core/services/vehiculos-maestros.service';
import { TallerService } from '@core/services/taller.service';
import { DialogoService } from '@core/services/dialogo.service';
import { PerfilService } from '@core/services/perfil.service';
import {
  Vehiculo, Sucursal, Municipalidad, Aseguradora, PlantaRevision,
  PermisoCirculacion, SeguroSoap, RevisionTecnica, OrdenTrabajo
} from '@core/models';
import { VehiculoQrPrintComponent } from './vehiculo-qr-print.component';

@Component({
  selector: 'app-vehiculos',
  standalone: true,
  imports: [
    CommonModule, FormsModule, ReactiveFormsModule,
    MatTableModule, MatButtonModule, MatIconModule,
    MatInputModule, MatSelectModule, MatProgressSpinnerModule,
    MatSnackBarModule, MatDialogModule, MatTooltipModule,
    MatSlideToggleModule,
  ],
  template: `
    <div class="encabezado-pagina">
      <h1>Vehículos</h1>
      @if (puedeEscribir()) {
        <button mat-flat-button class="btn-principal" (click)="abrirFormulario()">
          <mat-icon>add</mat-icon> Nuevo vehículo
        </button>
      }
    </div>

    <!-- Filtros -->
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
            <td mat-cell *matCellDef="let v"><span class="celda-placa">{{ v.patente }}</span></td>
          </ng-container>
          <ng-container matColumnDef="marcaModelo">
            <th mat-header-cell *matHeaderCellDef>Marca / Modelo</th>
            <td mat-cell *matCellDef="let v">
              {{ v.marca }} {{ v.modelo }}
              <span class="texto-atenuado">({{ v.anio }})</span>
              @if (v.condicion) {
                <span class="badge-condicion" [class.usado]="v.condicion==='USADO'">{{ v.condicion }}</span>
              }
            </td>
          </ng-container>
          <ng-container matColumnDef="tipo">
            <th mat-header-cell *matHeaderCellDef>Tipo</th>
            <td mat-cell *matCellDef="let v">{{ v.tipo }}</td>
          </ng-container>
          <ng-container matColumnDef="estado">
            <th mat-header-cell *matHeaderCellDef>Estado</th>
            <td mat-cell *matCellDef="let v">
              <span [class]="insigniaEstado(v.estadoOperacion ?? v.estado)">
                {{ etiquetaEstado(v.estadoOperacion ?? v.estado) }}
              </span>
            </td>
          </ng-container>
          <ng-container matColumnDef="kilometraje">
            <th mat-header-cell *matHeaderCellDef>Km actuales</th>
            <td mat-cell *matCellDef="let v">{{ v.kmActuales | number:'1.0-0' }} km</td>
          </ng-container>
          <ng-container matColumnDef="vencimientos">
            <th mat-header-cell *matHeaderCellDef>Vencimientos</th>
            <td mat-cell *matCellDef="let v">
              <div style="display:flex;gap:4px;flex-wrap:wrap">
                @if (v.vencimientoRevision) {
                  <span [class]="claseVencimiento(v.vencimientoRevision)" matTooltip="Rev. Técnica">
                    RT {{ v.vencimientoRevision | date:'MM/yy' }}
                  </span>
                }
                @if (v.vencimientoPermiso) {
                  <span [class]="claseVencimiento(v.vencimientoPermiso)" matTooltip="Permiso Circulación">
                    PC {{ v.vencimientoPermiso | date:'MM/yy' }}
                  </span>
                }
              </div>
            </td>
          </ng-container>
          <ng-container matColumnDef="acciones">
            <th mat-header-cell *matHeaderCellDef></th>
            <td mat-cell *matCellDef="let v" class="columna-acciones">
              <button mat-icon-button (click)="imprimirQr(v)" matTooltip="Imprimir QR" style="color:#007AF5">
                <mat-icon>qr_code_2</mat-icon>
              </button>
              @if (puedeEscribir()) {
                <button mat-icon-button (click)="abrirFormulario(v)" matTooltip="Editar">
                  <mat-icon>edit</mat-icon>
                </button>
                <button mat-icon-button color="warn" (click)="eliminar(v)" matTooltip="Eliminar">
                  <mat-icon>delete_outline</mat-icon>
                </button>
              }
            </td>
          </ng-container>
          <tr mat-header-row *matHeaderRowDef="columnas"></tr>
          <tr mat-row *matRowDef="let fila; columns: columnas;"
              [class.fila-clickable]="puedeEscribir()"
              (click)="puedeEscribir() && abrirFormulario(fila)"></tr>
        </table>
        @if (vehiculos().length === 0) {
          <div class="estado-vacio-tabla">
            <mat-icon>directions_bus</mat-icon>
            <p>No hay vehículos registrados.</p>
          </div>
        }
      </div>
    }

    <!-- ═══════════ MODAL DETALLE / EDICIÓN ═══════════ -->
    @if (mostrarFormulario()) {
      <div class="capa-modal" (click)="cerrarFormulario()">
        <div class="panel-detalle" (click)="$event.stopPropagation()">

          <!-- Cabecera modal -->
          <div class="detalle-cabecera">
            <div>
              <h2 style="margin:0">{{ idEdicion() ? (vehiculoEdicion()?.patente + ' — ' + vehiculoEdicion()?.marca + ' ' + vehiculoEdicion()?.modelo) : 'Nuevo vehículo' }}</h2>
              @if (idEdicion()) {
                <span class="texto-atenuado" style="font-size:12px">{{ vehiculoEdicion()?.anio }} · {{ vehiculoEdicion()?.tipo }}</span>
              }
            </div>
            <button mat-icon-button (click)="cerrarFormulario()"><mat-icon>close</mat-icon></button>
          </div>

          <div class="detalle-cuerpo">
            <form [formGroup]="formulario" (ngSubmit)="guardar()">

              <!-- ── SECCIÓN: Identificación ── -->
              <div class="seccion-titulo"><mat-icon>badge</mat-icon> Identificación</div>
              <div class="grid-3">
                <mat-form-field appearance="fill">
                  <mat-label>Patente *</mat-label>
                  <input matInput formControlName="patente" placeholder="ABCD-12" />
                </mat-form-field>
                <mat-form-field appearance="fill">
                  <mat-label>Marca *</mat-label>
                  <input matInput formControlName="marca" />
                </mat-form-field>
                <mat-form-field appearance="fill">
                  <mat-label>Modelo *</mat-label>
                  <input matInput formControlName="modelo" />
                </mat-form-field>
              </div>
              <div class="grid-4">
                <mat-form-field appearance="fill">
                  <mat-label>Año *</mat-label>
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
                <mat-form-field appearance="fill">
                  <mat-label>Condición</mat-label>
                  <mat-select formControlName="condicion">
                    <mat-option value="NUEVO">Nuevo</mat-option>
                    <mat-option value="USADO">Usado</mat-option>
                  </mat-select>
                </mat-form-field>
                <mat-form-field appearance="fill">
                  <mat-label>País de origen</mat-label>
                  <input matInput formControlName="paisOrigen" placeholder="Ej: Suecia, Alemania" />
                </mat-form-field>
              </div>

              <!-- ── SECCIÓN: Estado y Operación ── -->
              <div class="seccion-titulo"><mat-icon>settings_suggest</mat-icon> Estado y Operación</div>
              <div class="grid-3">
                <mat-form-field appearance="fill">
                  <mat-label>Estado operación</mat-label>
                  <mat-select formControlName="estadoOperacion">
                    <mat-option value="EN_OPERACION">En Operación</mat-option>
                    <mat-option value="EN_MANTENCION">En Mantención</mat-option>
                    <mat-option value="FUERA_SERVICIO">Fuera de Servicio</mat-option>
                    <mat-option value="BAJA">Baja</mat-option>
                  </mat-select>
                </mat-form-field>
                <mat-form-field appearance="fill">
                  <mat-label>Sucursal</mat-label>
                  <mat-select formControlName="sucursalId">
                    <mat-option value="">Sin asignar</mat-option>
                    @for (s of sucursales(); track s.id) {
                      <mat-option [value]="s.id">{{ s.nombre }}</mat-option>
                    }
                  </mat-select>
                </mat-form-field>
                <mat-form-field appearance="fill">
                  <mat-label>Km actuales</mat-label>
                  <input matInput inputmode="numeric" formControlName="kmActuales"
                         (focus)="enFoco($event)" (blur)="alSalir($event,'kmActuales')" />
                  <span matSuffix class="sufijo-unidad">km</span>
                </mat-form-field>
              </div>

              <!-- ── SECCIÓN: Combustible y Motor ── -->
              <div class="seccion-titulo"><mat-icon>local_gas_station</mat-icon> Combustible y Motor</div>
              <div class="grid-4">
                <mat-form-field appearance="fill">
                  <mat-label>Combustible</mat-label>
                  <mat-select formControlName="combustible">
                    <mat-option value="DIESEL">Diésel</mat-option>
                    <mat-option value="BENCINA">Bencina</mat-option>
                    <mat-option value="GNC">GNC</mat-option>
                    <mat-option value="ELECTRICO">Eléctrico</mat-option>
                    <mat-option value="HIDROGENO">Hidrógeno</mat-option>
                  </mat-select>
                </mat-form-field>
                <mat-form-field appearance="fill">
                  <mat-label>Norma Euro</mat-label>
                  <mat-select formControlName="normaEuro">
                    <mat-option value="">Sin especificar</mat-option>
                    <mat-option value="EURO_III">Euro III</mat-option>
                    <mat-option value="EURO_IV">Euro IV</mat-option>
                    <mat-option value="EURO_V">Euro V</mat-option>
                    <mat-option value="EURO_VI">Euro VI</mat-option>
                  </mat-select>
                </mat-form-field>
                <mat-form-field appearance="fill">
                  <mat-label>Color</mat-label>
                  <input matInput formControlName="color" />
                </mat-form-field>
                <mat-form-field appearance="fill">
                  <mat-label>Nº Motor</mat-label>
                  <input matInput formControlName="numMotor" />
                </mat-form-field>
              </div>
              <div class="grid-4" style="margin-top:8px">
                <mat-form-field appearance="fill">
                  <mat-label>Nº Chasis</mat-label>
                  <input matInput formControlName="numChasis" />
                </mat-form-field>
              </div>
              <!-- AdBlue toggle — debajo de Nº Chasis -->
              <div class="adblue-toggle-row">
                <mat-slide-toggle formControlName="usaAdBlue" color="primary">
                  Usa AdBlue (sistema SCR / reductor de emisiones NOx)
                </mat-slide-toggle>
                @if (formulario.value.usaAdBlue) {
                  <span class="adblue-badge"><mat-icon>opacity</mat-icon> AdBlue habilitado — se podrán registrar recargas en el módulo Operaciones</span>
                }
              </div>

              <!-- ── SECCIÓN: Especificaciones ── -->
              <div class="seccion-titulo"><mat-icon>monitor_weight</mat-icon> Especificaciones Técnicas</div>
              <div class="grid-3">
                <mat-form-field appearance="fill">
                  <mat-label>Capacidad estanque (L)</mat-label>
                  <input matInput inputmode="numeric" formControlName="capacidadEstanque"
                         (focus)="enFoco($event)" (blur)="alSalir($event,'capacidadEstanque')" />
                  <span matSuffix class="sufijo-unidad">L</span>
                </mat-form-field>
                <mat-form-field appearance="fill">
                  <mat-label>Tara (kg)</mat-label>
                  <input matInput inputmode="numeric" formControlName="taraKg"
                         (focus)="enFoco($event)" (blur)="alSalir($event,'taraKg')" />
                  <span matSuffix class="sufijo-unidad">kg</span>
                </mat-form-field>
                <mat-form-field appearance="fill">
                  <mat-label>Cap. carga (kg)</mat-label>
                  <input matInput inputmode="numeric" formControlName="capacidadCargaKg"
                         (focus)="enFoco($event)" (blur)="alSalir($event,'capacidadCargaKg')" />
                  <span matSuffix class="sufijo-unidad">kg</span>
                </mat-form-field>
              </div>

              <!-- ── SECCIÓN: Compra y Antigüedad ── -->
              <div class="seccion-titulo"><mat-icon>receipt_long</mat-icon> Compra y Antigüedad</div>
              <div class="grid-3">
                <mat-form-field appearance="fill">
                  <mat-label>Valor de compra</mat-label>
                  <input matInput inputmode="numeric" formControlName="valorCompra"
                         (focus)="enFoco($event)" (blur)="alSalir($event,'valorCompra')" />
                  <span matPrefix class="prefijo-unidad">$&nbsp;</span>
                </mat-form-field>
                <mat-form-field appearance="fill">
                  <mat-label>Fecha de compra</mat-label>
                  <input matInput type="date" formControlName="fechaCompra" />
                </mat-form-field>
                <div class="bloque-antiguedad">
                  @if (antiguedadFabricacion() !== null) {
                    <div class="dato-antiguedad">
                      <mat-icon>calendar_today</mat-icon>
                      <div>
                        <span class="etq-dato">Antigüedad del vehículo</span>
                        <strong>{{ antiguedadFabricacion() }} año{{ antiguedadFabricacion() !== 1 ? 's' : '' }}</strong>
                        <span class="texto-atenuado">(desde {{ formulario.value.anio }})</span>
                      </div>
                    </div>
                  }
                  @if (antiguedadPosesion()) {
                    <div class="dato-antiguedad">
                      <mat-icon>history</mat-icon>
                      <div>
                        <span class="etq-dato">Antigüedad en posesión</span>
                        <strong>{{ antiguedadPosesion() }}</strong>
                      </div>
                    </div>
                  }
                </div>
              </div>

              <!-- Botones formulario básico -->
              <div class="acciones-formulario" style="margin-bottom:8px">
                <button mat-button type="button" (click)="cerrarFormulario()">Cancelar</button>
                <button mat-flat-button class="btn-principal" type="submit" [disabled]="formulario.invalid || guardando()">
                  {{ guardando() ? 'Guardando…' : 'Guardar datos' }}
                </button>
              </div>
            </form>

            <!-- ═══ DOCUMENTOS LEGALES (solo en edición) ═══ -->
            @if (idEdicion()) {
              <!-- ── Permiso Circulación ── -->
              <div class="seccion-doc">
                <div class="doc-cabecera">
                  <div class="seccion-titulo" style="margin:0"><mat-icon>description</mat-icon> Permiso de Circulación</div>
                  <button mat-stroked-button (click)="mostrarAddPermiso.set(!mostrarAddPermiso())">
                    <mat-icon>{{ mostrarAddPermiso() ? 'expand_less' : 'add' }}</mat-icon>
                    {{ mostrarAddPermiso() ? 'Cancelar' : 'Agregar' }}
                  </button>
                </div>

                @if (mostrarAddPermiso()) {
                  <div class="mini-form">
                    <div class="grid-3">
                      <mat-form-field appearance="fill">
                        <mat-label>Municipalidad</mat-label>
                        <mat-select [(ngModel)]="frmPermiso.municipalidadId">
                          <mat-option value="">Sin especificar</mat-option>
                          @for (m of municipalidades(); track m.id) {
                            <mat-option [value]="m.id">{{ m.nombre }}</mat-option>
                          }
                        </mat-select>
                      </mat-form-field>
                      <mat-form-field appearance="fill">
                        <mat-label>Fecha pago</mat-label>
                        <input matInput type="date" [(ngModel)]="frmPermiso.fechaPago" />
                      </mat-form-field>
                      <mat-form-field appearance="fill">
                        <mat-label>Nº documento</mat-label>
                        <input matInput [(ngModel)]="frmPermiso.documento" />
                      </mat-form-field>
                    </div>
                    <div class="grid-3">
                      <mat-form-field appearance="fill">
                        <mat-label>Valor</mat-label>
                        <input matInput type="number" [(ngModel)]="frmPermiso.valor" />
                        <span matPrefix>$&nbsp;</span>
                      </mat-form-field>
                      <mat-form-field appearance="fill">
                        <mat-label>Fecha vencimiento</mat-label>
                        <input matInput type="date" [(ngModel)]="frmPermiso.fechaVencimiento" />
                      </mat-form-field>
                      <div style="display:flex;align-items:center">
                        <button mat-flat-button class="btn-principal" (click)="guardarPermiso()" [disabled]="guardandoDoc()">
                          Registrar permiso
                        </button>
                      </div>
                    </div>
                  </div>
                }

                <table mat-table [dataSource]="permisosCirculacion()" class="tabla-doc">
                  <ng-container matColumnDef="vigente">
                    <th mat-header-cell *matHeaderCellDef>Vigente</th>
                    <td mat-cell *matCellDef="let p; let i = index">
                      @if (i === 0) { <mat-icon style="color:#16a34a;font-size:18px">check_circle</mat-icon> }
                      @else { <mat-icon style="color:#94a3b8;font-size:16px">history</mat-icon> }
                    </td>
                  </ng-container>
                  <ng-container matColumnDef="municipalidad">
                    <th mat-header-cell *matHeaderCellDef>Municipalidad</th>
                    <td mat-cell *matCellDef="let p">{{ nombreMunicipalidad(p.municipalidadId) }}</td>
                  </ng-container>
                  <ng-container matColumnDef="fechaPago">
                    <th mat-header-cell *matHeaderCellDef>Fecha pago</th>
                    <td mat-cell *matCellDef="let p">{{ p.fechaPago | date:'dd/MM/yyyy' }}</td>
                  </ng-container>
                  <ng-container matColumnDef="valor">
                    <th mat-header-cell *matHeaderCellDef>Valor</th>
                    <td mat-cell *matCellDef="let p">{{ p.valor | currency:'CLP':'$':'1.0-0' }}</td>
                  </ng-container>
                  <ng-container matColumnDef="vencimiento">
                    <th mat-header-cell *matHeaderCellDef>Vence</th>
                    <td mat-cell *matCellDef="let p">
                      <span [class]="claseVencimiento(p.fechaVencimiento)">{{ p.fechaVencimiento | date:'dd/MM/yyyy' }}</span>
                    </td>
                  </ng-container>
                  <ng-container matColumnDef="doc">
                    <th mat-header-cell *matHeaderCellDef>Doc.</th>
                    <td mat-cell *matCellDef="let p"><span class="badge-mono">{{ p.documento ?? '—' }}</span></td>
                  </ng-container>
                  <ng-container matColumnDef="acc">
                    <th mat-header-cell *matHeaderCellDef></th>
                    <td mat-cell *matCellDef="let p">
                      <button mat-icon-button color="warn" (click)="eliminarPermiso(p)">
                        <mat-icon>delete_outline</mat-icon>
                      </button>
                    </td>
                  </ng-container>
                  <tr mat-header-row *matHeaderRowDef="colsPermiso"></tr>
                  <tr mat-row *matRowDef="let r; columns: colsPermiso;"></tr>
                </table>
                @if (permisosCirculacion().length === 0) {
                  <p class="sin-registros">Sin registros de permiso de circulación.</p>
                }
              </div>

              <!-- ── Seguro SOAP ── -->
              <div class="seccion-doc">
                <div class="doc-cabecera">
                  <div class="seccion-titulo" style="margin:0"><mat-icon>shield</mat-icon> Seguro SOAP</div>
                  <button mat-stroked-button (click)="mostrarAddSeguro.set(!mostrarAddSeguro())">
                    <mat-icon>{{ mostrarAddSeguro() ? 'expand_less' : 'add' }}</mat-icon>
                    {{ mostrarAddSeguro() ? 'Cancelar' : 'Agregar' }}
                  </button>
                </div>

                @if (mostrarAddSeguro()) {
                  <div class="mini-form">
                    <div class="grid-3">
                      <mat-form-field appearance="fill">
                        <mat-label>Aseguradora</mat-label>
                        <mat-select [(ngModel)]="frmSeguro.aseguradoraId">
                          <mat-option value="">Sin especificar</mat-option>
                          @for (a of aseguradoras(); track a.id) {
                            <mat-option [value]="a.id">{{ a.nombre }}</mat-option>
                          }
                        </mat-select>
                      </mat-form-field>
                      <mat-form-field appearance="fill">
                        <mat-label>Fecha emisión</mat-label>
                        <input matInput type="date" [(ngModel)]="frmSeguro.fechaEmision" />
                      </mat-form-field>
                      <mat-form-field appearance="fill">
                        <mat-label>Nº póliza</mat-label>
                        <input matInput [(ngModel)]="frmSeguro.poliza" />
                      </mat-form-field>
                    </div>
                    <div class="grid-3">
                      <mat-form-field appearance="fill">
                        <mat-label>Valor</mat-label>
                        <input matInput type="number" [(ngModel)]="frmSeguro.valor" />
                        <span matPrefix>$&nbsp;</span>
                      </mat-form-field>
                      <mat-form-field appearance="fill">
                        <mat-label>Fecha vencimiento</mat-label>
                        <input matInput type="date" [(ngModel)]="frmSeguro.fechaVencimiento" />
                      </mat-form-field>
                      <div style="display:flex;align-items:center">
                        <button mat-flat-button class="btn-principal" (click)="guardarSeguro()" [disabled]="guardandoDoc()">
                          Registrar seguro
                        </button>
                      </div>
                    </div>
                  </div>
                }

                <table mat-table [dataSource]="segurosSOAP()" class="tabla-doc">
                  <ng-container matColumnDef="vigente">
                    <th mat-header-cell *matHeaderCellDef>Vigente</th>
                    <td mat-cell *matCellDef="let s; let i = index">
                      @if (i === 0) { <mat-icon style="color:#16a34a;font-size:18px">check_circle</mat-icon> }
                      @else { <mat-icon style="color:#94a3b8;font-size:16px">history</mat-icon> }
                    </td>
                  </ng-container>
                  <ng-container matColumnDef="aseguradora">
                    <th mat-header-cell *matHeaderCellDef>Aseguradora</th>
                    <td mat-cell *matCellDef="let s">{{ nombreAseguradora(s.aseguradoraId) }}</td>
                  </ng-container>
                  <ng-container matColumnDef="poliza">
                    <th mat-header-cell *matHeaderCellDef>Póliza</th>
                    <td mat-cell *matCellDef="let s"><span class="badge-mono">{{ s.poliza ?? '—' }}</span></td>
                  </ng-container>
                  <ng-container matColumnDef="valor">
                    <th mat-header-cell *matHeaderCellDef>Valor</th>
                    <td mat-cell *matCellDef="let s">{{ s.valor | currency:'CLP':'$':'1.0-0' }}</td>
                  </ng-container>
                  <ng-container matColumnDef="vencimiento">
                    <th mat-header-cell *matHeaderCellDef>Vence</th>
                    <td mat-cell *matCellDef="let s">
                      <span [class]="claseVencimiento(s.fechaVencimiento)">{{ s.fechaVencimiento | date:'dd/MM/yyyy' }}</span>
                    </td>
                  </ng-container>
                  <ng-container matColumnDef="acc">
                    <th mat-header-cell *matHeaderCellDef></th>
                    <td mat-cell *matCellDef="let s">
                      <button mat-icon-button color="warn" (click)="eliminarSeguro(s)">
                        <mat-icon>delete_outline</mat-icon>
                      </button>
                    </td>
                  </ng-container>
                  <tr mat-header-row *matHeaderRowDef="colsSeguro"></tr>
                  <tr mat-row *matRowDef="let r; columns: colsSeguro;"></tr>
                </table>
                @if (segurosSOAP().length === 0) {
                  <p class="sin-registros">Sin registros de seguro SOAP.</p>
                }
              </div>

              <!-- ── Revisión Técnica ── -->
              <div class="seccion-doc">
                <div class="doc-cabecera">
                  <div class="seccion-titulo" style="margin:0"><mat-icon>fact_check</mat-icon> Revisión Técnica</div>
                  <button mat-stroked-button (click)="mostrarAddRevision.set(!mostrarAddRevision())">
                    <mat-icon>{{ mostrarAddRevision() ? 'expand_less' : 'add' }}</mat-icon>
                    {{ mostrarAddRevision() ? 'Cancelar' : 'Agregar' }}
                  </button>
                </div>

                @if (mostrarAddRevision()) {
                  <div class="mini-form">
                    <div class="grid-4">
                      <mat-form-field appearance="fill">
                        <mat-label>Planta</mat-label>
                        <mat-select [(ngModel)]="frmRevision.plantaId">
                          <mat-option value="">Sin especificar</mat-option>
                          @for (p of plantasRevision(); track p.id) {
                            <mat-option [value]="p.id">{{ p.nombre }}</mat-option>
                          }
                        </mat-select>
                      </mat-form-field>
                      <mat-form-field appearance="fill">
                        <mat-label>Fecha revisión</mat-label>
                        <input matInput type="date" [(ngModel)]="frmRevision.fechaRevision" />
                      </mat-form-field>
                      <mat-form-field appearance="fill">
                        <mat-label>Resultado</mat-label>
                        <mat-select [(ngModel)]="frmRevision.resultado">
                          <mat-option value="APROBADO">Aprobado</mat-option>
                          <mat-option value="CONDICIONADO">Condicionado</mat-option>
                          <mat-option value="RECHAZADO">Rechazado</mat-option>
                        </mat-select>
                      </mat-form-field>
                      <mat-form-field appearance="fill">
                        <mat-label>Valor</mat-label>
                        <input matInput type="number" [(ngModel)]="frmRevision.valor" />
                        <span matPrefix>$&nbsp;</span>
                      </mat-form-field>
                    </div>
                    <div class="grid-3">
                      <mat-form-field appearance="fill">
                        <mat-label>Fecha vencimiento</mat-label>
                        <input matInput type="date" [(ngModel)]="frmRevision.fechaVencimiento" />
                      </mat-form-field>
                      <div></div>
                      <div style="display:flex;align-items:center">
                        <button mat-flat-button class="btn-principal" (click)="guardarRevision()" [disabled]="guardandoDoc()">
                          Registrar revisión
                        </button>
                      </div>
                    </div>
                  </div>
                }

                <table mat-table [dataSource]="revisionesTecnicas()" class="tabla-doc">
                  <ng-container matColumnDef="vigente">
                    <th mat-header-cell *matHeaderCellDef>Vigente</th>
                    <td mat-cell *matCellDef="let r; let i = index">
                      @if (i === 0) { <mat-icon style="color:#16a34a;font-size:18px">check_circle</mat-icon> }
                      @else { <mat-icon style="color:#94a3b8;font-size:16px">history</mat-icon> }
                    </td>
                  </ng-container>
                  <ng-container matColumnDef="planta">
                    <th mat-header-cell *matHeaderCellDef>Planta</th>
                    <td mat-cell *matCellDef="let r">{{ nombrePlanta(r.plantaId) }}</td>
                  </ng-container>
                  <ng-container matColumnDef="fecha">
                    <th mat-header-cell *matHeaderCellDef>Fecha</th>
                    <td mat-cell *matCellDef="let r">{{ r.fechaRevision | date:'dd/MM/yyyy' }}</td>
                  </ng-container>
                  <ng-container matColumnDef="resultado">
                    <th mat-header-cell *matHeaderCellDef>Resultado</th>
                    <td mat-cell *matCellDef="let r">
                      <span [class]="claseResultado(r.resultado)">{{ r.resultado ?? '—' }}</span>
                    </td>
                  </ng-container>
                  <ng-container matColumnDef="valor">
                    <th mat-header-cell *matHeaderCellDef>Valor</th>
                    <td mat-cell *matCellDef="let r">{{ r.valor | currency:'CLP':'$':'1.0-0' }}</td>
                  </ng-container>
                  <ng-container matColumnDef="vencimiento">
                    <th mat-header-cell *matHeaderCellDef>Vence</th>
                    <td mat-cell *matCellDef="let r">
                      <span [class]="claseVencimiento(r.fechaVencimiento)">{{ r.fechaVencimiento | date:'dd/MM/yyyy' }}</span>
                    </td>
                  </ng-container>
                  <ng-container matColumnDef="acc">
                    <th mat-header-cell *matHeaderCellDef></th>
                    <td mat-cell *matCellDef="let r">
                      <button mat-icon-button color="warn" (click)="eliminarRevision(r)">
                        <mat-icon>delete_outline</mat-icon>
                      </button>
                    </td>
                  </ng-container>
                  <tr mat-header-row *matHeaderRowDef="colsRevision"></tr>
                  <tr mat-row *matRowDef="let r; columns: colsRevision;"></tr>
                </table>
                @if (revisionesTecnicas().length === 0) {
                  <p class="sin-registros">Sin registros de revisión técnica.</p>
                }
              </div>

              <!-- ── Historial Taller ── -->
              <div class="seccion-doc">
                <div class="seccion-titulo"><mat-icon>build</mat-icon> Historial de Mantenimiento (Taller)</div>
                @if (cargandoTaller()) {
                  <div style="padding:16px;text-align:center"><mat-spinner diameter="30" /></div>
                } @else if (historialTaller().length === 0) {
                  <p class="sin-registros">Sin órdenes de trabajo para este vehículo.</p>
                } @else {
                  <table mat-table [dataSource]="historialTaller()" class="tabla-doc">
                    <ng-container matColumnDef="numero">
                      <th mat-header-cell *matHeaderCellDef>OT</th>
                      <td mat-cell *matCellDef="let ot"><span class="badge-mono">{{ ot.numero }}</span></td>
                    </ng-container>
                    <ng-container matColumnDef="fecha">
                      <th mat-header-cell *matHeaderCellDef>Fecha apertura</th>
                      <td mat-cell *matCellDef="let ot">{{ ot.fechaApertura | date:'dd/MM/yyyy' }}</td>
                    </ng-container>
                    <ng-container matColumnDef="tipo">
                      <th mat-header-cell *matHeaderCellDef>Tipo</th>
                      <td mat-cell *matCellDef="let ot">{{ ot.tipo }}</td>
                    </ng-container>
                    <ng-container matColumnDef="descripcion">
                      <th mat-header-cell *matHeaderCellDef>Descripción</th>
                      <td mat-cell *matCellDef="let ot">{{ ot.descripcion }}</td>
                    </ng-container>
                    <ng-container matColumnDef="mecanico">
                      <th mat-header-cell *matHeaderCellDef>Mecánico</th>
                      <td mat-cell *matCellDef="let ot">{{ ot.mecanicoResponsable ?? '—' }}</td>
                    </ng-container>
                    <ng-container matColumnDef="estadoOT">
                      <th mat-header-cell *matHeaderCellDef>Estado</th>
                      <td mat-cell *matCellDef="let ot">
                        <span [class]="insigniaEstadoOT(ot.estado)">{{ ot.estado }}</span>
                      </td>
                    </ng-container>
                    <tr mat-header-row *matHeaderRowDef="colsTaller"></tr>
                    <tr mat-row *matRowDef="let r; columns: colsTaller;"></tr>
                  </table>
                }
              </div>
            }
          </div>
        </div>
      </div>
    }
  `,
  styles: [`
    .celda-placa { font-weight: 700; color: var(--azul-800); font-size: 14px; }
    .columna-acciones { white-space: nowrap; }
    .fila-clickable { cursor: pointer; }
    .fila-clickable:hover { background: var(--azul-50); }
    .badge-condicion {
      font-size: 10px; font-weight: 700; padding: 2px 6px; border-radius: 4px;
      background: #dcfce7; color: #166534; margin-left: 6px;
      &.usado { background: #fff7ed; color: #9a3412; }
    }
    .estado-vacio-tabla {
      display: flex; flex-direction: column; align-items: center; padding: 48px;
      gap: 8px; color: var(--color-texto-3);
      mat-icon { font-size: 48px; width: 48px; height: 48px; opacity: .3; }
    }

    /* Modal detalle */
    .panel-detalle {
      background: var(--color-superficie);
      border-radius: var(--radio-lg);
      width: min(900px, 96vw);
      max-height: 92vh;
      display: flex;
      flex-direction: column;
      box-shadow: 0 20px 60px rgba(0,0,0,.25);
    }
    .detalle-cabecera {
      display: flex; align-items: flex-start; justify-content: space-between;
      padding: 20px 24px 16px;
      border-bottom: 1px solid var(--color-borde);
      flex-shrink: 0;
    }
    .detalle-cuerpo {
      padding: 20px 24px 24px;
      overflow-y: auto;
      flex: 1;
    }

    /* Secciones */
    .seccion-titulo {
      display: flex; align-items: center; gap: 8px;
      font-size: 12px; font-weight: 700; color: var(--color-texto-2);
      text-transform: uppercase; letter-spacing: .5px;
      margin: 20px 0 12px;
      mat-icon { font-size: 16px; width: 16px; height: 16px; }
    }

    /* Grids */
    .grid-3 { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 12px; }
    .grid-4 { display: grid; grid-template-columns: 1fr 1fr 1fr 1fr; gap: 12px; }

    /* Antigüedad */
    .bloque-antiguedad {
      display: flex; flex-direction: column; gap: 8px;
      background: var(--azul-50); border: 1px solid var(--azul-200);
      border-radius: var(--radio-sm); padding: 10px 14px;
    }
    .dato-antiguedad {
      display: flex; align-items: center; gap: 8px; font-size: 13px;
      mat-icon { color: var(--azul-400); font-size: 18px; width: 18px; height: 18px; }
      div { display: flex; flex-direction: column; gap: 1px; }
    }
    .etq-dato { font-size: 10px; text-transform: uppercase; letter-spacing: .5px; color: var(--azul-400); }

    /* Sufijos y prefijos de unidad con padding */
    .sufijo-unidad {
      padding-right: 10px;
      color: var(--ink-soft);
      font-size: 13px;
      font-weight: 500;
    }
    .prefijo-unidad {
      padding-left: 10px;
      color: var(--ink-soft);
      font-size: 13px;
      font-weight: 500;
    }

    /* AdBlue toggle */
    .adblue-toggle-row {
      display: flex; align-items: center; gap: 16px; flex-wrap: wrap;
      background: var(--azul-50); border: 1px solid var(--azul-200);
      border-radius: var(--radio-sm); padding: 10px 14px; margin-top: 10px;
    }
    .adblue-badge {
      display: inline-flex; align-items: center; gap: 4px;
      font-size: 12px; font-weight: 600; color: #007AF5;
      mat-icon { font-size: 15px; width: 15px; height: 15px; }
    }

    /* Documentos legales */
    .seccion-doc {
      border: 1px solid var(--color-borde);
      border-radius: var(--radio-md);
      padding: 16px;
      margin-top: 20px;
    }
    .doc-cabecera {
      display: flex; justify-content: space-between; align-items: center;
      margin-bottom: 14px;
    }
    .mini-form {
      background: var(--azul-50); border-radius: var(--radio-sm);
      padding: 14px; margin-bottom: 14px;
      border: 1px solid var(--azul-200);
    }
    .tabla-doc { width: 100%; }
    .sin-registros {
      font-size: 13px; color: var(--color-texto-3);
      padding: 12px 0 4px; text-align: center; margin: 0;
    }
    .badge-mono { font-family: monospace; font-size: 12px; }

    /* Vencimientos */
    .badge-vence-ok  { background:#dcfce7;color:#166534;border-radius:4px;padding:2px 6px;font-size:11px;font-weight:600 }
    .badge-vence-warn{ background:#fff7ed;color:#9a3412;border-radius:4px;padding:2px 6px;font-size:11px;font-weight:600 }
    .badge-vence-exp { background:#fee2e2;color:#991b1b;border-radius:4px;padding:2px 6px;font-size:11px;font-weight:600 }
  `],
})
export class VehiculosComponent implements OnInit {
  private readonly servicio      = inject(VehiculosService);
  private readonly maestrosSvc   = inject(VehiculosMaestrosService);
  private readonly tallerSvc     = inject(TallerService);
  private readonly constructor_  = inject(FormBuilder);
  private readonly notificacion  = inject(MatSnackBar);
  private readonly dialog        = inject(MatDialog);
  private readonly dialogo       = inject(DialogoService);
  readonly perfil                = inject(PerfilService);

  readonly puedeEscribir = computed(() => this.perfil.puedeEscribir('vehiculos'));

  columnas = ['patente', 'marcaModelo', 'tipo', 'estado', 'kilometraje', 'vencimientos', 'acciones'];
  colsPermiso  = ['vigente', 'municipalidad', 'fechaPago', 'valor', 'vencimiento', 'doc', 'acc'];
  colsSeguro   = ['vigente', 'aseguradora', 'poliza', 'valor', 'vencimiento', 'acc'];
  colsRevision = ['vigente', 'planta', 'fecha', 'resultado', 'valor', 'vencimiento', 'acc'];
  colsTaller   = ['numero', 'fecha', 'tipo', 'descripcion', 'mecanico', 'estadoOT'];

  // Estado principal
  cargando          = signal(true);
  guardando         = signal(false);
  guardandoDoc      = signal(false);
  cargandoTaller    = signal(false);
  mostrarFormulario = signal(false);
  idEdicion         = signal<string | null>(null);
  vehiculoEdicion   = signal<Vehiculo | null>(null);

  // Datos
  vehiculos           = signal<Vehiculo[]>([]);
  sucursales          = signal<Sucursal[]>([]);
  municipalidades     = signal<Municipalidad[]>([]);
  aseguradoras        = signal<Aseguradora[]>([]);
  plantasRevision     = signal<PlantaRevision[]>([]);
  permisosCirculacion = signal<PermisoCirculacion[]>([]);
  segurosSOAP         = signal<SeguroSoap[]>([]);
  revisionesTecnicas  = signal<RevisionTecnica[]>([]);
  historialTaller     = signal<OrdenTrabajo[]>([]);

  // Toggle mini-formularios documentos
  mostrarAddPermiso  = signal(false);
  mostrarAddSeguro   = signal(false);
  mostrarAddRevision = signal(false);

  // Mini-forms de documentos
  frmPermiso  = { municipalidadId: '', fechaPago: '', valor: null as number | null, fechaVencimiento: '', documento: '' };
  frmSeguro   = { aseguradoraId: '', fechaEmision: '', valor: null as number | null, fechaVencimiento: '', poliza: '' };
  frmRevision = { plantaId: '', fechaRevision: '', valor: null as number | null, fechaVencimiento: '', resultado: 'APROBADO' };

  // Filtros lista
  busqueda     = '';
  filtroEstado = '';

  // Formulario vehículo
  formulario = this.constructor_.group({
    patente:           ['', [Validators.required]],
    marca:             ['', Validators.required],
    modelo:            ['', Validators.required],
    anio:              [new Date().getFullYear(), Validators.required],
    tipo:              ['CAMION'],
    condicion:         ['USADO'],
    paisOrigen:        [''],
    estadoOperacion:   ['EN_OPERACION'],
    sucursalId:        [''],
    combustible:       ['DIESEL'],
    color:             [''],
    numMotor:          [''],
    numChasis:         [''],
    kmActuales:        [0],
    kmProximoServicio: [null as number | null],
    capacidadEstanque: [null as number | null],
    taraKg:            [null as number | null],
    capacidadCargaKg:  [null as number | null],
    valorCompra:       [null as number | null],
    fechaCompra:       [''],
    usaAdBlue:         [false],
    normaEuro:         [''],
  });

  // Antigüedad calculada
  antiguedadFabricacion = computed((): number | null => {
    const anio = Number(this.formulario.value.anio);
    if (!anio) return null;
    return new Date().getFullYear() - anio;
  });

  antiguedadPosesion = computed((): string | null => {
    const fecha = this.formulario.value.fechaCompra;
    if (!fecha) return null;
    const inicio = new Date(fecha);
    const hoy = new Date();
    const meses = (hoy.getFullYear() - inicio.getFullYear()) * 12 + (hoy.getMonth() - inicio.getMonth());
    if (meses <= 0) return null;
    const anios = Math.floor(meses / 12);
    const rest  = meses % 12;
    if (anios === 0) return `${rest} mes${rest !== 1 ? 'es' : ''}`;
    return `${anios} año${anios > 1 ? 's' : ''} ${rest} mes${rest !== 1 ? 'es' : ''}`;
  });

  ngOnInit() {
    this.cargar();
    // Auto-mayúsculas
    (['patente', 'marca', 'modelo'] as const).forEach(campo => {
      this.formulario.get(campo)?.valueChanges.subscribe(v => {
        if (v && v !== v.toUpperCase())
          this.formulario.get(campo)?.setValue(v.toUpperCase(), { emitEvent: false });
      });
    });
    // Cargar maestros
    this.maestrosSvc.getSucursales().subscribe(r => this.sucursales.set(r));
    this.maestrosSvc.getMunicipalidades().subscribe(r => this.municipalidades.set(r));
    this.maestrosSvc.getAseguradoras().subscribe(r => this.aseguradoras.set(r));
    this.maestrosSvc.getPlantasRevision().subscribe(r => this.plantasRevision.set(r));
  }

  cargar() {
    this.cargando.set(true);
    this.servicio.getAll({ search: this.busqueda || undefined, estado: this.filtroEstado || undefined })
      .subscribe({
        next: r => { this.vehiculos.set(r.content); this.cargando.set(false); },
        error: ()  => this.cargando.set(false),
      });
  }

  abrirFormulario(v?: Vehiculo) {
    this.idEdicion.set(v?.id ?? null);
    this.vehiculoEdicion.set(v ?? null);
    if (v) {
      this.formulario.patchValue({
        ...v,
        fechaCompra: v.fechaCompra ? v.fechaCompra.substring(0, 10) : '',
        sucursalId:  v.sucursalId ?? '',
        usaAdBlue:   !!v.usaAdBlue,
        normaEuro:   v.normaEuro ?? '',
      } as any);
      // Cargar documentos del vehículo
      this.permisosCirculacion.set([]); this.segurosSOAP.set([]); this.revisionesTecnicas.set([]); this.historialTaller.set([]);
      this.mostrarAddPermiso.set(false); this.mostrarAddSeguro.set(false); this.mostrarAddRevision.set(false);
      forkJoin([
        this.maestrosSvc.getPermisosCirculacion(v.id),
        this.maestrosSvc.getSegurosSOAP(v.id),
        this.maestrosSvc.getRevisionesTecnicas(v.id),
      ]).subscribe(([permisos, seguros, revisiones]) => {
        this.permisosCirculacion.set(permisos);
        this.segurosSOAP.set(seguros);
        this.revisionesTecnicas.set(revisiones);
      });
      this.cargandoTaller.set(true);
      this.tallerSvc.getAll({ vehiculoId: v.id, size: 50 }).subscribe({
        next: r => { this.historialTaller.set(r.content); this.cargandoTaller.set(false); },
        error: ()  => this.cargandoTaller.set(false),
      });
    } else {
      this.formulario.reset({
        anio: new Date().getFullYear(), tipo: 'CAMION', condicion: 'USADO',
        estadoOperacion: 'EN_OPERACION', combustible: 'DIESEL', kmActuales: 0,
        usaAdBlue: false, normaEuro: '',
      });
      this.permisosCirculacion.set([]); this.segurosSOAP.set([]); this.revisionesTecnicas.set([]); this.historialTaller.set([]);
    }
    this.mostrarFormulario.set(true);
    this.formatearCamposNumericos();
  }

  cerrarFormulario() { this.mostrarFormulario.set(false); }

  guardar() {
    if (this.formulario.invalid) return;
    this.guardando.set(true);
    const v = this.formulario.value as any;
    // Normalizar campos numéricos que pueden venir como string formateado
    const parseMiles = (val: any) => {
      if (val == null || val === '') return null;
      if (typeof val === 'string') return parseFloat(val.replace(/\./g, '').replace(/,/g, '')) || null;
      return val;
    };
    const solicitud = {
      ...v,
      usaAdBlue:          v.usaAdBlue ? 1 : 0,
      kmActuales:         parseMiles(v.kmActuales)         ?? 0,
      capacidadEstanque:  parseMiles(v.capacidadEstanque),
      taraKg:             parseMiles(v.taraKg),
      capacidadCargaKg:   parseMiles(v.capacidadCargaKg),
      valorCompra:        parseMiles(v.valorCompra),
    };
    const operacion = this.idEdicion()
      ? this.servicio.update(this.idEdicion()!, solicitud)
      : this.servicio.create(solicitud);
    operacion.subscribe({
      next: (v) => {
        this.guardando.set(false);
        this.idEdicion.set(v.id);
        this.vehiculoEdicion.set(v);
        this.cargar();
        this.notificacion.open('Guardado correctamente', '', { duration: 3000 });
      },
      error: () => { this.guardando.set(false); this.notificacion.open('Error al guardar', '', { duration: 3000 }); },
    });
  }

  // ── Documentos ───────────────────────────────────────────────

  guardarPermiso() {
    const vid = this.idEdicion();
    if (!vid) return;
    this.guardandoDoc.set(true);
    this.maestrosSvc.createPermisoCirculacion({ ...this.frmPermiso, vehiculoId: vid } as any).subscribe({
      next: () => {
        this.guardandoDoc.set(false);
        this.mostrarAddPermiso.set(false);
        this.frmPermiso = { municipalidadId: '', fechaPago: '', valor: null, fechaVencimiento: '', documento: '' };
        this.maestrosSvc.getPermisosCirculacion(vid).subscribe(r => this.permisosCirculacion.set(r));
        this.notificacion.open('Permiso registrado', '', { duration: 2500 });
      },
      error: () => { this.guardandoDoc.set(false); this.notificacion.open('Error al registrar', '', { duration: 3000 }); },
    });
  }

  async eliminarPermiso(p: PermisoCirculacion) {
    const ok = await this.dialogo.confirmarEliminar('¿Eliminar este permiso de circulación?');
    if (!ok) return;
    this.maestrosSvc.deletePermisoCirculacion(p.id).subscribe({
      next: () => this.permisosCirculacion.update(l => l.filter(x => x.id !== p.id)),
    });
  }

  guardarSeguro() {
    const vid = this.idEdicion();
    if (!vid) return;
    this.guardandoDoc.set(true);
    this.maestrosSvc.createSeguroSOAP({ ...this.frmSeguro, vehiculoId: vid } as any).subscribe({
      next: () => {
        this.guardandoDoc.set(false);
        this.mostrarAddSeguro.set(false);
        this.frmSeguro = { aseguradoraId: '', fechaEmision: '', valor: null, fechaVencimiento: '', poliza: '' };
        this.maestrosSvc.getSegurosSOAP(vid).subscribe(r => this.segurosSOAP.set(r));
        this.notificacion.open('Seguro SOAP registrado', '', { duration: 2500 });
      },
      error: () => { this.guardandoDoc.set(false); this.notificacion.open('Error al registrar', '', { duration: 3000 }); },
    });
  }

  async eliminarSeguro(s: SeguroSoap) {
    const ok = await this.dialogo.confirmarEliminar('¿Eliminar este registro de seguro SOAP?');
    if (!ok) return;
    this.maestrosSvc.deleteSeguroSOAP(s.id).subscribe({
      next: () => this.segurosSOAP.update(l => l.filter(x => x.id !== s.id)),
    });
  }

  guardarRevision() {
    const vid = this.idEdicion();
    if (!vid) return;
    this.guardandoDoc.set(true);
    this.maestrosSvc.createRevisionTecnica({ ...this.frmRevision, vehiculoId: vid } as any).subscribe({
      next: () => {
        this.guardandoDoc.set(false);
        this.mostrarAddRevision.set(false);
        this.frmRevision = { plantaId: '', fechaRevision: '', valor: null, fechaVencimiento: '', resultado: 'APROBADO' };
        this.maestrosSvc.getRevisionesTecnicas(vid).subscribe(r => this.revisionesTecnicas.set(r));
        this.notificacion.open('Revisión técnica registrada', '', { duration: 2500 });
      },
      error: () => { this.guardandoDoc.set(false); this.notificacion.open('Error al registrar', '', { duration: 3000 }); },
    });
  }

  async eliminarRevision(r: RevisionTecnica) {
    const ok = await this.dialogo.confirmarEliminar('¿Eliminar este registro de revisión técnica?');
    if (!ok) return;
    this.maestrosSvc.deleteRevisionTecnica(r.id).subscribe({
      next: () => this.revisionesTecnicas.update(l => l.filter(x => x.id !== r.id)),
    });
  }

  // ── Helpers ──────────────────────────────────────────────────

  imprimirQr(v: Vehiculo) {
    this.dialog.open(VehiculoQrPrintComponent, {
      data: { vehiculo: v }, width: '240mm', maxWidth: '98vw', maxHeight: '98vh', panelClass: 'dlg-qr-print',
    });
  }

  async eliminar(v: Vehiculo) {
    const ok = await this.dialogo.confirmarEliminar(
      `¿Eliminar el vehículo ${v.patente}?`,
      `${v.marca} ${v.modelo} · ${v.anio}`
    );
    if (!ok) return;
    this.servicio.delete(v.id).subscribe({ next: () => this.cargar() });
  }

  insigniaEstado(estado: string): string {
    const m: Record<string, string> = {
      OPERATIVO: 'insignia insignia-exito',
      EN_OPERACION: 'insignia insignia-exito',
      EN_TALLER: 'insignia insignia-advertencia',
      EN_MANTENCION: 'insignia insignia-advertencia',
      FUERA_SERVICIO: 'insignia insignia-peligro',
      BAJA: 'insignia insignia-neutro',
    };
    return m[estado] ?? 'insignia insignia-info';
  }

  etiquetaEstado(estado: string): string {
    const m: Record<string, string> = {
      EN_OPERACION: 'En Operación', EN_MANTENCION: 'En Mantención',
      FUERA_SERVICIO: 'Fuera Servicio', BAJA: 'Baja',
      OPERATIVO: 'Operativo', EN_TALLER: 'En Taller',
    };
    return m[estado] ?? estado;
  }

  claseVencimiento(fecha: string | undefined): string {
    if (!fecha) return '';
    const d = new Date(fecha);
    const hoy = new Date();
    const dias = Math.floor((d.getTime() - hoy.getTime()) / 86400000);
    if (dias < 0)   return 'badge-vence-exp';
    if (dias < 60)  return 'badge-vence-warn';
    return 'badge-vence-ok';
  }

  claseResultado(r: string | undefined): string {
    if (!r) return '';
    if (r === 'APROBADO')   return 'insignia insignia-exito';
    if (r === 'RECHAZADO')  return 'insignia insignia-peligro';
    return 'insignia insignia-advertencia';
  }

  insigniaEstadoOT(estado: string): string {
    const m: Record<string, string> = {
      PENDIENTE: 'insignia insignia-neutro', EN_EJECUCION: 'insignia insignia-azul',
      BLOQUEADA: 'insignia insignia-peligro', CERRADA: 'insignia insignia-exito',
    };
    return m[estado] ?? 'insignia insignia-info';
  }

  nombreMunicipalidad(id: string | undefined): string {
    return this.municipalidades().find(m => m.id === id)?.nombre ?? '—';
  }
  nombreAseguradora(id: string | undefined): string {
    return this.aseguradoras().find(a => a.id === id)?.nombre ?? '—';
  }
  nombrePlanta(id: string | undefined): string {
    return this.plantasRevision().find(p => p.id === id)?.nombre ?? '—';
  }

  // ── Formato de miles en inputs numéricos ─────────────────────

  /** Al enfocar el campo: muestra el número plano sin separadores */
  enFoco(event: FocusEvent) {
    const input = event.target as HTMLInputElement;
    const raw = input.value.replace(/\./g, '').replace(/,/g, '');
    input.value = raw || '';
    input.select();
  }

  /** Al salir del campo: formatea con separador de miles (es-CL) y guarda número en el form */
  alSalir(event: FocusEvent, campo: string) {
    const input = event.target as HTMLInputElement;
    const limpio = input.value.replace(/\./g, '').replace(/,/g, '').trim();
    const num = limpio !== '' ? parseFloat(limpio) : null;
    this.formulario.get(campo)?.setValue(num, { emitEvent: false });
    input.value = num != null && !isNaN(num)
      ? num.toLocaleString('es-CL')
      : '';
  }

  /** Inicializa el display formateado en el input al abrir el formulario */
  formatearCamposNumericos() {
    const campos = ['kmActuales','capacidadEstanque','taraKg','capacidadCargaKg','valorCompra'];
    campos.forEach(c => {
      const ctrl = this.formulario.get(c);
      if (ctrl?.value != null && ctrl.value !== '') {
        const num = Number(ctrl.value);
        if (!isNaN(num) && num > 0) {
          // Los inputs están en el DOM; actualizar vía timeout
          setTimeout(() => {
            const inputs = document.querySelectorAll(`[formcontrolname="${c}"]`);
            inputs.forEach(el => {
              (el as HTMLInputElement).value = num.toLocaleString('es-CL');
            });
          }, 50);
        }
      }
    });
  }
}
