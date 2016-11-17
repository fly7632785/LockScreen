package com.jafir.lockscreen.bean;

import io.realm.RealmObject;

/**
 * Created by jafir on 16/11/16.
 */

public class Word extends RealmObject {


    private String word;
    private String meaning;
    private String exmple;

    @Override
    public String toString() {
        return "Word{" +
                "word='" + word + '\'' +
                ", meaning='" + meaning + '\'' +
                ", exmple='" + exmple + '\'' +
                '}';
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
