package edu.utk.biodynamics;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import edu.utk.biodynamics.DatabaseUtils.DBOpenHelper;

public class ViewRecActivity extends Activity {

    ArrayList<Record> arrayOfRecords = new ArrayList<Record>();

    SQLiteOpenHelper dbhelper;
    SQLiteDatabase database;
    Cursor cursor;
    ListView recListView;
    RecordCursorAdapter cursorAdapter = null;
    int count = 0;
    Context ctx;

    private static final String[] allColumns = {
            DBOpenHelper.COLUMN_ID,
            DBOpenHelper.COLUMN_RECID,
            DBOpenHelper.COLUMN_DATE,
            DBOpenHelper.COLUMN_TIME,
            DBOpenHelper.COLUMN_MAXHR,
            DBOpenHelper.COLUMN_FLAGGED
    };

    class Record {
        public String upldDate;
        public String recID;
        public String diagnosis;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_recs);
        ctx = this;
        PopulateTable populateTable = new PopulateTable();
        populateTable.execute();

    }

    private class PopulateTable extends AsyncTask<Void, String, Void> {

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected Void doInBackground(Void... params) {

            dbhelper = new DBOpenHelper(ctx);
            database = dbhelper.getWritableDatabase();

            cursor = database.query(DBOpenHelper.TABLE_RECORDS, allColumns,
                    null, null, null, null, null, null);




            return null;
        }

        @Override
        protected void onPostExecute(final Void success) {

            cursorAdapter = new RecordCursorAdapter(ctx,cursor,0);

            if(cursor.getCount()==0){
                TextView noRec_tv = (TextView) findViewById(R.id.no_records_label);
                noRec_tv.setText(R.string.no_rec_text);
            }

            recListView = (ListView) findViewById(R.id.listView);

            recListView.setAdapter(cursorAdapter);
            recListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, View view,
                                        int position, long id) {

                    TextView recTV = (TextView)view.findViewById(R.id.recID);
                    TextView dateTV = (TextView)view.findViewById(R.id.upldDate);
                    TextView timeTV = (TextView)view.findViewById(R.id.upldTime);
                    String recordID = recTV.getText().toString();
                    String upldDate = dateTV.getText().toString()+" "+timeTV.getText().toString();

                    onItemSelected(recordID, upldDate);
                    Log.d("recSelected", "AVID: " + Long.toString(parent.getSelectedItemId()));
                    Log.d("recSelected", "View: " + view.toString());
                    Log.d("recSelected", "UpldDate: " + upldDate);
                    Log.d("recSelected", "FID: " + recordID);

                }
            });

            recListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                    return false;
                }
            });

            dbhelper.close();
        }
    }

    public class RecordCursorAdapter extends CursorAdapter {
        public RecordCursorAdapter(Context context, Cursor cursor, int flags) {
            super(context, cursor, 0);
        }

        // The newView method is used to inflate a new view and return it,
        // you don't bind any data to the view at this point.
        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return LayoutInflater.from(context).inflate(R.layout.row, parent, false);
        }

        // The bindView method is used to bind all data to a given view
        // such as setting the text on a TextView.
        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            // Find fields to populate in inflated template
            TextView recTV = (TextView)view.findViewById(R.id.recID);
            TextView dateTV = (TextView)view.findViewById(R.id.upldDate);
            TextView timeTV = (TextView)view.findViewById(R.id.upldTime);
            TextView maxHRTV = (TextView)view.findViewById(R.id.maxHR);
            // Extract properties from cursor
            Boolean flagged = Boolean.parseBoolean(cursor.getString(cursor.getColumnIndexOrThrow(DBOpenHelper.COLUMN_FLAGGED)));
            String recordID = cursor.getString(cursor.getColumnIndexOrThrow(DBOpenHelper.COLUMN_RECID));
            String date = cursor.getString(cursor.getColumnIndexOrThrow(DBOpenHelper.COLUMN_DATE));
            String time = cursor.getString(cursor.getColumnIndexOrThrow(DBOpenHelper.COLUMN_TIME));
            int maxHR = cursor.getInt(cursor.getColumnIndexOrThrow(DBOpenHelper.COLUMN_MAXHR));
            recTV.setText(recordID);
            dateTV.setText(date);
            timeTV.setText(time);

            maxHRTV.setText(String.valueOf(maxHR));

        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(Uri uri);
    }

    public void onItemSelected(String recordID, String upldDate) {
        GetRecordData dataGetter = new GetRecordData();
        dataGetter.execute(recordID,upldDate);

    }

    private class GetRecordData extends AsyncTask<String, String, String[]> {

        @Override
        protected String[] doInBackground(String... params) {

           return params;
        }

        @Override
        protected void onPostExecute(final String[] result) {

            if (result!=null) {
                Intent intent = new Intent(ctx, ViewRecChartActivity.class)
                        .putExtra("recordID", result[0])
                        .putExtra("upldDate",result[1]);
                startActivity(intent);
            }else {

            }

        }
    }

    @Override
    public void onResume(){
        super.onResume();
        Log.d("ViewECG", "OnResume");


        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        String email = prefs.getString("email", "asd");

        PopulateTable populateTable = new PopulateTable();
        populateTable.execute();
    }
    @Override
    public void onBackPressed(){
        Intent intent = new Intent(ViewRecActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

}
