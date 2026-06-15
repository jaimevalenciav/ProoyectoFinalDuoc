import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PerfilUsuario, RolUsuario } from '@core/models';

export interface InvitacionDto {
  rol: RolUsuario;
  emailSugerido?: string;
  nota?: string;
  diasVigencia?: number;
}

export interface InvitacionResumen {
  token: string;
  rol: RolUsuario;
  emailSugerido?: string;
  nota?: string;
  expiresAt: string;
  usada: boolean;
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class InvitacionesService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/v1/invitaciones';

  crear(dto: InvitacionDto): Observable<InvitacionResumen> {
    return this.http.post<InvitacionResumen>(this.base, dto);
  }

  listar(): Observable<InvitacionResumen[]> {
    return this.http.get<InvitacionResumen[]>(this.base);
  }

  /** Valida el token sin autenticación — usado en el onboarding antes de hacer login */
  validar(token: string): Observable<InvitacionResumen> {
    return this.http.get<InvitacionResumen>(`${this.base}/${token}/validar`);
  }

  /** El usuario autenticado acepta la invitación → recibe su perfil completo */
  aceptar(token: string): Observable<PerfilUsuario> {
    return this.http.post<PerfilUsuario>(`${this.base}/${token}/aceptar`, {});
  }

  revocar(token: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${token}`);
  }
}
