import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';
import { Mecanico, PagedResponse } from '../models';

@Injectable({ providedIn: 'root' })
export class MecanicosService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/mecanicos`;

  getAll(params?: {
    busqueda?: string;
    activo?: number;
    pagina?: number;
    tamano?: number;
  }): Observable<PagedResponse<Mecanico>> {
    let p = new HttpParams()
      .set('pagina', params?.pagina ?? 0)
      .set('tamano', params?.tamano ?? 50);
    if (params?.busqueda)            p = p.set('busqueda', params.busqueda);
    if (params?.activo !== undefined) p = p.set('activo',   params.activo);
    return this.http.get<PagedResponse<Mecanico>>(this.base, { params: p });
  }

  getActivos(): Observable<Mecanico[]> {
    return this.http.get<Mecanico[]>(`${this.base}/activos`);
  }

  getById(id: string): Observable<Mecanico> {
    return this.http.get<Mecanico>(`${this.base}/${id}`);
  }

  create(dto: Partial<Mecanico>): Observable<Mecanico> {
    return this.http.post<Mecanico>(this.base, dto);
  }

  update(id: string, dto: Partial<Mecanico>): Observable<Mecanico> {
    return this.http.put<Mecanico>(`${this.base}/${id}`, dto);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
