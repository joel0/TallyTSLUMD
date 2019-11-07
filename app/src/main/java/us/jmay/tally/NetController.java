package us.jmay.tally;

import android.util.Log;

import androidx.lifecycle.ViewModel;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

public class NetController extends ViewModel {
    private TalliesModel mTallies = new TalliesModel();
    private NetThread mNetThread = null;
    private ITallyEvent mTallyListener = null;
    private static final String TAG = "NetController";

    public void StartAsync() {
        if (mNetThread != null)
            throw new IllegalStateException("Network thread is already running.");

        Log.v(TAG, "Creating and starting NetThread.");
        mNetThread = new NetThread();
        mNetThread.start();
    }

    /**
     * Starts listening on the network asynchronously, but does nothing if this instance is already
     * listening.
     */
    public void StartIfNotStartedAsync() {
        if (mNetThread == null) {
            StartAsync();
        } else {
            Log.v(TAG, "Not starting a new NetThread, because one is already running.");
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();

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
        private Socket mSocketClient = null;
        private static final String TAG = "NetController.NetThread";

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
                    mSocketClient = mSocket.accept();
                    String remoteAddr = mSocketClient.getInetAddress().toString();
                    setUIText(remoteAddr + " connected");

                    int ret;
                    byte tallyId;
                    byte tallyStatus;
                    String tallyLabel;
                    BufferedInputStream is = new BufferedInputStream(mSocketClient.getInputStream());
                    byte[] buffer = new byte[18];
                    do {
                        ret = is.read(buffer);
                        if (ret == 18) {
                            tallyId = (byte) (buffer[0] - 0x80);

                            // Note: tallies 3 & 4, brightness, and reserved bits are ignored.
                            // TODO: check reserved bits and log a warning.
                            tallyStatus = (byte) (buffer[1] & 0x03);
                            tallyLabel = new String(buffer, 2, 16, StandardCharsets.US_ASCII);
                            TalliesModel.TallyState newState = mTallies.SetTally(tallyId, tallyStatus, tallyLabel);

                            updateUIWithCurStatus(tallyId, newState);
                        }
                    } while (ret != -1 && mSocketClient.isConnected());
                } catch (SocketException ignored) {
                    // Thread is probably interrupted via interrupt() function. Time to exit.
                    Log.i(TAG, "SocketException, which is probably a request to close app.");
                } catch (IOException e) {
                    Log.w(TAG, "Unexpected IOException", e);
                }
            }

            // Close connections
            if (mSocketClient != null && mSocketClient.isConnected()) {
                try {
                    mSocketClient.close();
                } catch (IOException ignored) {
                    // Don't care if the connection failed to close.
                }
            }
            if (mSocket != null && !mSocket.isClosed()) {
                try {
                    mSocket.close();
                } catch (IOException ignored) {
                    // Don't care if the socket failed to close.
                }
            }
            Log.i(TAG, "NetThread stopped gracefully.");
        }

        @Override
        public void interrupt() {
            super.interrupt();

            Log.v(TAG, "Interrupting net thread to stop.");
            try {
                mSocket.close();
                if (mSocketClient != null && mSocketClient.isConnected()) {
                    mSocketClient.close();
                }
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
