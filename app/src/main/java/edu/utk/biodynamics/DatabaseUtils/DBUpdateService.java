package edu.utk.biodynamics.DatabaseUtils;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by DSClifford on 10/1/2015.
 */
public class DBUpdateService extends IntentService {
    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    Context ctx = this;
    public DBUpdateService() {
        super("DBUpdateService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        String recordID = intent.getStringExtra("recordID");
        int maxHR = intent.getIntExtra("maxHR", 0);
        String basepath = ctx.getFilesDir().getAbsolutePath();

        Log.d("DBUpdateService", "Started");
        DBDataSource dataSource;
        dataSource = new DBDataSource(ctx);
        dataSource.open();
        dataSource.create(recordID, maxHR);
        dataSource.close();

    }
}
