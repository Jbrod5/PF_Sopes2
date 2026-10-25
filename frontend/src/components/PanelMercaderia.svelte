<script>
  import { onMount } from "svelte";
  import { obtenerProductos, registrarMercaderia } from "../services/servicioApi";
  let productos = [];
  let producto = "";
  let cantidad = 1;
  let mensaje = "";

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
    const respuesta = await registrarMercaderia(producto, Number(cantidad));
    if (respuesta.mensaje) {
      mensaje = respuesta.mensaje;
    } else {
      mensaje = respuesta.error;
    }
  }
</script>

<div class="container mt-3">
  <h4>Registro de Mercaderia</h4>
  <select bind:value={producto} class="form-select mb-2">
    {#each productos as p}
      <option value={p.id}>{p.nombre} ({p.mercancia})</option>
    {/each}
  </select>
  <input type="number" min="1" bind:value={cantidad} class="form-control mb-2" />
  <button on:click={enviar} class="btn btn-primary">Registrar mercaderia</button>
  {#if mensaje}
    <p>{mensaje}</p>
  {/if}
</div>
