package com.redxela.service;

import com.redxela.model.Pedido;

import java.util.Comparator;

// comparador que implementa la politica de procesar primero pedidos con menor cantidad de articulos
public class ComparadorMenorCantidadArticulos implements Comparator<Pedido> {

    /**
     * Constructor por defecto del comparador por menor cantidad de articulos.
     */
    public ComparadorMenorCantidadArticulos() {
        // constructor vacio para instanciacion tradicional
    }

    /**
     * Comparar dos pedidos dando prioridad al que contiene menos unidades totales.
     *
     * @param pedido1 primer pedido a comparar.
     * @param pedido2 segundo pedido a comparar.
     * @return entero negativo si pedido1 tiene menos articulos positivo si tiene mas o cero si empatan.
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

        // obtener cantidades totales de articulos
        int cantidad1 = pedido1.getCantidadTotal();
        int cantidad2 = pedido2.getCantidadTotal();

        // comparar cantidades de articulos
        if (cantidad1 != cantidad2) {
            if (cantidad1 < cantidad2) {
                return -1;
            } else {
                return 1;
            }
        }

        // desempatar primero por nivel de servicio si las cantidades son identicas
        int prioridad1 = pedido1.getNivelServicio().getPrioridad();
        int prioridad2 = pedido2.getNivelServicio().getPrioridad();
        if (prioridad1 != prioridad2) {
            if (prioridad1 < prioridad2) {
                return -1;
            } else {
                return 1;
            }
        }

        // finalmente desempatar por tiempo de creacion en orden fifo
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
