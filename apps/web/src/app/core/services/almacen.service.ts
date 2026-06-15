import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';
import { Repuesto, MovimientoStock, PagedResponse } from '../models';

export interface AjusteStockDto {
  tipo: 'ENTRADA' | 'SALIDA' | 'AJUSTE';
  cantidad: number;
  precioUnit?: number;
  motivo?: string;
  otId?: string;
  referencia?: string;
  documento?: string;
  observacion?: string;
}

export interface IngresoFacturaLineaDto {
  repuestoId: string;
  cantidad: number;
  precioUnit: number;
}

export interface IngresoFacturaDto {
  tipoDocumento: 'FACTURA' | 'GUIA_DESPACHO';
  numDocumento: string;
  proveedor: string;
  rutProveedor?: string;
  fechaDocumento: string;
  lineas: IngresoFacturaLineaDto[];
}

@Injectable({ providedIn: 'root' })
export class AlmacenService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/repuestos`;
  private readonly movBase = `${environment.apiUrl}/movimientos-stock`;

  getAll(params?: { search?: string; categoria?: string; pagina?: number; tamano?: number }): Observable<PagedResponse<Repuesto>> {
    let p = new HttpParams();
    if (params?.search)    p = p.set('search', params.search);
    if (params?.categoria) p = p.set('categoria', params.categoria);
    p = p.set('pagina', params?.pagina ?? 0);
    p = p.set('tamano', params?.tamano ?? 50);
    return this.http.get<PagedResponse<Repuesto>>(this.base, { params: p });
  }

  getAllActivos(): Observable<Repuesto[]> {
    return this.http.get<Repuesto[]>(`${this.base}/activos`);
  }

  getBajoStock(): Observable<Repuesto[]> {
    return this.http.get<Repuesto[]>(`${this.base}/bajo-stock`);
  }

  getById(id: string): Observable<Repuesto> {
    return this.http.get<Repuesto>(`${this.base}/${id}`);
  }

  create(req: Partial<Repuesto>): Observable<Repuesto> {
    return this.http.post<Repuesto>(this.base, req);
  }

  update(id: string, req: Partial<Repuesto>): Observable<Repuesto> {
    return this.http.put<Repuesto>(`${this.base}/${id}`, req);
  }

  ajustarStock(id: string, dto: AjusteStockDto): Observable<Repuesto> {
    return this.http.post<Repuesto>(`${this.base}/${id}/ajuste`, dto);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }

  getMovimientos(params?: {
    repuestoId?: string;
    tipo?: string;
    documento?: string;
    desde?: string;
    hasta?: string;
    pagina?: number;
    tamano?: number;
  }): Observable<PagedResponse<MovimientoStock>> {
    let p = new HttpParams()
      .set('pagina', params?.pagina ?? 0)
      .set('tamano', params?.tamano ?? 30);
    if (params?.repuestoId) p = p.set('repuestoId', params.repuestoId);
    if (params?.tipo)       p = p.set('tipo', params.tipo);
    if (params?.documento)  p = p.set('documento', params.documento);
    if (params?.desde)      p = p.set('desde', params.desde);
    if (params?.hasta)      p = p.set('hasta', params.hasta);
    return this.http.get<PagedResponse<MovimientoStock>>(this.movBase, { params: p });
  }

  ingresoFactura(dto: IngresoFacturaDto): Observable<{ movimientos: number; totalCLP: number }> {
    return this.http.post<{ movimientos: number; totalCLP: number }>(
      `${this.base}/ingreso-factura`, dto
    );
  }
}
