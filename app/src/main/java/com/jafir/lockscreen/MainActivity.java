package com.jafir.lockscreen;

import android.database.sqlite.SQLiteDatabase;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.jafir.lockscreen.bean.Word;
import com.jafir.lockscreen.bean.WordWithIndex;
import com.jafir.lockscreen.util.DbUtil;
import com.jafir.lockscreen.util.PreferenceUtil;
import com.jafir.lockscreen.util.StringUtil;
import com.jafir.lockscreen.util.TimeUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmQuery;
import io.realm.RealmResults;

import static com.jafir.lockscreen.util.PreferenceUtil.write;
import static com.jafir.lockscreen.util.StringUtil.filter;


/**
 * create by jafir 2016/11/14
 */
public class MainActivity extends AppCompatActivity {

    private TextView mName;
    private TextView mExplain;
    private TextView mSoundmark;
    private TextView mExample;

    private TextView mEngConfirm;
    private TextView mChConfirm;
    private TextView mCheck;

    private EditText mEngEditl;
    private EditText mChEditl;

    //紧急进入按钮 双击进入
    private View mUrgency;

    private RelativeLayout mWordLinear;
    private LinearLayout mChLinear;
    private LinearLayout mEngLinear;
    private ViewGroup viewgroup;
    private WindowManager wm;

    private int[] imgs = new int[]{R.mipmap.night, R.mipmap.night1, R.mipmap.night2, R.mipmap.night3, R.mipmap.night4,
            R.mipmap.night5, R.mipmap.night6, R.mipmap.night7, R.mipmap.night8,
            R.mipmap.night9, R.mipmap.night10, R.mipmap.night11
    };
    private int mCount = 20;
    private Word mWord;
    private String[] meanings;
    private ProgressBar mProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!PreferenceUtil.readBoolean(this, "common", "isOpen")) {
            finish();
            return;
        }
        setContentView(R.layout.activity_main);
        overridePendingTransition(0, 0);
        initWindow();
        initView();
        initData();

    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 0) {//加载数据
                dismissDialog();
                List<Word> word = (List<Word>) msg.obj;
                int orderIndex = getWordIndexByRandom();
                mWord = word.get(orderIndex);
                setView(mWord);
            }

        }
    };


    /**
     * 获取word的index（经过算法策略过后的）
     */
    private int getWordIndexByRandom() {
        int index = PreferenceUtil.readInt(MainActivity.this, "common", "index", 0);
        Log.d("debug", "index:::" + index);
        //如果=0表示第一次进来  如果>=count表示轮完了一遍 重置
        if (index >= mCount || index == 0) {
            index = 0;
            //重新打乱排序
            setWordRandom();
            //设置index 为0
            PreferenceUtil.write(MainActivity.this, "common", "index", 0);
        }
        RealmResults<WordWithIndex> realmResults = getWordRandom();
        //获取打乱顺序之后的 index
        int orderIndex = realmResults.get(index).getIndex();
        Log.d("debug", "orderindex:::" + orderIndex);
        //保存数据库
        Realm realm = Realm.getDefaultInstance();
        realm.beginTransaction();
        //设置此下标已用
        realmResults.get(index).setUsed(true);
        realm.copyFromRealm(realmResults);
        realm.commitTransaction();
        PreferenceUtil.write(MainActivity.this, "common", "index", ++index);
        return orderIndex;
    }


    /**
     * 实现策略：
     * <p>
     * 一共20个单词，拿出来，打乱排序
     * 然后之后的20词锁屏出现的单词 按照这个顺序，依次执行完一遍
     * 然后再重新排序，避免出现有时候 老是随机出现一个单词，很多单词咩有出现过的情况
     */
    private void setWordRandom() {
        Integer[] order = getRandomId(mCount);
        RealmList<WordWithIndex> list = new RealmList<>();
        for (int i = 0; i < order.length; i++) {
            WordWithIndex wordWithIndex = new WordWithIndex();
            Log.d("debug", "iiiindex:" + order[i]);
            wordWithIndex.setIndex(order[i]);
            list.add(wordWithIndex);
        }
//保存到数据库
        final Realm realm = Realm.getDefaultInstance();
        realm.beginTransaction();
        realm.copyToRealm(list);
        realm.commitTransaction();

    }

    private RealmResults<WordWithIndex> getWordRandom() {
        final Realm realm = Realm.getDefaultInstance();
        //list
        RealmResults<WordWithIndex> realmResults = realm.where(WordWithIndex.class).findAll();
        return realmResults;
    }


    /**
     * 设置单词信息
     * 由于数据库里面的信息 有些有特殊字符 '\<\br\>' 'r n'等
     * 所以replace了
     *
     * @param word
     */
    private void setView(Word word) {
        if (TextUtils.isEmpty(word.getMeaning())) {
            mExplain.setVisibility(View.GONE);
        } else {
            //只过滤出中文
            String mean = mWord.getMeaning().replace("<br>", "");
            /**
             * 这里是 把意思拆解，  用'，'来分割成数组，然后检查是否填的是任意一个意思
             */
            for (int i = 0; i < mean.length(); i++) {
                Log.d("debug", "char:" + (int) mean.charAt(i));
            }
            Log.d("debug", "过滤" + filter(mean));
            String atlerfiter = StringUtil.filter(mWord.getMeaning().replace("<br>", ""));
            if (atlerfiter.contains("，")) {
                meanings = atlerfiter.split("，");
            } else {
                meanings = atlerfiter.split(",");
            }
            mExplain.setText(word.getMeaning().replace("<br>", ""));

            for (int i = 0; i < meanings.length; i++) {
                Log.d("debug", "过后数组:" + meanings[i] + "\n");
            }
        }
        if (TextUtils.isEmpty(word.getExmple())) {
            mExample.setVisibility(View.GONE);
        } else {
            mExample.setText(word.getExmple().replace("/r/n", "\n").replace("<br>", ""));
        }
        mEngLinear.setVisibility(View.VISIBLE);
        mName.setText(word.getWord().replace("<br>", ""));
        mSoundmark.setVisibility(View.GONE);
    }

    private void initWindow() {
        viewgroup = (ViewGroup) View.inflate(this, R.layout.lock_screen, null);
        viewgroup.setBackgroundResource(imgs[new Random().nextInt(5)]);
        wm = (WindowManager) getApplicationContext().getSystemService(WINDOW_SERVICE);
        WindowManager.LayoutParams wmParams = new WindowManager.LayoutParams();
        /**
         * 这里的type flag很关键
         */
        wmParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;
        wmParams.format = PixelFormat.OPAQUE;
        wmParams.flags = WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR
        ;
        wmParams.width = getResources().getDisplayMetrics().widthPixels;
        wmParams.height = getResources().getDisplayMetrics().heightPixels;
        wm.addView(viewgroup, wmParams);
    }


    /**
     * 记单词的策略
     * <p>
     * 一周7*n个
     * 一天就是n个
     * n个里面  随机排序
     * 等到所有的都显示完了一遍就重新打乱排序
     * <p>
     * <p>
     * 只有今天的第一次进入的时候 加载数据库 随机选出n个
     * 今天的n个存入数据库，今天就只出现他们
     * <p>
     * <p>
     * <p>
     * 记过的单词
     */
    private void initData() {
        Realm.init(this);
//        setWordRandom();
        //计算使用天数 只要开启的时候才计入
        int days = PreferenceUtil.readInt(this, "common", "days", 0);
        //如果 日期不是今天 那么说明是不同天数
        if (!TimeUtil.getTodayDate().equals(PreferenceUtil.readString(this, "common", "date", "defaultTIme"))) {
            //天数+1
            days++;
            write(this, "common", "days", days);
            loadData();
        } else {
            loadDataFromLocal();
        }
    }

    /**
     * 从本地数据库加载
     */
    private void loadDataFromLocal() {
        final Realm realm = Realm.getDefaultInstance();
        RealmQuery<Word> query = realm.where(Word.class);
        // Execute the query:
        RealmResults<Word> result1 = query.findAll();
        for (int i = 0; i < result1.size(); i++) {
            Word word = result1.get(i);
            Log.d("debug", "realm words:" + word.toString() + "\n");

//            String mean = word.getMeaning().replace("<br>","");
//            Log.d("debug","过滤"+StringUtil.filter(mean));
//            meanings = StringUtil.filter(word.getMeaning().replace("<br>", "")).split(",");
//
//            for (int j = 0; j < meanings.length; j++) {
//                Log.d("debug","过后数组:"+meanings[j]);
//
//            }

        }
        mWord = result1.get(getWordIndexByRandom());
        setView(mWord);

    }

    /**
     * 一天一次 从数据库加载数据进来
     */
    private void loadData() {
        showDialog();
        new Thread(new Runnable() {
            @Override
            public void run() {
                write(MainActivity.this, "common", "date", TimeUtil.getTodayDate());
                SQLiteDatabase db = DbUtil.openDatabase(MainActivity.this);
                WordsDao dao = new WordsDao(db);
                List<Word> list = dao.getRandomWordList(mCount);
                if (list.size() == 0 || list == null) {
                    return;
                }
                final Realm realm = Realm.getDefaultInstance();
                realm.beginTransaction();
                for (int i = 0; i < list.size(); i++) {
                    Word word = list.get(i);
                    Log.d("debug", "words:" + word.toString() + "\n");
                    // 存入数据库
                    realm.copyToRealm(word);
                }
                realm.commitTransaction();

                Message message = new Message();
                message.what = 0;
                message.obj = list;
                handler.sendMessage(message);
            }

        }).start();

    }

    /**
     * 随机打乱排序
     *
     * @param n
     * @return
     */
    public static Integer[] getRandomId(int n) {
        Integer[] arryRandom = new Integer[n];
        for (int i = 0; i < n; i++)
            arryRandom[i] = i;
        List list = Arrays.asList(arryRandom);
        Collections.shuffle(list);
        return arryRandom;
    }


    private void showDialog() {
        mProgress.setVisibility(View.VISIBLE);
    }

    private void dismissDialog() {
        mProgress.setVisibility(View.GONE);
    }

    private void initView() {
        mUrgency = viewgroup.findViewById(R.id.urgency);
        mUrgency.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                MainActivity.this.finish();
                return true;
            }
        });

        mName = (TextView) viewgroup.findViewById(R.id.name);
        mExplain = (TextView) viewgroup.findViewById(R.id.explain);
        mSoundmark = (TextView) viewgroup.findViewById(R.id.yinbiao);
        mExample = (TextView) viewgroup.findViewById(R.id.example);
        mProgress = (ProgressBar) viewgroup.findViewById(R.id.progress);
        mEngEditl = (EditText) viewgroup.findViewById(R.id.type);
        mChEditl = (EditText) viewgroup.findViewById(R.id.type_ch);
        mWordLinear = (RelativeLayout) viewgroup.findViewById(R.id.word_linear);
        mChLinear = (LinearLayout) viewgroup.findViewById(R.id.ch_linear);
        mEngLinear = (LinearLayout) viewgroup.findViewById(R.id.eng_linear);
        mEngConfirm = (TextView) viewgroup.findViewById(R.id.confirm);
        mChConfirm = (TextView) viewgroup.findViewById(R.id.confirm_ch);
        mCheck = (TextView) viewgroup.findViewById(R.id.check);
        mCheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reset();
                hideWord(false);

            }
        });

        mEngConfirm.setVisibility(View.GONE);
        mChConfirm.setVisibility(View.GONE);
/**
 * 在代码里设置有效
 */
        mEngEditl.setImeOptions(EditorInfo.IME_ACTION_DONE);
        mChEditl.setImeOptions(EditorInfo.IME_ACTION_DONE);

        mEngEditl.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                hideWord(true);
                return false;
            }
        });
        mEngEditl.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                if (!TextUtils.isEmpty(s.toString())) {
                    if (mWordLinear.getVisibility() != View.GONE) {
                        hideWord(true);
                    }
                } else {
                    hideWord(false);
                    hideSoftInput();
                }
                if (s.toString().equals(mWord.getWord())) {
                    mEngLinear.setVisibility(View.GONE);
                    mChLinear.setVisibility(View.VISIBLE);
                    mChEditl.requestFocus();
                }
                Log.d("debug", "onTextChanged");
            }

            @Override
            public void afterTextChanged(Editable s) {
                Log.d("debug", "afterTextChanged");
            }
        });
        mChEditl.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                if (!TextUtils.isEmpty(mWord.getMeaning())
                        &&
                        StringUtil.isContainChinese(s.toString())
                        &&
                        isContain(s.toString())) {
                    hideSoftInput();
                    MainActivity.this.finish();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    /**
     * 是否是所有意思中的任意一个
     *
     * @param s
     * @return
     */
    private boolean isContain(String s) {
        for (int i = 0; i < meanings.length; i++) {
            if (s.equals(meanings[i])) {
                return true;
            }
        }
        return false;
    }


    /**
     * 隐藏单词信息 动画
     *
     * @param b
     */

    private void hideWord(boolean b) {
        if (b) {
            AlphaAnimation a = new AlphaAnimation(0, 1);
            a.setDuration(1500);
            AlphaAnimation nameAnimation = new AlphaAnimation(1, 0);
            nameAnimation.setDuration(300);
            mCheck.setVisibility(View.VISIBLE);
            mName.startAnimation(nameAnimation);
            mExplain.startAnimation(nameAnimation);
            mSoundmark.startAnimation(nameAnimation);
            mExample.startAnimation(nameAnimation);
            mCheck.startAnimation(a);
            mWordLinear.setVisibility(View.GONE);

        } else {
            AlphaAnimation a = new AlphaAnimation(0, 1);
            a.setDuration(1000);
            mWordLinear.setVisibility(View.VISIBLE);
            mName.startAnimation(a);
            mExplain.startAnimation(a);
            mSoundmark.startAnimation(a);
            mExample.startAnimation(a);

            mCheck.setVisibility(View.GONE);
        }
    }

    /**
     * 重置
     */
    private void reset() {
        mEngLinear.setVisibility(View.VISIBLE);
        mChLinear.setVisibility(View.GONE);
        mChEditl.setText("");
        mEngEditl.setText("");
        hideSoftInput();
    }

    private void hideSoftInput() {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(mEngEditl.getWindowToken(), 0);
        inputMethodManager.hideSoftInputFromWindow(mChEditl.getWindowToken(), 0);
    }

    @Override
    protected void onDestroy() {
        setContentView(R.layout.transparent);
        if (wm != null) {
            wm.removeView(viewgroup);
        }
        super.onDestroy();
    }
}
