import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';
import { OrdenTrabajo, OrdenTrabajoRequest, DetalleOT, TareaOT, PagedResponse } from '../models';

@Injectable({ providedIn: 'root' })
export class TallerService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/ordenes-trabajo`;

  getAll(params?: { estado?: string; tipo?: string; vehiculoId?: string; size?: number }): Observable<PagedResponse<OrdenTrabajo>> {
    let p = new HttpParams();
    if (params?.estado)     p = p.set('estado', params.estado);
    if (params?.tipo)       p = p.set('tipo', params.tipo);
    if (params?.vehiculoId) p = p.set('vehiculoId', params.vehiculoId);
    if (params?.size)       p = p.set('size', String(params.size));
    return this.http.get<PagedResponse<OrdenTrabajo>>(this.base, { params: p });
  }

  getById(id: string): Observable<OrdenTrabajo> {
    return this.http.get<OrdenTrabajo>(`${this.base}/${id}`);
  }

  create(req: OrdenTrabajoRequest): Observable<OrdenTrabajo> {
    return this.http.post<OrdenTrabajo>(this.base, req);
  }

  update(id: string, data: Partial<OrdenTrabajo>): Observable<OrdenTrabajo> {
    return this.http.put<OrdenTrabajo>(`${this.base}/${id}`, data);
  }

  cerrar(id: string, data: { costoManoObra: number; notas?: string }): Observable<OrdenTrabajo> {
    return this.http.post<OrdenTrabajo>(`${this.base}/${id}/cerrar`, data);
  }

  agregarTarea(otId: string, descripcion: string): Observable<TareaOT> {
    return this.http.post<TareaOT>(`${this.base}/${otId}/tareas`, { descripcion });
  }

  completarTarea(otId: string, tareaId: string): Observable<OrdenTrabajo> {
    return this.http.patch<OrdenTrabajo>(`${this.base}/${otId}/tareas/${tareaId}/completar`, {});
  }

  eliminarTarea(otId: string, tareaId: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${otId}/tareas/${tareaId}`);
  }

  agregarRepuesto(id: string, detalle: Partial<DetalleOT>): Observable<DetalleOT> {
    return this.http.post<DetalleOT>(`${this.base}/${id}/repuestos`, detalle);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
