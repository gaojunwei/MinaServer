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
        if (in.remaining() < 8)//是用来当拆包时候剩余长度小于8的时候的保护，不加容易出错
        {
            return false;
        }
        if (in.remaining() > 1) {
            //以便后继的reset操作能恢复position位置
            in.mark();
            ////前6字节是包头，一个int和一个short，我们先取一个int
            byte StartFlage1 = in.get();//包头1
            byte StartFlage2 = in.get();//包头2
            int bodyLength = in.getInt();//先获取包体数据长度值

            //比较消息长度和实际收到的长度是否相等，这里-2是因为我们的消息头有个short值还没取
            if (bodyLength > in.remaining() - 2) {
                //出现断包，则重置恢复position位置到操作前,进入下一轮, 接收新数据，以拼凑成完整数据
                in.reset();//重置恢复position位置到操作前
                return false;
            }
            //消息内容足够
            in.reset();//重置恢复position位置到操作前
            //取出完整的数据包（不包含包尾）
            int sumLen = 8 + bodyLength;
            byte[] packArr = new byte[sumLen];
            in.get(packArr, 0, sumLen);
            IoBuffer buffer = IoBuffer.allocate(sumLen);
            buffer.put(packArr);
            buffer.flip();
            //分别取出包的部分信息
            StartFlage1 = buffer.get();//包头1
            StartFlage2 = buffer.get();//包头2
            bodyLength = buffer.getInt();//先获取包体数据长度值

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
                        logger.info("接收到心跳包2:"+mac+","+remoteAddress);
                        return true;
                    }
                } catch (UnsupportedEncodingException e) {
                    logger.error(e.getMessage());
                }
            }
            in.reset();//重置恢复position位置到操作前
        }
        return false;//下一个处理器进行处理
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
        logger.info("响应心跳包:"+datas);
        return null;

    }
}