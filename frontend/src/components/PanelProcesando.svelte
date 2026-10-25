<script>
  export let datos = [];
</script>

<div class="container mt-3">
  <h4>Pedidos en Proceso</h4>
  {#if datos.length === 0}
    <p>Sin pedidos en proceso.</p>
  {:else}
    {#each datos as p}
      <div class="card p-2 mb-2">
        <h6>{p.id} - {p.cliente}</h6>
        <p>Nivel: {p.nivel} | Articulos: {p.articulos} | Estado: {p.estado}</p>
        <p>Progreso: {p.progreso}% | Restante: {p.restanteMs} ms | Espera: {p.esperaMs} ms</p>
        <div class="progress">
          <div class="progress-bar" style="width: {p.progreso}%"></div>
        </div>
      </div>
    {/each}
  {/if}
</div>
