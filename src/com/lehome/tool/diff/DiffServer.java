package com.lehome.tool.diff;

import com.lehome.tool.T;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by xinyu.he on 2016/1/11.
 */
public class DiffServer extends Thread {

    private ServerSocket mServerSocket;
    private Socket mClientSocket;
    private BufferedReader mIn;
    private PrintWriter mOut;
    private boolean mShouldClose = false;
    private int mPort = -1;
    private WeakReference<DiffServerListener> mListener;

    public DiffServer(int port, DiffServerListener listener) throws IOException {
        super();
        mPort = port;
        mListener = new WeakReference<>(listener);
        clean();
    }

    @Override
    public void run() {
        this.loop();
    }

    private void loop() {
        try {
            mServerSocket = new ServerSocket(mPort);
        } catch (IOException e) {
            e.printStackTrace();
            if (mServerSocket != null) {
                try {
                    mServerSocket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            return;
        }
        while (!mShouldClose) {
            try {
                mClientSocket = mServerSocket.accept();
                mIn = new BufferedReader(
                        new InputStreamReader(mClientSocket.getInputStream()));
                mOut = new PrintWriter(mClientSocket.getOutputStream(), true);

                T.d("new client connected: " + mClientSocket.getLocalAddress().toString() + ":" + mClientSocket.getLocalPort());
                if (mListener.get() != null) {
                    mListener.get().onClientConnected(mClientSocket);
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
                if (mListener.get() != null) {
                    mListener.get().onClientDisconnected(mClientSocket);
                }

                try {
                    if (mClientSocket != null)
                        mClientSocket.close();
                    if (mIn != null)
                        mIn.close();
                    if (mOut != null)
                        mOut.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        clean();
    }

    public void writeStringToClient(String content) {
        if (mOut != null) {
            if (!content.endsWith("\n"))
                content += "\n";
            mOut.write(content);
            mOut.flush();
        }
    }

    private void clean() {
        if (mServerSocket != null && !mServerSocket.isClosed()) {
            try {
                mServerSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mServerSocket = null;
            }
        }
    }

    public void cleanup() {
        clean();
    }

    public void close() {
        mShouldClose = true;
        cleanup();
    }

    public interface DiffServerListener {
        String onReceiveString(String input);

        void onClientConnected(Socket clientSocket);

        void onClientDisconnected(Socket clientSocket);
    }
}
