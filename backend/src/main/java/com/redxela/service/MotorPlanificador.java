package com.redxela.service;

import com.redxela.concurrencia.GestorRecursos;
import com.redxela.concurrencia.ProcesoSimulacionPedido;
import com.redxela.model.Pedido;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
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

    // cerrojo para proteger el registro de hilos activos
    private final ReentrantLock cerrojoHilos;

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

                // intentar extraer el siguiente pedido con prioridad alta
                Pedido pedido = this.colaPedidos.desencolarPedidoConTiempo(500L, TimeUnit.MILLISECONDS);
                if (pedido != null) {
                    System.out.println("[PLANIFICADOR] Despachando pedido " + pedido.getId() + " (" + pedido.getNivelServicio().getNombre() + ", " + pedido.getCantidadTotal() + " articulos)");

                    // crear la tarea del pedido
                    ProcesoSimulacionPedido tarea = new ProcesoSimulacionPedido(pedido, this.gestorRecursos);
                    Thread hiloPedido = new Thread(tarea, "Atencion-" + pedido.getId());

                    // registrar y lanzar el hilo de procesamiento
                    this.cerrojoHilos.lock();
                    try {
                        this.hilosActivos.add(hiloPedido);
                    } finally {
                        this.cerrojoHilos.unlock();
                    }

                    hiloPedido.start();
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

    // remover de la lista aquellos hilos que completaron su ejecucion
    private void limpiarHilosFinalizados() {
        this.cerrojoHilos.lock();
        try {
            for (int i = this.hilosActivos.size() - 1; i >= 0; i--) {
                Thread h = this.hilosActivos.get(i);
                if (!h.isAlive()) {
                    this.hilosActivos.remove(i);
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
}
