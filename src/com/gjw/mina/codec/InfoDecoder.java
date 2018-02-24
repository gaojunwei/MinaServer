package com.gjw.mina.codec;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.demux.MessageDecoder;
import org.apache.mina.filter.codec.demux.MessageDecoderResult;

import java.nio.charset.Charset;

public class InfoDecoder implements MessageDecoder {
    private Charset charset;

    public InfoDecoder(Charset charset) {
        this.charset = charset;
    }

    protected boolean doDecode(IoSession session, IoBuffer in, ProtocolDecoderOutput out) throws Exception {
        if (in.remaining() < 8)//是用来当拆包时候剩余长度小于8的时候的保护，不加容易出错
        {
            return false;
        }
        if (in.remaining() > 1) {
            //以便后继的reset操作能恢复position位置
            in.mark();
            ////前6字节是包头，一个int和一个short，我们先取一个int
            int len = in.getInt();//先获取包体数据长度值

            //比较消息长度和实际收到的长度是否相等，这里-2是因为我们的消息头有个short值还没取
            if (len > in.remaining() - 2) {
                //出现断包，则重置恢复position位置到操作前,进入下一轮, 接收新数据，以拼凑成完整数据
                in.reset();
                return false;
            } else {
                //消息内容足够
                in.reset();//重置恢复position位置到操作前
                int sumLen = 6 + len;//总长 = 包头+包体
                byte[] packArr = new byte[sumLen];
                in.get(packArr, 0, sumLen);
                IoBuffer buffer = IoBuffer.allocate(sumLen);
                buffer.put(packArr);
                buffer.flip();
                out.write(buffer);
                //走到这里会调用DefaultHandler的messageReceived方法
                if (in.remaining() > 0) {//出现粘包，就让父类再调用一次，进行下一次解析
                    return true;
                }
            }
        }
        return false;//处理成功，让父类进行接收下个包

    }

    @Override
    public MessageDecoderResult decodable(IoSession session, IoBuffer in) {
        if (in.remaining() < 8)//是用来当拆包时候剩余长度小于8的时候的保护，不加容易出错
        {
            return MessageDecoderResult.NEED_DATA;
        }
        byte StartFlage1 = in.get();
        byte StartFlage2 = in.get();
        int bodyLength=in.getInt();

        if(StartFlage1==(byte) 0xaa && StartFlage2==(byte) 0xaa && in.remaining()>=bodyLength+2)
        {
            return MessageDecoderResult.OK;
        }
        return MessageDecoderResult.NEED_DATA;//继续粘包
    }

    @Override
    public MessageDecoderResult decode(IoSession session, IoBuffer in,
                                       ProtocolDecoderOutput out) throws Exception {
        //以便后继的reset操作能恢复position位置
        in.mark();

        byte StartFlage1 = in.get();
        byte StartFlage2 = in.get();
        int bodyLength=in.getInt();

        in.reset();//重置恢复position位置到操作前

        int dlength = bodyLength+8;
        byte[] packArr = new byte[dlength];

        in.get(packArr, 0, dlength);

        IoBuffer buffer = IoBuffer.allocate(dlength);
        buffer.put(packArr);
        buffer.flip();
        out.write(buffer);

        return MessageDecoderResult.OK;
    }

    @Override
    public void finishDecode(IoSession session, ProtocolDecoderOutput out)
            throws Exception {
        // TODO 自动生成的方法存根
    }
}