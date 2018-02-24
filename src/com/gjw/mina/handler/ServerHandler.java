package com.gjw.mina.handler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.Map;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerHandler extends IoHandlerAdapter{
    private static final Logger logger = LoggerFactory.getLogger(ServerHandler.class);

	@Override
	public void sessionCreated(IoSession session) throws Exception {
		InetSocketAddress remoteAddress = (InetSocketAddress) session.getRemoteAddress();
		logger.info("session created with IP: " + remoteAddress +", session ID:"+session.getId());
	}

	@Override
	public void sessionClosed(IoSession session) throws Exception {
		String mac = session.getAttribute(HandlerEvent.SESSION_MAC)==null?"":session.getAttribute(HandlerEvent.SESSION_MAC).toString();
		logger.info("sessionClosed: "+mac);
		HandlerEvent.getInstance().closSession(session);
	}

	@Override
	public void messageReceived(IoSession ioSession, Object message) {
		IoBuffer buf = (IoBuffer)message;
		logger.info("messageReceived:"+ioSession.getId());
		try {
			HandlerEvent.getInstance().handle(ioSession, buf);
		} catch (IOException | InterruptedException | SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void sessionIdle(IoSession session, IdleStatus status) {
		InetSocketAddress remoteAddress = (InetSocketAddress) session.getRemoteAddress();
		String mac = session.getAttribute(HandlerEvent.SESSION_MAC)==null?"":session.getAttribute(HandlerEvent.SESSION_MAC).toString();
		logger.info("session in idle-"+session.getIdleCount(status)+"-"+mac+","+remoteAddress +", session ID:"+session.getId());
		if(session.getIdleCount(status)>=HandlerEvent.IDLE_TIMES)
		{
			//空闲时间大于指定次数 则 自动关闭该链接
			logger.info("关闭链接 - 空闲次数大于："+HandlerEvent.IDLE_TIMES+"次，session-"+mac+","+remoteAddress);
			session.closeOnFlush();
		}
	}

	@Override
	public void exceptionCaught(IoSession session, Throwable cause) {
		String mac = session.getAttribute(HandlerEvent.SESSION_MAC)==null?"":session.getAttribute(HandlerEvent.SESSION_MAC).toString();
		logger.info("exceptionCaught: "+mac);
		session.closeOnFlush();
	}

	@Override
	public void inputClosed(IoSession session) throws Exception {
		String mac = session.getAttribute(HandlerEvent.SESSION_MAC)==null?"":session.getAttribute(HandlerEvent.SESSION_MAC).toString();
		logger.info("inputClosed: "+mac);
		
		HandlerEvent.getInstance().closSession(session);
		super.inputClosed(session);
	}
}