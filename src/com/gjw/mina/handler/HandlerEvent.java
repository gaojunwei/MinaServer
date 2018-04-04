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
import com.gjw.mina.util.EncryptionUtils;

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
				
				//String[] eplMac = {"80:EA:CA:FF:C0:62","80:EA:CA:88:AE:F5"};
				//String[] eplMac = {"80:EA:CA:89:AE:E7"};
				//String[] eplMac = {"80:EA:CA:88:AE:F5"};
				//String[] eplMac = {"80:EA:CA:88:AF:43"};
				String[] eplMac = {"80:ea:ca:88:ae:be"};
				//String[] eplMac = {"80:EA:CA:FF:C0:62"};
				for (int i = 0; i < eplMac.length; i++) {
					//String data = sendChangeImgDATA(eplMac[i]);
					String data = sendChangeImgDATAFile(eplMac[i].toUpperCase(), "C:/cs/电子价签模拟真实版/日化-牙膏/01-日化-3-1.bin");
	                //String data = sendCallOver();
	                logger.info("发送指令消息："+data);
	                IoBuffer ioBuffer = DataUtil.getDatabuffer(data);
	                iosession.write(ioBuffer);
				}
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
	
	public String sendChangeImgDATA(String eplMac)
	{
		Map<String, Object> dataMap = new HashMap<>();
		dataMap.put("order", "change_img");
		dataMap.put("operate_id", "110001");
		dataMap.put("msg_id", DataUtil.getMsgId());
		
		String base64Str = "////f////////////////////3////////////////////9/////////////////////f////////////////////3////////////////////9/////////////////////f////////////////////3////////////////////9/9/n/////+/+9/8D////vf/c5////4Af9s//d///4D3/0Of///+/74A/wXf/4Dd9/8Ln////oN/2z913/+23ff8O5gAP/6q//vfdA//tt33+HuYAD/+K78Bv3f//7bd9/l7mc5//qg/fn90D/+22/f/YAHOf/6r/2H/dd//ttv3/2ABzn/+qv9+PwXf/7b/9/97uc5//oN/Ad/93/+2gff/e7nOf/7/v/8f/A//tu73/3s5zn///////////7bu9/97Oc5//+//3f//7/+23vf/e+nOf//f/9P/gB//tt73//+Bzn//AD4AH/b//4C+9/4ACc5//P//1/8GD///vPf+ADnOf//fv9v/97////H3/nv5zn//oH+4H8E///7/9/57+c5//3/+Vb+Uv//8//f+e/nOf/z//22/Ub///f/3/nv5zn//f/9Vv8UP//v/9/57+c5//6A/OB/Vv//yAPf+eAAAP//f//v/wb//5+93/ngAAD///////////8/vd/77//////////////+f33f8+///////////////P993/Pv/////zP/mf/M//z/fd/77/////8z/5n/zP//Pv3f/////////////////5793//////////////////N/d/8AAAD////////////7fnf/AAAA/////////////fH3/z+P5/////////////z/9/8/j+f////////////+//f/P4/n///////////////3/z+P5///////////////9/8/j+f///////////////f/AAAA//77/99/////4AD3/wAAAP/+e/+AfwQP///+9/8AAAD//xv//39W7////vf////////Af///Vt////73//P/////4H///1bf///+9//zn////5v///9X/////vf/84f///57//9/VB//AAD3//OB/v/++////1dv///+9//z4P5///////9Xb////vf/8/j+f///////Bu////73//P//n////////2P///+9//z//5//4//zn///////vf/8//+f/90/71/9/////73//P//n//dv+7f/f//+AAd/gAAAB//3b/t3/oH/////f4AAAA//+B/89/32/////3//P//////////79v////9//z/////5H/3v9+7/////f/8/////9u/79/vu/////3//P/////bv+3f93v////9//z/////27/t3/t7/////f///////+R/8j/94/////3//+f/3////////f//+P/9///n/4/////9/////+vtvf+P5/8f//+/3ff////qrb3/j+f/H////93P+Af/6q29/4/n/j/////AP//3/+qtvf+P5/x/////vf//9//qrb3/j+f4/////73///f/6+29/4/n8f/////9/8AH/+Atvf+P58P//+P//f//9//r7b3/j+cH///dP4AH//f/6q29/4/kD///3b/9///3/+qtvf+PwD///92//f/4A//qrb3/gAD////gf///////6q29/4AH/////////////+qtvf+H5////+B/+3/////r4B3/j+YAf//fv/z/////+P/9/4/mAD//37/wP////////f+P5/8f/9+//P////////3/j+f/n//gf/t////////9/4/n/5////////////gAPf+P5/+f/////////////73/j+f/n/////ff//////+9/4/n/5/////gH///////vf+P5/+f/////9///////73/j+f/n/////////////+9/4/n/x///////////8AAPf//5+A/////85///////73//+fgP////+9f//////+9///////////u3///////vf//////////7d///////73//v////////Pf//////+9//x//8//////////////vf/4///P////t3f////4AB3/8f//z////9AP///////9/+P4AY/////3f////////f/ACAGP////0Af/////9/3/gAn/n////7d/////98/9/h/5/5/////eB/////cAPf4/+f+f////wW/////wv33//Pn/H////9u/////9799//z5/z/////YD/////e/ff/8+f4/////3/f////3gH3/+Pn8f//////H////9//t//n5gP//////////////Hf/5+YH////////////4AP3+ADn4////////////+//9/gAZ/P////////////v//f/zmfz////////////L//3/45n8f///////////4//9/+eZ/n////////////v//f/Hmf4////////////7//3/z5n+P///////////+//9/4+YAT///////////////f+fmAEf//////////////3/H5gBn/////////////+9/58f/4/////////////3Pf/4H/+P///////////gN33/+D//////////////7bT9/////////////////+2x/f/////////////////ts73/////////////////jb+9/////////////////+2/vf/////////////////tgD3/////////////////7bP9/////////////////+25/f/////////////////tuP3/////////////////7bJ9/////////////////+A3ff//////////////////773///////////////////+9/////////////////////f////////////////////3//////////////////f79//////////////////38/f/////////////////9+f3//////////////////eP9//////////////////2Pvf/////////////////8f73///////////////////+9////////////////////vf/////////////////AAH3//////////////////j/9//////////////////+P/f//////////////////J/3//////////////////nH9//////////////////z8/f/////////////////5/n3///////////////////99/////////////////////f////////////////////3////////////////////9//////////////////P/ff/////////////////z/33/////////////////5/99//////////////u//+AAff/////////////7u//gAH3/////////////wAP///99//////////////t/////ff/////////////7f////33/////////////7/f////9/////////////+73/////f/////////////u9////n3/////////////7vf///59/////////////+73/////f//////////+7vgA/////3///////////hX//////99///////////D7+9///v+ff//////////+5fsB//3/H3///////////h74u//7/l9///////////vf+4H/+/zff//////////73vf3//vx33//////////+wD0N//5499///////////rf97f//Afff//////////53/e3//4f33///////////9/3tv////9/////////////8D7/////f///////////f/+B////v3//////////93/////9799///////////sB+37/++/ff///////////+/t+//vv33///////////u/6gv/7799///////////sv+ar/+dfff//////////77/uq//wXv3//////////+ADwKv/8MD9///////////vv+6r///h/f//////////7r/mq/////3///////////m/6gv////9/////////////+37/////f/////////////t+//gAH3////////////7////4AB9///////////gB/7f////ff//////////6//qv////33//////////+u76gP///99///////////gm+qr////ff//////////6qvoq////33//////////8q34qv////9///////////qt9qr//v//f//////////4Kvaq//7v/3//////////+ub2qv//H/9///////////r+9oD/+H//f/////////////+///8f/3/////////////////+7/9//////////////3///v//f/////////////cB/////3/////////////7+/////9///////////////f//P/ff/////////////23//z/33///////////j/1t//5/99///////////3T+bf/+AAff//////////92/wA//gAH3///////////dv5t////99///////////4H9bf////ff/////////////23////33///////////j////////9///////////3T////////f//////////92/4f////33///////////dv////+/59///////////4H8AD//f8ff/////////////3///v+X3///////////n/7v//7/N9///////////2z+oD/+/Hff//////////+T/qr//nj33///////////yfwK//8B99///////////zb+qr//h/ff///////////5/qA/////3/////////////7v/////9//////////////////vf3f/////////////////L3z3/////////////////4AD9//////////////////vf/f/////////////////z3/3/////////////////6AAd/////////////////9vf/f/////////////////73/3///////4AMEwSYAP/79/d///////+ADBMEmAD/+/APf///////n+/z/Jv8//gP73///////5Bs8GN7BP/7t+9///////+QbPBjewT/+7vff///////kG+AmJsE//u9/3///////5BvgJibBP/7gAd///////+Qb//k+wT/+//3f///////n+/gYxv8///+D3///////5/v4GMb/P////9///////+ADJNkmAD/////f/////////yMBB///////3/////////8jAQf//////9///////+TjwAf4+T/////f///////k48AH+Pk/////3////////NzjwfkB/////9///////+TYPODHPz/////f///////k2Dzgxz8/////3////////xw82QEB/////9////////jDwOE////////f///////4w8DhP///////3///////5OQkISEAP////9///////+TkJCEhAD/////f///////n4+A/Afg/////3///////4x872BgGP////9///////+MfO9gYBj/////f////////AwDY4d8/////3////////wMA2OHfP////9///////+P/G984/z/////f///////nwDzZJiD/////3///////58A82SYg/////9///////+f/5B4fAf/////f///////k4CTgwAA/////3///////5OAk4MAAP////9///////////9/n2D/////f///////////f59g/////3///////4APYICbe/////9///////+f7OCAHxj/////f///////n+zggB8Y/////3///////5Bvj3yAB/////9///////+Qb4/8hxj/////f///////kG+P/IcY/////3///////5Bsn/vnn/////9///////+QbJ/755//////f///////n+98+5wH/////3///////4AMEGDgZP////9///////+ADBBg4GT/////f////////////////////3////////////////////9/////////////////////f////////////////////3////////////////////9/////////////////////f////////////////////3////////////////8=";
		
		Map<String, Object> map = new HashMap<>();
		map.put("operate_id", "CHANGE_IMG_Id_001");
		map.put("mac", eplMac);
		map.put("imgbase64", base64Str);
		map.put("change_time", DataUtil.simpleDateFormat.format(new Date()));
		map.put("change_type", "update_img");
		map.put("img_index", "00");
		
		dataMap.put("data", map);
		return JSON.toJSONString(dataMap);
	}
	
	public String sendChangeImgDATAFile(String eplMac,String filePath)
	{
		Map<String, Object> dataMap = new HashMap<>();
		dataMap.put("order", "change_img");
		dataMap.put("operate_id", "110001");
		dataMap.put("msg_id", DataUtil.getMsgId());
		
		
		String base64Str = null;;
		try {
			base64Str = EncryptionUtils.base64EncodeFile(filePath);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		Map<String, Object> map = new HashMap<>();
		map.put("operate_id", "CHANGE_IMG_Id_001");
		map.put("mac", eplMac);
		map.put("imgbase64", base64Str);
		map.put("change_time", DataUtil.simpleDateFormat.format(new Date()));
		map.put("change_type", "update_img");
		map.put("img_index", "00");
		
		dataMap.put("data", map);
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
