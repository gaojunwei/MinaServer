package com.gjw.mina.msg;

import java.nio.charset.Charset;

public class AbsMessage {
	/**
	 * ��ϢЭ���ʽ����ͷ:2 byte,����:4 byte,Json������:n byte,��β:2 byte;
	 */
	private byte[] startFlage = new byte[2];//��Ϣͷ2���ֽ�
	private int bodyLength = 0;//����ĳ���4���ֽ�
	byte[] bodyData = null;//��Ϣ������

	private byte[] endFlage = new byte[2];//��Ϣ��β2���ֽ�

	public AbsMessage(String data) {
		//��ͷ
		this.startFlage[0] = (byte) 0xaa;
		this.startFlage[1] = (byte) 0xaa;
		//��Ϣ������
		this.bodyData = data.getBytes(Charset.forName("utf-8"));
		//��Ϣ�峤��
		this.bodyLength = this.bodyData.length;
		//��β
		this.endFlage[0] = (byte) 0x0d;//'\r'
		this.endFlage[1] = (byte) 0x0a;//'\n'

	}

	public byte[] getStartFlage() {
		return startFlage;
	}

	public int getBodyLength() {
		return bodyLength;
	}

	public byte[] getBodyData() {
		return bodyData;
	}

	public byte[] getEndFlage() {
		return endFlage;
	}
}
