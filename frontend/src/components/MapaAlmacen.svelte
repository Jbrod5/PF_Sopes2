<script>
  import { onMount, onDestroy } from "svelte";
  import { obtenerAlmacen } from "../services/servicioApi.js";

  export let celdas = [];

  let resumen = null;
  let intervalo = null;

  function agruparPorPasillo(lista) {
    const grupos = [];
    for (const c of lista) {
      let grupo = grupos.find((g) => g.pasillo === c.pasillo);
      if (!grupo) {
        grupo = { pasillo: c.pasillo, niveles: [] };
        grupos.push(grupo);
      }
      let nivel = grupo.niveles.find((n) => n.nivel === c.nivel);
      if (!nivel) {
        nivel = { nivel: c.nivel, celdas: [] };
        grupo.niveles.push(nivel);
      }
      nivel.celdas.push(c);
    }
    return grupos;
  }

  $: grupos = agruparPorPasillo(celdas);

  async function cargar() {
    try {
      const datos = await obtenerAlmacen();
      celdas = datos.celdas || [];
      resumen = datos;
    } catch (e) {
      console.error("Error cargando almacen:", e);
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
  <div class="card-header bg-primary text-white d-flex justify-content-between align-items-center">
    <span>Mapa del Almacen</span>
    {#if resumen}
      <span>
        <span class="badge bg-success me-1">{resumen.espaciosOcupados} ocupadas</span>
        <span class="badge bg-light text-dark">{resumen.espaciosDisponibles} libres / {resumen.capacidadTotal}</span>
      </span>
    {/if}
  </div>
  <div class="card-body">
    <div class="mb-2">
      <span class="badge bg-success me-2">Ocupada (lote)</span>
      <span class="badge bg-light text-dark border">Libre</span>
    </div>
    <div class="row g-3">
      {#each grupos as g}
        <div class="col-md-6 col-xl-3">
          <div class="border rounded p-2 h-100">
            <h6>Pasillo {g.pasillo}</h6>
            {#each g.niveles as n}
              <div class="d-flex flex-wrap align-items-center mb-1">
                <small class="text-muted me-1">N{n.nivel}:</small>
                {#each n.celdas as c}
                  <div
                    title={c.ocupada ? "Lote " + c.lote : "Libre"}
                    class="celda-almacen border rounded text-center me-1 mb-1 p-1 {c.ocupada ? 'bg-success text-white' : 'bg-light text-muted'}"
                  >
                    {c.ocupada ? c.lote : "Libre"}
                  </div>
                {/each}
              </div>
            {/each}
          </div>
        </div>
      {/each}
    </div>
    {#if grupos.length === 0}
      <p class="text-muted mb-0">Cargando mapa del almacen...</p>
    {/if}
  </div>
</div>
