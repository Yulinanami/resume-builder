export const AI_BACKEND_BASE_URL = __AI_BACKEND_BASE_URL__

export function buildAiBackendUrl(path: string): string {
  const normalizedPath = path.startsWith('/') ? path : `/${path}`
  return `${AI_BACKEND_BASE_URL}${normalizedPath}`
}
