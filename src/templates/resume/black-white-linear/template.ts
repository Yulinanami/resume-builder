import ResumeTemplate from './ResumeTemplate.vue'
import { resolveTemplatePreviewImage } from '../previewImage'
import type { ResumeTemplateDefinition } from '../types'

const previewImage = resolveTemplatePreviewImage('../../../assets/templates/resume/black-white-linear-preview.svg', import.meta.url)

export const BLACK_WHITE_LINEAR_TEMPLATE: ResumeTemplateDefinition = {
  key: 'black-white-linear',
  name: '黑白线性模板',
  previewImage,
  component: ResumeTemplate,
}
