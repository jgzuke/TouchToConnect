package com.jgzuke.touchtoconnect;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.provider.Settings;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.rey.material.widget.EditText;
import com.soundcloud.android.crop.Crop;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends Activity implements NfcAdapter.CreateNdefMessageCallback, NfcAdapter.OnNdefPushCompleteCallback {

    public static final String PREFERENCES = "touch-connect-shared-pref";
    public static final String PREF_NAME = "touch-connect-name";
    public static final String PREF_NUMBER = "touch-connect-number";
    public static final String PREF_EMAIL = "touch-connect-email";
    public static final String PREF_PHOTO_URI = "touch-connect-photouri";

    public static final int BEAM_COMPLETE = 1;

    public static final String CARD_PRE = "BEGIN:VCARD\nVERSION:2.1\n";
    public static final String CARD_END = "END:VCARD";
    public static final String CARD_DEFAULT_DATA = "N:;New Contact;;;\n";

    public static final String PHOTO_FILE = "touch-connect-photo";

    public static final  int profileImageSize = 128;

    private EditText mNameInput;
    private EditText mNumberInput;
    private EditText mEmailInput;
    private CircleImageView mProfilePhoto;

    private Uri mPhotoUri;

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

        //mProfilePhoto = (CircleImageView) findViewById(R.id.profile_image);
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

        int colorPrimary = mRes.getColor(R.color.label_color);
        restoreLastText();

        /*mProfilePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectPhoto();
            }
        });*/

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

        setImageViewColor(R.id.name_input_icon, colorPrimary);
        setImageViewColor(R.id.number_input_icon, colorPrimary);
        setImageViewColor(R.id.email_input_icon, colorPrimary);

        mNameInput.addTextChangedListener(mTextWatcher);
        mNumberInput.addTextChangedListener(mTextWatcher);
        mEmailInput.addTextChangedListener(mTextWatcher);

        restoreLastText();
    }

    private void selectPhoto() {
        Crop.pickImage(this);
    }

    private void beginCrop(Uri source) {
        Uri destination = Uri.fromFile(new File(getCacheDir(), PHOTO_FILE));
        Crop.of(source, destination).asSquare().start(this);
    }

    private void endCrop(int resultCode, Intent result) {
        if (resultCode == RESULT_OK) {
            mProfilePhoto.setImageURI(null);
            mPhotoUri = Crop.getOutput(result);
            mProfilePhoto.setImageURI(mPhotoUri);
            saveChanges();
        } else if (resultCode == Crop.RESULT_ERROR) {
            Toast.makeText(this, Crop.getError(result).getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent result) {
        if (requestCode == Crop.REQUEST_PICK && resultCode == RESULT_OK) {
            beginCrop(result.getData());
        } else if (requestCode == Crop.REQUEST_CROP) {
            endCrop(resultCode, result);
        }
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
        NdefRecord[] ndefRecord = new NdefRecord[] {record};
        NdefMessage ndefMessage = new NdefMessage(ndefRecord);
        return ndefMessage;
    }

    @Override
    public void onNdefPushComplete(NfcEvent event) {
        mHandler.obtainMessage(BEAM_COMPLETE).sendToTarget();
    }

    private String generateContactCard() {
        String contactCard = CARD_PRE + generateContactCardData() + CARD_END;
        return contactCard;
    }

    private String generateContactCardData() {
        String contactData = appendPrefToString("", "N:;", PREF_NAME, ";;;\n");
        contactData = appendPrefToString(contactData, "TEL;CELL:", PREF_NUMBER, "\n");
        contactData = appendPrefToString(contactData, "EMAIL;HOME:", PREF_EMAIL, "\n");
        contactData += encodeSavedUriTobase64();
        if(contactData.isEmpty()) contactData = CARD_DEFAULT_DATA;
        return contactData;
    }

    private String encodeSavedUriTobase64()
    {
        if(!mPref.contains(PREF_PHOTO_URI)) return "";
        String uriString = mPref.getString(PREF_PHOTO_URI, "");
        if(uriString == null || uriString.isEmpty()) return "";

        Uri imageUri = Uri.parse(uriString);

        try {
            Bitmap image = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
            image = getScaledBitmap(image);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            image.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            byte[] b = baos.toByteArray();
            String base64EncodedImage = Base64.encodeToString(b, Base64.DEFAULT);
            return "PHOTO;ENCODING=BASE64;JPEG:".concat(base64EncodedImage).concat("\n");
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    private Bitmap getScaledBitmap(Bitmap original) {
        int originalSize = original.getWidth();
        if(originalSize <= profileImageSize) return original;
        return Bitmap.createScaledBitmap(original, profileImageSize, profileImageSize, false);
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
        /*if(mPref.contains(PREF_PHOTO_URI)) {
            mPhotoUri = Uri.parse(mPref.getString(PREF_PHOTO_URI, ""));
            mProfilePhoto.setImageURI(mPhotoUri);
        }*/
    }

    private void setTextByPref(EditText textView, String PrefID) {
        if(mPref.contains(PrefID)) textView.setText(mPref.getString(PrefID, ""));
    }

    private void saveChanges() {
        SharedPreferences.Editor editPref = mPref.edit();
        editPref.putString(PREF_NAME, mNameInput.getText().toString());
        editPref.putString(PREF_NUMBER, mNumberInput.getText().toString());
        editPref.putString(PREF_EMAIL, mEmailInput.getText().toString());
        /*if(mPhotoUri != null) {
            editPref.putString(PREF_PHOTO_URI, mPhotoUri.toString());
        }*/
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