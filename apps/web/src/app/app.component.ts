import { Component, OnInit, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { MsalService, MsalBroadcastService } from '@azure/msal-angular';
import { InteractionStatus } from '@azure/msal-browser';
import { filter, switchMap } from 'rxjs/operators';
import { PerfilService } from '@core/services/perfil.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  template: `<router-outlet />`,
})
export class AppComponent implements OnInit {
  private readonly msal      = inject(MsalService);
  private readonly broadcast = inject(MsalBroadcastService);
  private readonly perfil    = inject(PerfilService);

  ngOnInit(): void {
    this.msal.initialize().pipe(
      switchMap(() => this.msal.handleRedirectObservable()),
    ).subscribe({
      next: (result) => {
        if (result) {
          console.log('[MSAL] Login exitoso:', result.account?.username);
          // Sincronizar perfil con el backend tras un login exitoso
          this.perfil.sincronizarPerfil();
        }
      },
      error: (err) => console.error('[MSAL] Error en redirect:', err),
    });

    this.broadcast.inProgress$
      .pipe(filter(s => s === InteractionStatus.None))
      .subscribe(() => {
        this.msal.instance.enableAccountStorageEvents();
        // Sincronizar perfil si ya hay sesión activa (recarga de página)
        if (this.msal.instance.getAllAccounts().length > 0) {
          this.perfil.sincronizarPerfil();
        }
      });
  }
}
