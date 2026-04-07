import { buildAiBackendUrl } from '@/config/apiConfig'

export interface RealtimeClientSecretRequest {
  model?: string
  language?: string
}

export interface RealtimeClientSecretResponse {
  clientSecret: string
  expiresAt?: number | null
  model?: string
  realtimeApiBaseUrl: string
  realtimeCallsPath?: string
}

export function getRealtimeClientSecretEndpoint(): string {
  return buildAiBackendUrl('/api/ai/realtime/client-secret')
}

export async function postRealtimeClientSecret(
  payload: RealtimeClientSecretRequest = {},
  signal?: AbortSignal
): Promise<Response> {
  return fetch(getRealtimeClientSecretEndpoint(), {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(payload),
    signal,
  })
}

export async function postRealtimeCallSdp(
  endpoint: string,
  clientSecret: string,
  sdpOffer: string,
  signal?: AbortSignal
): Promise<Response> {
  return fetch(endpoint, {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${clientSecret}`,
      'Content-Type': 'application/sdp',
    },
    body: sdpOffer,
    signal,
  })
}