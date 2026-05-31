import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';
import { TareaDefinicion, PagedResponse } from '@core/models';

export interface TareaDefinicionDto {
  nombre: string;
  descripcion?: string;
  tipoOt?: string;
  articulos: { repuestoId: string; cantidad: number }[];
}

@Injectable({ providedIn: 'root' })
export class TareasDefinicionService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/tareas-definicion`;

  getAll(pagina = 0, tamano = 50): Observable<PagedResponse<TareaDefinicion>> {
    const p = new HttpParams().set('pagina', pagina).set('tamano', tamano);
    return this.http.get<PagedResponse<TareaDefinicion>>(this.base, { params: p });
  }

  getAllActivos(): Observable<TareaDefinicion[]> {
    return this.http.get<TareaDefinicion[]>(`${this.base}/activos`);
  }

  getById(id: string): Observable<TareaDefinicion> {
    return this.http.get<TareaDefinicion>(`${this.base}/${id}`);
  }

  create(dto: TareaDefinicionDto): Observable<TareaDefinicion> {
    return this.http.post<TareaDefinicion>(this.base, dto);
  }

  update(id: string, dto: TareaDefinicionDto): Observable<TareaDefinicion> {
    return this.http.put<TareaDefinicion>(`${this.base}/${id}`, dto);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
