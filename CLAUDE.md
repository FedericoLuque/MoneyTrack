# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

MoneyTrack is a native Android app (Kotlin) for personal finance tracking with Bitcoin portfolio management. It targets SDK 26–34 and uses Java 17.

## Build Commands

```bash
./gradlew assembleDebug          # Build debug APK
./gradlew installDebug           # Build and install on connected device/emulator
./gradlew test                   # Run unit tests
./gradlew connectedAndroidTest   # Run instrumented tests
./gradlew lint                   # Run Android lint
./gradlew clean                  # Clean build artifacts
```

After modifying Room entities or Hilt modules, a full rebuild may be needed (`./gradlew clean assembleDebug`).

## Architecture

**Clean Architecture + MVVM** with three strict layers:

- **`domain/`** — Pure Kotlin. Models, repository interfaces, and use cases. No Android dependencies.
- **`data/`** — Room database (`moneytrack_db`), Retrofit (CoinGecko API), and repository implementations. Entity classes here map to/from domain models.
- **`ui/`** — Fragments, ViewModels (`@HiltViewModel`), and RecyclerView adapters. Uses ViewBinding. ViewModels expose `StateFlow`/`SharedFlow`.
- **`di/`** — Hilt modules: `AppModule` (database/DAOs), `NetworkModule` (Retrofit), `RepositoryModule` (binds interfaces to implementations).

**Key rule**: domain layer must never depend on data or ui layers. Data flows: UI → ViewModel → UseCase → Repository Interface (domain) → Repository Impl (data) → DAO/API.

## Database

Room database with 5 entities: `Account`, `Category`, `Transaction`, `Budget`, `BitcoinHolding`. Room schema exports to `app/schemas/`. Foreign keys enforce referential integrity (Transaction→Account CASCADE, Transaction→Category SET NULL). `DataSeeder` populates default accounts and categories on first launch.

## Key Patterns

- **Use cases** encapsulate complex business logic (e.g., `AddTransactionUseCase` updates both the transaction record and the account balance atomically).
- **Separate entity/model classes** — Room entities in `data/local/entity/`, domain models in `domain/model/`, with mapping between them.
- **CoinGecko API** (`api.coingecko.com/api/v3/simple/price`) provides real-time Bitcoin prices in USD/EUR.
- Navigation uses a single-Activity architecture (`MainActivity`) with `money_graph.xml` nav graph and bottom navigation (Dashboard, Accounts, Bitcoin).

## Language

The codebase, comments, UI strings, and commit messages are in **Spanish**.
