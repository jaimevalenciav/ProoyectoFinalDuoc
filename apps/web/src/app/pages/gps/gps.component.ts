import { Component, OnInit, OnDestroy, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { VehiculosService } from '@core/services/vehiculos.service';
import { GpsPosicionActual, Vehiculo } from '@core/models';
import { interval, Subscription } from 'rxjs';
import { switchMap } from 'rxjs/operators';

@Component({
  selector: 'app-gps',
  standalone: true,
  imports: [CommonModule, FormsModule, MatSelectModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  template: `
    <div class="encabezado-pagina">
      <h1>GPS — Seguimiento en tiempo real</h1>
      <div class="acciones-encabezado">
        <mat-form-field appearance="fill" style="width:200px">
          <mat-label>Filtrar vehículo</mat-label>
          <mat-select [(ngModel)]="vehiculoSeleccionado">
            <mat-option value="">Todos</mat-option>
            @for (v of vehiculos(); track v.id) {
              <mat-option [value]="v.id">{{ v.patente }}</mat-option>
            }
          </mat-select>
        </mat-form-field>
        <button mat-flat-button class="btn-principal" (click)="cargarPosiciones()">
          <mat-icon>refresh</mat-icon> Actualizar
        </button>
        <span class="insignia insignia-exito indicador-auto">
          <mat-icon style="font-size:14px;height:14px;width:14px;margin-right:4px">autorenew</mat-icon>
          Cada 30s
        </span>
      </div>
    </div>

    <!-- Área del mapa -->
    <div class="superficie area-mapa">
      <div class="marcador-mapa">
        <mat-icon>map</mat-icon>
        <p>Mapa GPS interactivo</p>
        <p class="texto-atenuado" style="font-size:13px">
          Integrar <strong>Leaflet</strong> o <strong>Azure Maps</strong> con coordenadas de
          <code>/api/v1/gps/posiciones-actuales</code>
        </p>
        <!-- Puntos en mapa simulados -->
        <div class="puntos-simulados">
          @for (pos of posicionesFiltradas(); track pos.vehiculoId) {
            <div class="punto-mapa" [title]="pos.patente">
              <mat-icon>local_shipping</mat-icon>
            </div>
          }
        </div>
      </div>
    </div>

    <!-- Listado de vehículos -->
    <div class="titulo-seccion">
      Flota en línea ({{ posicionesFiltradas().length }})
    </div>

    <div class="superficie" style="padding:0;overflow:hidden">
      @if (cargando()) { <div style="padding:24px"><mat-spinner diameter="32" /></div> }

      @for (pos of posicionesFiltradas(); track pos.vehiculoId) {
        <div class="fila-vehiculo-gps">
          <div class="icono-vehiculo-gps">
            <mat-icon>local_shipping</mat-icon>
          </div>
          <div class="datos-vehiculo-gps">
            <span class="placa-gps">{{ pos.patente }}</span>
            <span class="coords-gps texto-atenuado">
              {{ pos.latitud | number:'1.5-5' }}, {{ pos.longitud | number:'1.5-5' }}
            </span>
            <span class="hora-gps texto-atenuado">{{ pos.recordedAt | date:'dd/MM/yyyy HH:mm:ss' }}</span>
          </div>
          <div class="velocidad-gps">{{ pos.velocidad }} km/h</div>
          <span class="insignia insignia-exito">En línea</span>
        </div>
      }

      @if (!cargando() && posicionesFiltradas().length === 0) {
        <div class="estado-vacio-tabla">
          <mat-icon>gps_off</mat-icon>
          <p>Sin posiciones GPS disponibles.</p>
        </div>
      }
    </div>
  `,
  styles: [`
    .indicador-auto { display: flex; align-items: center; font-size: 12px; }

    .area-mapa { margin-bottom: 16px; padding: 0; overflow: hidden; }
    .marcador-mapa {
      height: 380px;
      display: flex; flex-direction: column; align-items: center; justify-content: center;
      gap: 10px; color: var(--color-texto-2);
      background: linear-gradient(135deg, var(--azul-50), #fff);
      mat-icon { font-size: 64px; height: 64px; width: 64px; color: var(--azul-300); }
      p { font-size: 16px; font-weight: 500; color: var(--azul-700); }
      code { background: var(--azul-100); padding: 2px 6px; border-radius: 4px; font-size: 12px; color: var(--azul-700); }
    }
    .puntos-simulados {
      display: flex; gap: 12px; margin-top: 8px;
    }
    .punto-mapa {
      width: 36px; height: 36px; border-radius: 50%;
      background: var(--azul-600); display: flex; align-items: center; justify-content: center;
      box-shadow: 0 0 0 4px rgba(27,44,64,.2);
      mat-icon { color: #fff; font-size: 18px; width: 18px; height: 18px; }
    }

    .titulo-seccion { font-size: 17px; font-weight: 600; color: var(--azul-900); margin-bottom: 12px; }

    .fila-vehiculo-gps {
      display: flex; align-items: center; gap: 14px;
      padding: 14px 20px;
      border-bottom: 1px solid var(--azul-50);
      transition: background .15s;
      &:last-child { border-bottom: none; }
      &:hover { background: var(--azul-50); }
    }
    .icono-vehiculo-gps {
      width: 40px; height: 40px; border-radius: var(--radio-md);
      background: var(--azul-100); display: flex; align-items: center; justify-content: center;
      flex-shrink: 0;
      mat-icon { color: var(--azul-600); }
    }
    .datos-vehiculo-gps { display: flex; flex-direction: column; flex: 1; gap: 2px; }
    .placa-gps { font-weight: 700; font-size: 14px; color: var(--azul-900); }
    .coords-gps, .hora-gps { font-size: 12px; }
    .velocidad-gps { font-size: 15px; font-weight: 700; color: var(--azul-600); min-width: 60px; text-align: right; }

    .estado-vacio-tabla {
      display: flex; flex-direction: column; align-items: center;
      padding: 40px; gap: 8px; color: var(--color-texto-3);
      mat-icon { font-size: 40px; width: 40px; height: 40px; opacity: .3; }
    }
  `],
})
export class GpsComponent implements OnInit, OnDestroy {
  private readonly servicioVehiculos = inject(VehiculosService);
  private suscripcion?: Subscription;

  cargando            = signal(true);
  vehiculos           = signal<Vehiculo[]>([]);
  posiciones          = signal<GpsPosicionActual[]>([]);
  vehiculoSeleccionado = '';

  posicionesFiltradas = computed(() =>
    this.vehiculoSeleccionado
      ? this.posiciones().filter(p => p.vehiculoId === this.vehiculoSeleccionado)
      : this.posiciones()
  );

  ngOnInit(): void {
    this.servicioVehiculos.getAll({ size: 100 }).subscribe(r => this.vehiculos.set(r.content));
    this.cargarPosiciones();
    this.suscripcion = interval(30_000)
      .pipe(switchMap(() => this.servicioVehiculos.getPosicionesActuales()))
      .subscribe(p => this.posiciones.set(p));
  }

  ngOnDestroy(): void { this.suscripcion?.unsubscribe(); }

  cargarPosiciones(): void {
    this.servicioVehiculos.getPosicionesActuales().subscribe({
      next: p => { this.posiciones.set(p); this.cargando.set(false); },
      error: () => this.cargando.set(false),
    });
  }
}
