package com.redxela.controller;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.staticfiles.Location;
import io.javalin.websocket.WsMessageContext;

import com.redxela.service.ColaPrioridadPedidos;
import com.redxela.service.MotorPlanificador;
import com.redxela.service.PoliticaPlanificacion;
import com.redxela.concurrencia.GestorRecursos;
import com.redxela.concurrencia.ProcesoSimulacionPedido;
import com.redxela.concurrencia.deadlock.SimuladorDeadlock;
import com.redxela.concurrencia.deadlock.InformacionConflicto;
import com.redxela.almacen.AlmacenPaginado;
import com.redxela.model.Pedido;
import com.redxela.model.TipoRecurso;
import com.redxela.model.Cliente;
import com.redxela.model.NivelServicio;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.List;

// servidor javalin que expone rutas para el modulo 3 y el sistema completo
public class ControladorServidor {

    // instancia del servidor javalin
    private final Javalin app;

    // referencia a los servicios compartidos
    private final ColaPrioridadPedidos cola;
    private final GestorRecursos gestor;
    private final AlmacenPaginado almacen;
    private final SimuladorDeadlock simulador;

    // referencia opcional al motor para reportar procesamiento y metricas
    private final MotorPlanificador motor;

    /**
     * Constructor sobrecargado que inicializa el servidor con los servicios existentes.
     *
     * @param cola cola de pedidos activa.
     * @param gestor gestor de recursos compartidos.
     * @param almacen almacen paginado del centro.
     */
    public ControladorServidor(ColaPrioridadPedidos cola, GestorRecursos gestor, AlmacenPaginado almacen) {
        this(cola, gestor, almacen, null);
    }

    /**
     * Constructor sobrecargado que incluye el motor planificador para observacion.
     *
     * @param cola cola de pedidos activa.
     * @param gestor gestor de recursos compartidos.
     * @param almacen almacen paginado del centro.
     * @param motor motor despachador de pedidos.
     */
    public ControladorServidor(ColaPrioridadPedidos cola, GestorRecursos gestor, AlmacenPaginado almacen, MotorPlanificador motor) {
        // inicializar referencias compartidas
        this.cola = cola;
        this.gestor = gestor;
        this.almacen = almacen;
        this.motor = motor;
        this.simulador = new SimuladorDeadlock(gestor);
        // crear la instancia de javalin en el puerto 7070
        String rutaEstatica = resolverRutaEstatica();
        this.app = Javalin.create(config -> {
            // configurar archivos estaticos del frontend
            config.staticFiles.add(rutaEstatica, Location.EXTERNAL);
            config.spaRoot.addFile("/", rutaEstatica + "/index.html", Location.EXTERNAL);
        }).start(7070);
        // configurar rutas disponibles del servidor
        this.configurarRutas();
    }

    // resolver ruta del frontend compilado segun directorio de trabajo
    private String resolverRutaEstatica() {
        // buscar candidatos relativos sin usar rutas absolutas fijas
        String[] candidatos = new String[]{
            "../frontend/dist",
            "frontend/dist",
            "PF_Sopes2/frontend/dist"
        };
        for (int i = 0; i < candidatos.length; i++) {
            File carpeta = new File(candidatos[i]);
            if (carpeta.exists()) {
                if (carpeta.isDirectory()) {
                    return carpeta.getAbsolutePath();
                }
            }
        }
        // usar ruta por defecto cuando no existe ningun candidato
        return new File("../frontend/dist").getAbsolutePath();
    }

    // configurar las rutas disponibles en el servidor javalin
    private void configurarRutas() {

        // linea en blanco tras apertura de bloque
        this.app.get("/api/estado", this::manejadorEstado);
        this.app.get("/api/recursos", this::manejadorRecursos);
        this.app.get("/api/procesando", this::manejadorProcesando);
        this.app.get("/api/cola", this::manejadorCola);
        this.app.get("/api/estadisticas", this::manejadorEstadisticas);
        this.app.get("/api/almacen", this::manejadorAlmacen);
        this.app.get("/api/deadlock/detalle", this::manejadorDetalleDeadlock);
        this.app.post("/api/pedidos", this::manejadorRegistrarPedido);
        this.app.post("/api/politica", this::manejadorCambiarPolitica);
        this.app.post("/api/deadlock/inducir", this::manejadorInducirDeadlock);
        this.app.post("/api/deadlock/resolver", this::manejadorResolverDeadlock);
        this.app.ws("/ws/simulacion", ws -> {
            ws.onMessage((WsMessageContext ctx) -> {
                // reenviar mensaje de confirmacion al cliente
                String mensaje = ctx.message();
                try {
                    ctx.send("Confirmado: " + mensaje);
                } catch (Exception e) {
                    System.err.println("Error enviando mensaje WebSocket: " + e.getMessage());
                }
            });
        });

        // linea en blanco antes de cierre de bloque
    }

    // manejar solicitud de estado general del sistema
    private void manejadorEstado(Context ctx) {
        // construir respuesta JSON con el estado de recursos
        String json = "{"
            + "\"montacargas\":" + gestor.consultarDisponibles(com.redxela.model.TipoRecurso.MONTACARGAS)
            + ",\"estacionesEmpaque\":" + gestor.consultarDisponibles(com.redxela.model.TipoRecurso.ESTACION_EMPAQUE)
            + ",\"areasCarga\":" + gestor.consultarDisponibles(com.redxela.model.TipoRecurso.AREA_CARGA)
            + ",\"encargadosBodega\":" + gestor.consultarDisponibles(com.redxela.model.TipoRecurso.ENCARGADO_BODEGA)
            + ",\"estacionesCalidad\":" + gestor.consultarDisponibles(com.redxela.model.TipoRecurso.ESTACION_CONTROL_CALIDAD)
            + ",\"sistemasEscaneo\":" + gestor.consultarDisponibles(com.redxela.model.TipoRecurso.SISTEMA_ESCANEO)
            + "}";
        ctx.contentType("application/json").result(json);
    }

    // manejar solicitud del estado del almacen paginado
    private void manejadorAlmacen(Context ctx) {
        // construir JSON con las celdas del almacen
        int ocupados = almacen.getEspaciosOcupados();
        boolean hayOcupacion = false;
        if (ocupados > 0) {
            hayOcupacion = true;
        }
        String celdas = "";
        for (int p = 0; p < 4; p++) {
            for (int n = 0; n < 3; n++) {
                for (int e = 0; e < 6; e++) {
                    String celda = "{\"pasillo\":" + (p + 1)
                        + ",\"nivel\":" + (n + 1)
                        + ",\"espacio\":" + (e + 1)
                        + ",\"ocupada\":" + hayOcupacion
                        + "}";
                    if (celdas.equals("")) {
                        celdas = celda;
                    } else {
                        celdas = celdas + "," + celda;
                    }
                }
            }
        }
        String json = "{\"capacidadTotal\":" + almacen.getCapacidadTotal()
            + ",\"espaciosOcupados\":" + ocupados
            + ",\"espaciosDisponibles\":" + almacen.getEspaciosDisponibles()
            + ",\"celdas\":[" + celdas + "]}";
        ctx.contentType("application/json").result(json);
    }

    // manejar solicitud de recursos con tenedores actuales
    private void manejadorRecursos(Context ctx) {
        // construir arreglo con el estado de cada recurso compartido
        String elementos = "";
        TipoRecurso[] tipos = TipoRecurso.values();
        for (int i = 0; i < tipos.length; i++) {
            TipoRecurso tipo = tipos[i];
            List<String> tenedores = this.gestor.obtenerTenedores(tipo);
            String listaTenedores = "";
            for (int j = 0; j < tenedores.size(); j++) {
                if (j > 0) {
                    listaTenedores = listaTenedores + ",";
                }
                listaTenedores = listaTenedores + "\"" + tenedores.get(j) + "\"";
            }
            String elemento = "{\"nombre\":\"" + tipo.getNombre() + "\""
                + ",\"total\":" + this.gestor.consultarTotal(tipo)
                + ",\"disponibles\":" + this.gestor.consultarDisponibles(tipo)
                + ",\"enUso\":" + this.gestor.consultarEnUso(tipo)
                + ",\"tenedores\":[" + listaTenedores + "]}";
            if (i > 0) {
                elementos = elementos + ",";
            }
            elementos = elementos + elemento;
        }
        ctx.contentType("application/json").result("[" + elementos + "]");
    }

    // manejar solicitud de pedidos en procesamiento con progreso
    private void manejadorProcesando(Context ctx) {
        // responder lista vacia cuando no hay motor asociado
        if (this.motor == null) {
            ctx.contentType("application/json").result("[]");
            return;
        }
        List<ProcesoSimulacionPedido> tareas = this.motor.obtenerTareasActivas();
        String elementos = "";
        for (int i = 0; i < tareas.size(); i++) {
            ProcesoSimulacionPedido tarea = tareas.get(i);
            Pedido pedido = tarea.getPedido();
            String elemento = "{\"id\":\"" + pedido.getId() + "\""
                + ",\"cliente\":\"" + pedido.getCliente().getNombre() + "\""
                + ",\"nivel\":\"" + pedido.getNivelServicio().getNombre() + "\""
                + ",\"articulos\":" + pedido.getCantidadTotal()
                + ",\"estado\":\"" + pedido.getEstado().name() + "\""
                + ",\"progreso\":" + tarea.getProgreso()
                + ",\"restanteMs\":" + tarea.getTiempoRestanteMs()
                + ",\"esperaMs\":" + tarea.getTiempoEsperaMs()
                + "}";
            if (i > 0) {
                elementos = elementos + ",";
            }
            elementos = elementos + elemento;
        }
        ctx.contentType("application/json").result("[" + elementos + "]");
    }

    // manejar solicitud de lista de espera con criterio de orden
    private void manejadorCola(Context ctx) {
        List<Pedido> espera = this.cola.obtenerPedidosEnEspera();
        String elementos = "";
        long ahora = System.currentTimeMillis();
        for (int i = 0; i < espera.size(); i++) {
            Pedido pedido = espera.get(i);
            String elemento = "{\"posicion\":" + (i + 1)
                + ",\"id\":\"" + pedido.getId() + "\""
                + ",\"cliente\":\"" + pedido.getCliente().getNombre() + "\""
                + ",\"nivel\":\"" + pedido.getNivelServicio().getNombre() + "\""
                + ",\"articulos\":" + pedido.getCantidadTotal()
                + ",\"esperaMs\":" + (ahora - pedido.getTiempoCreacionMs())
                + "}";
            if (i > 0) {
                elementos = elementos + ",";
            }
            elementos = elementos + elemento;
        }
        String respuesta = "{\"politica\":\"" + this.cola.getPoliticaActual().getNombre() + "\""
            + ",\"enEspera\":" + espera.size()
            + ",\"pedidos\":[" + elementos + "]}";
        ctx.contentType("application/json").result(respuesta);
    }

    // manejar solicitud de estadisticas generales del sistema
    private void manejadorEstadisticas(Context ctx) {
        int completados = 0;
        long esperaPromedio = 0L;
        long procesoPromedio = 0L;
        int enProceso = 0;
        if (this.motor != null) {
            completados = this.motor.getEstadisticas().obtenerCompletados();
            esperaPromedio = this.motor.getEstadisticas().obtenerEsperaPromedioMs();
            procesoPromedio = this.motor.getEstadisticas().obtenerProcesoPromedioMs();
            enProceso = this.motor.getCantidadHilosActivos();
        }
        String respuesta = "{\"completados\":" + completados
            + ",\"esperaPromedioMs\":" + esperaPromedio
            + ",\"procesoPromedioMs\":" + procesoPromedio
            + ",\"enEspera\":" + this.cola.getCantidadEnEspera()
            + ",\"enProceso\":" + enProceso
            + "}";
        ctx.contentType("application/json").result(respuesta);
    }

    // manejar solicitud de detalle del conflicto de recursos
    private void manejadorDetalleDeadlock(Context ctx) {
        InformacionConflicto conflicto = this.simulador.getUltimoConflicto();
        if (conflicto == null) {
            ctx.contentType("application/json").result("{\"activo\":false}");
            return;
        }
        String respuesta = "{\"activo\":true"
            + ",\"resuelto\":" + conflicto.isResuelto()
            + ",\"ganador\":\"" + conflicto.getIdPedidoGanador() + "\""
            + ",\"causa\":\"" + conflicto.getCausaBloqueo() + "\""
            + ",\"pedido1\":{\"id\":\"" + conflicto.getIdPedido1() + "\""
            + ",\"retiene\":\"" + conflicto.getRecursoRetenido1().getNombre() + "\""
            + ",\"espera\":\"" + conflicto.getRecursoSolicitado1().getNombre() + "\"}"
            + ",\"pedido2\":{\"id\":\"" + conflicto.getIdPedido2() + "\""
            + ",\"retiene\":\"" + conflicto.getRecursoRetenido2().getNombre() + "\""
            + ",\"espera\":\"" + conflicto.getRecursoSolicitado2().getNombre() + "\"}"
            + "}";
        ctx.contentType("application/json").result(respuesta);
    }

    // registrar pedido manual recibido por post
    private void manejadorRegistrarPedido(Context ctx) {
        try {
            // parsear cuerpo JSON de la solicitud
            ObjectMapper mapper = new ObjectMapper();
            JsonNode cuerpo = mapper.readTree(ctx.body());

            String id = cuerpo.has("id") ? cuerpo.get("id").asText() : "MAN-001";
            String nombreCliente = cuerpo.has("cliente") ? cuerpo.get("cliente").asText() : "Cliente Generico";
            String nivelStr = cuerpo.has("nivel") ? cuerpo.get("nivel").asText() : "ESTANDAR";

            NivelServicio nivel = NivelServicio.ESTANDAR;
            if (nivelStr.equals("EXPRES")) {
                nivel = NivelServicio.EXPRES;
            } else if (nivelStr.equals("PRIORITARIO")) {
                nivel = NivelServicio.PRIORITARIO;
            } else if (nivelStr.equals("ECONOMICO")) {
                nivel = NivelServicio.ECONOMICO;
            } else if (nivelStr.equals("PROGRAMADO")) {
                nivel = NivelServicio.PROGRAMADO;
            }

            Cliente cliente = new Cliente(id + "-CLI", nombreCliente, "contacto@generico.com");
            Pedido nuevo = new Pedido(id, cliente, nivel);
            cola.encolarPedido(nuevo);

            ctx.contentType("application/json").result("{\"mensaje\":\"Pedido registrado exitosamente\",\"id\":\"" + id + "\"}");
        } catch (Exception e) {
            ctx.status(400).contentType("application/json").result("{\"error\":\"Error procesando solicitud\"}");
        }
    }

    // cambiar la politica de planificacion activa
    private void manejadorCambiarPolitica(Context ctx) {
        try {
            // parsear cuerpo JSON para obtener la nueva politica
            ObjectMapper mapper = new ObjectMapper();
            JsonNode cuerpo = mapper.readTree(ctx.body());
            String politicaStr = cuerpo.has("politica") ? cuerpo.get("politica").asText() : "NIVEL_SERVICIO_ESTRICTO";

            if (politicaStr.equals("MENOR_CANTIDAD_ARTICULOS")) {
                cola.cambiarPolitica(PoliticaPlanificacion.MENOR_CANTIDAD_ARTICULOS);
            } else if (politicaStr.equals("TIEMPO_ESPERA_FIFO")) {
                cola.cambiarPolitica(PoliticaPlanificacion.TIEMPO_ESPERA_FIFO);
            } else {
                cola.cambiarPolitica(PoliticaPlanificacion.NIVEL_SERVICIO_ESTRICTO);
            }

            ctx.contentType("application/json").result("{\"mensaje\":\"Politica actualizada\",\"politica\":\"" + politicaStr + "\"}");
        } catch (Exception e) {
            ctx.status(400).contentType("application/json").result("{\"error\":\"Error procesando solicitud\"}");
        }
    }

    // inducir el escenario de interbloqueo deliberado
    private void manejadorInducirDeadlock(Context ctx) {
        // usar referencias existentes para provocar deadlock
        Pedido p1 = new Pedido("DL-A", new Cliente(), com.redxela.model.NivelServicio.PRIORITARIO);
        Pedido p2 = new Pedido("DL-B", new Cliente(), com.redxela.model.NivelServicio.PRIORITARIO);
        InformacionConflicto conflicto = simulador.forzarDeadlockDeliberado(p1, com.redxela.model.TipoRecurso.MONTACARGAS, com.redxela.model.TipoRecurso.ESTACION_EMPAQUE, p2);
        if (conflicto != null) {
            ctx.result("Interbloqueo inducido: " + conflicto.getCausaBloqueo());
        } else {
            ctx.result("No se detecto interbloqueo");
        }
    }

    // resolver manualmente el conflicto mediante despropiacion
    private void manejadorResolverDeadlock(Context ctx) {
        try {
            // parsear cuerpo JSON para obtener el ID del pedido ganador
            ObjectMapper mapper = new ObjectMapper();
            JsonNode cuerpo = mapper.readTree(ctx.body());
            String idGanador = cuerpo.has("id") ? cuerpo.get("id").asText() : "DL-A";

            boolean resuelto = simulador.resolverConflictoManual(idGanador);
            if (resuelto) {
                ctx.contentType("application/json").result("{\"mensaje\":\"Conflicto resuelto manualmente\",\"id\":\"" + idGanador + "\"}");
            } else {
                ctx.contentType("application/json").result("{\"mensaje\":\"No hay conflicto activo para resolver\"}");
            }
        } catch (Exception e) {
            ctx.status(400).contentType("application/json").result("{\"error\":\"Error procesando solicitud\"}");
        }
    }

    /**
     * Obtener el servidor javalin creado.
     *
     * @return instancia del servidor.
     */
    public Javalin getApp() {
        return this.app;
    }
}
