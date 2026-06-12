export type WebSocketDataType =
  | 'Init'
  | 'AllClass'
  | 'AllField'
  | 'ClassInstance'
  | 'FieldDoc'
  | 'FieldDefaultValue'
  | 'GetFieldValue'
  | 'SetFieldValue'
  | 'AddElement'
  | 'ExportClass'
  | 'NewClass'
  | 'RemoveClass'
  | 'FetchDoc'
  | 'Error';

export interface Data {
  str?: string;
  int?: number;
  float?: number;
  list?: Data[];
  boolean?: boolean;
  obj?: Data;
  json?: string;
}

export interface WebSocketData {
  wsType: WebSocketDataType;
  content?: string;
  out?: boolean;
  dataList?: Record<string, Data>;
}

export interface MindustryMitRequest {
  wsType: WebSocketDataType;
  content?: Record<string, unknown>;
}

export class MindustryMitClient {
  private socket?: WebSocket;

  constructor(private readonly url: string) {}

  get isOpen() {
    return this.socket?.readyState === WebSocket.OPEN;
  }

  connect(): Promise<void> {
    if (this.isOpen) return Promise.resolve();

    return new Promise((resolve, reject) => {
      const socket = new WebSocket(this.url);
      this.socket = socket;

      socket.addEventListener('open', () => resolve(), { once: true });
      socket.addEventListener(
        'error',
        () => reject(new Error(`无法连接到 ${this.url}`)),
        { once: true },
      );
    });
  }

  close() {
    this.socket?.close();
    this.socket = undefined;
  }

  async request({ wsType, content = {} }: MindustryMitRequest): Promise<WebSocketData> {
    await this.connect();

    const socket = this.socket;
    if (!socket || socket.readyState !== WebSocket.OPEN) {
      throw new Error('WebSocket 未连接');
    }

    const payload: WebSocketData = {
      wsType,
      content: JSON.stringify(content),
    };

    return new Promise((resolve, reject) => {
      const cleanup = () => {
        socket.removeEventListener('message', onMessage);
        socket.removeEventListener('error', onError);
        socket.removeEventListener('close', onClose);
      };

      const onMessage = (event: MessageEvent<string>) => {
        cleanup();
        try {
          const response = JSON.parse(event.data) as WebSocketData;
          if (response.wsType === 'Error') {
            const message = response.dataList?.Message?.str || '后端返回错误';
            reject(new Error(message));
            return;
          }
          resolve(response);
        } catch (error) {
          reject(error);
        }
      };

      const onError = () => {
        cleanup();
        reject(new Error('WebSocket 请求失败'));
      };

      const onClose = () => {
        cleanup();
        reject(new Error('WebSocket 连接已关闭'));
      };

      socket.addEventListener('message', onMessage);
      socket.addEventListener('error', onError);
      socket.addEventListener('close', onClose);
      socket.send(JSON.stringify(payload));
    });
  }
}

export const defaultWsUrl = import.meta.env.VITE_WS_URL || 'ws://localhost:19190';
