"""
api_client.py
Single source of truth for Spring Boot API access and MongoDB connections.

load_workflows() reads directly from MongoDB — no Spring API credentials needed.
The Spring API helpers (api_get, etc.) remain available for other use cases.
"""
import logging
import os
import time

import pymongo
import requests
from dotenv import load_dotenv

load_dotenv()

SPRING_API   = os.getenv("SPRING_API_URL",  "http://backend:8080/api")
SPRING_EMAIL = os.getenv("SPRING_EMAIL",    "")
SPRING_PASS  = os.getenv("SPRING_PASSWORD", "")
MONGO_URI    = os.getenv("MONGODB_URI",     "")
MONGO_DB     = os.getenv("MONGODB_DB",      "workflow_db")

logger = logging.getLogger(__name__)

_token: str = ""


# ── Auth ──────────────────────────────────────────────────────────────────────

def refresh_token() -> str:
    global _token
    try:
        r = requests.post(
            f"{SPRING_API}/auth/login",
            json={"email": SPRING_EMAIL, "password": SPRING_PASS},
            timeout=8,
        )
        r.raise_for_status()
        d = r.json()
        _token = d.get("accessToken") or d.get("token") or d.get("jwt") or ""
    except Exception as e:
        logger.warning(f"api_client: auth failed — {e}")
        _token = ""
    return _token


def get_headers() -> dict:
    tok = _token or refresh_token()
    return {"Authorization": f"Bearer {tok}"} if tok else {}


def api_get(path: str, params: dict | None = None) -> list | dict:
    r = requests.get(f"{SPRING_API}{path}", headers=get_headers(), params=params, timeout=15)
    if r.status_code == 401:
        refresh_token()
        r = requests.get(f"{SPRING_API}{path}", headers=get_headers(), params=params, timeout=15)
    r.raise_for_status()
    return r.json()


# ── MongoDB ───────────────────────────────────────────────────────────────────

def get_mongo_db():
    """Return a pymongo Database handle for workflow_db."""
    client = pymongo.MongoClient(MONGO_URI, serverSelectionTimeoutMS=8000)
    return client[MONGO_DB]


# ── Workflow data — reads directly from MongoDB ───────────────────────────────

def load_workflows(max_retries: int = 4, retry_delay: int = 5) -> tuple[dict, dict]:
    """
    Load all workflows and their nodes directly from MongoDB.
    Retries up to max_retries times (retry_delay seconds apart) so that a slow
    MongoDB Atlas connection at container startup does not leave wf_map empty.

    Collections: 'workflows', 'workflow_nodo'
    Returns:
      wf_map  : {wfId -> {name, nodos, num_nodos, total_expected_min}}
      nodo_map: {nodoId -> {avgMinutes, order, total_nodos, wfId, name}}
    """
    for attempt in range(max_retries):
        try:
            db        = get_mongo_db()
            workflows = list(db["workflows"].find({}))

            wf_map:   dict = {}
            nodo_map: dict = {}

            for wf in workflows:
                wid = str(wf.get("_id", ""))
                if not wid:
                    continue

                all_nodos = sorted(
                    db["workflow_nodo"].find({"workflowId": wid}),
                    key=lambda n: n.get("order", 999),
                )
                nodos = [
                    n for n in all_nodos
                    if (n.get("nodeType") or "").lower() not in ("inicio", "fin", "start", "end")
                ]

                total_min = sum(n.get("avgMinutes") or 30 for n in nodos)
                wf_map[wid] = {
                    "name":               wf.get("name", ""),
                    "nodos":              [_nodo_doc(n) for n in nodos],
                    "num_nodos":          len(nodos),
                    "total_expected_min": max(total_min, 1),
                }
                for i, n in enumerate(nodos):
                    nid = str(n.get("_id", ""))
                    if not nid:
                        continue
                    nodo_map[nid] = {
                        "avgMinutes":  n.get("avgMinutes") or 30,
                        "order":       i,
                        "total_nodos": len(nodos),
                        "wfId":        wid,
                        "name":        n.get("name", "Nodo"),
                    }

            if wf_map:
                logger.info(f"load_workflows: {len(wf_map)} workflows, {len(nodo_map)} nodos")
                return wf_map, nodo_map

            # MongoDB responded but returned 0 workflows — data may not be seeded yet
            logger.warning(f"load_workflows: 0 workflows found (attempt {attempt + 1}/{max_retries})")

        except Exception as e:
            logger.warning(f"load_workflows attempt {attempt + 1}/{max_retries} failed: {e}")

        if attempt < max_retries - 1:
            time.sleep(retry_delay)

    logger.error("load_workflows: all retries exhausted — returning empty maps")
    return {}, {}


def _nodo_doc(n: dict) -> dict:
    """Convert a raw MongoDB nodo document to the dict shape expected by predictors."""
    return {
        "id":         str(n.get("_id", "")),
        "name":       n.get("name", ""),
        "nodeType":   n.get("nodeType", "proceso"),
        "order":      n.get("order", 0),
        "avgMinutes": n.get("avgMinutes") or 30,
        "workflowId": n.get("workflowId", ""),
    }
