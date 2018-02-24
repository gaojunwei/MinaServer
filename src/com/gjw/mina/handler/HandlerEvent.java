package com.gjw.mina.handler;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.gjw.mina.msg.AbsMessage;
import com.gjw.mina.task.Task;
import com.gjw.mina.util.DataUtil;

public class HandlerEvent {
	private static final Logger logger = LoggerFactory.getLogger(HandlerEvent.class);
	public static final String SESSION_TASK = "task";
	public static final String SESSION_MAC = "mac";
	public static final String CHARSET = "utf-8";
	public static final int IDLE_TIMES = 3;
	
	public static HandlerEvent handlerEvent = new HandlerEvent();
	private Map<Long, IoSession> sessionMap;
	
	public static HandlerEvent getInstance()
	{
		return handlerEvent;
	}
	
	public void handle(IoSession iosession, IoBuffer buf) throws IOException, InterruptedException, UnsupportedEncodingException, SQLException {
		byte StartFlage1 = buf.get();//获得包头1
		byte StartFlage2 = buf.get();//获得包头2
		int bodyLength=buf.getInt();//获得消息体长度

		byte[] bytes = new byte[bodyLength];
		buf.get(bytes);

		String json = new String(bytes,CHARSET);
        InetSocketAddress remoteAddress = (InetSocketAddress) iosession.getRemoteAddress();
        String mac = iosession.getAttribute(SESSION_MAC)==null?"":iosession.getAttribute(SESSION_MAC).toString();
        logger.info("*******服务端接收到客户端消息:("+mac+"-"+remoteAddress+"):"+json);

        Map<String,Object> rMap = (Map<String,Object>)JSON.parse(json);
        String order = rMap.get("order").toString();
        logger.info("order equals:"+order);

        switch (order) {
            case "report_mac"://设备端建立连接上报MAC地址
                mac = rMap.get("mac").toString().toUpperCase();
                //TODO 检查链接池中是否已有链接绑定此Mac地址
                logger.info("receive client report mac:"+mac);
				iosession.setAttribute(SESSION_MAC,mac);
                //获取对应mac的消息队列
				Task task = (Task)iosession.getAttribute(SESSION_TASK);
				if(task==null)
				{
					synchronized (this)
                    {
                        if(task==null)
                        {
                        	task = new Task(mac,iosession);
                        	iosession.setAttribute(SESSION_TASK,task);
                        	new Thread(task).start();
                            logger.info("创建消息任务队列 并 启动运行:"+mac);
                        }
                    }
				}else
				{
					if(!task.isFlag())
					{
						task.setFlag(true);
						new Thread(task).start();
						logger.info("消息任务队列重新启动:"+mac);
					}
				}
				
				Map<String, Object> dataMap = new HashMap<>();
                dataMap.put("order", "call-over");
                dataMap.put("operate_id", "1");
                dataMap.put("server_time", "2018-02-08 17:01:02");
                dataMap.put("epl_num", 10);
                
                String msgId = DataUtil.getMsgId();
                dataMap.put("msg_id", msgId);
                
                String data = JSON.toJSONString(dataMap);
                IoBuffer ioBuffer = DataUtil.getDatabuffer(data);
                iosession.write(ioBuffer);
				
                break;
            default:
                logger.info("[unkonwn order]"+order);
                break;
        }
	}
	
	/**
	 * 关闭链接处理
	 * @param iosession
	 */
	public void closSession(IoSession iosession)
	{
		if(iosession==null){return;}
		//iosession.closeOnFlush();
		Task task = (Task)iosession.getAttribute(SESSION_TASK);
		String mac = iosession.getAttribute(SESSION_MAC)==null?"": iosession.getAttribute(SESSION_MAC).toString();
		if(task!=null)
		{
			task.setFlag(false);
			logger.info("关闭对应的线程任务  mac:"+mac);
		}
		logger.info("关闭链接  mac:"+mac);
	}
	
	/**
	 * 打印当前链接情况
	 */
	public void printMap() {
		Map<Long, IoSession> sessionMap = HandlerEvent.getInstance().getSessionMap();
		if(sessionMap.size()==0)
		{
			System.out.println("暂无连接");
			return;
		}
		StringBuilder stringBuilder = new StringBuilder("\r\n");
		for(long sessionId:sessionMap.keySet())
		{
			IoSession session = sessionMap.get(sessionId);
			InetSocketAddress remoteAddress = (InetSocketAddress) session.getRemoteAddress();
			stringBuilder.append("[链接对象ID："+sessionId+"，链接对象地址："+remoteAddress+"]").append("\r\n");
		}
		logger.info(stringBuilder.toString());
	}
	
	public Map<Long, IoSession> getSessionMap() {
		return sessionMap;
	}

	public void setSessionMap(Map<Long, IoSession> sessionMap) {
		this.sessionMap = sessionMap;
	}
	
}
