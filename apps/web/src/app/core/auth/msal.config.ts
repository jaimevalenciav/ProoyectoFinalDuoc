import {
  BrowserCacheLocation,
  InteractionType,
  IPublicClientApplication,
  LogLevel,
  PublicClientApplication,
} from '@azure/msal-browser';
import {
  MsalGuardConfiguration,
  MsalInterceptorConfiguration,
} from '@azure/msal-angular';
import { environment } from '@env/environment';

export function msalInstanceFactory(): IPublicClientApplication {
  return new PublicClientApplication({
    auth: environment.msalConfig.auth,
    cache: {
      cacheLocation: BrowserCacheLocation.LocalStorage,
    },
    system: {
      loggerOptions: {
        logLevel: environment.production ? LogLevel.Warning : LogLevel.Info,
        piiLoggingEnabled: false,
      },
    },
  });
}

export function msalInterceptorConfigFactory(): MsalInterceptorConfiguration {
  return {
    interactionType: InteractionType.Redirect,
    protectedResourceMap: new Map(),
  };
}

export function msalGuardConfigFactory(): MsalGuardConfiguration {
  return {
    interactionType: InteractionType.Redirect,
  };
}
