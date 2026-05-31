import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { ConductoresService } from '@core/services/conductores.service';
import { Conductor } from '@core/models';

@Component({
  selector: 'app-conductores',
  standalone: true,
  imports: [
    CommonModule, FormsModule, ReactiveFormsModule,
    MatButtonModule, MatIconModule, MatInputModule,
    MatSelectModule, MatProgressSpinnerModule, MatSnackBarModule,
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
              <div class="avatar">{{ iniciales(conductor) }}</div>
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
                <span>Vence: {{ conductor.vencimientoLicencia | date:'dd/MM/yyyy' }}</span>
              </div>
              <span [class]="insigniaVencimiento(conductor.vencimientoLicencia)">
                {{ diasParaVencer(conductor.vencimientoLicencia) }}d
              </span>
            </div>
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

    @if (mostrarFormulario()) {
      <div class="capa-modal" (click)="cerrarFormulario()">
        <div class="panel-modal" (click)="$event.stopPropagation()">
          <h2>{{ idEdicion() ? 'Editar' : 'Nuevo' }} conductor</h2>
          <form [formGroup]="formulario" (ngSubmit)="guardar()">
            <mat-form-field appearance="fill" class="ancho-completo">
              <mat-label>Nombre completo</mat-label>
              <input matInput formControlName="nombre" />
            </mat-form-field>
            <mat-form-field appearance="fill" class="ancho-completo">
              <mat-label>RUT</mat-label>
              <input matInput formControlName="rut" placeholder="12.345.678-9" />
            </mat-form-field>
            <mat-form-field appearance="fill" class="ancho-completo">
              <mat-label>Teléfono</mat-label>
              <input matInput formControlName="telefono" />
            </mat-form-field>
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
                <mat-label>Vencimiento licencia</mat-label>
                <input matInput type="date" formControlName="vencimientoLicencia" />
              </mat-form-field>
            </div>
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
    .tarjeta-conductor { display: flex; flex-direction: column; gap: 14px; }
    .cabecera-tarjeta { display: flex; align-items: center; gap: 12px; }
    .avatar {
      width: 46px; height: 46px; border-radius: 50%;
      background: linear-gradient(135deg, var(--azul-600), var(--azul-400));
      color: #fff; font-weight: 700; font-size: 17px;
      display: flex; align-items: center; justify-content: center; flex-shrink: 0;
    }
    .info-tarjeta { flex: 1; min-width: 0; }
    .nombre-conductor { font-weight: 600; font-size: 15px; color: var(--azul-900); }
    .rut-conductor { font-size: 12px; }
    .detalles-tarjeta {
      display: flex; align-items: center; gap: 12px; flex-wrap: wrap;
      background: var(--azul-50); border-radius: var(--radio-sm); padding: 10px 12px;
    }
    .detalle-item {
      display: flex; align-items: center; gap: 4px; font-size: 12px; color: var(--color-texto-2);
      mat-icon { font-size: 14px; width: 14px; height: 14px; color: var(--azul-400); }
    }
    .acciones-tarjeta { display: flex; justify-content: flex-end; gap: 4px; }
    .estado-vacio-cuadricula {
      grid-column: 1/-1; display: flex; flex-direction: column; align-items: center;
      padding: 60px; gap: 8px; color: var(--color-texto-3);
      mat-icon { font-size: 48px; width: 48px; height: 48px; opacity: .3; }
    }
  `],
})
export class ConductoresComponent implements OnInit {
  private readonly servicio     = inject(ConductoresService);
  private readonly constructor_ = inject(FormBuilder);
  private readonly notificacion = inject(MatSnackBar);

  cargando          = signal(true);
  guardando         = signal(false);
  mostrarFormulario = signal(false);
  idEdicion         = signal<string | null>(null);
  conductores       = signal<Conductor[]>([]);
  busqueda          = '';
  filtroEstado      = '';

  formulario = this.constructor_.group({
    nombre:              ['', Validators.required],
    rut:                 ['', Validators.required],
    telefono:            [''],
    categoriaLicencia:   ['A3', Validators.required],
    vencimientoLicencia: ['', Validators.required],
  });

  ngOnInit() {
    this.cargar();
    // RUT siempre mayúscula (sufijo K), nombre en mayúsculas para consistencia
    (['nombre', 'rut'] as const).forEach(campo => {
      this.formulario.get(campo)?.valueChanges.subscribe(v => {
        if (v && v !== v.toUpperCase()) {
          this.formulario.get(campo)?.setValue(v.toUpperCase(), { emitEvent: false });
        }
      });
    });
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
    if (c) this.formulario.patchValue({ ...c, vencimientoLicencia: c.vencimientoLicencia?.substring(0, 10) });
    else this.formulario.reset({ categoriaLicencia: 'A3' });
    this.mostrarFormulario.set(true);
  }

  cerrarFormulario() { this.mostrarFormulario.set(false); }

  guardar() {
    if (this.formulario.invalid) return;
    this.guardando.set(true);
    const solicitud = this.formulario.value as any;
    const operacion = this.idEdicion() ? this.servicio.update(this.idEdicion()!, solicitud) : this.servicio.create(solicitud);
    operacion.subscribe({
      next: () => { this.guardando.set(false); this.cerrarFormulario(); this.cargar(); this.notificacion.open('Guardado', '', { duration: 3000 }); },
      error: () => { this.guardando.set(false); this.notificacion.open('Error al guardar', '', { duration: 3000 }); },
    });
  }

  eliminar(c: Conductor) {
    if (!confirm(`¿Eliminar conductor ${c.nombre}?`)) return;
    this.servicio.delete(c.id).subscribe({ next: () => this.cargar() });
  }

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
    if (dias < 0)  return 'insignia insignia-peligro';
    if (dias < 30) return 'insignia insignia-advertencia';
    return 'insignia insignia-exito';
  }
}
