package com.gjw.mina.util;

import java.util.Date;
import java.util.Random;

import org.apache.mina.core.buffer.IoBuffer;

import com.gjw.mina.msg.AbsMessage;

/**
 * @author gjw
 * @create 2017-11-21 13:53
 **/
public class DataUtil {
    private static Random random = new Random();
    /**
     * 封装消息
     * @return
     */
    public static IoBuffer getDatabuffer(String data)
    {
        AbsMessage msgHeads = new AbsMessage(data);
        //创建一个缓冲，缓冲大小为:消息头长度(8位)+消息体长度
        IoBuffer buffer = IoBuffer.allocate(8+msgHeads.getBodyLength());
        buffer.put(msgHeads.getStartFlage());
        buffer.putInt(msgHeads.getBodyLength());
        buffer.put(msgHeads.getBodyData());
        buffer.put(msgHeads.getEndFlage());
        //把消息体put进去
        buffer.flip();
        return buffer;
    }

    /**
     * 格式化mac（返回格式：EE:AA:BB:66:33:22）
     * @return
     */
    public static String formateMac(String mac)
    {
        mac = mac.replaceAll(":","").replaceAll("-","").trim();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(mac.substring(0,2));
        stringBuilder.append(":");
        stringBuilder.append(mac.substring(2,4));
        stringBuilder.append(":");
        stringBuilder.append(mac.substring(4,6));
        stringBuilder.append(":");
        stringBuilder.append(mac.substring(6,8));
        stringBuilder.append(":");
        stringBuilder.append(mac.substring(8,10));
        stringBuilder.append(":");
        stringBuilder.append(mac.substring(10,12));

        return stringBuilder.toString().toUpperCase();
    }

    /**
     * 生产全局消息id
     * @return
     */
    public static String getMsgId(){
        long time = new Date().getTime();
        int randomInt = random.nextInt(100000);
        return String.valueOf(time+randomInt);
    }
}