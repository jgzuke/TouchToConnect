package com.jgzuke.touchtoconnect;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.rey.material.widget.EditText;
import com.rey.material.widget.FloatingActionButton;

import java.nio.charset.Charset;

public class MainActivity extends Activity implements NfcAdapter.CreateNdefMessageCallback, NfcAdapter.OnNdefPushCompleteCallback {

    public static final String PREFERENCES = "touch-connect-shared-pref";
    public static final String PREF_NAME = "touch-connect-name";
    public static final String PREF_NUMBER = "touch-connect-number";
    public static final String PREF_EMAIL = "touch-connect-email";

    public static final int BEAM_COMPLETE = 1;

    public static final String CARD_PRE = "BEGIN:VCARD\nVERSION:2.1\n";
    public static final String CARD_END = "END:VCARD";
    public static final String CARD_DEFAULT_DATA = "N:;New Contact;;;\n";

    private EditText mNameInput;
    private EditText mNumberInput;
    private EditText mEmailInput;

    private Resources mRes;
    private SharedPreferences mPref;
    private TextWatcher mTextWatcher;
    private NfcAdapter mNfcAdapter;

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            if(message.what == BEAM_COMPLETE) toast(R.string.beam_complete);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mNameInput = (EditText) findViewById(R.id.name_input);
        mNumberInput = (EditText) findViewById(R.id.number_input);
        mEmailInput = (EditText) findViewById(R.id.email_input);
    }

    @Override
    public void onResume() {
        super.onResume();
        mRes = getResources();
        mPref = getSharedPreferences(PREFERENCES, MODE_PRIVATE);
        setNFCAdapter();

        //hides keyboard when you click off of an edittext
        findViewById(R.id.full_layout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyboard();
            }
        });

        restoreLastText();

        mTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                saveChanges();
            }
        };

        setImageViewColor(R.id.name_input_icon, mRes.getColor(R.color.label_color));
        setImageViewColor(R.id.number_input_icon, mRes.getColor(R.color.label_color));
        setImageViewColor(R.id.email_input_icon, mRes.getColor(R.color.label_color));

        mNameInput.addTextChangedListener(mTextWatcher);
        mNumberInput.addTextChangedListener(mTextWatcher);
        mEmailInput.addTextChangedListener(mTextWatcher);

        restoreLastText();
    }

    private void setNFCAdapter() {
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter == null) {
            toast(R.string.beam_unavaliable);
        } else if (!mNfcAdapter.isEnabled()) {
            toast(R.string.beam_disabled);
        } else {
            mNfcAdapter.setNdefPushMessageCallback(this, this);
            mNfcAdapter.setOnNdefPushCompleteCallback(this, this);
        }
    }

    @Override
    public NdefMessage createNdefMessage(NfcEvent event) {
        byte[] uriField = generateContactCard().getBytes(Charset.forName("US-ASCII"));
        byte[] payload = new byte[uriField.length + 1];
        System.arraycopy(uriField, 0, payload, 1, uriField.length);
        NdefRecord record = new NdefRecord(NdefRecord.TNF_MIME_MEDIA, "text/vcard".getBytes(), new byte[0], payload);
        return new NdefMessage(new NdefRecord[] {record});
    }

    @Override
    public void onNdefPushComplete(NfcEvent event) {
        mHandler.obtainMessage(BEAM_COMPLETE).sendToTarget();
    }

    private String generateContactCard() {
        return CARD_PRE + generateContactCardData() + CARD_END;
    }

    private String generateContactCardData() {
        String contactData = appendPrefToString("", "N:;", PREF_NAME, ";;;\n");
        contactData = appendPrefToString(contactData, "TEL;CELL:", PREF_NUMBER, "\n");
        contactData = appendPrefToString(contactData, "EMAIL;HOME:", PREF_EMAIL, "\n");
        if(contactData.isEmpty()) contactData = CARD_DEFAULT_DATA;
        return contactData;
    }

    private String appendPrefToString(String text, String pre, String PREF_ID, String end) {
        if(!mPref.contains(PREF_ID)) return "";
        String data = mPref.getString(PREF_ID, "");
        if(data == null || data.isEmpty()) return "";
        return text + pre + data + end;
    }

    private void restoreLastText() {
        setTextByPref(mNameInput, PREF_NAME);
        setTextByPref(mNumberInput, PREF_NUMBER);
        setTextByPref(mEmailInput, PREF_EMAIL);
    }

    private void setTextByPref(EditText textView, String PrefID) {
        if(mPref.contains(PrefID)) textView.setText(mPref.getString(PrefID, ""));
    }

    private void saveChanges() {
        SharedPreferences.Editor editPref = mPref.edit();
        editPref.putString(PREF_NAME, mNameInput.getText().toString());
        editPref.putString(PREF_NUMBER, mNumberInput.getText().toString());
        editPref.putString(PREF_EMAIL, mEmailInput.getText().toString());
        editPref.commit();
    }

    private void setImageViewColor(int viewID, int color) {
        ((ImageView) findViewById(viewID)).setColorFilter(color);
    }

    private void hideKeyboard() {
        InputMethodManager inputMethodManager = (InputMethodManager)  getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showNFCSettings() {
        startActivity(new Intent(Settings.ACTION_NFCSHARING_SETTINGS));
    }

    private void toast(int stringID) {
        Toast.makeText(this, mRes.getString(stringID), Toast.LENGTH_SHORT).show();
    }
}