import ResumeTemplate from './ResumeTemplate.vue'
import { resolveTemplatePreviewImage } from '../previewImage'
import type { ResumeTemplateDefinition } from '../types'

const previewImage = resolveTemplatePreviewImage('../../../assets/templates/resume/blue-linear-preview.svg', import.meta.url)

export const BLUE_LINEAR_TEMPLATE: ResumeTemplateDefinition = {
  key: 'blue-linear',
  name: '蓝色线性模板',
  previewImage,
  component: ResumeTemplate,
}
