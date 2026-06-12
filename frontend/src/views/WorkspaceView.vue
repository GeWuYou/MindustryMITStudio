<template>
  <t-layout class="workspace-shell">
    <t-header class="workspace-header">
      <div class="brand">
        <h1>Mindustry MIT Studio</h1>
        <span>{{ connection.wsUrl }}</span>
      </div>

      <t-space size="small" break-line>
        <t-tag :theme="connection.connected ? 'success' : 'default'" variant="light">
          {{ connection.connected ? '已连接' : '未连接' }}
        </t-tag>
        <t-tag :theme="connection.initialized ? 'success' : 'warning'" variant="light">
          {{ connection.initialized ? '已初始化' : '待初始化' }}
        </t-tag>
        <t-button theme="primary" :loading="loading.connect" @click="run('connect', connection.connect)">
          连接后端
        </t-button>
        <t-button variant="outline" @click="connection.disconnect">断开</t-button>
      </t-space>
    </t-header>

    <t-content class="workspace-content">
      <section class="control-strip">
        <t-input
          v-model="connection.dataDir"
          class="data-dir-input"
          label="数据目录"
          placeholder="mindustry_docs"
        />
        <t-button :loading="loading.init" @click="initialize">初始化</t-button>
        <t-button variant="outline" :loading="loading.fetch" @click="run('fetch', connection.fetchDocs)">
          抓取文档
        </t-button>
        <t-button variant="outline" :loading="loading.classes" @click="loadClasses">
          刷新类型
        </t-button>
        <span class="status-text">{{ connection.statusMessage }}</span>
      </section>

      <t-alert
        v-if="connection.errorMessage"
        class="workspace-alert"
        theme="error"
        :message="connection.errorMessage"
        close
      />
      <t-alert
        v-else-if="connection.lastMessage"
        class="workspace-alert"
        theme="success"
        :message="connection.lastMessage"
        close
      />

      <div class="workspace-grid">
        <aside class="class-pane">
          <div class="pane-header">
            <div>
              <h2>类型</h2>
              <p>{{ filteredClasses.length }} / {{ connection.classList.length }}</p>
            </div>
            <t-button size="small" variant="text" :loading="loading.classes" @click="loadClasses">
              刷新
            </t-button>
          </div>

          <t-input v-model="classKeyword" clearable placeholder="搜索 Block、Item、UnitType" />

          <div class="class-list">
            <t-loading :loading="loading.classes" size="small">
              <button
                v-for="className in filteredClasses"
                :key="className"
                class="class-row"
                :class="{ active: className === connection.selectedClass }"
                type="button"
                @click="selectClass(className)"
              >
                {{ className }}
              </button>
              <t-empty v-if="!filteredClasses.length" description="暂无类型" />
            </t-loading>
          </div>
        </aside>

        <main class="editor-pane">
          <div class="pane-header">
            <div>
              <h2>{{ connection.selectedClass || '选择类型开始编辑' }}</h2>
              <p>
                {{
                  connection.activeClassId == null
                    ? '创建类实例后可编辑字段并导出 JSON'
                    : `Class_Id: ${connection.activeClassId}`
                }}
              </p>
            </div>
            <t-space size="small">
              <t-button
                variant="outline"
                :disabled="connection.activeClassId == null"
                :loading="loading.export"
                @click="run('export', connection.exportActiveClass)"
              >
                导出
              </t-button>
            </t-space>
          </div>

          <t-table
            row-key="name"
            class="field-table"
            size="small"
            hover
            :data="visibleFields"
            :columns="fieldColumns"
            :loading="loading.classSelect"
            :height="tableHeight"
            :pagination="pagination"
            @page-change="onPageChange"
          >
            <template #doc="{ row }">
              <t-tooltip v-if="row.doc" :content="row.doc" placement="top-left">
                <span class="doc-cell">{{ row.doc }}</span>
              </t-tooltip>
              <span v-else class="muted">未加载</span>
            </template>

            <template #value="{ row }">
              <span class="value-cell">{{ row.value || row.defaultValue || 'null' }}</span>
            </template>

            <template #actions="{ row }">
              <t-space size="small">
                <t-button size="small" variant="text" @click="openFieldEditor(row)">
                  编辑
                </t-button>
                <t-button size="small" variant="text" @click="openArrayEditor(row)">
                  添加元素
                </t-button>
              </t-space>
            </template>
          </t-table>
        </main>

        <aside class="preview-pane">
          <t-tabs v-model="previewTab" theme="card" class="preview-tabs">
            <t-tab-panel value="json" label="JSON" :destroy-on-hide="false">
              <div class="json-toolbar">
                <span>导出预览</span>
                <t-button size="small" variant="text" :disabled="!connection.exportedJson" @click="copyJson">
                  复制
                </t-button>
              </div>
              <pre class="json-preview">{{ connection.exportedJson || '选择类型并编辑字段后显示 JSON' }}</pre>
            </t-tab-panel>
            <t-tab-panel value="objects" label="对象" :destroy-on-hide="false">
              <div class="object-list">
                <t-list v-if="connection.objectList.length" size="small" :split="true">
                  <t-list-item v-for="objectName in connection.objectList" :key="objectName">
                    {{ objectName }}
                  </t-list-item>
                </t-list>
                <t-empty v-else description="暂无运行时对象" />
              </div>
            </t-tab-panel>
          </t-tabs>
        </aside>
      </div>
    </t-content>
  </t-layout>

  <t-drawer
    v-model:visible="fieldDrawerVisible"
    :header="selectedField ? `编辑字段：${selectedField.name}` : '编辑字段'"
    size="420px"
    :confirm-btn="{ content: '保存', loading: loading.fieldSave }"
    @confirm="saveField"
  >
    <t-form label-align="top">
      <t-form-item label="字段说明">
        <p class="drawer-doc">{{ selectedField?.doc || '暂无说明' }}</p>
      </t-form-item>
      <t-form-item label="默认值">
        <t-input :value="selectedField?.defaultValue || 'null'" readonly />
      </t-form-item>
      <t-form-item label="当前值">
        <t-textarea v-model="fieldDraft" :autosize="{ minRows: 5, maxRows: 10 }" />
      </t-form-item>
    </t-form>
  </t-drawer>

  <t-drawer
    v-model:visible="arrayDrawerVisible"
    :header="selectedField ? `添加元素：${selectedField.name}` : '添加元素'"
    size="420px"
    :confirm-btn="{ content: '添加', loading: loading.elementAdd }"
    @confirm="addElement"
  >
    <t-form label-align="top">
      <t-form-item label="元素类型">
        <t-input v-model="elementTypeDraft" placeholder="留空则由后端推断" />
      </t-form-item>
      <t-form-item label="初始值">
        <t-textarea
          v-model="elementValueDraft"
          placeholder="可留空；基本类型可填写字面值"
          :autosize="{ minRows: 5, maxRows: 10 }"
        />
      </t-form-item>
    </t-form>
  </t-drawer>
</template>

<script setup lang="ts">
import { MessagePlugin, type PageInfo, type PrimaryTableCol } from 'tdesign-vue-next';
import { computed, reactive, ref, watch } from 'vue';
import { useConnectionStore, type FieldRow } from '../stores/connection';

const connection = useConnectionStore();

const loading = reactive({
  classSelect: false,
  classes: false,
  connect: false,
  elementAdd: false,
  export: false,
  fetch: false,
  fieldSave: false,
  init: false,
});

const classKeyword = ref('');
const previewTab = ref('json');
const fieldDrawerVisible = ref(false);
const arrayDrawerVisible = ref(false);
const selectedField = ref<FieldRow | null>(null);
const fieldDraft = ref('');
const elementTypeDraft = ref('');
const elementValueDraft = ref('');
const currentPage = ref(1);
const pageSize = ref(30);
const tableHeight = 'calc(100vh - 248px)';

const filteredClasses = computed(() => {
  const keyword = classKeyword.value.trim().toLowerCase();
  const list = keyword
    ? connection.classList.filter((item) => item.toLowerCase().includes(keyword))
    : connection.classList;
  return list.slice(0, 400);
});

const visibleFields = computed(() => connection.fields);

const pagination = computed(() => ({
  current: currentPage.value,
  pageSize: pageSize.value,
  total: connection.fields.length,
  showJumper: true,
  pageSizeOptions: [20, 30, 50, 100],
}));

const fieldColumns: PrimaryTableCol<FieldRow>[] = [
  { colKey: 'name', title: '字段', width: 180, ellipsis: true },
  { colKey: 'defaultValue', title: '默认值', width: 140, ellipsis: true },
  { colKey: 'doc', title: '说明', cell: 'doc', ellipsis: true },
  { colKey: 'value', title: '当前值', cell: 'value', width: 180, ellipsis: true },
  { colKey: 'actions', title: '操作', cell: 'actions', width: 156, fixed: 'right' },
];

async function run(key: keyof typeof loading, task: () => Promise<unknown>) {
  loading[key] = true;
  try {
    return await task();
  } catch {
    return undefined;
  } finally {
    loading[key] = false;
  }
}

async function initialize() {
  await run('init', async () => {
    const result = await connection.initialize();
    if (result.success) await connection.loadClasses();
  });
}

async function loadClasses() {
  await run('classes', connection.loadClasses);
}

async function selectClass(className: string) {
  currentPage.value = 1;
  await run('classSelect', () => connection.selectClass(className));
}

function onPageChange(pageInfo: PageInfo) {
  currentPage.value = pageInfo.current;
  pageSize.value = pageInfo.pageSize;
  const start = (pageInfo.current - 1) * pageInfo.pageSize;
  const fieldNames = connection.fields.slice(start, start + pageInfo.pageSize).map((field) => field.name);
  void connection.hydrateFieldRows(fieldNames);
}

function openFieldEditor(row: FieldRow) {
  selectedField.value = row;
  fieldDraft.value = row.value || row.defaultValue || '';
  fieldDrawerVisible.value = true;
}

function openArrayEditor(row: FieldRow) {
  selectedField.value = row;
  elementTypeDraft.value = '';
  elementValueDraft.value = '';
  arrayDrawerVisible.value = true;
}

async function saveField() {
  if (!selectedField.value) return;
  await run('fieldSave', async () => {
    await connection.setFieldValue(selectedField.value!.name, fieldDraft.value);
    fieldDrawerVisible.value = false;
  });
}

async function addElement() {
  if (!selectedField.value) return;
  await run('elementAdd', async () => {
    await connection.addElement(selectedField.value!.name, elementTypeDraft.value, elementValueDraft.value);
    arrayDrawerVisible.value = false;
  });
}

async function copyJson() {
  if (!connection.exportedJson) return;
  await navigator.clipboard.writeText(connection.exportedJson);
  MessagePlugin.success('JSON 已复制');
}

watch(
  () => connection.fields.length,
  () => {
    currentPage.value = 1;
  },
);
</script>

<style scoped>
.workspace-shell {
  height: 100%;
  background: #eef2f7;
  color: #1f2937;
}

.workspace-header {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 0 24px;
  background: #ffffff;
  border-bottom: 1px solid #d8dee9;
}

.brand {
  min-width: 0;
}

.brand h1 {
  margin: 0;
  font-size: 20px;
  line-height: 28px;
}

.brand span {
  display: block;
  color: #64748b;
  font-size: 13px;
  line-height: 20px;
}

.workspace-content {
  height: calc(100vh - 64px);
  overflow: hidden;
  padding: 16px 20px 20px;
}

.control-strip {
  min-height: 48px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  background: #ffffff;
  border: 1px solid #d8dee9;
  border-radius: 8px;
}

.data-dir-input {
  width: 320px;
}

.status-text {
  min-width: 180px;
  color: #64748b;
  font-size: 13px;
}

.workspace-alert {
  margin-top: 10px;
}

.workspace-grid {
  height: calc(100% - 64px);
  display: grid;
  grid-template-columns: 260px minmax(520px, 1fr) 360px;
  gap: 14px;
  margin-top: 14px;
  min-height: 0;
}

.workspace-alert + .workspace-grid {
  height: calc(100% - 118px);
}

.class-pane,
.editor-pane,
.preview-pane {
  min-height: 0;
  background: #ffffff;
  border: 1px solid #d8dee9;
  border-radius: 8px;
}

.class-pane,
.preview-pane {
  display: flex;
  flex-direction: column;
  padding: 14px;
}

.editor-pane {
  padding: 14px;
  overflow: hidden;
}

.pane-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.pane-header h2 {
  margin: 0;
  font-size: 16px;
  line-height: 24px;
}

.pane-header p {
  margin: 2px 0 0;
  color: #64748b;
  font-size: 12px;
  line-height: 18px;
}

.class-list {
  min-height: 0;
  flex: 1;
  margin-top: 12px;
  overflow: auto;
}

.class-row {
  width: 100%;
  height: 34px;
  display: block;
  padding: 0 10px;
  overflow: hidden;
  color: #334155;
  text-align: left;
  text-overflow: ellipsis;
  white-space: nowrap;
  background: transparent;
  border: 0;
  border-radius: 6px;
  cursor: pointer;
}

.class-row:hover {
  background: #f1f5f9;
}

.class-row.active {
  color: #0052d9;
  background: #ecf2fe;
  font-weight: 600;
}

.field-table {
  height: calc(100% - 48px);
}

.doc-cell,
.value-cell {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  vertical-align: middle;
  white-space: nowrap;
}

.muted {
  color: #94a3b8;
}

.preview-tabs {
  min-height: 0;
  flex: 1;
}

.preview-tabs :deep(.t-tabs__content) {
  height: calc(100% - 48px);
}

.preview-tabs :deep(.t-tab-panel) {
  height: 100%;
}

.json-toolbar {
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #64748b;
  font-size: 13px;
}

.json-preview {
  height: calc(100% - 36px);
  margin: 0;
  padding: 12px;
  overflow: auto;
  color: #dbeafe;
  font-size: 12px;
  line-height: 18px;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  background: #111827;
  border-radius: 6px;
}

.object-list {
  height: 100%;
  overflow: auto;
}

.drawer-doc {
  margin: 0;
  color: #475569;
  line-height: 22px;
}

@media (max-width: 1180px) {
  .workspace-header {
    height: auto;
    min-height: 64px;
    align-items: flex-start;
    flex-direction: column;
    padding: 12px 16px;
  }

  .workspace-content {
    height: calc(100vh - 100px);
    padding: 12px;
  }

  .control-strip {
    align-items: stretch;
    flex-wrap: wrap;
  }

  .data-dir-input {
    width: min(100%, 320px);
  }

  .workspace-grid {
    grid-template-columns: 220px minmax(420px, 1fr);
  }

  .preview-pane {
    display: none;
  }
}

@media (max-width: 760px) {
  .workspace-content {
    overflow: auto;
  }

  .workspace-grid {
    height: auto;
    grid-template-columns: 1fr;
  }

  .class-pane,
  .editor-pane {
    min-height: 360px;
  }
}
</style>
