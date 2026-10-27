<script>
  import { onMount } from "svelte";
  import { registrarPedido, cambiarPolitica, obtenerClientes, obtenerProductos } from "../services/servicioApi.js";

  let clientes = [];
  let productos = [];
  let idCliente = "";
  let nivel = "ESTANDAR";
  let politica = "NIVEL_SERVICIO_ESTRICTO";
  let idProducto = "";
  let cantidad = 1;
  let lineas = [];
  let mensaje = "";
  let exito = true;

  onMount(async () => {
    try {
      clientes = await obtenerClientes();
      if (clientes.length > 0) {
        idCliente = clientes[0].id;
      }
    } catch (e) {
      console.error("Error cargando clientes:", e);
    }
    try {
      productos = await obtenerProductos();
      if (productos.length > 0) {
        idProducto = productos[0].id;
      }
    } catch (e) {
      console.error("Error cargando productos:", e);
    }
  });

  function agregarLinea() {
    const unidades = Number(cantidad);
    if (!idProducto || unidades <= 0) {
      return;
    }
    lineas = [...lineas, { producto: idProducto, cantidad: unidades }];
  }

  function quitarLinea(indice) {
    lineas = lineas.filter((_, i) => i !== indice);
  }

  async function enviar() {
    mensaje = "";
    try {
      const respuesta = await registrarPedido({ idCliente, nivel, items: lineas });
      const texto = typeof respuesta === "string" ? respuesta : JSON.stringify(respuesta);
      mensaje = texto;
      exito = texto.includes("exitosamente");
      if (exito) {
        lineas = [];
      }
    } catch (e) {
      exito = false;
      mensaje = "Error de conexion con el servidor";
    }
  }

  async function actualizarPolitica() {
    await cambiarPolitica(politica);
  }
</script>

<div class="card tarjeta-panel h-100">
  <div class="card-header bg-primary text-white">Registro de Pedidos</div>
  <div class="card-body">
    <form on:submit|preventDefault={enviar}>
      <label class="form-label" for="sel-cliente">Cliente</label>
      <select id="sel-cliente" bind:value={idCliente} class="form-select mb-2">
        {#each clientes as c}
          <option value={c.id}>{c.id} - {c.nombre}</option>
        {/each}
      </select>
      <label class="form-label" for="sel-nivel">Nivel de servicio</label>
      <select id="sel-nivel" bind:value={nivel} class="form-select mb-2">
        <option>ESTANDAR</option>
        <option>EXPRES</option>
        <option>PRIORITARIO</option>
        <option>PROGRAMADO</option>
        <option>ECONOMICO</option>
      </select>
      <label class="form-label">Lineas del pedido</label>
      <div class="row g-2 mb-2">
        <div class="col-7">
          <select bind:value={idProducto} class="form-select" aria-label="Producto">
            {#each productos as p}
              <option value={p.id}>{p.id} - {p.nombre}</option>
            {/each}
          </select>
        </div>
        <div class="col-3">
          <input type="number" min="1" bind:value={cantidad} class="form-control" aria-label="Cantidad" />
        </div>
        <div class="col-2">
          <button type="button" on:click={agregarLinea} class="btn btn-outline-primary w-100" title="Agregar linea">+</button>
        </div>
      </div>
      {#if lineas.length > 0}
        <ul class="list-group mb-2">
          {#each lineas as l, i}
            <li class="list-group-item d-flex justify-content-between align-items-center py-1">
              <span>{l.producto} x {l.cantidad}</span>
              <button type="button" on:click={() => quitarLinea(i)} class="btn btn-sm btn-outline-danger">Quitar</button>
            </li>
          {/each}
        </ul>
      {/if}
      <button type="submit" class="btn btn-success w-100">Registrar pedido</button>
    </form>
    {#if mensaje}
      <div class="alert {exito ? 'alert-success' : 'alert-danger'} mt-2 mb-0 py-2">{mensaje}</div>
    {/if}
  </div>
  <div class="card-footer">
    <label class="form-label" for="sel-politica"><strong>Politica de atencion</strong></label>
    <div class="input-group">
      <select id="sel-politica" bind:value={politica} class="form-select">
        <option value="NIVEL_SERVICIO_ESTRICTO">Nivel de servicio estricto</option>
        <option value="MENOR_CANTIDAD_ARTICULOS">Menor cantidad de articulos</option>
        <option value="TIEMPO_ESPERA_FIFO">Tiempo de espera FIFO</option>
      </select>
      <button on:click={actualizarPolitica} class="btn btn-secondary">Aplicar</button>
    </div>
  </div>
</div>
