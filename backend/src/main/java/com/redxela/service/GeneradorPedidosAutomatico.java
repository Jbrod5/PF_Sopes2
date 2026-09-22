package com.redxela.service;

import com.redxela.model.Cliente;
import com.redxela.model.ItemPedido;
import com.redxela.model.NivelServicio;
import com.redxela.model.Pedido;
import com.redxela.model.Producto;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// generador en segundo plano que simula la llegada continua y aleatoria de clientes y pedidos
public class GeneradorPedidosAutomatico implements Runnable {

    // referencia a la cola de prioridad donde se depositan los pedidos generados
    private final ColaPrioridadPedidos colaPedidos;

    // catalogo de clientes disponibles para emitir pedidos
    private final List<Cliente> listaClientes;

    // catalogo de productos para conformar los pedidos
    private final List<Producto> listaProductos;

    // bandera de control para el ciclo de ejecucion del hilo
    private volatile boolean activo;

    // bandera para suspender temporalmente la generacion sin terminar el hilo
    private volatile boolean pausado;

    // tiempo en milisegundos entre cada pedido simulado
    private long intervaloGeneracionMs;

    // generador de numeros aleatorios
    private final Random aleatorio;

    // contador correlativo para los identificadores de pedidos
    private int contadorPedido;

    /**
     * Constructor sobrecargado con cola clientes y catalogo de productos.
     *
     * @param colaPedidos cola de prioridad compartida.
     * @param listaClientes clientes de muestra.
     * @param listaProductos catalogo de productos disponibles.
     */
    public GeneradorPedidosAutomatico(
            ColaPrioridadPedidos colaPedidos,
            List<Cliente> listaClientes,
            List<Producto> listaProductos
    ) {
        this.colaPedidos = colaPedidos;
        this.listaClientes = new ArrayList<Cliente>();
        if (listaClientes != null) {
            for (int i = 0; i < listaClientes.size(); i++) {
                this.listaClientes.add(listaClientes.get(i));
            }
        }
        this.listaProductos = new ArrayList<Producto>();
        if (listaProductos != null) {
            for (int i = 0; i < listaProductos.size(); i++) {
                this.listaProductos.add(listaProductos.get(i));
            }
        }
        this.activo = false;
        this.pausado = false;
        this.intervaloGeneracionMs = 800L;
        this.aleatorio = new Random();
        this.contadorPedido = 1;
    }

    /**
     * Constructor sobrecargado con intervalo configurable de generacion.
     *
     * @param colaPedidos cola de prioridad.
     * @param listaClientes lista de clientes.
     * @param listaProductos lista de productos.
     * @param intervaloGeneracionMs milisegundos entre creaciones.
     */
    public GeneradorPedidosAutomatico(
            ColaPrioridadPedidos colaPedidos,
            List<Cliente> listaClientes,
            List<Producto> listaProductos,
            long intervaloGeneracionMs
    ) {
        this.colaPedidos = colaPedidos;
        this.listaClientes = new ArrayList<Cliente>();
        if (listaClientes != null) {
            for (int i = 0; i < listaClientes.size(); i++) {
                this.listaClientes.add(listaClientes.get(i));
            }
        }
        this.listaProductos = new ArrayList<Producto>();
        if (listaProductos != null) {
            for (int i = 0; i < listaProductos.size(); i++) {
                this.listaProductos.add(listaProductos.get(i));
            }
        }
        this.activo = false;
        this.pausado = false;
        this.intervaloGeneracionMs = intervaloGeneracionMs;
        this.aleatorio = new Random();
        this.contadorPedido = 1;
    }

    /**
     * Generar un pedido aleatorio e ingresarlo directamente a la cola de prioridad.
     *
     * @return instancia del pedido generado y encolado.
     */
    public Pedido generarPedidoAleatorio() {
        if (this.listaClientes.isEmpty() || this.listaProductos.isEmpty()) {
            return null;
        }

        // seleccionar un cliente aleatorio
        int indiceCliente = this.aleatorio.nextInt(this.listaClientes.size());
        Cliente cliente = this.listaClientes.get(indiceCliente);

        // seleccionar un nivel de servicio aleatorio entre los cinco disponibles
        NivelServicio[] niveles = NivelServicio.values();
        int indiceNivel = this.aleatorio.nextInt(niveles.length);
        NivelServicio nivel = niveles[indiceNivel];

        // construir el identificador unico del pedido
        String idPedido = "AUT-" + this.contadorPedido;
        this.contadorPedido = this.contadorPedido + 1;

        Pedido nuevoPedido = new Pedido(idPedido, cliente, nivel);

        // definir cantidad de lineas de items entre 1 y 3
        int lineas = 1 + this.aleatorio.nextInt(3);
        for (int i = 0; i < lineas; i++) {
            int indiceProd = this.aleatorio.nextInt(this.listaProductos.size());
            Producto producto = this.listaProductos.get(indiceProd);
            int cantidad = 1 + this.aleatorio.nextInt(5);
            nuevoPedido.agregarItem(new ItemPedido(producto, cantidad));
        }

        // encolar el pedido generado en la cola concurrente
        this.colaPedidos.encolarPedido(nuevoPedido);

        System.out.println("[GENERADOR] Pedido " + nuevoPedido.getId() + " generado: " + nuevoPedido.getNivelServicio().getNombre() + " (" + nuevoPedido.getCantidadTotal() + " articulos) de " + nuevoPedido.getCliente().getNombre());
        return nuevoPedido;
    }

    /**
     * Iniciar la generacion continua de pedidos en segundo plano.
     */
    public void iniciar() {
        this.activo = true;
        this.pausado = false;
    }

    /**
     * Pausar temporalmente la emision de pedidos sin terminar el hilo.
     */
    public void pausar() {
        this.pausado = true;
        System.out.println("[GENERADOR] Generacion automatica pausada");
    }

    /**
     * Reanudar la emision continua de pedidos tras una pausa.
     */
    public void reanudar() {
        this.pausado = false;
        System.out.println("[GENERADOR] Generacion automatica reanudada");
    }

    /**
     * Detener permanentemente el ciclo de generacion de pedidos.
     */
    public void detener() {
        this.activo = false;
    }

    /**
     * Ejecucion del hilo en segundo plano que produce pedidos a intervalos regulares.
     */
    @Override
    public void run() {
        this.activo = true;
        System.out.println("[GENERADOR] Hilo generador de pedidos iniciado");

        while (this.activo) {
            try {
                // aguardar el lapso entre pedidos
                Thread.sleep(this.intervaloGeneracionMs);

                if (!this.activo) {
                    break;
                }

                // emitir pedido solo si no se encuentra en pausa
                if (!this.pausado) {
                    this.generarPedidoAleatorio();
                }

            } catch (InterruptedException excepcion) {
                Thread.currentThread().interrupt();
                System.err.println("[GENERADOR] Hilo generador interrumpido: " + excepcion.getMessage());
                break;
            }
        }

        System.out.println("[GENERADOR] Hilo generador finalizado");
    }

    /**
     * Consultar si el generador se encuentra en ejecucion activa.
     *
     * @return verdadero si esta activo.
     */
    public boolean isActivo() {
        return this.activo;
    }

    /**
     * Consultar si la generacion se encuentra en estado de pausa.
     *
     * @return verdadero si esta pausado.
     */
    public boolean isPausado() {
        return this.pausado;
    }
}
