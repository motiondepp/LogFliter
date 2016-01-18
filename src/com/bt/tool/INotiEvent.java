package com.bt.tool; /**
 *
 */

/**
 *
 */
public interface INotiEvent {
    int EVENT_CLICK_BOOKMARK = 0;
    int EVENT_CLICK_ERROR = 1;
    int EVENT_CHANGE_FILTER_SHOW_TAG = 2;
    int EVENT_CHANGE_FILTER_REMOVE_TAG = 3;
    int EVENT_CHANGE_FILTER_FIND_WORD = 4;
    int EVENT_CHANGE_FILTER_REMOVE_WORD = 5;
    int EVENT_CHANGE_FILTER_FROM_TIME = 6;
    int EVENT_CHANGE_FILTER_TO_TIME = 7;

    void notiEvent(EventParam param);

    class EventParam {
        int nEventId;
        Object param1;
        Object param2;
        Object param3;

        public EventParam(int nEventId) {
            this(nEventId, null, null, null);
        }

        public EventParam(int nEventId, Object param1) {
            this(nEventId, param1, null, null);
        }

        public EventParam(int nEventId, Object param1, Object param2) {
            this(nEventId, param1, param2, null);
        }

        public EventParam(int nEventId, Object param1, Object param2, Object param3) {
            this.nEventId = nEventId;
            this.param1 = param1;
            this.param2 = param2;
            this.param3 = param3;
        }
    }
}
