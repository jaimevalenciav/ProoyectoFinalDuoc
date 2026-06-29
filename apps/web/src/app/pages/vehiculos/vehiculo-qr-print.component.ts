import {
  Component, Input, OnInit, AfterViewInit,
  ElementRef, ViewChild, inject,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { Vehiculo } from '@core/models';
import QRCode from 'qrcode';

export interface VehiculoQrData { vehiculo: Vehiculo; }

@Component({
  selector: 'app-vehiculo-qr-print',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule, MatDialogModule],
  template: `
    <!-- ── Barra de acción (no se imprime) ───────────────────────────────────── -->
    <div class="barra-accion no-print">
      <span class="barra-titulo">QR — {{ v.patente }}</span>
      <div class="barra-btns">
        <button mat-flat-button class="btn-principal" (click)="imprimir()">
          <mat-icon>print</mat-icon> Imprimir
        </button>
        <button mat-icon-button (click)="cerrar()"><mat-icon>close</mat-icon></button>
      </div>
    </div>

    <!-- ── Hoja carta (se imprime) ───────────────────────────────────────────── -->
    <div class="hoja-carta" id="zona-impresion">

      <!-- Logo / encabezado empresa -->
      <div class="hoja-encabezado">
        <div class="empresa-nombre">TruckManager Pro</div>
        <div class="empresa-sub">Identificación de vehículo</div>
      </div>

      <!-- QR principal -->
      <div class="qr-wrapper">
        <canvas #qrCanvas class="qr-canvas"></canvas>
      </div>

      <!-- Datos del vehículo -->
      <div class="vehiculo-placa">{{ v.patente }}</div>
      <div class="vehiculo-info">
        <span class="info-chip">{{ v.marca }} {{ v.modelo }}</span>
        @if (v.anio) { <span class="info-chip">{{ v.anio }}</span> }
        @if (v.tipo) { <span class="info-chip">{{ labelTipo(v.tipo) }}</span> }
      </div>

      <!-- Instrucción -->
      <div class="instruccion">
        <mat-icon class="ico-instruccion no-print">smartphone</mat-icon>
        <p>Escanea este código con la app móvil TruckManager<br>para registrar el inicio de tu jornada en este vehículo.</p>
      </div>

      <!-- ID técnico (pequeño, para soporte) -->
      <div class="id-tecnico">ID: {{ v.id }}</div>

    </div>
  `,
  styles: [`
    /* ── Contenedor del dialog ────────────────────────────────────────────────── */
    :host {
      display: flex;
      flex-direction: column;
      height: 100%;
      background: #f0f4f9;
    }

    /* ── Barra superior ──────────────────────────────────────────────────────── */
    .barra-accion {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 12px 20px;
      background: #fff;
      border-bottom: 1.5px solid #e4eaf3;
      flex-shrink: 0;
    }
    .barra-titulo { font-size: 15px; font-weight: 700; color: #1a2540; font-family: monospace; }
    .barra-btns  { display: flex; align-items: center; gap: 8px; }

    /* ── Hoja carta ──────────────────────────────────────────────────────────── */
    .hoja-carta {
      width: 210mm;          /* A4 ≈ carta, usamos carta: 216mm×279mm */
      min-height: 270mm;
      background: #fff;
      margin: 24px auto;
      border-radius: 8px;
      box-shadow: 0 4px 24px rgba(26,37,64,.14);
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 0;
      padding: 20mm 16mm;
      box-sizing: border-box;
    }

    /* ── Encabezado ─────────────────────────────────────────────────────────── */
    .hoja-encabezado {
      text-align: center;
      margin-bottom: 8mm;
    }
    .empresa-nombre {
      font-size: 22pt;
      font-weight: 700;
      color: #1B2C40;
      letter-spacing: -.5px;
    }
    .empresa-sub {
      font-size: 11pt;
      color: #4a5878;
      margin-top: 2px;
      text-transform: uppercase;
      letter-spacing: .12em;
    }

    /* ── QR ─────────────────────────────────────────────────────────────────── */
    .qr-wrapper {
      display: flex;
      justify-content: center;
      align-items: center;
      margin: 6mm 0;
    }
    .qr-canvas {
      width: 140mm !important;
      height: 140mm !important;
      display: block;
    }

    /* ── Placa / datos ──────────────────────────────────────────────────────── */
    .vehiculo-placa {
      font-size: 44pt;
      font-weight: 900;
      font-family: 'JetBrains Mono', 'Fira Code', monospace;
      color: #1B2C40;
      letter-spacing: .12em;
      margin-top: 4mm;
    }
    .vehiculo-info {
      display: flex;
      gap: 8px;
      flex-wrap: wrap;
      justify-content: center;
      margin-top: 3mm;
    }
    .info-chip {
      background: #f0f4f9;
      border: 1.5px solid #ccd5e6;
      border-radius: 4px;
      padding: 4px 12px;
      font-size: 12pt;
      color: #1a2540;
      font-weight: 600;
    }

    /* ── Instrucción ────────────────────────────────────────────────────────── */
    .instruccion {
      display: flex;
      flex-direction: column;
      align-items: center;
      margin-top: 8mm;
      text-align: center;
    }
    .instruccion p {
      font-size: 11pt;
      color: #4a5878;
      line-height: 1.6;
      margin: 4px 0 0;
    }
    .ico-instruccion {
      font-size: 28px;
      color: #007AF5;
      margin-bottom: 4px;
    }

    /* ── ID técnico ─────────────────────────────────────────────────────────── */
    .id-tecnico {
      margin-top: 10mm;
      font-size: 7pt;
      color: #ccd5e6;
      font-family: monospace;
    }

    /* ── Reglas de impresión ────────────────────────────────────────────────── */
    @media print {
      :host { background: #fff !important; }

      .no-print { display: none !important; }

      .hoja-carta {
        width: 100%;
        min-height: 100vh;
        margin: 0;
        border-radius: 0;
        box-shadow: none;
        padding: 15mm 20mm;
        page-break-after: avoid;
      }

      .qr-canvas {
        width: 160mm !important;
        height: 160mm !important;
      }
    }

    @page {
      size: letter portrait;
      margin: 0;
    }
  `],
})
export class VehiculoQrPrintComponent implements AfterViewInit {
  @ViewChild('qrCanvas') canvasRef!: ElementRef<HTMLCanvasElement>;

  private readonly ref  = inject(MatDialogRef<VehiculoQrPrintComponent>);
  readonly data         = inject<VehiculoQrData>(MAT_DIALOG_DATA);
  readonly v            = this.data.vehiculo;

  /** Contenido codificado en el QR: JSON con id y patente para que la app lo parsee */
  private get qrContent(): string {
    return JSON.stringify({
      tipo:    'vehiculo',
      id:      this.v.id,
      patente: this.v.patente,
    });
  }

  ngAfterViewInit() {
    const canvas = this.canvasRef.nativeElement;
    // Tamaño en píxeles: 140mm a ~300dpi → ~1654px; usamos 1200 (equilibrio calidad/memoria)
    const size = 1200;
    canvas.width  = size;
    canvas.height = size;

    QRCode.toCanvas(canvas, this.qrContent, {
      width:         size,
      margin:        2,
      color: {
        dark:  '#1B2C40',   // navy
        light: '#FFFFFF',
      },
      errorCorrectionLevel: 'H',  // Alta redundancia para impresión
    });
  }

  imprimir() { window.print(); }
  cerrar()   { this.ref.close(); }

  labelTipo(tipo: string): string {
    const m: Record<string, string> = {
      CAMION: 'Camión', BUS: 'Bus', VAN: 'Van',
      FURGON: 'Furgón', PICKUP: 'Pick-up', OTRO: 'Otro',
    };
    return m[tipo] ?? tipo;
  }
}
