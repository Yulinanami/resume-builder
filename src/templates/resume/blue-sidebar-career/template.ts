import ResumeTemplate from './ResumeTemplate.vue'
import { resolveTemplatePreviewImage } from '../previewImage'
import type { ResumeTemplateDefinition } from '../types'

const previewImage = resolveTemplatePreviewImage('../../../assets/templates/resume/blue-sidebar-career-preview.svg', import.meta.url)

export const BLUE_SIDEBAR_CAREER_TEMPLATE: ResumeTemplateDefinition = {
  key: 'blue-sidebar-career',
  name: '蓝色侧栏职场模板',
  previewImage,
  component: ResumeTemplate,
}
