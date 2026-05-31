import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';
import { Factura, FacturarRequest, PagedResponse } from '../models';

@Injectable({ providedIn: 'root' })
export class FacturacionService {
  private readonly http = inject(HttpClient);
  readonly base = `${environment.apiUrl}/facturas`;

  getAll(params?: {
    clienteId?: string;
    estado?: string;
    pagina?: number;
    tamano?: number;
  }): Observable<PagedResponse<Factura>> {
    let p = new HttpParams();
    if (params?.clienteId)            p = p.set('clienteId', params.clienteId);
    if (params?.estado)               p = p.set('estado', params.estado);
    if (params?.pagina !== undefined) p = p.set('pagina', String(params.pagina));
    if (params?.tamano !== undefined) p = p.set('tamano', String(params.tamano));
    return this.http.get<PagedResponse<Factura>>(this.base, { params: p });
  }

  getById(id: string): Observable<Factura> {
    return this.http.get<Factura>(`${this.base}/${id}`);
  }

  facturar(req: FacturarRequest): Observable<Factura> {
    return this.http.post<Factura>(this.base, req);
  }

  anular(id: string): Observable<Factura> {
    return this.http.post<Factura>(`${this.base}/${id}/anular`, {});
  }
}
