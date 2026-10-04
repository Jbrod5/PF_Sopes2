<script>
  import PanelRecursos from "./components/PanelRecursos.svelte";
  import PanelPedidos from "./components/PanelPedidos.svelte";
  import PanelDeadlock from "./components/PanelDeadlock.svelte";
  import MapaAlmacen from "./components/MapaAlmacen.svelte";
</script>

<div class="container-fluid">
  <div class="row">
    <div class="col-md-6">
      <PanelRecursos datos={[]} />
    </div>
    <div class="col-md-6">
      <PanelPedidos />
    </div>
  </div>
  <div class="row">
    <div class="col-md-6">
      <PanelDeadlock />
    </div>
    <div class="col-md-6">
      <MapaAlmacen celdas={[]} />
    </div>
  </div>
</div>
