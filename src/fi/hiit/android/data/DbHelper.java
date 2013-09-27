package fi.hiit.android.data;

import android.util.Log;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


// DbHelper implementations
public class DbHelper extends SQLiteOpenHelper {
    public static final String TAG = "DbHelper";
    public static final int DB_VERSION = 1;

    private DbXmlParser mDbQueries;
    private Context mContext;

    /*[FIXME: better exceptions?]*/
    public DbHelper(Context context, String databasePath, int dbXmlResource) throws Exception {
        super(context, databasePath, null, DB_VERSION);
        mContext = context;
        mDbQueries = new DbXmlParser(mContext, dbXmlResource);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.i(TAG, "Creating database");
        db.execSQL(mDbQueries.getQuery("create_table"));
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        /*[FIXME: this should be ALTER TABLE .. statements rather than DROP]*/
        db.execSQL(mDbQueries.getQuery("drop_table"));
        this.onCreate(db);
    }

    public String getQuery(String name) {
        return mDbQueries.getQuery(name);
    }
}

