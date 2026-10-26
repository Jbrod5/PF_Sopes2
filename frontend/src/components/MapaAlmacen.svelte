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

<div class="container mt-3">
  <h4>Mapa Almacen</h4>
  {#if resumen}
    <p>Ocupadas: {resumen.espaciosOcupados} / Libres: {resumen.espaciosDisponibles} (Total: {resumen.capacidadTotal})</p>
  {/if}
  <div class="mb-2">
    <span class="badge bg-success me-2">Ocupada</span>
    <span class="badge bg-light text-dark border">Libre</span>
  </div>
  {#each grupos as g}
    <h6>Pasillo {g.pasillo}</h6>
    {#each g.niveles as n}
      <div class="d-flex flex-wrap mb-1">
        <span class="me-2">N{n.nivel}:</span>
        {#each n.celdas as c}
          <div
            title={c.ocupada ? c.lote : "Libre"}
            class="border p-1 text-center me-1 mb-1 {c.ocupada ? 'bg-success text-white' : 'bg-light'}"
            style="font-size: 0.7rem; min-width: 3rem;"
          >
            {c.ocupada ? c.lote : "Libre"}
          </div>
        {/each}
      </div>
    {/each}
  {/each}
</div>
