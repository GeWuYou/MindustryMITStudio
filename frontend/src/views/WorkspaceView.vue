<template>
  <main class="workspace">
    <section class="toolbar">
      <div>
        <h1>Mindustry MIT Studio</h1>
        <p>WebSocket 后端：{{ connection.wsUrl }}</p>
      </div>

      <t-space>
        <t-button theme="primary" :loading="connecting" @click="connect">
          连接后端
        </t-button>
        <t-button variant="outline" @click="disconnect">断开</t-button>
      </t-space>
    </section>

    <section class="status-grid">
      <t-card title="连接状态" :bordered="false">
        <t-tag :theme="connection.connected ? 'success' : 'default'" variant="light">
          {{ connection.connected ? '已连接' : '未连接' }}
        </t-tag>
        <p class="status-message">{{ connection.statusMessage }}</p>
      </t-card>

      <t-card title="初始化数据" :bordered="false">
        <t-space direction="vertical" class="full-width">
          <t-input v-model="dataDir" placeholder="数据目录" />
          <t-button :loading="initializing" @click="initData">初始化</t-button>
        </t-space>
      </t-card>
    </section>

    <section class="panel">
      <div class="panel-header">
        <div>
          <h2>Mindustry 类型</h2>
          <p>从后端读取可编辑的运行时类型列表。</p>
        </div>
        <t-button :loading="loadingClasses" @click="loadClasses">刷新</t-button>
      </div>

      <t-alert v-if="errorMessage" theme="error" :message="errorMessage" />

      <t-list v-if="classList.length" :split="true">
        <t-list-item v-for="className in classList" :key="className">
          {{ className }}
        </t-list-item>
      </t-list>

      <t-empty v-else description="暂无类型数据" />
    </section>
  </main>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useConnectionStore } from '../stores/connection';

const connection = useConnectionStore();

const connecting = ref(false);
const initializing = ref(false);
const loadingClasses = ref(false);
const errorMessage = ref('');
const dataDir = ref('mindustry_docs');
const classList = ref<string[]>([]);

async function runTask(task: () => Promise<void>) {
  errorMessage.value = '';
  try {
    await task();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : String(error);
  }
}

async function connect() {
  connecting.value = true;
  await runTask(connection.connect);
  connecting.value = false;
}

function disconnect() {
  connection.disconnect();
}

async function initData() {
  initializing.value = true;
  await runTask(async () => {
    const response = await connection.client.request({
      wsType: 'Init',
      content: { Data_Dir: dataDir.value },
    });
    connection.connected = true;
    const count = response.dataList?.Doc_Count?.int ?? 0;
    const message = response.dataList?.Message?.str || `已加载 ${count} 条文档`;
    errorMessage.value = message;
  });
  initializing.value = false;
}

async function loadClasses() {
  loadingClasses.value = true;
  await runTask(async () => {
    const response = await connection.client.request({ wsType: 'AllClass' });
    connection.connected = true;
    classList.value = response.dataList?.Class_List?.list?.map((item) => item.str || '') ?? [];
  });
  loadingClasses.value = false;
}
</script>

<style scoped>
.workspace {
  min-height: 100vh;
  padding: 32px;
  background: #f3f6fb;
  color: #1f2937;
}

.toolbar,
.panel,
.status-grid :deep(.t-card) {
  background: #ffffff;
  border: 1px solid #d8dee9;
  border-radius: 8px;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 24px;
}

h1,
h2,
p {
  margin: 0;
}

h1 {
  font-size: 28px;
  line-height: 36px;
}

h2 {
  font-size: 20px;
  line-height: 28px;
}

p {
  margin-top: 8px;
  color: #5f6b7a;
}

.status-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-top: 16px;
}

.status-message {
  min-height: 24px;
}

.panel {
  margin-top: 16px;
  padding: 24px;
}

.panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 16px;
}

.full-width {
  width: 100%;
}

@media (max-width: 720px) {
  .workspace {
    padding: 16px;
  }

  .toolbar,
  .panel-header {
    align-items: stretch;
    flex-direction: column;
  }

  .status-grid {
    grid-template-columns: 1fr;
  }
}
</style>
