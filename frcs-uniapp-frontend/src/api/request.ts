import type { CaseInfo, CaseQuery } from '../types/case'
import type { LawInfo } from '../types/law'

const caseMockData: CaseInfo[] = [
  { id: 1, country: '美国', title: '跨境软件许可合同纠纷案', englishTitle: 'Cross-border Software License Dispute', court: '美国纽约南区联邦地区法院', date: '2024-05-18', summary: '涉及跨境软件许可与适用法律的争议。', type: '合同纠纷', caseNumber: '24-CV-1024', source: '法院公开文书', aiSummaryStatus: 'completed', courtOpinion: '法院认为合同中的法律选择条款有效。', legalProvisions: ['美国统一商法典'] },
  { id: 2, country: '欧盟', title: '跨境数据传输合规案', englishTitle: 'Cross-border Data Transfer Compliance Case', court: '欧盟法院', date: '2023-11-02', summary: '围绕个人数据跨境传输合法基础展开审查。', type: '行政争议', caseNumber: 'C-221/23', source: 'EUR-Lex', aiSummaryStatus: 'completed', courtOpinion: '数据控制者应证明已采取充分保护措施。', legalProvisions: ['GDPR 第46条'] },
]

const lawMockData: LawInfo[] = [
  { id: 1, country: '美国', category: '合同法', title: '统一商法典（节选）', content: '本法典规定了商事交易的基本规则。', updatedAt: '2024-01-01', interpretation: '适用于多数商事买卖场景。', revisions: ['2024 年修订说明'], relatedCaseIds: [1] },
  { id: 2, country: '欧盟', category: '数据保护', title: '通用数据保护条例（GDPR）', content: '本条例保护自然人的个人数据处理权益。', updatedAt: '2023-09-15', interpretation: '跨境传输需要满足适当保障措施。', revisions: ['2023 年执行指引'], relatedCaseIds: [2] },
]

export const caseApi = {
  async list(query: CaseQuery = {}): Promise<CaseInfo[]> {
    const keyword = query.keyword?.trim().toLowerCase()
    return caseMockData.filter((item) => !keyword || `${item.title}${item.englishTitle}`.toLowerCase().includes(keyword))
  },
  async detail(id: number): Promise<CaseInfo | undefined> {
    return caseMockData.find((item) => item.id === id)
  },
}

export const lawApi = {
  async list(): Promise<LawInfo[]> { return lawMockData },
  async detail(id: number): Promise<LawInfo | undefined> { return lawMockData.find((item) => item.id === id) },
}
