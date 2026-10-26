package com.redxela.service;

import com.redxela.model.Pedido;

// oyente de eventos del simulador para notificar a la interfaz en tiempo real
public interface OyenteSimulacion {

    /**
     * Notificar que un pedido fue despachado a procesamiento.
     *
     * @param pedido pedido despachado.
     */
    void alDespacharPedido(Pedido pedido);

    /**
     * Notificar que un pedido completo su procesamiento.
     *
     * @param pedido pedido completado.
     */
    void alCompletarPedido(Pedido pedido);

    /**
     * Notificar que cambio el estado del conflicto de interbloqueo.
     */
    void alCambiarDeadlock();
}
