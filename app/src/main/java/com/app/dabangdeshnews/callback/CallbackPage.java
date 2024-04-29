package com.app.dabangdeshnews.callback;

import com.app.dabangdeshnews.model.Post;

import java.util.ArrayList;
import java.util.List;

public class CallbackPage {

    public String kind;
    public List<Post> items = new ArrayList<>();
    public String nextPageToken;
    public String etag;

}
