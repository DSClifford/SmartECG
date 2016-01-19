package edu.utk.biodynamics;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class alertActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_alert);

        Intent intent = getIntent();

        String alertText = intent.getStringExtra("alertText");

        final AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setMessage(alertText);

        alertDialog.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == edu.utk.biodynamics.R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
