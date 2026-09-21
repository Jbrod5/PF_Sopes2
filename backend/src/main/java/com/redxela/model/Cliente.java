package com.redxela.model;

// entidad que representa a un cliente del centro de distribucion
public class Cliente {

    // identificador unico del cliente
    private String id;

    // nombre completo de la persona o empresa
    private String nombre;

    // correo electronico de contacto
    private String correo;

    /**
     * Constructor por defecto de la clase cliente.
     */
    public Cliente() {
        // inicializar atributos con cadenas vacias
        this.id = "";
        this.nombre = "";
        this.correo = "";
    }

    /**
     * Constructor sobrecargado con identificador y nombre.
     *
     * @param id identificador unico del cliente.
     * @param nombre nombre completo del cliente.
     */
    public Cliente(String id, String nombre) {
        this.id = id;
        this.nombre = nombre;
        this.correo = "";
    }

    /**
     * Constructor sobrecargado con identificador nombre y correo.
     *
     * @param id identificador unico del cliente.
     * @param nombre nombre completo del cliente.
     * @param correo correo electronico del cliente.
     */
    public Cliente(String id, String nombre, String correo) {
        this.id = id;
        this.nombre = nombre;
        this.correo = correo;
    }

    /**
     * Obtener el identificador del cliente.
     *
     * @return identificador unico.
     */
    public String getId() {
        return this.id;
    }

    /**
     * Asignar el identificador del cliente.
     *
     * @param id identificador a registrar.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Obtener el nombre del cliente.
     *
     * @return nombre registrado.
     */
    public String getNombre() {
        return this.nombre;
    }

    /**
     * Asignar el nombre del cliente.
     *
     * @param nombre nombre a registrar.
     */
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    /**
     * Obtener el correo electronico del cliente.
     *
     * @return correo electronico.
     */
    public String getCorreo() {
        return this.correo;
    }

    /**
     * Asignar el correo electronico del cliente.
     *
     * @param correo correo a registrar.
     */
    public void setCorreo(String correo) {
        this.correo = correo;
    }

    /**
     * Representacion en cadena de texto de la informacion del cliente.
     *
     * @return cadena descriptiva del cliente.
     */
    @Override
    public String toString() {
        StringBuilder constructorCadena = new StringBuilder();
        constructorCadena.append("Cliente[id=");
        constructorCadena.append(this.id);
        constructorCadena.append(", nombre=");
        constructorCadena.append(this.nombre);
        constructorCadena.append("]");
        return constructorCadena.toString();
    }
}
