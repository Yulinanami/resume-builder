import { postAudioTranscription } from '@/api/speechApi'

const TEXT = {
  emptyAudio: '\u97f3\u9891\u6570\u636e\u4e3a\u7a7a\uff0c\u65e0\u6cd5\u8f6c\u5199',
  transcriptionFailed: '\u8bed\u97f3\u8f6c\u5199\u5931\u8d25',
  transcriptionEmpty: '\u8bed\u97f3\u8f6c\u5199\u7ed3\u679c\u4e3a\u7a7a',
} as const

function extractErrorText(text: string): string {
  const cleaned = text.trim()
  if (!cleaned) return ''
  try {
    const parsed = JSON.parse(cleaned) as { error?: string; message?: string }
    return parsed.message?.trim() || parsed.error?.trim() || cleaned
  } catch {
    return cleaned
  }
}

function detectAudioExtension(contentType: string): string {
  const value = contentType.toLowerCase()
  if (value.includes('ogg')) return 'ogg'
  if (value.includes('mp4') || value.includes('m4a')) return 'm4a'
  if (value.includes('wav')) return 'wav'
  return 'webm'
}

export async function transcribeInterviewAudio(blob: Blob, signal?: AbortSignal): Promise<string> {
  if (!blob || blob.size <= 0) {
    throw new Error(TEXT.emptyAudio)
  }

  const extension = detectAudioExtension(blob.type || '')
  const response = await postAudioTranscription(
    {
      blob,
      filename: `interview-${Date.now()}.${extension}`,
      language: 'zh',
    },
    signal
  )

  if (!response.ok) {
    const errorText = extractErrorText(await response.text().catch(() => ''))
    throw new Error(`${TEXT.transcriptionFailed} (${response.status}): ${errorText || response.statusText}`)
  }

  const payload = (await response.json().catch(() => ({}))) as { text?: unknown }
  const text = String(payload.text ?? '').trim()
  if (!text) {
    throw new Error(TEXT.transcriptionEmpty)
  }
  return text
}