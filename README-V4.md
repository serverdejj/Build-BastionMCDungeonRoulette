# BastionMCDungeonRoulette V4 — Instalación

Compatible con **Minecraft 1.21.11 / Purpur** y requiere **BastionDungeons con startRouletteDungeon**.

## Mejoras

- 7 tarjetas visibles por defecto: 2 a la izquierda, mapa central y 4 a la derecha (más una tarjeta de entrada temporal al desplazarse).
- Desplazamiento a la derecha con desaceleración, separación y escala configurables.
- El ganador **real** termina en el marcador central y se queda inmóvil durante 60 ticks (3 segundos).
- Al desaparecer la ruleta, se anuncia `Dungeon <nombre>` y su descripción a los jugadores próximos.
- Todos los textos, nombres, descripciones, tiempos y posiciones editables en `config.yml`.
- `/droulette set facil|normal|dificil`, `/droulette remove` y `/droulette reload` (permiso `bastionroulette.admin`, OP por defecto).

## GitHub Actions

Sube al repositorio los archivos y carpetas del ZIP (dentro de la carpeta de proyecto).
Ve a **Actions > Build BastionMCDungeonRoulette > Run workflow** o realiza un push a `main`.
Descarga el artifact `BastionMCDungeonRoulette-JAR` y extrae el `.jar`.
Apaga el servidor y reemplaza **solo** el `.jar` de `plugins/`.

## IMPORTANTE: conservar la configuración de los bloques

**NO BORRES** `plugins/BastionMCDungeonRoulette/config.yml` ni la carpeta del plugin.
El plugin carga las nuevas opciones del config por defecto del JAR cuando faltan en el archivo existente, pero si quieres editarlas explícitamente, incorpora los valores del nuevo `src/main/resources/config.yml` al tuyo, **conservando `blocks:`** (contiene tus ubicaciones).

Después de cambiar la configuración en el host, usa `/droulette reload` (solo OP). Si la ruleta estuviera girando, el reload la cancela limpiamente.

No requiere cambiar el resource pack de imágenes ni el JAR de Dungeons si ya tiene la integración de ruleta.

## Ajustes sugeridos

```yaml
roulette:
  display-height: 1.40
  image-scale: 0.72
  card-spacing: 1.18
  cards-left: 2
  cards-right: 4
  winner-pause-ticks: 60
```

Las tarjetas se muestran sobre el bloque seleccionado. La propiedad `name` es el nombre visible, `description` el subtítulo y `id` el ID de mapa que recibe Dungeons.
