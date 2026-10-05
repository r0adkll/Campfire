---
name: pr-stack
description: Open, describe, and maintain pull requests in Campfire, especially stacks of dependent PRs (each branch built on the one before). Use when opening a PR, splitting work into a series of PRs, restacking after a lower branch changes or merges, writing or updating PR descriptions, closing a superseded PR, or when asked for the "Stack" section.
---

# Pull requests and PR stacks

A stack is a chain, sometimes a tree, of PRs where each PR's base is the previous PR's branch. Reviewers can then read one concern at a time while the work keeps moving. Every PR in a stack carries a **Stack** section listing the whole stack.

## Before anything leaves the machine

- **Ask before pushing or opening PRs** unless the user said to go ahead. Building and committing locally needs no asking.
- **Check each PR on its own.** Every branch in the stack must pass by itself, not just the top one:
  - `./scripts/ktlint --check`;
  - the app compiles: `:app:desktop:compileKotlin`, `:app:android:compileAlphaDebugKotlin`, and iOS (`:app:ios:compileKotlinIosSimulatorArm64`) when shared or iOS code changed;
  - `./gradlew jvmTest test`.
  
  Never pipe Gradle into `tail`, because that hides the exit code.
- **Behavior changes get a device run.** Use the `campfire-device-tester` agent against the throwaway testbed, never a real server. For data or storage changes, include the upgrade path: install the parent branch's build, set non-default values, then `install -r` the new build.
- **Changelog** (see CLAUDE.md):
  - A feature keeps one entry across the whole stack, added by the PR where users first see the change.
  - A bug fix found along the way gets its own entry, in the PR that fixes it.
  - Refactors get no entry, and the PR says so.
- **Labels:** `ignore-db-change` for `.sq` changes that need no migration, and `skip-coverage` only when the user agrees. Add labels with `gh issue edit <n> --add-label ...`.

## Building the stack

- Branch each step from the previous one: `git switch -c <next-branch> <previous-branch>`. Name branches so they sort by step (`refactor/settings-async-1a`, `-1b`, `-1c`).
- Keep one concern per PR. A fix discovered on the way is its own PR, branched from where it applies, even if that makes the stack a tree.
- **Commit signing goes through 1Password and sometimes fails** with "failed to fill whole buffer". Tell the user and wait for them to say "retry". Never bypass signing (`--no-gpg-sign`), and never add `Co-Authored-By` trailers or session links.

## Writing the description

Lead with a short paragraph: what the PR does and why, and which issue it's part of (`Part of #607`). Then the sections that apply. Recent PRs use:

- **What changed:** the substance, grouped by area. Name the types and files reviewers should look at.
- **What didn't change**, for refactors: the behavior, keys or APIs that stayed the same.
- **Verification:** the exact checks you ran and their results, the on-device results, and anything you excluded and why (for example, native-library tests that can't run locally). Don't claim checks you didn't run.
- **Follow-ups:** known gaps, and links to the issues filed for them.

Put `Closes #N` in the **top** PR of the stack. GitHub only closes the issue when a PR merges into `main`, which happens once the whole stack has landed. End with the footer the session's attribution instructions give.

Don't write "Stacked on #N" prose. The Stack section says it, and stays true as the stack changes.

### The Stack section

Every open PR in a stack gets the same nested list, with each PR indented under its parent and `[This PR]` marking the PR being viewed. Bare `#N` references are enough, because GitHub renders them as rich links:

```markdown
## Stack

- #1189
  - #1195
    - [This PR] #1196
      - #1197
        - #1198
        - #1199
```

Don't write it by hand. [scripts/stack_section.py](scripts/stack_section.py) builds the tree from the open PRs' base and head branches, so it doesn't matter which PR you start from. It keeps the section between hidden `<!-- pr-stack:start -->` / `<!-- pr-stack:end -->` markers at the top of each body, replacing it in place on reruns:

```bash
python3 .claude/skills/pr-stack/scripts/stack_section.py 1196          # dry run: print each PR's section
python3 .claude/skills/pr-stack/scripts/stack_section.py 1196 --apply  # write the ones that changed
```

Run it after every change to the stack's shape: a PR opened, merged, closed or retargeted. When only one PR is left, it removes the section.

## Opening the stack

1. Push every branch: `git push -u origin <branch>`. A branch created from `origin/main` tracks `main` until you do this.
2. Open the PRs bottom-up, each with `--base` set to its parent's branch: `gh pr create --base <parent-branch> --head <branch> --title ... --body-file <file>`. Write bodies to files rather than inline heredocs.
3. Run `stack_section.py --apply` once all of them exist. It fills in every PR's Stack section, so you don't need to cross-link with placeholders.

## Editing PRs

`gh pr edit` fails in this repo (it queries the deprecated projectCards GraphQL field). Patch through the API instead (see [docs/agents/issue-tracker.md](../../../docs/agents/issue-tracker.md)):

```bash
gh api -X PATCH repos/r0adkll/Campfire/pulls/<n> -F body=@body.md   # description
gh api -X PATCH repos/r0adkll/Campfire/pulls/<n> -f base=<branch>   # retarget
gh pr ready <n> --undo                                              # back to draft
```

## Keeping the stack current

**A lower branch changed** (review fixes, a changelog entry, a rebase on `main`): restack every descendant, top-down from the changed branch, in order.

1. Before rewriting any branch, note each branch's old tip: `git rev-parse <branch>`.
2. Rebase each child onto its parent's new tip: `git rebase --onto <parent> <parent's old tip> <child>`.
   - A plain `git rebase <parent>` also works when the parent only gained commits. Git skips patches that are already applied, and prints a "skipped cherry-picks" hint.
3. Re-run each moved branch's checks.
4. Push with `git push --force-with-lease`. Ask first if the PRs are already under review.

**A PR merged.** `main` is squash-only and head branches are deleted on merge, so:
- GitHub retargets the merged PR's children to `main` by itself.
- Their branches still carry the merged PR's original commits, which won't match the squash commit.

Restack each child with `git fetch origin && git rebase --onto origin/main <merged branch's old tip> <child>`, then its descendants as above. Then run `stack_section.py --apply`, which drops the merged PR from every list.

**A PR is superseded.** Close it with a comment naming its replacement and what carried over (`gh pr close <n> --comment ...`). If it's still useful as a reference until the replacement lands, move it to draft instead, with a comment saying so.
