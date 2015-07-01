package com.jgzuke.touchtoconnect;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.rey.material.widget.EditText;
import com.rey.material.widget.FloatingActionButton;


public class MainActivity extends Activity {

    public static final String PREFERENCES = "touch-connect-shared-pref";
    public static final String PREF_NAME = "touch-connect-name";
    public static final String PREF_NUMBER = "touch-connect-number";
    public static final String PREF_EMAIL = "touch-connect-email";
    public static final String PREF_CONTACT = "touch-connect-contact";

    private EditText mNameInput;
    private EditText mNumberInput;
    private EditText mEmailInput;

    private FloatingActionButton mDoneFAB;

    private Resources mRes;
    private SharedPreferences mPref;

    private TextWatcher mTextWatcher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mRes = getResources();
        mPref = getSharedPreferences(PREFERENCES, MODE_PRIVATE);

        mNameInput = (EditText) findViewById(R.id.name_input);
        mNumberInput = (EditText) findViewById(R.id.number_input);
        mEmailInput = (EditText) findViewById(R.id.email_input);
        mDoneFAB = (FloatingActionButton) findViewById(R.id.done_changes);

        //hides keyboard when you click off of an edittext
        findViewById(R.id.full_layout).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent e) {
                hideKeyboard();
                return false;
            }
        });

        mDoneFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveChanges();
            }
        });

        mTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                setDataChanged();
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

    private void restoreLastText() {
        if(mPref.contains(PREF_NAME)) {
            mNameInput.setText(mPref.getString(PREF_NAME, "Name"));
        }
        if(mPref.contains(PREF_NUMBER)) {
            mNumberInput.setText(mPref.getString(PREF_NUMBER, "Number"));
        }
        if(mPref.contains(PREF_EMAIL)) {
            mEmailInput.setText(mPref.getString(PREF_EMAIL, "Email"));
        }
        mDoneFAB.setBackgroundColor(mRes.getColor(R.color.label_color));
    }

    private void saveChanges() {
        String name = mNameInput.getText().toString();
        String number = mNumberInput.getText().toString();
        String email = mEmailInput.getText().toString();
        SharedPreferences.Editor editPref = mPref.edit();
        editPref.putString(PREF_NAME, name);
        editPref.putString(PREF_NUMBER, number);
        editPref.putString(PREF_EMAIL, email);
        mDoneFAB.setBackgroundColor(mRes.getColor(R.color.label_color));
        toast(R.string.info_saved);

        Uri uri = Uri.fromParts("tel", number, "");
        startActivity(new Intent(ContactsContract.Intents.SHOW_OR_CREATE_CONTACT, uri));

        editPref.putString(PREF_CONTACT, email);

        editPref.commit();
    }

    private void setDataChanged() {
        mDoneFAB.setBackgroundColor(mRes.getColor(R.color.error_color));
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
