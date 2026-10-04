package com.redxela.controller;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.staticfiles.Location;

import com.redxela.service.ColaPrioridadPedidos;
import com.redxela.service.PoliticaPlanificacion;
import com.redxela.concurrencia.GestorRecursos;
import com.redxela.concurrencia.deadlock.SimuladorDeadlock;
import com.redxela.concurrencia.deadlock.InformacionConflicto;
import com.redxela.almacen.AlmacenPaginado;
import com.redxela.model.Pedido;
import com.redxela.model.EstadoPedido;
import com.redxela.model.Cliente;

import java.util.HashMap;
import java.util.Map;

// servidor javalin que expone rutas para el modulo 3 y el sistema completo
public class ControladorServidor {

    // instancia del servidor javalin
    private final Javalin app;

    // referencia a los servicios compartidos
    private final ColaPrioridadPedidos cola;
    private final GestorRecursos gestor;
    private final AlmacenPaginado almacen;
    private final SimuladorDeadlock simulador;

    /**
     * Constructor sobrecargado que inicializa el servidor con los servicios existentes.
     *
     * @param cola cola de pedidos activa.
     * @param gestor gestor de recursos compartidos.
     * @param almacen almacen paginado del centro.
     */
    public ControladorServidor(ColaPrioridadPedidos cola, GestorRecursos gestor, AlmacenPaginado almacen) {
        // inicializar referencias compartidas
        this.cola = cola;
        this.gestor = gestor;
        this.almacen = almacen;
        this.simulador = new SimuladorDeadlock(gestor);
        // crear la instancia de javalin en el puerto 7070
        this.app = Javalin.create().start(7070);
        // configurar rutas disponibles del servidor
        this.configurarRutas();
    }

    // configurar las rutas disponibles en el servidor javalin
    private void configurarRutas() {

        // linea en blanco tras apertura de bloque
        this.app.get("/api/estado", this::manejadorEstado);
        this.app.post("/api/pedidos", this::manejadorRegistrarPedido);
        this.app.post("/api/politica", this::manejadorCambiarPolitica);
        this.app.post("/api/deadlock/inducir", this::manejadorInducirDeadlock);
        this.app.post("/api/deadlock/resolver", this::manejadorResolverDeadlock);
        this.app.ws("/ws/simulacion", ws -> {
            ws.onMessage(ctx -> {
                // reenviar evento a todos los clientes conectados
            });
        });

        // configurar servicio de archivos estaticos del frontend compilado
        this.app.addStaticFiles("/home/jorge/Sopes2/PF/PF_Sopes2/frontend/dist", Location.EXTERNAL);
        // configurar ruta raiz para entregar index.html compilado
        this.app.addSinglePageRoot("/", "/home/jorge/Sopes2/PF/PF_Sopes2/frontend/dist/index.html", Location.EXTERNAL);

        // linea en blanco antes de cierre de bloque
    }

    // manejar solicitud de estado general del sistema
    private void manejadorEstado(Context ctx) {
        // construir respuesta con concatenacion de cadenas
        String respuesta = "Estado: recursos=" + gestor.consultarDisponibles(com.redxela.model.TipoRecurso.MONTACARGAS);
        ctx.result(respuesta);
    }

    // registrar pedido manual recibido por post
    private void manejadorRegistrarPedido(Context ctx) {
        // crear pedido basico para demostracion
        Pedido nuevo = new Pedido("MAN-001", new Cliente(), com.redxela.model.NivelServicio.ESTANDAR);
        cola.encolarPedido(nuevo);
        ctx.result("Pedido registrado: " + nuevo.getId());
    }

    // cambiar la politica de planificacion activa
    private void manejadorCambiarPolitica(Context ctx) {
        cola.cambiarPolitica(PoliticaPlanificacion.NIVEL_SERVICIO_ESTRICTO);
        ctx.result("Politica actualizada");
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
        boolean resuelto = simulador.resolverConflictoManual("DL-A");
        if (resuelto) {
            ctx.result("Conflicto resuelto manualmente");
        } else {
            ctx.result("No hay conflicto activo para resolver");
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
