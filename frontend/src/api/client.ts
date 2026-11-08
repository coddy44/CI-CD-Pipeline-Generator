import type {
  ApiError,
  GenerateRequest,
  GenerateResponse,
  SupportedResponse,
} from "./types";

const BASE_URL = import.meta.env.VITE_API_URL || "http://localhost:8080/api/v1";

export class BackendError extends Error {
  status: number
  code: string
  details: string[]

  constructor(
    status: number,
    code: string,
    message: string,
    details: string[] = [],
  ) {
    super(message)
    this.name = 'BackendError'
    
    this.status = status
    this.code = code
    this.details = details
  }
}

export async function getSupported(): Promise<SupportedResponse> {
  const res = await fetch(`${BASE_URL}/supported`);
  if (!res.ok) {
    throw new Error("Не удалось загрузить список поддерживаемых платформ");
  }
  return (await res.json()) as SupportedResponse;
}

export async function generatePipeline(
  req: GenerateRequest,
): Promise<GenerateResponse> {
  const res = await fetch(`${BASE_URL}/pipelines/generate`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(req),
  });

  if (!res.ok) {
    let payload: ApiError | null = null;
    try {
      payload = (await res.json()) as ApiError;
    } catch {}

    throw new BackendError(
      res.status,
      payload?.code || "UNKNOWN",
      payload?.message || `Ошибка сервера (HTTP ${res.status})`,
      payload?.details || [],
    );
  }

  return (await res.json()) as GenerateResponse;
}
