package com.woc.chat.plugin.MatchUser.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.woc.chat.plugin.MatchUser.MatchUserPlugin;

import redis.clients.jedis.Jedis;
/**
 * 操作Redis数据库，模拟队列
 */
public class Dao {
	private static Dao dao; 
	private static Jedis jRedis;
	private final static String KEY="UserQueue";
	private final static String VPN_HOST="10.173.32.26";
	private final static String LOCAL_HOST="127.0.0.1";
	private final static int PORT=6739;
	private  final Logger Log = LoggerFactory.getLogger(Dao.class);
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Dao dao=Dao.getInstance();
	}
	/**
	 * 
	 * @return
	 */
	public static Dao getInstance()
	{
		if(dao==null)
		{
			dao = new Dao();
		}
		
		return dao;
		
	}
	
	/**
	 * 移除指定元素
	 */
	public Long remove(String value)
	{
		return jRedis.lrem(KEY, 1, value);
	}
	/**
	 * 弹出最左边的数据
	 * @return
	 */
	public  String lpop() {
		// TODO Auto-generated method stub
		return jRedis.lpop(KEY);
	}
	
	/**
	 * 获取最左边的数据
	 * @return
	 */
	public  String lget() {
		// TODO Auto-generated method stub
		return jRedis.lindex(KEY, jRedis.llen(KEY)-1);
	}
	/**
	 * 将元素放入队列尾
	 * @param jid
	 */
	public  void push(String jid) {
		// TODO Auto-generated method stub
		jRedis.rpush(KEY, jid);
	}
	
	private Dao(){

		if(jRedis==null)
		{
			jRedis=new Jedis(LOCAL_HOST);
		}
		
		String ping=jRedis.ping();
		Log.info("---------->"+ping);
	}

}
