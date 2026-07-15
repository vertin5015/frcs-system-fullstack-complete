import type { CaseInfo, CaseQuery } from '../types/case'
import type { LawInfo } from '../types/law'
import type { User } from '../types/user'

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

//模拟的用户数据
const mockUser: User = {
  id: 1,
  username: 'admin',
  email: 'admin@example.com', // 假设你的 User type 中有这些字段
  avatar: 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png',
}

// [!code ++] 新增：认证相关的 Mock API
export const authApi = {
  /**
   * 模拟登录接口
   * @param email 用户名 (测试请用 admin)
   * @param password 密码 (测试请用 123456)
   */
  async login(email: string, password: string): Promise<{ token: string; user: User }> {
    return new Promise((resolve, reject) => {
      setTimeout(() => {
        if (email === 'admin@example.com' && password === '123456') {
          resolve({
            token: 'mock-jwt-token-admin-123456',
            user: mockUser,
          })
        } else {
          reject(new Error('用户名或密码错误'))
        }
      }, 500) // 500ms 延迟
    })
  },

  //微信一键登录
  async wechatLogin(code: string): Promise<{ token: string; user: User }> {
    await new Promise(resolve => setTimeout(resolve, 500)) // 模拟网络延迟
    console.log('后端收到微信 code:', code) // 模拟后端接收
    return {
      token: 'mock-wechat-token',
      user: { 
        id: 999, 
        username: '微信用户', 
        email: 'wechat_user@example.com', 
        avatar: 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png' 
      }
    }
  },

  //游客登录
  async guestLogin(): Promise<{ token: string; user: User }> {
    await new Promise(resolve => setTimeout(resolve, 300)) // 稍微快一点
    return {
      token: 'mock-guest-token',
      user: { 
        id: 0, 
        username: '游客', 
        email: 'guest@example.com', 
        avatar: '' 
      }
    }
  }
}

