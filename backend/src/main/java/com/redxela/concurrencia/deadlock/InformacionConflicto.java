package com.redxela.concurrencia.deadlock;

import com.redxela.model.TipoRecurso;

// clase que encapsula la informacion descriptiva de un interbloqueo detectado
public class InformacionConflicto {

    // identificador del primer pedido en conflicto
    private String idPedido1;

    // recurso actualmente retenido por el primer pedido
    private TipoRecurso recursoRetenido1;

    // recurso que el primer pedido esta esperando
    private TipoRecurso recursoSolicitado1;

    // identificador del segundo pedido en conflicto
    private String idPedido2;

    // recurso actualmente retenido por el segundo pedido
    private TipoRecurso recursoRetenido2;

    // recurso que el segundo pedido esta esperando
    private TipoRecurso recursoSolicitado2;

    // indicador de resolucion del conflicto
    private boolean resuelto;

    // identificador del pedido seleccionado manualmente para continuar
    private String idPedidoGanador;

    // explicacion detallada del motivo del bloqueo
    private String causaBloqueo;

    /**
     * Constructor por defecto de la informacion de conflicto.
     */
    public InformacionConflicto() {
        this.idPedido1 = "";
        this.idPedido2 = "";
        this.resuelto = false;
        this.idPedidoGanador = "";
        this.causaBloqueo = "";
    }

    /**
     * Constructor sobrecargado con los pedidos y recursos involucrados en la espera circular.
     *
     * @param idPedido1 identificador del primer pedido.
     * @param recursoRetenido1 recurso retenido por el primer pedido.
     * @param recursoSolicitado1 recurso esperado por el primer pedido.
     * @param idPedido2 identificador del segundo pedido.
     * @param recursoRetenido2 recurso retenido por el segundo pedido.
     * @param recursoSolicitado2 recurso esperado por el segundo pedido.
     */
    public InformacionConflicto(
            String idPedido1,
            TipoRecurso recursoRetenido1,
            TipoRecurso recursoSolicitado1,
            String idPedido2,
            TipoRecurso recursoRetenido2,
            TipoRecurso recursoSolicitado2
    ) {
        this.idPedido1 = idPedido1;
        this.recursoRetenido1 = recursoRetenido1;
        this.recursoSolicitado1 = recursoSolicitado1;
        this.idPedido2 = idPedido2;
        this.recursoRetenido2 = recursoRetenido2;
        this.recursoSolicitado2 = recursoSolicitado2;
        this.resuelto = false;
        this.idPedidoGanador = "";
        this.causaBloqueo = this.construirCausa();
    }

    // construir el texto explicativo de la causa del interbloqueo
    private String construirCausa() {
        StringBuilder constructor = new StringBuilder();
        constructor.append("Espera circular detectada: Pedido ");
        constructor.append(this.idPedido1);
        constructor.append(" retiene ");
        constructor.append(this.recursoRetenido1.getNombre());
        constructor.append(" y espera ");
        constructor.append(this.recursoSolicitado1.getNombre());
        constructor.append(", mientras que Pedido ");
        constructor.append(this.idPedido2);
        constructor.append(" retiene ");
        constructor.append(this.recursoRetenido2.getNombre());
        constructor.append(" y espera ");
        constructor.append(this.recursoSolicitado2.getNombre());
        return constructor.toString();
    }

    /**
     * Obtener el identificador del primer pedido.
     *
     * @return identificador de pedido.
     */
    public String getIdPedido1() {
        return this.idPedido1;
    }

    /**
     * Obtener el recurso retenido por el primer pedido.
     *
     * @return recurso asignado.
     */
    public TipoRecurso getRecursoRetenido1() {
        return this.recursoRetenido1;
    }

    /**
     * Obtener el recurso solicitado por el primer pedido.
     *
     * @return recurso esperado.
     */
    public TipoRecurso getRecursoSolicitado1() {
        return this.recursoSolicitado1;
    }

    /**
     * Obtener el identificador del segundo pedido.
     *
     * @return identificador de pedido.
     */
    public String getIdPedido2() {
        return this.idPedido2;
    }

    /**
     * Obtener el recurso retenido por el segundo pedido.
     *
     * @return recurso asignado.
     */
    public TipoRecurso getRecursoRetenido2() {
        return this.recursoRetenido2;
    }

    /**
     * Obtener el recurso solicitado por el segundo pedido.
     *
     * @return recurso esperado.
     */
    public TipoRecurso getRecursoSolicitado2() {
        return this.recursoSolicitado2;
    }

    /**
     * Consultar si el conflicto ya fue resuelto manualmente.
     *
     * @return verdadero si fue resuelto.
     */
    public boolean isResuelto() {
        return this.resuelto;
    }

    /**
     * Marcar el estado de resolucion del conflicto.
     *
     * @param resuelto estado booleano de resolucion.
     */
    public void setResuelto(boolean resuelto) {
        this.resuelto = resuelto;
    }

    /**
     * Obtener el identificador del pedido seleccionado como ganador.
     *
     * @return identificador del pedido ganador.
     */
    public String getIdPedidoGanador() {
        return this.idPedidoGanador;
    }

    /**
     * Asignar el pedido seleccionado para continuar con los recursos.
     *
     * @param idPedidoGanador identificador del pedido ganador.
     */
    public void setIdPedidoGanador(String idPedidoGanador) {
        this.idPedidoGanador = idPedidoGanador;
    }

    /**
     * Obtener la explicacion textual de la causa del conflicto.
     *
     * @return texto de la causa.
     */
    public String getCausaBloqueo() {
        return this.causaBloqueo;
    }

    /**
     * Representacion descriptiva del conflicto en formato de texto.
     *
     * @return resumen del estado de interbloqueo.
     */
    @Override
    public String toString() {
        StringBuilder constructor = new StringBuilder();
        constructor.append("Conflicto[Pedido1=");
        constructor.append(this.idPedido1);
        constructor.append(" (retiene ");
        constructor.append(this.recursoRetenido1.getNombre());
        constructor.append(", espera ");
        constructor.append(this.recursoSolicitado1.getNombre());
        constructor.append(") <---> Pedido2=");
        constructor.append(this.idPedido2);
        constructor.append(" (retiene ");
        constructor.append(this.recursoRetenido2.getNombre());
        constructor.append(", espera ");
        constructor.append(this.recursoSolicitado2.getNombre());
        constructor.append("), Resuelto=");
        constructor.append(this.resuelto);
        constructor.append("]");
        return constructor.toString();
    }
}
