# MoneyTrack

## Architecture
This project follows **Clean Architecture** with **MVVM**.

### Packages
- `data`: Data sources (Room, Retrofit).
    - `local`: Room Database, Entities, DAOs.
    - `remote`: Retrofit services (Future).
    - `repository`: Implementation of Domain Repositories.
- `domain`: Business logic.
    - `model`: Pure Kotlin data classes.
    - `repository`: Interfaces for repositories.
    - `usecase`: Business logic Use Cases (Future).
- `ui`: Presentation layer (Activities, Fragments, ViewModels).
- `di`: Dependency Injection (Hilt Modules).

## Future Logic
- **Business Logic**: Place complex logic in **Use Cases** within the `domain/usecase` package.
- **Data Logic**: Implement the Repository interfaces in `data/repository`.
- **UI Logic**: Use **ViewModels** in `ui` packages to handle UI state and events.

## Setup
1. Sync Gradle.
2. Build the project to generate Hilt and Room code.
3. Run on Emulator/Device.
