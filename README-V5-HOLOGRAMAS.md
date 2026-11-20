# BastionMCDungeonRoulette V5 — Hologramas por mapa

El holograma aparece justo al detenerse la ruleta, **durante la pausa del ganador**, y desaparece al finalizar esa pausa o cuando `duration-ticks` del holograma expire. Ya no se envía al chat ni como título salvo que actives `also-show-chat` o `also-show-title`.

## Configuración

En `winner-hologram`, `height` y `offset-x`, `offset-y`, `offset-z` establecen la ubicación general sobre la imagen ganadora; el centro del holograma es el centro de la ruleta. `line-spacing`, `scale`, `shadow`, `see-through`, `enabled` y `duration-ticks` controlan apariencia y duración. `duration-ticks: 0` coincide con toda la pausa del ganador. Un valor positivo no puede prolongar el holograma más allá de la pausa de la ruleta (`roulette.winner-pause-ticks`).

En cada entrada de `roulette.maps`, añade un bloque `hologram` con sus propias `lines`, `height`, `offset-x`, `offset-y`, `offset-z`, `line-spacing`, `scale`, `shadow`, `see-through` y `enabled`. `height` y offsets por mapa **se suman** a los ajustes generales.

Variables admitidas: `{name}` y `{map}` son el nombre visible de cada mapa, `{id}` es el identificador interno, y `{description}` es la descripción del mapa. Todas las líneas aceptan `&` y `&#RRGGBB`.

## Instalar

Sube el proyecto a GitHub y compila con Actions; reemplaza solo el JAR. Respalda el config actual, combina los nuevos bloques de configuración o usa el `config.yml` incluido. ¡No borres `blocks`! Después ejecuta `/droulette reload` como OP. No cambia BastionDungeons ni el resource pack.
