import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/**
 * Elimina puntos, guión y espacios del RUT
 */
export function limpiarRut(rut: string): string {
  return rut.replace(/\./g, '').replace(/-/g, '').replace(/\s/g, '').toUpperCase();
}

/**
 * Formatea un RUT limpio como XX.XXX.XXX-Y
 * Acepta con o sin puntos/guión.
 */
export function formatearRut(rut: string): string {
  const limpio = limpiarRut(rut);
  if (limpio.length < 2) return limpio;

  const cuerpo = limpio.slice(0, -1);
  const dv     = limpio.slice(-1);

  // Agrega puntos de mil al cuerpo
  const cuerpoFormateado = cuerpo
    .split('')
    .reverse()
    .reduce((acc, d, i) => (i > 0 && i % 3 === 0 ? d + '.' + acc : d + acc), '');

  return `${cuerpoFormateado}-${dv}`;
}

/**
 * Valida el dígito verificador de un RUT chileno.
 * @param rut Puede venir con o sin formato (puntos/guión)
 */
export function validarRut(rut: string): boolean {
  const limpio = limpiarRut(rut);
  if (limpio.length < 2) return false;

  const cuerpo = limpio.slice(0, -1);
  const dvIngresado = limpio.slice(-1);

  if (!/^\d+$/.test(cuerpo)) return false;

  let suma = 0;
  let multiplo = 2;

  for (let i = cuerpo.length - 1; i >= 0; i--) {
    suma += parseInt(cuerpo[i], 10) * multiplo;
    multiplo = multiplo === 7 ? 2 : multiplo + 1;
  }

  const dvEsperado = 11 - (suma % 11);
  const dvCalculado =
    dvEsperado === 11 ? '0' :
    dvEsperado === 10 ? 'K' :
    String(dvEsperado);

  return dvIngresado === dvCalculado;
}

/**
 * Angular ValidatorFn para usar en Reactive Forms.
 * Marca error { rutInvalido: true } si el DV no corresponde.
 */
export const rutValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const valor = control.value;
  if (!valor || valor.trim() === '') return null; // dejar required hacerlo
  return validarRut(valor) ? null : { rutInvalido: true };
};

/**
 * Handler para usar en (input) de un campo de texto.
 * Formatea mientras escribe: agrega puntos y guión automáticamente.
 * Uso: (input)="onRutInput($event, 'rut')" en el template
 *      onRutInput(e: Event, campo: string) { ... en el componente }
 */
export function procesarInputRut(event: Event): string {
  const input = event.target as HTMLInputElement;
  // Solo dígitos y K
  let raw = input.value.replace(/[^0-9kK]/g, '').toUpperCase();
  // Limitar a 9 caracteres (8 dígitos + DV)
  if (raw.length > 9) raw = raw.slice(0, 9);

  if (raw.length === 0) {
    input.value = '';
    return '';
  }

  let formatted: string;
  if (raw.length <= 1) {
    formatted = raw;
  } else {
    const cuerpo = raw.slice(0, -1);
    const dv = raw.slice(-1);
    const cuerpoFmt = cuerpo
      .split('')
      .reverse()
      .reduce((acc, d, i) => (i > 0 && i % 3 === 0 ? d + '.' + acc : d + acc), '');
    formatted = `${cuerpoFmt}-${dv}`;
  }

  input.value = formatted;
  return formatted;
}
