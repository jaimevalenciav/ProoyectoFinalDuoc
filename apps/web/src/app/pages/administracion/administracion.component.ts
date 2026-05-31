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
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { AlmacenService } from '@core/services/almacen.service';
import { TareasDefinicionService } from '@core/services/tareas-definicion.service';
import { VehiculosMaestrosService } from '@core/services/vehiculos-maestros.service';
import { DialogoService } from '@core/services/dialogo.service';
import { Repuesto, TareaDefinicion, Sucursal, Municipalidad, Aseguradora, PlantaRevision } from '@core/models';

@Component({
  selector: 'app-administracion',
  standalone: true,
  imports: [
    CommonModule, FormsModule, ReactiveFormsModule,
    MatTabsModule, MatButtonModule, MatIconModule, MatInputModule,
    MatSelectModule, MatTableModule, MatProgressSpinnerModule,
    MatSnackBarModule, MatChipsModule, MatSlideToggleModule,
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

      <!-- ══════════ TAB: Sucursales ══════════ -->
      <mat-tab label="Sucursales">
        <div class="tab-contenido">
          <div class="barra-acciones">
            <span style="flex:1"></span>
            <button class="btn-primary" (click)="abrirSucursal()">
              <mat-icon>add</mat-icon> Nueva sucursal
            </button>
          </div>
          @if (cargandoSuc()) {
            <div class="spinner-centrado"><mat-spinner diameter="36" /></div>
          } @else {
            <div class="superficie" style="padding:0;overflow:hidden">
              <table mat-table [dataSource]="sucursales()">
                <ng-container matColumnDef="nombre">
                  <th mat-header-cell *matHeaderCellDef>Nombre</th>
                  <td mat-cell *matCellDef="let r">
                    <div style="font-weight:500">{{ r.nombre }}</div>
                    @if (r.ciudad) { <div style="font-size:11px;color:var(--ink-soft)">{{ r.ciudad }}</div> }
                  </td>
                </ng-container>
                <ng-container matColumnDef="direccion">
                  <th mat-header-cell *matHeaderCellDef>Dirección</th>
                  <td mat-cell *matCellDef="let r">{{ r.direccion || '—' }}</td>
                </ng-container>
                <ng-container matColumnDef="activa">
                  <th mat-header-cell *matHeaderCellDef>Estado</th>
                  <td mat-cell *matCellDef="let r">
                    <span [class]="r.activa ? 'pill pill-activo' : 'pill pill-fuera'">
                      {{ r.activa ? 'Activa' : 'Inactiva' }}
                    </span>
                  </td>
                </ng-container>
                <ng-container matColumnDef="acciones">
                  <th mat-header-cell *matHeaderCellDef></th>
                  <td mat-cell *matCellDef="let r" style="white-space:nowrap">
                    <button mat-icon-button (click)="abrirSucursal(r)" matTooltip="Editar"><mat-icon>edit</mat-icon></button>
                    <button mat-icon-button color="warn" (click)="eliminarSucursal(r)" matTooltip="Eliminar"><mat-icon>delete_outline</mat-icon></button>
                  </td>
                </ng-container>
                <tr mat-header-row *matHeaderRowDef="colsSuc"></tr>
                <tr mat-row *matRowDef="let r; columns: colsSuc;"></tr>
              </table>
              @if (sucursales().length === 0) {
                <div class="estado-vacio-tabla">
                  <mat-icon>store</mat-icon><p>No hay sucursales registradas.</p>
                </div>
              }
            </div>
          }
        </div>
      </mat-tab>

      <!-- ══════════ TAB: Municipalidades ══════════ -->
      <mat-tab label="Municipalidades">
        <div class="tab-contenido">
          <div class="barra-acciones">
            <span style="flex:1"></span>
            <button class="btn-primary" (click)="abrirMunicipalidad()">
              <mat-icon>add</mat-icon> Nueva municipalidad
            </button>
          </div>
          @if (cargandoMun()) {
            <div class="spinner-centrado"><mat-spinner diameter="36" /></div>
          } @else {
            <div class="superficie" style="padding:0;overflow:hidden">
              <table mat-table [dataSource]="municipalidades()">
                <ng-container matColumnDef="nombre">
                  <th mat-header-cell *matHeaderCellDef>Nombre</th>
                  <td mat-cell *matCellDef="let r"><div style="font-weight:500">{{ r.nombre }}</div></td>
                </ng-container>
                <ng-container matColumnDef="region">
                  <th mat-header-cell *matHeaderCellDef>Región</th>
                  <td mat-cell *matCellDef="let r">{{ r.region || '—' }}</td>
                </ng-container>
                <ng-container matColumnDef="activa">
                  <th mat-header-cell *matHeaderCellDef>Estado</th>
                  <td mat-cell *matCellDef="let r">
                    <span [class]="r.activa ? 'pill pill-activo' : 'pill pill-fuera'">
                      {{ r.activa ? 'Activa' : 'Inactiva' }}
                    </span>
                  </td>
                </ng-container>
                <ng-container matColumnDef="acciones">
                  <th mat-header-cell *matHeaderCellDef></th>
                  <td mat-cell *matCellDef="let r" style="white-space:nowrap">
                    <button mat-icon-button (click)="abrirMunicipalidad(r)" matTooltip="Editar"><mat-icon>edit</mat-icon></button>
                    <button mat-icon-button color="warn" (click)="eliminarMunicipalidad(r)" matTooltip="Eliminar"><mat-icon>delete_outline</mat-icon></button>
                  </td>
                </ng-container>
                <tr mat-header-row *matHeaderRowDef="colsMun"></tr>
                <tr mat-row *matRowDef="let r; columns: colsMun;"></tr>
              </table>
              @if (municipalidades().length === 0) {
                <div class="estado-vacio-tabla">
                  <mat-icon>location_city</mat-icon><p>No hay municipalidades registradas.</p>
                </div>
              }
            </div>
          }
        </div>
      </mat-tab>

      <!-- ══════════ TAB: Aseguradoras ══════════ -->
      <mat-tab label="Aseguradoras">
        <div class="tab-contenido">
          <div class="barra-acciones">
            <span style="flex:1"></span>
            <button class="btn-primary" (click)="abrirAseguradora()">
              <mat-icon>add</mat-icon> Nueva aseguradora
            </button>
          </div>
          @if (cargandoAseg()) {
            <div class="spinner-centrado"><mat-spinner diameter="36" /></div>
          } @else {
            <div class="superficie" style="padding:0;overflow:hidden">
              <table mat-table [dataSource]="aseguradoras()">
                <ng-container matColumnDef="nombre">
                  <th mat-header-cell *matHeaderCellDef>Nombre</th>
                  <td mat-cell *matCellDef="let r"><div style="font-weight:500">{{ r.nombre }}</div></td>
                </ng-container>
                <ng-container matColumnDef="rut">
                  <th mat-header-cell *matHeaderCellDef>RUT</th>
                  <td mat-cell *matCellDef="let r">{{ r.rut || '—' }}</td>
                </ng-container>
                <ng-container matColumnDef="activa">
                  <th mat-header-cell *matHeaderCellDef>Estado</th>
                  <td mat-cell *matCellDef="let r">
                    <span [class]="r.activa ? 'pill pill-activo' : 'pill pill-fuera'">
                      {{ r.activa ? 'Activa' : 'Inactiva' }}
                    </span>
                  </td>
                </ng-container>
                <ng-container matColumnDef="acciones">
                  <th mat-header-cell *matHeaderCellDef></th>
                  <td mat-cell *matCellDef="let r" style="white-space:nowrap">
                    <button mat-icon-button (click)="abrirAseguradora(r)" matTooltip="Editar"><mat-icon>edit</mat-icon></button>
                    <button mat-icon-button color="warn" (click)="eliminarAseguradora(r)" matTooltip="Eliminar"><mat-icon>delete_outline</mat-icon></button>
                  </td>
                </ng-container>
                <tr mat-header-row *matHeaderRowDef="colsAseg"></tr>
                <tr mat-row *matRowDef="let r; columns: colsAseg;"></tr>
              </table>
              @if (aseguradoras().length === 0) {
                <div class="estado-vacio-tabla">
                  <mat-icon>shield</mat-icon><p>No hay aseguradoras registradas.</p>
                </div>
              }
            </div>
          }
        </div>
      </mat-tab>

      <!-- ══════════ TAB: Plantas Revisión Técnica ══════════ -->
      <mat-tab label="Plantas Rev. Técnica">
        <div class="tab-contenido">
          <div class="barra-acciones">
            <span style="flex:1"></span>
            <button class="btn-primary" (click)="abrirPlanta()">
              <mat-icon>add</mat-icon> Nueva planta
            </button>
          </div>
          @if (cargandoPlanta()) {
            <div class="spinner-centrado"><mat-spinner diameter="36" /></div>
          } @else {
            <div class="superficie" style="padding:0;overflow:hidden">
              <table mat-table [dataSource]="plantas()">
                <ng-container matColumnDef="nombre">
                  <th mat-header-cell *matHeaderCellDef>Nombre</th>
                  <td mat-cell *matCellDef="let r"><div style="font-weight:500">{{ r.nombre }}</div></td>
                </ng-container>
                <ng-container matColumnDef="direccion">
                  <th mat-header-cell *matHeaderCellDef>Dirección</th>
                  <td mat-cell *matCellDef="let r">{{ r.direccion || '—' }}</td>
                </ng-container>
                <ng-container matColumnDef="activa">
                  <th mat-header-cell *matHeaderCellDef>Estado</th>
                  <td mat-cell *matCellDef="let r">
                    <span [class]="r.activa ? 'pill pill-activo' : 'pill pill-fuera'">
                      {{ r.activa ? 'Activa' : 'Inactiva' }}
                    </span>
                  </td>
                </ng-container>
                <ng-container matColumnDef="acciones">
                  <th mat-header-cell *matHeaderCellDef></th>
                  <td mat-cell *matCellDef="let r" style="white-space:nowrap">
                    <button mat-icon-button (click)="abrirPlanta(r)" matTooltip="Editar"><mat-icon>edit</mat-icon></button>
                    <button mat-icon-button color="warn" (click)="eliminarPlanta(r)" matTooltip="Eliminar"><mat-icon>delete_outline</mat-icon></button>
                  </td>
                </ng-container>
                <tr mat-header-row *matHeaderRowDef="colsPlanta"></tr>
                <tr mat-row *matRowDef="let r; columns: colsPlanta;"></tr>
              </table>
              @if (plantas().length === 0) {
                <div class="estado-vacio-tabla">
                  <mat-icon>directions_car</mat-icon><p>No hay plantas registradas.</p>
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

    <!-- ══════════ MODAL: Sucursal ══════════ -->
    @if (modalSuc()) {
      <div class="capa-modal" (click)="cerrarSucursal()">
        <div class="panel-modal" style="max-width:480px" (click)="$event.stopPropagation()">
          <h2>{{ idSuc() ? 'Editar' : 'Nueva' }} sucursal</h2>
          <form [formGroup]="formSuc" (ngSubmit)="guardarSucursal()">
            <mat-form-field appearance="fill" class="ancho-completo">
              <mat-label>Nombre</mat-label>
              <input matInput formControlName="nombre" />
            </mat-form-field>
            <div class="dos-columnas">
              <mat-form-field appearance="fill">
                <mat-label>Ciudad</mat-label>
                <input matInput formControlName="ciudad" />
              </mat-form-field>
              <mat-form-field appearance="fill">
                <mat-label>Dirección</mat-label>
                <input matInput formControlName="direccion" />
              </mat-form-field>
            </div>
            <div style="display:flex;align-items:center;gap:12px;margin:8px 0 16px">
              <mat-slide-toggle formControlName="activa">Activa</mat-slide-toggle>
            </div>
            <div class="acciones-formulario">
              <button mat-button type="button" (click)="cerrarSucursal()">Cancelar</button>
              <button mat-flat-button class="btn-principal" type="submit" [disabled]="formSuc.invalid || guardando()">Guardar</button>
            </div>
          </form>
        </div>
      </div>
    }

    <!-- ══════════ MODAL: Municipalidad ══════════ -->
    @if (modalMun()) {
      <div class="capa-modal" (click)="cerrarMunicipalidad()">
        <div class="panel-modal" style="max-width:440px" (click)="$event.stopPropagation()">
          <h2>{{ idMun() ? 'Editar' : 'Nueva' }} municipalidad</h2>
          <form [formGroup]="formMun" (ngSubmit)="guardarMunicipalidad()">
            <mat-form-field appearance="fill" class="ancho-completo">
              <mat-label>Nombre</mat-label>
              <input matInput formControlName="nombre" />
            </mat-form-field>
            <mat-form-field appearance="fill" class="ancho-completo">
              <mat-label>Región</mat-label>
              <mat-select formControlName="region">
                <mat-option value="">— Sin región —</mat-option>
                <mat-option value="Arica y Parinacota">Arica y Parinacota</mat-option>
                <mat-option value="Tarapacá">Tarapacá</mat-option>
                <mat-option value="Antofagasta">Antofagasta</mat-option>
                <mat-option value="Atacama">Atacama</mat-option>
                <mat-option value="Coquimbo">Coquimbo</mat-option>
                <mat-option value="Valparaíso">Valparaíso</mat-option>
                <mat-option value="O'Higgins">O'Higgins</mat-option>
                <mat-option value="Maule">Maule</mat-option>
                <mat-option value="Ñuble">Ñuble</mat-option>
                <mat-option value="Biobío">Biobío</mat-option>
                <mat-option value="La Araucanía">La Araucanía</mat-option>
                <mat-option value="Los Ríos">Los Ríos</mat-option>
                <mat-option value="Los Lagos">Los Lagos</mat-option>
                <mat-option value="Aysén">Aysén</mat-option>
                <mat-option value="Magallanes">Magallanes</mat-option>
                <mat-option value="Metropolitana">Metropolitana</mat-option>
              </mat-select>
            </mat-form-field>
            <div style="display:flex;align-items:center;gap:12px;margin:8px 0 16px">
              <mat-slide-toggle formControlName="activa">Activa</mat-slide-toggle>
            </div>
            <div class="acciones-formulario">
              <button mat-button type="button" (click)="cerrarMunicipalidad()">Cancelar</button>
              <button mat-flat-button class="btn-principal" type="submit" [disabled]="formMun.invalid || guardando()">Guardar</button>
            </div>
          </form>
        </div>
      </div>
    }

    <!-- ══════════ MODAL: Aseguradora ══════════ -->
    @if (modalAseg()) {
      <div class="capa-modal" (click)="cerrarAseguradora()">
        <div class="panel-modal" style="max-width:440px" (click)="$event.stopPropagation()">
          <h2>{{ idAseg() ? 'Editar' : 'Nueva' }} aseguradora</h2>
          <form [formGroup]="formAseg" (ngSubmit)="guardarAseguradora()">
            <mat-form-field appearance="fill" class="ancho-completo">
              <mat-label>Nombre</mat-label>
              <input matInput formControlName="nombre" />
            </mat-form-field>
            <mat-form-field appearance="fill" class="ancho-completo">
              <mat-label>RUT</mat-label>
              <input matInput formControlName="rut" placeholder="Ej: 76.123.456-7" />
            </mat-form-field>
            <div style="display:flex;align-items:center;gap:12px;margin:8px 0 16px">
              <mat-slide-toggle formControlName="activa">Activa</mat-slide-toggle>
            </div>
            <div class="acciones-formulario">
              <button mat-button type="button" (click)="cerrarAseguradora()">Cancelar</button>
              <button mat-flat-button class="btn-principal" type="submit" [disabled]="formAseg.invalid || guardando()">Guardar</button>
            </div>
          </form>
        </div>
      </div>
    }

    <!-- ══════════ MODAL: Planta Revisión Técnica ══════════ -->
    @if (modalPlanta()) {
      <div class="capa-modal" (click)="cerrarPlanta()">
        <div class="panel-modal" style="max-width:480px" (click)="$event.stopPropagation()">
          <h2>{{ idPlanta() ? 'Editar' : 'Nueva' }} planta</h2>
          <form [formGroup]="formPlanta" (ngSubmit)="guardarPlanta()">
            <mat-form-field appearance="fill" class="ancho-completo">
              <mat-label>Nombre</mat-label>
              <input matInput formControlName="nombre" />
            </mat-form-field>
            <mat-form-field appearance="fill" class="ancho-completo">
              <mat-label>Dirección</mat-label>
              <input matInput formControlName="direccion" />
            </mat-form-field>
            <div style="display:flex;align-items:center;gap:12px;margin:8px 0 16px">
              <mat-slide-toggle formControlName="activa">Activa</mat-slide-toggle>
            </div>
            <div class="acciones-formulario">
              <button mat-button type="button" (click)="cerrarPlanta()">Cancelar</button>
              <button mat-flat-button class="btn-principal" type="submit" [disabled]="formPlanta.invalid || guardando()">Guardar</button>
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
  private readonly almacenSvc  = inject(AlmacenService);
  private readonly tareasSvc   = inject(TareasDefinicionService);
  private readonly maestrosSvc = inject(VehiculosMaestrosService);
  private readonly snack       = inject(MatSnackBar);
  private readonly fb          = inject(FormBuilder);
  private readonly dialogo     = inject(DialogoService);

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

  // ── Sucursales ────────────────────────────────────────────────
  sucursales    = signal<Sucursal[]>([]);
  cargandoSuc   = signal(false);
  modalSuc      = signal(false);
  idSuc         = signal<string | null>(null);
  colsSuc       = ['nombre', 'direccion', 'activa', 'acciones'];

  formSuc = this.fb.group({
    nombre:    ['', Validators.required],
    ciudad:    [''],
    direccion: [''],
    activa:    [true],
  });

  // ── Municipalidades ───────────────────────────────────────────
  municipalidades = signal<Municipalidad[]>([]);
  cargandoMun     = signal(false);
  modalMun        = signal(false);
  idMun           = signal<string | null>(null);
  colsMun         = ['nombre', 'region', 'activa', 'acciones'];

  formMun = this.fb.group({
    nombre: ['', Validators.required],
    region: [''],
    activa: [true],
  });

  // ── Aseguradoras ──────────────────────────────────────────────
  aseguradoras  = signal<Aseguradora[]>([]);
  cargandoAseg  = signal(false);
  modalAseg     = signal(false);
  idAseg        = signal<string | null>(null);
  colsAseg      = ['nombre', 'rut', 'activa', 'acciones'];

  formAseg = this.fb.group({
    nombre: ['', Validators.required],
    rut:    [''],
    activa: [true],
  });

  // ── Plantas Revisión ──────────────────────────────────────────
  plantas       = signal<PlantaRevision[]>([]);
  cargandoPlanta = signal(false);
  modalPlanta   = signal(false);
  idPlanta      = signal<string | null>(null);
  colsPlanta    = ['nombre', 'direccion', 'activa', 'acciones'];

  formPlanta = this.fb.group({
    nombre:    ['', Validators.required],
    direccion: [''],
    activa:    [true],
  });

  ngOnInit() {
    this.cargarRepuestos();
    this.cargarTareas();
    this.cargarSucursales();
    this.cargarMunicipalidades();
    this.cargarAseguradoras();
    this.cargarPlantas();
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

  async eliminarRepuesto(r: Repuesto) {
    const ok = await this.dialogo.confirmarEliminar(`¿Eliminar repuesto "${r.descripcion}"?`, `Código: ${r.codigo}`);
    if (!ok) return;
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

  async eliminarTarea(td: TareaDefinicion) {
    const ok = await this.dialogo.confirmarEliminar(`¿Eliminar tarea "${td.nombre}"?`);
    if (!ok) return;
    this.tareasSvc.delete(td.id).subscribe({ next: () => this.cargarTareas() });
  }

  // ── Sucursales CRUD ───────────────────────────────────────────
  cargarSucursales() {
    this.cargandoSuc.set(true);
    this.maestrosSvc.getSucursales().subscribe({
      next: r => { this.sucursales.set(r); this.cargandoSuc.set(false); },
      error: () => this.cargandoSuc.set(false),
    });
  }

  abrirSucursal(r?: Sucursal) {
    this.idSuc.set(r?.id ?? null);
    if (r) this.formSuc.patchValue({ nombre: r.nombre, ciudad: r.ciudad ?? '', direccion: r.direccion ?? '', activa: !!r.activa });
    else this.formSuc.reset({ activa: true });
    this.modalSuc.set(true);
  }
  cerrarSucursal() { this.modalSuc.set(false); }

  guardarSucursal() {
    if (this.formSuc.invalid) return;
    this.guardando.set(true);
    const dto: any = { ...this.formSuc.value, activa: this.formSuc.value.activa ? 1 : 0 };
    const op = this.idSuc()
      ? this.maestrosSvc.updateSucursal(this.idSuc()!, dto)
      : this.maestrosSvc.createSucursal(dto);
    op.subscribe({
      next: () => { this.guardando.set(false); this.cerrarSucursal(); this.cargarSucursales(); this.snack.open('Guardado', '', { duration: 2500 }); },
      error: () => { this.guardando.set(false); this.snack.open('Error al guardar', '', { duration: 2500 }); },
    });
  }

  async eliminarSucursal(r: Sucursal) {
    const ok = await this.dialogo.confirmarEliminar(`¿Eliminar sucursal "${r.nombre}"?`);
    if (!ok) return;
    this.maestrosSvc.deleteSucursal(r.id).subscribe({ next: () => this.cargarSucursales() });
  }

  // ── Municipalidades CRUD ──────────────────────────────────────
  cargarMunicipalidades() {
    this.cargandoMun.set(true);
    this.maestrosSvc.getMunicipalidades().subscribe({
      next: r => { this.municipalidades.set(r); this.cargandoMun.set(false); },
      error: () => this.cargandoMun.set(false),
    });
  }

  abrirMunicipalidad(r?: Municipalidad) {
    this.idMun.set(r?.id ?? null);
    if (r) this.formMun.patchValue({ nombre: r.nombre, region: r.region ?? '', activa: !!r.activa });
    else this.formMun.reset({ activa: true });
    this.modalMun.set(true);
  }
  cerrarMunicipalidad() { this.modalMun.set(false); }

  guardarMunicipalidad() {
    if (this.formMun.invalid) return;
    this.guardando.set(true);
    const dto: any = { ...this.formMun.value, activa: this.formMun.value.activa ? 1 : 0 };
    const op = this.idMun()
      ? this.maestrosSvc.updateMunicipalidad(this.idMun()!, dto)
      : this.maestrosSvc.createMunicipalidad(dto);
    op.subscribe({
      next: () => { this.guardando.set(false); this.cerrarMunicipalidad(); this.cargarMunicipalidades(); this.snack.open('Guardado', '', { duration: 2500 }); },
      error: () => { this.guardando.set(false); this.snack.open('Error al guardar', '', { duration: 2500 }); },
    });
  }

  async eliminarMunicipalidad(r: Municipalidad) {
    const ok = await this.dialogo.confirmarEliminar(`¿Eliminar municipalidad "${r.nombre}"?`);
    if (!ok) return;
    this.maestrosSvc.deleteMunicipalidad(r.id).subscribe({ next: () => this.cargarMunicipalidades() });
  }

  // ── Aseguradoras CRUD ─────────────────────────────────────────
  cargarAseguradoras() {
    this.cargandoAseg.set(true);
    this.maestrosSvc.getAseguradoras().subscribe({
      next: r => { this.aseguradoras.set(r); this.cargandoAseg.set(false); },
      error: () => this.cargandoAseg.set(false),
    });
  }

  abrirAseguradora(r?: Aseguradora) {
    this.idAseg.set(r?.id ?? null);
    if (r) this.formAseg.patchValue({ nombre: r.nombre, rut: r.rut ?? '', activa: !!r.activa });
    else this.formAseg.reset({ activa: true });
    this.modalAseg.set(true);
  }
  cerrarAseguradora() { this.modalAseg.set(false); }

  guardarAseguradora() {
    if (this.formAseg.invalid) return;
    this.guardando.set(true);
    const dto: any = { ...this.formAseg.value, activa: this.formAseg.value.activa ? 1 : 0 };
    const op = this.idAseg()
      ? this.maestrosSvc.updateAseguradora(this.idAseg()!, dto)
      : this.maestrosSvc.createAseguradora(dto);
    op.subscribe({
      next: () => { this.guardando.set(false); this.cerrarAseguradora(); this.cargarAseguradoras(); this.snack.open('Guardado', '', { duration: 2500 }); },
      error: () => { this.guardando.set(false); this.snack.open('Error al guardar', '', { duration: 2500 }); },
    });
  }

  async eliminarAseguradora(r: Aseguradora) {
    const ok = await this.dialogo.confirmarEliminar(`¿Eliminar aseguradora "${r.nombre}"?`);
    if (!ok) return;
    this.maestrosSvc.deleteAseguradora(r.id).subscribe({ next: () => this.cargarAseguradoras() });
  }

  // ── Plantas Revisión Técnica CRUD ─────────────────────────────
  cargarPlantas() {
    this.cargandoPlanta.set(true);
    this.maestrosSvc.getPlantasRevision().subscribe({
      next: r => { this.plantas.set(r); this.cargandoPlanta.set(false); },
      error: () => this.cargandoPlanta.set(false),
    });
  }

  abrirPlanta(r?: PlantaRevision) {
    this.idPlanta.set(r?.id ?? null);
    if (r) this.formPlanta.patchValue({ nombre: r.nombre, direccion: r.direccion ?? '', activa: !!r.activa });
    else this.formPlanta.reset({ activa: true });
    this.modalPlanta.set(true);
  }
  cerrarPlanta() { this.modalPlanta.set(false); }

  guardarPlanta() {
    if (this.formPlanta.invalid) return;
    this.guardando.set(true);
    const dto: any = { ...this.formPlanta.value, activa: this.formPlanta.value.activa ? 1 : 0 };
    const op = this.idPlanta()
      ? this.maestrosSvc.updatePlantaRevision(this.idPlanta()!, dto)
      : this.maestrosSvc.createPlantaRevision(dto);
    op.subscribe({
      next: () => { this.guardando.set(false); this.cerrarPlanta(); this.cargarPlantas(); this.snack.open('Guardado', '', { duration: 2500 }); },
      error: () => { this.guardando.set(false); this.snack.open('Error al guardar', '', { duration: 2500 }); },
    });
  }

  async eliminarPlanta(r: PlantaRevision) {
    const ok = await this.dialogo.confirmarEliminar(`¿Eliminar planta "${r.nombre}"?`);
    if (!ok) return;
    this.maestrosSvc.deletePlantaRevision(r.id).subscribe({ next: () => this.cargarPlantas() });
  }
}
