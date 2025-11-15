# Repository Guidelines

## Project Structure & Module Organization
The repo currently contains Gradle wrapper files plus the root build scripts (`build.gradle.kts`, `settings.gradle.kts`). Add production sources under `src/main/kotlin` (or `src/main/java`) and keep shared assets in `src/main/resources`. Mirror the package layout in `src/test/kotlin` so every class has an adjacent test twin; integration fixtures belong in `src/test/resources`. When the codebase grows, split features into Gradle subprojects inside `/modules/<feature-name>` and register them in `settings.gradle.kts` to keep the root slim.

## Build, Test, and Development Commands
- `./gradlew build` — default workflow: compiles, runs tests, and writes artifacts to `build/`.
- `./gradlew test` — executes only the unit tests; use this before every commit.
- `./gradlew clean build` — wipes stale class files before a release build.
- `./gradlew -t test` — continuous test run that re-executes on file changes; ideal for TDD.
- `./gradlew dependencies` — prints the dependency graph when validating new libraries.

## Coding Style & Naming Conventions
Adopt the official Kotlin style guide: 4-space indentation, 120-char line limit, PascalCase types, camelCase methods/properties, and UPPER_SNAKE_CASE constants. Keep files scoped to one responsibility and favor composition over shared singletons. Resource directories stay lowercase, Gradle module names use kebab-case, and package names should remain lowercase dot-separated structures. Reformat code before commits via IntelliJ’s “Reformat Code” or `./gradlew ktlintFormat` once linting is wired in.

## Testing Guidelines
Use JUnit 5 with AssertJ-style assertions. Test classes mirror the production type name plus a `Test` suffix (e.g., `UserServiceTest`). Within a class, prefer `fun methodUnderTest_condition_expectedResult()` naming. Target at least 80% line coverage and make sure error scenarios (timeouts, null payloads) have explicit tests. Run `./gradlew test` locally, then `./gradlew check` when adding detekt/ktlint or functional suites.

## Commit & Pull Request Guidelines
History is empty, so establish clarity early. Write imperative, ≤72-character subjects (e.g., `Add auth filter`), follow with wrapped body text that explains *what* and *why*, and reference issue IDs in the footer (`Refs #42`). Pull requests must summarize scope, list runnable artifacts or screenshots, describe testing performed, and link related issues. Request at least one peer review before merging to `main`.

## Environment & Configuration Tips
JDK 21 is the default toolchain; keep the Gradle wrapper on 9.2+ and avoid system Gradle. Never commit secrets—store them in `~/.gradle/gradle.properties` or CI secrets and document the variable names in the README. Use `.env.example` files for local configuration once environment variables are introduced.
