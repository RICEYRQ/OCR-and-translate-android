package com.example.riceyrq.ocr;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.baidu.ocr.sdk.OCR;
import com.baidu.ocr.sdk.OnResultListener;
import com.baidu.ocr.sdk.exception.OCRError;
import com.baidu.ocr.sdk.model.AccessToken;
import com.baidu.ocr.sdk.model.GeneralParams;
import com.baidu.ocr.sdk.model.GeneralResult;
import com.baidu.ocr.sdk.model.Word;
import com.baidu.ocr.sdk.model.WordSimple;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    String token;
    final static String AK = "rQw09Zbu38gcGDaVqe1kqDcP";
    final static String SK = "MORQTv6vvnBx2ufhOIVFY0NK114yTga5";
    Button take;
    Button find;
    Button getTex;
    ImageView imageView;
    private List<String> mPermissionList = new ArrayList<>();
    private String[] permissions = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
            Manifest.permission.INTERNET
    };
    String picPath = "";
    private List<ImageMes> list = new ArrayList<>();
    int count = 0;
    Spinner fromText;
    Spinner toText;
    int fromPos = 0;
    int toPos = 1;
    String[] textList = {
            "中文", "英语", "粤语", "文言文", "日语", "韩语", "法语", "西班牙语", "泰语",
            "阿拉伯语", "俄语", "葡萄牙语", "德语", "意大利语", "希腊语", "荷兰语", "波兰语",
            "保加利亚语", "爱沙尼亚语", "丹麦语", "芬兰语", "捷克语", "罗马尼亚语",
            "斯洛文尼亚语", "瑞典语", "匈牙利语", "繁体中文", "越南语"
    };
    String[] codeList = {
            "zh", "en", "yue", "wyw", "jp", "kor", "fra", "spa", "th",
            "ara", "ru", "pt", "de", "it", "el", "nl", "pl",
            "bul", "est", "dan", "fin", "cs", "rom",
            "slo", "swe", "hu", "cht", "vie"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        take = findViewById(R.id.take_photo);
        find = findViewById(R.id.find_photo);
        getTex = findViewById(R.id.get_text);
        imageView = findViewById(R.id.image);
        toText = findViewById(R.id.to);
        fromText = findViewById(R.id.from);
        ArrayAdapter<String> adapter =
                new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, textList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        fromText.setAdapter(adapter);
        fromText.setSelection(fromPos);
        fromText.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                fromPos = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        toText.setAdapter(adapter);
        toText.setSelection(toPos);
        toText.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                toPos = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        take.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!checkPer()) {
                    return;
                }
                String state = Environment.getExternalStorageState();// 获取内存卡可用状态
                if (state.equals(Environment.MEDIA_MOUNTED)) {
                    // 内存卡状态可用
                    Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
                    startActivityForResult(intent, 1);
                } else {
                    // 不可用
                    showToast("内存不可用");
                }
            }
        });
        find.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!checkPer()) {
                    return;
                }
                // 打开本地相册
                Intent i = new Intent(
                        Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                // 设定结果返回
                startActivityForResult(i, 2);
            }
        });
        getTex.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                list.clear();
                GeneralParams params = new GeneralParams();
                params.setDetectDirection(true);
                params.setImageFile(new File(picPath));
                OCR.getInstance(getApplicationContext()).recognizeGeneral(params,
                        new OnResultListener<GeneralResult>() {
                            @Override
                            public void onResult(GeneralResult generalResult) {
                                StringBuilder sb = new StringBuilder();
                                Log.e("YRQYRQ", generalResult.getJsonRes());
//                                Log.e("YRQYRQ", sb.toString());
                                try {
                                    JSONObject jsonObject1 = new JSONObject(generalResult.getJsonRes());
                                    JSONArray jsonArray =
                                            new JSONArray(jsonObject1.getString("words_result"));
                                    for (int i = 0; i < jsonArray.length(); i++) {
                                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                                        ImageMes imageMes = new ImageMes();
                                        imageMes.message = jsonObject.getString("words");
                                        imageMes.left = jsonObject.getJSONObject("location").getInt("left");
                                        imageMes.height =
                                                jsonObject.getJSONObject("location").getInt("height");
                                        imageMes.top =
                                                jsonObject.getJSONObject("location").getInt("top");
                                        imageMes.width =
                                                jsonObject.getJSONObject("location").getInt("width");
                                        list.add(imageMes);
                                    }
                                } catch (JSONException e) {
                                    Log.e("YRQYRQ", "JSONERROR " + e);
                                }
                                doTranslate();
                            }

                            @Override
                            public void onError(OCRError ocrError) {
                                Log.e("YRQYRQ", ocrError.toString());
                            }
                        });
            }
        });
        OCR.getInstance(getApplicationContext()).initAccessTokenWithAkSk(new OnResultListener<AccessToken>() {
            @Override
            public void onResult(AccessToken accessToken) {
                token = accessToken.getAccessToken();
                Log.e("YRQYRQ", token);
            }

            @Override
            public void onError(OCRError ocrError) {
                Log.e("YRQYRQ", ocrError.toString());
            }
        }, getApplicationContext(), AK, SK);
    }

    private void doTranslate() {
        getTex.setEnabled(false);
        count = 0;
        for (int i = 0; i < list.size(); i++) {
            final int finalI = i;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    String appId = "20180524000165845";
                    String salt = String.valueOf(System.currentTimeMillis());
                    String key = "q3HRvZXwsFiozZeEBJOm";
                    String p = null;
                    try {
                        p = new String(list.get(finalI).message.getBytes(), "UTF-8");
                    } catch (UnsupportedEncodingException e) {

                    }
                    OkHttpClient okHttpClient = new OkHttpClient.Builder().build();
                    FormBody formBody = new FormBody.Builder()
                            .add("q", encode(p))
                            .add("from", "zh")
                            .add("to", "en")
                            .add("salt", salt)
                            .add("appid", appId)
                            .add("sign",
                                    getSign(appId, p, salt, key))
                            .build();
                    String url = "http://api.fanyi.baidu.com/api/trans/vip/translate?"
                            + "q=" + p
                            + "&"
                            + "from=" + codeList[fromPos]
                            + "&"
                            + "to=" + codeList[toPos]
                            + "&"
                            + "appid=" + appId
                            + "&"
                            + "salt=" + salt
                            + "&"
                            + "sign=" +  getSign(appId, p, salt, key);
                    final Request request = new Request.Builder()
                            .url(url)
                            .get()
//                            .post(formBody)
                            .build();
                    Call call = okHttpClient.newCall(request);
                    call.enqueue(new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            Log.e("YRQYRQ", "get onFailure " + e);
                            count++;
                            if (count == list.size()) {
                                drawImageText();
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        getTex.setEnabled(true);
                                    }
                                });
                            }
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
//                            Log.e("YRQYRQ", "translate " + response.body().string());
                            String re = response.body().string();
                            Log.e("YRQYRQ", "translate " + re);
                            try {
                                JSONObject jsonObject = new JSONObject(re);
                                JSONArray jsonArray = jsonObject.getJSONArray("trans_result");
                                list.get(finalI).translateResult =
                                        jsonArray.getJSONObject(0).getString("dst");
                                Log.e("YRQYRQ", "translate " + list.get(finalI).translateResult);
                                count++;

                            } catch (JSONException e) {

                            }
                            if (count == list.size()) {
                                drawImageText();
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        getTex.setEnabled(true);
                                    }
                                });
                            }
                        }
                    });


                }
            }).start();
        }

    }

    private int sp2px(float spValue) {
        final float scale = this.getResources().getDisplayMetrics().density;
        return (int) (spValue * scale + 0.5f);
    }

    private float getRightSize(String text, float canvasWidth){
        Paint paint = new Paint();
        paint.setTextSize(sp2px(200));
        paint.setTextAlign(Paint.Align.CENTER);
        //根据最大值，计算出当前文本占用的宽度
        float preWidth = paint.measureText(text);
        //如果文本占用的宽度比画布宽度小，说明不用伸缩，直接返回当前字号
        if(preWidth < canvasWidth){
            return 200;
        }
        //已知当前文本字号、文本占用宽度、画布宽度，计算出合适的字号，并返回
        return 200 * canvasWidth / preWidth;
    }

    public void drawImageText() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Bitmap bitmap =
                        BitmapFactory.decodeFile(picPath).copy(Bitmap.Config.RGB_565, true);
                Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                paint.setColor(Color.WHITE);
                paint.setStyle(Paint.Style.FILL);
                Paint paint1 = new Paint(Paint.ANTI_ALIAS_FLAG);
                Canvas canvas = new Canvas(bitmap);
                for (int i = 0; i < list.size(); i++) {
                    ImageMes imageMes = list.get(i);
                    if (TextUtils.isEmpty(imageMes.translateResult))
                        continue;
                    Rect rect = new Rect(imageMes.left, imageMes.top,
                            imageMes.left + imageMes.width, imageMes.top + imageMes.height);
                    canvas.drawRect(rect, paint);
                    paint1.setColor(Color.BLACK);
                    paint1.setTextSize(
                            sp2px(getRightSize(imageMes.translateResult, imageMes.width)));
                    Log.e("YRQYRQ",
                            "size " + getRightSize(imageMes.translateResult, imageMes.width));
                    paint1.setStyle(Paint.Style.FILL);
                    paint1.setTextAlign(Paint.Align.CENTER);
                    Paint.FontMetrics fontMetrics = paint1.getFontMetrics();
                    float top = fontMetrics.top;//为基线到字体上边框的距离,即上图中的top
                    float bottom = fontMetrics.bottom;//为基线到字体下边框的距离,即上图中的bottom
                    int baseLineY = (int) (rect.centerY() - top / 2
                            - bottom / 2);
                    // 计算Baseline绘制的起点X轴坐标 ，计算方式：画布宽度的一半 - 文字宽度的一半
                    Log.e("YRQYRQ",
                            "width " + imageMes.width
                                    + " textWidth " + paint1.measureText(imageMes.translateResult));
                    int x = (int) (imageMes.width / 2
                            - paint1.measureText(imageMes.translateResult) / 2);

                    // 计算Baseline绘制的Y坐标 ，计算方式：画布高度的一半 - 文字总高度的一半
                    int y = (int) ((imageMes.height / 2)
                            - ((paint1.descent() + paint1.ascent()) / 2));
                    Log.e("YRQYRQ", "x " + x + " y " + y);
                    Log.e("YRQYRQ", "text " + imageMes.translateResult);
                    canvas.drawText(imageMes.translateResult, rect.centerX(), baseLineY, paint1);
                }
                final Bitmap bitmap1 = bitmap;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        imageView.setImageBitmap(bitmap1);
                    }
                });
            }
        }).start();
    }

    private String getSign(String appId, String p, String salt, String key) {
        String iii = appId + p + salt + key;
        String sign = MD5.md5(iii);
        Log.e("YRQYRQ", "iii " + iii);
        Log.e("YRQYRQ", "sign " + sign);
        Log.e("YRQYRQ", "encode " + encode(p));
        return sign;
    }

    /**
     * 对输入的字符串进行URL编码, 即转换为%20这种形式
     *
     * @param input 原文
     * @return URL编码. 如果编码失败, 则返回原文
     */
    public static String encode(String input) {
        if (input == null) {
            return "";
        }

        try {
            return URLEncoder.encode(input, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return input;
    }


    @Override
    protected void onActivityResult ( int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        Bitmap photo = null;
        if (data != null) {
            switch (requestCode) {
                case 1:
                    if (data.getData() != null || data.getExtras() != null) { // 防止没有返回结果
                        Uri uri = data.getData();
                        if (uri != null) {
                            photo = BitmapFactory.decodeFile(uri.getPath()); // 拿到图片
                        }
                        if (photo == null) {
                            Bundle bundle = data.getExtras();
                            if (bundle != null) {
                                photo = (Bitmap) bundle.get("data");
                                FileOutputStream fileOutputStream = null;
                                try {
                                    // 获取 SD 卡根目录 生成图片并新建目录
                                    String saveDir = Environment
                                            .getExternalStorageDirectory()
                                            + "/dhj_Photos";
                                    File dir = new File(saveDir);
                                    if (!dir.exists())
                                        dir.mkdir();
                                    // 生成文件名
                                    SimpleDateFormat t = new SimpleDateFormat(
                                            "yyyyMMddssSSS");
                                    String filename = "MT" + (t.format(new Date()))
                                            + ".jpg";
                                    // 新建文件
                                    File file = new File(saveDir, filename);
                                    // 打开文件输出流
                                    fileOutputStream = new FileOutputStream(file);
                                    // 生成图片文件
                                    photo.compress(Bitmap.CompressFormat.JPEG,
                                            100, fileOutputStream);
                                    // 相片的完整路径
                                    picPath = file.getPath();
                                    imageView.setImageBitmap(photo);
                                } catch (FileNotFoundException e) {

                                } finally {
                                    if (fileOutputStream != null) {
                                        try {
                                            fileOutputStream.close();
                                        } catch (IOException e) {


                                        }
                                    }
                                }
                            }
                        }
                    }
                    break;
                case 2:
                    Uri selectedImage = data.getData();
                    String[] filePathColumn = {MediaStore.Images.Media.DATA};
                    Cursor cursor = getContentResolver().query(selectedImage,
                            filePathColumn, null, null, null);
                    cursor.moveToFirst();
                    int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                    picPath = cursor.getString(columnIndex);
                    cursor.close();
                    imageView.setImageBitmap(BitmapFactory.decodeFile(picPath));
                    break;
            }
        }
    }

    private boolean checkPer() {
        mPermissionList.clear();
        for (int i = 0; i < permissions.length; i++) {
            if (ContextCompat.checkSelfPermission(MainActivity.this, permissions[i]) != PackageManager.PERMISSION_GRANTED) {
                mPermissionList.add(permissions[i]);
            }
        }
        if (mPermissionList.isEmpty()) {
            showToast("权限都有了");
            return true;
        }
        String[] permissions = mPermissionList.toArray(new String[mPermissionList.size()]);//将List转为数组
        ActivityCompat.requestPermissions(MainActivity.this, permissions, 2);
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        if (requestCode == 1) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showToast("权限已申请");
            } else {
                showToast("权限已拒绝");
            }
        } else if (requestCode == 2) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    //判断是否勾选禁止后不再询问
                    boolean showRequestPermission =
                            ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                                    permissions[i]);
                    if (showRequestPermission) {
                        showToast("权限未申请");
                    }
                } else {
                    mPermissionList.remove(permissions[i]);
                }
            }
            if (mPermissionList.isEmpty()) {

            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void showToast(String string) {
        Toast.makeText(MainActivity.this, string, Toast.LENGTH_LONG).show();
    }

    class ImageMes {
        public String message;
        public int left;
        public int width;
        public int top;
        public int height;
        public String translateResult;
    }
}
