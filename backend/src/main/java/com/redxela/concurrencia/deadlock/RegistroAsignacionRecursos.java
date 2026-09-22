package com.redxela.concurrencia.deadlock;

import com.redxela.model.TipoRecurso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

// registro centralizado del grafo de asignacion y espera de recursos entre pedidos
public class RegistroAsignacionRecursos {

    // mapa de recursos actualmente retenidos por cada pedido
    private final Map<String, List<TipoRecurso>> recursosRetenidosPorPedido;

    // mapa del recurso que cada pedido se encuentra esperando activamente
    private final Map<String, TipoRecurso> recursoEsperadoPorPedido;

    // cerrojo para garantizar acceso seguro en entornos multihilo
    private final ReentrantLock cerrojo;

    /**
     * Constructor por defecto del registro de asignacion de recursos.
     */
    public RegistroAsignacionRecursos() {
        this.recursosRetenidosPorPedido = new HashMap<String, List<TipoRecurso>>();
        this.recursoEsperadoPorPedido = new HashMap<String, TipoRecurso>();
        this.cerrojo = new ReentrantLock(true);
    }

    /**
     * Registrar la asignacion efectiva de un recurso a un pedido.
     *
     * @param idPedido identificador del pedido.
     * @param recurso recurso obtenido.
     */
    public void registrarAsignacion(String idPedido, TipoRecurso recurso) {
        if (idPedido == null || recurso == null) {
            return;
        }

        this.cerrojo.lock();
        try {
            // remover el recurso del mapa de espera si lo estaba esperando
            if (this.recursoEsperadoPorPedido.containsKey(idPedido)) {
                if (this.recursoEsperadoPorPedido.get(idPedido) == recurso) {
                    this.recursoEsperadoPorPedido.remove(idPedido);
                }
            }

            // registrar el recurso en la lista de recursos retenidos
            List<TipoRecurso> lista;
            if (this.recursosRetenidosPorPedido.containsKey(idPedido)) {
                lista = this.recursosRetenidosPorPedido.get(idPedido);
            } else {
                lista = new ArrayList<TipoRecurso>();
                this.recursosRetenidosPorPedido.put(idPedido, lista);
            }

            if (!lista.contains(recurso)) {
                lista.add(recurso);
            }
        } finally {
            this.cerrojo.unlock();
        }
    }

    /**
     * Registrar que un pedido ha iniciado la solicitud bloqueante de un recurso.
     *
     * @param idPedido identificador del pedido solicitante.
     * @param recurso recurso esperado.
     */
    public void registrarSolicitud(String idPedido, TipoRecurso recurso) {
        if (idPedido == null || recurso == null) {
            return;
        }

        this.cerrojo.lock();
        try {
            this.recursoEsperadoPorPedido.put(idPedido, recurso);
        } finally {
            this.cerrojo.unlock();
        }
    }

    /**
     * Registrar la liberacion de un recurso que estaba retenido por un pedido.
     *
     * @param idPedido identificador del pedido.
     * @param recurso recurso liberado.
     */
    public void registrarLiberacion(String idPedido, TipoRecurso recurso) {
        if (idPedido == null || recurso == null) {
            return;
        }

        this.cerrojo.lock();
        try {
            if (this.recursosRetenidosPorPedido.containsKey(idPedido)) {
                List<TipoRecurso> lista = this.recursosRetenidosPorPedido.get(idPedido);
                lista.remove(recurso);
                if (lista.isEmpty()) {
                    this.recursosRetenidosPorPedido.remove(idPedido);
                }
            }
        } finally {
            this.cerrojo.unlock();
        }
    }

    /**
     * Limpiar el estado de espera de un pedido cuando desiste o adquiere.
     *
     * @param idPedido identificador del pedido.
     */
    public void limpiarSolicitud(String idPedido) {
        if (idPedido == null) {
            return;
        }

        this.cerrojo.lock();
        try {
            this.recursoEsperadoPorPedido.remove(idPedido);
        } finally {
            this.cerrojo.unlock();
        }
    }

    /**
     * Limpiar completamente el registro de un pedido tras su finalizacion o cancelacion.
     *
     * @param idPedido identificador del pedido.
     */
    public void limpiarTodoPedido(String idPedido) {
        if (idPedido == null) {
            return;
        }

        this.cerrojo.lock();
        try {
            this.recursosRetenidosPorPedido.remove(idPedido);
            this.recursoEsperadoPorPedido.remove(idPedido);
        } finally {
            this.cerrojo.unlock();
        }
    }

    /**
     * Obtener una copia de la lista de recursos retenidos por un pedido.
     *
     * @param idPedido identificador del pedido.
     * @return lista de recursos retenidos.
     */
    public List<TipoRecurso> obtenerRecursosRetenidos(String idPedido) {
        this.cerrojo.lock();
        try {
            List<TipoRecurso> copia = new ArrayList<TipoRecurso>();
            if (this.recursosRetenidosPorPedido.containsKey(idPedido)) {
                List<TipoRecurso> lista = this.recursosRetenidosPorPedido.get(idPedido);
                for (int i = 0; i < lista.size(); i++) {
                    copia.add(lista.get(i));
                }
            }
            return copia;
        } finally {
            this.cerrojo.unlock();
        }
    }

    /**
     * Obtener el recurso que un pedido esta esperando actualmente.
     *
     * @param idPedido identificador del pedido.
     * @return recurso esperado o null si no esta bloqueado en espera.
     */
    public TipoRecurso obtenerRecursoEsperado(String idPedido) {
        this.cerrojo.lock();
        try {
            return this.recursoEsperadoPorPedido.get(idPedido);
        } finally {
            this.cerrojo.unlock();
        }
    }

    /**
     * Obtener una copia de todos los pedidos registrados como retenedores de recursos.
     *
     * @return lista de identificadores de pedidos reteniendo recursos.
     */
    public List<String> obtenerPedidosConRecursos() {
        this.cerrojo.lock();
        try {
            List<String> pedidos = new ArrayList<String>();
            for (String clave : this.recursosRetenidosPorPedido.keySet()) {
                pedidos.add(clave);
            }
            return pedidos;
        } finally {
            this.cerrojo.unlock();
        }
    }

    /**
     * Obtener una copia de todos los pedidos que estan actualmente esperando recursos.
     *
     * @return lista de identificadores de pedidos en espera.
     */
    public List<String> obtenerPedidosEnEspera() {
        this.cerrojo.lock();
        try {
            List<String> pedidos = new ArrayList<String>();
            for (String clave : this.recursoEsperadoPorPedido.keySet()) {
                pedidos.add(clave);
            }
            return pedidos;
        } finally {
            this.cerrojo.unlock();
        }
    }
}
