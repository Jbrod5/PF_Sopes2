<script>
  import { registrarPedido, cambiarPolitica } from "../services/servicioApi";
  let nombre = "";
  let nivel = "ESTANDAR";

  async function enviar() {
    await registrarPedido({ nombre, nivel });
  }

  async function actualizarPolitica() {
    await cambiarPolitica(nivel);
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
    </select>
    <button type="submit" class="btn btn-primary">Registrar pedido</button>
  </form>
</div>
