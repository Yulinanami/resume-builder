import { buildAiBackendUrl } from '@/config/apiConfig'

export interface AudioTranscriptionRequest {
  blob: Blob
  language?: string
  model?: string
  prompt?: string
  filename?: string
}

export function getAudioTranscriptionEndpoint(): string {
  return buildAiBackendUrl('/api/ai/audio/transcriptions')
}

export async function postAudioTranscription(
  request: AudioTranscriptionRequest,
  signal?: AbortSignal
): Promise<Response> {
  const formData = new FormData()
  formData.append('file', request.blob, request.filename || 'audio.webm')

  if (request.language) {
    formData.append('language', request.language)
  }
  if (request.model) {
    formData.append('model', request.model)
  }
  if (request.prompt) {
    formData.append('prompt', request.prompt)
  }

  return fetch(getAudioTranscriptionEndpoint(), {
    method: 'POST',
    body: formData,
    signal,
  })
}