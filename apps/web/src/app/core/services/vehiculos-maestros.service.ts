import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';
import {
  Sucursal, Municipalidad, Aseguradora, PlantaRevision,
  PermisoCirculacion, SeguroSoap, RevisionTecnica
} from '../models';

@Injectable({ providedIn: 'root' })
export class VehiculosMaestrosService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiUrl;

  // ── Sucursales ────────────────────────────────────────────────
  getSucursales(): Observable<Sucursal[]> {
    return this.http.get<Sucursal[]>(`${this.base}/sucursales`);
  }
  createSucursal(dto: Partial<Sucursal>): Observable<Sucursal> {
    return this.http.post<Sucursal>(`${this.base}/sucursales`, dto);
  }
  updateSucursal(id: string, dto: Partial<Sucursal>): Observable<Sucursal> {
    return this.http.put<Sucursal>(`${this.base}/sucursales/${id}`, dto);
  }
  deleteSucursal(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/sucursales/${id}`);
  }

  // ── Municipalidades ───────────────────────────────────────────
  getMunicipalidades(): Observable<Municipalidad[]> {
    return this.http.get<Municipalidad[]>(`${this.base}/municipalidades`);
  }
  createMunicipalidad(dto: Partial<Municipalidad>): Observable<Municipalidad> {
    return this.http.post<Municipalidad>(`${this.base}/municipalidades`, dto);
  }
  updateMunicipalidad(id: string, dto: Partial<Municipalidad>): Observable<Municipalidad> {
    return this.http.put<Municipalidad>(`${this.base}/municipalidades/${id}`, dto);
  }
  deleteMunicipalidad(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/municipalidades/${id}`);
  }

  // ── Aseguradoras ──────────────────────────────────────────────
  getAseguradoras(): Observable<Aseguradora[]> {
    return this.http.get<Aseguradora[]>(`${this.base}/aseguradoras`);
  }
  createAseguradora(dto: Partial<Aseguradora>): Observable<Aseguradora> {
    return this.http.post<Aseguradora>(`${this.base}/aseguradoras`, dto);
  }
  updateAseguradora(id: string, dto: Partial<Aseguradora>): Observable<Aseguradora> {
    return this.http.put<Aseguradora>(`${this.base}/aseguradoras/${id}`, dto);
  }
  deleteAseguradora(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/aseguradoras/${id}`);
  }

  // ── Plantas de Revisión ───────────────────────────────────────
  getPlantasRevision(): Observable<PlantaRevision[]> {
    return this.http.get<PlantaRevision[]>(`${this.base}/plantas-revision`);
  }
  createPlantaRevision(dto: Partial<PlantaRevision>): Observable<PlantaRevision> {
    return this.http.post<PlantaRevision>(`${this.base}/plantas-revision`, dto);
  }
  updatePlantaRevision(id: string, dto: Partial<PlantaRevision>): Observable<PlantaRevision> {
    return this.http.put<PlantaRevision>(`${this.base}/plantas-revision/${id}`, dto);
  }
  deletePlantaRevision(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/plantas-revision/${id}`);
  }

  // ── Permiso Circulación ───────────────────────────────────────
  getPermisosCirculacion(vehiculoId: string): Observable<PermisoCirculacion[]> {
    return this.http.get<PermisoCirculacion[]>(`${this.base}/permisos-circulacion`, {
      params: new HttpParams().set('vehiculoId', vehiculoId)
    });
  }
  createPermisoCirculacion(dto: Partial<PermisoCirculacion>): Observable<PermisoCirculacion> {
    return this.http.post<PermisoCirculacion>(`${this.base}/permisos-circulacion`, dto);
  }
  deletePermisoCirculacion(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/permisos-circulacion/${id}`);
  }

  // ── Seguro SOAP ───────────────────────────────────────────────
  getSegurosSOAP(vehiculoId: string): Observable<SeguroSoap[]> {
    return this.http.get<SeguroSoap[]>(`${this.base}/seguros-soap`, {
      params: new HttpParams().set('vehiculoId', vehiculoId)
    });
  }
  createSeguroSOAP(dto: Partial<SeguroSoap>): Observable<SeguroSoap> {
    return this.http.post<SeguroSoap>(`${this.base}/seguros-soap`, dto);
  }
  deleteSeguroSOAP(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/seguros-soap/${id}`);
  }

  // ── Revisión Técnica ──────────────────────────────────────────
  getRevisionesTecnicas(vehiculoId: string): Observable<RevisionTecnica[]> {
    return this.http.get<RevisionTecnica[]>(`${this.base}/revisiones-tecnicas`, {
      params: new HttpParams().set('vehiculoId', vehiculoId)
    });
  }
  createRevisionTecnica(dto: Partial<RevisionTecnica>): Observable<RevisionTecnica> {
    return this.http.post<RevisionTecnica>(`${this.base}/revisiones-tecnicas`, dto);
  }
  deleteRevisionTecnica(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/revisiones-tecnicas/${id}`);
  }
}
