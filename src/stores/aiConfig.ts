import { defineStore } from 'pinia'
import { computed, ref, watch } from 'vue'

export interface AiConfig {
  apiUrl: string
  apiToken: string
  modelName: string
  useBackendSpeech: boolean
  backendSpeechAutoDisabled: boolean
}

const STORAGE_KEY = 'resume-builder-ai-config'

export const useAiConfigStore = defineStore('aiConfig', () => {
  const apiUrl = ref('')
  const apiToken = ref('')
  const modelName = ref('')
  const useBackendSpeech = ref(true)
  const backendSpeechAutoDisabled = ref(false)

  const isConfigured = computed(
    () => apiUrl.value.trim() !== '' && apiToken.value.trim() !== '' && modelName.value.trim() !== '',
  )
  const shouldRequestBackendSpeech = computed(
    () => useBackendSpeech.value && !backendSpeechAutoDisabled.value,
  )

  function loadFromStorage() {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return
    try {
      const data = JSON.parse(raw) as Partial<AiConfig>
      if (data.apiUrl) apiUrl.value = data.apiUrl
      if (data.apiToken) apiToken.value = data.apiToken
      if (data.modelName) modelName.value = data.modelName
      if (typeof data.useBackendSpeech === 'boolean') useBackendSpeech.value = data.useBackendSpeech
      if (typeof data.backendSpeechAutoDisabled === 'boolean') {
        backendSpeechAutoDisabled.value = data.backendSpeechAutoDisabled
      }
    } catch {
      console.warn('Failed to load AI config from localStorage')
    }
  }

  function saveToStorage() {
    const data: AiConfig = {
      apiUrl: apiUrl.value,
      apiToken: apiToken.value,
      modelName: modelName.value,
      useBackendSpeech: useBackendSpeech.value,
      backendSpeechAutoDisabled: backendSpeechAutoDisabled.value,
    }
    localStorage.setItem(STORAGE_KEY, JSON.stringify(data))
  }

  function updateConfig(config: Pick<AiConfig, 'apiUrl' | 'apiToken' | 'modelName'> & Partial<Pick<AiConfig, 'useBackendSpeech'>>) {
    apiUrl.value = config.apiUrl
    apiToken.value = config.apiToken
    modelName.value = config.modelName
    if (typeof config.useBackendSpeech === 'boolean') {
      useBackendSpeech.value = config.useBackendSpeech
      if (config.useBackendSpeech) {
        backendSpeechAutoDisabled.value = false
      }
    }
    saveToStorage()
  }

  function setUseBackendSpeech(enabled: boolean) {
    useBackendSpeech.value = enabled
    if (enabled) {
      backendSpeechAutoDisabled.value = false
    }
    saveToStorage()
  }

  function markBackendSpeechUnavailable() {
    if (!useBackendSpeech.value) return
    backendSpeechAutoDisabled.value = true
    saveToStorage()
  }

  function clearBackendSpeechUnavailable() {
    backendSpeechAutoDisabled.value = false
    saveToStorage()
  }

  function clearConfig() {
    apiUrl.value = ''
    apiToken.value = ''
    modelName.value = ''
    useBackendSpeech.value = true
    backendSpeechAutoDisabled.value = false
    localStorage.removeItem(STORAGE_KEY)
  }

  loadFromStorage()

  watch([apiUrl, apiToken, modelName, useBackendSpeech, backendSpeechAutoDisabled], () => saveToStorage(), { deep: true })

  return {
    apiUrl,
    apiToken,
    modelName,
    useBackendSpeech,
    backendSpeechAutoDisabled,
    shouldRequestBackendSpeech,
    isConfigured,
    updateConfig,
    setUseBackendSpeech,
    markBackendSpeechUnavailable,
    clearBackendSpeechUnavailable,
    clearConfig,
  }
})
