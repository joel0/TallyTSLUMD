package us.jmay.tally;

import androidx.annotation.NonNull;

import java.util.ArrayList;

public class TalliesModel {
    public static final byte TALLY_LIGHT_OFF = 0;
    public static final byte TALLY_LIGHT_PREVIEW = 1;
    public static final byte TALLY_LIGHT_PROGRAM = 2;

    public static class TallyState {
        private byte mId = 0;
        private byte mLight = 0;
        private String mLabel = "";

        public TallyState(byte id, byte lightState, String label) {
            mId = id;
            mLight = lightState;
            mLabel = label;
        }

        public byte getId() {
            return mId;
        }

        public byte getLight() {
            return mLight;
        }

        public void setLight(byte state) {
            mLight = state;
        }

        public String getLabel() {
            return mLabel;
        }

        public void setLabel(String label) {
            mLabel = label;
        }

        @NonNull
        @Override
        public String toString() {
            return mLabel;
        }
    }


    private TallyState mTallies[] = new TallyState[256];

    public TallyState GetTally(byte id) {
        return GetTally(id & 0xFF);
    }
    public TallyState GetTally(int id) {
        return mTallies[id];
    }

    public TallyState SetTally(int id, byte lightState, String label) {
        TallyState newState = new TallyState((byte) id, lightState, label);
        mTallies[id] = newState;
        return newState;
    }
    public TallyState SetTally(byte id, byte lightState, String label) {
        return SetTally(id & 0xFF, lightState, label);
    }

    public TallyState[] GetAll() {
        ArrayList<TallyState> allTallies = new ArrayList<>();
        for (TallyState tally : mTallies) {
            if (tally != null) {
                allTallies.add(tally);
            }
        }
        return allTallies.toArray(new TallyState[0]);
    }
}
