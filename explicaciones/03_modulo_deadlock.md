# Modulo 3: Deadlock / Interbloqueo

## Como se provoco deliberadamente el deadlock

Se uso un escenario controlado con dos pedidos y dos recursos en orden inverso. Cada pedido retiene su primer recurso y solicita el segundo, generando una espera circular. [cite: 1]

## Mapeo de recursos y procesos

El registro de asignacion mapea cada pedido con los recursos retenidos y los recursos esperados. El detector analiza los ciclos entre los procesos involucrados.

## Como funciona la despropiacion manual

Se selecciona un pedido ganador y se desapropia el perdedor, liberando sus recursos retenidos para romper la espera circular y restaurar la capacidad. [cite: 1]
