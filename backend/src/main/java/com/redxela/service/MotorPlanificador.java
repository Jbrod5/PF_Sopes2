package com.redxela.service;

import com.redxela.concurrencia.GestorRecursos;
import com.redxela.concurrencia.ProcesoSimulacionPedido;
import com.redxela.model.EstadoPedido;
import com.redxela.model.Pedido;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

// motor despachador que extrae pedidos de la cola de prioridad y asigna hilos de ejecucion
public class MotorPlanificador implements Runnable {

    // cola de prioridad de donde se extraen los pedidos
    private final ColaPrioridadPedidos colaPedidos;

    // gestor de recursos compartidos para atender pedidos
    private final GestorRecursos gestorRecursos;

    // bandera de control para el ciclo de vida del hilo despachador
    private volatile boolean activo;

    // bandera para suspender la extraccion de pedidos
    private volatile boolean pausado;

    // lista de hilos de pedidos actualmente en ejecucion
    private final List<Thread> hilosActivos;

    // lista de tareas de pedidos actualmente en ejecucion
    private final List<ProcesoSimulacionPedido> tareasActivas;

    // cerrojo para proteger el registro de hilos activos
    private final ReentrantLock cerrojoHilos;

    // registro de metricas de atencion de pedidos
    private final RegistroEstadisticas estadisticas;

    // controlador de existencias para verificar y descontar stock
    private final ControladorInventario inventario;

    // oyente opcional de eventos para la interfaz en tiempo real
    private OyenteSimulacion oyente;

    // identificadores de pedidos ya descontados de existencias
    private final java.util.Set<String> pedidosDescontados;

    /**
     * Constructor sobrecargado con cola de pedidos y gestor de recursos.
     *
     * @param colaPedidos cola concurrida de pedidos.
     * @param gestorRecursos gestor de semaforos de recursos.
     */
    public MotorPlanificador(ColaPrioridadPedidos colaPedidos, GestorRecursos gestorRecursos) {
        this.colaPedidos = colaPedidos;
        this.gestorRecursos = gestorRecursos;
        this.activo = false;
        this.pausado = false;
        this.hilosActivos = new ArrayList<Thread>();
        this.tareasActivas = new ArrayList<ProcesoSimulacionPedido>();
        this.estadisticas = new RegistroEstadisticas();
        this.inventario = null;
        this.pedidosDescontados = new java.util.HashSet<String>();
        this.cerrojoHilos = new ReentrantLock(true);
    }

    /**
     * Constructor sobrecargado con controlador de inventario para validar existencias.
     *
     * @param colaPedidos cola concurrida de pedidos.
     * @param gestorRecursos gestor de semaforos de recursos.
     * @param inventario controlador de existencias del almacen.
     */
    public MotorPlanificador(ColaPrioridadPedidos colaPedidos, GestorRecursos gestorRecursos, ControladorInventario inventario) {
        this.colaPedidos = colaPedidos;
        this.gestorRecursos = gestorRecursos;
        this.activo = false;
        this.pausado = false;
        this.hilosActivos = new ArrayList<Thread>();
        this.tareasActivas = new ArrayList<ProcesoSimulacionPedido>();
        this.estadisticas = new RegistroEstadisticas();
        this.inventario = inventario;
        this.pedidosDescontados = new java.util.HashSet<String>();
        this.cerrojoHilos = new ReentrantLock(true);
    }

    /**
     * Iniciar el servicio de despacho de pedidos.
     */
    public void iniciar() {
        this.activo = true;
        this.pausado = false;
    }

    /**
     * Pausar el despacho de nuevos pedidos sin cancelar los ya iniciados.
     */
    public void pausar() {
        this.pausado = true;
        System.out.println("[PLANIFICADOR] Despacho de pedidos pausado");
    }

    /**
     * Asignar el oyente de eventos del simulador.
     *
     * @param oyente oyente a notificar en despacho y completado.
     */
    public void setOyente(OyenteSimulacion oyente) {
        this.oyente = oyente;
    }

    /**
     * Reanudar el despacho de pedidos.
     */
    public void reanudar() {
        this.pausado = false;
        System.out.println("[PLANIFICADOR] Despacho de pedidos reanudado");
    }

    /**
     * Detener el motor planificador.
     */
    public void detener() {
        this.activo = false;
    }

    /**
     * Ciclo principal del despachador que extrae pedidos de la cola y crea sus hilos de atencion.
     */
    @Override
    public void run() {
        this.activo = true;
        System.out.println("[PLANIFICADOR] Motor planificador iniciado");

        while (this.activo) {
            try {
                if (this.pausado) {
                    Thread.sleep(200L);
                    continue;
                }

                // seleccionar el siguiente pedido con existencias suficientes
                Pedido pedido = this.seleccionarPedidoConStock();
                if (pedido == null) {
                    // esperar sin consumir CPU cuando no hay pedido atendible
                    Thread.sleep(300L);
                    continue;
                }

                System.out.println("[PLANIFICADOR] Despachando pedido " + pedido.getId() + " (" + pedido.getNivelServicio().getNombre() + ", " + pedido.getCantidadTotal() + " articulos)");

                // crear la tarea del pedido
                ProcesoSimulacionPedido tarea = new ProcesoSimulacionPedido(pedido, this.gestorRecursos, this.estadisticas);
                Thread hiloPedido = new Thread(tarea, "Atencion-" + pedido.getId());

                // registrar y lanzar el hilo de procesamiento
                this.cerrojoHilos.lock();
                try {
                    this.hilosActivos.add(hiloPedido);
                    this.tareasActivas.add(tarea);
                } finally {
                    this.cerrojoHilos.unlock();
                }

                hiloPedido.start();

                // notificar a la interfaz que se despacho un pedido
                if (this.oyente != null) {
                    this.oyente.alDespacharPedido(pedido);
                }

                // limpiar referencias de hilos que ya hayan terminado
                this.limpiarHilosFinalizados();

            } catch (InterruptedException excepcion) {
                Thread.currentThread().interrupt();
                System.err.println("[PLANIFICADOR] Motor despachador interrumpido: " + excepcion.getMessage());
                break;
            }
        }

        System.out.println("[PLANIFICADOR] Motor planificador finalizado");
    }

    // seleccionar el primer pedido en espera con existencias suficientes
    private Pedido seleccionarPedidoConStock() {
        // obtener copia ordenada segun la politica activa
        List<Pedido> espera = this.colaPedidos.obtenerPedidosEnEspera();
        for (int i = 0; i < espera.size(); i++) {
            Pedido candidato = espera.get(i);
            if (this.hayStockSuficiente(candidato)) {
                // remover solo el elegido para no alterar el resto de la cola
                return this.colaPedidos.removerPedidoPorId(candidato.getId());
            }
        }
        return null;
    }

    // verificar que el inventario cubra todos los items del pedido
    private boolean hayStockSuficiente(Pedido pedido) {
        if (this.inventario == null) {
            return true;
        }
        List<com.redxela.model.ItemPedido> items = pedido.getListaItems();
        if (items.isEmpty()) {
            return true;
        }
        for (int i = 0; i < items.size(); i++) {
            com.redxela.model.ItemPedido actual = items.get(i);
            String idProducto = actual.getProducto().getId();
            int cantidad = actual.getCantidad();
            if (!this.inventario.tieneStockSuficiente(idProducto, cantidad)) {
                return false;
            }
        }
        return true;
    }

    // descontar del inventario los items de un pedido completado
    private void descontarStockPedido(Pedido pedido) {
        if (this.inventario == null) {
            return;
        }
        List<com.redxela.model.ItemPedido> items = pedido.getListaItems();
        for (int i = 0; i < items.size(); i++) {
            com.redxela.model.ItemPedido actual = items.get(i);
            this.inventario.descontarStock(actual.getProducto().getId(), actual.getCantidad());
        }
    }

    // remover de la lista aquellos hilos que completaron su ejecucion
    private void limpiarHilosFinalizados() {
        this.cerrojoHilos.lock();
        try {
            for (int i = this.hilosActivos.size() - 1; i >= 0; i--) {
                Thread h = this.hilosActivos.get(i);
                if (!h.isAlive()) {
                    ProcesoSimulacionPedido tarea = this.tareasActivas.get(i);
                    Pedido terminado = tarea.getPedido();
                    // descontar existencias una sola vez por pedido completado
                    if (terminado.getEstado() == EstadoPedido.COMPLETADO) {
                        if (!this.pedidosDescontados.contains(terminado.getId())) {
                            this.descontarStockPedido(terminado);
                            this.pedidosDescontados.add(terminado.getId());
                        }
                        // notificar a la interfaz que se completo un pedido
                        if (this.oyente != null) {
                            this.oyente.alCompletarPedido(terminado);
                        }
                    } else if (terminado.getEstado() == EstadoPedido.CANCELADO) {
                        // reencolar cancelados con tope de reintentos para no perderlos
                        if (terminado.getReintentos() < Pedido.MAX_REINTENTOS) {
                            terminado.registrarReintento();
                            this.colaPedidos.encolarPedido(terminado);
                            System.out.println("[PLANIFICADOR] Pedido " + terminado.getId() + " reencolado tras cancelacion (intento " + terminado.getReintentos() + ")");
                        } else {
                            System.err.println("[PLANIFICADOR] Pedido " + terminado.getId() + " descartado tras agotar reintentos");
                        }
                    }
                    this.hilosActivos.remove(i);
                    this.tareasActivas.remove(i);
                }
            }
        } finally {
            this.cerrojoHilos.unlock();
        }
    }

    /**
     * Esperar la finalizacion de todos los hilos actualmente en ejecucion.
     */
    public void esperarHilosActivos() {
        List<Thread> copiaHilos = new ArrayList<Thread>();
        this.cerrojoHilos.lock();
        try {
            for (int i = 0; i < this.hilosActivos.size(); i++) {
                copiaHilos.add(this.hilosActivos.get(i));
            }
        } finally {
            this.cerrojoHilos.unlock();
        }

        for (int i = 0; i < copiaHilos.size(); i++) {
            try {
                copiaHilos.get(i).join();
            } catch (InterruptedException excepcion) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Consultar la cantidad de hilos que se encuentran procesando pedidos activamente.
     *
     * @return cantidad de pedidos en ejecucion simultanea.
     */
    public int getCantidadHilosActivos() {
        this.limpiarHilosFinalizados();
        this.cerrojoHilos.lock();
        try {
            return this.hilosActivos.size();
        } finally {
            this.cerrojoHilos.unlock();
        }
    }

    /**
     * Obtener el registro de metricas de atencion del motor.
     *
     * @return instancia del registro de estadisticas.
     */
    public RegistroEstadisticas getEstadisticas() {
        return this.estadisticas;
    }

    /**
     * Obtener una copia de las tareas que se encuentran procesando pedidos activamente.
     *
     * @return lista de tareas en ejecucion simultanea.
     */
    public List<ProcesoSimulacionPedido> obtenerTareasActivas() {
        this.limpiarHilosFinalizados();
        this.cerrojoHilos.lock();
        try {
            List<ProcesoSimulacionPedido> copia = new ArrayList<ProcesoSimulacionPedido>();
            for (int i = 0; i < this.tareasActivas.size(); i++) {
                copia.add(this.tareasActivas.get(i));
            }
            return copia;
        } finally {
            this.cerrojoHilos.unlock();
        }
    }
}
