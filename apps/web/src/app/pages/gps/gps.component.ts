import {
  Component, OnInit, OnDestroy, AfterViewInit,
  inject, signal, computed, ElementRef, ViewChild, NgZone,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { VehiculosService } from '@core/services/vehiculos.service';
import { GpsPosicionActual, Vehiculo } from '@core/models';
import { interval, Subscription } from 'rxjs';
import { switchMap } from 'rxjs/operators';
import * as L from 'leaflet';

// ── Corrige la ruta de los íconos por defecto de Leaflet en Webpack/Angular ──
delete (L.Icon.Default.prototype as any)._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
  iconUrl:       'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
  shadowUrl:     'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
});

// ── Ícono personalizado de camión ─────────────────────────────────────────────
function iconoCamion(enLinea: boolean): L.DivIcon {
  return L.divIcon({
    className: '',
    html: `
      <div style="
        background:${enLinea ? '#1b5e20' : '#455a64'};
        border:3px solid #fff;
        border-radius:50% 50% 50% 0;
        transform:rotate(-45deg);
        width:32px; height:32px;
        box-shadow:0 2px 8px rgba(0,0,0,.35);
        display:flex; align-items:center; justify-content:center;">
        <span style="transform:rotate(45deg); font-size:14px; color:#fff;">🚛</span>
      </div>`,
    iconSize:   [32, 32],
    iconAnchor: [16, 32],
    popupAnchor:[0, -34],
  });
}

@Component({
  selector: 'app-gps',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatSelectModule, MatButtonModule, MatIconModule,
    MatProgressSpinnerModule, MatTooltipModule,
  ],
  template: `
    <div class="encabezado-pagina">
      <h1>GPS — Seguimiento en tiempo real</h1>
      <div class="acciones-encabezado">
        <mat-form-field appearance="fill" style="width:200px">
          <mat-label>Filtrar vehículo</mat-label>
          <mat-select [(ngModel)]="vehiculoSeleccionado" (ngModelChange)="onFiltroChange()">
            <mat-option value="">Todos</mat-option>
            @for (v of vehiculos(); track v.id) {
              <mat-option [value]="v.id">{{ v.patente }}</mat-option>
            }
          </mat-select>
        </mat-form-field>

        <button mat-flat-button class="btn-principal" (click)="cargarPosiciones()"
          [disabled]="cargando()">
          <mat-icon>refresh</mat-icon> Actualizar
        </button>

        <span class="insignia insignia-exito indicador-auto">
          <mat-icon style="font-size:14px;height:14px;width:14px;margin-right:4px">autorenew</mat-icon>
          Cada 30s
        </span>

        <span class="insignia" [class.insignia-exito]="posiciones().length > 0"
          [class.insignia-advertencia]="posiciones().length === 0">
          {{ posiciones().length }} veh. en línea
        </span>
      </div>
    </div>

    <!-- ── Mapa Leaflet ──────────────────────────────────────────────────── -->
    <div class="superficie area-mapa">
      <div #mapaRef id="mapa-leaflet" class="contenedor-mapa"></div>

      @if (cargando()) {
        <div class="overlay-carga">
          <mat-spinner diameter="40" />
          <span>Cargando posiciones GPS…</span>
        </div>
      }

      @if (!cargando() && posiciones().length === 0) {
        <div class="overlay-vacio">
          <mat-icon>gps_off</mat-icon>
          <span>Sin datos GPS disponibles</span>
        </div>
      }
    </div>

    <!-- ── Panel lateral / lista ─────────────────────────────────────────── -->
    <div class="titulo-seccion">
      Flota en línea ({{ posicionesFiltradas().length }})
    </div>

    <div class="superficie" style="padding:0;overflow:hidden">

      @for (pos of posicionesFiltradas(); track pos.vehiculoId) {
        <div class="fila-vehiculo-gps" (click)="centrarEn(pos)"
          [class.fila-seleccionada]="seleccionado()?.vehiculoId === pos.vehiculoId">

          <div class="icono-vehiculo-gps">
            <mat-icon>local_shipping</mat-icon>
          </div>

          <div class="datos-vehiculo-gps">
            <span class="placa-gps">{{ pos.patente }}</span>
            <span class="modelo-gps texto-atenuado">{{ pos.marca }} {{ pos.modelo }}</span>
            @if (pos.conductorNombre) {
              <span class="conductor-gps texto-atenuado">
                <mat-icon style="font-size:12px;height:12px;width:12px">person</mat-icon>
                {{ pos.conductorNombre }}
              </span>
            }
            <span class="coords-gps texto-atenuado">
              {{ pos.latitud | number:'1.5-5' }}, {{ pos.longitud | number:'1.5-5' }}
            </span>
            <span class="hora-gps texto-atenuado">
              {{ (pos.recordedAt ? pos.recordedAt + 'Z' : null) | date:'dd/MM HH:mm':'America/Santiago' }}
            </span>
          </div>

          <div class="panel-derecho">
            <div class="velocidad-gps">{{ pos.velocidad ?? 0 }} <small>km/h</small></div>
            <span class="insignia insignia-exito">En línea</span>
            <button mat-icon-button class="btn-centrar"
              matTooltip="Ver en mapa"
              (click)="$event.stopPropagation(); centrarEn(pos)">
              <mat-icon>my_location</mat-icon>
            </button>
          </div>
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
    /* ── Encabezado ──────────────────────────────────────────────────────── */
    .indicador-auto { display:flex; align-items:center; font-size:12px; }

    /* ── Mapa ────────────────────────────────────────────────────────────── */
    .area-mapa { position:relative; margin-bottom:16px; padding:0; overflow:hidden; border-radius:var(--radio-lg); }

    .contenedor-mapa { width:100%; height:480px; z-index:1; border-radius:var(--radio-lg); }

    .overlay-carga,
    .overlay-vacio {
      position:absolute; inset:0;
      display:flex; flex-direction:column; align-items:center; justify-content:center;
      gap:12px; background:rgba(255,255,255,.75); z-index:10;
      color:var(--azul-700); font-size:14px;
      mat-icon { font-size:48px; width:48px; height:48px; opacity:.4; }
    }

    /* ── Lista de vehículos ──────────────────────────────────────────────── */
    .titulo-seccion { font-size:17px; font-weight:600; color:var(--azul-900); margin-bottom:12px; }

    .fila-vehiculo-gps {
      display:flex; align-items:center; gap:14px;
      padding:14px 20px;
      border-bottom:1px solid var(--azul-50);
      cursor:pointer; transition:background .15s;
      &:last-child { border-bottom:none; }
      &:hover { background:var(--azul-50); }
    }
    .fila-seleccionada { background:var(--azul-100) !important; }

    .icono-vehiculo-gps {
      width:40px; height:40px; border-radius:var(--radio-md);
      background:var(--azul-100); display:flex; align-items:center; justify-content:center;
      flex-shrink:0;
      mat-icon { color:var(--azul-600); }
    }

    .datos-vehiculo-gps { display:flex; flex-direction:column; flex:1; gap:2px; }
    .placa-gps     { font-weight:700; font-size:14px; color:var(--azul-900); }
    .modelo-gps,
    .conductor-gps,
    .coords-gps,
    .hora-gps      { font-size:12px; display:flex; align-items:center; gap:2px; }

    .panel-derecho { display:flex; flex-direction:column; align-items:flex-end; gap:4px; min-width:80px; }
    .velocidad-gps { font-size:15px; font-weight:700; color:var(--azul-600); }

    .btn-centrar { width:32px; height:32px; line-height:32px;
      mat-icon { font-size:18px; color:var(--azul-500); } }

    .estado-vacio-tabla {
      display:flex; flex-direction:column; align-items:center;
      padding:40px; gap:8px; color:var(--color-texto-3);
      mat-icon { font-size:40px; width:40px; height:40px; opacity:.3; }
    }
  `],
})
export class GpsComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('mapaRef') mapaRef!: ElementRef<HTMLDivElement>;

  private readonly servicioVehiculos = inject(VehiculosService);
  private readonly ngZone            = inject(NgZone);

  private mapa?: L.Map;
  private markers = new Map<string, L.Marker>();
  private suscripcion?: Subscription;

  cargando             = signal(true);
  vehiculos            = signal<Vehiculo[]>([]);
  posiciones           = signal<GpsPosicionActual[]>([]);
  seleccionado         = signal<GpsPosicionActual | null>(null);
  vehiculoSeleccionado = '';

  posicionesFiltradas = computed(() =>
    this.vehiculoSeleccionado
      ? this.posiciones().filter(p => p.vehiculoId === this.vehiculoSeleccionado)
      : this.posiciones()
  );

  // ── Ciclo de vida ────────────────────────────────────────────────────────
  ngOnInit(): void {
    this.servicioVehiculos.getAll({ size: 100 })
      .subscribe(r => this.vehiculos.set(r.content));

    // Carga inicial: el mapa puede no estar listo aún (se crea en ngAfterViewInit)
    // Los marcadores se pintarán en ngAfterViewInit una vez el mapa exista
    this.cargarPosiciones();

    // Refresco automático cada 30 s
    this.suscripcion = interval(30_000)
      .pipe(switchMap(() => this.servicioVehiculos.getPosicionesActuales()))
      .subscribe(p => {
        this.posiciones.set(p);
        this.ngZone.runOutsideAngular(() => this.actualizarMarcadores(p));
      });
  }

  ngAfterViewInit(): void {
    this.ngZone.runOutsideAngular(() => {
      this.initMapa();
      // Pintar marcadores con los datos ya cargados (si llegaron antes que el mapa)
      const pos = this.posiciones();
      if (pos.length > 0) this.actualizarMarcadores(pos);
    });
  }

  ngOnDestroy(): void {
    this.suscripcion?.unsubscribe();
    this.mapa?.remove();
  }

  // ── Mapa ─────────────────────────────────────────────────────────────────
  private initMapa(): void {
    // Centro por defecto: Santiago de Chile
    this.mapa = L.map(this.mapaRef.nativeElement, {
      center:      [-33.45, -70.67],
      zoom:        11,
      zoomControl: true,
    });

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
      maxZoom: 19,
    }).addTo(this.mapa);
  }

  private actualizarMarcadores(posiciones: GpsPosicionActual[]): void {
    if (!this.mapa) return;

    const idsActuales = new Set(posiciones.map(p => p.vehiculoId));

    // Eliminar marcadores de vehículos que ya no están
    this.markers.forEach((marker, id) => {
      if (!idsActuales.has(id)) {
        marker.remove();
        this.markers.delete(id);
      }
    });

    const bounds: L.LatLng[] = [];

    posiciones.forEach(pos => {
      if (!pos.latitud || !pos.longitud) return;
      const lat = Number(pos.latitud);
      const lng = Number(pos.longitud);
      if (isNaN(lat) || isNaN(lng)) return;

      const latlng = L.latLng(lat, lng);
      bounds.push(latlng);

      const popupHtml = `
        <div style="min-width:180px; font-family:Inter,sans-serif;">
          <strong style="font-size:15px;color:#1b2c40">🚛 ${pos.patente}</strong><br>
          <span style="color:#666;font-size:12px">${pos.marca ?? ''} ${pos.modelo ?? ''}</span>
          ${pos.conductorNombre ? `<br><span style="font-size:12px">👤 ${pos.conductorNombre}</span>` : ''}
          <hr style="margin:6px 0;border-color:#eee">
          <table style="font-size:12px;width:100%;border-collapse:collapse">
            <tr><td style="color:#888;padding:1px 4px 1px 0">Velocidad</td><td><b>${pos.velocidad ?? 0} km/h</b></td></tr>
            <tr><td style="color:#888;padding:1px 4px 1px 0">Latitud</td><td>${lat.toFixed(6)}</td></tr>
            <tr><td style="color:#888;padding:1px 4px 1px 0">Longitud</td><td>${lng.toFixed(6)}</td></tr>
            <tr><td style="color:#888;padding:1px 4px 1px 0">Hora</td><td>${pos.recordedAt ? new Date(pos.recordedAt + 'Z').toLocaleTimeString('es-CL', {timeZone:'America/Santiago'}) : '—'}</td></tr>
          </table>
        </div>`;

      if (this.markers.has(pos.vehiculoId)) {
        const marker = this.markers.get(pos.vehiculoId)!;
        marker.setLatLng(latlng);
        marker.setIcon(iconoCamion(true));
        marker.setPopupContent(popupHtml);
      } else {
        const marker = L.marker(latlng, { icon: iconoCamion(true) })
          .bindPopup(popupHtml, { maxWidth: 250 })
          .addTo(this.mapa!);
        this.markers.set(pos.vehiculoId, marker);
      }
    });

    // Ajustar vista si hay marcadores y el usuario no está en filtro específico
    if (bounds.length > 0 && !this.vehiculoSeleccionado) {
      this.mapa.fitBounds(L.latLngBounds(bounds), { padding: [40, 40], maxZoom: 14 });
    }
  }

  // ── Acciones ─────────────────────────────────────────────────────────────
  cargarPosiciones(): void {
    this.cargando.set(true);
    this.servicioVehiculos.getPosicionesActuales().subscribe({
      next: p => {
        this.posiciones.set(p);
        this.cargando.set(false);
        this.ngZone.runOutsideAngular(() => this.actualizarMarcadores(p));
      },
      error: () => this.cargando.set(false),
    });
  }

  centrarEn(pos: GpsPosicionActual): void {
    this.seleccionado.set(pos);
    if (!this.mapa) return;
    const lat = Number(pos.latitud);
    const lng = Number(pos.longitud);
    if (isNaN(lat) || isNaN(lng)) return;

    this.ngZone.runOutsideAngular(() => {
      this.mapa!.setView([lat, lng], 16, { animate: true });
      this.markers.get(pos.vehiculoId)?.openPopup();
    });
  }

  onFiltroChange(): void {
    const filtradas = this.posicionesFiltradas();
    if (!this.mapa || filtradas.length === 0) return;

    this.ngZone.runOutsideAngular(() => {
      if (filtradas.length === 1) {
        this.centrarEn(filtradas[0]);
      } else {
        const bounds = filtradas
          .filter(p => p.latitud && p.longitud)
          .map(p => L.latLng(Number(p.latitud), Number(p.longitud)));
        if (bounds.length > 0)
          this.mapa!.fitBounds(L.latLngBounds(bounds), { padding: [40, 40], maxZoom: 14 });
      }
    });
  }
}
