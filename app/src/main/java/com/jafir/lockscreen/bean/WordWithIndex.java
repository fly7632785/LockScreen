package com.jafir.lockscreen.bean;

import io.realm.RealmObject;

/**
 * Created by jafir on 16/11/17.
 */

public class WordWithIndex extends RealmObject{
    private int index;
    private boolean isUsed = false;

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }


    public boolean isUsed() {
        return isUsed;
    }

    public void setUsed(boolean used) {
        isUsed = used;
    }
}
