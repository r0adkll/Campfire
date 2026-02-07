---
name: create-subproject
description: Generate a new set of modules, i.e. a Subproject, in a given directory following a prescribed format
argument-hint: [name] [path] [--no-ui]
---

## Additional resources

- When generating modules, follow [MODULARIZATION.md](../../../docs/architecture/MODULARIZATION.md)

## Generate subproject

Using $1 as the root directory, follow this plan

1. Generate directories per the modularization guildlines
   a. If $2 is present, DO NOT generate the `ui` module and ignore following instructions related to that module
2. Generate the `build.gradle.kts` file for each module
3. Generate the src directories for each, all following the same path `src/commonMain/kotlin`
4. Generate the package directories in the previous source directory, follow the pattern `app/campfire/$0/{{module type}}`
   a. Add a `.gitkeep` file as placeholders to make it recognizable by Git
5. Add the new modules to `settings.gradle.kts`, using a single, separate, `include(…)` statement
6. Add the `impl` module as a dependency in `app/common/build.gradle.kts`
7. If `ui` module was added, also add it to `app/common/build.gradle.kts`


