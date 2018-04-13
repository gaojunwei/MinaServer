package com.gjw.mina.filter;

import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.keepalive.KeepAliveMessageFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.gjw.mina.util.DataUtil;

public class KeepAliveMessageFactoryImpl implements KeepAliveMessageFactory {
    private static final Logger logger = LoggerFactory.getLogger(KeepAliveMessageFactoryImpl.class);
    @Override
    public boolean isRequest(IoSession session, Object message) {
        IoBuffer in = (IoBuffer)message;
        if (in.remaining() < 8)//�����������ʱ��ʣ�೤��С��8��ʱ��ı������������׳���
        {
            return false;
        }
        if (in.remaining() > 1) {
            //�Ա��̵�reset�����ָܻ�positionλ��
            in.mark();
            ////ǰ6�ֽ��ǰ�ͷ��һ��int��һ��short��������ȡһ��int
            byte StartFlage1 = in.get();//��ͷ1
            byte StartFlage2 = in.get();//��ͷ2
            int bodyLength = in.getInt();//�Ȼ�ȡ�������ݳ���ֵ

            //�Ƚ���Ϣ���Ⱥ�ʵ���յ��ĳ����Ƿ���ȣ�����-2����Ϊ���ǵ���Ϣͷ�и�shortֵ��ûȡ
            if (bodyLength > in.remaining() - 2) {
                //���ֶϰ��������ûָ�positionλ�õ�����ǰ,������һ��, ���������ݣ���ƴ�ճ���������
                in.reset();//���ûָ�positionλ�õ�����ǰ
                return false;
            }
            //��Ϣ�����㹻
            in.reset();//���ûָ�positionλ�õ�����ǰ
            //ȡ�����������ݰ�����������β��
            int sumLen = 8 + bodyLength;
            byte[] packArr = new byte[sumLen];
            in.get(packArr, 0, sumLen);
            IoBuffer buffer = IoBuffer.allocate(sumLen);
            buffer.put(packArr);
            buffer.flip();
            //�ֱ�ȡ�����Ĳ�����Ϣ
            StartFlage1 = buffer.get();//��ͷ1
            StartFlage2 = buffer.get();//��ͷ2
            bodyLength = buffer.getInt();//�Ȼ�ȡ�������ݳ���ֵ

            if(StartFlage1==(byte) 0xaa && StartFlage2==(byte) 0xaa && buffer.remaining()>=bodyLength+2)
            {
                byte[] bytes = new byte[bodyLength];
                buffer.get(bytes);
                String json;
                try {
                    json = new String(bytes,"utf-8");
                    Map<String,Object> map = (Map<String,Object>) JSON.parse(json);
                    String ht = map.get("order")==null?"":map.get("order").toString();
                    if(ht.equals("h_t"))
                    {
                        String mac = session.getAttribute("mac")==null?"":session.getAttribute("mac").toString();
                        InetSocketAddress remoteAddress = (InetSocketAddress) session.getRemoteAddress();
                        logger.info("���յ�������2:"+mac+","+remoteAddress);
                        return true;
                    }
                } catch (UnsupportedEncodingException e) {
                    logger.error(e.getMessage());
                }
            }
            in.reset();//���ûָ�positionλ�õ�����ǰ
        }
        return false;//��һ�����������д���
    }

    @Override
    public boolean isResponse(IoSession session, Object message) {
        return false;
    }

    @Override
    public Object getRequest(IoSession session) {
        return null;
    }

    @Override
    public Object getResponse(IoSession session, Object request) {
        Map<String,Object> maps = new HashMap<String, Object>();
        maps.put("order","h_t");
        String datas = JSON.toJSONString(maps);
        IoBuffer buffers = DataUtil.getDatabuffer(datas);
        session.write(buffers);
        logger.info("��Ӧ������:"+datas);
        return null;

    }
}