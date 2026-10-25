package com.redxela.service;

import java.util.concurrent.locks.ReentrantLock;

// registro de metricas de atencion de pedidos para estadisticas del sistema
public class RegistroEstadisticas {

    // cerrojo para proteger los contadores en entornos multihilo
    private final ReentrantLock cerrojo;

    // cantidad de pedidos que completaron su procesamiento
    private int pedidosCompletados;

    // suma acumulada de tiempos de espera de pedidos completados
    private long sumaEsperaMs;

    // suma acumulada de tiempos de procesamiento de pedidos completados
    private long sumaProcesoMs;

    /**
     * Constructor por defecto del registro de estadisticas.
     */
    public RegistroEstadisticas() {
        this.cerrojo = new ReentrantLock(true);
        this.pedidosCompletados = 0;
        this.sumaEsperaMs = 0L;
        this.sumaProcesoMs = 0L;
    }

    /**
     * Registrar la finalizacion de un pedido con sus tiempos medidos.
     *
     * @param esperaMs tiempo que el pedido espero recursos en milisegundos.
     * @param procesoMs tiempo que el pedido uso recursos en milisegundos.
     */
    public void registrarPedidoCompletado(long esperaMs, long procesoMs) {
        this.cerrojo.lock();
        try {
            this.pedidosCompletados = this.pedidosCompletados + 1;
            this.sumaEsperaMs = this.sumaEsperaMs + esperaMs;
            this.sumaProcesoMs = this.sumaProcesoMs + procesoMs;
        } finally {
            this.cerrojo.unlock();
        }
    }

    /**
     * Obtener la cantidad de pedidos completados registrados.
     *
     * @return numero de pedidos completados.
     */
    public int obtenerCompletados() {
        this.cerrojo.lock();
        try {
            return this.pedidosCompletados;
        } finally {
            this.cerrojo.unlock();
        }
    }

    /**
     * Obtener el tiempo promedio de espera en milisegundos.
     *
     * @return promedio de espera o cero si no hay registros.
     */
    public long obtenerEsperaPromedioMs() {
        this.cerrojo.lock();
        try {
            if (this.pedidosCompletados == 0) {
                return 0L;
            }
            return this.sumaEsperaMs / this.pedidosCompletados;
        } finally {
            this.cerrojo.unlock();
        }
    }

    /**
     * Obtener el tiempo promedio de procesamiento en milisegundos.
     *
     * @return promedio de proceso o cero si no hay registros.
     */
    public long obtenerProcesoPromedioMs() {
        this.cerrojo.lock();
        try {
            if (this.pedidosCompletados == 0) {
                return 0L;
            }
            return this.sumaProcesoMs / this.pedidosCompletados;
        } finally {
            this.cerrojo.unlock();
        }
    }
}
