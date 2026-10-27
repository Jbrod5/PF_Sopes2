<script>
  import { onMount, onDestroy, tick } from "svelte";
  import { obtenerBitacora } from "../services/servicioApi.js";

  let eventos = [];
  let cantidad = 0;
  let intervalo = null;
  let contenedor = null;
  let seguir = true;

  async function cargar() {
    try {
      const datos = await obtenerBitacora();
      eventos = datos.eventos || [];
      cantidad = datos.cantidad || 0;
      if (seguir && contenedor) {
        await tick();
        contenedor.scrollTop = contenedor.scrollHeight;
      }
    } catch (e) {
      console.error("Error cargando bitacora:", e);
    }
  }

  onMount(() => {
    cargar();
    intervalo = setInterval(cargar, 2000);
  });

  onDestroy(() => {
    if (intervalo) {
      clearInterval(intervalo);
    }
  });
</script>

<div class="card tarjeta-panel">
  <div class="card-header bg-dark text-white d-flex justify-content-between align-items-center">
    <span>Bitacora de la Simulacion</span>
    <span class="d-flex align-items-center gap-2">
      <span class="badge bg-light text-dark">{cantidad} eventos</span>
      <label class="form-check-label small mb-0">
        <input type="checkbox" bind:checked={seguir} class="form-check-input me-1" />Seguir
      </label>
    </span>
  </div>
  <div class="card-body p-2">
    <div
      bind:this={contenedor}
      class="bg-dark text-light rounded p-2"
      style="font-family: monospace; font-size: 0.8rem; max-height: 320px; overflow-y: auto;"
    >
      {#if eventos.length === 0}
        <span class="text-muted">Esperando eventos de la simulacion...</span>
      {:else}
        {#each eventos as e}
          <div>{e}</div>
        {/each}
      {/if}
    </div>
  </div>
</div>
