package com.gjw.mina.task;


import java.net.InetSocketAddress;

import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Task implements Runnable{
	private static final Logger logger = LoggerFactory.getLogger(Task.class);
	
	private boolean flag = true;//�Ƿ����ѭ�������ʶ
	private final String mac;//������mac
	private final IoSession session;//������mac
	
	public Task(String mac,IoSession session) {
        this.mac = mac;
        this.session = session;
    }
	
	@Override
	public void run() {
		dosomeThing();
	}
	
	public void dosomeThing() {
		logger.info("��ʼ��������***********************"+this.mac);
        while(true)
        {
        	if(!this.flag)
        	{
        		logger.info("start send msg queue*****"+this.mac+"***��ֹ����");
        		break;
        	}
            if(session==null || !session.isConnected())
            {
            	//������Ӳ�������ֹͣ��������
            	this.flag = false;
            	logger.info("��������ֹͣ�����Ӳ����ã�*****"+this.mac);
            	continue;
            }
            
            InetSocketAddress remoteAddress = (InetSocketAddress) session.getRemoteAddress();
            logger.info("��ǰ���ӿ���*****"+this.mac+"******Doing#####@@@@@ "+remoteAddress);
        	
        	try {
				Thread.sleep(3*1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
        }
        logger.info("������������***********************"+this.mac);
	}
	
	public boolean isFlag() {
		return flag;
	}

	public void setFlag(boolean flag) {
		this.flag = flag;
	}

	public String getMac() {
		return mac;
	}
}