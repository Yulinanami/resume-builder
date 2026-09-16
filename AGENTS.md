<!-- author: jf -->
# AI 执行入口

本文件只保留后续会话容易猜错的仓库事实；完整规范在 `.workflow/specs/`。

## 必须遵守

1. 对话必须使用中文。
2. 处理本仓库任务前，必须先读取 `.workflow/specs/index.md`、`.workflow/specs/global.md` 和 `.workflow/specs/conventions.md`，再按索引读取与任务相关的 Spec。不要默认把全部 Spec 塞进上下文。
3. `.workflow/specs/` 下新增规范默认同样属于仓库级强制规范；如索引未及时更新，也不得绕过。`.rules/` 已废弃，禁止重建为平行规范入口。
4. 新增或修改文件时，除 `mapper.xml` 外必须标记作者为 `jf`，且禁止出现作者为 `ai` 的标识。
5. 新增或修改代码中的注释和日志必须使用中文；标识符用英文。
6. 禁止新增或修改测试代码、测试脚本、fixture 或 mock 文件，详见 `.workflow/specs/testing.md`。仓库当前没有测试套件，也不要去补。
7. 修改或设计 UI 界面时，先遵守现有设计系统和 `.workflow/specs/frontend.md`；设计类 Skill、MCP 或其他工具仅按需选用，不得作为强制前置门禁。
8. 除非用户要求、入口规则或完整 Harness 需要，不要创建与当前任务无关的文档、脚本或 README。
9. 前端单文件不超过 1000 行，后端单文件不超过 800 行；超限按职责拆分。
10. 内置发给模型的 prompt 必须使用中文。

## 仓库事实

- 三件套：根目录 Vue 3 前端、`spring-ai-backend/`、`python-ai-backend/`。两套后端共享 `/api/auth` 与 `/api/ai` 契约，都监听 `8999`，禁止同时启动。
- 前端 `vite.config.ts` 把 `/api` 和 `/ws` 代理到 `http://localhost:8999`。`start-docker-*.bat` 不启动开发前端；本地联调前端仍用 `npm run dev`（`http://localhost:5173`）。
- Compose profile `spring-ai` 与 `python-ai` 互斥。Realtime 也不对称：Spring 默认 DashScope，前端连 `/ws/ai/realtime-asr`；Python 只发 `/api/ai/realtime/client-secret`，没有该 WebSocket。
- 应用启动不执行项目 SQL。先起库，再由独立 Flyway 容器跑 `sql/migrations/`。pgvector 宿主机端口默认 `5433`，不是 `5432`。
- 无 `.github/workflows`，不要假设 CI 会跑测试或构建。
- `docs/requirements/` 被 gitignore；准备提交说明时必须直接读文件，不能只看 `git status`。

## 命令

前端（仓库根目录，npm / `package-lock.json`）：

```powershell
npm install
npm run dev
npm run lint
npm run type-check
npm run build
```

脚本名是 `type-check`，不是 `typecheck`。前端可见改动必须跑 `npm run lint`；涉及交互时再做浏览器级或等价手工验证。

后端：先复制对应 `.env.example` 为 `.env`。Spring 用 JDK 21，根目录 `.\start-spring-backend.bat`，或在 `spring-ai-backend/` 执行 `mvn spring-boot:run`。Python 用 `uv`，根目录 `.\start-python-backend.bat`。健康检查：`GET http://localhost:8999/health`；Python 另有 `/health/runtime`。

数据库（仓库根目录；按实际后端改 profile）：

```powershell
docker compose --profile spring-ai up -d mysql pgvector
docker compose --profile migration build flyway-mysql
docker compose --profile migration run --rm --no-deps flyway-mysql
docker compose --profile migration run --rm --no-deps flyway-pgvector
```

已应用迁移禁止改，只能新增更高版本。手工建库在 `sql/bootstrap/`，本地种子在 `sql/seeds/`（生产禁跑）。运行时代码禁止硬编码 SQL；MySQL 自定义 SQL 只写 `mapper.xml`。Spring `PgVectorStore` 必须保持 `initializeSchema(false)`。直连数据库用 `usql`，连接信息从配置读取。

验证只允许现有命令、一次性不落盘检查、日志/健康检查和手工操作；不要为验证创建临时项目文件。

## Git

- 禁止在 `main` 或 `dev` 上直接开发和提交。
- 分支英文 kebab-case，前缀 `feat/`、`fix/`、`hotfix/`、`docs/`、`chore/`、`refactor/`、`style/`、`perf/`、`build/`、`ci/`。
- 提交信息中文 + Conventional Commits；Git 与 PR 禁止 AI 生成标识。
- 未获用户明确要求不要提交。

## Harness 路由门禁

1. 用户要求新增、修改、修复、优化或调整功能时，先按 `.workflow/specs/harness-lifecycle.md` 判断走“直接修改”还是“完整 Harness”，不得默认强制创建 PRD。
2. 目标、范围和验收清晰，且属于低风险、可回退的局部修改或细节优化时，直接读取相关 Spec、修改并执行针对性验证；不创建 PRD，不要求输出完整阶段文档。
3. 新功能、跨模块或跨层契约、数据库结构、权限安全、高风险状态流程、范围不清或用户明确要求 PRD 时，进入完整 Harness，经过 Brainstorm 后生成包含验收标准的 PRD。
4. 无法可靠判断复杂度，或直接修改与完整 Harness 都合理时，只询问一次用户是否创建 PRD；优先使用可用的弹窗选项，用户选择“不创建”后直接修改，不得重复追问。
5. 涉及前端、后端、数据库、OpenAI、PR / Issue / Review 或 UI 的任务，仍需读取对应专项 Spec；跳过 PRD 不等于跳过安全、授权和必要验证。
6. 直接修改过程中若发现范围扩大到完整 Harness 条件，应停止扩展并询问是否升级为 PRD，不得静默扩大改动。
7. Harness 和直接修改车道都不强制调用指定 MCP、Skill、浏览器或其他工具；进入需要插件增强的 Hook 时，按 `.workflow/lifecycle-plugins.json` 和 `.workflow/specs/lifecycle-plugins.md` 解析，只调用已选择、可用且已获必要授权的插件。

## Spec 入口

- `.workflow/specs/index.md`：规范索引、必读项和任务路由。
- `.workflow/specs/conventions.md`：低 Token 通用约束摘要。
- `.workflow/specs/harness-lifecycle.md`：复杂度路由、直接修改车道以及完整 Harness 的 PRD、Quality Gate、可选 Review 和归档。
- `.workflow/lifecycle-plugins.json`：MCP 与 Skill 的生命周期插件注册表。
- `.workflow/specs/lifecycle-plugins.md`：插件 Hook、选择、授权、降级和扩展协议。
- `docs/harness-engineering-workflow.md`：仓库级 Harness Engineering 协作参考。
