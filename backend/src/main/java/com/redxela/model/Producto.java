package com.redxela.model;

// entidad que representa un producto del catalogo del centro de distribucion
public class Producto {

    // identificador unico del producto
    private String id;

    // nombre descriptivo del producto
    private String nombre;

    // clasificacion del producto segun su tipo de mercancia
    private TipoMercancia tipoMercancia;

    // peso unitario en kilogramos
    private double pesoUnitario;

    // precio unitario del producto
    private double precioUnitario;

    /**
     * Constructor por defecto de la clase producto.
     */
    public Producto() {
        // inicializar atributos basicos
        this.id = "";
        this.nombre = "";
        this.tipoMercancia = TipoMercancia.ESTANDAR;
        this.pesoUnitario = 1.0;
        this.precioUnitario = 0.0;
    }

    /**
     * Constructor sobrecargado con identificador nombre y tipo de mercancia.
     *
     * @param id identificador unico del producto.
     * @param nombre nombre comercial del producto.
     * @param tipoMercancia categoria de mercancia asociada.
     */
    public Producto(String id, String nombre, TipoMercancia tipoMercancia) {
        this.id = id;
        this.nombre = nombre;
        this.tipoMercancia = tipoMercancia;
        this.pesoUnitario = 1.0;
        this.precioUnitario = 0.0;
    }

    /**
     * Constructor sobrecargado completo con todas las propiedades.
     *
     * @param id identificador unico del producto.
     * @param nombre nombre comercial del producto.
     * @param tipoMercancia categoria de mercancia asociada.
     * @param pesoUnitario peso individual en kilogramos.
     * @param precioUnitario precio monetario por unidad.
     */
    public Producto(String id, String nombre, TipoMercancia tipoMercancia, double pesoUnitario, double precioUnitario) {
        this.id = id;
        this.nombre = nombre;
        this.tipoMercancia = tipoMercancia;
        this.pesoUnitario = pesoUnitario;
        this.precioUnitario = precioUnitario;
    }

    /**
     * Obtener el identificador unico del producto.
     *
     * @return identificador del producto.
     */
    public String getId() {
        return this.id;
    }

    /**
     * Asignar el identificador unico del producto.
     *
     * @param id identificador a registrar.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Obtener el nombre comercial del producto.
     *
     * @return nombre del producto.
     */
    public String getNombre() {
        return this.nombre;
    }

    /**
     * Asignar el nombre comercial del producto.
     *
     * @param nombre nombre a registrar.
     */
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    /**
     * Obtener la clasificacion de tipo de mercancia.
     *
     * @return tipo de mercancia asignado.
     */
    public TipoMercancia getTipoMercancia() {
        return this.tipoMercancia;
    }

    /**
     * Asignar la clasificacion de tipo de mercancia.
     *
     * @param tipoMercancia categoria a asignar.
     */
    public void setTipoMercancia(TipoMercancia tipoMercancia) {
        this.tipoMercancia = tipoMercancia;
    }

    /**
     * Obtener el peso unitario del producto.
     *
     * @return peso en kilogramos.
     */
    public double getPesoUnitario() {
        return this.pesoUnitario;
    }

    /**
     * Asignar el peso unitario del producto.
     *
     * @param pesoUnitario peso a registrar.
     */
    public void setPesoUnitario(double pesoUnitario) {
        this.pesoUnitario = pesoUnitario;
    }

    /**
     * Obtener el precio unitario del producto.
     *
     * @return precio del producto.
     */
    public double getPrecioUnitario() {
        return this.precioUnitario;
    }

    /**
     * Asignar el precio unitario del producto.
     *
     * @param precioUnitario precio a registrar.
     */
    public void setPrecioUnitario(double precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    /**
     * Representacion en texto de los datos del producto.
     *
     * @return cadena con el detalle del producto.
     */
    @Override
    public String toString() {
        StringBuilder constructorCadena = new StringBuilder();
        constructorCadena.append("Producto[id=");
        constructorCadena.append(this.id);
        constructorCadena.append(", nombre=");
        constructorCadena.append(this.nombre);
        constructorCadena.append(", tipo=");
        constructorCadena.append(this.tipoMercancia.getNombre());
        constructorCadena.append("]");
        return constructorCadena.toString();
    }
}
