import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';
import { Conductor, ConductorRequest, PagedResponse } from '../models';

@Injectable({ providedIn: 'root' })
export class ConductoresService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/conductores`;

  getAll(params?: { estado?: string; search?: string }): Observable<PagedResponse<Conductor>> {
    let p = new HttpParams();
    if (params?.estado) p = p.set('estado', params.estado);
    if (params?.search) p = p.set('search', params.search);
    return this.http.get<PagedResponse<Conductor>>(this.base, { params: p });
  }

  getById(id: string): Observable<Conductor> {
    return this.http.get<Conductor>(`${this.base}/${id}`);
  }

  create(req: ConductorRequest): Observable<Conductor> {
    return this.http.post<Conductor>(this.base, req);
  }

  update(id: string, req: ConductorRequest): Observable<Conductor> {
    return this.http.put<Conductor>(`${this.base}/${id}`, req);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }

  getScore(id: string): Observable<{ score: number; detalle: any }> {
    return this.http.get<{ score: number; detalle: any }>(`${this.base}/${id}/score`);
  }
}
