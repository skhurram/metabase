import * as ML from "cljs/metabase.lib.js";
import type { Field } from "metabase-types/api";
import type { OrderByClause, Query } from "./types";

export function orderableColumns(query: Query): Field[] {
  return ML.orderable_columns(query);
}

export function orderBys(query: Query): OrderByClause[] {
  const result = ML.order_bys(query);
  return result === null ? [] : result;
}

declare function OrderByFn(query: Query, field: Field): Query;
declare function OrderByFn(
  query: Query,
  stageIndex: number,
  field: Field,
): Query;

export const orderBy: typeof OrderByFn = ML.order_by;

type OrderByDirection = "asc" | "desc";

declare function OrderByClauseFn(
  query: Query,
  stageNumber: number,
  field: Field,
  direction?: OrderByDirection,
): OrderByClause;
declare function OrderByClauseFn(
  query: Query,
  stageNumber: number,
  field: Field,
  direction?: OrderByDirection,
): OrderByClause;

export const orderByClause: typeof OrderByClauseFn = ML.order_by_clause;
