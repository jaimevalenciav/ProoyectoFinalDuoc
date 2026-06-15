import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { UsuarioSistema, PerfilUsuario, RolUsuario } from '@core/models';

export interface UsuarioDto {
  nombre: string;
  email: string;
  rol: RolUsuario;
  azureOid?: string;
  activo?: number;
}

@Injectable({ providedIn: 'root' })
export class UsuariosService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/v1/usuarios';

  /** Perfil del usuario autenticado (crea el registro en BD si no existe) */
  perfilActual(): Observable<PerfilUsuario> {
    return this.http.get<PerfilUsuario>(`${this.base}/perfil-actual`);
  }

  /** Lista todos los usuarios de la empresa */
  listar(): Observable<UsuarioSistema[]> {
    return this.http.get<UsuarioSistema[]>(this.base);
  }

  /** Crea un nuevo usuario */
  crear(dto: UsuarioDto): Observable<UsuarioSistema> {
    return this.http.post<UsuarioSistema>(this.base, dto);
  }

  /** Actualiza datos de un usuario */
  actualizar(id: string, dto: UsuarioDto): Observable<UsuarioSistema> {
    return this.http.put<UsuarioSistema>(`${this.base}/${id}`, dto);
  }

  /** Cambia solo el rol */
  cambiarRol(id: string, rol: RolUsuario): Observable<{ rol: string }> {
    return this.http.patch<{ rol: string }>(`${this.base}/${id}/rol`, { rol });
  }

  /** Activa o desactiva un usuario */
  cambiarActivo(id: string, activo: boolean): Observable<{ activo: boolean }> {
    return this.http.patch<{ activo: boolean }>(`${this.base}/${id}/activo`, { activo });
  }

  /** Elimina un usuario */
  eliminar(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
