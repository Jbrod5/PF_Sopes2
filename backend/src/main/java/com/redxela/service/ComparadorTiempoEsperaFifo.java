package com.redxela.service;

import com.redxela.model.Pedido;

import java.util.Comparator;

// comparador que implementa la politica fifo estricta segun el tiempo de llegada del pedido
public class ComparadorTiempoEsperaFifo implements Comparator<Pedido> {

    /**
     * Constructor por defecto del comparador por orden de llegada.
     */
    public ComparadorTiempoEsperaFifo() {
        // constructor vacio para instanciacion tradicional
    }

    /**
     * Comparar dos pedidos dando prioridad al que fue creado antes.
     *
     * @param pedido1 primer pedido a comparar.
     * @param pedido2 segundo pedido a comparar.
     * @return entero negativo si pedido1 llego antes positivo si llego despues o cero si empatan.
     */
    @Override
    public int compare(Pedido pedido1, Pedido pedido2) {
        if (pedido1 == null && pedido2 == null) {
            return 0;
        }
        if (pedido1 == null) {
            return 1;
        }
        if (pedido2 == null) {
            return -1;
        }

        // obtener marcas de tiempo de creacion
        long tiempo1 = pedido1.getTiempoCreacionMs();
        long tiempo2 = pedido2.getTiempoCreacionMs();

        // comparar marcas temporales
        if (tiempo1 < tiempo2) {
            return -1;
        } else if (tiempo1 > tiempo2) {
            return 1;
        }

        // desempatar por nivel de servicio si la marca temporal es identica
        int prioridad1 = pedido1.getNivelServicio().getPrioridad();
        int prioridad2 = pedido2.getNivelServicio().getPrioridad();
        if (prioridad1 < prioridad2) {
            return -1;
        } else if (prioridad1 > prioridad2) {
            return 1;
        } else {
            return 0;
        }
    }
}
