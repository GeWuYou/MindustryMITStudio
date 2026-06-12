import { defineStore } from 'pinia';
import { computed, ref } from 'vue';
import { MindustryMitClient, defaultWsUrl } from '../api/mindustrymit';

export const useConnectionStore = defineStore('connection', () => {
  const wsUrl = defaultWsUrl;
  const client = new MindustryMitClient(wsUrl);
  const connected = ref(false);

  const statusMessage = computed(() => {
    if (connected.value) return '可以调用后端 JSON 编辑接口。';
    return '先启动 Kotlin 后端：bash ./gradlew :server:run';
  });

  async function connect() {
    await client.connect();
    connected.value = true;
  }

  function disconnect() {
    client.close();
    connected.value = false;
  }

  return {
    client,
    connected,
    statusMessage,
    wsUrl,
    connect,
    disconnect,
  };
});
