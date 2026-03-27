# Resume Builder

一个基于 Vue 3 + Vite 的简历编辑与 AI 面试一体化工具。

当前版本包含两大主菜单：
- `简历编辑`
- `AI面试`

## 核心能力

### 1) 简历编辑
- 模块化编辑：基本信息、教育经历、专业技能、工作经历、项目经历、荣誉奖项、个人简介。
- 模块开关与顺序调整：支持可见性切换与拖动式上下调整（基础信息固定在首位）。
- 实时完整度统计：根据已启用模块动态计算简历完整度。
- 自动本地保存：编辑内容自动持久化到 `localStorage`，支持手动“保存草稿”。
- 模板切换：内置 6 套简历模板。
- 导出能力：
  - 高清 PDF
  - 压缩 PDF
  - Markdown

### 2) AI 优化（简历编辑内）
- 支持 OpenAI 兼容接口的模型配置（`API URL` / `API Key` / `Model`）。
- 按模块发起 AI 优化（流式返回）。
- 输出分为“优化建议”和“优化后内容”。
- 一键应用优化结果，并支持撤回。
- 当前可直接应用的模块：`skills`、`selfIntro`、`workExperience`、`projectExperience`、`awards`。

### 3) AI 面试
- 双模式切换：
  - `你是面试者`（AI 扮演面试官）
  - `你是面试官`（AI 扮演候选人）
- 面试控制台：开始、暂停/继续、重置、结束并评分，支持时长 `-5m/+5m` 调整。
- 计时规则：总时长限制 15~120 分钟，倒计时结束可自动触发收尾评估。
- 多轮对话与流式回答渲染，支持 Markdown 展示。
- 语音输入：支持浏览器语音识别，快捷键 `Ctrl + I` 开关语音。
- 快捷输入：`Enter` 发送，`Ctrl + Enter` 换行。
- 面试结束后输出综合评分（含分项得分与改进建议）。

## 内置模板

- 默认模板
- 蓝色线性模板
- 黑白线性模板
- 通用职场模板
- 蓝色侧栏职场模板
- 蓝色分栏专业模板

## 技术栈

- 前端框架：Vue 3（Composition API）
- 构建工具：Vite
- 状态管理：Pinia
- 类型系统：TypeScript + vue-tsc
- 代码质量：ESLint + Oxlint + Oxfmt
- 导出：html2canvas + jsPDF / html2pdf.js
- Markdown 渲染：markdown-it

## 运行环境

- Node.js: `^20.19.0 || >=22.12.0`
- npm: 建议使用最新稳定版

## 快速开始

```bash
npm install
npm run dev
```

默认开发地址：`http://localhost:5173`

## 常用脚本

```bash
# 开发
npm run dev

# 构建（含类型检查）
npm run build

# 仅构建前端产物
npm run build-only

# 预览构建产物
npm run preview

# 类型检查
npm run type-check

# 代码检查（oxlint + eslint）
npm run lint

# 代码格式化
npm run format
```

## AI 配置说明

在“AI优化”或“AI面试”中点击“配置模型”，填写：
- `API URL`（无需手动拼 `/v1/chat/completions`，系统会自动补全）
- `API Key`
- `Model Name`

配置会保存在本地浏览器存储中。

## 项目结构

```text
src/
  components/
    ai/
      interview/        # AI 面试主界面与子组件
    resume/             # 简历编辑区、预览区、模板选择
    common/             # 通用组件（侧边栏、富文本等）
  services/
    prompts/            # AI 提示词（简历优化 / 面试）
    aiService.ts        # 简历模块 AI 优化
    interviewService.ts # AI 面试请求与响应归一化
    exportMarkdown.ts   # Markdown 导出
  stores/
    resume.ts           # 简历数据与模块配置
    aiConfig.ts         # AI 配置
  templates/resume/     # 简历模板注册与实现
```

## 注意事项

- 当前是纯前端项目，简历与 AI 配置默认保存在浏览器本地。
- 语音输入依赖浏览器语音识别能力，部分浏览器可能不支持。

## License

MIT
