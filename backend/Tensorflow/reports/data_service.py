"""
data_service.py
Lee datos de trámites directamente desde MongoDB (sin depender de la Spring API).
"""

import logging
from datetime import datetime

from core.api_client import get_mongo_db

logger = logging.getLogger(__name__)


class DataService:

    def __init__(self):
        self.db = get_mongo_db()
        logger.info("DataService listo (MongoDB directo).")

    # ─── Helpers de lookup ────────────────────────────────────────────────────

    def _build_lookups(self):
        """Carga diccionarios de lookup para joins en memoria."""
        db = self.db
        workflows   = {str(w["_id"]): w.get("name", "") for w in db["workflows"].find({}, {"_id": 1, "name": 1})}
        users       = {str(u["_id"]): u.get("name", "") for u in db["users"].find({}, {"_id": 1, "name": 1})}
        departments = {str(d["_id"]): d.get("name", "") for d in db["departments"].find({}, {"_id": 1, "name": 1})}
        nodos       = {
            str(n["_id"]): {
                "deptId": n.get("responsibleDepartmentId", ""),
                "wfId":   n.get("workflowId", ""),
            }
            for n in db["workflow_nodo"].find({}, {"_id": 1, "responsibleDepartmentId": 1, "workflowId": 1})
        }
        return workflows, users, departments, nodos

    def _enrich(self, t: dict, workflows: dict, users: dict, departments: dict, nodos: dict) -> dict:
        tid        = str(t.get("_id", ""))
        wf_id      = t.get("workflowId", "")
        nodo_id    = t.get("currentNodoId", "")
        user_id    = t.get("requestedById", "")

        nodo_info   = nodos.get(nodo_id, {})
        dept_id     = nodo_info.get("deptId", "")

        created_raw   = t.get("createdAt")
        completed_raw = t.get("updatedAt") if t.get("status") in ("COMPLETADO", "RECHAZADO") else None

        def _fmt(dt):
            if dt is None:
                return None
            if isinstance(dt, datetime):
                return dt.isoformat()
            return str(dt)

        return {
            "tramiteId":      tid,
            "code":           t.get("code", tid[:8]),
            "title":          t.get("title", ""),
            "workflowId":     wf_id,
            "workflowName":   workflows.get(wf_id, ""),
            "departmentName": departments.get(dept_id, ""),
            "status":         t.get("status", ""),
            "userName":       users.get(user_id, ""),
            "createdAt":      _fmt(created_raw),
            "completedAt":    _fmt(completed_raw),
        }

    # ─── Data fetching ────────────────────────────────────────────────────────

    def get_all_enriched(self) -> list[dict]:
        try:
            workflows, users, departments, nodos = self._build_lookups()
            tramites = list(self.db["tramites"].find({}))
            rows = [self._enrich(t, workflows, users, departments, nodos) for t in tramites]
            logger.info(f"DataService.get_all_enriched: {len(rows)} tramites.")
            return rows
        except Exception as e:
            logger.error(f"DataService.get_all_enriched error: {e}")
            return []

    def extract_context(self, rows: list[dict]) -> dict:
        depts     = sorted({r.get("departmentName", "") for r in rows if r.get("departmentName")})
        workflows = sorted({r.get("workflowName", "") for r in rows if r.get("workflowName")})
        return {"departments": depts, "workflows": workflows, "users": []}

    # ─── Tramite list (default) ───────────────────────────────────────────────

    def filter_rows(self, rows: list[dict], spec: dict) -> list[dict]:
        filters = spec.get("filters", {})

        if filters.get("code"):
            code_f = filters["code"].upper()
            return [r for r in rows if (r.get("code") or "").upper() == code_f]

        result = []
        for row in rows:
            if filters.get("departmentName"):
                dept_f = filters["departmentName"].lower()
                dept_r = (row.get("departmentName") or "").lower()
                if dept_f not in dept_r and dept_r not in dept_f:
                    continue
            if filters.get("workflowName"):
                wf_f = filters["workflowName"].lower()
                wf_r = (row.get("workflowName") or "").lower()
                if wf_f not in wf_r and wf_r not in wf_f:
                    continue
            if filters.get("status"):
                if (row.get("status") or "").upper() != filters["status"].upper():
                    continue
            created = row.get("createdAt", "")
            if filters.get("dateFrom") and created:
                if created[:10] < filters["dateFrom"]:
                    continue
            if filters.get("dateTo") and created:
                if created[:10] > filters["dateTo"]:
                    continue
            result.append(row)

        order_by  = spec.get("orderBy", "createdAt")
        order_dir = spec.get("orderDir", "desc")
        result.sort(key=lambda r: (r.get(order_by) or ""), reverse=(order_dir == "desc"))
        return result

    # ─── Department flow ──────────────────────────────────────────────────────

    def aggregate_department_flow(self, rows: list[dict], spec: dict) -> list[dict]:
        filters   = spec.get("filters", {})
        order_dir = spec.get("orderDir", "desc")

        counts: dict[str, int] = {}
        for row in rows:
            if filters.get("dateFrom"):
                if (row.get("createdAt") or "")[:10] < filters["dateFrom"]:
                    continue
            if filters.get("dateTo"):
                if (row.get("createdAt") or "")[:10] > filters["dateTo"]:
                    continue
            if filters.get("status"):
                if (row.get("status") or "").upper() != filters["status"].upper():
                    continue
            dept = row.get("departmentName") or "Sin departamento"
            counts[dept] = counts.get(dept, 0) + 1

        result = [{"departmentName": k, "total": v} for k, v in counts.items()]
        result.sort(key=lambda r: r["total"], reverse=(order_dir == "desc"))
        return result

    # ─── Average resolution time ─────────────────────────────────────────────

    def aggregate_avg_time(self, rows: list[dict], spec: dict) -> list[dict]:
        filters   = spec.get("filters", {})
        order_dir = spec.get("orderDir", "desc")

        groups: dict[tuple, list[float]] = {}
        for row in rows:
            if (row.get("status") or "").upper() not in ("COMPLETADO", "RECHAZADO"):
                continue
            created_raw   = row.get("createdAt")
            completed_raw = row.get("completedAt")
            if not created_raw or not completed_raw:
                continue
            if filters.get("departmentName"):
                dept_f = filters["departmentName"].lower()
                dept_r = (row.get("departmentName") or "").lower()
                if dept_f not in dept_r and dept_r not in dept_f:
                    continue
            if filters.get("workflowName"):
                wf_f = filters["workflowName"].lower()
                wf_r = (row.get("workflowName") or "").lower()
                if wf_f not in wf_r and wf_r not in wf_f:
                    continue
            try:
                created   = datetime.fromisoformat(created_raw[:19].replace("T", " "))
                completed = datetime.fromisoformat(completed_raw[:19].replace("T", " "))
                minutes   = max(0.0, (completed - created).total_seconds() / 60)
            except Exception:
                continue

            key = (row.get("departmentName") or "—", row.get("workflowName") or "—")
            groups.setdefault(key, []).append(minutes)

        result = []
        for (dept, wf), mins in groups.items():
            avg = round(sum(mins) / len(mins), 1) if mins else 0.0
            result.append({"departmentName": dept, "workflowName": wf, "avgMinutes": avg, "count": len(mins)})

        result.sort(key=lambda r: r["avgMinutes"], reverse=(order_dir == "desc"))
        return result

    # ─── Legacy query (kept for /nlp/download) ───────────────────────────────

    def query(self, spec: dict) -> list[dict]:
        try:
            rows = self.get_all_enriched()
            return self.filter_rows(rows, spec)
        except Exception as e:
            logger.error(f"DataService.query error: {e}")
            return []
