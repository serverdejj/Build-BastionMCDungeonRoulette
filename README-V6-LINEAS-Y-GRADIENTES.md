# BastionMCDungeonRoulette V6 — textos por línea

Cada `roulette.maps[].hologram.lines[]` acepta objetos YAML individuales:

```yaml
hologram:
  height: 0.0
  offset-x: 0.0
  offset-y: 0.0
  offset-z: 0.0
  lines:
    - text: '&#2529A7&le&#2822A6&lj&#2C1CA6&le&#2F15A5&lm&#3420AF&lp&#3A2CBA&ll&#3F37C4&lo'
      height: 0.92
      scale: 0.72
      offset-x: 0.0
      offset-y: 0.0
      offset-z: 0.0
    - text: '&f&l{name}'
      height: 0.44
      scale: 0.60
      offset-x: 0.0
      offset-y: 0.0
      offset-z: 0.0
    - text: '&7Mata a el jefe para ganar'
      height: 0.03
      scale: 0.38
      offset-x: 0.0
      offset-y: 0.0
      offset-z: 0.0
```

`height` es la altura propia de la línea sobre la base del holograma; no depende de las otras líneas.
`scale` cambia su tamaño. `offset-x`, `offset-y`, `offset-z` desplazan solo esa línea.
Cada mapa tiene su propia lista; puedes configurarlas de modo diferente para cada mapa.
La sintaxis `&#RRGGBB&l` admite degradado artesanal carácter a carácter. El uso de `&l` tras cada color mantiene negrita.
Las líneas antiguas en formato `- '&fTexto'` siguen siendo compatibles y usan `line-spacing`.

Para instalar: sube el proyecto a GitHub y compila el jar; después actualiza el `config.yml` guardando copia de tus `blocks` si los cambiaste. Ejecuta `/droulette reload`.
