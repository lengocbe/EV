package vn.gheptuvung.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class WordDatabase extends SQLiteOpenHelper {
    private static final String TABLE = "words";

    public WordDatabase(Context context) {
        super(context, "ghep_tu_vung.db", null, 1);
    }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE words (id INTEGER PRIMARY KEY AUTOINCREMENT, english TEXT NOT NULL, vietnamese TEXT NOT NULL, UNIQUE(english, vietnamese))");
        String[][] starter = {
                {"apple", "quả táo"}, {"book", "quyển sách"}, {"cat", "con mèo"}, {"dog", "con chó"},
                {"water", "nước"}, {"school", "trường học"}, {"house", "ngôi nhà"}, {"happy", "vui vẻ"},
                {"friend", "bạn bè"}, {"sun", "mặt trời"}, {"moon", "mặt trăng"}, {"flower", "bông hoa"},
                {"car", "xe hơi"}, {"food", "đồ ăn"}, {"mother", "mẹ"}, {"father", "bố"},
                {"red", "màu đỏ"}, {"blue", "màu xanh dương"}, {"morning", "buổi sáng"}, {"thank you", "cảm ơn"}
        };
        for (String[] word : starter) insert(db, word[0], word[1]);
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { }

    private void insert(SQLiteDatabase db, String english, String vietnamese) {
        ContentValues values = new ContentValues();
        values.put("english", english.trim());
        values.put("vietnamese", vietnamese.trim());
        db.insertWithOnConflict(TABLE, null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public boolean add(String english, String vietnamese) {
        if (english == null || vietnamese == null || english.trim().isEmpty() || vietnamese.trim().isEmpty()) return false;
        insert(getWritableDatabase(), english, vietnamese);
        return true;
    }

    public int addAll(List<WordItem> items) {
        SQLiteDatabase db = getWritableDatabase();
        int added = 0;
        db.beginTransaction();
        try {
            for (WordItem item : items) {
                ContentValues values = new ContentValues();
                values.put("english", item.english.trim());
                values.put("vietnamese", item.vietnamese.trim());
                if (db.insertWithOnConflict(TABLE, null, values, SQLiteDatabase.CONFLICT_IGNORE) != -1) added++;
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return added;
    }

    public int countWords() {
        Cursor cursor = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM " + TABLE, null);
        try { return cursor.moveToFirst() ? cursor.getInt(0) : 0; } finally { cursor.close(); }
    }

    public List<WordItem> randomTen() {
        List<WordItem> result = allWords();
        Collections.shuffle(result);
        return result.subList(0, Math.min(10, result.size()));
    }

    private List<WordItem> allWords() {
        List<WordItem> result = new ArrayList<>();
        Cursor cursor = getReadableDatabase().query(TABLE, null, null, null, null, null, "id ASC");
        try {
            while (cursor.moveToNext()) result.add(new WordItem(cursor.getLong(0), cursor.getString(1), cursor.getString(2)));
        } finally { cursor.close(); }
        return result;
    }
}
