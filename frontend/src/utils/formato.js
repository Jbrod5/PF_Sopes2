// funciones de formato compartidas por los paneles del tablero

// clase bootstrap para el nivel de servicio
export function claseNivel(nivel) {
  if (nivel === "Expres") {
    return "bg-danger";
  }
  if (nivel === "Prioritario") {
    return "bg-warning text-dark";
  }
  if (nivel === "Estandar") {
    return "bg-primary";
  }
  if (nivel === "Programado") {
    return "bg-info text-dark";
  }
  return "bg-secondary";
}

// clase bootstrap para el estado del pedido
export function claseEstado(estado) {
  if (estado === "PROCESANDO") {
    return "bg-primary";
  }
  if (estado === "COMPLETADO") {
    return "bg-success";
  }
  if (estado === "CANCELADO") {
    return "bg-danger";
  }
  if (estado === "EN_CONFLICTO") {
    return "bg-danger";
  }
  if (estado === "EN_ESPERA") {
    return "bg-warning text-dark";
  }
  return "bg-secondary";
}

// formato legible de milisegundos
export function formatoTiempo(ms) {
  if (ms === null || ms === undefined) {
    return "-";
  }
  if (ms < 1000) {
    return ms + " ms";
  }
  const segundos = Math.round((ms / 1000) * 10) / 10;
  return segundos + " s";
}
