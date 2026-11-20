# BastionMCDungeonRoulette V7 — hologramas sin fondo por defecto

El fondo de las tres líneas del holograma es **transparente por defecto** (opacidad 0).

## Configuración

En `winner-hologram`:

```yaml
winner-hologram:
  background-opacity: 0
```

Opcionalmente, en cada mapa dentro de `roulette.maps[].hologram`:

```yaml
hologram:
  background-opacity: 0
```

Y para cada línea:

```yaml
lines:
  - text: '&#C084FC&lMapa Ganador'
    height: 0.92
    scale: 0.72
    background-opacity: 0
```

Rango de 0 (sin fondo) a 255 (opaco negro). Los ajustes de cada línea tienen prioridad sobre los del mapa, que tienen prioridad sobre el valor general. Si omites todos los valores, será transparente.

Los colores legacy, HEX y degradados por carácter continúan funcionando, al igual que `/droulette reload`, `/droulette remove` y las ubicaciones de los tres bloques.

**Instalación:** sube el contenido del ZIP al repositorio, compila con GitHub Actions y reemplaza el JAR; no necesitas cambiar tus coordenadas de `blocks`. Reinicia el servidor al instalar el nuevo JAR; después podrás aplicar modificaciones del YAML con `/droulette reload`.
