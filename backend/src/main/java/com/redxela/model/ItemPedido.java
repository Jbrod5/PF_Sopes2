package com.redxela.model;

// clase que representa una linea de articulo y cantidad dentro de un pedido
public class ItemPedido {

    // producto solicitado en esta linea
    private Producto producto;

    // cantidad de unidades solicitadas
    private int cantidad;

    /**
     * Constructor por defecto del item del pedido.
     */
    public ItemPedido() {
        // inicializar valores por defecto
        this.producto = new Producto();
        this.cantidad = 1;
    }

    /**
     * Constructor sobrecargado con producto y cantidad.
     *
     * @param producto producto solicitado.
     * @param cantidad numero de unidades pedidas.
     */
    public ItemPedido(Producto producto, int cantidad) {
        this.producto = producto;
        this.cantidad = cantidad;
    }

    /**
     * Obtener el producto asociado al item.
     *
     * @return producto solicitado.
     */
    public Producto getProducto() {
        return this.producto;
    }

    /**
     * Asignar el producto del item.
     *
     * @param producto producto a registrar.
     */
    public void setProducto(Producto producto) {
        this.producto = producto;
    }

    /**
     * Obtener la cantidad de unidades solicitadas.
     *
     * @return cantidad de unidades.
     */
    public int getCantidad() {
        return this.cantidad;
    }

    /**
     * Asignar la cantidad de unidades.
     *
     * @param cantidad numero de unidades.
     */
    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    /**
     * Representacion en texto de la linea de pedido.
     *
     * @return cadena con el detalle del item.
     */
    @Override
    public String toString() {
        StringBuilder constructorCadena = new StringBuilder();
        constructorCadena.append("ItemPedido[producto=");
        constructorCadena.append(this.producto.getNombre());
        constructorCadena.append(", cantidad=");
        constructorCadena.append(this.cantidad);
        constructorCadena.append("]");
        return constructorCadena.toString();
    }
}
