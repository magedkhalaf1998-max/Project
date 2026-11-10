package com.example.screenshothelper;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final int PICK_IMAGE = 1001;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private Uri selectedImage;
    private ImageView preview;
    private EditText apiKeyInput;
    private EditText modelInput;
    private TextView status;
    private TextView answer;
    private Button sendButton;
    private ProgressBar progress;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildScreen();
    }

    private void buildScreen() {
        int pad = dp(18);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(pad, pad, pad, pad);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(content);
        setContentView(scroll);

        TextView title = new TextView(this);
        title.setText("مساعد الأسئلة بالذكاء الاصطناعي");
        title.setTextSize(23);
        title.setGravity(Gravity.CENTER);
        content.addView(title, matchWrap());

        TextView note = new TextView(this);
        note.setText("اختر صورة للسؤال. لن تُرسل إلى الذكاء الاصطناعي إلا بعد ضغط الإرسال وتأكيدك.");
        note.setTextSize(16);
        note.setPadding(0, dp(12), 0, dp(12));
        content.addView(note, matchWrap());

        apiKeyInput = new EditText(this);
        apiKeyInput.setHint("مفتاح OpenAI API (لا يُحفظ)");
        apiKeyInput.setSingleLine(true);
        apiKeyInput.setInputType(129);
        content.addView(apiKeyInput, matchWrap());

        modelInput = new EditText(this);
        modelInput.setHint("اسم النموذج");
        modelInput.setText("gpt-4o-mini");
        modelInput.setSingleLine(true);
        content.addView(modelInput, matchWrap());

        Button chooseButton = new Button(this);
        chooseButton.setText("اختيار صورة من الهاتف");
        content.addView(chooseButton, matchWrap());
        chooseButton.setOnClickListener(v -> chooseImage());

        preview = new ImageView(this);
        preview.setAdjustViewBounds(true);
        preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        content.addView(preview, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(260)));

        sendButton = new Button(this);
        sendButton.setText("إرسال الصورة وشرح السؤال");
        content.addView(sendButton, matchWrap());
        sendButton.setOnClickListener(v -> confirmAndSend());

        progress = new ProgressBar(this);
        progress.setVisibility(android.view.View.GONE);
        content.addView(progress, centeredWrap());

        status = new TextView(this);
        status.setTextSize(14);
        content.addView(status, matchWrap());

        answer = new TextView(this);
        answer.setTextSize(17);
        answer.setTextIsSelectable(true);
        answer.setPadding(0, dp(12), 0, dp(12));
        content.addView(answer, matchWrap());

        TextView privacy = new TextView(this);
        privacy.setText("تنبيه: عند التأكيد تُرسل الصورة إلى OpenAI عبر الإنترنت. لا تستخدم صوراً حساسة، واستخدم التطبيق وفق قواعد مدرستك أو مكان عملك.");
        privacy.setTextSize(13);
        privacy.setPadding(0, dp(16), 0, 0);
        content.addView(privacy, matchWrap());
    }

    private void chooseImage() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedImage = data.getData();
            preview.setImageURI(selectedImage);
            status.setText("تم اختيار الصورة. راجعها قبل الإرسال.");
            answer.setText("");
        }
    }

    private void confirmAndSend() {
        if (selectedImage == null) {
            status.setText("اختر صورة أولاً.");
            return;
        }
        String key = apiKeyInput.getText().toString().trim();
        if (key.isEmpty()) {
            status.setText("أدخل مفتاح OpenAI API أولاً.");
            return;
        }
        String model = modelInput.getText().toString().trim();
        if (model.isEmpty()) model = "gpt-4o-mini";
        final String chosenModel = model;

        new AlertDialog.Builder(this)
                .setTitle("تأكيد إرسال الصورة")
                .setMessage("سيتم إرسال الصورة التي اخترتها إلى OpenAI لمعالجة السؤال. هل تريد المتابعة؟")
                .setNegativeButton("إلغاء", (dialog, which) -> { })
                .setPositiveButton("إرسال", (dialog, which) -> sendToAi(key, chosenModel))
                .show();
    }

    private void sendToAi(String key, String model) {
        setBusy(true);
        status.setText("جارٍ تحليل الصورة…");
        answer.setText("");
        Uri imageUri = selectedImage;
        executor.execute(() -> {
            try {
                String result = requestAnswer(key, model, imageUri);
                runOnUiThread(() -> answer.setText(result));
                runOnUiThread(() -> status.setText("اكتمل التحليل. تحقق من الإجابة؛ قد يخطئ الذكاء الاصطناعي."));
            } catch (Exception e) {
                String message = e.getMessage() == null ? "حدث خطأ غير معروف." : e.getMessage();
                runOnUiThread(() -> status.setText("تعذر تحليل الصورة: " + message));
            } finally {
                runOnUiThread(() -> setBusy(false));
            }
        });
    }

    private String requestAnswer(String key, String model, Uri imageUri) throws Exception {
        Bitmap bitmap;
        try (InputStream input = getContentResolver().openInputStream(imageUri)) {
            if (input == null) throw new IllegalStateException("تعذر فتح الصورة.");
            bitmap = BitmapFactory.decodeStream(input);
        }
        if (bitmap == null) throw new IllegalArgumentException("الملف ليس صورة قابلة للقراءة.");

        int largest = Math.max(bitmap.getWidth(), bitmap.getHeight());
        if (largest > 1800) {
            float scale = 1800f / largest;
            Bitmap smaller = Bitmap.createScaledBitmap(bitmap,
                    Math.round(bitmap.getWidth() * scale), Math.round(bitmap.getHeight() * scale), true);
            bitmap.recycle();
            bitmap = smaller;
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 88, bytes);
        bitmap.recycle();
        String encoded = android.util.Base64.encodeToString(bytes.toByteArray(), android.util.Base64.NO_WRAP);

        JSONObject imageUrl = new JSONObject().put("url", "data:image/jpeg;base64," + encoded);
        JSONObject imagePart = new JSONObject().put("type", "image_url").put("image_url", imageUrl);
        JSONObject textPart = new JSONObject().put("type", "text")
                .put("text", "اقرأ السؤال الظاهر في الصورة واشرح طريقة الحل بالعربية. إذا لم يكن جزء من الصورة واضحاً، اطلب التوضيح ولا تخمّن.");
        JSONArray userContent = new JSONArray().put(textPart).put(imagePart);
        JSONArray messages = new JSONArray()
                .put(new JSONObject().put("role", "system").put("content",
                        "أنت مساعد تعليمي. اشرح الحل بدقة وبالعربية، ولا تخمّن إذا كانت الصورة غير واضحة."))
                .put(new JSONObject().put("role", "user").put("content", userContent));
        JSONObject body = new JSONObject().put("model", model).put("messages", messages);

        HttpURLConnection connection = (HttpURLConnection) new URL("https://api.openai.com/v1/chat/completions").openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(20000);
        connection.setReadTimeout(60000);
        connection.setDoOutput(true);
        connection.setRequestProperty("Authorization", "Bearer " + key);
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        try (OutputStream output = connection.getOutputStream()) {
            output.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }
        int code = connection.getResponseCode();
        InputStream responseStream = code >= 200 && code < 300
                ? connection.getInputStream() : connection.getErrorStream();
        String responseText;
        try (InputStream input = responseStream; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            responseText = output.toString("UTF-8");
        } finally {
            connection.disconnect();
        }
        JSONObject response = new JSONObject(responseText);
        if (code < 200 || code >= 300) {
            JSONObject error = response.optJSONObject("error");
            throw new IllegalStateException(error == null ? "طلب API غير ناجح (HTTP " + code + ")." : error.optString("message", "طلب API غير ناجح."));
        }
        return response.getJSONArray("choices").getJSONObject(0)
                .getJSONObject("message").optString("content", "لم يصل رد نصي.");
    }

    private void setBusy(boolean busy) {
        progress.setVisibility(busy ? android.view.View.VISIBLE : android.view.View.GONE);
        sendButton.setEnabled(!busy);
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams centeredWrap() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER;
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        executor.shutdown();
        super.onDestroy();
    }
}
