package com.redxela.service;

import com.redxela.model.Cliente;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

// registro concurrente de cuentas de clientes del centro de distribucion
public class RegistroClientes {

    // mapa de clientes por identificador unico
    private final Map<String, Cliente> clientesPorId;

    // cerrojo para garantizar exclusion mutua en el registro
    private final ReentrantLock cerrojoRegistro;

    /**
     * Constructor por defecto del registro de clientes.
     */
    public RegistroClientes() {
        // inicializar mapa y cerrojo con equidad
        this.clientesPorId = new HashMap<String, Cliente>();
        this.cerrojoRegistro = new ReentrantLock(true);
    }

    /**
     * Registrar una nueva cuenta de cliente si el identificador esta libre.
     *
     * @param cliente cliente a registrar.
     * @return verdadero si se registro o falso si es nulo duplicado o invalido.
     */
    public boolean registrarCliente(Cliente cliente) {
        if (cliente == null) {
            return false;
        }
        if (cliente.getId() == null) {
            return false;
        }
        if (cliente.getId().trim().equals("")) {
            return false;
        }
        // adquirir el cerrojo para revisar y registrar con exclusion mutua
        this.cerrojoRegistro.lock();
        try {
            if (this.clientesPorId.containsKey(cliente.getId())) {
                return false;
            }
            this.clientesPorId.put(cliente.getId(), cliente);
            System.out.println("[CLIENTES] Cuenta registrada: " + cliente.getId() + " (" + cliente.getNombre() + ")");
            return true;
        } finally {
            // liberar el cerrojo de forma segura
            this.cerrojoRegistro.unlock();
        }
    }

    /**
     * Obtener el cliente asociado a un identificador.
     *
     * @param idCliente identificador del cliente.
     * @return cliente registrado o null si no existe.
     */
    public Cliente obtenerCliente(String idCliente) {
        if (idCliente == null) {
            return null;
        }
        // adquirir el cerrojo para leer con exclusion mutua
        this.cerrojoRegistro.lock();
        try {
            return this.clientesPorId.get(idCliente);
        } finally {
            // liberar el cerrojo de forma segura
            this.cerrojoRegistro.unlock();
        }
    }

    /**
     * Verificar si un identificador de cliente ya esta registrado.
     *
     * @param idCliente identificador a consultar.
     * @return verdadero si existe en el registro.
     */
    public boolean existeCliente(String idCliente) {
        if (idCliente == null) {
            return false;
        }
        // adquirir el cerrojo para consultar con exclusion mutua
        this.cerrojoRegistro.lock();
        try {
            return this.clientesPorId.containsKey(idCliente);
        } finally {
            // liberar el cerrojo de forma segura
            this.cerrojoRegistro.unlock();
        }
    }

    /**
     * Obtener una copia de todos los clientes registrados.
     *
     * @return lista con los clientes del registro.
     */
    public List<Cliente> listarClientes() {
        // adquirir el cerrojo para copiar con exclusion mutua
        this.cerrojoRegistro.lock();
        try {
            List<Cliente> copia = new ArrayList<Cliente>();
            for (Cliente actual : this.clientesPorId.values()) {
                copia.add(actual);
            }
            return copia;
        } finally {
            // liberar el cerrojo de forma segura
            this.cerrojoRegistro.unlock();
        }
    }

    /**
     * Consultar la cantidad de cuentas registradas.
     *
     * @return numero de clientes en el registro.
     */
    public int getCantidadClientes() {
        // adquirir el cerrojo para contar con exclusion mutua
        this.cerrojoRegistro.lock();
        try {
            return this.clientesPorId.size();
        } finally {
            // liberar el cerrojo de forma segura
            this.cerrojoRegistro.unlock();
        }
    }
}
