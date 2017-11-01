package com.woc.chat.plugin.MatchUser;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentFactory;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.XPP3Reader;
import org.jivesoftware.openfire.IQHandlerInfo;
import org.jivesoftware.openfire.IQRouter;
import org.jivesoftware.openfire.SessionManager;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.auth.UnauthorizedException;
import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;
import org.jivesoftware.openfire.handler.IQHandler;
import org.jivesoftware.openfire.session.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParserException;
import org.xmpp.packet.IQ;
import org.xmpp.packet.PacketError;
import com.woc.chat.plugin.MatchUser.dao.Dao;
import com.woc.chat.plugin.MatchUser.util.HttpUtil;
/**
 * MatchUserPlugin这个类的构造函数只能用默认构造函数
 * @author zyw
 *
 */
public class MatchUserPlugin  implements Plugin {

	private IQRouter iqRouter;
	private  final Logger Log = LoggerFactory.getLogger(MatchUserPlugin.class);
	private SimpleDateFormat simpleDateFormat=new SimpleDateFormat("HH:mm:ss.SSS");
	SessionManager sessionManager=new SessionManager();
	
	@Override
	public void destroyPlugin() {
		// TODO Auto-generated method stub
		Log.info("------------------->MatchUserPlugin destroy!");
		
	}

	@Override
	public void initializePlugin(PluginManager pluginManager, File file) {
		// TODO Auto-generated method stub
		Log.info("------------------->MatchUserPlugin initialize!");
		try {

			iqRouter  = XMPPServer.getInstance().getIQRouter();
			iqRouter.addHandler(new MyIQHandler("MyIQHandler"));  

		} catch (Exception e) {
			// TODO: handle exception
			Log.info("------------------->Bind Exception:"+e.toString());
		}
	}


	/**
	 * 处理数据包
	 * @author Administrator
	 *
	 */
	private class MyIQHandler extends IQHandler{
		//客户端可能发来的消息
		private static final String QUIT_MATCH="quit_match";
		private static final String MATCH_USER="match_user";
		//服务端要发送的消息
		private static final String MATCH_WAIT="match_wait";
		private static final String MATCH_SUCCESS="match_success_server";
		private static final String QUIT_MATCH_SUCCESS="quit_match_success";
		private static final String NAMESPACE_INFO="match:iq:info";
		private static final String NAMESPACE_DATA="match:iq:info:data";
		private static final String QUIT_CHAT="quit_chat";
		private Dao redisDAO;
		private IQHandlerInfo info;
		private final String queryUserUrl="http://127.0.0.1:9090/plugins/presence/status?jid=%s&type=xml";
		//private final  Logger log = LoggerFactory.getLogger(MyIQHandler.class);
		@Override
		public void initialize(XMPPServer server) {
			// TODO Auto-generated method stub
			super.initialize(server);
			
		}
		public MyIQHandler(String moduleName) {
			super(moduleName);
			redisDAO=Dao.getInstance();
			// TODO Auto-generated constructor stub
			info=new IQHandlerInfo("info", NAMESPACE_INFO);
		}

		@Override
		public IQHandlerInfo getInfo() {
			// TODO Auto-generated method stub
			return info;
		}

		@Override
		public IQ handleIQ(IQ packet) throws UnauthorizedException {
			// TODO Auto-generated method stub
			/**
			 * replyIQ初始结构
			 * <iq type="result" 
			 * id="TeUQc-12" 
			 * from="localhost/MatchUserPlugin" 
			 * to="zyw8@localhost/Smack"/> 
			 * 
			 */
			String childName=packet.getChildElement().getName();
			
			if(childName==null)
				return packet;
			
			if(!childName.equals("info"))
			{
				return packet;
			}
			
			IQ replyIQ=IQ.createResultIQ(packet);
			ClientSession clientSession=sessionManager.getSession(packet.getFrom());
			
			if(clientSession==null)
			{
				log("Error user info.Session not found"
						+sessionManager.getPreAuthenticatedKeys()
						+" from "+packet.getFrom());
				replyIQ.setChildElement(packet.getChildElement().createCopy());
				replyIQ.setError(PacketError.Condition.forbidden);
				return replyIQ;
			}

			org.dom4j.Element child= packet.getChildElement().createCopy();
			String type=child.attributeValue("type");
			String ns=child.getNamespaceURI();
			if(!ns.equals(NAMESPACE_INFO))
			{
				log("This namespace is valid !"
						+sessionManager.getPreAuthenticatedKeys()
						+" from "+packet.getFrom());
				replyIQ.setChildElement(packet.getChildElement().createCopy());
				replyIQ.setError(PacketError.Condition.bad_request);
				return replyIQ;
			}
			
			//log("------------------->type:"+type);
			//log("------------------->IQ:"+packet.toXML());
			
			Element infoElement = DocumentHelper.createElement("info");
			infoElement.addNamespace("", NAMESPACE_INFO);
			//Element dataElement=infoElement.addElement("data", null);
			switch (type) {
				case MATCH_USER:
					//log("------------------->MATCH_USER");
					String  queueJID=redisDAO.lget();
					//队列中没有人
					if(queueJID==null)
					{
						//log("------------------->jid==null");
						//将自己的jid放入队列，并返回等待
						try {
							redisDAO.push(packet.getFrom().toFullJID());
							
							infoElement.addAttribute("type", MATCH_WAIT);
							infoElement.addAttribute("data","nil");
							replyIQ.setChildElement(infoElement);
							//log("------------------->reply1:"+replyIQ.toXML());
						} catch (Exception e) {
							// TODO: handle exception
							//log("------------------->ERROR:"+e.toString());
						}
						
					}
					//队列中有人
					else 
					{
						//log("------------------->jid!=null");
						try {
							//如果队列只有自己,等待
							if(queueJID.equals(packet.getFrom().toFullJID()))
							{
								infoElement.addAttribute("type", MATCH_WAIT);
								infoElement.addAttribute("data","nil");
								replyIQ.setChildElement(infoElement);
							
								
								//log("------------------->reply2:"+replyIQ.toXML());
								
							}
							else
							{
								//队列不是自己
								queueJID=redisDAO.lpop();
								boolean isUnavailable=HttpUtil.httpGet(String.format(queryUserUrl, queueJID))
										.contains("Unavailable");
								if(!isUnavailable)
								{
									//对方在线，从队列中弹出一个元素
									infoElement.addAttribute("type", MATCH_SUCCESS);
									infoElement.addAttribute("data",queueJID);
									replyIQ.setChildElement(infoElement);
								}
								else
								{
									
									//对方不在线
									//把自己名字放进去
									try {
										redisDAO.push(packet.getFrom().toFullJID());
										
										infoElement.addAttribute("type", MATCH_WAIT);
										infoElement.addAttribute("data","nil");
										replyIQ.setChildElement(infoElement);
										//log("------------------->reply1:"+replyIQ.toXML());
									} catch (Exception e) {
										// TODO: handle exception
										//log("------------------->ERROR:"+e.toString());
									}
								}
								//log("------------------->reply3:"+replyIQ.toXML());
							}
						} catch (Exception e) {
							// TODO: handle exception
							log("------------------->ERROR:"+e.toString());
						}
					}
					break;
				case QUIT_MATCH:
					long idx=redisDAO.remove(packet.getFrom().toFullJID());
					if(idx!=-1){
						infoElement.addAttribute("type", QUIT_MATCH_SUCCESS);
						infoElement.addAttribute("data","nil");
						replyIQ.setChildElement(infoElement);
					}
					break;
				case QUIT_CHAT:
						//replyIQ.setChildElement(infoElement);
						//log("--------------------------->QUIT_CHAT:"+packet.toXML());
						return packet;
				default:
					return packet;
			}//switch
			//log("------------------->return");
			return replyIQ;
		}
	}//MyIQHandler

	public void log(String text)
	{
		Log.info(text);
	}
}
