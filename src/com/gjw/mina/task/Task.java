package com.gjw.mina.task;


import java.net.InetSocketAddress;

import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Task implements Runnable{
	private static final Logger logger = LoggerFactory.getLogger(Task.class);
	
	private boolean flag = true;//是否继续循环处理标识
	private final String mac;//所属的mac
	private final IoSession session;//所属的mac
	
	public Task(String mac,IoSession session) {
        this.mac = mac;
        this.session = session;
    }
	
	@Override
	public void run() {
		dosomeThing();
	}
	
	public void dosomeThing() {
		logger.info("开始任务运行***********************"+this.mac);
        while(true)
        {
        	if(!this.flag)
        	{
        		logger.info("start send msg queue*****"+this.mac+"***终止运行");
        		break;
        	}
            if(session==null || !session.isConnected())
            {
            	//如果链接不可用则停止发送任务
            	this.flag = false;
            	logger.info("发送任务停止（链接不可用）*****"+this.mac);
            	continue;
            }
            
            InetSocketAddress remoteAddress = (InetSocketAddress) session.getRemoteAddress();
            logger.info("当前链接可用*****"+this.mac+"******Doing#####@@@@@ "+remoteAddress);
        	
        	try {
				Thread.sleep(3*1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
        }
        logger.info("结束任务运行***********************"+this.mac);
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