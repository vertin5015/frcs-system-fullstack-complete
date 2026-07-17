#!/usr/bin/env bash
set -Eeuo pipefail

BASE_URL="${1:-http://127.0.0.1:80}"

if ! command -v curl >/dev/null 2>&1; then
  echo "curl is not installed; skip demo KB seed."
  exit 0
fi

echo "Checking local KB demo content..."
for _ in $(seq 1 30); do
  if curl -fsS "${BASE_URL}/api/kb/query" \
      -H 'Content-Type: application/json' \
      --data '{"question":"涉外合同纠纷 管辖 法律适用","language":"zh","topK":1}' >/tmp/frcs-kb-check.json 2>/dev/null; then
    break
  fi
  sleep 2
done

if [[ ! -s /tmp/frcs-kb-check.json ]]; then
  echo "Backend KB API is not ready; skip demo KB seed."
  exit 0
fi

if ! grep -q '"hitCount"[[:space:]]*:[[:space:]]*0' /tmp/frcs-kb-check.json; then
  echo "Local KB already has content; skip demo KB seed."
  exit 0
fi

echo "Seeding demo legal knowledge into local KB..."
curl -fsS "${BASE_URL}/api/kb/ingest" \
  -H 'Content-Type: application/json' \
  --data-binary @- <<'JSON' >/tmp/frcs-kb-seed.json
{
  "sourceId": "demo-foreign-legal-rag",
  "title": "涉外案例查询与法律知识系统演示知识库",
  "language": "zh",
  "content": "涉外民商事案件通常需要先判断法院管辖、准据法以及域外证据的可采性。合同纠纷中，当事人可以依法协议选择合同适用的法律；没有选择的，通常适用与合同有最密切联系的法律。跨境买卖、服务、技术许可、知识产权许可等场景中，法院会结合合同签订地、履行地、标的物所在地、当事人住所地、争议发生地等因素进行判断。涉外合同争议的处理步骤包括：一，确认主体身份和涉外因素；二，审查管辖条款、仲裁条款或专属管辖规则；三，确定准据法；四，围绕合同成立、履行、违约、损害赔偿、免责事由进行事实认定；五，判断判决或仲裁裁决的承认与执行路径。\\n\\n涉外知识产权纠纷常见争点包括权利归属、侵权行为地、网络传播行为的地域连接点、许可合同解释、损害赔偿计算以及禁令救济。对于网络侵权，法院通常关注服务器所在地、用户可访问地、主要市场影响地、被告经营地等连接因素。知识产权许可合同同时可能涉及合同准据法与知识产权保护地法，需要区分合同义务和权利效力问题。\\n\\n涉外证据方面，域外形成的证据可能需要履行公证、认证或其他证明手续。电子数据应重点审查来源、完整性、生成和保存过程、哈希校验、时间戳、取证主体和取证环境。机器翻译材料通常只能辅助理解，关键证据应提交准确译文。\\n\\nRAG 法律问答助手的工作边界是：根据已经入库的案例、法规、课堂资料和爬虫结果进行检索增强回答；当知识库没有材料或爬虫不可用时，应明确提示依据不足，不能编造案例编号、法院名称或裁判结论。Agent 流程可以先尝试案例搜索，再把搜索结果或本地知识库片段作为上下文生成答案；如果爬虫服务不可用，应降级为本地知识库问答，并在 trace 中说明失败原因。"
}
JSON

cat /tmp/frcs-kb-seed.json
echo
