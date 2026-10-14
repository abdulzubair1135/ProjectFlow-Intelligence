# Contributing Guide

## Development Workflow
1. **Branching**: Create feature branches from `main` using `feature/<topic>` or `fix/<topic>`.
2. **Local Verification**: Before pushing or creating a pull request, execute:
   ```bat
   TEST_PROJECTFLOW.bat
   ```
   All shared-core tests, backend integration tests, and TypeScript compilations must pass.
3. **Commit Messages**: Follow Conventional Commits:
   - `feat(risk): add capacity overload rule threshold`
   - `fix(desktop): resolve table column layout constraint`
   - `docs: update deployment runbook for Ubuntu 22.04`
4. **Code Review**: Pull requests require review and clean CI pipeline passes.
