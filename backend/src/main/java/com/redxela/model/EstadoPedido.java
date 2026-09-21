package com.redxela.model;

// enumeracion para representar los posibles estados por los que atraviesa un pedido
public enum EstadoPedido {

    RECIBIDO("Recibido en el sistema"),
    EN_ESPERA("En lista de espera por recursos"),
    PROCESANDO("En procesamiento activo"),
    EN_CONFLICTO("En interbloqueo o contencion de recursos"),
    COMPLETADO("Completado satisfactoriamente"),
    CANCELADO("Cancelado o desapropiado");

    // descripcion textual del estado del pedido
    private final String descripcion;

    // constructor del enum
    EstadoPedido(String descripcion) {
        this.descripcion = descripcion;
    }

    /**
     * Obtener la descripcion del estado.
     *
     * @return texto descriptivo del estado actual.
     */
    public String getDescripcion() {
        return this.descripcion;
    }
}
