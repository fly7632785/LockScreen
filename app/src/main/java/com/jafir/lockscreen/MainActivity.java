package com.jafir.lockscreen;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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

import static com.jafir.lockscreen.util.PreferenceUtil.readInt;
import static com.jafir.lockscreen.util.PreferenceUtil.write;
import static com.jafir.lockscreen.util.StringUtil.filter;


/**
 * create by jafir 2016/11/14
 * 这是锁屏界面的activity
 * <p>
 * 原理就是 在activity中控制 window的锁屏显示
 * <p>
 * 代码比较简单，个人觉得，之所以冗长，主要是因为有一些动画，
 * 还有就是 算法策略 对数据库的操作 这些比较繁杂
 */
public class MainActivity extends BaseActivity {

    private TextView mName;
    private TextView mExplain;
    private TextView mSoundmark;
    private TextView mExample;

    private TextView mNeverShow;

    private TextView mEngConfirm;
    private TextView mChConfirm;
    private TextView mCheck;

    private EditText mEngEditl;
    /**
     * 由于数据库数据格式 不统一，不能很好的匹配中文规则，
     * 并且输入中文意思的意思也不大，暂时弃用
     */
//    private EditText mChEditl;

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
    //每天默认记多少单词 ，就在数据库抽选 多少单词，然后在里面轮着记
    private static final int COUNT = 20;
    private int mCount = COUNT;
    private Word mWord;
    private List<Word> mWords;
    private String[] meanings;
    private ProgressBar mProgress;

    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                Log.e("debug", "锁屏");
                MainActivity.this.finish();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!PreferenceUtil.readBoolean(this, "common", "isOpen")) {
            finish();
            return;
        }
        registerReceiver(receiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));
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
                mWords = (List<Word>) msg.obj;
                int orderIndex = getWordIndexByRandom(mWords.size());
                mWord = mWords.get(orderIndex);
                setView(mWord);
            }

        }
    };


    /**
     * 获取word的index（经过算法策略过后的）
     */
    private int getWordIndexByRandom(int count) {
        int index = readInt(MainActivity.this, "common", "index", 0);
        int oldCount = readInt(MainActivity.this, "common", "count", COUNT);
        Log.d("debug", "index:::" + index);
        //如果 count减小到0 说明这轮已经记完了 就要重新初始化为count
        if (oldCount == 0) {
            PreferenceUtil.write(MainActivity.this, "common", "count", COUNT);
            oldCount = COUNT;
        }
        //如果=0表示第一次进来  如果>=count表示轮完了一遍 重置
        if (index >= count || index == 0 || index >= oldCount) {
            index = 0;
            //重新打乱排序
            setWordRandom(oldCount);
            //设置index 为0
            PreferenceUtil.write(MainActivity.this, "common", "index", 0);
        }
        RealmResults<WordWithIndex> realmResults = getWordRandom();
        for (int i = 0; i < realmResults.size(); i++) {
            Log.d("debug", "iiii:" + realmResults.get(i));
        }
        //获取打乱顺序之后的 index
        int orderIndex = realmResults.get(index).getIndex();
        Log.d("debug", "orderindex:::" + orderIndex);
        //保存数据库
        Realm realm = Realm.getDefaultInstance();
        realm.beginTransaction();
        //设置此下标已用
        realmResults.get(index).setUsed(true);
        realm.copyToRealm(realmResults);
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
    private void setWordRandom(int count) {
        Integer[] order = getRandomId(count);
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
        realm.delete(WordWithIndex.class);
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
            Log.d("debug", "过滤" + filter(mean));
            String atlerfiter = StringUtil.filter(mWord.getMeaning().replace("<br>", ""));
            if (atlerfiter.contains("，")) {
                meanings = atlerfiter.split("，");
            } else {
                meanings = atlerfiter.split(",");
            }
            mExplain.setText(word.getMeaning().replace("<br>", ""));

            for (int i = 0; i < meanings.length; i++) {
                Log.d("debug", "过后数组:" + meanings[i] + "\t");
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
        String imgPath = PreferenceUtil.readString(MainActivity.this, "common", "imgPaht", "null");
        if (!imgPath.equals("null")) {
            Drawable dw = Drawable.createFromPath(imgPath);
            if (dw != null) {
                viewgroup.setBackground(dw);
            }
        } else {
            viewgroup.setBackgroundResource(imgs[new Random().nextInt(imgs.length)]);
        }
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
        int days = readInt(this, "common", "days", 0);
        int count = readInt(this, "common", "count", COUNT);
        mCount = count;
        //如果 日期不是今天 那么说明是不同天数
        if (!TimeUtil.getTodayDate().equals(PreferenceUtil.readString(this, "common", "date", "defaultTIme"))
                ) {
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
        //如果 小于 说明 之前已经删除realm数据库中的数据了，不够了。要重新请求

        for (int i = 0; i < result1.size(); i++) {
            Word word = result1.get(i);
            realm.beginTransaction();
            word.setFromRealm(true);
            realm.commitTransaction();
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
        mWords = result1;
        int index = getWordIndexByRandom(mWords.size());
        if (index >= result1.size()) {
            loadData();
            return;
        }
        mWord = result1.get(index);
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
                WordsDao dao = new WordsDao(MainActivity.this, db);
                List<Word> list = dao.getRandomWordList(COUNT);
                if (list.size() == 0 || list == null) {
                    return;
                }
                final Realm realm = Realm.getDefaultInstance();
                realm.beginTransaction();
                realm.delete(Word.class);
                for (int i = 0; i < list.size(); i++) {
                    Word word = list.get(i);
                    word.setFromRealm(false);
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
        mNeverShow = (TextView) viewgroup.findViewById(R.id.never_show);
        mProgress = (ProgressBar) viewgroup.findViewById(R.id.progress);
        mEngEditl = (EditText) viewgroup.findViewById(R.id.type);
//        mChEditl = (EditText) viewgroup.findViewById(R.id.type_ch);
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


        /**
         * 这里比较复杂，高能区域。
         * 首先我们的规则是 一次COUNT个，从15000个单词库里面抽选COUNT个
         * 然后我们有一个raelm数据库 存的是 乱序的COUNT个wordWithIndex
         * 这是为了 解决 随机排序 然后按着随机排序 出现单词，避免 多次随机到同一个单词的情况
         * 就像 听音乐一样，你随机听，但是还是不会在听完一遍之前出现 重复听到一个首的现象
         * 这里跟音乐随机很相似。就是 ，所有都要走完一遍之后，再重新乱序。
         *
         * 比如这种情况：我们现在有5个单词，我们选择 "不再出现" 之后，还有4个，
         * 我们的规则是，依旧你之后 继续 在4个单词里面按着随机的乱序继续循环出现，
         * 因为：你想要记5个单词，你不可能一个不再出现，然后其他4个就不记了，
         * 所以这里还要继续 其他4个的循环出现。
         *
         * 只有一种情况会重新 抓取新的数据，那就是 5个单词 都被你 点 "不再出现了"
         * 就会重新从15000个大数据库里面重新抓取5个新的单词数据
         *
         * 所以上述逻辑比较复杂，下面的代码也比较冗长复杂
         * 主要是要注意几个地方：
         * 1、每次有单词移除，首先要移除变量list里面的，（这里有坑，因为我们的list的数据
         * 来自于2个地方，一个是第一次的时候直接从15000大数据库里面拿来的list，另一个是从realm数据库里面拿来的
         * 所以，需要判断是从哪个地方来，如果是从realm那么操作数据 需要调用realm 的事务《个人感觉realm用起来很麻烦》
         * ）
         * 2、要把现在还有多少个单词的数量保存到sharepreference ，因为每次重新打开都要去抓取
         * 你第二次打开 还有4个，你依旧是4个而不是原来的COUNT 5个。
         * 3、注意初始化
         * 4、getWordIndexByRandom这个方法里面也是高能，很复杂
         *
         *
         *
         */
        mNeverShow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //换一个
                //移除 重新 找20个
                Log.d("debug", "mWord：" + mWord.isFromRealm());
                //从本地的大的数据库删除
                WordsDao wordsDao = new WordsDao(MainActivity.this, DbUtil.openDatabase(MainActivity.this));
                wordsDao.deleteById(mWord.getId());

                if (mWord.isFromRealm()) {
                    //如果是来自realm数据库的数据 那么就从realm数据库直接删除
                    Realm realm = Realm.getDefaultInstance();
                    realm.beginTransaction();
                    mWord.deleteFromRealm();
                    //删除了之后要重新请求 避免 已经删除了 还在用
                    RealmQuery<Word> query = realm.where(Word.class);
                    // Execute the query:
                    RealmResults<Word> result1 = query.findAll();
                    mWords = result1;
                    realm.commitTransaction();
                } else {
                    //如果不是来自数据库，比如第一进入从本地数据库加载，就直接list remove
                    mWords.remove(mWord);
                }
                mCount--;
                Log.d("debug", "mCount ::" + mCount);
                PreferenceUtil.write(MainActivity.this, "common", "count", mCount);
                //如果==0说明全部记完了 从本地数据库加载啊
                //设置 index 初始化为0
                PreferenceUtil.write(MainActivity.this, "common", "index", 0);
                if (mCount == 0) {
                    //重新导入 setrandomindex  提示用户
                    mCount = COUNT;
                    loadData();

                } else {//否则从 realm数据库加载

                    mWord = mWords.get(getWordIndexByRandom(mCount));
                    setView(mWord);
                }


            }
        });

        mEngConfirm.setVisibility(View.GONE);
        mChConfirm.setVisibility(View.GONE);
/**
 * 在代码里设置有效
 */
        mEngEditl.setImeOptions(EditorInfo.IME_ACTION_DONE);
//        mChEditl.setImeOptions(EditorInfo.IME_ACTION_DONE);

        mEngEditl.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mEngEditl.requestFocus();
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

                    //增加次数
                    int times = PreferenceUtil.readInt(MainActivity.this, "common", "times", 0);
                    times++;
                    PreferenceUtil.write(MainActivity.this, "common", "times", times);


                    hideSoftInput();
                    finish();
//                    mEngLinear.setVisibility(View.GONE);
//                    mChLinear.setVisibility(View.VISIBLE);
//                    mChEditl.requestFocus();
                }
                Log.d("debug", "onTextChanged");
            }

            @Override
            public void afterTextChanged(Editable s) {
                Log.d("debug", "afterTextChanged");
            }
        });
//        mChEditl.addTextChangedListener(new TextWatcher() {
//            @Override
//            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
//            }
//
//            @Override
//            public void onTextChanged(CharSequence s, int start, int before, int count) {
//
//                if (!TextUtils.isEmpty(mWord.getMeaning())
//                        &&
//                        StringUtil.isContainChinese(s.toString())
//                        &&
//                        isContain(s.toString())) {
//                    hideSoftInput();
//                    MainActivity.this.finish();
//                }
//            }
//
//            @Override
//            public void afterTextChanged(Editable s) {
//            }
//        });
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
//        mChEditl.setText("");
        mEngEditl.setText("");
        hideSoftInput();
    }

    private void hideSoftInput() {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(mEngEditl.getWindowToken(), 0);
//        inputMethodManager.hideSoftInputFromWindow(mChEditl.getWindowToken(), 0);
    }

    @Override
    protected void onDestroy() {
        setContentView(R.layout.transparent);
        if (wm != null) {
            wm.removeView(viewgroup);
        }
        unregisterReceiver(receiver);
        super.onDestroy();
    }
}
