// src/api/mockCase.ts

export interface CaseItem {
  id: string;              // 唯一标识（使用数字字符串以便判断单双数）
  title: string;           // 案件名称
  englishTitle: string;    // 英文名
  country: string;         // 判决国家
  court: string;           // 判决法院
  caseNumber: string;      // 案号
  date: string;            // 判决时间 YYYY-MM-DD
  type: string;            // 案件类型 (用于筛选)
  dataSource: string;      // 数据源 (用于筛选)
  summary: string;         // 案件简介
  courtOpinion: string;    // 法院观点
  legalCitations: string[];// 相关法律援引
  aiSummaryStatus: 'completed' | 'none'; // AI摘要状态
}

// 预设的 15 个具备完备真实属性的涉外法律案例
export const mockCases: CaseItem[] = [
  { 
    id: '1', 
    title: '苹果与三星智能手机专利侵权案', 
    englishTitle: 'Apple Inc. v. Samsung Electronics Co.', 
    country: '美国', 
    court: '美国加州北区联邦地区法院', 
    caseNumber: '5:11-cv-01846',
    date: '2018-05-24', 
    type: '知识产权', 
    dataSource: 'Westlaw',
    summary: '苹果公司起诉三星电子其Galaxy系列手机和平板电脑侵犯了iPhone的多项实用新型专利和外观设计专利（如圆角矩形设计、双击放大等功能）。',
    courtOpinion: '陪审团及法院认定三星确实侵犯了苹果的多项外观设计及实用专利，并驳回了三星大部分反诉请求。法院认为外观设计专利的侵权赔偿应基于侵权产品的整体利润。',
    legalCitations: ['35 U.S.C. § 289 (外观设计专利侵权)', '35 U.S.C. § 284 (损害赔偿)'],
    aiSummaryStatus: 'completed' 
  },
  { 
    id: '2', 
    title: '谷歌购物搜索反垄断罚款案', 
    englishTitle: 'Google Search (Shopping) Antitrust Case', 
    country: '欧盟', 
    court: '欧洲常设法院 (General Court)', 
    caseNumber: 'T-612/17',
    date: '2021-11-10', 
    type: '反垄断', 
    dataSource: 'LexisNexis',
    summary: '欧盟委员会指控谷歌在其通用搜索结果中，非法偏袒自家的比价购物服务（Google Shopping），降低了竞争对手的搜索排名，构成滥用市场支配地位。',
    courtOpinion: '法院维持了欧盟委员会24.2亿欧元的罚款决定，认定谷歌通过算法偏袒自身服务，对市场竞争和消费者选择造成了实质性的负面影响，违反了公平竞争原则。',
    legalCitations: ['《欧盟运行条约》第102条 (TFEU Art. 102)'],
    aiSummaryStatus: 'completed' 
  },
  { 
    id: '3', 
    title: '微软收购动视暴雪反垄断审查案', 
    englishTitle: 'FTC v. Microsoft Corp. & Activision Blizzard', 
    country: '美国', 
    court: '美国加州北区联邦地区法院', 
    caseNumber: '3:23-cv-02880',
    date: '2023-07-11', 
    type: '反垄断', 
    dataSource: '官方公报',
    summary: '美国联邦贸易委员会(FTC)申请初步禁令，试图阻止微软以687亿美元收购动视暴雪，理由是该收购可能抑制主机游戏及云游戏市场的竞争。',
    courtOpinion: '法院驳回了FTC的初步禁令申请，认为FTC未能提供充分证据证明该收购会实质性减少竞争。法院指出，微软已承诺将《使命召唤》等游戏保留在PlayStation平台上。',
    legalCitations: ['《克莱顿法》第7条 (Clayton Act § 7)', '《联邦贸易委员会法》第13(b)条'],
    aiSummaryStatus: 'none' 
  },
  { 
    id: '4', 
    title: '索尼音乐娱乐著作权纠纷案', 
    englishTitle: 'Sony Music Entertainment v. ISP Copyright Dispute', 
    country: '日本', 
    court: '东京地方裁判所', 
    caseNumber: '令和4年(ワ)第1234号',
    date: '2022-03-15', 
    type: '知识产权', 
    dataSource: '裁判文书网',
    summary: '索尼音乐指控某互联网服务提供商(ISP)未能及时屏蔽未经授权分享受版权保护音乐文件的用户，要求其承担间接侵权责任。',
    courtOpinion: '法院裁定ISP无需承担直接侵权责任，但在收到明确的侵权通知后若未采取合理措施（如断开链接），则构成帮助侵权，需承担连带赔偿责任。',
    legalCitations: ['日本《著作权法》第119条', '《特定电气通信役务提供者损害赔偿责任限制及发信者信息披露法》'],
    aiSummaryStatus: 'completed' 
  },
  { 
    id: '5', 
    title: '亚马逊GDPR数据隐私合规罚款案', 
    englishTitle: 'Amazon Europe Core S.à r.l. GDPR Compliance', 
    country: '欧盟', 
    court: '卢森堡国家数据保护委员会 (CNPD)', 
    caseNumber: 'CNPD-2021-001',
    date: '2021-07-16', 
    type: '数据合规', 
    dataSource: 'HUDOC',
    summary: '法国隐私保护组织La Quadrature du Net代表万名消费者投诉亚马逊的定向广告系统在未获得有效同意的情况下处理用户个人数据。',
    courtOpinion: '监管机构认定亚马逊在核心业务中的数据处理系统违反了GDPR的同意原则，开出了创纪录的7.46亿欧元罚款，并要求其修改隐私合规架构。',
    legalCitations: ['《通用数据保护条例》(GDPR) 第6、7、12条'],
    aiSummaryStatus: 'completed' 
  },
  { 
    id: '6', 
    title: '特斯拉自动驾驶技术商业机密案', 
    englishTitle: 'Tesla, Inc. v. Guangzhi Cao', 
    country: '美国', 
    court: '加州高等法院', 
    caseNumber: '19-CV-344605',
    date: '2023-01-20', 
    type: '知识产权', 
    dataSource: 'Westlaw',
    summary: '特斯拉起诉其前自动驾驶工程师曹光植，指控其在离职前将包含自动驾驶源代码（Autopilot）的机密文件上传至个人iCloud账户并涉嫌泄露给竞争对手。',
    courtOpinion: '双方最终达成和解。曹承认上传了文件但否认转移给第三方。法院强调了企业在保护核心算法作为商业秘密时，保密措施的有效性与离职员工审计的重要性。',
    legalCitations: ['《保护商业机密法》(DTSA)', '加州《统一商业秘密法》(CUTSA)'],
    aiSummaryStatus: 'none' 
  },
  { 
    id: '7', 
    title: 'Meta跨大西洋数据传输违规案', 
    englishTitle: 'DPC Ireland v. Meta Platforms Ireland Limited', 
    country: '欧盟', 
    court: '爱尔兰数据保护委员会 (DPC)', 
    caseNumber: 'IN-20-8-1',
    date: '2023-05-22', 
    type: '数据合规', 
    dataSource: 'LexisNexis',
    summary: '继“隐私盾”协议被欧盟法院宣告无效后，Meta继续使用标准合同条款(SCC)将欧盟用户的个人数据传输至美国服务器，被指控无法防范美国情报机构的监控。',
    courtOpinion: 'DPC认定Meta的传输行为违反GDPR，对其处以12亿欧元罚款，并下令在5个月内停止未来向美国的非法数据传输，6个月内停止处理已非法存储的数据。',
    legalCitations: ['《通用数据保护条例》(GDPR) 第46条', '欧盟法院 Schrems II 判决'],
    aiSummaryStatus: 'completed' 
  },
  { 
    id: '8', 
    title: '任天堂游戏破解与模拟器侵权案', 
    englishTitle: 'Nintendo Co., Ltd. v. MariCar Inc.', 
    country: '日本', 
    court: '日本最高裁判所', 
    caseNumber: '令和2年(受)第1040号',
    date: '2024-02-18', 
    type: '知识产权', 
    dataSource: '官方公报',
    summary: 'MariCar公司在东京提供卡丁车租赁服务，并向顾客提供任天堂《马里奥赛车》角色的服装，任天堂指控其侵犯版权及违反不正当竞争法。',
    courtOpinion: '最高裁驳回了MariCar的上诉，维持二审判决，认定其未经许可使用著名角色元素进行商业营利，属于搭便车的不正当竞争行为，判令支付5000万日元赔偿并停止使用。',
    legalCitations: ['日本《不正当竞争防止法》第2条', '日本《著作权法》'],
    aiSummaryStatus: 'completed' 
  },
  { 
    id: '9', 
    title: '高通芯片排他性交易反垄断上诉案', 
    englishTitle: 'Qualcomm Inc. v. European Commission', 
    country: '欧盟', 
    court: '欧盟普通法院 (General Court)', 
    caseNumber: 'T-235/18',
    date: '2022-06-15', 
    type: '反垄断', 
    dataSource: 'HUDOC',
    summary: '欧盟委员会曾对高通罚款9.97亿欧元，指控其向苹果支付数十亿美元以确保苹果的设备独家使用高通的LTE基带芯片，排挤了竞争对手（如英特尔）。',
    courtOpinion: '普通法院撤销了欧盟委员会的罚款决定。法院认为，委员会在程序上存在违规（未妥善记录会谈），且未能充分证明高通的排他性支付实际导致了反竞争的封锁效应。',
    legalCitations: ['《欧盟运行条约》第102条 (TFEU Art. 102)', '欧盟基本权利宪章第41条'],
    aiSummaryStatus: 'none' 
  },
  { 
    id: '10', 
    title: '丰田汽车全球供应链违约索赔案', 
    englishTitle: 'Toyota Motor Corp. v. Global Supplier Ltd.', 
    country: '日本', 
    court: '名古屋地方裁判所', 
    caseNumber: '令和3年(ワ)第4051号',
    date: '2023-11-05', 
    type: '合同纠纷', 
    dataSource: '裁判文书网',
    summary: '一家关键半导体部件供应商因不可抗力（疫情与火灾）未能按期向丰田交付零件，导致丰田多条生产线停产。丰田提起违约索赔。',
    courtOpinion: '法院判定部分不可抗力抗辩成立，免除了供应商在火灾期间的延迟交付责任；但认定供应商在恢复生产后的资源分配存在偏袒，对后续的违约行为需承担部分赔偿责任。',
    legalCitations: ['日本《民法典》第415条 (债务不履行)', '日本《民法典》第419条'],
    aiSummaryStatus: 'completed' 
  },
  { 
    id: '11', 
    title: '甲骨文诉谷歌Java API版权争议案', 
    englishTitle: 'Oracle America, Inc. v. Google LLC', 
    country: '美国', 
    court: '美国最高法院', 
    caseNumber: '18-956',
    date: '2021-04-05', 
    type: '知识产权', 
    dataSource: 'Westlaw',
    summary: '甲骨文指控谷歌在开发Android系统时，未经许可复制了Java SE大约11500行API（应用程序编程接口）声明代码。',
    courtOpinion: '最高法院以6比2的投票结果裁定，谷歌对Java API代码的复制构成了“合理使用”（Fair Use）。法院指出，API具有强烈的实用性，谷歌的复制仅限于允许程序员将其原有技能投入新平台所需的代码。',
    legalCitations: ['美国《版权法》第107条 (17 U.S.C. § 107 - Fair Use)'],
    aiSummaryStatus: 'completed' 
  },
  { 
    id: '12', 
    title: 'TikTok未成年人数据安全合规案', 
    englishTitle: 'TikTok Data Security for Minors Compliance', 
    country: '欧盟', 
    court: '欧洲数据保护委员会 (EDPB)', 
    caseNumber: 'EDPB-2023-TK',
    date: '2023-09-15', 
    type: '数据合规', 
    dataSource: '官方公报',
    summary: '爱尔兰DPC调查发现TikTok在2020年期间对儿童账户的默认设置公开，且其“家庭配对”功能未能有效验证绑定账户是否真为儿童的父母或监护人。',
    courtOpinion: '监管机构裁定TikTok未能向未成年人提供充分的透明度保护，未能落实“设计即隐私”原则。处以3.45亿欧元的行政罚款，并要求其在规定期限内整改其数据处理操作。',
    legalCitations: ['《通用数据保护条例》(GDPR) 第5, 12, 24, 25条'],
    aiSummaryStatus: 'completed' 
  },
  { 
    id: '13', 
    title: '软银集团WeWork投资并购毁约案', 
    englishTitle: 'WeWork Founder v. SoftBank Group Corp.', 
    country: '美国', 
    court: '特拉华州衡平法院 (Delaware Court of Chancery)', 
    caseNumber: '2020-0258-AGB',
    date: '2021-02-26', 
    type: '合同纠纷', 
    dataSource: 'LexisNexis',
    summary: 'WeWork创始人起诉软银集团，指控软银违反了2019年达成的价值30亿美元的股票收购要约（Tender Offer）协议，非法撤回了投资承诺。',
    courtOpinion: '在进入庭审前，双方达成全面和解。软银同意支付约15亿美元以解决这一诉讼，法院批准了和解协议并驳回了案件。本案凸显了重大并购交易中前提条件条款（Closing Conditions）的法律效力。',
    legalCitations: ['特拉华州公司法 (DGCL)'],
    aiSummaryStatus: 'none' 
  },
  { 
    id: '14', 
    title: '辉瑞与莫德纳mRNA疫苗专利争议', 
    englishTitle: 'ModernaTx, Inc. v. Pfizer Inc. and BioNTech SE', 
    country: '美国', 
    court: '美国麻萨诸塞州联邦地区法院', 
    caseNumber: '1:22-cv-11378',
    date: '2022-08-26', 
    type: '知识产权', 
    dataSource: 'Westlaw',
    summary: 'Moderna起诉辉瑞和BioNTech，指控其联合研发的COVID-19疫苗（Comirnaty）侵犯了Moderna在疫情前多年研发的有关mRNA技术和脂质纳米颗粒递送系统的基础专利。',
    courtOpinion: '案件目前在审理中。辉瑞提出了专利无效的抗辩。法院在早期马克曼听证会（Markman Hearing）上对涉案专利的关键权利要求术语进行了有利或不利的解释。',
    legalCitations: ['35 U.S.C. § 271 (专利侵权)', '《拜杜法案》(Bayh-Dole Act 涉及政府资助抗辩)'],
    aiSummaryStatus: 'completed' 
  },
  { 
    id: '15', 
    title: '波音公司737MAX航空购销合同违约案', 
    englishTitle: 'Aviation Leasing Co. v. The Boeing Company', 
    country: '美国', 
    court: '伊利诺伊州北区联邦地区法院', 
    caseNumber: '1:20-cv-03456',
    date: '2020-10-12', 
    type: '合同纠纷', 
    dataSource: '官方公报',
    summary: '由于波音737 MAX机型因两起致命空难遭遇全球禁飞，某大型航空租赁公司起诉波音违约，要求取消尚未交付的数十架飞机的订单并退还定金。',
    courtOpinion: '波音试图以合同中的不可抗力条款（Excusable Delay）进行抗辩。法院认定，设计缺陷和监管机构的禁飞令不能简单归属于常规的不可抗力，允许租赁公司取消部分严重延误的订单并获得赔偿。',
    legalCitations: ['《统一商法典》(UCC) 第2编 (货物销售)', '华盛顿州合同法'],
    aiSummaryStatus: 'completed' 
  }
];

const delay = (ms: number) => new Promise(resolve => setTimeout(resolve, ms));

/**
 * 模拟后端拉取案例搜索结果的接口 (适配检索页)
 * @param keyword 关键词（将匹配标题、英文名、案号、简介）
 * @param page 当前页码
 * @param pageSize 每页条数
 */
export const fetchCasesMock = async (
  keyword: string, 
  page: number = 1, 
  pageSize: number = 10
): Promise<{ data: CaseItem[], total: number, hasMore: boolean }> => {
  
  await delay(400); // 模拟网络延迟

  let filtered = mockCases;
  
  // 复杂的本地模糊搜索逻辑
  if (keyword) {
    const kw = keyword.toLowerCase();
    filtered = mockCases.filter(c => 
      c.title.toLowerCase().includes(kw) || 
      c.englishTitle.toLowerCase().includes(kw) || 
      c.caseNumber.toLowerCase().includes(kw) ||
      c.summary.toLowerCase().includes(kw) ||
      c.country.includes(kw) || 
      c.type.includes(kw)
    );
  }

  const total = filtered.length;
  const start = (page - 1) * pageSize;
  const end = Math.min(start + pageSize, total);
  
  return { 
    data: filtered.slice(start, end), 
    total, 
    hasMore: end < total 
  };
};

/**
 * 模拟获取用户收藏列表的接口 (适配收藏页)
 * 核心逻辑：设定 `id` 为单数 (1, 3, 5, 7...) 的案例默认为被收藏的案例
 */
export const fetchFavoritesMock = async (): Promise<CaseItem[]> => {
  await delay(300);
  
  // 筛选出被收藏的案例：判断 id 转换为数字后是否为单数
  const favoriteCases = mockCases.filter(c => {
    const numId = parseInt(c.id, 10);
    return !isNaN(numId) && numId % 2 !== 0;
  });

  return favoriteCases;
};

export const fetchCaseDetailMock = async (id: number | string) => {
  return new Promise<CaseItem>((resolve, reject) => {
    setTimeout(() => {
      const targetCase = mockCases.find(c => String(c.id) === String(id));
      if (targetCase) {
        resolve(targetCase);
      } else {
        reject(new Error('未找到对应案例'));
      }
    }, 300); // 模拟网络延迟
  });
};