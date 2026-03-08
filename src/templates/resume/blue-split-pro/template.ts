import ResumeTemplate from './ResumeTemplate.vue'
import { resolveTemplatePreviewImage } from '../previewImage'
import type { ResumeTemplateDefinition } from '../types'

const previewImage = resolveTemplatePreviewImage('../../../assets/templates/resume/blue-split-pro-preview.svg', import.meta.url)

export const BLUE_SPLIT_PRO_TEMPLATE: ResumeTemplateDefinition = {
  key: 'blue-split-pro',
  name: '蓝色分栏专业模板',
  previewImage,
  component: ResumeTemplate,
}
