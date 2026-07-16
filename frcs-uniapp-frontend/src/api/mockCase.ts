// src/api/mockCase.ts

export interface CaseItem {
  id: string;
  title: string;
  englishTitle: string;
  country: string;
  court: string;
  date: string;
  type: string;
  aiSummaryStatus: 'completed' | 'none';
}

const delay = (ms: number) => new Promise(resolve => setTimeout(resolve, ms));

const caseTypes = ['合同纠纷', '知识产权', '国际贸易', '数据合规', '反垄断'];
const courts = ['美国纽约南区联邦地区法院', '欧盟法院', '日本最高裁判所', '美国最高法院'];

/**
 * 模拟后端拉取案例搜索结果的接口
 * @param keyword 关键词（模糊搜索）
 * @param page 当前页码
 * @param pageSize 每页条数
 */
export const fetchCasesMock = async (
  keyword: string, 
  page: number = 1, 
  pageSize: number = 10
): Promise<{ data: CaseItem[], total: number, hasMore: boolean }> => {
  
  await delay(500); // 模拟网络延迟

  const mockData: CaseItem[] = [];
  // 如果没有关键词，假定总数为 50；有关键词假定搜到了 25 条
  const total = keyword ? 25 : 50; 
  const start = (page - 1) * pageSize;
  const end = Math.min(start + pageSize, total);

  for (let i = start; i < end; i++) {
    const isUS = i % 2 === 0;
    mockData.push({
      id: `case-${Date.now()}-${i}`,
      title: keyword ? `【匹配: ${keyword}】的涉外争议案件` : `涉外标准典型案例指引第${i + 1}号`,
      englishTitle: 'Cross-border Dispute Resolution Case',
      country: isUS ? '美国' : '欧盟',
      court: courts[i % courts.length],
      date: `202${Math.floor(Math.random() * 5)}-0${Math.floor(Math.random() * 9) + 1}-1${Math.floor(Math.random() * 9)}`,
      type: caseTypes[i % caseTypes.length],
      aiSummaryStatus: i % 3 === 0 ? 'completed' : 'none' // 1/3 的概率有 AI 摘要
    });
  }

  return {
    data: mockData,
    total,
    hasMore: end < total
  };
};