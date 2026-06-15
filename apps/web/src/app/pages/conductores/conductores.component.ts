import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ConductoresService } from '@core/services/conductores.service';
import { DialogoService } from '@core/services/dialogo.service';
import { Conductor } from '@core/models';
import { rutValidator, procesarInputRut } from '@core/utils/rut.utils';

const LS_DIAS = 'param_dias_vencimiento';

@Component({
  selector: 'app-conductores',
  standalone: true,
  imports: [
    CommonModule, FormsModule, ReactiveFormsModule,
    MatButtonModule, MatIconModule, MatInputModule,
    MatSelectModule, MatProgressSpinnerModule, MatSnackBarModule, MatTooltipModule,
  ],
  template: `
    <div class="encabezado-pagina">
      <h1>Conductores</h1>
      <button mat-flat-button class="btn-principal" (click)="abrirFormulario()">
        <mat-icon>add</mat-icon> Nuevo conductor
      </button>
    </div>

    <div class="barra-busqueda">
      <mat-form-field appearance="fill">
        <mat-label>Buscar nombre o RUT</mat-label>
        <input matInput [(ngModel)]="busqueda" (ngModelChange)="cargar()" />
        <mat-icon matSuffix>search</mat-icon>
      </mat-form-field>
      <mat-form-field appearance="fill">
        <mat-label>Estado</mat-label>
        <mat-select [(ngModel)]="filtroEstado" (ngModelChange)="cargar()">
          <mat-option value="">Todos</mat-option>
          <mat-option value="ACTIVO">Activo</mat-option>
          <mat-option value="INACTIVO">Inactivo</mat-option>
          <mat-option value="VACACIONES">Vacaciones</mat-option>
        </mat-select>
      </mat-form-field>
    </div>

    @if (cargando()) {
      <div class="spinner-centrado"><mat-spinner diameter="40" /></div>
    } @else {
      <div class="cuadricula-conductores">
        @for (conductor of conductores(); track conductor.id) {
          <div class="tarjeta-conductor superficie">
            <div class="cabecera-tarjeta">
              <!-- Avatar / Foto -->
              @if (conductor.fotoBase64) {
                <img class="avatar avatar-foto" [src]="conductor.fotoBase64" [alt]="conductor.nombre" />
              } @else {
                <div class="avatar">{{ iniciales(conductor) }}</div>
              }
              <div class="info-tarjeta">
                <div class="nombre-conductor">{{ conductor.nombre }}</div>
                <div class="rut-conductor texto-atenuado">{{ conductor.rut }}</div>
              </div>
              <span [class]="insigniaEstado(conductor.estado)">{{ conductor.estado }}</span>
            </div>

            <div class="detalles-tarjeta">
              <div class="detalle-item">
                <mat-icon>credit_card</mat-icon>
                <span>Lic. {{ conductor.categoriaLicencia }}</span>
              </div>
              <div class="detalle-item">
                <mat-icon>event</mat-icon>
                <span>Lic: {{ conductor.vencimientoLicencia | date:'dd/MM/yyyy' }}</span>
              </div>
              <span [class]="insigniaVencimiento(conductor.vencimientoLicencia)"
                    [matTooltip]="'Licencia vence en ' + diasParaVencer(conductor.vencimientoLicencia) + ' días'">
                {{ diasParaVencer(conductor.vencimientoLicencia) }}d
              </span>
            </div>

            @if (conductor.vencimientoCedula) {
              <div class="detalles-tarjeta">
                <div class="detalle-item">
                  <mat-icon>badge</mat-icon>
                  <span>Cédula: {{ conductor.vencimientoCedula | date:'dd/MM/yyyy' }}</span>
                </div>
                <span [class]="insigniaVencimiento(conductor.vencimientoCedula)"
                      [matTooltip]="'Cédula vence en ' + diasParaVencer(conductor.vencimientoCedula) + ' días'">
                  {{ diasParaVencer(conductor.vencimientoCedula) }}d
                </span>
              </div>
            }

            <!-- Indicador licencia adjunta -->
            @if (conductor.licenciaFrente || conductor.licenciaReverso) {
              <div class="indicador-docs">
                <mat-icon>description</mat-icon>
                <span>Licencia adjunta
                  {{ conductor.licenciaFrente && conductor.licenciaReverso ? '(frente y reverso)' : conductor.licenciaFrente ? '(frente)' : '(reverso)' }}
                </span>
              </div>
            }

            <div class="acciones-tarjeta">
              <button mat-button (click)="abrirFormulario(conductor)" style="color:var(--azul-600)">
                <mat-icon>edit</mat-icon> Editar
              </button>
              <button mat-button color="warn" (click)="eliminar(conductor)">
                <mat-icon>delete_outline</mat-icon> Eliminar
              </button>
            </div>
          </div>
        }
        @if (conductores().length === 0) {
          <div class="estado-vacio-cuadricula">
            <mat-icon>badge</mat-icon>
            <p>No hay conductores registrados.</p>
          </div>
        }
      </div>
    }

    <!-- ══════════ MODAL: Formulario conductor ══════════ -->
    @if (mostrarFormulario()) {
      <div class="capa-modal" (click)="cerrarFormulario()">
        <div class="panel-modal panel-conductor" (click)="$event.stopPropagation()">
          <h2>{{ idEdicion() ? 'Editar' : 'Nuevo' }} conductor</h2>

          <!-- ── Foto del conductor ── -->
          <div class="seccion-foto">
            <div class="zona-foto" (click)="triggerFoto()">
              @if (fotoPreview()) {
                <img [src]="fotoPreview()!" class="foto-preview" alt="Foto conductor" />
                <div class="overlay-foto"><mat-icon>photo_camera</mat-icon></div>
              } @else {
                <mat-icon class="icono-foto-vacio">add_a_photo</mat-icon>
                <span>Agregar foto</span>
              }
            </div>
            @if (fotoPreview()) {
              <button mat-button color="warn" class="btn-quitar-foto" (click)="quitarFoto()">
                <mat-icon>close</mat-icon> Quitar foto
              </button>
            }
            <input #inputFoto type="file" accept="image/*" style="display:none"
                   (change)="onFotoSeleccionada($event)" />
          </div>

          <form [formGroup]="formulario" (ngSubmit)="guardar()">

            <!-- Datos personales -->
            <div class="subtitulo-seccion">Datos personales</div>
            <mat-form-field appearance="fill" class="ancho-completo">
              <mat-label>Nombre completo</mat-label>
              <input matInput formControlName="nombre" />
            </mat-form-field>
            <div class="dos-columnas">
              <mat-form-field appearance="fill">
                <mat-label>RUT</mat-label>
                <input matInput formControlName="rut" placeholder="12.345.678-9"
                       (input)="onRutInput($event)" />
                @if (formulario.get('rut')?.hasError('rutInvalido') && formulario.get('rut')?.touched) {
                  <mat-error>RUT inválido — revise el dígito verificador</mat-error>
                }
              </mat-form-field>
              <mat-form-field appearance="fill">
                <mat-label>Venc. Cédula de Identidad</mat-label>
                <input matInput type="date" formControlName="vencimientoCedula" />
                <mat-icon matSuffix style="font-size:16px;color:var(--ink-soft)">badge</mat-icon>
              </mat-form-field>
            </div>
            <div class="dos-columnas">
              <mat-form-field appearance="fill">
                <mat-label>Teléfono</mat-label>
                <input matInput formControlName="telefono" />
              </mat-form-field>
              <mat-form-field appearance="fill">
                <mat-label>Email</mat-label>
                <input matInput formControlName="email" />
              </mat-form-field>
            </div>

            <!-- Licencia de conducir -->
            <div class="subtitulo-seccion">Licencia de conducir</div>
            <div class="dos-columnas">
              <mat-form-field appearance="fill">
                <mat-label>Categoría licencia</mat-label>
                <mat-select formControlName="categoriaLicencia">
                  <mat-option value="A1">A1</mat-option>
                  <mat-option value="A2">A2</mat-option>
                  <mat-option value="A3">A3</mat-option>
                  <mat-option value="B">B</mat-option>
                  <mat-option value="D">D</mat-option>
                </mat-select>
              </mat-form-field>
              <mat-form-field appearance="fill">
                <mat-label>Vencimiento licencia *</mat-label>
                <input matInput type="date" formControlName="vencimientoLicencia" />
              </mat-form-field>
            </div>

            <!-- Imágenes de licencia -->
            <div class="bloque-licencias">
              <!-- Frente -->
              <div class="zona-licencia" (click)="triggerLicencia('frente')">
                @if (licenciaFrentePreview()) {
                  <img [src]="licenciaFrentePreview()!" class="img-licencia" alt="Frente licencia" />
                  <div class="overlay-foto"><mat-icon>photo_camera</mat-icon></div>
                } @else {
                  <mat-icon>upload_file</mat-icon>
                  <span>Frente</span>
                }
              </div>
              <!-- Reverso -->
              <div class="zona-licencia" (click)="triggerLicencia('reverso')">
                @if (licenciaReversoPreview()) {
                  <img [src]="licenciaReversoPreview()!" class="img-licencia" alt="Reverso licencia" />
                  <div class="overlay-foto"><mat-icon>photo_camera</mat-icon></div>
                } @else {
                  <mat-icon>upload_file</mat-icon>
                  <span>Reverso</span>
                }
              </div>
            </div>
            <div class="acciones-licencias">
              @if (licenciaFrentePreview()) {
                <button mat-button color="warn" type="button" (click)="quitarLicencia('frente')">
                  <mat-icon>close</mat-icon> Quitar frente
                </button>
              }
              @if (licenciaReversoPreview()) {
                <button mat-button color="warn" type="button" (click)="quitarLicencia('reverso')">
                  <mat-icon>close</mat-icon> Quitar reverso
                </button>
              }
            </div>
            <input #inputLicenciaFrente type="file" accept="image/*" style="display:none"
                   (change)="onLicenciaSeleccionada($event, 'frente')" />
            <input #inputLicenciaReverso type="file" accept="image/*" style="display:none"
                   (change)="onLicenciaSeleccionada($event, 'reverso')" />

            <!-- Estado -->
            <div class="subtitulo-seccion">Estado</div>
            <mat-form-field appearance="fill" class="ancho-completo">
              <mat-label>Estado</mat-label>
              <mat-select formControlName="estado">
                <mat-option value="ACTIVO">Activo</mat-option>
                <mat-option value="INACTIVO">Inactivo</mat-option>
                <mat-option value="VACACIONES">Vacaciones</mat-option>
              </mat-select>
            </mat-form-field>

            <div class="acciones-formulario">
              <button mat-button type="button" (click)="cerrarFormulario()">Cancelar</button>
              <button mat-flat-button class="btn-principal" type="submit" [disabled]="formulario.invalid || guardando()">
                {{ guardando() ? 'Guardando…' : 'Guardar' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    }
  `,
  styles: [`
    .cuadricula-conductores {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(290px,1fr));
      gap: 16px;
    }
    .tarjeta-conductor { display: flex; flex-direction: column; gap: 12px; }
    .cabecera-tarjeta { display: flex; align-items: center; gap: 12px; }
    .avatar {
      width: 48px; height: 48px; border-radius: 50%;
      background: linear-gradient(135deg, var(--azul-600), var(--azul-400));
      color: #fff; font-weight: 700; font-size: 17px;
      display: flex; align-items: center; justify-content: center; flex-shrink: 0;
    }
    .avatar-foto {
      width: 48px; height: 48px; border-radius: 50%;
      object-fit: cover; border: 2px solid var(--azul-200);
    }
    .info-tarjeta { flex: 1; min-width: 0; }
    .nombre-conductor { font-weight: 600; font-size: 15px; color: var(--azul-900); }
    .rut-conductor { font-size: 12px; }
    .detalles-tarjeta {
      display: flex; align-items: center; gap: 10px; flex-wrap: wrap;
      background: var(--azul-50); border-radius: var(--radio-sm); padding: 8px 12px;
    }
    .detalle-item {
      display: flex; align-items: center; gap: 4px; font-size: 12px; color: var(--color-texto-2);
      mat-icon { font-size: 14px; width: 14px; height: 14px; color: var(--azul-400); }
    }
    .indicador-docs {
      display: flex; align-items: center; gap: 6px;
      font-size: 11px; color: #166534;
      background: #F0FDF4; border-radius: 4px; padding: 4px 10px;
      mat-icon { font-size: 14px; width: 14px; height: 14px; }
    }
    .acciones-tarjeta { display: flex; justify-content: flex-end; gap: 4px; }
    .estado-vacio-cuadricula {
      grid-column: 1/-1; display: flex; flex-direction: column; align-items: center;
      padding: 60px; gap: 8px; color: var(--color-texto-3);
      mat-icon { font-size: 48px; width: 48px; height: 48px; opacity: .3; }
    }

    /* ── Modal conductor (más ancho) ── */
    .panel-conductor { width: min(640px, 96vw) !important; max-height: 90vh; overflow-y: auto; }

    /* ── Subtítulos de sección ── */
    .subtitulo-seccion {
      font-size: 10px; font-weight: 700; color: var(--ink-soft);
      text-transform: uppercase; letter-spacing: .08em;
      padding: 8px 0 4px; border-bottom: 1px solid var(--slate-dark);
      margin-bottom: 10px;
    }

    /* ── Zona foto conductor ── */
    .seccion-foto {
      display: flex; align-items: center; gap: 16px;
      margin-bottom: 20px; padding-bottom: 16px;
      border-bottom: 1px solid var(--slate-dark);
    }
    .zona-foto {
      width: 90px; height: 90px; border-radius: 50%;
      border: 2px dashed var(--azul-300);
      display: flex; flex-direction: column; align-items: center; justify-content: center;
      cursor: pointer; position: relative; overflow: hidden;
      background: var(--azul-50); flex-shrink: 0;
      transition: border-color .15s;
      font-size: 11px; color: var(--azul-500); gap: 4px;
      &:hover { border-color: var(--azul-500); }
      .icono-foto-vacio { font-size: 28px; width: 28px; height: 28px; color: var(--azul-400); }
    }
    .foto-preview {
      width: 100%; height: 100%; object-fit: cover; border-radius: 50%;
    }
    .overlay-foto {
      position: absolute; inset: 0; background: rgba(0,0,0,.4);
      display: flex; align-items: center; justify-content: center;
      opacity: 0; transition: opacity .15s; border-radius: 50%;
      mat-icon { color: #fff; font-size: 22px; }
    }
    .zona-foto:hover .overlay-foto { opacity: 1; }
    .btn-quitar-foto { font-size: 12px !important; }

    /* ── Imágenes licencia ── */
    .bloque-licencias {
      display: grid; grid-template-columns: 1fr 1fr; gap: 10px; margin-bottom: 8px;
    }
    .zona-licencia {
      border: 2px dashed var(--azul-300); border-radius: 8px;
      min-height: 100px; display: flex; flex-direction: column;
      align-items: center; justify-content: center;
      cursor: pointer; position: relative; overflow: hidden;
      background: var(--azul-50); gap: 6px;
      font-size: 12px; color: var(--azul-500);
      transition: border-color .15s;
      mat-icon { font-size: 28px; width: 28px; height: 28px; color: var(--azul-400); }
      &:hover { border-color: var(--azul-500); }
    }
    .img-licencia { width: 100%; height: 100%; object-fit: cover; position: absolute; inset: 0; }
    .zona-licencia .overlay-foto { border-radius: 6px; }
    .zona-licencia:hover .overlay-foto { opacity: 1; }
    .acciones-licencias {
      display: flex; gap: 8px; margin-bottom: 12px;
      button { font-size: 11px !important; }
    }
  `],
})
export class ConductoresComponent implements OnInit {
  private readonly servicio     = inject(ConductoresService);
  private readonly fb           = inject(FormBuilder);
  private readonly notificacion = inject(MatSnackBar);
  private readonly dialogo      = inject(DialogoService);

  cargando          = signal(true);
  guardando         = signal(false);
  mostrarFormulario = signal(false);
  idEdicion         = signal<string | null>(null);
  conductores       = signal<Conductor[]>([]);
  busqueda          = '';
  filtroEstado      = '';

  // Previews de imágenes
  fotoPreview            = signal<string | null>(null);
  licenciaFrentePreview  = signal<string | null>(null);
  licenciaReversoPreview = signal<string | null>(null);

  // Días de anticipación leídos desde parámetros del sistema
  diasAlerta = computed(() =>
    parseInt(localStorage.getItem(LS_DIAS) ?? '30', 10)
  );

  formulario = this.fb.group({
    nombre:              ['', Validators.required],
    rut:                 ['', [Validators.required, rutValidator]],
    telefono:            [''],
    email:               [''],
    categoriaLicencia:   ['A3', Validators.required],
    vencimientoLicencia: ['', Validators.required],
    vencimientoCedula:   [''],
    estado:              ['ACTIVO'],
  });

  // ── Referencias a inputs de archivo (ViewChild manual) ──────
  private inputFotoEl: HTMLInputElement | null = null;
  private inputFrenteEl: HTMLInputElement | null = null;
  private inputReversoEl: HTMLInputElement | null = null;

  ngOnInit() {
    this.cargar();
    // Auto-uppercase para nombre
    this.formulario.get('nombre')?.valueChanges.subscribe(v => {
      if (v && v !== v.toUpperCase())
        this.formulario.get('nombre')?.setValue(v.toUpperCase(), { emitEvent: false });
    });
  }

  /** Formatea el RUT mientras se escribe: 12345678-9 → 12.345.678-9 */
  onRutInput(event: Event) {
    const formatted = procesarInputRut(event);
    this.formulario.get('rut')?.setValue(formatted, { emitEvent: true });
  }

  cargar() {
    this.cargando.set(true);
    this.servicio.getAll({ search: this.busqueda || undefined, estado: this.filtroEstado || undefined }).subscribe({
      next: r => { this.conductores.set(r.content); this.cargando.set(false); },
      error: () => this.cargando.set(false),
    });
  }

  abrirFormulario(c?: Conductor) {
    this.idEdicion.set(c?.id ?? null);
    this.fotoPreview.set(c?.fotoBase64 ?? null);
    this.licenciaFrentePreview.set(c?.licenciaFrente ?? null);
    this.licenciaReversoPreview.set(c?.licenciaReverso ?? null);
    if (c) {
      this.formulario.patchValue({
        ...c,
        vencimientoLicencia: c.vencimientoLicencia?.substring(0, 10),
        vencimientoCedula:   c.vencimientoCedula?.substring(0, 10) ?? '',
      });
    } else {
      this.formulario.reset({ categoriaLicencia: 'A3', estado: 'ACTIVO' });
    }
    this.mostrarFormulario.set(true);
  }

  cerrarFormulario() { this.mostrarFormulario.set(false); }

  guardar() {
    if (this.formulario.invalid) return;
    this.guardando.set(true);
    const v = this.formulario.value as any;
    const solicitud = {
      ...v,
      fotoBase64:     this.fotoPreview()            ?? undefined,
      licenciaFrente: this.licenciaFrentePreview()  ?? undefined,
      licenciaReverso: this.licenciaReversoPreview() ?? undefined,
    };
    const op = this.idEdicion()
      ? this.servicio.update(this.idEdicion()!, solicitud)
      : this.servicio.create(solicitud);
    op.subscribe({
      next: () => {
        this.guardando.set(false);
        this.cerrarFormulario();
        this.cargar();
        this.notificacion.open('Guardado', '', { duration: 3000 });
      },
      error: () => {
        this.guardando.set(false);
        this.notificacion.open('Error al guardar', '', { duration: 3000 });
      },
    });
  }

  async eliminar(c: Conductor) {
    const ok = await this.dialogo.confirmarEliminar(
      `¿Eliminar conductor ${c.nombre}?`,
      'Esta acción no se puede deshacer.'
    );
    if (!ok) return;
    this.servicio.delete(c.id).subscribe({ next: () => this.cargar() });
  }

  // ── Manejo de imágenes ──────────────────────────────────────

  triggerFoto() {
    if (!this.inputFotoEl) {
      this.inputFotoEl = document.querySelector('input[type=file]:first-of-type') as HTMLInputElement;
    }
    document.querySelectorAll('input[type=file]')[0]?.dispatchEvent(new MouseEvent('click'));
  }

  triggerLicencia(lado: 'frente' | 'reverso') {
    const inputs = document.querySelectorAll('input[type=file]');
    const idx = lado === 'frente' ? 1 : 2;
    inputs[idx]?.dispatchEvent(new MouseEvent('click'));
  }

  onFotoSeleccionada(event: Event) {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;
    this.leerImagenBase64(file).then(b64 => this.fotoPreview.set(b64));
    (event.target as HTMLInputElement).value = '';
  }

  onLicenciaSeleccionada(event: Event, lado: 'frente' | 'reverso') {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;
    this.leerImagenBase64(file).then(b64 => {
      if (lado === 'frente') this.licenciaFrentePreview.set(b64);
      else                   this.licenciaReversoPreview.set(b64);
    });
    (event.target as HTMLInputElement).value = '';
  }

  quitarFoto()                     { this.fotoPreview.set(null); }
  quitarLicencia(l: 'frente' | 'reverso') {
    if (l === 'frente') this.licenciaFrentePreview.set(null);
    else                this.licenciaReversoPreview.set(null);
  }

  private leerImagenBase64(file: File): Promise<string> {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => resolve(reader.result as string);
      reader.onerror = reject;
      reader.readAsDataURL(file);
    });
  }

  // ── Helpers ─────────────────────────────────────────────────

  iniciales(c: Conductor): string {
    const partes = c.nombre.trim().split(' ');
    return partes.length >= 2
      ? (partes[0][0] + partes[1][0]).toUpperCase()
      : c.nombre.substring(0, 2).toUpperCase();
  }

  diasParaVencer(fecha?: string): number {
    if (!fecha) return 0;
    return Math.ceil((new Date(fecha).getTime() - Date.now()) / 86_400_000);
  }

  insigniaEstado(estado: string): string {
    const mapa: Record<string, string> = {
      ACTIVO:     'insignia insignia-exito',
      INACTIVO:   'insignia insignia-info',
      VACACIONES: 'insignia insignia-advertencia',
    };
    return mapa[estado] ?? 'insignia insignia-info';
  }

  insigniaVencimiento(fecha?: string): string {
    const dias = this.diasParaVencer(fecha);
    const umbral = this.diasAlerta();
    if (dias < 0)       return 'insignia insignia-peligro';
    if (dias < umbral)  return 'insignia insignia-advertencia';
    return 'insignia insignia-exito';
  }
}
