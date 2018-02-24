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
    	//�����������ӵĶ���
        acceptor = new NioSocketAcceptor();
        //filter����

        KeepAliveMessageFactoryImpl heartBeatFactory = new KeepAliveMessageFactoryImpl();
        //IdleStatus����ΪBOTH_IDLE,�����������ǰ���ӵĶ�дͨ�������е�ʱ����ָ����ʱ����getRequestInterval���ͳ���Idle�¼���
        KeepAliveFilter kaf = new KeepAliveFilter(heartBeatFactory,IdleStatus.BOTH_IDLE);
        kaf.setRequestInterval(10);
        //ʹ���� KeepAliveFilter֮��IoHandlerAdapter�е�sessionIdle����Ĭ���ǲ����ٱ����õģ� ���Ա��������仰 sessionIdle�Żᱻ����
        kaf.setForwardEvent(true);

        acceptor.getFilterChain().addLast("exceutor", new ExecutorFilter());
        //����
        acceptor.getFilterChain().addLast("heart", kaf);
        //���������������־��¼������Ϣ������ session ���½������յ�����Ϣ�����͵���Ϣ��session �Ĺرյ�
        acceptor.getFilterChain().addLast( "logger", new LoggingFilter());
        //�Զ���ӽ���������
        MyCodecFactory myCodecFactory = new MyCodecFactory(new InfoDecoder(Charset.forName("utf-8")),new InfoEncoder(Charset.forName("utf-8")));
        acceptor.getFilterChain().addLast( "codec", new ProtocolCodecFilter(myCodecFactory));


        //������
        ServerHandler handler = new ServerHandler();
        acceptor.setHandler(handler);

        HandlerEvent.getInstance().setSessionMap(acceptor.getManagedSessions());
        new Thread(new PrintTask()).start();
        
        //acceptor.getSessionConfig().setIdleTime(IdleStatus.BOTH_IDLE, 30);//���ÿ���ʱ��s
        //���÷����������Ķ˿�
        try {
            acceptor.bind( new InetSocketAddress(port));
            logger.info("Socket start,listen port: "+port);
        } catch (IOException e) {
            logger.error("socket server error: "+e.getMessage());
        }
	}
}
