package com.sonant.paymentcollection.pojo;

public class VoiceData {
    private String v_msg;
    private String v_msg2;
    private String v_type;
    private char  v_type2;
    private int size = 0;

    public int getSize() {
        return size;
    }

    public void addsize(int pos){
        this.size = pos;
    }
    public void removeSize(){
        this.size -= 1;
    }

    public VoiceData()  {

    }

    public VoiceData(String v_msg, String v_type)
    {
        this.v_msg = v_msg;
        this.v_type = v_type;
    }
    public VoiceData(String v_msg, char v_type)
    {
        this.v_msg2 = v_msg;
        this.v_type2 = v_type;
    }
    public String getMSG(){
        return v_msg;
    }
    public String getMSG2(){
        return v_msg2;
    }
    public String getType(){
        return v_type;
    }
    public String getType2(){
        return String.valueOf(v_type2);
    }
    public void setMSG(String v_msg){
         this.v_msg = v_msg;
    }

    public void setType(String v_type){
        this.v_type = v_type;
    }
    public void setType2(char v_type){
        this.v_type2 = v_type;
    }

}
