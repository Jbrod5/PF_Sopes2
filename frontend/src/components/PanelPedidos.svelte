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
      lineas = [];
    } catch (e) {
      mensaje = "Error de conexion con el servidor";
    }
  }

  async function actualizarPolitica() {
    await cambiarPolitica(politica);
  }
</script>

<div class="container mt-3">
  <h4>Panel de Pedidos</h4>
  <form on:submit|preventDefault={enviar}>
    <label class="form-label">Cliente</label>
    <select bind:value={idCliente} class="form-select mb-2">
      {#each clientes as c}
        <option value={c.id}>{c.id} - {c.nombre}</option>
      {/each}
    </select>
    <label class="form-label">Nivel de servicio</label>
    <select bind:value={nivel} class="form-select mb-2">
      <option>ESTANDAR</option>
      <option>EXPRES</option>
      <option>PRIORITARIO</option>
      <option>PROGRAMADO</option>
      <option>ECONOMICO</option>
    </select>
    <label class="form-label">Agregar producto</label>
    <div class="row g-2 mb-2">
      <div class="col-7">
        <select bind:value={idProducto} class="form-select">
          {#each productos as p}
            <option value={p.id}>{p.id} - {p.nombre}</option>
          {/each}
        </select>
      </div>
      <div class="col-3">
        <input type="number" min="1" bind:value={cantidad} class="form-control" />
      </div>
      <div class="col-2">
        <button type="button" on:click={agregarLinea} class="btn btn-outline-primary">+</button>
      </div>
    </div>
    {#if lineas.length > 0}
      <ul class="list-group mb-2">
        {#each lineas as l, i}
          <li class="list-group-item d-flex justify-content-between">
            <span>{l.producto} x {l.cantidad}</span>
            <button type="button" on:click={() => quitarLinea(i)} class="btn btn-sm btn-danger">Quitar</button>
          </li>
        {/each}
      </ul>
    {/if}
    <button type="submit" class="btn btn-primary">Registrar pedido</button>
  </form>
  {#if mensaje}
    <div class="alert alert-info mt-2">{mensaje}</div>
  {/if}
  <h4 class="mt-3">Politica de Atencion</h4>
  <select bind:value={politica} class="form-select mb-2">
    <option value="NIVEL_SERVICIO_ESTRICTO">Nivel de servicio estricto</option>
    <option value="MENOR_CANTIDAD_ARTICULOS">Menor cantidad de articulos</option>
    <option value="TIEMPO_ESPERA_FIFO">Tiempo de espera FIFO</option>
  </select>
  <button on:click={actualizarPolitica} class="btn btn-secondary">Cambiar politica</button>
</div>
