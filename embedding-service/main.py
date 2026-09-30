import os
from functools import lru_cache

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
from sentence_transformers import SentenceTransformer

# MiniLM multilíngue da família Sentence-Transformers (não é o BERT original).
# Fixei a revisão para repetir os mesmos resultados durante o experimento.
MODEL_NAME = os.getenv(
    "EMBEDDING_MODEL",
    "sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2",
)
MODEL_REVISION = os.getenv(
    "EMBEDDING_MODEL_REVISION",
    "e62509716f15c5fd03a6fd3156a4bc5e43f83f26",
)

app = FastAPI(title="Ágora Embeddings", version="1.0.0")
# Uso um modelo multilíngue já treinado; os feedbacks não alteram os pesos dele.


class EmbeddingRequest(BaseModel):
    texts: list[str] = Field(min_length=1, max_length=128)


class EmbeddingResponse(BaseModel):
    embeddings: list[list[float]]
    model: str
    revision: str


@lru_cache(maxsize=1)
def load_model() -> SentenceTransformer:
    # Carrego uma vez e uso CPU para não exigir placa de vídeo.
    return SentenceTransformer(MODEL_NAME, revision=MODEL_REVISION, device="cpu")


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok", "model": MODEL_NAME}


@app.post("/embeddings", response_model=EmbeddingResponse)
def embeddings(request: EmbeddingRequest) -> EmbeddingResponse:
    texts = [text.strip() for text in request.texts]
    if any(not text for text in texts):
        raise HTTPException(status_code=400, detail="Todos os textos devem conter título ou resumo")

    try:
        model = load_model()
        vectors = model.encode(
            texts,
            normalize_embeddings=True,
            batch_size=16,
            show_progress_bar=False,
            convert_to_numpy=True,
        )
        return EmbeddingResponse(
            embeddings=vectors.tolist(),
            model=MODEL_NAME,
            revision=MODEL_REVISION,
        )
    except Exception as exc:
        raise HTTPException(status_code=503, detail=f"Não foi possível gerar embeddings: {exc}") from exc
