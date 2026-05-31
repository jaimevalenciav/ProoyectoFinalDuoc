import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';
import { AlertaCombustible, CargaAdBlue, CargaCombustible, Servicio, ServicioRequest, Cliente, ClienteRequest, TipoServicio, PagedResponse } from '../models';

@Injectable({ providedIn: 'root' })
export class OperacionesService {
  private readonly http = inject(HttpClient);
  private readonly urlBase = environment.apiUrl;

  // ── Combustible ───────────────────────────────────────────
  getCargas(params?: { vehiculoId?: string; desde?: string; hasta?: string }): Observable<PagedResponse<CargaCombustible>> {
    let p = new HttpParams();
    if (params?.vehiculoId) p = p.set('vehiculoId', params.vehiculoId);
    if (params?.desde) p = p.set('desde', params.desde);
    if (params?.hasta) p = p.set('hasta', params.hasta);
    return this.http.get<PagedResponse<CargaCombustible>>(`${this.urlBase}/combustible/cargas`, { params: p });
  }

  registrarCarga(req: Partial<CargaCombustible>): Observable<CargaCombustible> {
    return this.http.post<CargaCombustible>(`${this.urlBase}/combustible/cargas`, req);
  }

  getAnomalias(): Observable<CargaCombustible[]> {
    return this.http.get<CargaCombustible[]>(`${this.urlBase}/combustible/anomalias`);
  }

  // ── Tipos de Servicio ──────────────────────────────────────
  getTiposServicio(): Observable<TipoServicio[]> {
    return this.http.get<TipoServicio[]>(`${this.urlBase}/tipos-servicio`);
  }

  // ── Servicios ─────────────────────────────────────────────
  getServicios(params?: {
    clienteId?: string;
    estado?: string;
    desde?: string;
    hasta?: string;
    facturado?: number;
    pagina?: number;
    tamano?: number;
  }): Observable<PagedResponse<Servicio>> {
    let p = new HttpParams();
    if (params?.clienteId)             p = p.set('clienteId', params.clienteId);
    if (params?.estado)                p = p.set('estado', params.estado);
    if (params?.desde)                 p = p.set('desde', params.desde);
    if (params?.hasta)                 p = p.set('hasta', params.hasta);
    if (params?.facturado !== undefined) p = p.set('facturado', String(params.facturado));
    if (params?.pagina !== undefined)  p = p.set('pagina', String(params.pagina));
    if (params?.tamano !== undefined)  p = p.set('tamano', String(params.tamano));
    return this.http.get<PagedResponse<Servicio>>(`${this.urlBase}/servicios`, { params: p });
  }

  getServicioById(id: string): Observable<Servicio> {
    return this.http.get<Servicio>(`${this.urlBase}/servicios/${id}`);
  }

  createServicio(req: ServicioRequest): Observable<Servicio> {
    return this.http.post<Servicio>(`${this.urlBase}/servicios`, req);
  }

  updateServicio(id: string, req: Partial<ServicioRequest>): Observable<Servicio> {
    return this.http.put<Servicio>(`${this.urlBase}/servicios/${id}`, req);
  }

  facturarServicio(id: string, data: { tipoDocumento: string; numDocumento: string }): Observable<Servicio> {
    return this.http.post<Servicio>(`${this.urlBase}/servicios/${id}/facturar`, data);
  }

  deleteServicio(id: string): Observable<void> {
    return this.http.delete<void>(`${this.urlBase}/servicios/${id}`);
  }

  eliminarCarga(id: string): Observable<void> {
    return this.http.delete<void>(`${this.urlBase}/combustible/cargas/${id}`);
  }

  getUltimaCargaVehiculo(vehiculoId: string): Observable<CargaCombustible | null> {
    return this.http.get<CargaCombustible | null>(`${this.urlBase}/combustible/ultima-carga/${vehiculoId}`);
  }

  // ── Alertas Combustible ───────────────────────────────────
  /** activas=true → no leídas; activas=false → historial; omitido → todas */
  getAlertasCombustible(activas?: boolean): Observable<AlertaCombustible[]> {
    let p = new HttpParams();
    if (activas !== undefined) p = p.set('activas', String(activas));
    return this.http.get<AlertaCombustible[]>(`${this.urlBase}/combustible/alertas`, { params: p });
  }

  guardarAlertasCombustible(alertas: Partial<AlertaCombustible>[]): Observable<void> {
    return this.http.post<void>(`${this.urlBase}/combustible/alertas`, alertas);
  }

  marcarAlertaLeida(id: string, leidaPor: string): Observable<AlertaCombustible> {
    return this.http.patch<AlertaCombustible>(`${this.urlBase}/combustible/alertas/${id}/leida`, { leidaPor });
  }

  aprobarServicio(id: string): Observable<Servicio> {
    return this.http.patch<Servicio>(`${this.urlBase}/servicios/${id}/aprobar`, {});
  }

  asignarServicio(id: string, dto: { vehiculoId?: string; conductorId?: string }): Observable<Servicio> {
    return this.http.patch<Servicio>(`${this.urlBase}/servicios/${id}/asignar`, dto);
  }

  iniciarServicio(id: string): Observable<Servicio> {
    return this.http.patch<Servicio>(`${this.urlBase}/servicios/${id}/iniciar`, {});
  }

  completarServicio(id: string): Observable<Servicio> {
    return this.http.patch<Servicio>(`${this.urlBase}/servicios/${id}/completar`, {});
  }

  cancelarServicio(id: string): Observable<Servicio> {
    return this.http.patch<Servicio>(`${this.urlBase}/servicios/${id}/cancelar`, {});
  }

  getViajesConductor(conductorId: string): Observable<Servicio[]> {
    return this.http.get<Servicio[]>(`${this.urlBase}/servicios/conductor/${conductorId}`);
  }

  // ── Clientes ──────────────────────────────────────────────
  getClientes(search?: string): Observable<PagedResponse<Cliente>> {
    const params = search ? new HttpParams().set('search', search) : undefined;
    return this.http.get<PagedResponse<Cliente>>(`${this.urlBase}/clientes`, { params });
  }

  getClienteById(id: string): Observable<Cliente> {
    return this.http.get<Cliente>(`${this.urlBase}/clientes/${id}`);
  }

  createCliente(req: ClienteRequest): Observable<Cliente> {
    return this.http.post<Cliente>(`${this.urlBase}/clientes`, req);
  }

  updateCliente(id: string, req: ClienteRequest): Observable<Cliente> {
    return this.http.put<Cliente>(`${this.urlBase}/clientes/${id}`, req);
  }

  deleteCliente(id: string): Observable<void> {
    return this.http.delete<void>(`${this.urlBase}/clientes/${id}`);
  }

  getFacturacionCliente(id: string, desde?: string, hasta?: string): Observable<any> {
    let p = new HttpParams();
    if (desde) p = p.set('desde', desde);
    if (hasta) p = p.set('hasta', hasta);
    return this.http.get<any>(`${this.urlBase}/clientes/${id}/facturacion`, { params: p });
  }

  // ── AdBlue ────────────────────────────────────────────────
  getCargasAdBlue(params?: { vehiculoId?: string; desde?: string; hasta?: string }): Observable<PagedResponse<CargaAdBlue>> {
    let p = new HttpParams();
    if (params?.vehiculoId) p = p.set('vehiculoId', params.vehiculoId);
    if (params?.desde)      p = p.set('desde', params.desde);
    if (params?.hasta)      p = p.set('hasta', params.hasta);
    return this.http.get<PagedResponse<CargaAdBlue>>(`${this.urlBase}/adblue/cargas`, { params: p });
  }

  registrarCargaAdBlue(req: Partial<CargaAdBlue>): Observable<CargaAdBlue> {
    return this.http.post<CargaAdBlue>(`${this.urlBase}/adblue/cargas`, req);
  }

  eliminarCargaAdBlue(id: string): Observable<void> {
    return this.http.delete<void>(`${this.urlBase}/adblue/cargas/${id}`);
  }

  getUltimaCargaAdBlueVehiculo(vehiculoId: string): Observable<CargaAdBlue | null> {
    return this.http.get<CargaAdBlue | null>(`${this.urlBase}/adblue/ultima-carga/${vehiculoId}`);
  }

  getAnomaliasAdBlue(): Observable<CargaAdBlue[]> {
    return this.http.get<CargaAdBlue[]>(`${this.urlBase}/adblue/anomalias`);
  }
}
