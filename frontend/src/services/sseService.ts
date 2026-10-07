const BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';

export interface StreamEvent {
  eventType: 'DRIFT_DETECTED' | 'DRIFT_RESOLVED' | 'SCAN_COMPLETED' | 'DEVICE_STATUS_CHANGED' | 'ALERT_CREATED' | 'HEARTBEAT';
  deviceId?: number;
  hostname?: string;
  message?: string;
  data?: any;
  timestamp: string;
}

export function subscribeToRealtimeEvents(onEvent: (event: StreamEvent) => void): () => void {
  const token = localStorage.getItem('nms_token') || '';
  const url = `${BASE_URL}/events/stream?token=${encodeURIComponent(token)}`;
  const eventSource = new EventSource(url);

  eventSource.onmessage = (e) => {
    try {
      const parsed: StreamEvent = JSON.parse(e.data);
      if (parsed.eventType !== 'HEARTBEAT') {
        onEvent(parsed);
      }
    } catch (err) {
      // Ignore heartbeat/ping formatting
    }
  };

  const eventTypes = [
    'DRIFT_DETECTED',
    'DRIFT_RESOLVED',
    'SCAN_COMPLETED',
    'DEVICE_STATUS_CHANGED',
    'ALERT_CREATED'
  ];

  eventTypes.forEach((type) => {
    eventSource.addEventListener(type, (e: any) => {
      try {
        const parsed: StreamEvent = JSON.parse(e.data);
        onEvent(parsed);
      } catch (err) {
        console.error('Failed to parse SSE event data', err);
      }
    });
  });

  eventSource.onerror = (err) => {
    // EventSource will automatically reconnect in background
  };

  return () => {
    eventSource.close();
  };
}
