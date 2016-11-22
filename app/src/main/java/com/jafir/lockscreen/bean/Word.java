package com.jafir.lockscreen.bean;

import io.realm.RealmObject;

/**
 * Created by jafir on 16/11/16.
 */

public class Word extends RealmObject {


    private String id;
    private String word;
    private String meaning;
    private String exmple;
    private boolean isFromRealm;

    @Override
    public String toString() {
        return "Word{" +
                "id='" + id + '\'' +
                ", word='" + word + '\'' +
                ", meaning='" + meaning + '\'' +
                ", exmple='" + exmple + '\'' +
                ", isFromRealm=" + isFromRealm +
                '}';
    }

    public boolean isFromRealm() {
        return isFromRealm;
    }

    public void setFromRealm(boolean fromRealm) {
        isFromRealm = fromRealm;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public String getMeaning() {
        return meaning;
    }

    public void setMeaning(String meaning) {
        this.meaning = meaning;
    }

    public String getExmple() {
        return exmple;
    }

    public void setExmple(String exmple) {
        this.exmple = exmple;
    }
}
