import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';

export interface KpiReporte {
  // Estructura del backend ms-reportes
  totalVehiculos: number;
  vehiculosOperativos: number;
  vehiculosEnTaller: number;
  vehiculosFuera: number;
  totalOts: number;
  otsPendientes: number;
  otsEnEjecucion: number;
  otsCerradas: number;
  totalConductores: number;
  alertasBajoStock: number;
  // Campos del período (combustible, costos, ingresos)
  otCerradasMes?: number;
  costoMantenimientoMes?: number;
  litrosCargadosMes?: number;
  costoCombustibleMes?: number;
  ingresoServiciosMes?: number;
  kmRecorridosMes?: number;
  // legacy aliases (compatibilidad con dashboard y reportes)
  vehiculosActivos?: number;
  conductoresActivos?: number;
  otAbiertas?: number;
}

export interface ConsumoVehiculo {
  vehiculoId: string;
  placa: string;
  litrosTotales: number;
  kmTotales: number;
  rendimientoPromedio: number;
  costoTotal: number;
}

export interface CostoMantenimiento {
  mes: string;
  costoManoObra: number;
  costoRepuestos: number;
  total: number;
}

@Injectable({ providedIn: 'root' })
export class ReportesService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/reportes`;

  getKpis(desde?: string, hasta?: string): Observable<KpiReporte> {
    let p = new HttpParams();
    if (desde) p = p.set('desde', desde);
    if (hasta) p = p.set('hasta', hasta);
    return this.http.get<KpiReporte>(`${this.base}/kpis`, { params: p });
  }

  getConsumoFlota(desde?: string, hasta?: string): Observable<ConsumoVehiculo[]> {
    let p = new HttpParams();
    if (desde) p = p.set('desde', desde);
    if (hasta) p = p.set('hasta', hasta);
    return this.http.get<ConsumoVehiculo[]>(`${this.base}/consumo-flota`, { params: p });
  }

  getCostosMantenimiento(desde?: string, hasta?: string): Observable<CostoMantenimiento[]> {
    let p = new HttpParams();
    if (desde) p = p.set('desde', desde);
    if (hasta) p = p.set('hasta', hasta);
    return this.http.get<CostoMantenimiento[]>(`${this.base}/costos-mantenimiento`, { params: p });
  }

  exportarExcel(tipo: 'combustible' | 'mantenimiento' | 'servicios', desde?: string, hasta?: string): Observable<Blob> {
    let p = new HttpParams().set('tipo', tipo);
    if (desde) p = p.set('desde', desde);
    if (hasta) p = p.set('hasta', hasta);
    return this.http.get(`${this.base}/exportar`, { params: p, responseType: 'blob' });
  }
}
