package cl.truckmanager.almacen.service;

import cl.truckmanager.almacen.dto.AjusteStockDto;
import cl.truckmanager.almacen.dto.IngresoFacturaDto;
import cl.truckmanager.almacen.dto.RepuestoDto;
import cl.truckmanager.almacen.entity.MovimientoStock;
import cl.truckmanager.almacen.entity.Repuesto;
import cl.truckmanager.almacen.repository.MovimientoStockRepository;
import cl.truckmanager.almacen.repository.RepuestoRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RepuestoService — pruebas unitarias")
class RepuestoServiceTest {

    @Mock private RepuestoRepository repuestoRepo;
    @Mock private MovimientoStockRepository movimientoRepo;

    @InjectMocks private RepuestoService servicio;

    private static final String EMP   = "EMP-001";
    private static final String REP_ID = "REP-001";

    private Repuesto repuesto;

    @BeforeEach
    void setUp() {
        repuesto = Repuesto.builder()
                .id(REP_ID)
                .empresaId(EMP)
                .codigo("FIL-001")
                .descripcion("Filtro de aceite")
                .categoria("FILTROS")
                .unidad("UN")
                .stockActual(new BigDecimal("10"))
                .stockMinimo(new BigDecimal("2"))
                .precioUnitario(new BigDecimal("5000"))
                .build();
    }

    @Test
    @DisplayName("getAll devuelve pagina de repuestos")
    void getAll_devuelvePagina() {
        Page<Repuesto> pagina = new PageImpl<>(List.of(repuesto));
        when(repuestoRepo.buscar(eq(EMP), any(), any(), any(PageRequest.class))).thenReturn(pagina);

        Page<Repuesto> resultado = servicio.getAll(EMP, null, null, 0, 10);

        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).getCodigo()).isEqualTo("FIL-001");
    }

    @Test
    @DisplayName("getAllActivos retorna lista de repuestos activos")
    void getAllActivos_retornaLista() {
        when(repuestoRepo.findAllActivosByEmpresa(EMP)).thenReturn(List.of(repuesto));

        List<Repuesto> resultado = servicio.getAllActivos(EMP);

        assertThat(resultado).hasSize(1);
    }

    @Test
    @DisplayName("getBajoStock retorna repuestos con stock bajo minimo")
    void getBajoStock_retornaLista() {
        when(repuestoRepo.findBajoStock(EMP)).thenReturn(List.of(repuesto));

        List<Repuesto> resultado = servicio.getBajoStock(EMP);

        assertThat(resultado).hasSize(1);
    }

    @Test
    @DisplayName("getById retorna repuesto existente")
    void getById_retornaRepuesto() {
        when(repuestoRepo.findById(REP_ID)).thenReturn(Optional.of(repuesto));

        Repuesto resultado = servicio.getById(REP_ID);

        assertThat(resultado.getId()).isEqualTo(REP_ID);
    }

    @Test
    @DisplayName("getById lanza EntityNotFoundException si no existe")
    void getById_lanzaExcepcionSiNoExiste() {
        when(repuestoRepo.findById(REP_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.getById(REP_ID))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(REP_ID);
    }

    @Test
    @DisplayName("crear guarda nuevo repuesto")
    void crear_guardaRepuesto() {
        RepuestoDto dto = new RepuestoDto();
        dto.setCodigo("FIL-002");
        dto.setDescripcion("Filtro de aire");
        dto.setCategoria("FILTROS");
        dto.setStockActual(new BigDecimal("5"));
        dto.setPrecioUnitario(new BigDecimal("3500"));

        when(repuestoRepo.save(any(Repuesto.class))).thenReturn(repuesto);

        Repuesto resultado = servicio.crear(EMP, dto);

        assertThat(resultado).isNotNull();
        verify(repuestoRepo).save(any(Repuesto.class));
    }

    @Test
    @DisplayName("actualizar modifica campos del repuesto")
    void actualizar_modificaRepuesto() {
        RepuestoDto dto = new RepuestoDto();
        dto.setCodigo("FIL-001-MOD");
        dto.setDescripcion("Filtro de aceite modificado");
        dto.setCategoria("FILTROS");
        dto.setStockMinimo(new BigDecimal("3"));
        dto.setPrecioUnitario(new BigDecimal("5500"));

        when(repuestoRepo.findById(REP_ID)).thenReturn(Optional.of(repuesto));
        when(repuestoRepo.save(any(Repuesto.class))).thenReturn(repuesto);

        Repuesto resultado = servicio.actualizar(REP_ID, dto);

        assertThat(resultado).isNotNull();
        verify(repuestoRepo).save(repuesto);
    }

    @Test
    @DisplayName("ajustarStock ENTRADA incrementa stock")
    void ajustarStock_entrada_incrementaStock() {
        AjusteStockDto dto = new AjusteStockDto();
        dto.setTipo("ENTRADA");
        dto.setCantidad(new BigDecimal("5"));

        when(repuestoRepo.findById(REP_ID)).thenReturn(Optional.of(repuesto));
        when(repuestoRepo.save(any())).thenReturn(repuesto);
        when(movimientoRepo.save(any(MovimientoStock.class))).thenReturn(null);

        servicio.ajustarStock(REP_ID, dto);

        assertThat(repuesto.getStockActual()).isEqualByComparingTo(new BigDecimal("15"));
        verify(movimientoRepo).save(any(MovimientoStock.class));
    }

    @Test
    @DisplayName("ajustarStock SALIDA descuenta stock")
    void ajustarStock_salida_descontaStock() {
        AjusteStockDto dto = new AjusteStockDto();
        dto.setTipo("SALIDA");
        dto.setCantidad(new BigDecimal("3"));

        when(repuestoRepo.findById(REP_ID)).thenReturn(Optional.of(repuesto));
        when(repuestoRepo.save(any())).thenReturn(repuesto);
        when(movimientoRepo.save(any(MovimientoStock.class))).thenReturn(null);

        servicio.ajustarStock(REP_ID, dto);

        assertThat(repuesto.getStockActual()).isEqualByComparingTo(new BigDecimal("7"));
    }

    @Test
    @DisplayName("ajustarStock SALIDA lanza excepcion si stock insuficiente")
    void ajustarStock_salida_lanzaExcepcionStockInsuficiente() {
        AjusteStockDto dto = new AjusteStockDto();
        dto.setTipo("SALIDA");
        dto.setCantidad(new BigDecimal("20"));

        when(repuestoRepo.findById(REP_ID)).thenReturn(Optional.of(repuesto));

        assertThatThrownBy(() -> servicio.ajustarStock(REP_ID, dto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Stock insuficiente");
    }

    @Test
    @DisplayName("ajustarStock AJUSTE establece stock absoluto")
    void ajustarStock_ajuste_estableceStockAbsoluto() {
        AjusteStockDto dto = new AjusteStockDto();
        dto.setTipo("AJUSTE");
        dto.setCantidad(new BigDecimal("8"));

        when(repuestoRepo.findById(REP_ID)).thenReturn(Optional.of(repuesto));
        when(repuestoRepo.save(any())).thenReturn(repuesto);
        when(movimientoRepo.save(any(MovimientoStock.class))).thenReturn(null);

        servicio.ajustarStock(REP_ID, dto);

        assertThat(repuesto.getStockActual()).isEqualByComparingTo(new BigDecimal("8"));
    }

    @Test
    @DisplayName("eliminar marca repuesto como eliminado")
    void eliminar_marcaComoEliminado() {
        when(repuestoRepo.findById(REP_ID)).thenReturn(Optional.of(repuesto));
        when(repuestoRepo.save(any())).thenReturn(repuesto);

        servicio.eliminar(REP_ID);

        assertThat(repuesto.getEliminado()).isEqualTo(1);
        verify(repuestoRepo).save(repuesto);
    }

    @Test
    @DisplayName("ingresoFactura procesa multiples lineas y actualiza stock")
    void ingresoFactura_procesaLineas() {
        IngresoFacturaDto.LineaDto linea1 = new IngresoFacturaDto.LineaDto();
        linea1.setRepuestoId(REP_ID);
        linea1.setCantidad(new BigDecimal("5"));
        linea1.setPrecioUnit(new BigDecimal("4500"));

        IngresoFacturaDto dto = new IngresoFacturaDto();
        dto.setTipoDocumento("FACTURA");
        dto.setNumDocumento("F-001");
        dto.setProveedor("Proveedor SA");
        dto.setLineas(List.of(linea1));

        when(repuestoRepo.findById(REP_ID)).thenReturn(Optional.of(repuesto));
        when(repuestoRepo.save(any())).thenReturn(repuesto);
        when(movimientoRepo.save(any(MovimientoStock.class))).thenReturn(null);

        java.util.Map<String, Object> resultado = servicio.ingresoFactura(EMP, dto);

        assertThat(resultado.get("movimientos")).isEqualTo(1);
        assertThat((java.math.BigDecimal) resultado.get("totalCLP"))
                .isEqualByComparingTo(new BigDecimal("22500"));
        assertThat(repuesto.getStockActual()).isEqualByComparingTo(new BigDecimal("15"));
    }

    @Test
    @DisplayName("ingresoFactura con precio cero no actualiza precio unitario")
    void ingresoFactura_precioCero_noActualizaPrecio() {
        IngresoFacturaDto.LineaDto linea = new IngresoFacturaDto.LineaDto();
        linea.setRepuestoId(REP_ID);
        linea.setCantidad(new BigDecimal("2"));
        linea.setPrecioUnit(BigDecimal.ZERO);

        IngresoFacturaDto dto = new IngresoFacturaDto();
        dto.setTipoDocumento("GUIA_DESPACHO");
        dto.setNumDocumento("GD-001");
        dto.setProveedor("Proveedor SA");
        dto.setLineas(List.of(linea));

        BigDecimal precioOriginal = repuesto.getPrecioUnitario();
        when(repuestoRepo.findById(REP_ID)).thenReturn(Optional.of(repuesto));
        when(repuestoRepo.save(any())).thenReturn(repuesto);
        when(movimientoRepo.save(any(MovimientoStock.class))).thenReturn(null);

        servicio.ingresoFactura(EMP, dto);

        assertThat(repuesto.getPrecioUnitario()).isEqualByComparingTo(precioOriginal);
    }
}
