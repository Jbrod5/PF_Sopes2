package com.redxela.controller;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.staticfiles.Location;
import io.javalin.websocket.WsCloseContext;
import io.javalin.websocket.WsConnectContext;
import io.javalin.websocket.WsMessageContext;
import io.javalin.websocket.WsContext;

import com.redxela.service.ColaPrioridadPedidos;
import com.redxela.service.MotorPlanificador;
import com.redxela.service.OyenteSimulacion;
import com.redxela.service.PoliticaPlanificacion;
import com.redxela.service.RegistroClientes;
import com.redxela.service.ServicioRecepcion;
import com.redxela.concurrencia.GestorRecursos;
import com.redxela.concurrencia.ProcesoSimulacionPedido;
import com.redxela.concurrencia.deadlock.SimuladorDeadlock;
import com.redxela.concurrencia.deadlock.InformacionConflicto;
import com.redxela.almacen.AlmacenPaginado;
import com.redxela.model.Pedido;
import com.redxela.model.ItemPedido;
import com.redxela.model.Producto;
import com.redxela.model.TipoRecurso;
import com.redxela.model.Cliente;
import com.redxela.model.NivelServicio;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

// servidor javalin que expone rutas para el modulo 3 y el sistema completo
public class ControladorServidor implements OyenteSimulacion {

    // instancia del servidor javalin
    private final Javalin app;

    // referencia a los servicios compartidos
    private final ColaPrioridadPedidos cola;
    private final GestorRecursos gestor;
    private final AlmacenPaginado almacen;
    private final SimuladorDeadlock simulador;

    // referencia opcional al motor para reportar procesamiento y metricas
    private final MotorPlanificador motor;

    // referencia opcional a recepcion para el ingreso manual de mercaderia
    private ServicioRecepcion recepcion;

    // referencia opcional al registro de cuentas de clientes
    private RegistroClientes registroClientes;

    // contador secuencial para generar identificadores de pedido en el servidor
    private final AtomicInteger contadorPedidos;

    // sesiones activas del canal de eventos en tiempo real
    private final Set<WsContext> sesionesWs;

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
        this.recepcion = null;
        this.registroClientes = null;
        this.contadorPedidos = new AtomicInteger(1);
        this.sesionesWs = ConcurrentHashMap.newKeySet();
        this.simulador = new SimuladorDeadlock(gestor);
        // suscribir el servidor a los eventos del motor para el tablero en vivo
        if (motor != null) {
            motor.setOyente(this);
        }
        // crear la instancia de javalin en el puerto 7070
        java.net.URL recursoEmpaquetado = ControladorServidor.class.getResource("/public/index.html");
        boolean enJar = false;
        if (recursoEmpaquetado != null) {
            enJar = true;
        }
        // buscar carpeta externa solo cuando no hay frontend empaquetado
        String rutaExterna = null;
        if (!enJar) {
            rutaExterna = resolverRutaEstatica();
        }
        boolean servirFrontend = false;
        if (enJar) {
            servirFrontend = true;
        } else if (rutaExterna != null) {
            servirFrontend = true;
        }
        final boolean empaquetadoFinal = enJar;
        final boolean servirFinal = servirFrontend;
        final String rutaFinal = rutaExterna;
        this.app = Javalin.create(config -> {
            // servir frontend empaquetado en el jar o carpeta externa en desarrollo
            if (empaquetadoFinal) {
                config.staticFiles.add("/public", Location.CLASSPATH);
                config.spaRoot.addFile("/", "/public/index.html", Location.CLASSPATH);
            } else if (servirFinal) {
                config.staticFiles.add(rutaFinal, Location.EXTERNAL);
                config.spaRoot.addFile("/", rutaFinal + "/index.html", Location.EXTERNAL);
            }
        }).start(7070);
        if (!servirFinal) {
            System.err.println("[SERVIDOR] Advertencia: no se encontro frontend/dist, solo se servira la API REST");
        }
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
        // permitir ruta personalizada por variable de entorno
        String rutaEntorno = System.getenv("REDXELA_FRONTEND_DIST");
        if (rutaEntorno != null) {
            File carpetaEntorno = new File(rutaEntorno);
            if (carpetaEntorno.exists()) {
                if (carpetaEntorno.isDirectory()) {
                    return carpetaEntorno.getAbsolutePath();
                }
            }
        }
        // retornar nulo cuando no existe ningun candidato
        return null;
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
        this.app.get("/api/productos", this::manejadorProductos);
        this.app.post("/api/recepcion", this::manejadorRegistrarMercaderia);
        this.app.get("/api/deadlock/detalle", this::manejadorDetalleDeadlock);
        this.app.post("/api/pedidos", this::manejadorRegistrarPedido);
        this.app.post("/api/politica", this::manejadorCambiarPolitica);
        this.app.post("/api/deadlock/inducir", this::manejadorInducirDeadlock);
        this.app.post("/api/deadlock/resolver", this::manejadorResolverDeadlock);
        this.app.get("/api/clientes", this::manejadorListarClientes);
        this.app.post("/api/clientes", this::manejadorRegistrarCliente);
        this.app.ws("/ws/simulacion", ws -> {
            ws.onConnect((WsConnectContext ctx) -> {
                // registrar la nueva sesion del tablero en vivo
                this.sesionesWs.add(ctx);
            });
            ws.onClose((WsCloseContext ctx) -> {
                // remover la sesion desconectada del tablero en vivo
                this.sesionesWs.remove(ctx);
            });
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
        // construir JSON con el estado real celda por celda
        List<com.redxela.almacen.UbicacionAlmacen> celdas = almacen.obtenerEstadoCeldas();
        String lista = "";
        for (int i = 0; i < celdas.size(); i++) {
            com.redxela.almacen.UbicacionAlmacen actual = celdas.get(i);
            String lote = actual.getIdLoteAsignado();
            if (lote == null) {
                lote = "";
            }
            String elemento = "{\"pasillo\":" + actual.getPasillo()
                + ",\"nivel\":" + actual.getNivel()
                + ",\"espacio\":" + actual.getEspacio()
                + ",\"ocupada\":" + actual.isOcupada()
                + ",\"lote\":\"" + lote + "\"}";
            if (i > 0) {
                lista = lista + ",";
            }
            lista = lista + elemento;
        }
        String json = "{\"capacidadTotal\":" + almacen.getCapacidadTotal()
            + ",\"espaciosOcupados\":" + almacen.getEspaciosOcupados()
            + ",\"espaciosDisponibles\":" + almacen.getEspaciosDisponibles()
            + ",\"celdas\":[" + lista + "]}";
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

    // manejar solicitud del catalogo de productos disponibles
    private void manejadorProductos(Context ctx) {
        if (this.recepcion == null) {
            ctx.contentType("application/json").result("[]");
            return;
        }
        List<Producto> catalogo = this.recepcion.obtenerCatalogo();
        String elementos = "";
        for (int i = 0; i < catalogo.size(); i++) {
            Producto producto = catalogo.get(i);
            String elemento = "{\"id\":\"" + producto.getId() + "\""
                + ",\"nombre\":\"" + producto.getNombre() + "\""
                + ",\"mercancia\":\"" + producto.getTipoMercancia().getNombre() + "\"}";
            if (i > 0) {
                elementos = elementos + ",";
            }
            elementos = elementos + elemento;
        }
        ctx.contentType("application/json").result("[" + elementos + "]");
    }

    // registrar manualmente la llegada de mercaderia
    private void manejadorRegistrarMercaderia(Context ctx) {
        if (this.recepcion == null) {
            ctx.status(503).contentType("application/json").result("{\"error\":\"Recepcion no disponible\"}");
            return;
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode cuerpo = mapper.readTree(ctx.body());
            String idProducto = "";
            if (cuerpo.has("producto")) {
                idProducto = cuerpo.get("producto").asText();
            }
            int cantidad = 0;
            if (cuerpo.has("cantidad")) {
                cantidad = cuerpo.get("cantidad").asInt(0);
            }
            boolean ingresado = this.recepcion.ingresarLoteManual(idProducto, cantidad);
            if (ingresado) {
                ctx.contentType("application/json").result("{\"mensaje\":\"Mercaderia registrada exitosamente\"}");
            } else {
                ctx.status(400).contentType("application/json").result("{\"error\":\"Producto desconocido o cantidad invalida\"}");
            }
        } catch (Exception e) {
            ctx.status(400).contentType("application/json").result("{\"error\":\"Error procesando solicitud\"}");
        }
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

            // resolver identificador unico generando uno en el servidor si falta o se repite
            String id = "";
            if (cuerpo.has("id")) {
                id = cuerpo.get("id").asText().trim();
            }
            if (id.equals("")) {
                id = "WEB-" + this.contadorPedidos.getAndIncrement();
            } else if (this.cola.existePedidoConId(id)) {
                id = "WEB-" + this.contadorPedidos.getAndIncrement();
            }

            // resolver nivel de servicio con valor por defecto
            String nivelStr = "ESTANDAR";
            if (cuerpo.has("nivel")) {
                nivelStr = cuerpo.get("nivel").asText();
            }
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

            // resolver cliente desde el registro cuando se indica identificador
            Cliente cliente = null;
            if (cuerpo.has("idCliente")) {
                String idCliente = cuerpo.get("idCliente").asText();
                if (this.registroClientes == null) {
                    ctx.status(503).contentType("application/json").result("{\"error\":\"Registro de clientes no disponible\"}");
                    return;
                }
                cliente = this.registroClientes.obtenerCliente(idCliente);
                if (cliente == null) {
                    ctx.status(400).contentType("application/json").result("{\"error\":\"Cliente no registrado\"}");
                    return;
                }
            } else {
                // mantener compatibilidad creando cliente sintetico con el nombre recibido
                String nombreCliente = "Cliente Generico";
                if (cuerpo.has("cliente")) {
                    nombreCliente = cuerpo.get("cliente").asText();
                }
                cliente = new Cliente(id + "-CLI", nombreCliente, "contacto@generico.com");
            }

            // crear el pedido base con cliente y nivel resueltos
            Pedido nuevo = new Pedido(id, cliente, nivel);

            // agregar lineas de productos cuando se incluyen en la solicitud
            if (cuerpo.has("items")) {
                if (this.recepcion == null) {
                    ctx.status(503).contentType("application/json").result("{\"error\":\"Recepcion no disponible para validar productos\"}");
                    return;
                }
                List<Producto> catalogo = this.recepcion.obtenerCatalogo();
                JsonNode items = cuerpo.get("items");
                for (int i = 0; i < items.size(); i++) {
                    JsonNode linea = items.get(i);
                    String idProducto = "";
                    if (linea.has("producto")) {
                        idProducto = linea.get("producto").asText();
                    }
                    int cantidad = 0;
                    if (linea.has("cantidad")) {
                        cantidad = linea.get("cantidad").asInt(0);
                    }
                    if (cantidad <= 0) {
                        ctx.status(400).contentType("application/json").result("{\"error\":\"Cantidad invalida en la linea " + (i + 1) + "\"}");
                        return;
                    }
                    Producto encontrado = null;
                    for (int j = 0; j < catalogo.size(); j++) {
                        if (catalogo.get(j).getId().equals(idProducto)) {
                            encontrado = catalogo.get(j);
                        }
                    }
                    if (encontrado == null) {
                        ctx.status(400).contentType("application/json").result("{\"error\":\"Producto desconocido: " + idProducto + "\"}");
                        return;
                    }
                    nuevo.agregarItem(new ItemPedido(encontrado, cantidad));
                }
            }

            // encolar el pedido construido
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
            this.alCambiarDeadlock();
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
                this.alCambiarDeadlock();
                ctx.contentType("application/json").result("{\"mensaje\":\"Conflicto resuelto manualmente\",\"id\":\"" + idGanador + "\"}");
            } else {
                ctx.contentType("application/json").result("{\"mensaje\":\"No hay conflicto activo para resolver\"}");
            }
        } catch (Exception e) {
            ctx.status(400).contentType("application/json").result("{\"error\":\"Error procesando solicitud\"}");
        }
    }

    // difundir un evento a todas las sesiones del tablero en vivo
    private void difundirEvento(String tipo, String datosJson) {
        if (this.sesionesWs.isEmpty()) {
            return;
        }
        // construir el mensaje con formato tipo y datos
        String mensaje = "{\"tipo\":\"" + tipo + "\",\"datos\":" + datosJson + "}";
        for (WsContext sesion : this.sesionesWs) {
            try {
                sesion.send(mensaje);
            } catch (Exception e) {
                System.err.println("Error difundiendo evento WebSocket: " + e.getMessage());
            }
        }
    }

    /**
     * Notificar que un pedido fue despachado a procesamiento.
     *
     * @param pedido pedido despachado.
     */
    @Override
    public void alDespacharPedido(Pedido pedido) {
        this.difundirEvento("tablero", "{\"evento\":\"despacho\",\"id\":\"" + pedido.getId() + "\"}");
    }

    /**
     * Notificar que un pedido completo su procesamiento.
     *
     * @param pedido pedido completado.
     */
    @Override
    public void alCompletarPedido(Pedido pedido) {
        this.difundirEvento("tablero", "{\"evento\":\"completado\",\"id\":\"" + pedido.getId() + "\"}");
    }

    /**
     * Notificar que cambio el estado del conflicto de interbloqueo.
     */
    @Override
    public void alCambiarDeadlock() {
        this.difundirEvento("deadlock", "{\"cambio\":true}");
    }

    /**
     * Asignar el servicio de recepcion para el ingreso manual de mercaderia.
     *
     * @param recepcion servicio de recepcion con acceso al buffer.
     */
    public void setServicioRecepcion(ServicioRecepcion recepcion) {
        this.recepcion = recepcion;
    }

    /**
     * Asignar el registro de cuentas de clientes para los pedidos manuales.
     *
     * @param registroClientes registro concurrente de clientes.
     */
    public void setRegistroClientes(RegistroClientes registroClientes) {
        this.registroClientes = registroClientes;
    }

    // manejar solicitud de listado de clientes registrados
    private void manejadorListarClientes(Context ctx) {
        if (this.registroClientes == null) {
            ctx.status(503).contentType("application/json").result("{\"error\":\"Registro de clientes no disponible\"}");
            return;
        }
        // construir arreglo con las cuentas registradas
        List<Cliente> clientes = this.registroClientes.listarClientes();
        String elementos = "";
        for (int i = 0; i < clientes.size(); i++) {
            Cliente actual = clientes.get(i);
            String elemento = "{\"id\":\"" + actual.getId() + "\""
                + ",\"nombre\":\"" + actual.getNombre() + "\""
                + ",\"correo\":\"" + actual.getCorreo() + "\"}";
            if (i > 0) {
                elementos = elementos + ",";
            }
            elementos = elementos + elemento;
        }
        ctx.contentType("application/json").result("[" + elementos + "]");
    }

    // registrar manualmente una cuenta nueva de cliente
    private void manejadorRegistrarCliente(Context ctx) {
        if (this.registroClientes == null) {
            ctx.status(503).contentType("application/json").result("{\"error\":\"Registro de clientes no disponible\"}");
            return;
        }
        try {
            // parsear cuerpo JSON de la solicitud
            ObjectMapper mapper = new ObjectMapper();
            JsonNode cuerpo = mapper.readTree(ctx.body());
            String id = "";
            if (cuerpo.has("id")) {
                id = cuerpo.get("id").asText().trim();
            }
            String nombre = "";
            if (cuerpo.has("nombre")) {
                nombre = cuerpo.get("nombre").asText().trim();
            }
            String correo = "";
            if (cuerpo.has("correo")) {
                correo = cuerpo.get("correo").asText().trim();
            }
            if (id.equals("")) {
                ctx.status(400).contentType("application/json").result("{\"error\":\"Identificador de cliente requerido\"}");
                return;
            }
            if (nombre.equals("")) {
                ctx.status(400).contentType("application/json").result("{\"error\":\"Nombre de cliente requerido\"}");
                return;
            }
            Cliente nuevo = new Cliente(id, nombre, correo);
            boolean registrado = this.registroClientes.registrarCliente(nuevo);
            if (!registrado) {
                ctx.status(400).contentType("application/json").result("{\"error\":\"Identificador de cliente duplicado\"}");
                return;
            }
            ctx.contentType("application/json").result("{\"mensaje\":\"Cliente registrado exitosamente\",\"id\":\"" + id + "\"}");
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
