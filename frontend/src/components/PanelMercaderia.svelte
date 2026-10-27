<script>
  import { onMount } from "svelte";
  import { obtenerProductos, registrarMercaderia } from "../services/servicioApi";
  let productos = [];
  let producto = "";
  let cantidad = 1;
  let mensaje = "";
  let exito = true;

  onMount(async () => {
    try {
      productos = await obtenerProductos();
      if (productos.length > 0) {
        producto = productos[0].id;
      }
    } catch (e) {
      console.error("Error obteniendo productos:", e);
    }
  });

  async function enviar() {
    try {
      const respuesta = await registrarMercaderia(producto, Number(cantidad));
      if (respuesta.mensaje) {
        exito = true;
        mensaje = respuesta.mensaje;
      } else {
        exito = false;
        mensaje = respuesta.error || "Respuesta inesperada del servidor";
      }
    } catch (e) {
      exito = false;
      mensaje = "Error de conexion con el servidor";
    }
  }
</script>

<div class="card tarjeta-panel h-100">
  <div class="card-header bg-primary text-white">Recepcion de Mercaderia</div>
  <div class="card-body">
    <label class="form-label" for="sel-producto">Producto del catalogo</label>
    <select id="sel-producto" bind:value={producto} class="form-select mb-2">
      {#each productos as p}
        <option value={p.id}>{p.nombre} ({p.mercancia})</option>
      {/each}
    </select>
    <div class="row g-2">
      <div class="col-6">
        <label class="form-label" for="inp-cantidad">Unidades recibidas</label>
        <input id="inp-cantidad" type="number" min="1" bind:value={cantidad} class="form-control" />
      </div>
      <div class="col-6 d-flex align-items-end">
        <button on:click={enviar} class="btn btn-success w-100">Registrar llegada</button>
      </div>
    </div>
    {#if mensaje}
      <div class="alert {exito ? 'alert-success' : 'alert-danger'} mt-2 mb-0 py-2">{mensaje}</div>
    {/if}
    <p class="text-muted small mt-2 mb-0">El lote entra al buffer, inventario lo clasifica y el almacen le asigna celdas no contiguas.</p>
  </div>
</div>
