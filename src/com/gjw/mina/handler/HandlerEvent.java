package com.gjw.mina.handler;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
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
                String data = sendChangeImg();
                logger.info("发送指令消息："+data);
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
	
	public String sendChangeImg()
	{
		Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("order", "change_img");
        dataMap.put("operate_id", "CHANGE_IMG_Id");
        dataMap.put("msg_id", DataUtil.getMsgId());
        List<Map<String, Object>> list = new ArrayList<>();

        String base64Str = "////f////////////////////3////////////////////9/////////////////////f////////////////////3////////////////////9/////////////////////f////////////////////3////////////////////9///////////+B/fv/+YAff7//////////u/37//mAH3+Pmf//////4Lv8B/////9/w5n//////+67/fb/////f/GZ///////ugf31/////3/4Gf//////7v/+8/////9//gA//////+6B8A//+AAff/8AP//////uu/7///gAH3/8Gf//////4Lv+/v/79/9/8Jn///////+7/v7/+/f/f8OZ////////gf4B//v3/3+Pmf/////////////55/9/v//////////9/f///A//f//////////wA/kA//4f/3///////////t/1V/////9//////////+DB/Vb/////f//////////+9/kA/////3/////D////+Cf////4AB9/4f//g/////KX8MH/+AAff+P//wP////qN/16///f/3/H//4D////+KH7dv//v/9/x//8I/////q3927//7//f4//+GP////4N/54//+f/3+P//Bj////////////gB9/j//g4////////////8Aff4//weP//////////////3+P/4Pj/////mf/M/////9/j/8H4/////5n/zP///D/f4/+D+P////////////AP3+H/B/j////////////zz9/w/B/4////////////7/ff8AA/+P///////////+/33/gAf/j////////////v99/8Af/4////////////7/ff/4f//////////////+fv3//////////////////wD9////////////3/////+D/f///8f//////B/7v/////3///8H/////+N/+7v////9///+B//////4f8AD//4Aff//+Af//////x/7f//+AH3///BH///////f/////3/9///Ax//////998MD//7//f//g8f//////Af1V//+//3//wfH//////3X9Vf//n/9//wfx//////93+1X//4Aff/4P8f//////j/tV///AH3/4P/H////////2QP////9/8H/x//////99////////f8H/8f//////Af5t///wf3+H//H//////3X9Tf//wD9/gAAAA/////938yv//9sff4AAAAP/////j/7r//+733+AAAAD///////9/v//u99////x//////v3/D3//7vff///8f/////79/XL//+b33////H/////+Af59///w79////x///////3/cv//+P/f///////////9/w9/////3/////////////9/v//gP9//////////////////gA/f/4D////////j/9///zvH3/4AP/D/////1f3Af/5399/4AB/4/////9X+/v/+9/ff+AAP+P/////l//+//vf33/D/B/j///////4Hf/7z59/h/4f4///////+9v//eA/f4//H+P///////vf///wf3+P/x/j///////73/////9/j/8f4///////+9v/////f4//H+P///////gd/////3+P/x/D/////////v////9/h/8fx/////fv////////f4f+Pwf////27//e/////3/B/H4P////9q/wXf//7/9/4ADgH////+LP91P//8/vf/AAAD/////27/cO///d73/4AAD/////9gH3Xv//vW9//wAH//////bv917//52vf//////////iz/BB//9dz3//////////9q/////+3e9///////////bv/Af//N3vf//////////37//+//nd73/+A//////////wAP/zwA9/+AD/w//////b////893vf+AAf+P////wW/////3d73/gAD/j////9Vf////+3c9/w/wf4/////VP/////12vf4f+H+P////1X/////+9b3+P/x/j////8AH/////3e9/j/8f4/////Vf/////8/vf4//H+P////1T/9+///v/3+P/x/j////9Vf/dv//v/9/j/8fw/////Bb/rb//z//f4f/H8f/////2/22//4AH3+H/j8H///////7tv/8ve9/wfx+D///////94D/+b3vf+AA4B////////u2//O973/wAAA////////9tv/jve9/+AAA/////////rb/+73vf/8AB/////////92//uB73//////////////fv/6Pe9/////////////////+D3vf/////////////////m973/////////////wAP//ve9/////5///////9vf//73vf////8P///////dv//+A73/////D///////3n////+9/////w///////92////+ff////8P///////b3/////3/////////////3b/////9/////////////95////+/f/////////////du//3v33///wH////////29v/7799///AAH///////8AD/++/ff//gAA////////////vv33//wf+H////////3//5199//4//4////////y///Be/f/+f//P///////7v//wwP3//n//z///////8A///+H9//x//8////////+v/////f/+f//P//////////////3//j//j////////D////99//4f/x////////vf//v+ff//AAAf///////37//3/H3//4AAf///////92//7/l9///gAf////////Mf/+/zff//////////////f//vx33/////////////////5499///8B/////////////Afff//wAB////////////4f33//4AAP//////////////9//8H/h///////////////f/+P/+P///////////+B/3//n//z////////////AH9//5//8////////////n4/f/8f//P///////////z/v3//n//z///////////5/99//4//4///////////+//ff/+H/8f///////////v333//wAAH///////////7999//+AAH///////////+/fff//4AH////////////vwH3/////////////////98D9/////////////////////f////////////////////3////////////////////9/////////////////+AAff/////////////////gAH3/////////////////7799/////////////////++/ff/////////////////vv33/////////////////5z99//////////////u///Beff/////////////7u//wwP3/////////////wAP//+H9//////////////t//////f/////////////7f/////3/////////////7/f////9/////////////+73/////f/////////////u9/////3/////////////7vf///f9/////////////+73///H/f/////////////gA///l/3///////////d3/////jf9///////////wr+9///z3/f//////////h9/sB//59/3///////////cv4u//4/f9///////////w9+4H/+AAff//////////3v/f3//gAH3//////////9730N////f9///////////YB97f///3/f//////////1v/e3/////3//////////87/3tv//gf9///////////+/8D7//wB/f/////////////+B//5+P3///////////v/////8/79//////////+7/+37/+f/ff//////////2A/t+//v/33////////////f6gv/7999///////////3f+ar/+/fff//////////2X/uq//v333//////////99/wKv/78B9///////////AB+6r//fA/f//////////33/mq/////3//////////91/6gv///f9///////////zf+37/+73/f/////////////t+//u9/3////////////3////7vf9///////////AD////+73vf//////////1//+3//u973//////////9d36r//7ve9///////////BN+oD/+73vf//////////1Vfqq//gAH3//////////5Vv6Kv/7vf9///////////Vb+Kr/+73/f//////////wVfaq//e9/3//////////9c32qv/3vf9///////////X99qr/973/f/////////////aA//e9/3//////////////v//3vf9///////////////////3/f/////////////9////9/3/////////////3Af/+/P9/////////////+/v//vH/f//////////////3//6P/3///////////H/9t//wAAd///////////un9bf//s//f//////////7t/m3//7v93//////////+7f8AP/+9+d///////////wP+bf///8ff/////////////W3//gA/3///////////H/9t//7//9///////////un////+///f//////////7t/4f//v//3//////////+7f////7//9///////////wP8AD/+AAPf/////////////3/////73///////////P/7v////+9///////////tn+oD////Pf//////////8n/qr///+H3///////////k/wK/////9///////////m3+qr/////f///////////z/qA/////3/////////////7v/////9/////////////////////f////////////////////3////////////////////9/////////////////////f////////////////////3////////////////////9/////////////////////f////////////////////3///////4AM85yYAP////9///////+ADPOcmAD/////f///////n+8cG/v8/////3///////5BvH+QbBP////9///////+Qbx/kGwT/////f///////kGwcgJsE/////3///////5BsHICbBP////9///////+Qb5//ewT/////f///////n+xg5Bv8/////3///////5/sYOQb/P////9///////+ADJNkmAD/////f/////////8A+5///////3//////////APuf//////9/////////jIMD5Jz/////f////////4yDA+Sc/////3///////+OTY+SDH/////9////////ggAD4Hxv/////f///////4IAA+B8b/////3////////OTjIRgAP////9///////+cgAzj5Jz/////f///////nIAM4+Sc/////3///////4Af/4RgnP////9///////+AH/+EYJz/////f///////7ANvh5sf/////3///////+z8bGN/f/////9////////s/Gxjf3//////f///////k2+THAN//////3///////5NvkxwDf/////9///////+A8/ADY5z/////f////////IOTZGDg/////3////////yDk2Rg4P////9///////+AE4OHh4P/////f///////jw9z4wAA/////3///////48Pc+MAAP////9//////////GAYH3j/////f/////////xgGB94/////3///////4APA3ubHP////9///////+f7Oz4nxj/////f///////n+zs+J8Y/////3///////5Bv7xyABP////9///////+QbwOE/+f/////f///////kG8DhP/n/////3///////5BvjH+En/////9///////+Qb4x/hJ//////f///////n++DBGD4/////3///////4APc3hkB/////9///////+AD3N4ZAf/////f////////////////////3////////////////////9/////////////////////f////////////////////3////////////////////9/////////////////////f////////////////////3////////////////8=";

        Map<String, Object> map = new HashMap<>();
        map.put("operate_id", "CHANGE_IMG_Id_001");
        map.put("mac", "E1:00:11:00:00:F3");
        map.put("imgbase64", base64Str);
        map.put("change_time", DataUtil.simpleDateFormat.format(new Date()));
        map.put("change_type", "update_img");
        map.put("img_index", "00");
        
        Map<String, Object> map2 = new HashMap<>();
        map2.put("operate_id", "CHANGE_IMG_Id_002");
        map2.put("mac", "80:EA:CA:DE:AD:99");
        map2.put("imgbase64", base64Str);
        map2.put("change_time", DataUtil.simpleDateFormat.format(new Date()));
        map2.put("change_type", "update_img");
        map2.put("img_index", "00");
        
        //list.add(map2);
        list.add(map);

        dataMap.put("list", list);
        return JSON.toJSONString(dataMap);
	}
	
	public String sendCallOver()
	{
		Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("order", "call-over");
        dataMap.put("msg_id", DataUtil.getMsgId());
        dataMap.put("operate_id", "1");
        dataMap.put("server_time", "2018-02-08 17:01:02");
        dataMap.put("epl_num", 10);
        
        String msgId = DataUtil.getMsgId();
        dataMap.put("msg_id", msgId);
        
        return JSON.toJSONString(dataMap);
	}
	
}
