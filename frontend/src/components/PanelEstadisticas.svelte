<script>
  export let datos = null;
</script>

<div class="container mt-3">
  <h4>Estadisticas del Sistema</h4>
  {#if datos}
    <p>Pedidos completados: {datos.completados}</p>
    <p>Espera promedio: {datos.esperaPromedioMs} ms</p>
    <p>Proceso promedio: {datos.procesoPromedioMs} ms</p>
    <p>En espera: {datos.enEspera}</p>
    <p>En proceso: {datos.enProceso}</p>
  {:else}
    <p>Cargando estadisticas...</p>
  {/if}
</div>
