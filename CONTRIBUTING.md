# Contributing 🐀

## Philosophy

Krat is a collection of small, focused utilities — not a framework. Think of it like [shadcn/ui](https://ui.shadcn.com) but for backend Kotlin: **browse the code, copy what you like, and make it yours.**

You don't need to depend on a library for a 30-line utility. If a module solves your problem, use the Maven dependency. But if you only need a piece of it, or want to tweak it — just grab the code. There's no magic, no deep inheritance hierarchies, no hidden state. That's by design.

## Ways to contribute

### Found a bug or have an idea?

Open an [issue](https://github.com/jordi9/krat/issues). Keep it short — a failing test or a code snippet says more than a paragraph.

### Want to add a feature?

Open an issue first so it can be discussed. This avoids wasted effort on things that might not fit. Bug fixes for trivial or non-controversial things can skip this step.

### Want to send a PR?

1. Fork the repo
2. Create a branch from `main`
3. Set up git hooks: `git config core.hooksPath .githooks`
4. Make your changes
5. Run `./gradlew spotlessApply` then `./gradlew build`
6. Open a PR

Keep PRs small and focused. One concern per PR.

### Adding a new module

Follow the structure in [CLAUDE.md](CLAUDE.md#adding-a-new-module). Every module should be small enough that someone could read and understand it in a few minutes.

## Code style

- Run `./gradlew spotlessApply` before committing
- Write tests with [Kotest](https://kotest.io) and [kogiven](krat-kogiven/) for BDD scenarios
- Less is more — expose as little as possible
- Keep things simple — if a module needs a long README to explain, it's probably too complex
