import ResumeTemplate from './ResumeTemplate.vue'
import { resolveTemplatePreviewImage } from '../previewImage'
import type { ResumeTemplateDefinition } from '../types'

const previewImage = resolveTemplatePreviewImage('../../../assets/templates/resume/default-preview.svg', import.meta.url)

export const DEFAULT_TEMPLATE: ResumeTemplateDefinition = {
  key: 'default',
  name: '默认模板',
  previewImage,
  component: ResumeTemplate,
}
