package com.lehome.tool; /**
 *
 */

/**
 *
 */
public interface INotiEvent {

    enum TYPE {
        EVENT_CLICK_BOOKMARK,
        EVENT_CLICK_ERROR,
        EVENT_CHANGE_FILTER_SHOW_PID,
        EVENT_CHANGE_FILTER_SHOW_TAG,
        EVENT_CHANGE_FILTER_REMOVE_TAG,
        EVENT_CHANGE_FILTER_FIND_WORD,
        EVENT_CHANGE_FILTER_REMOVE_WORD,
        EVENT_CHANGE_FILTER_FROM_TIME,
        EVENT_CHANGE_FILTER_TO_TIME,
        EVENT_CHANGE_SELECTION,
    }


    void notiEvent(EventParam param);

    class EventParam {
        TYPE type;
        Object param1;
        Object param2;
        Object param3;

        public EventParam(TYPE type) {
            this(type, null, null, null);
        }

        public EventParam(TYPE type, Object param1) {
            this(type, param1, null, null);
        }

        public EventParam(TYPE type, Object param1, Object param2) {
            this(type, param1, param2, null);
        }

        public EventParam(TYPE type, Object param1, Object param2, Object param3) {
            this.type = type;
            this.param1 = param1;
            this.param2 = param2;
            this.param3 = param3;
        }
    }
}
