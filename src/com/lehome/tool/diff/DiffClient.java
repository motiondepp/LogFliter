package com.lehome.tool.diff;

import com.lehome.tool.T;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.net.Socket;

/**
 * Created by xinyu.he on 2016/1/11.
 */
public class DiffClient extends Thread {

    private Socket mClientSocket;
    private BufferedReader mIn;
    private PrintWriter mOut;
    private boolean mShouldClose = false;
    private int mPort = -1;
    private WeakReference<DiffClientListener> mListener;

    public DiffClient(int port, DiffClientListener listener) throws IOException {
        super();
        mPort = port;
        mListener = new WeakReference<>(listener);
        setup();
    }

    private void setup() throws IOException {
        clean();
        mClientSocket = new Socket("localhost", mPort);
    }

    @Override
    public void run() {
        this.loop();
    }

    private void loop() {
        T.d("enter loop with port:" + mPort);
        if (mClientSocket != null && !mShouldClose) {
            try {
                mIn = new BufferedReader(
                        new InputStreamReader(mClientSocket.getInputStream()));
                mOut = new PrintWriter(mClientSocket.getOutputStream(), true);

                if (mListener.get() != null) {
                    mListener.get().onConnected();
                }
                while (!mShouldClose) {
                    String input = mIn.readLine();
                    if (input == null) {
                        break;
                    } else {
                        if (mListener.get() != null) {
                            String rep = mListener.get().onReceiveString(input);
                            if (rep != null) {
                                mOut.write(rep);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (mClientSocket != null)
                        mClientSocket.close();
                    if (mIn != null)
                        mIn.close();
                    if (mOut != null)
                        mOut.close();
                    mClientSocket = null;
                    mIn = null;
                    mOut = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (mListener.get() != null) {
                    mListener.get().onDisconnected();
                }
            }
        }
    }

    public void writeStringToServer(String content) {
        if (mOut != null) {
            if (!content.endsWith("\n"))
                content += "\n";
            mOut.write(content);
            mOut.flush();
        }
    }

    private void clean() {
        if (mClientSocket != null && !mClientSocket.isClosed()) {
            try {
                if (mClientSocket != null)
                    mClientSocket.close();
                if (mIn != null)
                    mIn.close();
                if (mOut != null)
                    mOut.close();
                mClientSocket = null;
                mIn = null;
                mOut = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void cleanup() {
        mShouldClose = true;
        clean();
    }

    public interface DiffClientListener {
        String onReceiveString(String input);

        void onConnected();

        void onDisconnected();
    }
}
