import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatButtonModule } from '@angular/material/button';
import { RouterLink } from '@angular/router';
import { ReportesService, KpiReporte } from '@core/services/reportes.service';
import { VehiculosService } from '@core/services/vehiculos.service';
import { GpsPosicionActual } from '@core/models';

interface TarjetaKpi {
  etiqueta: string; icono: string; valorTexto: string;
  claseValor: string; pie?: string;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatProgressSpinnerModule, MatButtonModule, RouterLink],
  template: `
    <div class="encabezado-pagina">
      <h1>Dashboard</h1>
      <span class="texto-atenuado fecha-hoy">{{ fechaHoy }}</span>
    </div>

    @if (cargando()) {
      <div class="spinner-centrado"><mat-spinner diameter="48" /></div>
    } @else {

      <!-- KPIs -->
      <div class="cuadricula-kpi">
        @for (kpi of tarjetasKpi(); track kpi.etiqueta) {
          <div class="tarjeta-kpi">
            <div class="icono-kpi">
              <mat-icon>{{ kpi.icono }}</mat-icon>
            </div>
            <div class="etiqueta-kpi">{{ kpi.etiqueta }}</div>
            <div class="valor-kpi {{ kpi.claseValor }}">{{ kpi.valorTexto }}</div>
            @if (kpi.pie) { <div class="sub-kpi">{{ kpi.pie }}</div> }
          </div>
        }
      </div>

      <!-- Posiciones GPS -->
      <div class="titulo-seccion">
        <span>Flota en línea</span>
        <a mat-button [routerLink]="['/gps']" class="enlace-ver-mas">
          Ver mapa completo <mat-icon>arrow_forward</mat-icon>
        </a>
      </div>

      <div class="superficie lista-posiciones">
        @if (posiciones().length === 0) {
          <div class="estado-vacio">
            <mat-icon>gps_off</mat-icon>
            <p>Sin datos GPS disponibles en este momento.</p>
          </div>
        }
        @for (pos of posiciones(); track pos.vehiculoId) {
          <div class="fila-posicion">
            <div class="icono-posicion">
              <mat-icon>local_shipping</mat-icon>
            </div>
            <div class="info-posicion">
              <span class="placa-posicion">{{ pos.patente }}</span>
              <span class="coord-posicion texto-atenuado">
                {{ pos.latitud | number:'1.4-4' }}, {{ pos.longitud | number:'1.4-4' }}
              </span>
            </div>
            <div class="velocidad-posicion">{{ pos.velocidad }} km/h</div>
            <span class="insignia insignia-exito">En línea</span>
          </div>
        }
      </div>
    }
  `,
  styles: [`
    .fecha-hoy { font-size: 14px; }

    .cuadricula-kpi {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(190px, 1fr));
      gap: 16px;
      margin-bottom: 32px;
    }

    .titulo-seccion {
      display: flex; align-items: center; justify-content: space-between;
      font-size: 17px; font-weight: 600; color: var(--azul-900);
      margin-bottom: 12px;
    }
    .enlace-ver-mas {
      font-size: 13px; color: var(--azul-600) !important;
      display: flex; align-items: center; gap: 4px;
      mat-icon { font-size: 16px; width: 16px; height: 16px; }
    }

    .lista-posiciones { display: flex; flex-direction: column; gap: 0; padding: 0; }

    .fila-posicion {
      display: flex; align-items: center; gap: 12px;
      padding: 14px 20px;
      border-bottom: 1px solid var(--azul-50);
      transition: background .15s;
      &:last-child { border-bottom: none; }
      &:hover { background: var(--azul-50); }
    }
    .icono-posicion {
      width: 38px; height: 38px; border-radius: var(--radio-md);
      background: var(--azul-100);
      display: flex; align-items: center; justify-content: center;
      flex-shrink: 0;
      mat-icon { color: var(--azul-600); font-size: 20px; }
    }
    .info-posicion { display: flex; flex-direction: column; flex: 1; }
    .placa-posicion { font-weight: 600; font-size: 14px; color: var(--azul-900); }
    .coord-posicion { font-size: 12px; }
    .velocidad-posicion { font-size: 13px; font-weight: 600; color: var(--azul-600); min-width: 56px; text-align: right; }

    .estado-vacio {
      display: flex; flex-direction: column; align-items: center;
      padding: 40px; gap: 8px; color: var(--color-texto-3);
      mat-icon { font-size: 48px; width: 48px; height: 48px; opacity: .3; }
    }
  `],
})
export class DashboardComponent implements OnInit {
  private readonly servicioReportes  = inject(ReportesService);
  private readonly servicioVehiculos = inject(VehiculosService);

  cargando  = signal(true);
  kpis      = signal<KpiReporte | null>(null);
  posiciones = signal<GpsPosicionActual[]>([]);
  tarjetasKpi = signal<TarjetaKpi[]>([]);

  fechaHoy = new Date().toLocaleDateString('es-CL', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' });

  ngOnInit(): void {
    this.servicioReportes.getKpis().subscribe({
      next: kpi => {
        this.kpis.set(kpi);
        this.tarjetasKpi.set(this.construirTarjetas(kpi));
        this.cargando.set(false);
      },
      error: () => this.cargando.set(false),
    });
    this.servicioVehiculos.getPosicionesActuales().subscribe(p => this.posiciones.set(p));
  }

  private construirTarjetas(kpi: KpiReporte): TarjetaKpi[] {
    const otAbiertas = (kpi.otsPendientes ?? 0) + (kpi.otsEnEjecucion ?? 0);
    return [
      { etiqueta: 'Total vehículos',      icono: 'directions_bus',    valorTexto: String(kpi.totalVehiculos ?? 0),      claseValor: 'texto-primario' },
      { etiqueta: 'Operativos',           icono: 'check_circle',      valorTexto: String(kpi.vehiculosOperativos ?? 0),  claseValor: 'texto-exito' },
      { etiqueta: 'En taller',            icono: 'build_circle',      valorTexto: String(kpi.vehiculosEnTaller ?? 0),   claseValor: 'texto-advertencia' },
      { etiqueta: 'Fuera de servicio',    icono: 'do_not_disturb',    valorTexto: String(kpi.vehiculosFuera ?? 0),      claseValor: 'texto-peligro' },
      { etiqueta: 'Conductores',          icono: 'badge',             valorTexto: String(kpi.totalConductores ?? 0),    claseValor: '' },
      { etiqueta: 'OT abiertas',          icono: 'assignment',        valorTexto: String(otAbiertas),                    claseValor: 'texto-advertencia', pie: `${kpi.otsPendientes ?? 0} pendientes` },
      { etiqueta: 'OT cerradas',          icono: 'task_alt',          valorTexto: String(kpi.otsCerradas ?? 0),         claseValor: 'texto-exito' },
      { etiqueta: 'Alertas bajo stock',   icono: 'warning_amber',     valorTexto: String(kpi.alertasBajoStock ?? 0),    claseValor: kpi.alertasBajoStock > 0 ? 'texto-peligro' : 'texto-exito' },
    ];
  }

  private formatearClp(valor: number): string {
    return new Intl.NumberFormat('es-CL', { style: 'currency', currency: 'CLP', maximumFractionDigits: 0 }).format(valor ?? 0);
  }
}
