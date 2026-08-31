/**
 * 后端 /api/cases/search 返回的单条案例（CaseBaseInfo）
 */
export interface CaseBaseInfo {
  case_id: string
  case_name: string
  judgement_date?: string
  country?: string
  /** 摘要/关键词 */
  tags?: string
  citationCount?: number
  favoritedCount?: number
  isfavored?: boolean
  original_document_url?: string
}

/** 收藏列表项（FavoriteInfo） */
export interface FavoriteInfo {
  caseId: string
  caseName: string
  judgementDate?: string
  country?: string
  tags?: string
  citationCount?: number
  originalDocumentUrl?: string
  favoriteDate?: string
}

/** 浏览历史项（BrowseHistoryInfo） */
export interface BrowseHistoryInfo {
  caseId: string
  caseName: string
  judgementDate?: string
  country?: string
  tags?: string
  citationCount?: number
  originalDocumentUrl?: string
  browseTime?: string
}

/** 搜索分页结果 */
export interface SearchResult {
  totalCount: number
  cases: CaseBaseInfo[]
}

/** 收藏分页结果 */
export interface FavoriteListResult {
  totalCount: number
  favoriteInfoList: FavoriteInfo[]
}

/** 历史分页结果 */
export interface HistoryListResult {
  totalCount: number
  browseHistoryInfoList: BrowseHistoryInfo[]
}

export type SummaryStatusType = 'IDLE' | 'RUNNING' | 'DONE' | 'FAILED'

/** 异步摘要状态（SummaryStatusResVO） */
export interface SummaryStatus {
  status: SummaryStatusType
  content?: string
  errorMessage?: string
  summaryUpdatedAtMs?: number
}

/** 知识库检索命中项 */
export interface KbHit {
  chunkId: string
  sourceId: string
  title: string
  score?: number
  preview?: string
}

/** Agent 执行轨迹步骤 */
export interface TraceStep {
  name: string
  status: string
  detail: string
}

/** Agent 问答返回（AgentAskResVO） */
export interface AgentResult {
  answer: string
  route?: string
  kbHitCount?: number
  searchTotalCount?: number
  relatedCases?: CaseBaseInfo[]
  kbHits?: KbHit[]
  trace?: TraceStep[]
}

/** 知识库查询返回（KbQueryResVO） */
export interface KbQueryResult {
  answer: string
  hitCount: number
  hits: KbHit[]
}

/** 案例检索入参 */
export interface CaseQuery {
  keyword?: string
  country?: string
  period?: number
  sources?: string
  pagenum?: number
  pagesize?: number
}
