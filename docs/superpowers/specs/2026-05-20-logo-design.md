# MoneyTrack Logo / App Icon — Design Spec

**Fecha:** 2026-05-20  
**Concepto aprobado:** B — Escudo con checkmark  
**Mensaje visual:** "Confianza y control"

---

## Concepto

Ícono minimalista: un escudo semitransparente con un checkmark sólido en blanco, sobre fondo degradado azul–violeta. Reutiliza exactamente la paleta Clean Blue ya implementada (`bg_balance_card`), creando coherencia visual entre el ícono del launcher y la interfaz interior de la app.

---

## Formato: Android Adaptive Icon

El sistema de adaptive icons de Android (API 26+) separa el ícono en dos capas:

- **Background layer** (`ic_launcher_background.xml`): el degradado. Puede ser recortado por el launcher a cualquier forma (círculo, squircle, cuadrado redondeado).
- **Foreground layer** (`ic_launcher_foreground.xml`): el símbolo. Debe diseñarse dentro de la "safe zone" interior (72dp de los 108dp totales) para que no quede recortado en ninguna forma.

Para Android < 26 se proveen PNG de fallback en las 5 densidades estándar.

---

## Colores

| Token | Valor | Uso |
|-------|-------|-----|
| `blue_700` | `#1D4ED8` | Color inicial del degradado |
| `violet_700` | `#7C3AED` | Color final del degradado |
| Blanco puro | `#FFFFFF` | Escudo y checkmark |

El ángulo del degradado es 135° (diagonal arriba-izquierda → abajo-derecha), idéntico a `bg_balance_card.xml`.

---

## Geometría del foreground

Canvas total: 108dp × 108dp (estándar adaptive icon)  
Safe zone: círculo de 72dp de diámetro centrado (18dp de margen en cada lado)  
El escudo y el check se diseñan dentro de ~60dp efectivos para dejar respiración.

**Escudo**: path SVG clásico, relleno blanco al 20% de opacidad + stroke blanco al 100%, grosor 2.5dp.  
**Checkmark**: trazo blanco sólido, stroke-width 3.5dp, extremos redondeados (`round` linecap/linejoin).  
Proporciones tomadas del concepto B mostrado en la sesión de brainstorming.

---

## Archivos a crear / modificar

| Acción | Archivo |
|--------|---------|
| Crear | `app/src/main/res/drawable/ic_launcher_background.xml` |
| Crear | `app/src/main/res/drawable/ic_launcher_foreground.xml` |
| Crear | `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` |
| Crear | `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml` |
| Reemplazar | `app/src/main/res/mipmap-mdpi/ic_launcher.png` (48×48 px) |
| Reemplazar | `app/src/main/res/mipmap-hdpi/ic_launcher.png` (72×72 px) |
| Reemplazar | `app/src/main/res/mipmap-xhdpi/ic_launcher.png` (96×96 px) |
| Reemplazar | `app/src/main/res/mipmap-xxhdpi/ic_launcher.png` (144×144 px) |
| Reemplazar | `app/src/main/res/mipmap-xxxhdpi/ic_launcher.png` (192×192 px) |
| Reemplazar | `app/src/main/res/mipmap-mdpi/ic_launcher_round.png` |
| Reemplazar | `app/src/main/res/mipmap-hdpi/ic_launcher_round.png` |
| Reemplazar | `app/src/main/res/mipmap-xhdpi/ic_launcher_round.png` |
| Reemplazar | `app/src/main/res/mipmap-xxhdpi/ic_launcher_round.png` |
| Reemplazar | `app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png` |

Los PNG de fallback se generan con el script de Python incluido en el plan de implementación (requiere `cairosvg` o `Pillow`+`svglib`). Si no hay entorno Python disponible, se generan con Android Studio Image Asset Studio (Vector Asset → Image Asset).

---

## AndroidManifest.xml

No requiere cambios: ya referencia `@mipmap/ic_launcher` y `@mipmap/ic_launcher_round`. Al reemplazar los archivos, el nuevo ícono se aplica automáticamente.

---

## Verificación

1. `./gradlew assembleDebug` → BUILD SUCCESSFUL (los XML deben ser válidos)
2. Instalar en emulador/dispositivo → ícono visible en launcher
3. Probar en launcher circular (Pixel) y cuadrado redondeado (Samsung) → sin recortes en el símbolo
4. Verificar que no aparece el ícono verde genérico de Android

---

## Decisiones de diseño

- **Sin wordmark en el ícono**: el nombre "MoneyTrack" aparece debajo del ícono en el launcher de forma nativa — añadirlo al ícono lo haría ilegible a 48dp.
- **Gradiente en background, no en foreground**: permite que el launcher aplique efectos (sombra, parallax) correctamente.
- **Opacidad del escudo al 20%**: suficiente para dar forma sin competir con el checkmark.
