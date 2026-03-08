import ResumeTemplate from './ResumeTemplate.vue'
import { resolveTemplatePreviewImage } from '../previewImage'
import type { ResumeTemplateDefinition } from '../types'

const previewImage = resolveTemplatePreviewImage('../../../assets/templates/resume/workplace-general-preview.svg', import.meta.url)

export const WORKPLACE_GENERAL_TEMPLATE: ResumeTemplateDefinition = {
  key: 'workplace-general',
  name: '通用职场模板',
  previewImage,
  component: ResumeTemplate,
}
