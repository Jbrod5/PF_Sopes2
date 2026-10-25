<script>
  import { onMount, onDestroy } from "svelte";
  import { inducirDeadlock, resolverDeadlock, obtenerDetalleDeadlock } from "../services/servicioApi";
  let ganador = "";
  let detalle = null;
  let intervalo = null;

  async function actualizar() {
    try {
      detalle = await obtenerDetalleDeadlock();
    } catch (e) {
      console.error("Error obteniendo detalle:", e);
    }
  }

  async function inducir() {
    await inducirDeadlock();
    await actualizar();
  }

  async function resolver() {
    await resolverDeadlock(ganador);
    await actualizar();
  }

  onMount(async () => {
    await actualizar();
    intervalo = setInterval(actualizar, 2000);
  });

  onDestroy(() => {
    if (intervalo) {
      clearInterval(intervalo);
    }
  });
</script>

<div class="container mt-3">
  <h4>Panel Deadlock</h4>
  {#if detalle && detalle.activo}
    <div class="alert alert-danger">Interbloqueo activo</div>
    <p>Causa: {detalle.causa}</p>
    <p>Pedido {detalle.pedido1.id}: retiene {detalle.pedido1.retiene}, espera {detalle.pedido1.espera}</p>
    <p>Pedido {detalle.pedido2.id}: retiene {detalle.pedido2.retiene}, espera {detalle.pedido2.espera}</p>
    {#if detalle.resuelto}
      <p>Resuelto. Ganador: {detalle.ganador}</p>
    {/if}
  {:else}
    <div class="alert alert-success">Sin interbloqueo activo</div>
  {/if}
  <button on:click={inducir} class="btn btn-danger me-2">Inducir</button>
  <input bind:value={ganador} placeholder="ID ganador" class="form-control mb-2" />
  <button on:click={resolver} class="btn btn-success">Resolver</button>
</div>
