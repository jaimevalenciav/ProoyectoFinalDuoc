import { bootstrapApplication } from '@angular/platform-browser';
import { registerLocaleData } from '@angular/common';
import localeEsCL from '@angular/common/locales/es-CL';
import { appConfig } from './app/app.config';
import { AppComponent } from './app/app.component';

// Registrar locale chileno: punto como separador de miles, coma como decimal
registerLocaleData(localeEsCL, 'es-CL');

bootstrapApplication(AppComponent, appConfig).catch(err => console.error(err));
