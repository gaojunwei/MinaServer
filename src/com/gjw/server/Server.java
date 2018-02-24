package com.gjw.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;

import org.apache.log4j.Logger;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.executor.ExecutorFilter;
import org.apache.mina.filter.keepalive.KeepAliveFilter;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

import com.gjw.mina.codec.InfoDecoder;
import com.gjw.mina.codec.InfoEncoder;
import com.gjw.mina.codec.MyCodecFactory;
import com.gjw.mina.filter.KeepAliveMessageFactoryImpl;
import com.gjw.mina.handler.HandlerEvent;
import com.gjw.mina.handler.ServerHandler;
import com.gjw.mina.task.PrintTask;

public class Server {
	private static Logger logger = Logger.getLogger(Server.class);
	
	private static int port = 8088;
    private static IoAcceptor acceptor = null;
    
    public static void main(String[] args) {
    	//监听传入连接的对象
        acceptor = new NioSocketAcceptor();
        //filter设置

        KeepAliveMessageFactoryImpl heartBeatFactory = new KeepAliveMessageFactoryImpl();
        //IdleStatus参数为BOTH_IDLE,及表明如果当前连接的读写通道都空闲的时候在指定的时间间隔getRequestInterval后发送出发Idle事件。
        KeepAliveFilter kaf = new KeepAliveFilter(heartBeatFactory,IdleStatus.BOTH_IDLE);
        kaf.setRequestInterval(10);
        //使用了 KeepAliveFilter之后，IoHandlerAdapter中的sessionIdle方法默认是不会再被调用的！ 所以必须加入这句话 sessionIdle才会被调用
        kaf.setForwardEvent(true);

        acceptor.getFilterChain().addLast("exceutor", new ExecutorFilter());
        //心跳
        acceptor.getFilterChain().addLast("heart", kaf);
        //这个过滤器将会日志记录所有信息，比如 session 的新建、接收到的消息、发送的消息、session 的关闭等
        acceptor.getFilterChain().addLast( "logger", new LoggingFilter());
        //自定义加解码器工厂
        MyCodecFactory myCodecFactory = new MyCodecFactory(new InfoDecoder(Charset.forName("utf-8")),new InfoEncoder(Charset.forName("utf-8")));
        acceptor.getFilterChain().addLast( "codec", new ProtocolCodecFilter(myCodecFactory));


        //处理器
        ServerHandler handler = new ServerHandler();
        acceptor.setHandler(handler);

        HandlerEvent.getInstance().setSessionMap(acceptor.getManagedSessions());
        new Thread(new PrintTask()).start();
        
        //acceptor.getSessionConfig().setIdleTime(IdleStatus.BOTH_IDLE, 30);//设置空闲时间s
        //设置服务器监听的端口
        try {
            acceptor.bind( new InetSocketAddress(port));
            logger.info("Socket start,listen port: "+port);
        } catch (IOException e) {
            logger.error("socket server error: "+e.getMessage());
        }
	}
}
