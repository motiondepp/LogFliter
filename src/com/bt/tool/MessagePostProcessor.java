package com.bt.tool;

/**
 * Created by xinyu.he on 2016/1/6.
 */
public abstract class MessagePostProcessor implements IPostProcessor<String> {

    private boolean enable = false;
    private IPostProcessor<String> mNextProcessor;

    public MessagePostProcessor() {
    }

    public MessagePostProcessor(IPostProcessor<String> next) {
        mNextProcessor = next;
    }

    public MessagePostProcessor(boolean enable) {
        this.enable = enable;
    }

    @Override
    public String postProcess(LogInfo info) {
        if (enable && shouldProcess(info)) {
            String result = process(info.m_strMessage);
            if (result != null && mNextProcessor != null) {
                result = mNextProcessor.postProcess(info);
            }
            return result;
        } else {
            return info.m_strMessage;
        }
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public void setmNextProcessor(IPostProcessor<String> mNextProcessor) {
        this.mNextProcessor = mNextProcessor;
    }

    abstract protected String process(String src);

    abstract public boolean shouldProcess(LogInfo info);
}
