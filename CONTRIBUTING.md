# Contributing to MindIO

Thanks for your interest in contributing to `mindio-server`!

## Contributor License Agreement

Before we can accept your contribution, you need to agree to our
[Individual Contributor License Agreement](CLA.md). This grants MindIO's maintainer the rights needed
to keep the project both open source (AGPL v3.0) and available under other licensing terms (e.g. the
commercial MindIO Pro license), without restricting your own rights to use your contribution however
you like.

You don't need to do anything in advance — when you open your first pull request, a bot will
automatically comment with instructions to sign the CLA by replying to the PR. This only needs to be
done once; your signature is shared across all `mindio-me` repositories.

## How to contribute

1. Fork this repository and create a branch from `main` for your change.
2. Run `./mvnw spring-boot:run` (or `.\mvnw.cmd spring-boot:run` on Windows) to start the API locally
   against a local MySQL `mindio_app` database (see the README's Development section for connection
   overrides).
3. Make your change, keeping it focused — small, single-purpose PRs review faster.
4. Open a pull request describing what changed and why.
5. Sign the CLA when the bot prompts you (first PR only).
6. Address review feedback; once approved and CI passes, we'll merge it.

## Reporting bugs / requesting features

Please open a GitHub issue with as much detail as possible: steps to reproduce, expected vs. actual
behavior, and your environment (OS, Java version, MySQL version).
