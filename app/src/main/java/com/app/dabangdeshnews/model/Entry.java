package com.app.dabangdeshnews.model;

import com.app.dabangdeshnews.model.entries.Link;
import com.app.dabangdeshnews.model.entries.Object;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Entry implements Serializable {

    public Object id = null;
    public Object published = null;
    public Object updated = null;
    public List<Category> category = new ArrayList<>();
    public Object title = null;
    public Object content = null;
    public List<Link> link = new ArrayList<>();

}
