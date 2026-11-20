# BASTION MC — Ruleta de mazmorras

Requisitos: Purpur 1.21.11, Java 21, BastionDungeons actualizado con `startRouletteDungeon`.

## Compilación
Subir el proyecto a un repositorio GitHub y ejecutar `gradle build` con Java 21.
Instalar los JAR: `BastionDungeons` (versión integrada) y `BastionMCDungeonRoulette`.

## Recursos
Activar el pack `BASTION_MC_Ruleta_Recursos_1.21.11.zip` en Minecraft. Contiene las 3 capturas reales entregadas por el usuario; el paquete puede combinarse después con otros packs de BASTION MC. Para todos los jugadores, alojar el pack y configurarlo como pack del servidor.

## Configuración
En el spawn, mirar el bloque que funcionará como portal y ejecutar:

- `/droulette set facil`
- `/droulette set normal`
- `/droulette set dificil`

Los tres usan las mismas capturas y los tres mapas en `config.yml`; cada bloque define la dificultad.
El líder del equipo activa la ruleta con clic derecho sobre el bloque; se hace una selección aleatoria ponderada y las imágenes pasan de lado a lado antes de iniciar la partida.

## Importante
Este código no ha sido compilado ni verificado en Minecraft. Haz copia de seguridad del plugin de Dungeons y prueba primero en un servidor de ensayo.
`/dungeon go` sigue disponible para compatibilidad; la votación no aparece **al entrar desde los bloques**. Si quieres eliminarla incluso en `/dungeon go`, se requiere deshabilitar ese acceso por separado.
La ruleta no muestra nombres durante el giro; tras decidir, comunica el nombre del mapa seleccionado en chat.
