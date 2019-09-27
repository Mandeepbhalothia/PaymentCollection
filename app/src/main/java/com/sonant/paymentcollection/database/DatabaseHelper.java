package com.sonant.paymentcollection.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "Project.db";
    private static final String TABLE_NAME = "project_table";
    private static final String TABLE_NAME3 = "gifdata";
    private static final String TABLE_NAME2 = "conversation_table";
    private static final int DATABASE_VERSION = 1;
    private static final String COL_1 = "ID";
    private static final String COL_31 = "ID";
    private static final String COL_2 = "Digit";
    private static final String COL_3 = "Alpha";
    private static final String COL_4 = "Word";
    private static final String COL_5 = "Sentence";
    private static final String COL_11 = "ID";
    private static final String COL_12 = "VT";
    private static final String COL_32 = "Data";
    private static final String COL_13 = "Message";
    private static final String COL_14 = "PartialMessage";
    int count;
    String dataGif;
    String left = "left";
    Context mcontext;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mcontext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
       sqLiteDatabase.execSQL("CREATE TABLE " + TABLE_NAME +"(" + COL_1 + " INTEGER PRIMARY KEY AUTOINCREMENT," + COL_2 + " INTEGER,"  + COL_3 + " TEXT," +  COL_4 + " TEXT,"+ COL_5 +" TEXT)");
       sqLiteDatabase.execSQL("CREATE TABLE " + TABLE_NAME3 +"(" + COL_32 + " TEXT)" );
       sqLiteDatabase.execSQL("CREATE TABLE " + TABLE_NAME2 +"(" + COL_11 + " INTEGER PRIMARY KEY AUTOINCREMENT," + COL_12 + " VARCHAR(30),"+ COL_13+" VARCHAR(50),"+ COL_14+")");

    }
    public boolean insertData(String digit, String alpha, String word, String sentence ){
        // Open database connection
        SQLiteDatabase db = this.getWritableDatabase();
        // Define values for each field
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_2, digit);
        contentValues.put(COL_3, alpha);
        contentValues.put(COL_4, word);
        contentValues.put(COL_5, sentence);
        long result = db.insert(TABLE_NAME,null,contentValues);
        if (result==-1)
            return false;
        else
            return true;
        //db.close();

    }
    public boolean insertData3( String data ){
        // Open database connection
        SQLiteDatabase db = this.getWritableDatabase();
        // Define values for each field
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_32, data);
        long result = db.insert(TABLE_NAME3,null,contentValues);
        if (result==-1) {
            return false;
        }
        else {
            return true;
        }
        //db.close();

    }

    public boolean insertData2( String vt){
        // Open database connection
        SQLiteDatabase db = this.getWritableDatabase();
        // Define values for each field
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_12, vt);
//        contentValues.put(COL_13, msg);

        long result = db.insert(TABLE_NAME2,null,contentValues);
        if (result==-1)
            return false;
        else
            Log.d("Database","database table created");
            return true;
        //db.close();

    }
    public boolean insertData22( String msg){
        // Open database connection
        SQLiteDatabase db = this.getWritableDatabase();
        // Define values for each field
        ContentValues contentValues = new ContentValues();
       // contentValues.put(COL_12, vt);
        contentValues.put(COL_13, msg);
        long result = db.insert(TABLE_NAME2,null,contentValues);
        if (result==-1)
            return false;
        else
            Log.d("Database","database table created");
            return true;
        //db.close();

    }
    public boolean insertData23( String pmsg){
        // Open database connection
        SQLiteDatabase db = this.getWritableDatabase();
        // Define values for each field
        ContentValues contentValues = new ContentValues();
       // contentValues.put(COL_12, vt);
        contentValues.put(COL_14, pmsg);
        long result = db.insert(TABLE_NAME2,null,contentValues);
        if (result==-1)
            return false;
        else
            Log.d("Database","database table created");
            return true;
        //db.close();

    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME2);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME3);
        onCreate(sqLiteDatabase);

    }
    public int getcountVTL(String TABLE_NAME2 ){
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*)FROM "+TABLE_NAME2+" WHERE VT = left",   null);
        cursor.moveToFirst();
        count = cursor.getInt(0);
        cursor.close();
        return count;

    }
    public int getcountVTR(String TABLE_NAME2 ){
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*)FROM "+TABLE_NAME2+" WHERE VT = right",   null);
        cursor.moveToFirst();
        count = cursor.getInt(0);
        cursor.close();
        return count;

    }

    public int getcount(String TABLE_NAME){
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*)FROM "+TABLE_NAME+"",   null);
        cursor.moveToFirst();
        count = cursor.getInt(0);
        cursor.close();
        return count;

    }
    public int getcount3(String TABLE_NAME3){
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*)FROM "+TABLE_NAME3+"",   null);
        cursor.moveToFirst();
        count = cursor.getInt(0);
        cursor.close();
        return count;

    }
    public Cursor getAllMessage(){
        SQLiteDatabase db = this.getWritableDatabase();
        return db.rawQuery("select * from " + TABLE_NAME2, null);
    }
    public Cursor getAllData3(){
        SQLiteDatabase db = this.getWritableDatabase();
        return db.rawQuery("select * from " + TABLE_NAME3, null);
    }


    public Cursor getAllData(){
        SQLiteDatabase db = this.getWritableDatabase();
        return db.rawQuery("select * from " + TABLE_NAME, null);
    }

    public Cursor getDigit(int id){
        SQLiteDatabase db = this.getWritableDatabase();
        return db.rawQuery(" SELECT DIGIT FROM project_table WHERE ID ="+id, null);
    }
    public Cursor getDigit3(int id){
        SQLiteDatabase db = this.getWritableDatabase();
        return db.rawQuery(" SELECT DIGIT FROM project_table2 WHERE ID ="+id, null);
    }
    public Cursor getAlpha(int id){
        SQLiteDatabase db = this.getWritableDatabase();
        return db.rawQuery(" SELECT ALPHA FROM project_table WHERE ID ="+ id, null);
    }
    public Cursor getAlpha3(String word){
        SQLiteDatabase db = this.getWritableDatabase();

        return db.rawQuery(" SELECT * FROM gifdata WHERE TRIM(Data) = '"+word.trim()+"'" , null);
    }
    public Cursor getWord(int id){
        SQLiteDatabase db = this.getWritableDatabase();
        return db.rawQuery(" SELECT WORD FROM project_table WHERE ID =" + id, null);
    }
    public Cursor getWord3(int id){
        SQLiteDatabase db = this.getWritableDatabase();
        return db.rawQuery(" SELECT WORD FROM project_table2 WHERE ID =" + id, null);
    }
    public Cursor getSentence(int id){
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(" SELECT SENTENCE FROM project_table WHERE ID =" + id, null);
        return cursor;
    }
    public Cursor getSentence3(int id){
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(" SELECT SENTENCE FROM project_table2 WHERE ID =" + id, null);
        return cursor;
    }
    public boolean updateWord(int id, String word){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_1, id);
        contentValues.put(COL_4, word);
        db.update(TABLE_NAME, contentValues, "ID = "+id, null );
        return true;
    }
    public boolean updateWord3(int id, String word){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_1, id);
        contentValues.put(COL_4, word);
        db.update(TABLE_NAME3, contentValues, "ID = "+id, null );
        return true;
    }
    public boolean updateSent(int id, String sent){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_1, id);
        contentValues.put(COL_5, sent);
        db.update(TABLE_NAME, contentValues, "ID = "+id, null );
        return true;
    }
    public boolean updateSent3(int id, String sent){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_1, id);
        contentValues.put(COL_5, sent);
        db.update(TABLE_NAME3, contentValues, "ID = "+id, null );
        return true;
    }

}
