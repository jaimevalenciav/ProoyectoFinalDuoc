import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';
import { Vehiculo, VehiculoRequest, GpsPosicionActual, PagedResponse } from '../models';

@Injectable({ providedIn: 'root' })
export class VehiculosService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/vehiculos`;
  private readonly gpsBase = `${environment.apiUrl}/gps`;

  getAll(params?: { estado?: string; search?: string; page?: number; size?: number }): Observable<PagedResponse<Vehiculo>> {
    let p = new HttpParams();
    if (params?.estado) p = p.set('estado', params.estado);
    if (params?.search) p = p.set('search', params.search);
    if (params?.page !== undefined) p = p.set('page', params.page);
    if (params?.size !== undefined) p = p.set('size', params.size ?? 20);
    return this.http.get<PagedResponse<Vehiculo>>(this.base, { params: p });
  }

  getById(id: string): Observable<Vehiculo> {
    return this.http.get<Vehiculo>(`${this.base}/${id}`);
  }

  create(req: VehiculoRequest): Observable<Vehiculo> {
    return this.http.post<Vehiculo>(this.base, req);
  }

  update(id: string, req: VehiculoRequest): Observable<Vehiculo> {
    return this.http.put<Vehiculo>(`${this.base}/${id}`, req);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }

  getHistorial(id: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.base}/${id}/historial`);
  }

  getPosicionesActuales(): Observable<GpsPosicionActual[]> {
    return this.http.get<GpsPosicionActual[]>(`${this.gpsBase}/posiciones-actuales`);
  }

  getRecorrido(vehiculoId: string, desde: string, hasta: string): Observable<any[]> {
    const params = new HttpParams().set('vehiculoId', vehiculoId).set('desde', desde).set('hasta', hasta);
    return this.http.get<any[]>(`${this.gpsBase}/recorrido`, { params });
  }

}
