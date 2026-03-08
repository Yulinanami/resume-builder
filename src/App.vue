<script setup lang="ts">
import { ref } from 'vue'
import ModuleSidebar from '@/components/ModuleSidebar.vue'
import EditorPanel from '@/components/EditorPanel.vue'
import PreviewPanel from '@/components/PreviewPanel.vue'

const sidebarCollapsed = ref(false)

function handleContinueEditing() {
  sidebarCollapsed.value = false
  const editorPanel = document.querySelector('.editor-panel') as HTMLElement | null
  if (!editorPanel) return
  editorPanel.scrollTo({ top: 0, behavior: 'smooth' })
  requestAnimationFrame(() => {
    const firstField = editorPanel.querySelector('input, textarea, select, [contenteditable="true"]') as
      | HTMLElement
      | null
    firstField?.focus()
  })
}
</script>

<template>
  <div class="app-layout">
    <ModuleSidebar
      :collapsed="sidebarCollapsed"
      @toggle-collapse="sidebarCollapsed = !sidebarCollapsed"
      @continue-editing="handleContinueEditing"
    />
    <div class="main-content">
      <EditorPanel />
      <PreviewPanel />
    </div>
  </div>
</template>

<style scoped>
.app-layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

.main-content {
  flex: 1;
  display: flex;
  overflow: hidden;
  min-width: 0;
}
</style>
