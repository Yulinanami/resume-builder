import { buildAiBackendUrl } from '@/config/apiConfig'

export interface ChatRequestPayload {
  message: string
  sanitizeOutput?: boolean
}

export interface ChatResponsePayload {
  answer?: string
}

export function getAiChatEndpoint(): string {
  return buildAiBackendUrl('/api/ai/chat')
}

export function getAiChatStreamEndpoint(): string {
  return buildAiBackendUrl('/api/ai/chat/stream')
}

export async function postAiChat(
  requestBody: ChatRequestPayload,
  signal?: AbortSignal
): Promise<Response> {
  return fetch(getAiChatEndpoint(), {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'application/json',
    },
    body: JSON.stringify(requestBody),
    signal,
  })
}

export async function postAiChatStream(
  requestBody: ChatRequestPayload,
  signal?: AbortSignal
): Promise<Response> {
  return fetch(getAiChatStreamEndpoint(), {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream',
    },
    body: JSON.stringify(requestBody),
    signal,
  })
}
