package com.magratheadesign.streamstation;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

/**
 * Created by ejntoo on 07/10/14.
 * Preference activity
 */
public class Preferences extends PreferenceActivity {
    SharedPreferences preferences;
    Resources res;
    Preference dirPreference;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        res = getResources();

        EditTextPreference portPreference = (EditTextPreference) findPreference("pref_port");
        portPreference.setSummary(res.getString(R.string.pref_port_summ) + " " + portPreference.getText());
        portPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                preference.setSummary(res.getString(R.string.pref_port_summ) + " " + ((EditTextPreference) preference).getText());
                return true;
            }
        });


        dirPreference = findPreference("pref_dir");
        dirPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(Preferences.this, DirectoryPicker.class);
                intent.putExtra("currentDir", preferences.getString("pref_dir", "/"));
                startActivityForResult(intent, 0);
                return true;
            }
        });
        dirPreference.setSummary(res.getString(R.string.pref_dir_summ) + " " + preferences.getString("pref_dir", "/"));
        dirPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                preference.setSummary(res.getString(R.string.pref_dir_summ) + " " + preferences.getString("pref_dir", "/"));
                return true;
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_CANCELED) return;
        if (requestCode == 0) {
            String newValue = data.getCharSequenceExtra("resultDir").toString();
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString("pref_dir", newValue);
            editor.commit();
            dirPreference.setSummary(res.getString(R.string.pref_dir_summ) + " " + preferences.getString("pref_dir", "/"));
        }
    }
}
