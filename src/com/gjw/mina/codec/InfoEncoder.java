package com.gjw.mina.codec;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.apache.mina.filter.codec.demux.MessageEncoder;

import com.gjw.mina.msg.AbsMessage;

import java.nio.charset.Charset;

/**
 * 自定义编码器
 */
public class InfoEncoder implements MessageEncoder<AbsMessage> {
	private Charset charset;

	public InfoEncoder(Charset charset) {
		this.charset = charset;
	}

	@Override
	public void encode(IoSession session, AbsMessage message, ProtocolEncoderOutput out)
			throws Exception {
		AbsMessage req = (AbsMessage) message;
		IoBuffer buf=IoBuffer.allocate(8+req.getBodyLength());
		buf.put(req.getStartFlage());//
		buf.putInt(req.getBodyLength());
		buf.put(req.getBodyData());//消息体
		buf.put(req.getEndFlage());

		buf.flip();
		out.write(buf);
	}

}