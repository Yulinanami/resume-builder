<script setup lang="ts">
import { computed } from 'vue'
import { useResumeStore } from '@/stores/resume'
import { getModuleIconPaths, MODULE_ICON_VIEWBOX } from '@/constants/moduleIcons'

const store = useResumeStore()
const props = withDefaults(defineProps<{ collapsed?: boolean }>(), {
  collapsed: false,
})
const emit = defineEmits<{
  (e: 'toggle-collapse'): void
  (e: 'continue-editing'): void
}>()

const enabledCount = computed(() => store.modules.filter((m) => m.visible).length)
const displayName = computed(() => store.basicInfo.name?.trim() || '同学')

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

const completionPercent = computed(() => {
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

  const moduleCompletion: Record<string, number> = {
    basicInfo: basicInfoScore,
    education: educationScore,
    skills: hasTextContent(store.skills) ? 1 : 0,
    workExperience: workScore,
    projectExperience: projectScore,
    awards: awardsScore,
    selfIntro: hasTextContent(store.selfIntro) ? 1 : 0,
  }

  const enabledModules = store.modules.filter((m) => m.visible)
  if (enabledModules.length === 0) return 0
  const total = enabledModules.reduce((sum, mod) => sum + (moduleCompletion[mod.key] ?? 0), 0)
  return Math.round((total / enabledModules.length) * 100)
})

function moduleIconPaths(key: string): string[] {
  return getModuleIconPaths(key)
}
</script>

<template>
  <aside class="sidebar" :class="{ collapsed: props.collapsed }">
    <div class="brand">
      <div class="brand-left">
        <span class="brand-mark"></span>
        <span class="brand-text">Resume Builder</span>
      </div>
      <button
        class="collapse-btn"
        type="button"
        :aria-label="props.collapsed ? '展开侧边菜单' : '收起侧边菜单'"
        :title="props.collapsed ? '展开' : '收缩'"
        :data-tip="props.collapsed ? '展开' : '收缩'"
        @click="emit('toggle-collapse')"
      >
        {{ props.collapsed ? '>' : '<' }}
      </button>
    </div>

    <p class="menu-caption">模块开关</p>

    <ul class="module-list">
      <li
        v-for="mod in store.modules"
        :key="mod.key"
        class="module-item"
        :class="{ active: mod.visible, muted: !mod.visible }"
      >
        <div class="module-info">
          <span class="module-icon" aria-hidden="true">
            <svg class="module-icon-svg" :viewBox="MODULE_ICON_VIEWBOX">
              <path v-for="(d, idx) in moduleIconPaths(mod.key)" :key="`${mod.key}-${idx}`" :d="d" />
            </svg>
          </span>
          <span class="module-label">{{ mod.label }}</span>
        </div>

        <label class="toggle-switch">
          <input
            type="checkbox"
            :checked="mod.visible"
            :aria-label="`${mod.label}开关`"
            @change="store.toggleModule(mod.key)"
          />
          <span class="toggle-slider"></span>
        </label>
      </li>
    </ul>

    <div class="profile-card">
      <div class="profile-name">你好，{{ displayName }}</div>
      <div class="profile-meta">已完成度 {{ completionPercent }}%</div>
      <div class="progress-track">
        <div class="progress-fill" :style="{ width: `${completionPercent}%` }"></div>
      </div>
      <button class="continue-btn" @click="emit('continue-editing')">继续编辑</button>
    </div>
  </aside>
</template>

<style scoped>
.sidebar {
  width: 272px;
  min-width: 272px;
  background: #efe7dc;
  padding: 18px 14px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  border-right: 1px solid #dfd2c2;
  overflow-y: auto;
}

.brand {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
  padding: 4px 4px 2px;
}

.brand-left {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.brand-mark {
  width: 12px;
  height: 12px;
  border-radius: 4px;
  background: #d97745;
}

.brand-text {
  font-family: 'Noto Sans SC', sans-serif;
  font-size: 13px;
  font-weight: 700;
  color: #2d2521;
}

.collapse-btn {
  position: relative;
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 8px;
  background: #f7f3ee;
  color: #d97745;
  font-size: 14px;
  font-weight: 700;
  line-height: 1;
  cursor: pointer;
  flex-shrink: 0;
  transition: background 0.18s ease, color 0.18s ease;
}

.collapse-btn::after {
  content: attr(data-tip);
  position: absolute;
  left: 50%;
  top: -8px;
  transform: translate(-50%, -100%);
  background: #2d2521;
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  line-height: 1;
  padding: 5px 8px;
  border-radius: 6px;
  white-space: nowrap;
  pointer-events: none;
  opacity: 0;
  transition: opacity 0.16s ease;
  z-index: 6;
}

.collapse-btn::before {
  content: '';
  position: absolute;
  left: 50%;
  top: -8px;
  transform: translateX(-50%);
  border-left: 5px solid transparent;
  border-right: 5px solid transparent;
  border-top: 6px solid #2d2521;
  opacity: 0;
  transition: opacity 0.16s ease;
  pointer-events: none;
  z-index: 6;
}

.collapse-btn:hover::after,
.collapse-btn:hover::before,
.collapse-btn:focus-visible::after,
.collapse-btn:focus-visible::before {
  opacity: 1;
}
.collapse-btn:hover {
  background: #f2ece5;
  color: #c96a3b;
}

.menu-caption {
  color: #8a7461;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.03em;
  padding: 0 6px;
}

.module-list {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.module-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-radius: 10px;
  padding: 10px 12px;
  border: 1px solid transparent;
  background: #f2ece5;
  transition: all 0.18s ease;
}

.module-item.active {
  background: #ffffff;
  border-color: #e9ded0;
}

.module-item.muted {
  opacity: 0.9;
}

.module-info {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.module-icon {
  width: 18px;
  height: 18px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.module-icon-svg {
  width: 16px;
  height: 16px;
  fill: none;
  stroke: #8a7461;
  stroke-width: 1.8;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.module-item.active .module-icon-svg {
  stroke: #d97745;
}

.module-label {
  color: #2d2521;
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.toggle-switch {
  position: relative;
  width: 42px;
  height: 24px;
  flex-shrink: 0;
}

.toggle-switch input {
  opacity: 0;
  width: 0;
  height: 0;
}

.toggle-slider {
  position: absolute;
  inset: 0;
  border-radius: 999px;
  background: #b8afa6;
  transition: 0.2s ease;
}

.toggle-slider::before {
  content: '';
  position: absolute;
  width: 18px;
  height: 18px;
  left: 3px;
  top: 3px;
  border-radius: 50%;
  background: #ffffff;
  transition: 0.2s ease;
}

.toggle-switch input:checked + .toggle-slider {
  background: #d97745;
}

.toggle-switch input:checked + .toggle-slider::before {
  transform: translateX(18px);
}

.profile-card {
  margin-top: auto;
  padding: 12px;
  border-radius: 10px;
  background: #ffffff;
  border: 1px solid #e9ded0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.profile-name {
  color: #2d2521;
  font-size: 12px;
  font-weight: 600;
}

.profile-meta {
  color: #8a7461;
  font-size: 11px;
}

.progress-track {
  width: 100%;
  height: 8px;
  border-radius: 999px;
  background: #e6d8ca;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  border-radius: inherit;
  background: #d97745;
  transition: width 0.25s ease;
}

.continue-btn {
  border: none;
  height: 34px;
  border-radius: 9px;
  background: #2d2521;
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}

.continue-btn:hover {
  background: #1f1916;
}

.sidebar.collapsed {
  width: 92px;
  min-width: 92px;
  padding: 14px 8px;
}

.sidebar.collapsed .menu-caption,
.sidebar.collapsed .profile-card,
.sidebar.collapsed .brand-text,
.sidebar.collapsed .module-label {
  display: none;
}

.sidebar.collapsed .brand {
  justify-content: center;
}

.sidebar.collapsed .module-item {
  justify-content: center;
  padding: 8px 6px;
}

.sidebar.collapsed .module-info {
  min-width: auto;
}

.sidebar.collapsed .toggle-switch {
  width: 36px;
  height: 20px;
}

.sidebar.collapsed .toggle-slider::before {
  width: 14px;
  height: 14px;
}

.sidebar.collapsed .toggle-switch input:checked + .toggle-slider::before {
  transform: translateX(16px);
}
</style>


