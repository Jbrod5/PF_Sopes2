package com.redxela.service;

import com.redxela.model.EstadoPedido;
import com.redxela.model.Pedido;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

// cola de prioridad concurrente y bloqueante para administrar pedidos segun politicas dinamicas
public class ColaPrioridadPedidos {

    // cerrojo para garantizar exclusion mutua en todas las operaciones sobre la cola
    private final ReentrantLock cerrojo;

    // condicion para suspender hilos consumidores cuando la cola este vacia
    private final Condition noVacia;

    // lista interna que almacena los pedidos ordenados segun la politica activa
    private final List<Pedido> listaPedidos;

    // politica de planificacion actualmente configurada
    private PoliticaPlanificacion politicaActual;

    // comparador activo correspondiente a la politica
    private Comparator<Pedido> comparadorActivo;

    /**
     * Constructor por defecto que inicializa la cola con la politica de nivel de servicio estricto.
     */
    public ColaPrioridadPedidos() {
        // inicializar mecanismos de sincronizacion
        this.cerrojo = new ReentrantLock(true);
        this.noVacia = this.cerrojo.newCondition();
        this.listaPedidos = new ArrayList<Pedido>();

        // establecer politica inicial por defecto
        this.politicaActual = PoliticaPlanificacion.NIVEL_SERVICIO_ESTRICTO;
        this.comparadorActivo = new ComparadorNivelServicio();
    }

    /**
     * Constructor sobrecargado que permite especificar la politica de planificacion inicial.
     *
     * @param politicaInicial politica con la que iniciara la cola.
     */
    public ColaPrioridadPedidos(PoliticaPlanificacion politicaInicial) {
        this.cerrojo = new ReentrantLock(true);
        this.noVacia = this.cerrojo.newCondition();
        this.listaPedidos = new ArrayList<Pedido>();

        this.politicaActual = politicaInicial;
        this.asignarComparadorPorPolitica(politicaInicial);
    }

    // asignar el comparador adecuado segun la politica indicada
    private void asignarComparadorPorPolitica(PoliticaPlanificacion politica) {
        if (politica == PoliticaPlanificacion.NIVEL_SERVICIO_ESTRICTO) {
            this.comparadorActivo = new ComparadorNivelServicio();
        } else if (politica == PoliticaPlanificacion.MENOR_CANTIDAD_ARTICULOS) {
            this.comparadorActivo = new ComparadorMenorCantidadArticulos();
        } else if (politica == PoliticaPlanificacion.TIEMPO_ESPERA_FIFO) {
            this.comparadorActivo = new ComparadorTiempoEsperaFifo();
        } else {
            this.comparadorActivo = new ComparadorNivelServicio();
        }
    }

    /**
     * Encolar un nuevo pedido en la estructura y reordenar segun la politica activa.
     *
     * @param pedido pedido a ingresar a la cola.
     */
    public void encolarPedido(Pedido pedido) {
        if (pedido == null) {
            return;
        }

        // adquirir el cerrojo para modificar la lista con exclusion mutua
        this.cerrojo.lock();
        try {
            // marcar el estado del pedido como en espera
            pedido.setEstado(EstadoPedido.EN_ESPERA);

            // anadir el pedido a la lista
            this.listaPedidos.add(pedido);

            // ordenar la lista segun el comparador activo
            Collections.sort(this.listaPedidos, this.comparadorActivo);

            // notificar a los hilos esperando que hay al menos un elemento disponible
            this.noVacia.signal();
        } finally {
            // liberar el cerrojo de forma segura
            this.cerrojo.unlock();
        }
    }

    /**
     * Desencolar de forma bloqueante el pedido de mayor prioridad segun la politica activa.
     *
     * @return pedido con mayor prioridad o null si fue interrumpido.
     */
    public Pedido desencolarPedido() {
        this.cerrojo.lock();
        try {
            // esperar mientras la cola no tenga pedidos
            while (this.listaPedidos.isEmpty()) {
                this.noVacia.await();
            }

            // extraer y remover el primer elemento correspondiente a la maxima prioridad
            Pedido seleccionado = this.listaPedidos.remove(0);
            return seleccionado;
        } catch (InterruptedException excepcion) {
            Thread.currentThread().interrupt();
            System.err.println("Interrupcion al desencolar pedido: " + excepcion.getMessage());
            return null;
        } finally {
            this.cerrojo.unlock();
        }
    }

    /**
     * Desencolar con tiempo limite de espera el siguiente pedido de la cola.
     *
     * @param tiempoEspera tiempo maximo a aguardar.
     * @param unidad unidad de tiempo aplicada.
     * @return pedido seleccionado o null si expiro el tiempo limite.
     */
    public Pedido desencolarPedidoConTiempo(long tiempoEspera, TimeUnit unidad) {
        this.cerrojo.lock();
        try {
            long nanosegundosRestantes = unidad.toNanos(tiempoEspera);
            while (this.listaPedidos.isEmpty()) {
                if (nanosegundosRestantes <= 0L) {
                    return null;
                }
                nanosegundosRestantes = this.noVacia.awaitNanos(nanosegundosRestantes);
            }

            // extraer el pedido con mayor prioridad
            Pedido seleccionado = this.listaPedidos.remove(0);
            return seleccionado;
        } catch (InterruptedException excepcion) {
            Thread.currentThread().interrupt();
            System.err.println("Interrupcion en desencolado con tiempo: " + excepcion.getMessage());
            return null;
        } finally {
            this.cerrojo.unlock();
        }
    }

    /**
     * Cambiar dinamicamente la politica de planificacion y reordenar los pedidos en espera.
     *
     * @param nuevaPolitica nueva politica a aplicar.
     */
    public void cambiarPolitica(PoliticaPlanificacion nuevaPolitica) {
        if (nuevaPolitica == null) {
            return;
        }

        this.cerrojo.lock();
        try {
            // actualizar la politica actual y su comparador asociado
            this.politicaActual = nuevaPolitica;
            this.asignarComparadorPorPolitica(nuevaPolitica);

            // reordenar todos los pedidos existentes con el nuevo criterio
            Collections.sort(this.listaPedidos, this.comparadorActivo);

            System.out.println("[PLANIFICADOR] Politica cambiada dinamicamente a: " + nuevaPolitica.getNombre());
        } finally {
            this.cerrojo.unlock();
        }
    }

    /**
     * Obtener una copia de la lista de pedidos actualmente en espera.
     *
     * @return lista con los pedidos en cola.
     */
    public List<Pedido> obtenerPedidosEnEspera() {
        this.cerrojo.lock();
        try {
            // crear nueva lista para no exponer la estructura interna
            List<Pedido> copia = new ArrayList<Pedido>();
            for (int i = 0; i < this.listaPedidos.size(); i++) {
                copia.add(this.listaPedidos.get(i));
            }
            return copia;
        } finally {
            this.cerrojo.unlock();
        }
    }

    /**
     * Consultar la cantidad de pedidos que se encuentran actualmente en la cola de espera.
     *
     * @return numero de pedidos en espera.
     */
    public int getCantidadEnEspera() {
        this.cerrojo.lock();
        try {
            return this.listaPedidos.size();
        } finally {
            this.cerrojo.unlock();
        }
    }

    /**
     * Verificar si la cola se encuentra vacia sin ningun pedido en espera.
     *
     * @return verdadero si esta vacia o falso si contiene elementos.
     */
    public boolean estaVacia() {
        this.cerrojo.lock();
        try {
            return this.listaPedidos.isEmpty();
        } finally {
            this.cerrojo.unlock();
        }
    }

    /**
     * Obtener la politica de planificacion actualmente configurada.
     *
     * @return politica de planificacion activa.
     */
    public PoliticaPlanificacion getPoliticaActual() {
        this.cerrojo.lock();
        try {
            return this.politicaActual;
        } finally {
            this.cerrojo.unlock();
        }
    }
}
