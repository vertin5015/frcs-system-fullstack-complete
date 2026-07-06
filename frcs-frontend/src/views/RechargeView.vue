<template>
  <div class="recharge-page">
    <div class="recharge-header">
      <h2>{{ lang === "zh" ? "购买 AI 摘要次数" : "Buy AI summary credits" }}</h2>
      <p class="sub">
        {{ lang === "zh" ? "当前剩余：" : "Balance: " }}
        <strong>{{ creditsDisplay }}</strong>
      </p>
      <p class="hint">{{ channelHint }}</p>
    </div>

    <el-row :gutter="16" class="pkg-row">
      <el-col v-for="p in packages" :key="p.id" :xs="24" :sm="12" :md="8">
        <el-card shadow="hover" class="pkg-card">
          <div class="pkg-title">{{ p.label }}</div>
          <div class="pkg-credits">{{ p.credits }} {{ lang === "zh" ? "次" : " credits" }}</div>
          <div class="pkg-price">{{ formatPrice(p) }}</div>
          <el-button type="primary" class="pkg-btn" @click="buy(p)" :loading="orderingId === p.id">
            {{ lang === "zh" ? "购买" : "Buy" }}
          </el-button>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="mockDialogVisible" :title="lang === 'zh' ? '模拟支付' : 'Mock payment'" width="420px">
      <p>{{ lang === "zh" ? "订单号：" : "Order: " }}<code>{{ pendingOrderNo }}</code></p>
      <p class="mock-tip">{{ lang === "zh" ? "开发/测试环境可点击确认完成支付并到账。" : "Click confirm to complete payment (test)." }}</p>
      <template #footer>
        <el-button @click="mockDialogVisible = false">{{ lang === "zh" ? "取消" : "Cancel" }}</el-button>
        <el-button type="primary" :loading="mockConfirming" @click="confirmMock">{{ lang === "zh" ? "确认支付" : "Confirm" }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, computed, onMounted } from "vue";
import { useStore } from "vuex";
import { ElMessage } from "element-plus";
import api from "../api/index";
import { getAuth, setAuth } from "../utils/authStorage";

export default {
  name: "RechargeView",
  setup() {
    const store = useStore();
    const lang = computed(() => store.getters.lang);
    const packages = ref([]);
    const orderingId = ref("");
    const mockDialogVisible = ref(false);
    const pendingOrderNo = ref("");
    const mockConfirming = ref(false);
    const channelInfo = ref("");
    const summaryCredits = ref(null);

    const uid = () => parseInt(getAuth("userId") || "0", 10);

    const creditsDisplay = computed(() => {
      if (uid() === 0) return lang.value === "zh" ? "—（游客）" : "— (guest)";
      if (summaryCredits.value != null) return summaryCredits.value;
      return getAuth("summaryCredits") ?? "—";
    });

    const channelHint = computed(() => {
      if (channelInfo.value) return channelInfo.value;
      return lang.value === "zh" ? "加载中…" : "Loading…";
    });

    const loadCredits = async () => {
      if (uid() === 0) return;
      const r = await api.getUserSummaryCredits(uid());
      if (r.code === 200 && r.data != null) {
        summaryCredits.value = r.data;
        setAuth("summaryCredits", String(r.data));
      }
    };

    const loadPackages = async () => {
      const r = await api.getPaymentPackages(lang.value === "zh" ? "zh" : "en");
      if (r.code === 200 && r.data) {
        packages.value = r.data;
      }
    };

    const loadChannels = async () => {
      const r = await api.getPaymentChannels();
      if (r.code === 200 && r.data) {
        const d = r.data;
        const zh = lang.value === "zh";
        const parts = [];
        if (d.mock) parts.push(zh ? "模拟支付" : "Mock");
        if (d.stripe) parts.push("Stripe");
        if (d.alipay) parts.push("支付宝");
        if (d.wechatPay) parts.push(zh ? "微信" : "WeChat");
        channelInfo.value = (zh ? "支付方式：" : "Channels: ") + (parts.join(" / ") || "—");
        if (zh && d.noteZh) channelInfo.value += " " + d.noteZh;
        if (!zh && d.noteEn) channelInfo.value += " " + d.noteEn;
      }
    };

    const formatPrice = (p) => {
      const c = (p.currency || "").toUpperCase();
      if (c === "CNY" || c === "cny") return `¥${(p.priceCents / 100).toFixed(2)}`;
      return `${(p.priceCents / 100).toFixed(2)} ${c}`;
    };

    const buy = async (p) => {
      if (uid() === 0) {
        ElMessage.warning(lang.value === "zh" ? "请先登录" : "Please log in");
        return;
      }
      orderingId.value = p.id;
      try {
        const r = await api.createPaymentOrder(uid(), p.id);
        if (r.code !== 200) {
          ElMessage.error(r.message || "error");
          return;
        }
        const d = r.data || {};
        if (d.stripeCheckoutUrl) {
          window.location.href = d.stripeCheckoutUrl;
          return;
        }
        if (d.mockPay && d.orderNo) {
          pendingOrderNo.value = d.orderNo;
          mockDialogVisible.value = true;
        }
      } finally {
        orderingId.value = "";
      }
    };

    const confirmMock = async () => {
      mockConfirming.value = true;
      try {
        const r = await api.confirmMockPayment(uid(), pendingOrderNo.value);
        if (r.code !== 200) {
          ElMessage.error(r.message || "error");
          return;
        }
        ElMessage.success(lang.value === "zh" ? "支付成功，次数已到账" : "Paid");
        mockDialogVisible.value = false;
        await loadCredits();
      } finally {
        mockConfirming.value = false;
      }
    };

    onMounted(async () => {
      await loadChannels();
      await loadPackages();
      await loadCredits();
      const q = new URLSearchParams(window.location.search);
      if (q.get("paid") === "1") {
        ElMessage.success(lang.value === "zh" ? "支付完成，正在刷新余额…" : "Payment complete, refreshing…");
        await loadCredits();
      }
      if (q.get("cancel") === "1") {
        ElMessage.info(lang.value === "zh" ? "已取消支付" : "Cancelled");
      }
    });

    return {
      lang,
      packages,
      orderingId,
      mockDialogVisible,
      pendingOrderNo,
      mockConfirming,
      creditsDisplay,
      channelHint,
      formatPrice,
      buy,
      confirmMock,
    };
  },
};
</script>

<style scoped>
.recharge-page {
  padding: 24px;
  max-width: 1100px;
  margin: 0 auto;
}
.recharge-header h2 {
  margin: 0 0 8px;
  font-size: 22px;
  color: #303133;
}
.sub {
  margin: 0 0 8px;
  color: #606266;
  font-size: 15px;
}
.sub strong {
  color: #409eff;
  font-size: 18px;
}
.hint {
  font-size: 13px;
  color: #909399;
  margin: 0 0 20px;
  line-height: 1.5;
}
.pkg-row {
  margin-top: 8px;
}
.pkg-card {
  margin-bottom: 16px;
  text-align: center;
}
.pkg-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}
.pkg-credits {
  font-size: 14px;
  color: #606266;
  margin-bottom: 8px;
}
.pkg-price {
  font-size: 20px;
  font-weight: 700;
  color: #409eff;
  margin-bottom: 16px;
}
.pkg-btn {
  width: 100%;
}
.mock-tip {
  font-size: 13px;
  color: #909399;
}
</style>
