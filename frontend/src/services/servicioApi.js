// funciones fetch para interactuar con los endpoints REST

export async function obtenerEstado() {
  const res = await fetch("http://localhost:7070/api/estado");
  return res.json();
}

export async function registrarPedido(datos) {
  const res = await fetch("http://localhost:7070/api/pedidos", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(datos),
  });
  return res.text();
}

export async function cambiarPolitica(politica) {
  const res = await fetch("http://localhost:7070/api/politica", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ politica }),
  });
  return res.text();
}

export async function inducirDeadlock() {
  const res = await fetch("http://localhost:7070/api/deadlock/inducir", {
    method: "POST",
  });
  return res.text();
}

export async function resolverDeadlock(id) {
  const res = await fetch("http://localhost:7070/api/deadlock/resolver", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ id }),
  });
  return res.text();
}

// obtener recursos con tenedores actuales
export async function obtenerRecursos() {
  const res = await fetch("http://localhost:7070/api/recursos");
  return res.json();
}

// obtener pedidos en procesamiento con progreso
export async function obtenerProcesando() {
  const res = await fetch("http://localhost:7070/api/procesando");
  return res.json();
}

// obtener lista de espera con criterio de orden
export async function obtenerCola() {
  const res = await fetch("http://localhost:7070/api/cola");
  return res.json();
}

// obtener estadisticas generales del sistema
export async function obtenerEstadisticas() {
  const res = await fetch("http://localhost:7070/api/estadisticas");
  return res.json();
}

// obtener detalle del conflicto de recursos
export async function obtenerDetalleDeadlock() {
  const res = await fetch("http://localhost:7070/api/deadlock/detalle");
  return res.json();
}

// obtener catalogo de productos disponibles
export async function obtenerProductos() {
  const res = await fetch("http://localhost:7070/api/productos");
  return res.json();
}

// obtener estado del almacen paginado
export async function obtenerAlmacen() {
  const res = await fetch("http://localhost:7070/api/almacen");
  return res.json();
}

// registrar manualmente la llegada de mercaderia
export async function registrarMercaderia(producto, cantidad) {
  const res = await fetch("http://localhost:7070/api/recepcion", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ producto, cantidad }),
  });
  return res.json();
}

// obtener cuentas de clientes registrados
export async function obtenerClientes() {
  const res = await fetch("http://localhost:7070/api/clientes");
  return res.json();
}

// registrar una cuenta nueva de cliente
export async function registrarCliente(datos) {
  const res = await fetch("http://localhost:7070/api/clientes", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(datos),
  });
  return res.json();
}
