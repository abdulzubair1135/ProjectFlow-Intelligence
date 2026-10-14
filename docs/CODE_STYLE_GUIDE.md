# Code Style Guide

## Java (Shared Core, Backend, Desktop, Android)
- **Formatting**: Google Java Style Guide with 4-space indentation.
- **Naming**: `CamelCase` for classes, `camelCase` for methods and variables, `UPPER_SNAKE_CASE` for constants.
- **Null Safety**: Avoid returning raw null collections; use `Collections.emptyList()` or `Optional<T>`.
- **Imports**: Avoid wildcard imports (`import java.util.*`); use explicit imports where feasible.

## TypeScript / React (Web Admin Console)
- **Strict Typing**: TypeScript `strict: true`. Avoid `any`; use typed domain interfaces.
- **Functional Components**: React functional components with hooks (`useState`, `useEffect`).
- **Styling**: Tailwind CSS utility classes following Natural Mineral design tokens.
- **Icons**: Use `lucide-react` components exclusively. Never use emojis for UI icons.
