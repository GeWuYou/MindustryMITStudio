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
  requestId?: string;
  content?: string;
  out?: boolean;
  dataList?: Record<string, Data>;
}

export interface MindustryMitRequest {
  wsType: WebSocketDataType;
  content?: Record<string, unknown>;
}

export interface MindustryMitOperationResult {
  success: boolean;
  message: string;
}

export interface MindustryMitDocResult extends MindustryMitOperationResult {
  docCount: number;
}

export interface MindustryMitFieldValueResult extends MindustryMitOperationResult {
  value: string;
}

export interface MindustryMitAddElementResult extends MindustryMitOperationResult {
  index: number;
}

export interface MindustryMitExportClassResult extends MindustryMitOperationResult {
  content: string;
}

export type MindustryMitFieldPath = string[];

function readData(response: WebSocketData, key: string): Data | undefined {
  return response.dataList?.[key];
}

function readDataString(data: Data | undefined): string | undefined {
  return typeof data?.str === 'string' ? data.str : undefined;
}

export function readString(response: WebSocketData, key: string, fallback = ''): string {
  return readDataString(readData(response, key)) ?? fallback;
}

export function readInt(response: WebSocketData, key: string, fallback = 0): number {
  const value = readData(response, key)?.int;
  return typeof value === 'number' ? value : fallback;
}

export function readBool(response: WebSocketData, key: string, fallback = false): boolean {
  const value = readData(response, key)?.boolean;
  return typeof value === 'boolean' ? value : fallback;
}

export function readStringList(response: WebSocketData, key: string): string[] {
  const value = readData(response, key)?.list;
  if (!Array.isArray(value)) return [];

  return value
    .map((item) => readDataString(item))
    .filter((item): item is string => item !== undefined);
}

function readOperationResult(response: WebSocketData): MindustryMitOperationResult {
  return {
    success: readBool(response, 'Success'),
    message: readString(response, 'Message'),
  };
}

interface PendingRequest {
  reject: (reason?: unknown) => void;
  resolve: (value: WebSocketData) => void;
}

export class MindustryMitClient {
  private socket?: WebSocket;
  private connectPromise?: Promise<void>;
  private readonly pendingRequests = new Map<string, PendingRequest>();
  private requestSequence = 0;

  constructor(private readonly url: string) {}

  get isOpen() {
    return this.socket?.readyState === WebSocket.OPEN;
  }

  connect(): Promise<void> {
    if (this.isOpen) return Promise.resolve();
    if (this.connectPromise) return this.connectPromise;

    this.connectPromise = new Promise((resolve, reject) => {
      const socket = new WebSocket(this.url);
      this.socket = socket;

      const cleanup = () => {
        socket.removeEventListener('open', onOpen);
        socket.removeEventListener('error', onConnectError);
      };

      const onOpen = () => {
        cleanup();
        this.connectPromise = undefined;
        resolve();
      };

      const onConnectError = () => {
        cleanup();
        this.connectPromise = undefined;
        this.socket = undefined;
        reject(new Error(`无法连接到 ${this.url}`));
      };

      socket.addEventListener('open', onOpen, { once: true });
      socket.addEventListener(
        'error',
        onConnectError,
        { once: true },
      );
      socket.addEventListener('message', (event: MessageEvent<string>) => this.handleMessage(event));
      socket.addEventListener('error', () => this.rejectPending(new Error('WebSocket 请求失败')));
      socket.addEventListener('close', () => {
        this.connectPromise = undefined;
        this.socket = undefined;
        this.rejectPending(new Error('WebSocket 连接已关闭'));
      });
    });

    return this.connectPromise;
  }

  close() {
    this.socket?.close();
    this.socket = undefined;
    this.connectPromise = undefined;
    this.rejectPending(new Error('WebSocket 连接已关闭'));
  }

  async request({ wsType, content = {} }: MindustryMitRequest): Promise<WebSocketData> {
    await this.connect();

    const socket = this.socket;
    if (!socket || socket.readyState !== WebSocket.OPEN) {
      throw new Error('WebSocket 未连接');
    }

    const payload: WebSocketData = {
      wsType,
      requestId: this.nextRequestId(),
      content: JSON.stringify(content),
    };

    return new Promise((resolve, reject) => {
      this.pendingRequests.set(payload.requestId!, { reject, resolve });
      try {
        socket.send(JSON.stringify(payload));
      } catch (error) {
        this.pendingRequests.delete(payload.requestId!);
        reject(error);
      }
    });
  }

  async init(dataDir: string): Promise<MindustryMitDocResult> {
    const response = await this.typedRequest('Init', { Data_Dir: dataDir });
    return this.readDocResult(response);
  }

  async fetchDoc(dataDir: string): Promise<MindustryMitDocResult> {
    const response = await this.typedRequest('FetchDoc', { Data_Dir: dataDir });
    return this.readDocResult(response);
  }

  async allClass(): Promise<string[]> {
    const response = await this.typedRequest('AllClass');
    return readStringList(response, 'Class_List');
  }

  async listClasses(): Promise<string[]> {
    return this.allClass();
  }

  async allField(className: string): Promise<string[]> {
    const response = await this.typedRequest('AllField', { Class_Name: className });
    return readStringList(response, 'Field_List');
  }

  async listFields(className: string): Promise<string[]> {
    return this.allField(className);
  }

  async classInstance(className: string): Promise<string[]> {
    const response = await this.typedRequest('ClassInstance', { Class_Name: className });
    return readStringList(response, 'Object_List');
  }

  async listClassInstances(className: string): Promise<string[]> {
    return this.classInstance(className);
  }

  async fieldDoc(className: string, fieldName: string): Promise<string> {
    const response = await this.typedRequest('FieldDoc', {
      Class_Name: className,
      Field_Name: fieldName,
    });
    return readString(response, 'Field_Doc');
  }

  async getFieldDoc(className: string, fieldName: string): Promise<string> {
    return this.fieldDoc(className, fieldName);
  }

  async fieldDefaultValue(className: string, fieldName: string): Promise<string> {
    const response = await this.typedRequest('FieldDefaultValue', {
      Class_Name: className,
      Field_Name: fieldName,
    });
    return readString(response, 'Default_Value');
  }

  async getFieldDefaultValue(className: string, fieldName: string): Promise<string> {
    return this.fieldDefaultValue(className, fieldName);
  }

  async newClass(className: string): Promise<number> {
    const response = await this.typedRequest('NewClass', { Class_Name: className });
    return readInt(response, 'Class_Id');
  }

  async removeClass(classId: number): Promise<boolean> {
    const response = await this.typedRequest('RemoveClass', { Class_Id: classId });
    return readBool(response, 'Success');
  }

  async getFieldValue(
    classId: number,
    fieldPath: MindustryMitFieldPath,
  ): Promise<MindustryMitFieldValueResult> {
    const response = await this.typedRequest('GetFieldValue', {
      Class_Id: classId,
      Field_Path: fieldPath,
    });
    return {
      ...readOperationResult(response),
      value: readString(response, 'Value'),
    };
  }

  async setFieldValue(
    classId: number,
    fieldPath: MindustryMitFieldPath,
    value: string,
  ): Promise<MindustryMitFieldValueResult> {
    const response = await this.typedRequest('SetFieldValue', {
      Class_Id: classId,
      Field_Path: fieldPath,
      Value: value,
    });
    return {
      ...readOperationResult(response),
      value: readString(response, 'Value'),
    };
  }

  async addElement(
    classId: number,
    fieldPath: MindustryMitFieldPath,
    elementType: string,
    value: string,
  ): Promise<MindustryMitAddElementResult> {
    const response = await this.typedRequest('AddElement', {
      Class_Id: classId,
      Field_Path: fieldPath,
      Element_Type: elementType,
      Value: value,
    });
    return {
      ...readOperationResult(response),
      index: readInt(response, 'Index', -1),
    };
  }

  async exportClass(classId: number): Promise<MindustryMitExportClassResult> {
    const response = await this.typedRequest('ExportClass', { Class_Id: classId });
    return {
      ...readOperationResult(response),
      content: readString(response, 'Content'),
    };
  }

  private async typedRequest(
    wsType: WebSocketDataType,
    content: Record<string, unknown> = {},
  ): Promise<WebSocketData> {
    const response = await this.request({ wsType, content });
    if (response.wsType !== wsType) {
      throw new Error(`WebSocket 响应类型不匹配：期望 ${wsType}，实际 ${response.wsType}`);
    }
    return response;
  }

  private readDocResult(response: WebSocketData): MindustryMitDocResult {
    return {
      ...readOperationResult(response),
      docCount: readInt(response, 'Doc_Count'),
    };
  }

  private handleMessage(event: MessageEvent<string>) {
    let response: WebSocketData;
    try {
      response = JSON.parse(event.data) as WebSocketData;
    } catch (error) {
      this.rejectPending(error);
      return;
    }

    const requestId = response.requestId;
    const pending = requestId ? this.pendingRequests.get(requestId) : undefined;
    if (!pending) {
      if (requestId) return;

      if (!requestId && this.pendingRequests.size === 1) {
        const [legacyPending] = this.pendingRequests.values();
        this.pendingRequests.clear();
        if (response.wsType === 'Error') {
          legacyPending.reject(new Error(response.dataList?.Message?.str || '后端返回错误'));
          return;
        }
        legacyPending.resolve(response);
        return;
      }

      if (response.wsType === 'Error') {
        this.rejectPending(new Error(response.dataList?.Message?.str || '后端返回错误'));
        return;
      }
      this.rejectPending(new Error('WebSocket 响应缺少 requestId，无法匹配并发请求'));
      return;
    }

    this.pendingRequests.delete(requestId!);
    if (response.wsType === 'Error') {
      pending.reject(new Error(response.dataList?.Message?.str || '后端返回错误'));
      return;
    }

    pending.resolve(response);
  }

  private nextRequestId() {
    this.requestSequence += 1;
    return `${Date.now().toString(36)}-${this.requestSequence.toString(36)}`;
  }

  private rejectPending(error: unknown) {
    const pendingRequests = [...this.pendingRequests.values()];
    this.pendingRequests.clear();
    pendingRequests.forEach((pending) => pending.reject(error));
  }
}

export const defaultWsUrl = import.meta.env.VITE_WS_URL || 'ws://localhost:19190';
