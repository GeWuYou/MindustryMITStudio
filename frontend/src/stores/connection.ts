import { defineStore } from 'pinia';
import { computed, ref } from 'vue';
import { MindustryMitClient, defaultWsUrl } from '../api/mindustrymit';

export interface FieldRow {
  name: string;
  doc: string;
  defaultValue: string;
  value: string;
  loading: boolean;
}

export const useConnectionStore = defineStore('connection', () => {
  const wsUrl = defaultWsUrl;
  const client = new MindustryMitClient(wsUrl);

  const connected = ref(false);
  const initialized = ref(false);
  const dataDir = ref('mindustry_docs');
  const lastMessage = ref('');
  const errorMessage = ref('');

  const classList = ref<string[]>([]);
  const selectedClass = ref('');
  const activeClassId = ref<number | null>(null);
  const fields = ref<FieldRow[]>([]);
  const objectList = ref<string[]>([]);
  const exportedJson = ref('');

  const statusMessage = computed(() => {
    if (!connected.value) return '先启动 Kotlin 后端：bash ./gradlew :server:run';
    if (!initialized.value) return '已连接，初始化文档后可加载类型和字段。';
    if (selectedClass.value) return `正在编辑 ${selectedClass.value}`;
    return '已初始化，可以选择 Mindustry 类型创建 JSON。';
  });

  function fail(error: unknown): never {
    const message = error instanceof Error ? error.message : String(error);
    errorMessage.value = message;
    throw error;
  }

  function markConnected() {
    connected.value = client.isOpen;
  }

  async function connect() {
    errorMessage.value = '';
    try {
      await client.connect();
      connected.value = true;
    } catch (error) {
      connected.value = false;
      fail(error);
    }
  }

  function disconnect() {
    client.close();
    connected.value = false;
  }

  async function initialize() {
    errorMessage.value = '';
    try {
      const response = await client.init(dataDir.value);
      connected.value = true;
      initialized.value = response.success;
      lastMessage.value = response.message;
      return response;
    } catch (error) {
      markConnected();
      fail(error);
    }
  }

  async function fetchDocs() {
    errorMessage.value = '';
    try {
      const response = await client.fetchDoc(dataDir.value);
      connected.value = true;
      lastMessage.value = response.message;
      return response;
    } catch (error) {
      markConnected();
      fail(error);
    }
  }

  async function loadClasses() {
    errorMessage.value = '';
    try {
      classList.value = await client.listClasses();
      connected.value = true;
    } catch (error) {
      markConnected();
      fail(error);
    }
  }

  async function selectClass(className: string) {
    errorMessage.value = '';
    selectedClass.value = className;
    fields.value = [];
    objectList.value = [];
    exportedJson.value = '';

    try {
      const [classId, fieldNames, objects] = await Promise.all([
        client.newClass(className),
        client.listFields(className),
        client.listClassInstances(className),
      ]);
      activeClassId.value = classId;
      objectList.value = objects;
      fields.value = fieldNames.map((name) => ({
        name,
        doc: '',
        defaultValue: '',
        value: '',
        loading: false,
      }));
      connected.value = true;
      await hydrateFieldRows(fieldNames.slice(0, 12));
      await exportActiveClass();
    } catch (error) {
      markConnected();
      fail(error);
    }
  }

  async function hydrateFieldRows(fieldNames: string[]) {
    if (!selectedClass.value || fieldNames.length === 0) return;

    await Promise.all(
      fieldNames.map(async (name) => {
        const row = fields.value.find((item) => item.name === name);
        if (!row || row.loading) return;

        row.loading = true;
        try {
          const [doc, defaultValue] = await Promise.all([
            client.getFieldDoc(selectedClass.value, name),
            client.getFieldDefaultValue(selectedClass.value, name),
          ]);
          row.doc = doc;
          row.defaultValue = defaultValue;
        } finally {
          row.loading = false;
        }
      }),
    );
  }

  async function setFieldValue(fieldName: string, value: string) {
    if (activeClassId.value == null) return;
    errorMessage.value = '';

    try {
      const result = await client.setFieldValue(activeClassId.value, [fieldName], value);
      const row = fields.value.find((item) => item.name === fieldName);
      if (row) row.value = value;
      lastMessage.value = result.message || '字段已更新';
      await exportActiveClass();
    } catch (error) {
      markConnected();
      fail(error);
    }
  }

  async function addElement(fieldName: string, elementType = '', value = '') {
    if (activeClassId.value == null) return;
    errorMessage.value = '';

    try {
      const result = await client.addElement(activeClassId.value, [fieldName], elementType, value);
      lastMessage.value = result.message || `已添加第 ${result.index} 个元素`;
      await exportActiveClass();
      return result;
    } catch (error) {
      markConnected();
      fail(error);
    }
  }

  async function exportActiveClass() {
    if (activeClassId.value == null) return '';
    errorMessage.value = '';

    try {
      const result = await client.exportClass(activeClassId.value);
      exportedJson.value = result.content;
      lastMessage.value = result.message || lastMessage.value;
      return result.content;
    } catch (error) {
      markConnected();
      fail(error);
    }
  }

  return {
    activeClassId,
    classList,
    client,
    connected,
    dataDir,
    disconnect,
    errorMessage,
    exportedJson,
    fetchDocs,
    fields,
    hydrateFieldRows,
    initialize,
    initialized,
    lastMessage,
    loadClasses,
    objectList,
    selectClass,
    selectedClass,
    statusMessage,
    wsUrl,
    addElement,
    connect,
    exportActiveClass,
    setFieldValue,
  };
});
