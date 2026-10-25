<script>
  import { registrarPedido, cambiarPolitica } from "../services/servicioApi.js";
  let nombre = "";
  let nivel = "ESTANDAR";
  let politica = "NIVEL_SERVICIO_ESTRICTO";
  let contador = 1;

  async function enviar() {
    const id = "WEB-" + contador;
    contador = contador + 1;
    await registrarPedido({ id, cliente: nombre, nivel });
    nombre = "";
  }

  async function actualizarPolitica() {
    await cambiarPolitica(politica);
  }
</script>

<div class="container mt-3">
  <h4>Panel de Pedidos</h4>
  <form on:submit|preventDefault={enviar}>
    <input bind:value={nombre} placeholder="Nombre pedido" class="form-control mb-2" />
    <select bind:value={nivel} class="form-select mb-2">
      <option>ESTANDAR</option>
      <option>EXPRES</option>
      <option>PRIORITARIO</option>
      <option>PROGRAMADO</option>
      <option>ECONOMICO</option>
    </select>
    <button type="submit" class="btn btn-primary">Registrar pedido</button>
  </form>
  <h4 class="mt-3">Politica de Atencion</h4>
  <select bind:value={politica} class="form-select mb-2">
    <option value="NIVEL_SERVICIO_ESTRICTO">Nivel de servicio estricto</option>
    <option value="MENOR_CANTIDAD_ARTICULOS">Menor cantidad de articulos</option>
    <option value="TIEMPO_ESPERA_FIFO">Tiempo de espera FIFO</option>
  </select>
  <button on:click={actualizarPolitica} class="btn btn-secondary">Cambiar politica</button>
</div>
