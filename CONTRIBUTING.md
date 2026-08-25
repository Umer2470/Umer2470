# Contributing Guidelines

Thank you for contributing to the CH UMER POS project. To maintain project stability, high security, and clean CI/CD builds, please adhere to the following development practices.

---

## 1. CI/CD Best Practices

### Official Gradle Wrapper Requirement
All repository builds, including local builds and GitHub Actions pipelines (`gradle/actions/wrapper-validation@v4`), strictly require official, untampered Gradle Wrapper JARs.
- **Never manually edit or substitute** binary wrapper JARs (`gradle/wrapper/gradle-wrapper.jar`).
- **Never download unofficial wrapper JARs** from third-party sources or external mirrors.
- If upgrading or regenerating the Gradle Wrapper, always use the authentic Gradle toolchain command:
  ```bash
  gradle wrapper --gradle-version <VERSION> --distribution-type bin
  ```
- Ensure that the wrapper SHA-256 checksum matches Gradle's officially published checksum database before committing changes.

### Uncompromised Security & Integrity
- Do not bypass, disable, or relax Gradle wrapper validation in CI workflows (`.github/workflows/*.yml`).
- Keep license activation workflows and cryptographic validation logic completely intact.
- Maintain offline-first local persistence architecture (Room database) for all core POS workflows.

---

## 2. Local Build Validation

Before pushing any commits or opening pull requests, verify your changes locally using the Gradle wrapper with the following sequential validation commands:

### Step 1: Verify Gradle Wrapper Version
Ensure the Gradle runtime version is aligned with the project's Android Gradle Plugin configuration:
```bash
./gradlew --version
```

### Step 2: Run Unit & JVM Robolectric Tests
Verify all business logic, POS calculations, licensing checks, and invoice formatting tests pass:
```bash
./gradlew :app:testDebugUnitTest
```

### Step 3: Verify Debug Assembly
Confirm that debug builds assemble without syntax, manifest, or resource compilation errors:
```bash
./gradlew :app:assembleDebug
```

### Step 4: Verify Release Assembly & ProGuard/R8 Minification
Verify that R8 rules and code shrinking succeed for production release builds:
```bash
./gradlew :app:assembleRelease
```

### Step 5: Verify Release App Bundle (AAB)
Ensure production Android App Bundle packaging succeeds:
```bash
./gradlew :app:bundleRelease
```

---

## 3. Code Standards & Architecture Guidelines

- **Kotlin & Jetpack Compose**: Use modern idiomatic Kotlin with Jetpack Compose for UI.
- **Offline-First Resilience**: All checkout, inventory, billing, thermal ESC/POS printing, and attendance functions must operate locally without network dependency.
- **Module Separation**: Keep UI and state management clearly decoupled between distinct feature packages (e.g., `com.example.ui.invoice` and `com.example.ui.attendance`).
