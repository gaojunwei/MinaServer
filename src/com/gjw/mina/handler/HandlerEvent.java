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
		byte StartFlage1 = buf.get();//��ð�ͷ1
		byte StartFlage2 = buf.get();//��ð�ͷ2
		int bodyLength=buf.getInt();//�����Ϣ�峤��

		byte[] bytes = new byte[bodyLength];
		buf.get(bytes);

		String json = new String(bytes,CHARSET);
        InetSocketAddress remoteAddress = (InetSocketAddress) iosession.getRemoteAddress();
        String mac = iosession.getAttribute(SESSION_MAC)==null?"":iosession.getAttribute(SESSION_MAC).toString();
        logger.info("*******����˽��յ��ͻ�����Ϣ:("+mac+"-"+remoteAddress+"):"+json);

        Map<String,Object> rMap = (Map<String,Object>)JSON.parse(json);
        String order = rMap.get("order").toString();
        logger.info("order equals:"+order);

        switch (order) {
            case "report_mac"://�豸�˽��������ϱ�MAC��ַ
                mac = rMap.get("mac").toString().toUpperCase();
                //TODO ������ӳ����Ƿ��������Ӱ󶨴�Mac��ַ
                logger.info("receive client report mac:"+mac);
				iosession.setAttribute(SESSION_MAC,mac);
                //��ȡ��Ӧmac����Ϣ����
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
                            logger.info("������Ϣ������� �� ��������:"+mac);
                        }
                    }
				}else
				{
					if(!task.isFlag())
					{
						task.setFlag(true);
						new Thread(task).start();
						logger.info("��Ϣ���������������:"+mac);
					}
				}
                String data = sendChangeImgDATA("E1:00:11:00:0B:71");
                //String data = sendCallOver();
                logger.info("����ָ����Ϣ��"+data);
                IoBuffer ioBuffer = DataUtil.getDatabuffer(data);
                iosession.write(ioBuffer);
                break;
            default:
                logger.info("[unkonwn order]"+order);
                break;
        }
	}
	
	/**
	 * �ر����Ӵ���
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
			logger.info("�رն�Ӧ���߳�����  mac:"+mac);
		}
		logger.info("�ر�����  mac:"+mac);
	}
	
	/**
	 * ��ӡ��ǰ�������
	 */
	public void printMap() {
		Map<Long, IoSession> sessionMap = HandlerEvent.getInstance().getSessionMap();
		if(sessionMap.size()==0)
		{
			System.out.println("��������");
			return;
		}
		StringBuilder stringBuilder = new StringBuilder("\r\n");
		for(long sessionId:sessionMap.keySet())
		{
			IoSession session = sessionMap.get(sessionId);
			InetSocketAddress remoteAddress = (InetSocketAddress) session.getRemoteAddress();
			stringBuilder.append("[���Ӷ���ID��"+sessionId+"�����Ӷ����ַ��"+remoteAddress+"]").append("\r\n");
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
		
		//String base64Str = "////f////////////////////3////////////////////9/////////////////////f////////////////////3////////////////////9/////////////////////f////////////////////3////////////////////9///////////+B/fv/+YAff7//////////u/37//mAH3+Pmf//////4Lv8B/////9/w5n//////+67/fb/////f/GZ///////ugf31/////3/4Gf//////7v/+8/////9//gA//////+6B8A//+AAff/8AP//////uu/7///gAH3/8Gf//////4Lv+/v/79/9/8Jn///////+7/v7/+/f/f8OZ////////gf4B//v3/3+Pmf/////////////55/9/v//////////9/f///A//f//////////wA/kA//4f/3///////////t/1V/////9//////////+DB/Vb/////f//////////+9/kA/////3/////D////+Cf////4AB9/4f//g/////KX8MH/+AAff+P//wP////qN/16///f/3/H//4D////+KH7dv//v/9/x//8I/////q3927//7//f4//+GP////4N/54//+f/3+P//Bj////////////gB9/j//g4////////////8Aff4//weP//////////////3+P/4Pj/////mf/M/////9/j/8H4/////5n/zP///D/f4/+D+P////////////AP3+H/B/j////////////zz9/w/B/4////////////7/ff8AA/+P///////////+/33/gAf/j////////////v99/8Af/4////////////7/ff/4f//////////////+fv3//////////////////wD9////////////3/////+D/f///8f//////B/7v/////3///8H/////+N/+7v////9///+B//////4f8AD//4Aff//+Af//////x/7f//+AH3///BH///////f/////3/9///Ax//////998MD//7//f//g8f//////Af1V//+//3//wfH//////3X9Vf//n/9//wfx//////93+1X//4Aff/4P8f//////j/tV///AH3/4P/H////////2QP////9/8H/x//////99////////f8H/8f//////Af5t///wf3+H//H//////3X9Tf//wD9/gAAAA/////938yv//9sff4AAAAP/////j/7r//+733+AAAAD///////9/v//u99////x//////v3/D3//7vff///8f/////79/XL//+b33////H/////+Af59///w79////x///////3/cv//+P/f///////////9/w9/////3/////////////9/v//gP9//////////////////gA/f/4D////////j/9///zvH3/4AP/D/////1f3Af/5399/4AB/4/////9X+/v/+9/ff+AAP+P/////l//+//vf33/D/B/j///////4Hf/7z59/h/4f4///////+9v//eA/f4//H+P///////vf///wf3+P/x/j///////73/////9/j/8f4///////+9v/////f4//H+P///////gd/////3+P/x/D/////////v////9/h/8fx/////fv////////f4f+Pwf////27//e/////3/B/H4P////9q/wXf//7/9/4ADgH////+LP91P//8/vf/AAAD/////27/cO///d73/4AAD/////9gH3Xv//vW9//wAH//////bv917//52vf//////////iz/BB//9dz3//////////9q/////+3e9///////////bv/Af//N3vf//////////37//+//nd73/+A//////////wAP/zwA9/+AD/w//////b////893vf+AAf+P////wW/////3d73/gAD/j////9Vf////+3c9/w/wf4/////VP/////12vf4f+H+P////1X/////+9b3+P/x/j////8AH/////3e9/j/8f4/////Vf/////8/vf4//H+P////1T/9+///v/3+P/x/j////9Vf/dv//v/9/j/8fw/////Bb/rb//z//f4f/H8f/////2/22//4AH3+H/j8H///////7tv/8ve9/wfx+D///////94D/+b3vf+AA4B////////u2//O973/wAAA////////9tv/jve9/+AAA/////////rb/+73vf/8AB/////////92//uB73//////////////fv/6Pe9/////////////////+D3vf/////////////////m973/////////////wAP//ve9/////5///////9vf//73vf////8P///////dv//+A73/////D///////3n////+9/////w///////92////+ff////8P///////b3/////3/////////////3b/////9/////////////95////+/f/////////////du//3v33///wH////////29v/7799///AAH///////8AD/++/ff//gAA////////////vv33//wf+H////////3//5199//4//4////////y///Be/f/+f//P///////7v//wwP3//n//z///////8A///+H9//x//8////////+v/////f/+f//P//////////////3//j//j////////D////99//4f/x////////vf//v+ff//AAAf///////37//3/H3//4AAf///////92//7/l9///gAf////////Mf/+/zff//////////////f//vx33/////////////////5499///8B/////////////Afff//wAB////////////4f33//4AAP//////////////9//8H/h///////////////f/+P/+P///////////+B/3//n//z////////////AH9//5//8////////////n4/f/8f//P///////////z/v3//n//z///////////5/99//4//4///////////+//ff/+H/8f///////////v333//wAAH///////////7999//+AAH///////////+/fff//4AH////////////vwH3/////////////////98D9/////////////////////f////////////////////3////////////////////9/////////////////+AAff/////////////////gAH3/////////////////7799/////////////////++/ff/////////////////vv33/////////////////5z99//////////////u///Beff/////////////7u//wwP3/////////////wAP//+H9//////////////t//////f/////////////7f/////3/////////////7/f////9/////////////+73/////f/////////////u9/////3/////////////7vf///f9/////////////+73///H/f/////////////gA///l/3///////////d3/////jf9///////////wr+9///z3/f//////////h9/sB//59/3///////////cv4u//4/f9///////////w9+4H/+AAff//////////3v/f3//gAH3//////////9730N////f9///////////YB97f///3/f//////////1v/e3/////3//////////87/3tv//gf9///////////+/8D7//wB/f/////////////+B//5+P3///////////v/////8/79//////////+7/+37/+f/ff//////////2A/t+//v/33////////////f6gv/7999///////////3f+ar/+/fff//////////2X/uq//v333//////////99/wKv/78B9///////////AB+6r//fA/f//////////33/mq/////3//////////91/6gv///f9///////////zf+37/+73/f/////////////t+//u9/3////////////3////7vf9///////////AD////+73vf//////////1//+3//u973//////////9d36r//7ve9///////////BN+oD/+73vf//////////1Vfqq//gAH3//////////5Vv6Kv/7vf9///////////Vb+Kr/+73/f//////////wVfaq//e9/3//////////9c32qv/3vf9///////////X99qr/973/f/////////////aA//e9/3//////////////v//3vf9///////////////////3/f/////////////9////9/3/////////////3Af/+/P9/////////////+/v//vH/f//////////////3//6P/3///////////H/9t//wAAd///////////un9bf//s//f//////////7t/m3//7v93//////////+7f8AP/+9+d///////////wP+bf///8ff/////////////W3//gA/3///////////H/9t//7//9///////////un////+///f//////////7t/4f//v//3//////////+7f////7//9///////////wP8AD/+AAPf/////////////3/////73///////////P/7v////+9///////////tn+oD////Pf//////////8n/qr///+H3///////////k/wK/////9///////////m3+qr/////f///////////z/qA/////3/////////////7v/////9/////////////////////f////////////////////3////////////////////9/////////////////////f////////////////////3////////////////////9/////////////////////f////////////////////3///////4AM85yYAP////9///////+ADPOcmAD/////f///////n+8cG/v8/////3///////5BvH+QbBP////9///////+Qbx/kGwT/////f///////kGwcgJsE/////3///////5BsHICbBP////9///////+Qb5//ewT/////f///////n+xg5Bv8/////3///////5/sYOQb/P////9///////+ADJNkmAD/////f/////////8A+5///////3//////////APuf//////9/////////jIMD5Jz/////f////////4yDA+Sc/////3///////+OTY+SDH/////9////////ggAD4Hxv/////f///////4IAA+B8b/////3////////OTjIRgAP////9///////+cgAzj5Jz/////f///////nIAM4+Sc/////3///////4Af/4RgnP////9///////+AH/+EYJz/////f///////7ANvh5sf/////3///////+z8bGN/f/////9////////s/Gxjf3//////f///////k2+THAN//////3///////5NvkxwDf/////9///////+A8/ADY5z/////f////////IOTZGDg/////3////////yDk2Rg4P////9///////+AE4OHh4P/////f///////jw9z4wAA/////3///////48Pc+MAAP////9//////////GAYH3j/////f/////////xgGB94/////3///////4APA3ubHP////9///////+f7Oz4nxj/////f///////n+zs+J8Y/////3///////5Bv7xyABP////9///////+QbwOE/+f/////f///////kG8DhP/n/////3///////5BvjH+En/////9///////+Qb4x/hJ//////f///////n++DBGD4/////3///////4APc3hkB/////9///////+AD3N4ZAf/////f////////////////////3////////////////////9/////////////////////f////////////////////3////////////////////9/////////////////////f////////////////////3////////////////8=";
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
