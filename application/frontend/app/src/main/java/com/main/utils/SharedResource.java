package com.main.utils;

import org.json.JSONObject;

public class SharedResource {

    JSONObject response=null;
    public synchronized JSONObject getResponse(){
        return response;
    }
    public synchronized  boolean setResponse(JSONObject obj){
        this.response=obj;
        return true;
    }
}
