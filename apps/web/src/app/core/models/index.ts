// ─── Empresa ────────────────────────────────────────────────
export interface Empresa {
  id: string;
  nombre: string;
  rut: string;
  giro?: string;
  direccion?: string;
  ciudad?: string;
  plan: 'STARTER' | 'STANDARD' | 'PREMIUM';
  activa: boolean;
}

// ─── Usuario ─────────────────────────────────────────────────
export interface UsuarioSistema {
  id: string;
  empresaId: string;
  azureOid?: string;
  nombre: string;
  email: string;
  rol: 'ADMIN' | 'SUPERVISOR' | 'JEFE_TALLER' | 'OPERADOR' | 'CONDUCTOR' | 'BODEGA';
  activo: boolean;
}

// ─── Vehículo ────────────────────────────────────────────────
export type EstadoVehiculo = 'OPERATIVO' | 'EN_TALLER' | 'FUERA_SERVICIO';
export type TipoVehiculo = 'CAMION' | 'BUS' | 'FURGON' | 'CAMIONETA' | 'OTRO';
export type TipoCombustible = 'DIESEL' | 'BENCINA' | 'GASOLINA' | 'GNC' | 'GAS' | 'ELECTRICO' | 'HIDROGENO';
export type EstadoOperacion  = 'EN_OPERACION' | 'EN_MANTENCION' | 'FUERA_SERVICIO' | 'BAJA';
export type CondicionVehiculo = 'NUEVO' | 'USADO';

export interface Vehiculo {
  id: string;
  empresaId: string;
  patente: string;
  marca: string;
  modelo: string;
  anio: number;
  tipo: TipoVehiculo;
  combustible: TipoCombustible;
  estado: EstadoVehiculo;
  estadoOperacion?: EstadoOperacion;
  kmActuales: number;
  kmProximoServicio: number;
  vencimientoRevision?: string;
  vencimientoPermiso?: string;
  color?: string;
  numMotor?: string;
  numChasis?: string;
  qrCode?: string;
  capacidadEstanque?: number;
  taraKg?: number;
  capacidadCargaKg?: number;
  condicion?: CondicionVehiculo;
  valorCompra?: number;
  fechaCompra?: string;
  paisOrigen?: string;
  sucursalId?: string;
  eliminado: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface VehiculoRequest {
  patente: string;
  marca: string;
  modelo: string;
  anio: number;
  tipo: TipoVehiculo;
  combustible: TipoCombustible;
  kmActuales?: number;
  kmProximoServicio?: number;
  vencimientoRevision?: string;
  vencimientoPermiso?: string;
  color?: string;
}

// ─── Conductor ───────────────────────────────────────────────
export type EstadoConductor = 'ACTIVO' | 'INACTIVO' | 'VACACIONES';

export interface Conductor {
  id: string;
  empresaId: string;
  usuarioId?: string;
  nombre: string;
  rut: string;
  telefono?: string;
  email?: string;
  categoriaLicencia: string;
  vencimientoLicencia: string;
  estado: EstadoConductor;
  scoreConduccion: number;
  horasMes: number;
  kmMes: number;
  infraccionesMes: number;
  eliminado: boolean;
  createdAt: string;
}

export interface ConductorRequest {
  nombre: string;
  rut: string;
  telefono?: string;
  email?: string;
  categoriaLicencia: string;
  vencimientoLicencia: string;
  estado: EstadoConductor;
}

// ─── GPS ─────────────────────────────────────────────────────
export interface GpsTrack {
  id: number;
  empresaId: string;
  vehiculoId: string;
  conductorId?: string;
  latitud: number;
  longitud: number;
  velocidad: number;
  rumbo: number;
  estadoMotor: 'ENCENDIDO' | 'APAGADO';
  odometro?: number;
  recordedAt: string;
}

export interface GpsPosicionActual extends GpsTrack {
  patente: string;
  marca: string;
  modelo: string;
  estadoVehiculo: EstadoVehiculo;
  conductorNombre?: string;
}

export interface Geocerca {
  id: string;
  empresaId: string;
  nombre: string;
  tipo: 'CIRCULO' | 'POLIGONO';
  latitudCentro?: number;
  longitudCentro?: number;
  radioM?: number;
  activa: boolean;
}

// ─── Alerta ──────────────────────────────────────────────────
export type TipoAlerta = 'VELOCIDAD' | 'GEOCERCA' | 'MANTENCION' | 'STOCK' | 'RALENTI' | 'LICENCIA' | 'COMBUSTIBLE';
export type SeveridadAlerta = 'INFO' | 'WARNING' | 'CRITICAL';

export interface Alerta {
  id: string;
  empresaId: string;
  vehiculoId?: string;
  conductorId?: string;
  tipo: TipoAlerta;
  severidad: SeveridadAlerta;
  mensaje: string;
  resuelta: boolean;
  resueltaAt?: string;
  createdAt: string;
}

// ─── Orden de Trabajo ────────────────────────────────────────
export type TipoOT = 'PREVENTIVA' | 'CORRECTIVA' | 'NEUMATICOS' | 'ELECTRICA';
export type EstadoOT = 'PENDIENTE' | 'EN_EJECUCION' | 'BLOQUEADA' | 'CERRADA';

export interface TareaOT {
  id: string;
  descripcion: string;
  completada: number;
  orden: number;
  completadaAt?: string;
  createdAt: string;
}

export interface OrdenTrabajo {
  id: string;
  empresaId: string;
  numero: string;
  vehiculoId: string;
  vehiculoPatente?: string;
  vehiculoMarca?: string;
  tipo: TipoOT;
  estado: EstadoOT;
  descripcion: string;
  mecanicoResponsable?: string;
  avance: number;
  costoManoObra: number;
  costoRepuestos: number;
  costoTotal: number;
  fechaApertura: string;
  fechaCierreEst?: string;
  fechaCierreReal?: string;
  notas?: string;
  eliminado: boolean;
  tareas: TareaOT[];
  createdAt: string;
  updatedAt: string;
}

export interface OrdenTrabajoRequest {
  vehiculoId: string;
  tipo: TipoOT;
  descripcion: string;
  mecanicoResponsable?: string;
  fechaCierreEst?: string;
  tareas?: { descripcion: string; orden: number }[];
}

export interface DetalleOT {
  id: string;
  otId: string;
  repuestoId: string;
  repuestoCodigo?: string;
  repuestoDescripcion?: string;
  cantidad: number;
  precioUnit: number;
  subtotal: number;
}

// ─── Repuesto ────────────────────────────────────────────────
export interface Repuesto {
  id: string;
  empresaId: string;
  codigo: string;
  descripcion: string;
  categoria: string;
  unidad: string;
  stockActual: number;
  stockMinimo: number;
  precioUnitario: number;
  proveedor?: string;
  eliminado: boolean;
  createdAt: string;
}

export interface MovimientoStock {
  id: string;
  empresaId: string;
  repuestoId: string;
  repuestoCodigo?: string;
  repuestoDescripcion?: string;
  tipo: 'ENTRADA' | 'SALIDA' | 'AJUSTE';
  cantidad: number;
  precioUnit?: number;
  stockAnterior: number;
  stockNuevo: number;
  costoTotal?: number;
  referencia?: string;
  documento?: string;
  otId?: string;
  createdAt: string;
}

// ─── TareaDefinicion (catálogo de tareas OT con artículos) ───
export interface TareaDefArticulo {
  id: string;
  repuestoId: string;
  repuestoNombre?: string;
  repuestoCodigo?: string;
  repuestoUnidad?: string;
  cantidad: number;
}
export interface TareaDefinicion {
  id: string;
  empresaId: string;
  nombre: string;
  descripcion?: string;
  tipoOt?: string;
  activo: number;
  articulos: TareaDefArticulo[];
  createdAt: string;
}

// ─── KPI Dashboard ───────────────────────────────────────────
export interface KpiDashboard {
  totalVehiculos: number;
  vehiculosOperativos: number;
  vehiculosEnTaller: number;
  vehiculosFuera: number;
  totalOts: number;
  otsPendientes: number;
  otsEnEjecucion: number;
  otsCerradas: number;
  totalConductores: number;
  alertasBajoStock: number;
}

// ─── Maestros Vehículos ───────────────────────────────────────
export interface Sucursal {
  id: string; empresaId: string; nombre: string;
  direccion?: string; ciudad?: string; activa: number; eliminado: number;
}
export interface Municipalidad {
  id: string; empresaId: string; nombre: string; region?: string;
  activa: number; eliminado: number;
}
export interface Aseguradora {
  id: string; empresaId: string; nombre: string; rut?: string;
  activa: number; eliminado: number;
}
export interface PlantaRevision {
  id: string; empresaId: string; nombre: string; direccion?: string;
  activa: number; eliminado: number;
}

// ─── Documentos Vehículo ──────────────────────────────────────
export interface PermisoCirculacion {
  id: string; vehiculoId: string; empresaId: string;
  municipalidadId?: string; municipalidadNombre?: string;
  fechaPago?: string; valor?: number; fechaVencimiento?: string;
  documento?: string; createdAt: string;
}
export interface SeguroSoap {
  id: string; vehiculoId: string; empresaId: string;
  aseguradoraId?: string; aseguradoraNombre?: string;
  fechaEmision?: string; valor?: number; fechaVencimiento?: string;
  poliza?: string; createdAt: string;
}
export interface RevisionTecnica {
  id: string; vehiculoId: string; empresaId: string;
  plantaId?: string; plantaNombre?: string;
  fechaRevision?: string; valor?: number; fechaVencimiento?: string;
  resultado?: 'APROBADO' | 'RECHAZADO' | 'CONDICIONADO'; createdAt: string;
}

// ─── Combustible ─────────────────────────────────────────────
export interface CargaCombustible {
  id: string;
  empresaId: string;
  vehiculoId: string;
  vehiculoPatente?: string;
  conductorId?: string;
  conductorNombre?: string;
  numDocumento?: string;
  proveedor: string;
  estacion?: string;
  fechaCarga: string;
  litros: number;
  precioLitro: number;
  costoTotal: number;
  kmVehiculo: number;
  consumo100km?: number;
  createdAt: string;
}

// ─── Cliente ─────────────────────────────────────────────────
export interface Cliente {
  id: string;
  empresaId: string;
  rut: string;
  razonSocial: string;
  giro?: string;
  direccion?: string;
  ciudad?: string;
  pais: string;
  telefono?: string;
  email?: string;
  repLegalNombre?: string;
  repLegalRut?: string;
  activo: boolean;
  createdAt: string;
}

export interface ClienteRequest {
  rut: string;
  razonSocial: string;
  giro?: string;
  direccion?: string;
  ciudad?: string;
  pais?: string;
  telefono?: string;
  email?: string;
  repLegalNombre?: string;
  repLegalRut?: string;
}

// ─── Servicio ────────────────────────────────────────────────
export type EstadoServicio = 'BORRADOR' | 'PENDIENTE' | 'APROBADO' | 'EN_CURSO' | 'COMPLETADO' | 'CANCELADO';

export interface Servicio {
  id: string;
  empresaId: string;
  clienteId: string;
  clienteRazonSocial?: string;
  vehiculoId?: string;
  vehiculoPatente?: string;
  conductorId?: string;
  conductorNombre?: string;
  numServicio: string;
  origen: string;
  destino: string;
  kmsRecorrido?: number;
  fechaServicio: string;
  fechaTermino?: string;
  idaVuelta?: number;
  tipoServicioId?: string;
  estado: EstadoServicio;
  valorNeto: number;
  iva: number;
  valorTotal: number;
  tipoDocumento: 'FACTURA' | 'BOLETA' | 'GUIA_DESPACHO';
  numDocumento?: string;
  fechaFactura?: string;
  facturado: number;
  facturaId?: string;
  notas?: string;
  eliminado: boolean;
  createdAt: string;
}

export interface ServicioRequest {
  clienteId: string;
  vehiculoId?: string;
  conductorId?: string;
  origen: string;
  destino: string;
  kmsRecorrido?: number;
  fechaServicio: string;
  fechaTermino?: string;
  idaVuelta?: number;
  tipoServicioId?: string;
  estado?: string;
  valorNeto: number;
  tipoDocumento: 'FACTURA' | 'BOLETA' | 'GUIA_DESPACHO';
  notas?: string;
}

// ─── Tipo de Servicio ────────────────────────────────────────
export interface TipoServicio {
  id: string;
  empresaId: string;
  codigo: string;
  nombre: string;
  activo: number;
}

// ─── Factura ─────────────────────────────────────────────────
export interface Factura {
  id: string;
  empresaId: string;
  clienteId: string;
  clienteRazonSocial?: string;
  numFactura: string;
  fechaEmision: string;
  subtotal: number;
  iva: number;
  total: number;
  estado: 'EMITIDA' | 'PAGADA' | 'ANULADA';
  notas?: string;
  createdAt: string;
}

export interface FacturarRequest {
  clienteId: string;
  servicioIds: string[];
  notas?: string;
}

// ─── Dashboard / Reportes ────────────────────────────────────
export interface DashboardKpis {
  totalVehiculos: number;
  vehiculosOperativos: number;
  vehiculosEnTaller: number;
  vehiculosFueraServicio: number;
  otActivas: number;
  alertasCriticas: number;
  kmFlotaMes: number;
  costoMes: number;
}

export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
