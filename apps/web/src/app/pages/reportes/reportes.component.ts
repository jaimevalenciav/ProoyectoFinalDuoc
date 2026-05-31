import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { ReportesService, KpiReporte, ConsumoVehiculo, CostoMantenimiento } from '@core/services/reportes.service';

@Component({
  selector: 'app-reportes',
  standalone: true,
  imports: [
    CommonModule, FormsModule, MatButtonModule, MatIconModule,
    MatInputModule, MatSelectModule, MatProgressSpinnerModule,
    MatSnackBarModule, MatTableModule,
  ],
  template: `
    <div class="encabezado-pagina">
      <h1>Reportes y KPIs</h1>
      <div class="acciones-encabezado">
        <mat-form-field appearance="fill">
          <mat-label>Desde</mat-label>
          <input matInput type="date" [(ngModel)]="desde" />
        </mat-form-field>
        <mat-form-field appearance="fill">
          <mat-label>Hasta</mat-label>
          <input matInput type="date" [(ngModel)]="hasta" />
        </mat-form-field>
        <button mat-flat-button class="btn-principal" (click)="cargar()">
          <mat-icon>refresh</mat-icon> Actualizar
        </button>
        <button mat-stroked-button class="btn-secundario" (click)="exportar('combustible')">
          <mat-icon>download</mat-icon> Exportar Excel
        </button>
      </div>
    </div>

    @if (cargando()) {
      <div class="spinner-centrado"><mat-spinner diameter="48" /></div>
    } @else {

      <div class="titulo-seccion">Resumen del período</div>
      <div class="cuadricula-kpi">
        <div class="tarjeta-kpi">
          <div class="etiqueta-kpi">Vehículos activos</div>
          <div class="valor-kpi texto-exito">{{ kpis()?.vehiculosActivos ?? '–' }}</div>
        </div>
        <div class="tarjeta-kpi">
          <div class="etiqueta-kpi">OT cerradas</div>
          <div class="valor-kpi">{{ kpis()?.otCerradasMes ?? '–' }}</div>
        </div>
        <div class="tarjeta-kpi">
          <div class="etiqueta-kpi">Costo mantenimiento</div>
          <div class="valor-kpi texto-peligro">{{ kpis()?.costoMantenimientoMes | currency:'CLP':'$':'1.0-0' }}</div>
        </div>
        <div class="tarjeta-kpi">
          <div class="etiqueta-kpi">Litros cargados</div>
          <div class="valor-kpi">{{ kpis()?.litrosCargadosMes ?? '–' }} L</div>
        </div>
        <div class="tarjeta-kpi">
          <div class="etiqueta-kpi">Costo combustible</div>
          <div class="valor-kpi texto-peligro">{{ kpis()?.costoCombustibleMes | currency:'CLP':'$':'1.0-0' }}</div>
        </div>
        <div class="tarjeta-kpi">
          <div class="etiqueta-kpi">Ingresos servicios</div>
          <div class="valor-kpi texto-primario">{{ kpis()?.ingresoServiciosMes | currency:'CLP':'$':'1.0-0' }}</div>
        </div>
        <div class="tarjeta-kpi">
          <div class="etiqueta-kpi">Km recorridos</div>
          <div class="valor-kpi">{{ kpis()?.kmRecorridosMes | number:'1.0-0' }} km</div>
        </div>
      </div>

      <div class="titulo-seccion">Consumo combustible por vehículo</div>
      <div class="superficie" style="padding:0;overflow:hidden;margin-bottom:24px">
        <table mat-table [dataSource]="consumo()">
          <ng-container matColumnDef="placa">
            <th mat-header-cell *matHeaderCellDef>Placa</th>
            <td mat-cell *matCellDef="let r"><strong>{{ r.placa }}</strong></td>
          </ng-container>
          <ng-container matColumnDef="litros">
            <th mat-header-cell *matHeaderCellDef>Litros totales</th>
            <td mat-cell *matCellDef="let r">{{ r.litrosTotales | number:'1.1-1' }} L</td>
          </ng-container>
          <ng-container matColumnDef="km">
            <th mat-header-cell *matHeaderCellDef>Km totales</th>
            <td mat-cell *matCellDef="let r">{{ r.kmTotales | number:'1.0-0' }}</td>
          </ng-container>
          <ng-container matColumnDef="rendimiento">
            <th mat-header-cell *matHeaderCellDef>Rendimiento</th>
            <td mat-cell *matCellDef="let r">
              <span [class]="insigniaRendimiento(r.rendimientoPromedio)">{{ r.rendimientoPromedio | number:'1.1-1' }} km/L</span>
            </td>
          </ng-container>
          <ng-container matColumnDef="costo">
            <th mat-header-cell *matHeaderCellDef>Costo total</th>
            <td mat-cell *matCellDef="let r">{{ r.costoTotal | currency:'CLP':'$':'1.0-0' }}</td>
          </ng-container>
          <tr mat-header-row *matHeaderRowDef="columnasConsumo"></tr>
          <tr mat-row *matRowDef="let fila; columns: columnasConsumo;"></tr>
        </table>
        @if (consumo().length === 0) {
          <div class="estado-vacio-tabla">
            <mat-icon>local_gas_station</mat-icon><p>Sin datos de consumo para el período.</p>
          </div>
        }
      </div>

      <div class="titulo-seccion">Costos de mantenimiento mensual</div>
      <div class="superficie" style="padding:0;overflow:hidden">
        <table mat-table [dataSource]="costos()">
          <ng-container matColumnDef="mes">
            <th mat-header-cell *matHeaderCellDef>Mes</th>
            <td mat-cell *matCellDef="let r">{{ r.mes }}</td>
          </ng-container>
          <ng-container matColumnDef="manoObra">
            <th mat-header-cell *matHeaderCellDef>Mano de obra</th>
            <td mat-cell *matCellDef="let r">{{ r.costoManoObra | currency:'CLP':'$':'1.0-0' }}</td>
          </ng-container>
          <ng-container matColumnDef="repuestos">
            <th mat-header-cell *matHeaderCellDef>Repuestos</th>
            <td mat-cell *matCellDef="let r">{{ r.costoRepuestos | currency:'CLP':'$':'1.0-0' }}</td>
          </ng-container>
          <ng-container matColumnDef="total">
            <th mat-header-cell *matHeaderCellDef>Total</th>
            <td mat-cell *matCellDef="let r"><strong>{{ r.total | currency:'CLP':'$':'1.0-0' }}</strong></td>
          </ng-container>
          <tr mat-header-row *matHeaderRowDef="columnasCostos"></tr>
          <tr mat-row *matRowDef="let fila; columns: columnasCostos;"></tr>
        </table>
        @if (costos().length === 0) {
          <div class="estado-vacio-tabla">
            <mat-icon>build</mat-icon><p>Sin datos de mantenimiento para el período.</p>
          </div>
        }
      </div>
    }
  `,
  styles: [`
    .acciones-encabezado { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
    .cuadricula-kpi { display: grid; grid-template-columns: repeat(auto-fill,minmax(180px,1fr)); gap: 16px; margin-bottom: 8px; }
    .spinner-centrado { display: flex; justify-content: center; padding: 80px; }
    .estado-vacio-tabla {
      display: flex; flex-direction: column; align-items: center; padding: 48px;
      gap: 8px; color: var(--color-texto-3);
      mat-icon { font-size: 48px; width: 48px; height: 48px; opacity: .3; }
    }
  `],
})
export class ReportesComponent implements OnInit {
  private readonly servicio     = inject(ReportesService);
  private readonly notificacion = inject(MatSnackBar);

  columnasConsumo = ['placa', 'litros', 'km', 'rendimiento', 'costo'];
  columnasCostos  = ['mes', 'manoObra', 'repuestos', 'total'];
  cargando        = signal(true);
  kpis            = signal<KpiReporte | null>(null);
  consumo         = signal<ConsumoVehiculo[]>([]);
  costos          = signal<CostoMantenimiento[]>([]);

  desde = new Date(new Date().getFullYear(), new Date().getMonth(), 1).toISOString().substring(0, 10);
  hasta = new Date().toISOString().substring(0, 10);

  ngOnInit() { this.cargar(); }

  cargar() {
    this.cargando.set(true);
    this.servicio.getKpis(this.desde, this.hasta).subscribe(k => this.kpis.set(k));
    this.servicio.getConsumoFlota(this.desde, this.hasta).subscribe(c => this.consumo.set(c));
    this.servicio.getCostosMantenimiento(this.desde, this.hasta).subscribe(c => {
      this.costos.set(c);
      this.cargando.set(false);
    });
  }

  exportar(tipo: 'combustible' | 'mantenimiento' | 'servicios') {
    this.servicio.exportarExcel(tipo, this.desde, this.hasta).subscribe(blob => {
      const url = URL.createObjectURL(blob);
      const enlace = document.createElement('a');
      enlace.href = url;
      enlace.download = `reporte-${tipo}.xlsx`;
      enlace.click();
      URL.revokeObjectURL(url);
      this.notificacion.open('Descargando…', '', { duration: 2000 });
    });
  }

  insigniaRendimiento(kmPorLitro: number): string {
    if (kmPorLitro >= 8) return 'insignia insignia-exito';
    if (kmPorLitro >= 5) return 'insignia insignia-advertencia';
    return 'insignia insignia-peligro';
  }
}
