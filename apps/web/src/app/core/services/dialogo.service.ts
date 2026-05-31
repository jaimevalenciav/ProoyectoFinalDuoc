import { Injectable } from '@angular/core';
import Swal from 'sweetalert2';

/** Wrapper centralizado de SweetAlert2 con la paleta del sistema */
@Injectable({ providedIn: 'root' })
export class DialogoService {

  /** Diálogo de confirmación de eliminación (botón rojo) */
  async confirmarEliminar(mensaje: string, detalle?: string): Promise<boolean> {
    const result = await Swal.fire({
      title: mensaje,
      text: detalle,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Sí, eliminar',
      cancelButtonText: 'Cancelar',
      confirmButtonColor: '#C10A5A',
      cancelButtonColor: '#1B2C40',
      reverseButtons: true,
      focusCancel: true,
      customClass: { popup: 'swal-flota' },
    });
    return result.isConfirmed;
  }

  /** Diálogo de confirmación genérica (botón naranja) */
  async confirmar(titulo: string, texto?: string, botonOk = 'Confirmar'): Promise<boolean> {
    const result = await Swal.fire({
      title: titulo,
      text: texto,
      icon: 'question',
      showCancelButton: true,
      confirmButtonText: botonOk,
      cancelButtonText: 'Cancelar',
      confirmButtonColor: '#C25E01',
      cancelButtonColor: '#1B2C40',
      reverseButtons: true,
      focusCancel: true,
      customClass: { popup: 'swal-flota' },
    });
    return result.isConfirmed;
  }

  /** Diálogo de acción irreversible (texto de confirmación requerido) — para anulaciones */
  async confirmarCritico(titulo: string, texto: string, botonOk = 'Confirmar'): Promise<boolean> {
    const result = await Swal.fire({
      title: titulo,
      html: texto,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: botonOk,
      cancelButtonText: 'Cancelar',
      confirmButtonColor: '#C10A5A',
      cancelButtonColor: '#1B2C40',
      reverseButtons: true,
      focusCancel: true,
      customClass: { popup: 'swal-flota' },
    });
    return result.isConfirmed;
  }
}
