<script setup lang="ts">
import { computed, reactive, ref, type Component } from 'vue'
import { useResumeStore } from '@/stores/resume'
import BasicInfoEditor from './editors/BasicInfoEditor.vue'
import EducationEditor from './editors/EducationEditor.vue'
import SkillsEditor from './editors/SkillsEditor.vue'
import WorkExperienceEditor from './editors/WorkExperienceEditor.vue'
import ProjectExperienceEditor from './editors/ProjectExperienceEditor.vue'
import AwardsEditor from './editors/AwardsEditor.vue'
import SelfIntroEditor from './editors/SelfIntroEditor.vue'
import { getModuleIconPaths, MODULE_ICON_VIEWBOX } from '@/constants/moduleIcons'

const store = useResumeStore()
const showSaved = ref(false)
const searchValue = ref('')

const expanded = reactive<Record<string, boolean>>({
  basicInfo: true,
  education: false,
  skills: false,
  workExperience: false,
  projectExperience: false,
  awards: false,
  selfIntro: false,
})

const editorMap: Record<string, Component> = {
  basicInfo: BasicInfoEditor,
  education: EducationEditor,
  skills: SkillsEditor,
  workExperience: WorkExperienceEditor,
  projectExperience: ProjectExperienceEditor,
  awards: AwardsEditor,
  selfIntro: SelfIntroEditor,
}

const visibleCount = computed(() => store.modules.filter((m) => m.visible).length)
const searchKeyword = computed(() => searchValue.value.trim())
const filteredModules = computed(() =>
  store.modules.filter((m) => (searchKeyword.value ? m.label.includes(searchKeyword.value) : true))
)

function hasTextContent(value: string | undefined): boolean {
  if (!value) return false
  const text = value
    .replace(/<[^>]*>/g, ' ')
    .replace(/&nbsp;/gi, ' ')
    .trim()
  return text.length > 0
}

function countFilled(values: Array<string | undefined>): number {
  return values.reduce((count, value) => count + (value?.trim() ? 1 : 0), 0)
}

function scoreByFilled(values: Array<string | undefined>): number {
  if (values.length === 0) return 0
  return countFilled(values) / values.length
}

const moduleCompletion = computed<Record<string, number>>(() => {
  const basic = store.basicInfo

  const basicInfoScore = scoreByFilled([
    basic.name,
    basic.phone,
    basic.email,
    basic.jobTitle,
    basic.expectedLocation,
    basic.educationLevel,
  ])

  const firstEducation = store.educationList.find((e) =>
    [e.school, e.major, e.degree, e.startDate].some((value) => value?.trim())
  )
  const educationScore = firstEducation
    ? scoreByFilled([firstEducation.school, firstEducation.major, firstEducation.degree, firstEducation.startDate])
    : 0

  const firstWork = store.workList.find((w) =>
    [w.company, w.position, w.startDate, w.description].some((value) => value?.trim())
  )
  const workScore = firstWork
    ? scoreByFilled([firstWork.company, firstWork.position, firstWork.startDate, firstWork.description])
    : 0

  const firstProject = store.projectList.find((p) =>
    [p.name, p.role, p.startDate, p.mainWork].some((value) => value?.trim())
  )
  const projectScore = firstProject
    ? scoreByFilled([firstProject.name, firstProject.role, firstProject.startDate, firstProject.mainWork])
    : 0

  const firstAward = store.awardList.find((a) => [a.name, a.date].some((value) => value?.trim()))
  const awardsScore = firstAward ? scoreByFilled([firstAward.name, firstAward.date]) : 0

  return {
    basicInfo: basicInfoScore,
    education: educationScore,
    skills: hasTextContent(store.skills) ? 1 : 0,
    workExperience: workScore,
    projectExperience: projectScore,
    awards: awardsScore,
    selfIntro: hasTextContent(store.selfIntro) ? 1 : 0,
  }
})

const completionPercent = computed(() => {
  const enabledModules = store.modules.filter((m) => m.visible)
  if (enabledModules.length === 0) return 0
  const total = enabledModules.reduce((sum, mod) => sum + (moduleCompletion.value[mod.key] ?? 0), 0)
  return Math.round((total / enabledModules.length) * 100)
})

function handleSave() {
  store.saveToStorage()
  showSaved.value = true
  setTimeout(() => {
    showSaved.value = false
  }, 1800)
}

const isDefaultOrder = computed(() => store.isDefaultModuleOrder())

function handleResetOrder() {
  store.resetModuleOrder()
}

function toggleExpand(key: string) {
  expanded[key] = !expanded[key]
}

function moduleIconPaths(key: string): string[] {
  return getModuleIconPaths(key)
}

function canMoveUp(key: string): boolean {
  return store.canMoveModule(key, 'up')
}

function canMoveDown(key: string): boolean {
  return store.canMoveModule(key, 'down')
}

function moveUp(key: string) {
  store.moveModule(key, 'up')
}

function moveDown(key: string) {
  store.moveModule(key, 'down')
}
</script>

<template>
  <main class="editor-panel">
    <div class="editor-toolbar">
      <input v-model="searchValue" class="search-input" placeholder="搜索模块：基本信息 / 教育经历 / 专业技能" />
      <span class="chip">自动保存中</span>
      <button class="btn-action">AI 优化建议</button>
    </div>

    <div class="stats-row">
      <div class="stat-card">
        <p class="stat-label">简历完整度</p>
        <p class="stat-value">{{ completionPercent }}%</p>
      </div>
      <div class="stat-card">
        <p class="stat-label">模块已启用</p>
        <p class="stat-value">{{ visibleCount }} / {{ store.modules.length }}</p>
      </div>
    </div>

    <section class="info-editor">
      <div class="info-editor-header">
        <h2 class="editor-title">信息编辑区</h2>
        <div class="editor-header-actions">
          <button class="btn-reset-order" :disabled="isDefaultOrder" @click="handleResetOrder">恢复默认顺序</button>
          <button class="btn-save" @click="handleSave">保存草稿</button>
        </div>
      </div>
      <p class="editor-subtitle">模块顺序与侧边菜单一致，点击右侧可展开/收起</p>
      <transition name="fade">
        <p v-if="showSaved" class="save-hint">已保存</p>
      </transition>

      <div class="module-sections">
        <article
          v-for="mod in filteredModules"
          :key="mod.key"
          class="module-block"
          :class="{ disabled: !mod.visible }"
        >
	          <header class="module-head" @click="toggleExpand(mod.key)">
	            <div class="module-head-left">
	              <span class="module-head-icon" aria-hidden="true">
	                <svg class="module-head-icon-svg" :viewBox="MODULE_ICON_VIEWBOX">
	                  <path v-for="(d, idx) in moduleIconPaths(mod.key)" :key="`${mod.key}-${idx}`" :d="d" />
	                </svg>
	              </span>
	              <span class="module-head-title">{{ mod.label }}</span>
	            </div>
            <div class="module-head-right">
              <div v-if="mod.key !== 'basicInfo' && mod.visible" class="order-actions">
                <button class="order-btn" :disabled="!canMoveUp(mod.key)" @click.stop="moveUp(mod.key)">↑</button>
                <button class="order-btn" :disabled="!canMoveDown(mod.key)" @click.stop="moveDown(mod.key)">↓</button>
              </div>
              <span v-if="!mod.visible" class="disabled-tag">已关闭</span>
              <span class="expand-text">{{ expanded[mod.key] && mod.visible ? '收起' : '展开' }} ▸</span>
            </div>
          </header>

          <div v-if="expanded[mod.key] && mod.visible" class="module-body">
            <component :is="editorMap[mod.key]" />
          </div>
        </article>

        <div v-if="filteredModules.length === 0" class="empty-result">没有匹配的模块</div>
      </div>
    </section>
  </main>
</template>

<style scoped>
.editor-panel {
  flex: 1;
  min-width: 0;
  height: 100%;
  overflow-y: auto;
  overflow-x: hidden;
  container-type: inline-size;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.editor-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
}

.search-input {
  flex: 1;
  min-width: 0;
  height: 40px;
  border: 1px solid #ddd2c6;
  border-radius: 8px;
  padding: 0 12px;
  background: #fff;
  color: #2d2521;
  font-size: 13px;
}

.search-input:focus {
  outline: none;
  border-color: #d97745;
  box-shadow: 0 0 0 3px rgba(217, 119, 69, 0.14);
}

.chip {
  height: 40px;
  padding: 0 12px;
  border-radius: 8px;
  background: #efe7dc;
  color: #7b6a5b;
  display: inline-flex;
  align-items: center;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}

.btn-action {
  height: 40px;
  padding: 0 12px;
  border-radius: 8px;
  border: none;
  background: #2d2521;
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}

.stats-row {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.stat-card {
  background: #fff;
  border: 1px solid #e9ded0;
  border-radius: 10px;
  padding: 14px;
}

.stat-label {
  font-size: 11px;
  color: #8a7461;
  margin-bottom: 4px;
}

.stat-value {
  font-family: 'Noto Sans SC', sans-serif;
  font-size: 32px;
  line-height: 1;
  font-weight: 700;
  color: #2d2521;
}

.info-editor {
  background: #fff;
  border: 1px solid #e9ded0;
  border-radius: 12px;
  padding: 16px;
}

.info-editor-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.editor-header-actions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.editor-title {
  font-size: 18px;
  font-weight: 700;
  color: #2d2521;
}

.editor-subtitle {
  margin-top: 2px;
  font-size: 12px;
  color: #8a7461;
}

.btn-save {
  border: none;
  height: 36px;
  padding: 0 14px;
  border-radius: 8px;
  background: #2d2521;
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}

.btn-reset-order {
  border: 1px solid #ddcfbf;
  height: 36px;
  padding: 0 12px;
  border-radius: 8px;
  background: #fff;
  color: #8a7461;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}

.btn-reset-order:hover:not(:disabled) {
  border-color: #d97745;
  color: #d97745;
}

.btn-reset-order:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.save-hint {
  margin-top: 6px;
  color: #d97745;
  font-size: 12px;
  font-weight: 600;
}

.module-sections {
  margin-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.module-block {
  border-radius: 10px;
  background: #f8f3ed;
  border: 1px solid #efe4d8;
  overflow: hidden;
}

.module-block.disabled {
  background: #f2ece5;
}

.module-head {
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 0 12px;
  cursor: pointer;
}

.module-head-left {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.module-head-icon {
  width: 18px;
  height: 18px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.module-head-icon-svg {
  width: 16px;
  height: 16px;
  fill: none;
  stroke: #8a7461;
  stroke-width: 1.8;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.module-block:not(.disabled) .module-head-icon-svg {
  stroke: #d97745;
}

.module-head-title {
  font-size: 14px;
  font-weight: 700;
  color: #2d2521;
}

.module-head-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.order-actions {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.order-btn {
  width: 22px;
  height: 22px;
  border: 1px solid #ddcfbf;
  border-radius: 6px;
  background: #fff;
  color: #8a7461;
  font-size: 12px;
  line-height: 1;
  font-weight: 700;
  cursor: pointer;
}

.order-btn:hover:not(:disabled) {
  border-color: #d97745;
  color: #d97745;
}

.order-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.disabled-tag {
  font-size: 11px;
  color: #a08c7b;
  font-weight: 600;
}

.expand-text {
  font-size: 12px;
  color: #8a7461;
  font-weight: 600;
}

.module-body {
  padding: 0 10px 10px;
}

.empty-result {
  font-size: 12px;
  color: #8a7461;
  text-align: center;
  padding: 18px 0;
}

.module-body :deep(.editor-section) {
  margin: 0;
  border: none;
  border-radius: 8px;
  background: transparent;
  box-shadow: none;
}

.module-body :deep(.editor-section:hover) {
  box-shadow: none;
}

.module-body :deep(.section-header) {
  display: none;
}

.module-body :deep(.section-body) {
  padding: 10px;
  background: #fff;
  border: 1px solid #e9ded0;
  border-radius: 8px;
}

.module-body :deep(.entry-card) {
  background: #fff;
  border-color: #e9ded0;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

@container (max-width: 560px) {
  .chip,
  .btn-action {
    display: none;
  }

  .stats-row {
    grid-template-columns: 1fr;
    gap: 8px;
  }

  .info-editor-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .editor-header-actions {
    width: 100%;
    flex-wrap: wrap;
  }

  .expand-text {
    display: none;
  }
}

@container (max-width: 420px) {
  .editor-toolbar {
    flex-wrap: wrap;
  }

  .search-input {
    width: 100%;
  }

  .editor-panel {
    padding: 14px;
    gap: 10px;
  }

  .info-editor {
    padding: 12px;
  }

  .stat-value {
    font-size: 26px;
  }
}
</style>

