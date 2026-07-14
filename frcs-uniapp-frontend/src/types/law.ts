export interface LawInfo {
  id: number
  country: string
  category: string
  title: string
  content: string
  updatedAt?: string
  interpretation?: string
  revisions?: string[]
  relatedCaseIds?: number[]
}
