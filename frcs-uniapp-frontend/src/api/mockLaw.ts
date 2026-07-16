// src/api/mockLaw.ts

export interface LawItem {
  id: string;
  title: string;
  country: string;
  category: string;
  publishDate: string;
  status: string;
}

// 预设一些真实感较强的法律条文用于 Mock
const baseTitles = [
  '统一商法典 (UCC)', '联邦民事诉讼规则', '反海外腐败法 (FCPA)', 
  '通用数据保护条例 (GDPR)', '数字市场法案 (DMA)', '人工智能法案 (AI Act)',
  '民法典', '公司法', '反垄断法', '个人信息保护法'
];

// 模拟延时请求
const delay = (ms: number) => new Promise(resolve => setTimeout(resolve, ms));

/**
 * 模拟后端拉取法典列表的接口
 * @param country 国家
 * @param category 分类
 * @param page 当前页码
 * @param pageSize 每页条数
 */
export const fetchLawsMock = async (
  country: string, 
  category: string, 
  page: number = 1, 
  pageSize: number = 10
): Promise<{ data: LawItem[], total: number, hasMore: boolean }> => {
  
  await delay(600); // 模拟网络延时 600ms

  const mockData: LawItem[] = [];
  // 模拟数据总数（每个分类下假定有 35 条数据）
  const total = 35; 
  const start = (page - 1) * pageSize;
  const end = Math.min(start + pageSize, total);

  for (let i = start; i < end; i++) {
    const randomTitle = baseTitles[Math.floor(Math.random() * baseTitles.length)];
    mockData.push({
      id: `${country}-${category}-${i + 1}`,
      title: `【${country}】${randomTitle} - 释义与应用指南(第${i + 1}卷)`,
      country: country === '全部国家' ? '多国通用' : country,
      category: category === '全部' ? '综合' : category,
      publishDate: `202${Math.floor(Math.random() * 5)}-0${Math.floor(Math.random() * 9) + 1}-1${Math.floor(Math.random() * 9)}`,
      status: '现行有效'
    });
  }

  return {
    data: mockData,
    total,
    hasMore: end < total
  };
};