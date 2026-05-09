"""
FAISS retrieval sidecar. Loads the index and metadata at startup,
exposes POST /search for similarity queries.
"""

import json
import logging
from contextlib import asynccontextmanager
from pathlib import Path
from typing import Annotated

import faiss
import numpy as np
from fastapi import FastAPI, Body, HTTPException
from pydantic import BaseModel, Field
from sentence_transformers import SentenceTransformer

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
log = logging.getLogger("retrieval-service")

SERVICE_DIR = Path(__file__).parent
INDEX_FILE = SERVICE_DIR / "faiss_index.bin"
METADATA_FILE = SERVICE_DIR / "chunk_metadata.json"

MODEL_NAME = "sentence-transformers/all-MiniLM-L6-v2"

state: dict = {}


@asynccontextmanager
async def lifespan(app: FastAPI):
    log.info("Loading FAISS index from %s", INDEX_FILE)
    state["index"] = faiss.read_index(str(INDEX_FILE))
    log.info("Loaded index with %d vectors", state["index"].ntotal)

    log.info("Loading metadata from %s", METADATA_FILE)
    state["metadata"] = json.loads(METADATA_FILE.read_text(encoding="utf-8"))
    log.info("Loaded %d metadata entries", len(state["metadata"]))

    log.info("Loading embedding model %s", MODEL_NAME)
    state["model"] = SentenceTransformer(MODEL_NAME)
    log.info("Service ready")
    yield
    log.info("Shutting down")
    state.clear()


app = FastAPI(title="AgentFlow Retrieval Service", lifespan=lifespan)


class SearchRequest(BaseModel):
    query: str = Field(min_length=1, max_length=1000)
    k: int = Field(default=5, ge=1, le=20)


class SearchHit(BaseModel):
    id: str
    score: float
    source: str
    section: str
    text: str


class SearchResponse(BaseModel):
    query: str
    hits: list[SearchHit]


@app.get("/health")
def health() -> dict:
    return {"status": "ok", "vectors": state["index"].ntotal}


@app.post("/search", response_model=SearchResponse)
def search(request: Annotated[SearchRequest, Body()]) -> SearchResponse:
    if "index" not in state:
        raise HTTPException(status_code=503, detail="Index not loaded")

    embedding = state["model"].encode(
        [request.query],
        normalize_embeddings=True,
        convert_to_numpy=True,
    ).astype(np.float32)

    scores, indices = state["index"].search(embedding, request.k)

    hits = []
    for score, idx in zip(scores[0], indices[0]):
        if idx < 0:
            continue
        meta = state["metadata"][idx]
        hits.append(SearchHit(
            id=meta["id"],
            score=float(score),
            source=meta["source"],
            section=meta["section"],
            text=meta["text"],
        ))

    return SearchResponse(query=request.query, hits=hits)