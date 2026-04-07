import { buildAiBackendUrl } from '@/config/apiConfig'
import type { InterviewTurnRequest } from '@/services/interview/types'

export function getInterviewTurnStreamEndpoint(): string {
  return buildAiBackendUrl('/api/ai/interview/turn/stream')
}

export function getInterviewSessionsEndpoint(limit?: number): string {
  const query = typeof limit === 'number' && Number.isFinite(limit) ? `?limit=${Math.max(1, Math.floor(limit))}` : ''
  return buildAiBackendUrl(`/api/ai/interview/sessions${query}`)
}

export function getInterviewSessionDetailEndpoint(sessionId: string): string {
  return buildAiBackendUrl(`/api/ai/interview/sessions/${encodeURIComponent(sessionId)}`)
}

export async function postInterviewTurnStream(
  requestBody: InterviewTurnRequest,
  signal?: AbortSignal
): Promise<Response> {
  return fetch(getInterviewTurnStreamEndpoint(), {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'application/x-ndjson',
    },
    body: JSON.stringify(requestBody),
    signal,
  })
}

export async function getInterviewSessions(limit = 20, signal?: AbortSignal): Promise<Response> {
  return fetch(getInterviewSessionsEndpoint(limit), {
    method: 'GET',
    headers: {
      Accept: 'application/json',
    },
    signal,
  })
}

export async function getInterviewSessionDetail(sessionId: string, signal?: AbortSignal): Promise<Response> {
  return fetch(getInterviewSessionDetailEndpoint(sessionId), {
    method: 'GET',
    headers: {
      Accept: 'application/json',
    },
    signal,
  })
}
