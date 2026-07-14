export interface CaseInfo {
  id: number
  country: string
  title: string
  englishTitle: string
  court: string
  date: string
  summary: string
  type?: string
  caseNumber?: string
  source?: string
  aiSummaryStatus?: 'completed' | 'pending'
  courtOpinion?: string
  legalProvisions?: string[]
}

export interface CaseQuery {
  keyword?: string
  country?: string
  date?: string
  source?: string
}
