package us.jmay.tally;

import androidx.lifecycle.ViewModel;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class NetController extends ViewModel {
    private TalliesModel mTallies = new TalliesModel();
    private NetThread mNetThread = null;
    private ITallyEvent mTallyListener = null;

    public void StartAsync() {
        if (mNetThread != null) throw new IllegalStateException("Network thread is already running.");
        mNetThread = new NetThread();
        mNetThread.start();
    }

    public void Stop() {
        mNetThread.interrupt();
        try {
            mNetThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mNetThread = null;
    }

    public TalliesModel getTallies() {
        return mTallies;
    }

    public void setTallyListener(ITallyEvent mTallyListener) {
        this.mTallyListener = mTallyListener;
    }

    class NetThread extends Thread {
        private ServerSocket mSocket;

        NetThread() {
            this.setName("Net Thread");

            try {
                mSocket = new ServerSocket();
                mSocket.setReuseAddress(true);
                mSocket.bind(new InetSocketAddress(1337), 2);
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }

        @Override
        public void run() {
            while (!isInterrupted()) {
                try {
                    setUIText("Listening");
                    Socket client = mSocket.accept();
                    String remoteAddr = client.getInetAddress().toString();
                    setUIText(remoteAddr + " connected");

                    int ret;
                    byte tallyId;
                    byte tallyStatus;
                    String tallyLabel;
                    BufferedInputStream is = new BufferedInputStream(client.getInputStream());
                    byte[] buffer = new byte[18];
                    do {
                        ret = is.read(buffer);
                        if (ret == 18) {
                            tallyId = buffer[0];

                            tallyStatus = Byte.valueOf(Character.toString((char) buffer[1]));
                            tallyLabel = new String(buffer, 2, 16, StandardCharsets.US_ASCII);
                            TalliesModel.TallyState newState = mTallies.SetTally(tallyId, tallyStatus, tallyLabel);

                            updateUIWithCurStatus(tallyId, newState);
                        }
                    } while (ret != -1 && client.isConnected());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void interrupt() {
            super.interrupt();

            try {
                mSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        void setUIText(String text) {
            if (mTallyListener != null) {
                mTallyListener.statusChange(text);
            }
        }

        void updateUIWithCurStatus(byte tallyId, TalliesModel.TallyState state) {
            if (mTallyListener != null) {
                mTallyListener.tallyChange(tallyId, state);
            }
        }
    }

    public interface ITallyEvent {
        void tallyChange(byte id, TalliesModel.TallyState state);
        void statusChange(String message);
    }
}
