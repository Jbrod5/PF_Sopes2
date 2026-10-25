<script>
  export let datos = null;
</script>

<div class="container mt-3">
  <h4>Lista de Espera</h4>
  {#if datos}
    <p>Criterio de orden: {datos.politica} | En espera: {datos.enEspera}</p>
    {#if datos.pedidos.length === 0}
      <p>Sin pedidos en espera.</p>
    {:else}
      {#each datos.pedidos as p}
        <div class="card p-2 mb-2">
          <h6>{p.posicion}. {p.id} - {p.cliente}</h6>
          <p>Nivel: {p.nivel} | Articulos: {p.articulos} | Espera: {p.esperaMs} ms</p>
        </div>
      {/each}
    {/if}
  {:else}
    <p>Cargando lista de espera...</p>
  {/if}
</div>
