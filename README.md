# MoneyTrack

## Arquitectura
Este proyecto sigue los principios de **Clean Architecture** (Arquitectura Limpia) junto con el patrón **MVVM**.

### Estructura de Paquetes
- `data`: Orígenes de datos (Room, Retrofit).
    - `local`: Base de datos Room, Entidades y DAOs.
    - `remote`: Servicios de Retrofit (Para futura integración con APIs).
    - `repository`: Implementación concreta de los repositorios definidos en el Dominio.
- `domain`: Lógica de negocio pura (independiente de la plataforma).
    - `model`: Clases de datos (POJOs) de Kotlin.
    - `repository`: Interfaces de los repositorios.
    - `usecase`: Casos de uso específicos de la lógica de negocio (Próximamente).
- `ui`: Capa de presentación (Activities, Fragments, ViewModels).
- `di`: Inyección de dependencias (Módulos de Hilt).

## Lógica Futura
- **Lógica de Negocio**: Coloca la lógica compleja en los **Casos de Uso** (Use Cases) dentro del paquete `domain/usecase`.
- **Lógica de Datos**: Implementa las interfaces de los repositorios en `data/repository`.
- **Lógica de UI**: Utiliza los **ViewModels** en los paquetes de `ui` para gestionar el estado y los eventos de la interfaz.

## Configuración Inicial
1. Sincroniza Gradle para descargar las dependencias.
2. Compila el proyecto para generar el código automático de Hilt y Room.
3. Ejecuta en un emulador o dispositivo físico.