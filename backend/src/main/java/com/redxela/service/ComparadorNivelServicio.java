package com.redxela.service;

import com.redxela.model.Pedido;

import java.util.Comparator;

// comparador que implementa la politica de ordenamiento por nivel de servicio estricto
public class ComparadorNivelServicio implements Comparator<Pedido> {

    /**
     * Constructor por defecto del comparador de nivel de servicio.
     */
    public ComparadorNivelServicio() {
        // constructor vacio para instanciacion tradicional
    }

    /**
     * Comparar dos pedidos dando prioridad al menor valor numerico de nivel de servicio.
     *
     * @param pedido1 primer pedido a comparar.
     * @param pedido2 segundo pedido a comparar.
     * @return entero negativo si pedido1 tiene mayor prioridad positivo si menor o cero si empatan.
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

        // obtener la prioridad numerica donde 1 es expres y 5 economico
        int prioridad1 = pedido1.getNivelServicio().getPrioridad();
        int prioridad2 = pedido2.getNivelServicio().getPrioridad();

        // verificar si tienen distinta prioridad
        if (prioridad1 != prioridad2) {
            if (prioridad1 < prioridad2) {
                return -1;
            } else {
                return 1;
            }
        }

        // en caso de empate desempatar por el tiempo de creacion en orden fifo
        long tiempo1 = pedido1.getTiempoCreacionMs();
        long tiempo2 = pedido2.getTiempoCreacionMs();
        if (tiempo1 < tiempo2) {
            return -1;
        } else if (tiempo1 > tiempo2) {
            return 1;
        } else {
            return 0;
        }
    }
}
