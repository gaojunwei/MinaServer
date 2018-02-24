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
     * ��װ��Ϣ
     * @return
     */
    public static IoBuffer getDatabuffer(String data)
    {
        AbsMessage msgHeads = new AbsMessage(data);
        //����һ�����壬�����СΪ:��Ϣͷ����(8λ)+��Ϣ�峤��
        IoBuffer buffer = IoBuffer.allocate(8+msgHeads.getBodyLength());
        buffer.put(msgHeads.getStartFlage());
        buffer.putInt(msgHeads.getBodyLength());
        buffer.put(msgHeads.getBodyData());
        buffer.put(msgHeads.getEndFlage());
        //����Ϣ��put��ȥ
        buffer.flip();
        return buffer;
    }

    /**
     * ��ʽ��mac�����ظ�ʽ��EE:AA:BB:66:33:22��
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
     * ����ȫ����Ϣid
     * @return
     */
    public static String getMsgId(){
        long time = new Date().getTime();
        int randomInt = random.nextInt(100000);
        return String.valueOf(time+randomInt);
    }
}