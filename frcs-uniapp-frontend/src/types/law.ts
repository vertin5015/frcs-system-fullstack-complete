/**
 * 知识库条文信息：后端无独立“法律条文”列表接口，
 * 小程序端通过 /api/kb/query 检索命中后展示（preview 即条文片段）。
 */
export interface LawInfo {
  id: string
  sourceId: string
  title: string
  country?: string
  category?: string
  content: string
  preview?: string
  score?: number
  updatedAt?: string
  status?: string
}
