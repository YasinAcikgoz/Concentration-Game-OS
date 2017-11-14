package com.yacikgoz.concentration;

import android.content.Context;
import android.graphics.Bitmap;

/**
 *
 * Created by yacikgoz on 1.06.2017.
 */

public class Cell extends android.support.v7.widget.AppCompatButton {
    public Bitmap bitmap;
    public boolean status;
    public String url;

    public Cell(Context context) {
        super(context);
    }
    public void setCell(boolean status, Bitmap bitmap) {
       // setUrl(url);
        setBitmap(bitmap);
        setStatus(status);
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public boolean status() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return "url: " + getUrl() + " status " + status;
    }

    @Override
    public boolean equals(Object obj) {
        Cell cell = (Cell) obj;
       // System.out.println("myequals");
        return cell.getUrl().equals(this.getUrl());
    }
}
