<script>
  import { onMount } from "svelte";
  import { obtenerClientes, registrarCliente } from "../services/servicioApi.js";

  let clientes = [];
  let id = "";
  let nombre = "";
  let correo = "";
  let mensaje = "";
  let exito = true;

  async function cargar() {
    try {
      clientes = await obtenerClientes();
    } catch (e) {
      exito = false;
      mensaje = "No se pudo cargar el registro de clientes";
    }
  }

  onMount(cargar);

  async function enviar() {
    mensaje = "";
    try {
      const respuesta = await registrarCliente({ id, nombre, correo });
      if (respuesta.error) {
        exito = false;
        mensaje = respuesta.error;
        return;
      }
      exito = true;
      mensaje = "Cuenta registrada: " + respuesta.id;
      id = "";
      nombre = "";
      correo = "";
      await cargar();
    } catch (e) {
      exito = false;
      mensaje = "Error de conexion con el servidor";
    }
  }
</script>

<div class="card tarjeta-panel h-100">
  <div class="card-header bg-primary text-white d-flex justify-content-between align-items-center">
    <span>Cuentas de Clientes</span>
    <span class="badge bg-light text-dark">{clientes.length} registradas</span>
  </div>
  <div class="card-body">
    <form on:submit|preventDefault={enviar}>
      <div class="row g-2">
        <div class="col-4">
          <input bind:value={id} placeholder="ID (ej. CLI-010)" class="form-control" />
        </div>
        <div class="col-4">
          <input bind:value={nombre} placeholder="Nombre del cliente" class="form-control" />
        </div>
        <div class="col-4">
          <input bind:value={correo} placeholder="Correo electronico" class="form-control" />
        </div>
      </div>
      <button type="submit" class="btn btn-success w-100 mt-2">Crear cuenta</button>
    </form>
    {#if mensaje}
      <div class="alert {exito ? 'alert-success' : 'alert-danger'} mt-2 mb-0 py-2">{mensaje}</div>
    {/if}
    {#if clientes.length > 0}
      <ul class="list-group mt-2">
        {#each clientes as c}
          <li class="list-group-item d-flex justify-content-between align-items-center py-1">
            <span><strong>{c.id}</strong> - {c.nombre}</span>
            <small class="text-muted">{c.correo}</small>
          </li>
        {/each}
      </ul>
    {/if}
  </div>
</div>
