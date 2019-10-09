package us.jmay.tally;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.ViewModelProviders;

import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    private TextView mStatusText;
    private TextView mTallyStatusTextView;
    private Spinner mCamChoiceSpinner;
    private ConstraintLayout mMainLayout;
    private int mThisTallyId = 0x84;
    private NetController mNetController;
    private TallyListenerClass mListener = new TallyListenerClass();
    private ArrayAdapter<TalliesModel.TallyState> mCamSpinnerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mStatusText = findViewById(R.id.statusTextView);
        mTallyStatusTextView = findViewById(R.id.tallyTextView);
        mCamChoiceSpinner = findViewById(R.id.camChoiceSpinner);
        mMainLayout = findViewById(R.id.mainLayout);

        mNetController = ViewModelProviders.of(this).get(NetController.class);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mCamSpinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        mCamChoiceSpinner.setAdapter(mCamSpinnerAdapter);
        mCamChoiceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                mThisTallyId = ((TalliesModel.TallyState) parent.getItemAtPosition(pos)).getId() & 0xFF;
                updateUINow();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        mNetController.setTallyListener(mListener);
        mNetController.StartAsync();
        updateUINow();
    }

    @Override
    protected void onStop() {
        super.onStop();

        mNetController.setTallyListener(null);
        mNetController.Stop();
    }

    public void updateUINow() {
        TalliesModel.TallyState[] activeTallies = mNetController.getTallies().GetAll();
        TalliesModel.TallyState myTally = mNetController.getTallies().GetTally(mThisTallyId);

        // Tally Spinner
        mCamSpinnerAdapter.clear();
        mCamSpinnerAdapter.addAll(activeTallies);

        // My tally
        if (myTally == null) {
            // The tally has not yet been observed.
            myTally = new TalliesModel.TallyState((byte) 0, TalliesModel.TALLY_LIGHT_OFF, "UNKNOWN");
        }
        mTallyStatusTextView.setText(myTally.getLabel());
        int colorId = R.color.tallyOff;
        switch (myTally.getLight()) {
            case TalliesModel.TALLY_LIGHT_PREVIEW:
                colorId = R.color.tallyPreview;
                break;
            case TalliesModel.TALLY_LIGHT_PROGRAM:
            case TalliesModel.TALLY_LIGHT_PREVIEW + TalliesModel.TALLY_LIGHT_PROGRAM:
                colorId = R.color.tallyLive;
                break;
        }
        mMainLayout.setBackgroundResource(colorId);
    }

    private class TallyListenerClass implements NetController.ITallyEvent {
        @Override
        public void tallyChange(byte id, TalliesModel.TallyState state) {
            int unsignedId = id & 0xFF;
            if (unsignedId == mThisTallyId) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateUINow();
                    }
                });
            }
        }

        @Override
        public void statusChange(final String message) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mStatusText.setText(message);
                }
            });
        }
    }
}
