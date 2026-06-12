---
name: logical-commit
description: Inspect local git changes, split them into independent logical commits, and write high-quality Chinese Conventional Commit messages. Use when the user asks to commit current local changes, batch commits, split commits by intent, preserve existing worktree changes, or follow commit best practices.
---

# Logical Commit

Use this skill to turn an existing dirty worktree into one or more clean, reviewable commits.

## Workflow

1. Inspect repository guidance before touching the index:
   - Read the nearest `AGENTS.md` files that apply to changed paths.
   - Run `git status --short --untracked-files=all`, `git diff --stat`, and, when useful, recent `git log --oneline`.
   - Check both unstaged and staged changes with `git diff` and `git diff --cached`.

2. Identify logical commit groups:
   - Group by independently understandable intent, not by directory alone.
   - Prefer commits that can be reviewed and reverted independently.
   - Keep generated output, dependency install directories, build artifacts, and unrelated local files out of commits unless the repo explicitly tracks them.
   - Preserve user staging intent when possible, but reorganize the index when the user asked for split commits.
   - Use partial staging for mixed files when unrelated edits appear in the same file.

3. Validate each group before committing:
   - Stage only the files or hunks for that group.
   - Review `git diff --cached --stat`.
   - Review the cached diff when the group is non-trivial.
   - If a path is ignored but tracked, use an update-only staging approach such as `git add -u path`.

4. Write Chinese Conventional Commit messages:
   - Subject format: `type(scope): 动词开头的简短中文说明`.
   - Omit `scope` only when a broad repository-wide scope is clearer.
   - Use imperative, concrete wording.
   - Add a body for multi-file or behavior-changing commits, with concise Chinese bullets.
   - Mention what changed and why it belongs in this logical group, not implementation noise.

5. Commit sequentially:
   - Commit one group at a time.
   - Re-check `git status --short --untracked-files=all` after each commit.
   - Continue until all intended changes are committed or only intentionally ignored/untracked files remain.

6. Finish with evidence:
   - Show the new commits with `git log --oneline -N`.
   - Confirm whether the worktree is clean.
   - If verification commands were run, report them; if not, say so.

## Logical Grouping Heuristics

Use these boundaries when deciding commit granularity:

- `docs`: repository instructions, README updates, user-facing documentation.
- `feat`: new user-visible capability, new module, new route, new API behavior.
- `fix`: behavior correction, missing runtime dependency, broken configuration.
- `refactor`: code structure changes without behavior changes.
- `test`: tests or test fixtures only.
- `chore(build)`: Gradle, package manager, CI, wrapper, IDE build metadata.
- `chore(deps)`: dependency version or lockfile maintenance without feature work.

Split commits when:

- One part can be reverted without reverting the other.
- One part changes build wiring and another adds product code.
- Documentation or contributor instructions are independent of implementation.
- Backend protocol changes and frontend consumption can be reviewed separately.

Keep together when:

- A source change and its exact test update express one behavior change.
- A lockfile update only records the dependencies introduced by the same feature.
- A small config file is necessary for the new module to function.

## Message Examples

```text
feat(frontend): 添加 Vue Web 客户端

- 新增 Vue 3、Vite、Pinia 和 TDesign 前端工程
- 添加 WebSocket API 客户端和连接状态管理
- 实现工作区页面的后端连接和类型列表加载
```

```text
chore(build): 接入前端 Gradle 模块

- 在 settings 中包含 frontend 模块
- 让根 build 和 check 聚合前后端任务
- 更新 IDE 项目模块配置
```

```text
fix(server): 调整 Mindustry 运行依赖配置

- 将 Mindustry 和 Arc 依赖改为 implementation
- 移除测试中重复声明的运行库依赖
```
