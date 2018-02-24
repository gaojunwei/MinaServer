package com.gjw.mina.codec;

import org.apache.mina.filter.codec.demux.DemuxingProtocolCodecFactory;
import org.apache.mina.filter.codec.demux.MessageDecoder;
import org.apache.mina.filter.codec.demux.MessageEncoder;

import com.gjw.mina.msg.AbsMessage;

/**
 * 自定义编码工厂类
 */
public class MyCodecFactory extends DemuxingProtocolCodecFactory {

	private MessageDecoder decoder;
	private MessageEncoder<AbsMessage> encoder;

	public MyCodecFactory(MessageDecoder decoder,
			MessageEncoder<AbsMessage> encoder) {
		this.decoder = decoder;
		this.encoder = encoder;
		addMessageDecoder(this.decoder);
		addMessageEncoder(AbsMessage.class, this.encoder);
	}
}