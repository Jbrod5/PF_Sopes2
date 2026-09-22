package com.redxela.service;

import com.redxela.model.LoteMercancia;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

// buffer acotado concurrente basado en el patron productor consumidor con semaforos y cerrojo
public class BufferRecepcion {

    // capacidad maxima de elementos que puede albergar el buffer
    private final int capacidad;

    // semaforo que contabiliza los espacios libres disponibles para producir
    private final Semaphore semaforoEspacios;

    // semaforo que contabiliza los elementos disponibles listos para consumir
    private final Semaphore semaforoElementos;

    // cerrojo de exclusion mutua para proteger la seccion critica de insercion y extraccion
    private final ReentrantLock cerrojoMutex;

    // lista interna que almacena los lotes en transito
    private final List<LoteMercancia> colaLotes;

    /**
     * Constructor por defecto con capacidad predeterminada de diez lotes.
     */
    public BufferRecepcion() {
        this.capacidad = 10;
        this.semaforoEspacios = new Semaphore(10, true);
        this.semaforoElementos = new Semaphore(0, true);
        this.cerrojoMutex = new ReentrantLock(true);
        this.colaLotes = new ArrayList<LoteMercancia>();
    }

    /**
     * Constructor sobrecargado para configurar la capacidad maxima del buffer acotado.
     *
     * @param capacidad cantidad maxima de lotes simultaneos en el buffer.
     */
    public BufferRecepcion(int capacidad) {
        this.capacidad = capacidad;
        this.semaforoEspacios = new Semaphore(capacidad, true);
        this.semaforoElementos = new Semaphore(0, true);
        this.cerrojoMutex = new ReentrantLock(true);
        this.colaLotes = new ArrayList<LoteMercancia>();
    }

    /**
     * Enviar un lote de mercaderia al buffer bloqueando si el buffer esta lleno.
     *
     * @param lote lote entrante remitido desde recepcion.
     * @return verdadero si el lote fue depositado exitosamente o falso en interrupcion.
     */
    public boolean enviarLote(LoteMercancia lote) {
        if (lote == null) {
            return false;
        }

        try {
            // solicitar un espacio libre bloqueando el productor si la capacidad esta colmada
            this.semaforoEspacios.acquire();

            // acceder a la seccion critica con cerrojo de exclusion mutua
            this.cerrojoMutex.lock();
            try {
                this.colaLotes.add(lote);
            } finally {
                this.cerrojoMutex.unlock();
            }

            // senalizar al consumidor incrementando el semaforo de elementos listos
            this.semaforoElementos.release();
            return true;

        } catch (InterruptedException excepcion) {
            Thread.currentThread().interrupt();
            System.err.println("Interrupcion al enviar lote a buffer: " + excepcion.getMessage());
            return false;
        }
    }

    /**
     * Intentar enviar un lote con tiempo limite de espera antes de desistir.
     *
     * @param lote lote a enviar.
     * @param tiempoEspera tiempo maximo de espera.
     * @param unidad unidad de tiempo aplicada.
     * @return verdadero si logro depositarlo o falso si el tiempo expiro.
     */
    public boolean intentarEnviarLote(LoteMercancia lote, long tiempoEspera, TimeUnit unidad) {
        if (lote == null) {
            return false;
        }

        try {
            boolean espacioObtenido = this.semaforoEspacios.tryAcquire(tiempoEspera, unidad);
            if (!espacioObtenido) {
                return false;
            }

            this.cerrojoMutex.lock();
            try {
                this.colaLotes.add(lote);
            } finally {
                this.cerrojoMutex.unlock();
            }

            this.semaforoElementos.release();
            return true;

        } catch (InterruptedException excepcion) {
            Thread.currentThread().interrupt();
            System.err.println("Interrupcion al intentar enviar lote: " + excepcion.getMessage());
            return false;
        }
    }

    /**
     * Recibir y remover un lote del buffer bloqueando si se encuentra vacio.
     *
     * @return lote extraido del buffer o null en caso de interrupcion.
     */
    public LoteMercancia recibirLote() {
        try {
            // solicitar un elemento disponible bloqueando al consumidor si no hay datos
            this.semaforoElementos.acquire();

            LoteMercancia loteSeleccionado;

            // seccion critica para extraer el primer lote encolado
            this.cerrojoMutex.lock();
            try {
                loteSeleccionado = this.colaLotes.remove(0);
            } finally {
                this.cerrojoMutex.unlock();
            }

            // senalizar al productor devolviendo un espacio libre al semaforo
            this.semaforoEspacios.release();
            return loteSeleccionado;

        } catch (InterruptedException excepcion) {
            Thread.currentThread().interrupt();
            System.err.println("Interrupcion al recibir lote de buffer: " + excepcion.getMessage());
            return null;
        }
    }

    /**
     * Recibir un lote del buffer con tiempo maximo de espera.
     *
     * @param tiempoEspera tiempo maximo a aguardar.
     * @param unidad unidad temporal.
     * @return lote extraido o null si expiro el tiempo.
     */
    public LoteMercancia recibirLoteConTiempo(long tiempoEspera, TimeUnit unidad) {
        try {
            boolean elementoObtenido = this.semaforoElementos.tryAcquire(tiempoEspera, unidad);
            if (!elementoObtenido) {
                return null;
            }

            LoteMercancia loteSeleccionado;

            this.cerrojoMutex.lock();
            try {
                loteSeleccionado = this.colaLotes.remove(0);
            } finally {
                this.cerrojoMutex.unlock();
            }

            this.semaforoEspacios.release();
            return loteSeleccionado;

        } catch (InterruptedException excepcion) {
            Thread.currentThread().interrupt();
            System.err.println("Interrupcion al recibir lote con tiempo: " + excepcion.getMessage());
            return null;
        }
    }

    /**
     * Consultar la capacidad maxima del buffer acotado.
     *
     * @return capacidad total.
     */
    public int getCapacidad() {
        return this.capacidad;
    }

    /**
     * Consultar la cantidad de lotes almacenados en el buffer en este instante.
     *
     * @return numero de lotes en espera en el buffer.
     */
    public int getCantidadElementos() {
        this.cerrojoMutex.lock();
        try {
            return this.colaLotes.size();
        } finally {
            this.cerrojoMutex.unlock();
        }
    }

    /**
     * Consultar si el buffer acotado se encuentra completamente vacio.
     *
     * @return verdadero si esta vacio o falso si contiene lotes.
     */
    public boolean estaVacio() {
        this.cerrojoMutex.lock();
        try {
            return this.colaLotes.isEmpty();
        } finally {
            this.cerrojoMutex.unlock();
        }
    }
}
