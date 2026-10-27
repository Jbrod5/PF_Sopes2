package com.redxela.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

// registro concurrente de eventos de la simulacion para la bitacora visible
public class RegistroEventos {

    // lista circular de mensajes en orden de llegada
    private final List<String> eventos;

    // cantidad maxima de mensajes conservados
    private final int capacidadMaxima;

    // cerrojo para garantizar exclusion mutua en el registro
    private final ReentrantLock cerrojo;

    /**
     * Constructor por defecto con capacidad de quinientos eventos.
     */
    public RegistroEventos() {
        this.eventos = new ArrayList<String>();
        this.capacidadMaxima = 500;
        this.cerrojo = new ReentrantLock(true);
    }

    /**
     * Constructor sobrecargado con capacidad personalizada.
     *
     * @param capacidadMaxima cantidad maxima de eventos a conservar.
     */
    public RegistroEventos(int capacidadMaxima) {
        this.eventos = new ArrayList<String>();
        int tope = capacidadMaxima;
        if (tope <= 0) {
            tope = 500;
        }
        this.capacidadMaxima = tope;
        this.cerrojo = new ReentrantLock(true);
    }

    /**
     * Anotar un evento nuevo descartando los mas antiguos al llenar el tope.
     *
     * @param evento texto del evento a registrar.
     */
    public void anotar(String evento) {
        if (evento == null) {
            return;
        }
        // adquirir el cerrojo para anotar con exclusion mutua
        this.cerrojo.lock();
        try {
            this.eventos.add(evento);
            while (this.eventos.size() > this.capacidadMaxima) {
                this.eventos.remove(0);
            }
        } finally {
            // liberar el cerrojo de forma segura
            this.cerrojo.unlock();
        }
    }

    /**
     * Obtener una copia de todos los eventos registrados en orden.
     *
     * @return lista con los eventos.
     */
    public List<String> obtenerEventos() {
        // adquirir el cerrojo para copiar con exclusion mutua
        this.cerrojo.lock();
        try {
            List<String> copia = new ArrayList<String>();
            for (int i = 0; i < this.eventos.size(); i++) {
                copia.add(this.eventos.get(i));
            }
            return copia;
        } finally {
            // liberar el cerrojo de forma segura
            this.cerrojo.unlock();
        }
    }

    /**
     * Obtener una copia de los ultimos eventos registrados.
     *
     * @param cantidad cantidad maxima a devolver.
     * @return lista con los eventos mas recientes.
     */
    public List<String> obtenerUltimos(int cantidad) {
        // adquirir el cerrojo para copiar con exclusion mutua
        this.cerrojo.lock();
        try {
            List<String> copia = new ArrayList<String>();
            int inicio = this.eventos.size() - cantidad;
            if (inicio < 0) {
                inicio = 0;
            }
            for (int i = inicio; i < this.eventos.size(); i++) {
                copia.add(this.eventos.get(i));
            }
            return copia;
        } finally {
            // liberar el cerrojo de forma segura
            this.cerrojo.unlock();
        }
    }

    /**
     * Consultar la cantidad de eventos registrados.
     *
     * @return numero de eventos conservados.
     */
    public int getCantidad() {
        // adquirir el cerrojo para contar con exclusion mutua
        this.cerrojo.lock();
        try {
            return this.eventos.size();
        } finally {
            // liberar el cerrojo de forma segura
            this.cerrojo.unlock();
        }
    }
}
