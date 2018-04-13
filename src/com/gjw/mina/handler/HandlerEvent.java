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
				//String[] eplMac = {"80:EA:CA:89:AE:E7","80:ea:ca:88:ae:be"};
				//String[] eplMac = {"80:EA:CA:88:AE:F5"};
				//String[] eplMac = {"80:EA:CA:88:AF:43"};
				//String[] eplMac = {"80:ea:ca:de:ad:99"};
				//String[] eplMac = {"80:EA:CA:FF:C0:62"};
				//String[] eplMac = {"80:EA:CA:88:AE:D3"};
				//String[] eplMac = {"80:EA:CA:89:AE:E7"};
				String[] eplMac = {"80:EA:CA:89:AE:E7","80:EA:CA:88:AE:D3"};
				for (int i = 0; i < 10000; i++) {
					String data = sendChangeImgDATA(eplMac[0]);
					//String data = sendChangeImgDATAFile(eplMac[i].toUpperCase(), "C:/cs/default_img.bin");
	                //String data = sendCallOver();
	                logger.info("发送指令消息："+data);
	                IoBuffer ioBuffer = DataUtil.getDatabuffer(data);
	                iosession.write(ioBuffer);
	                Thread.sleep(2*1000);
	                data = sendChangeImgDATA(eplMac[1]);
					//String data = sendChangeImgDATAFile(eplMac[i].toUpperCase(), "C:/cs/default_img.bin");
	                //String data = sendCallOver();
	                logger.info("发送指令消息："+data);
	                ioBuffer = DataUtil.getDatabuffer(data);
	                iosession.write(ioBuffer);
	                
	                Thread.sleep(10*1000);
				}
				String data = null;
				IoBuffer ioBuffer = null;
				
				
				/*data = sendOTACMD("80EACA88AF43", "epl", "http://127.0.0.1:8080/EPL_FIRMWARE_0.0.8.bin", "04168f2fb80905f9cd18e8490bf1f45c", "0.0.8");
				//data = sendOTACMD("80EACA88AF43", "epl", "http://127.0.0.1:8080/EPL_FIRMWARE_0.0.8.bin", "04168f2fb80905f9cd18e8490bf1f45c", "0.0.8");
				logger.info("发送指令消息："+data);
                ioBuffer = DataUtil.getDatabuffer(data);
                iosession.write(ioBuffer);
				*/
                /*
                String[] eplMac = {"80:EA:CA:89:AE:E7"};
				for (int i = 0; i < 1; i++) {
					data = sendChangeImgDATA(eplMac[0]);
					//String data = sendChangeImgDATAFile(eplMac[i].toUpperCase(), "C:/cs/default_img.bin");
	                //String data = sendCallOver();
	                logger.info("发送指令消息："+data);
	                ioBuffer = DataUtil.getDatabuffer(data);
	                iosession.write(ioBuffer);
	                
	                Thread.sleep(8*1000);
				}
                */
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
	/**
	 * 发送OTA升级指令
	 * @param deviceMac 设备mac地址
	 * @param deviceType 设备类型
	 * @param fileUrl 下载文件路径
	 * @param fileMd5 文件md5值
	 * @param version_number 版本号
	 * @return
	 */
	public String sendOTACMD(String deviceMac,String deviceType,String fileUrl,String fileMd5,String version_number)
	{
		Map<String, Object> dataMap = new HashMap<>();
		dataMap.put("order", "device_ota");
		dataMap.put("msg_id", DataUtil.getMsgId());
		dataMap.put("operate_id", "10001");
		dataMap.put("server_time", DataUtil.simpleDateFormat.format(new Date()));
		dataMap.put("device_type", deviceType);
		dataMap.put("device_mac", deviceMac);//设备MAC地址
		dataMap.put("version_number", version_number);
		dataMap.put("file_url", fileUrl);
		dataMap.put("file_md5", fileMd5);
		
		return JSON.toJSONString(dataMap);
	}
	
	public String sendChangeImgDATA(String eplMac)
	{
		Map<String, Object> dataMap = new HashMap<>();
		dataMap.put("order", "change_img");
		dataMap.put("operate_id", "110001");
		dataMap.put("msg_id", DataUtil.getMsgId());
		
		//String base64Str = "////f////////////////////3////////////////////9/////////////////////f////////////////////3////////////////////9/////////////////////f////////////////////3////////////////////9/9/n/////+/+9/8D////vf/c5////4Af9s//d///4D3/0Of///+/74A/wXf/4Dd9/8Ln////oN/2z913/+23ff8O5gAP/6q//vfdA//tt33+HuYAD/+K78Bv3f//7bd9/l7mc5//qg/fn90D/+22/f/YAHOf/6r/2H/dd//ttv3/2ABzn/+qv9+PwXf/7b/9/97uc5//oN/Ad/93/+2gff/e7nOf/7/v/8f/A//tu73/3s5zn///////////7bu9/97Oc5//+//3f//7/+23vf/e+nOf//f/9P/gB//tt73//+Bzn//AD4AH/b//4C+9/4ACc5//P//1/8GD///vPf+ADnOf//fv9v/97////H3/nv5zn//oH+4H8E///7/9/57+c5//3/+Vb+Uv//8//f+e/nOf/z//22/Ub///f/3/nv5zn//f/9Vv8UP//v/9/57+c5//6A/OB/Vv//yAPf+eAAAP//f//v/wb//5+93/ngAAD///////////8/vd/77//////////////+f33f8+///////////////P993/Pv/////zP/mf/M//z/fd/77/////8z/5n/zP//Pv3f/////////////////5793//////////////////N/d/8AAAD////////////7fnf/AAAA/////////////fH3/z+P5/////////////z/9/8/j+f////////////+//f/P4/n///////////////3/z+P5///////////////9/8/j+f///////////////f/AAAA//77/99/////4AD3/wAAAP/+e/+AfwQP///+9/8AAAD//xv//39W7////vf////////Af///Vt////73//P/////4H///1bf///+9//zn////5v///9X/////vf/84f///57//9/VB//AAD3//OB/v/++////1dv///+9//z4P5///////9Xb////vf/8/j+f///////Bu////73//P//n////////2P///+9//z//5//4//zn///////vf/8//+f/90/71/9/////73//P//n//dv+7f/f//+AAd/gAAAB//3b/t3/oH/////f4AAAA//+B/89/32/////3//P//////////79v////9//z/////5H/3v9+7/////f/8/////9u/79/vu/////3//P/////bv+3f93v////9//z/////27/t3/t7/////f///////+R/8j/94/////3//+f/3////////f//+P/9///n/4/////9/////+vtvf+P5/8f//+/3ff////qrb3/j+f/H////93P+Af/6q29/4/n/j/////AP//3/+qtvf+P5/x/////vf//9//qrb3/j+f4/////73///f/6+29/4/n8f/////9/8AH/+Atvf+P58P//+P//f//9//r7b3/j+cH///dP4AH//f/6q29/4/kD///3b/9///3/+qtvf+PwD///92//f/4A//qrb3/gAD////gf///////6q29/4AH/////////////+qtvf+H5////+B/+3/////r4B3/j+YAf//fv/z/////+P/9/4/mAD//37/wP////////f+P5/8f/9+//P////////3/j+f/n//gf/t////////9/4/n/5////////////gAPf+P5/+f/////////////73/j+f/n/////ff//////+9/4/n/5/////gH///////vf+P5/+f/////9///////73/j+f/n/////////////+9/4/n/x///////////8AAPf//5+A/////85///////73//+fgP////+9f//////+9///////////u3///////vf//////////7d///////73//v////////Pf//////+9//x//8//////////////vf/4///P////t3f////4AB3/8f//z////9AP///////9/+P4AY/////3f////////f/ACAGP////0Af/////9/3/gAn/n////7d/////98/9/h/5/5/////eB/////cAPf4/+f+f////wW/////wv33//Pn/H////9u/////9799//z5/z/////YD/////e/ff/8+f4/////3/f////3gH3/+Pn8f//////H////9//t//n5gP//////////////Hf/5+YH////////////4AP3+ADn4////////////+//9/gAZ/P////////////v//f/zmfz////////////L//3/45n8f///////////4//9/+eZ/n////////////v//f/Hmf4////////////7//3/z5n+P///////////+//9/4+YAT///////////////f+fmAEf//////////////3/H5gBn/////////////+9/58f/4/////////////3Pf/4H/+P///////////gN33/+D//////////////7bT9/////////////////+2x/f/////////////////ts73/////////////////jb+9/////////////////+2/vf/////////////////tgD3/////////////////7bP9/////////////////+25/f/////////////////tuP3/////////////////7bJ9/////////////////+A3ff//////////////////773///////////////////+9/////////////////////f////////////////////3//////////////////f79//////////////////38/f/////////////////9+f3//////////////////eP9//////////////////2Pvf/////////////////8f73///////////////////+9////////////////////vf/////////////////AAH3//////////////////j/9//////////////////+P/f//////////////////J/3//////////////////nH9//////////////////z8/f/////////////////5/n3///////////////////99/////////////////////f////////////////////3////////////////////9//////////////////P/ff/////////////////z/33/////////////////5/99//////////////u//+AAff/////////////7u//gAH3/////////////wAP///99//////////////t/////ff/////////////7f////33/////////////7/f////9/////////////+73/////f/////////////u9////n3/////////////7vf///59/////////////+73/////f//////////+7vgA/////3///////////hX//////99///////////D7+9///v+ff//////////+5fsB//3/H3///////////h74u//7/l9///////////vf+4H/+/zff//////////73vf3//vx33//////////+wD0N//5499///////////rf97f//Afff//////////53/e3//4f33///////////9/3tv////9/////////////8D7/////f///////////f/+B////v3//////////93/////9799///////////sB+37/++/ff///////////+/t+//vv33///////////u/6gv/7799///////////sv+ar/+dfff//////////77/uq//wXv3//////////+ADwKv/8MD9///////////vv+6r///h/f//////////7r/mq/////3///////////m/6gv////9/////////////+37/////f/////////////t+//gAH3////////////7////4AB9///////////gB/7f////ff//////////6//qv////33//////////+u76gP///99///////////gm+qr////ff//////////6qvoq////33//////////8q34qv////9///////////qt9qr//v//f//////////4Kvaq//7v/3//////////+ub2qv//H/9///////////r+9oD/+H//f/////////////+///8f/3/////////////////+7/9//////////////3///v//f/////////////cB/////3/////////////7+/////9///////////////f//P/ff/////////////23//z/33///////////j/1t//5/99///////////3T+bf/+AAff//////////92/wA//gAH3///////////dv5t////99///////////4H9bf////ff/////////////23////33///////////j////////9///////////3T////////f//////////92/4f////33///////////dv////+/59///////////4H8AD//f8ff/////////////3///v+X3///////////n/7v//7/N9///////////2z+oD/+/Hff//////////+T/qr//nj33///////////yfwK//8B99///////////zb+qr//h/ff///////////5/qA/////3/////////////7v/////9//////////////////vf3f/////////////////L3z3/////////////////4AD9//////////////////vf/f/////////////////z3/3/////////////////6AAd/////////////////9vf/f/////////////////73/3///////4AMEwSYAP/79/d///////+ADBMEmAD/+/APf///////n+/z/Jv8//gP73///////5Bs8GN7BP/7t+9///////+QbPBjewT/+7vff///////kG+AmJsE//u9/3///////5BvgJibBP/7gAd///////+Qb//k+wT/+//3f///////n+/gYxv8///+D3///////5/v4GMb/P////9///////+ADJNkmAD/////f/////////yMBB///////3/////////8jAQf//////9///////+TjwAf4+T/////f///////k48AH+Pk/////3////////NzjwfkB/////9///////+TYPODHPz/////f///////k2Dzgxz8/////3////////xw82QEB/////9////////jDwOE////////f///////4w8DhP///////3///////5OQkISEAP////9///////+TkJCEhAD/////f///////n4+A/Afg/////3///////4x872BgGP////9///////+MfO9gYBj/////f////////AwDY4d8/////3////////wMA2OHfP////9///////+P/G984/z/////f///////nwDzZJiD/////3///////58A82SYg/////9///////+f/5B4fAf/////f///////k4CTgwAA/////3///////5OAk4MAAP////9///////////9/n2D/////f///////////f59g/////3///////4APYICbe/////9///////+f7OCAHxj/////f///////n+zggB8Y/////3///////5Bvj3yAB/////9///////+Qb4/8hxj/////f///////kG+P/IcY/////3///////5Bsn/vnn/////9///////+QbJ/755//////f///////n+98+5wH/////3///////4AMEGDgZP////9///////+ADBBg4GT/////f////////////////////3////////////////////9/////////////////////f////////////////////3////////////////////9/////////////////////f////////////////////3////////////////8=";
		//云端生成的
		String base64Str = "/////////////////3//////////////////////////////////////////f/////////////////////////////////////////9//////////////////////////////////////////3////93n///////////////////d1//////////////f////7ff///////////////////X3/////////////9/////0F///////////////////+ff/////////////3/////W3//+GZgeeGAAf///////s1///hmYHnhgAH//f////3Xf/4f4ef4eZ/5///////933/+H+Hn+Hmf+f/9//////////n5+f+ZmBn////////////5+fn/mZgZ//3////+Af/4YGZgZn+YGf///////b3/+GBmYGZ/mBn//f////3d//gYAGfmB5gZ////////tf/4GABn5geYGf/9/////4x//+H4B/5hn/n///////+7///h+Af+YZ/5//3////8wH/4GZgeZhmAAf///////39/+BmYHmYZgAH//f////8Bf/nh+eGB4f/////////+/3/54fnhgeH////9/////YB/+eABmGZngef///////////ngAZhmZ4Hn//3////9///555/4YB55/////////Pf/+eef+GAeef///f////3z//5hh/4GBhgf///////99//+YYf+BgYYH//9/////ff/+AZ5+YGeYGH///////wAf/gGefmBnmBh//3////9/////n5geZ+eef///////f////5+YHmfnnn//f////3///5gZ+YYZ+B5///////9///+YGfmGGfgef/9/////f///+ZnmYYeH5n////////////mZ5mGHh+Z//3////8d//55+Gfgfn5mf///////cz/+efhn4H5+Zn//f////6tf/5gZ/mYAZh5////////bf/+YGf5mAGYef/9/////oB/+eB+AeeGf+f///////3b//ngfgHnhn/n//3////9W//554f4eZgf+f///////1v/+eeH+HmYH/n//f////7Gf/4B5+BgZmeH///////8lf/+AefgYGZnh//9/////rP//4HmBgfgB4H///////////+B5gYH4AeB//3////+DH////+fgYf//////////eN/////n4GH/////f////2vf/gAGZmZmYAB///////+qX/4ABmZmZmAAf/9/j///6d/+f+eZ/hnn/n///3e//+vf/n/nmf4Z5/5//377v/+Bv/5gZ///gGYGf//++7//3b/+YGf//4BmBn//fvu//9w//mBmGZ+YZgZ///73v/+Bv/5gZhmfmGYGf/9/MD//+7/+YGYeBgZmBn///////////mBmHgYGZgZ//3+A//8AH/5/5/5mYef+f///f3//u9/+f+f+ZmHn/n//fv+//7vf/gAGAHhh4AB///7/v/+73/4ABgB4YeAAf/9/f3//u9///////////////4D//7/f/////////////3/////9f///////////////////+3//////////////fv///wAf//////////////7///93f/////////////9+AD//t3///////////////v9//////////////////37+////////////////////////////////////////f/////////////////////+A//////////////////9/f3///////////////////v+//////////////////37/v///////////////////f3//////////////////f4D///////////////////////////////////////9//////////////////////v///////////////////37////////////////////+AD//////////////////fv9///////////////////7+//////////////////9//////////////////////////////////////////3+A////////////////////f3//////////////////fv+///////////////////7/v/////////////////9/f3///////////////////4D//////////////////3//////////////////////////////////////////ff5///////////////////3+3/////////////////99gt///////////////////vq//////////////////376v///////////////////ew//////////////////fwu///////////////////77v/////////////////9++////////////////////fhP/////////////////30Dv//////////////////9/3//////////////////f/7///////////////////////////////////////9//////////////////////f7//////////////////33+///////////////////++p//////////////////fuLf//////////////////9a7/////////////////9/uv///////////////////1oP/////////////////37o///////////////////+9v//////////////////fe7f//////////////////3en/////////////////99vn///////////////////3///////////////////3//////////////////////////////////////////f/9///////////////////4AP/////////////////98/3///////////////////v9//////////////////37wf///////////////////z3//////////////////f79///////////////////94f/////////////////9+93///////////////////u8P///////////+AAB//33ef////////////gAAf//9vf////////////4AAH//f7v////////////+AAB////////////////7/gAAf/9/////////////+/4AAH////3f//////////A+A/B//3wFT//////////r3gQIf//9VV//////////2+4IBH//fVVf/////////+veBAh///9UD//////////wPgPwf/9/VV//////////+/4AAH///1Vf//////////v+AAB//3wFX//////////wPgPwf///vd//////////694ECH//f33//////////9vuCAR///4An//////////r3gQIf/9+/d//////////8D4D8H////3v//////////v+AAB//3/9///////////7/gAAf////5//////////+/4AAH//f/5///////////v+AAB////5/////55////7/gAAf/9/5///////////y/4MAH///5////////////v+AAB//35//////jj////7/gAAf/////////7rv///+/4AAH//f//////+23////A+A/B////v3////rr////r3gQIf/9855////8MP///2+4IBH///u9f/////////+veBAh//39v3////gL////wPgPwf///b9////6rf/////4AAH//f6Bf////qv/////+AAB///+P3////6q/////7gAEf/9/bt////4Cf////U4ArH///u9f//////////0uALR//37r3////wH////cfgjgf//97d////79////wX4PoH//fe/f///+/f///9x+COB////v3////2A////9LgC0f/9///////+d/////U4ArH//////////ff/////uAAR//34AP////t7//////gAAf//8/3////7e//////4AAH//fp9/////wP/////+AAB///7Pf////77//////gAAf/9/2X////++//////4AAH///6cP/////////+eeBhh//3+e/////////////gAAf////f////////////4AAH//f//f//////////9+EAB///zAD///////////vhfAf/9/f9//////////gN4IXH///4Bf///////////uCFR//35/3///////////3gBUf//9wA//////////4L4HVH//f////////////99+CFR/////f/////////+/zghUf/9+AD//////////gD4QXH///P9///////////7+F8B//37/f//////////9/hAAf//+8H////////////4AAH//f89//////////5/uACB///+/f//////////YLhkgf/9/eH//////////6q4NEH///vd///////////quCVB//37vD/////////+CjglIf//93n//////////uq4JRH//fb3//gv//////+guC0h///+7//97f//////f7g1Qf/9/////e3//////oA4ZEH///////wA//////9/+CSB//3/93/97f/////+//gAgf//8BU/+C3////////4AAH//fVVf//v////////+AAB///1VX//HP////////gAAf/9/VA//mr////////4AAH///1Vf/92//////////////39VX/4AP//////////////8BV//////////////////f73f/33///////////////99//58//////////////9+AJ//ff//////8AP//////v3f/73//////+AA/////3/97//d///////D8H///////f//4D//////j/x/////f////93//////4/+f/////////+9//////+P/H////98B///ff//////w/D//////vff/v3//////+AB/////37wD/79///////wB//////+9d//////////////////fAXf/gA+Hn/////////////93/7/v36////wA/////9//d/+nr+AP///4AD//////AXf/qa//v///8Pwf////37wH/62v7r///+P/H/////+9//+gL8C////j/5/////fvf//ra/Wv///4/8f/////wH//62v7/////D8P////9////+vr8Df///4AH//////////v+/e7////AH/////3//v/4AP/v////////////8AZ//////////////////fe2///7+AD////P///////31v//Afv/////h//////9+CT//7v/Af///wP///////2y//+7+/////8D//////39tn//u/0A////h////////bK/+AD+Av///8///////fgk//+7/f7/////////////1v//u/sA///////////9/7b//7v/zf///g//wP////AG//8B+C7///4P/wA///3//v////93///+D/4AH////////////////g/4AAf//f////////////4P8AAH///w/f/////////+D+AAA//9+/z//////////g/APAP///39f/55/nn///4PgP8B//3+AD/////////+DwH/gf///e3//////////g4D/8H//f3t//j3/0H///4MB//B///4Df/7A/9V///+CA//wf/9++3/+3f4Ff///gAf/8H///rt//oA/0D///4AP//B//399//77/8V///+AH//g///+AJ//AH/Qf///gD//4P//fv3f/9X/3f///4B//+D////97/+9/gQ///+A///B//9//f//AD/1////gf//gf///////n3/gD///4P//////34AD/99/n/////////////8////////////////////fv////t+B/////////////9AP//7f3f/////////f/98////+n90P//////9vH///sAf//l+Bb///////bD//39/3//7P/2///////2j////gN//+X/9v//////gD///f3/f//p+Bb//////4A////7AH//7f3Q///////2j//99/L//g393///////9sP///+Pf/39+B////////bx//3wd7/7/////////////f//+Xv//////////////////f/////////////////////////////////////////9//////////////////////////////////////////3////////////////////9///8=";
		
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
