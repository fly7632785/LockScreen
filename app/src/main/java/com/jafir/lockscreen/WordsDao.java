package com.jafir.lockscreen;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.util.Log;

import com.jafir.lockscreen.bean.Word;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by jafir on 16/11/16.
 */

public class WordsDao {
    private WordsDbHelper helper;
    private SQLiteDatabase db = null;

    public WordsDao(Context mContext, SQLiteDatabase db) {
        helper = new WordsDbHelper(mContext);
        this.db = db;
    }

    public WordsDao(SQLiteDatabase db) {
        this.db = db;
    }

    public List<Word> getWordList() {
        List<Word> proList = new ArrayList<Word>();
        int i = 0;
        try {
            Cursor cursor = db.rawQuery("select * from cetsix", null);
            if (null != cursor) {
                // 按顺序 选词
                while (cursor.moveToNext()) {
                    Word bean = getWord(cursor);
                    proList.add(bean);
                    i++;
                }
            }
            Log.d("text", "工 ： " + i);
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != db) {
                db.close();
            }
        }
        return proList;
    }

    /**
     * @param n 一次性随机选出多少个
     * @return
     */
    public List<Word> getRandomWordList(int n) {
        List<Word> proList = new ArrayList<Word>();
        try {
            Cursor cursor = db.rawQuery("select * from cetsix", null);
            if (null != cursor) {
                /**
                 * 随机选词
                 *
                 */
                Random random = new Random();
                for (int j = 0; j < n; j++) {
//                  有多跳数据 随机取出index
                    int index = random.nextInt(cursor.getCount());
                    if (cursor.moveToPosition(index)) {
                        Word bean = getWord(cursor);
                        proList.add(bean);
                    }
                }
            }
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != db) {
                db.close();
            }
        }
        return proList;
    }

    @NonNull
    private Word getWord(Cursor cursor) {
        int id = cursor.getInt(cursor.getColumnIndex("ID"));
        String word = cursor.getString(cursor.getColumnIndex("words"));
        String meaning = cursor.getString(cursor.getColumnIndex("meaning"));
        String exmple = cursor.getString(cursor.getColumnIndex("lx"));
        Word bean = new Word();
        bean.setId(String.valueOf(id));
        bean.setWord(word);
        bean.setExmple(exmple);
        bean.setMeaning(meaning);
        return bean;
    }


    /**
     * 删除
     * <p>
     * 在选择 不再出现之后 就会从数据库删除
     *
     * @param id
     */
    public void deleteById(String id) {
        try {
            db.beginTransaction();
            db.delete("cetsix", "ID=?", new String[]{id});
            db.endTransaction();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
