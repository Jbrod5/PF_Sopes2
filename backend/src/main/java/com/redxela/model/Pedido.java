package com.redxela.model;

// import java.util.ArrayList;
// import java.util.List;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


// entidad principal que representa un pedido de cliente en el centro de distribucion
public class Pedido {

    // identificador unico del pedido
    private String id;

    // cliente asociado a la cuenta que solicito el pedido
    private Cliente cliente;

    // nivel de servicio y prioridad contratada
    private NivelServicio nivelServicio;

    // lista de productos solicitados con sus cantidades
    private List<ItemPedido> listaItems;

    // cantidad total calculada de articulos del pedido
    private int cantidadTotal;

    // estado actual del pedido dentro del ciclo de vida
    private EstadoPedido estado;

    // marca de tiempo en milisegundos de la creacion del pedido
    private long tiempoCreacionMs;

    /**
     * Constructor por defecto del pedido.
     */
    public Pedido() {
        // inicializar atributos con valores por defecto
        this.id = "";
        this.cliente = new Cliente();
        this.nivelServicio = NivelServicio.ESTANDAR;
        this.listaItems = new ArrayList<ItemPedido>();
        this.cantidadTotal = 0;
        this.estado = EstadoPedido.RECIBIDO;
        this.tiempoCreacionMs = System.currentTimeMillis();
    }

    /**
     * Constructor sobrecargado con identificador cliente y nivel de servicio.
     *
     * @param id identificador unico del pedido.
     * @param cliente cliente que emite el pedido.
     * @param nivelServicio nivel de prioridad asignado.
     */
    public Pedido(String id, Cliente cliente, NivelServicio nivelServicio) {
        this.id = id;
        this.cliente = cliente;
        this.nivelServicio = nivelServicio;
        this.listaItems = new ArrayList<ItemPedido>();
        this.cantidadTotal = 0;
        this.estado = EstadoPedido.RECIBIDO;
        this.tiempoCreacionMs = System.currentTimeMillis();
    }

    /**
     * Constructor sobrecargado completo con lista inicial de items.
     *
     * @param id identificador unico del pedido.
     * @param cliente cliente emisor.
     * @param nivelServicio nivel de prioridad asignado.
     * @param listaItems lista con los articulos solicitados.
     */
    public Pedido(String id, Cliente cliente, NivelServicio nivelServicio, List<ItemPedido> listaItems) {
        this.id = id;
        this.cliente = cliente;
        this.nivelServicio = nivelServicio;
        this.listaItems = new ArrayList<ItemPedido>();
        this.estado = EstadoPedido.RECIBIDO;
        this.tiempoCreacionMs = System.currentTimeMillis();
        // agregar los items pasados en la lista
        if (listaItems != null) {
            for (int i = 0; i < listaItems.size(); i++) {
                this.agregarItem(listaItems.get(i));
            }
        }
        this.recalcularCantidadTotal();
    }

    /**
     * Agregar un nuevo item a la lista del pedido.
     *
     * @param item articulo y cantidad a anadir.
     */
    public void agregarItem(ItemPedido item) {
        // verificar que el item no sea nulo antes de agregarlo
        if (item != null) {
            this.listaItems.add(item);
            this.recalcularCantidadTotal();
        }
    }

    /**
     * Recalcular la suma total de articulos sumando las cantidades de cada item.
     */
    public void recalcularCantidadTotal() {
        int acumulador = 0;
        // recorrer todos los items para sumar unidades
        for (int i = 0; i < this.listaItems.size(); i++) {
            ItemPedido actual = this.listaItems.get(i);
            if (actual != null) {
                acumulador = acumulador + actual.getCantidad();
            }
        }
        this.cantidadTotal = acumulador;
    }

    /**
     * Obtener la lista consolidada de recursos requeridos para atender este pedido.
     *
     * @return lista sin duplicados con los tipos de recurso necesarios.
     */
    public List<TipoRecurso> obtenerRecursosRequeridos() {
        List<TipoRecurso> recursosConsolidados = new ArrayList<TipoRecurso>();
        // recorrer los items del pedido
        for (int i = 0; i < this.listaItems.size(); i++) {
            ItemPedido item = this.listaItems.get(i);
            if (item != null) {
                Producto producto = item.getProducto();
                if (producto != null) {
                    TipoMercancia tipoMercancia = producto.getTipoMercancia();
                    if (tipoMercancia != null) {
                        List<TipoRecurso> requeridos = tipoMercancia.getRecursosRequeridos();
                        // agregar recursos evitando duplicados en la lista
                        for (int j = 0; j < requeridos.size(); j++) {
                            TipoRecurso recurso = requeridos.get(j);
                            boolean yaExiste = false;
                            for (int k = 0; k < recursosConsolidados.size(); k++) {
                                if (recursosConsolidados.get(k) == recurso) {
                                    yaExiste = true;
                                    break;
                                }
                            }
                            if (!yaExiste) {
                                recursosConsolidados.add(recurso);
                            }
                        }
                    }
                }
            }
        }
        // return recursosConsolidados;
        // ordenar recursos segun su posicion fija en el enum para prevenir espera circular
        Collections.sort(recursosConsolidados);
        return recursosConsolidados;
    }

    /**
     * Obtener el identificador del pedido.
     *
     * @return identificador unico.
     */
    public String getId() {
        return this.id;
    }

    /**
     * Asignar el identificador del pedido.
     *
     * @param id identificador a registrar.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Obtener el cliente asociado.
     *
     * @return cliente del pedido.
     */
    public Cliente getCliente() {
        return this.cliente;
    }

    /**
     * Asignar el cliente del pedido.
     *
     * @param cliente cliente a asociar.
     */
    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    /**
     * Obtener el nivel de servicio.
     *
     * @return nivel de servicio.
     */
    public NivelServicio getNivelServicio() {
        return this.nivelServicio;
    }

    /**
     * Asignar el nivel de servicio del pedido.
     *
     * @param nivelServicio nivel a registrar.
     */
    public void setNivelServicio(NivelServicio nivelServicio) {
        this.nivelServicio = nivelServicio;
    }

    /**
     * Obtener la lista de items del pedido.
     *
     * @return lista de lineas de pedido.
     */
    public List<ItemPedido> getListaItems() {
        return this.listaItems;
    }

    /**
     * Asignar la lista de items del pedido.
     *
     * @param listaItems nueva lista de items.
     */
    public void setListaItems(List<ItemPedido> listaItems) {
        this.listaItems = listaItems;
        this.recalcularCantidadTotal();
    }

    /**
     * Obtener la cantidad total de unidades contenidas en el pedido.
     *
     * @return total de unidades.
     */
    public int getCantidadTotal() {
        return this.cantidadTotal;
    }

    /**
     * Asignar la cantidad total de articulos.
     *
     * @param cantidadTotal numero total.
     */
    public void setCantidadTotal(int cantidadTotal) {
        this.cantidadTotal = cantidadTotal;
    }

    /**
     * Obtener el estado actual del pedido.
     *
     * @return estado del ciclo de vida.
     */
    public EstadoPedido getEstado() {
        return this.estado;
    }

    /**
     * Asignar el nuevo estado del pedido.
     *
     * @param estado nuevo estado.
     */
    public void setEstado(EstadoPedido estado) {
        this.estado = estado;
    }

    /**
     * Obtener la marca de tiempo de creacion en milisegundos.
     *
     * @return tiempo en milisegundos.
     */
    public long getTiempoCreacionMs() {
        return this.tiempoCreacionMs;
    }

    /**
     * Asignar la marca de tiempo de creacion.
     *
     * @param tiempoCreacionMs tiempo en milisegundos.
     */
    public void setTiempoCreacionMs(long tiempoCreacionMs) {
        this.tiempoCreacionMs = tiempoCreacionMs;
    }

    /**
     * Representacion descriptiva del pedido en formato de texto.
     *
     * @return cadena con el resumen del pedido.
     */
    @Override
    public String toString() {
        StringBuilder constructorCadena = new StringBuilder();
        constructorCadena.append("Pedido[id=");
        constructorCadena.append(this.id);
        constructorCadena.append(", cliente=");
        constructorCadena.append(this.cliente.getNombre());
        constructorCadena.append(", nivel=");
        constructorCadena.append(this.nivelServicio.getNombre());
        constructorCadena.append(", articulos=");
        constructorCadena.append(this.cantidadTotal);
        constructorCadena.append(", estado=");
        constructorCadena.append(this.estado.name());
        constructorCadena.append("]");
        return constructorCadena.toString();
    }
}
