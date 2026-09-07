package vn.gheptuvung.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity {
    private static final int IMPORT_REQUEST = 12;
    private static final int BLUE = Color.rgb(221, 240, 255);
    private static final int ORANGE = Color.rgb(255, 235, 207);
    private static final int YELLOW = Color.rgb(249, 193, 50);
    private static final int GREEN = Color.rgb(185, 234, 202);
    private static final int RED = Color.rgb(255, 211, 211);
    private static final int NAVY = Color.rgb(31, 59, 107);

    private final Handler handler = new Handler(Looper.getMainLooper());
    private WordDatabase database;
    private TextView countText;
    private TextView gameInfo;
    private WordItem selectedVietnamese;
    private WordItem selectedEnglish;
    private TextView selectedVietnameseCard;
    private TextView selectedEnglishCard;
    private int matched;
    private int wrong;
    private boolean resolving;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        database = new WordDatabase(this);
        getWindow().setStatusBarColor(NAVY);
        showHome();
    }

    private void showHome() {
        selectedVietnamese = null;
        selectedEnglish = null;
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(20), dp(22), dp(20), dp(18));
        page.setBackgroundColor(Color.rgb(249, 250, 255));

        TextView icon = text("📚", 50, NAVY, false);
        icon.setGravity(Gravity.CENTER);
        page.addView(icon);
        TextView title = text("GHÉP TỪ VỰNG", 27, NAVY, true);
        title.setGravity(Gravity.CENTER);
        page.addView(title);
        TextView intro = text("Chọn nghĩa tiếng Việt và từ tiếng Anh tương ứng để ghép cặp.", 16, Color.rgb(82, 91, 115), false);
        intro.setGravity(Gravity.CENTER);
        intro.setPadding(dp(8), dp(10), dp(8), dp(22));
        page.addView(intro);

        countText = text("", 17, Color.rgb(35, 100, 77), true);
        countText.setGravity(Gravity.CENTER);
        countText.setPadding(0, dp(10), 0, dp(18));
        page.addView(countText);

        Button play = mainButton("BẮT ĐẦU GHÉP 10 CẶP", Color.rgb(50, 120, 224));
        play.setOnClickListener(v -> startGame());
        page.addView(play, fullWidth());
        Button add = mainButton("THÊM TỪ THỦ CÔNG", Color.rgb(38, 151, 103));
        add.setOnClickListener(v -> showAddWordDialog());
        page.addView(add, marginTop(10));
        Button importFile = mainButton("IMPORT CSV / EXCEL", Color.rgb(242, 137, 55));
        importFile.setOnClickListener(v -> chooseImportFile());
        page.addView(importFile, marginTop(10));

        TextView guide = text("Mẫu file: cột A là English, cột B là Vietnamese. Có thể dùng file .csv hoặc .xlsx từ Excel/Google Sheets.",
                14, Color.rgb(100, 108, 126), false);
        guide.setPadding(dp(5), dp(24), dp(5), 0);
        page.addView(guide);

        setContentView(page);
        refreshCount();
    }

    private void refreshCount() {
        if (countText != null) countText.setText("Kho từ hiện có: " + database.countWords() + " cặp từ");
    }

    private void startGame() {
        List<WordItem> pairs = new ArrayList<>(database.randomTen());
        if (pairs.size() < 2) {
            Toast.makeText(this, "Cần ít nhất 2 cặp từ để chơi.", Toast.LENGTH_SHORT).show();
            return;
        }
        matched = 0;
        wrong = 0;
        selectedVietnamese = null;
        selectedEnglish = null;
        showGame(pairs);
    }

    private void showGame(List<WordItem> pairs) {
        ScrollView scroll = new ScrollView(this);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(12), dp(14), dp(12), dp(18));
        page.setBackgroundColor(Color.rgb(249, 250, 255));
        scroll.addView(page);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        Button back = new Button(this);
        back.setText("← Quay lại");
        back.setOnClickListener(v -> showHome());
        top.addView(back);
        gameInfo = text("Ghép 0/" + pairs.size() + " cặp", 17, NAVY, true);
        gameInfo.setGravity(Gravity.CENTER);
        top.addView(gameInfo, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        page.addView(top);

        TextView tip = text("Chọn 1 thẻ Anh bên trái và 1 thẻ Việt bên phải.", 14, Color.rgb(88, 95, 111), false);
        tip.setGravity(Gravity.CENTER);
        tip.setPadding(0, dp(4), 0, dp(13));
        page.addView(tip);

        LinearLayout columns = new LinearLayout(this);
        columns.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout englishColumn = column("ENGLISH", Color.rgb(225, 121, 37));
        LinearLayout vietnameseColumn = column("TIẾNG VIỆT", Color.rgb(47, 112, 210));
        columns.addView(englishColumn, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        LinearLayout.LayoutParams rightParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        rightParams.setMargins(dp(8), 0, 0, 0);
        columns.addView(vietnameseColumn, rightParams);
        page.addView(columns);

        List<WordItem> left = new ArrayList<>(pairs);
        List<WordItem> right = new ArrayList<>(pairs);
        Collections.shuffle(left);
        Collections.shuffle(right);
        for (WordItem item : left) englishColumn.addView(card(item.english, item, true), marginTop(7));
        for (WordItem item : right) vietnameseColumn.addView(card(item.vietnamese, item, false), marginTop(7));
        setContentView(scroll);
    }

    private LinearLayout column(String heading, int color) {
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        TextView header = text(heading, 14, color, true);
        header.setGravity(Gravity.CENTER);
        header.setPadding(dp(4), 0, dp(4), dp(5));
        column.addView(header);
        return column;
    }

    private TextView card(String label, WordItem item, boolean english) {
        TextView card = text(label, 16, Color.rgb(36, 43, 60), true);
        card.setGravity(Gravity.CENTER);
        card.setMinHeight(dp(50));
        card.setPadding(dp(6), dp(7), dp(6), dp(7));
        card.setTag(item);
        setCardStyle(card, english ? ORANGE : BLUE, Color.TRANSPARENT);
        card.setOnClickListener(v -> selectCard((TextView) v, english));
        return card;
    }

    private void selectCard(TextView card, boolean english) {
        if (resolving) return;
        if (card.getVisibility() != View.VISIBLE) return;
        WordItem item = (WordItem) card.getTag();
        if (english) {
            if (selectedEnglishCard != null) setCardStyle(selectedEnglishCard, ORANGE, Color.TRANSPARENT);
            selectedEnglish = item;
            selectedEnglishCard = card;
            setCardStyle(card, ORANGE, YELLOW);
        } else {
            if (selectedVietnameseCard != null) setCardStyle(selectedVietnameseCard, BLUE, Color.TRANSPARENT);
            selectedVietnamese = item;
            selectedVietnameseCard = card;
            setCardStyle(card, BLUE, YELLOW);
        }
        if (selectedEnglish != null && selectedVietnamese != null) checkPair();
    }

    private void checkPair() {
        resolving = true;
        boolean correct = selectedEnglish.id == selectedVietnamese.id;
        if (correct) {
            setCardStyle(selectedEnglishCard, GREEN, Color.rgb(32, 155, 82));
            setCardStyle(selectedVietnameseCard, GREEN, Color.rgb(32, 155, 82));
            TextView left = selectedVietnameseCard;
            TextView right = selectedEnglishCard;
            handler.postDelayed(() -> {
                left.setVisibility(View.GONE);
                right.setVisibility(View.GONE);
                matched++;
                selectedEnglish = null;
                selectedVietnamese = null;
                selectedEnglishCard = null;
                selectedVietnameseCard = null;
                resolving = false;
                updateGameInfo();
            }, 360);
        } else {
            wrong++;
            setCardStyle(selectedEnglishCard, RED, Color.rgb(220, 76, 76));
            setCardStyle(selectedVietnameseCard, RED, Color.rgb(220, 76, 76));
            handler.postDelayed(() -> {
                setCardStyle(selectedEnglishCard, ORANGE, Color.TRANSPARENT);
                setCardStyle(selectedVietnameseCard, BLUE, Color.TRANSPARENT);
                selectedEnglish = null;
                selectedVietnamese = null;
                selectedEnglishCard = null;
                selectedVietnameseCard = null;
                resolving = false;
                updateGameInfo();
            }, 550);
        }
    }

    private void updateGameInfo() {
        if (gameInfo == null) return;
        int total = Math.min(10, database.countWords());
        gameInfo.setText("Ghép " + matched + "/" + total + " cặp");
        if (matched == total) {
            new AlertDialog.Builder(this).setTitle("Hoàn thành! 🎉")
                    .setMessage("Bạn đã ghép đúng " + total + " cặp từ.\nSố lần ghép sai: " + wrong)
                    .setPositiveButton("Chơi lại", (d, w) -> startGame())
                    .setNegativeButton("Trang chủ", (d, w) -> showHome()).show();
        }
    }

    private void showAddWordDialog() {
        LinearLayout form = new LinearLayout(this);
        form.setPadding(dp(22), dp(5), dp(22), 0);
        form.setOrientation(LinearLayout.VERTICAL);
        EditText english = new EditText(this);
        english.setHint("Từ tiếng Anh, ví dụ: apple");
        form.addView(english);
        EditText vietnamese = new EditText(this);
        vietnamese.setHint("Nghĩa tiếng Việt, ví dụ: quả táo");
        form.addView(vietnamese);
        new AlertDialog.Builder(this).setTitle("Thêm cặp từ")
                .setView(form).setNegativeButton("Hủy", null).setPositiveButton("Lưu", (d, w) -> {
                    if (database.add(english.getText().toString(), vietnamese.getText().toString())) {
                        refreshCount(); Toast.makeText(this, "Đã thêm từ.", Toast.LENGTH_SHORT).show();
                    } else Toast.makeText(this, "Hãy nhập đủ tiếng Anh và tiếng Việt.", Toast.LENGTH_SHORT).show();
                }).show();
    }

    private void chooseImportFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"text/csv", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"});
        startActivityForResult(intent, IMPORT_REQUEST);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != IMPORT_REQUEST || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        String name = getDisplayName(uri);
        if (name == null) name = "vocabulary.xlsx";
        try {
            List<WordItem> imported = ImportReader.read(getContentResolver(), uri, name);
            int added = database.addAll(imported);
            refreshCount();
            Toast.makeText(this, "Đã đọc " + imported.size() + " dòng, thêm " + added + " cặp từ.", Toast.LENGTH_LONG).show();
        } catch (Exception error) {
            Toast.makeText(this, "Không import được: " + error.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String getDisplayName(Uri uri) {
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (column >= 0) return cursor.getString(column);
                }
            }
        }
        return uri.getLastPathSegment();
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView text = new TextView(this);
        text.setText(value); text.setTextSize(size); text.setTextColor(color);
        if (bold) text.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return text;
    }

    private Button mainButton(String label, int color) {
        Button button = new Button(this);
        button.setText(label); button.setTextColor(Color.WHITE); button.setTextSize(15); button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        GradientDrawable background = new GradientDrawable();
        background.setColor(color); background.setCornerRadius(dp(14));
        button.setBackground(background); button.setPadding(dp(8), dp(12), dp(8), dp(12));
        return button;
    }

    private void setCardStyle(TextView view, int backgroundColor, int borderColor) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(backgroundColor); shape.setCornerRadius(dp(13));
        shape.setStroke(dp(borderColor == Color.TRANSPARENT ? 1 : 3), borderColor == Color.TRANSPARENT ? Color.rgb(221, 225, 233) : borderColor);
        view.setBackground(shape);
    }

    private LinearLayout.LayoutParams fullWidth() { return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT); }
    private LinearLayout.LayoutParams marginTop(int top) { LinearLayout.LayoutParams p = fullWidth(); p.setMargins(0, dp(top), 0, 0); return p; }
    private int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }
}
