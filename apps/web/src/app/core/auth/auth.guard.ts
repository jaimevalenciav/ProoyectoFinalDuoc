import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { MsalService } from '@azure/msal-angular';

export const authGuard: CanActivateFn = () => {
  const msal = inject(MsalService);
  const router = inject(Router);
  const accounts = msal.instance.getAllAccounts();
  if (accounts.length > 0) return true;
  router.navigate(['/login']);
  return false;
};
