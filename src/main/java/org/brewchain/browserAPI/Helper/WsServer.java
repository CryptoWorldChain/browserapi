package org.brewchain.browserAPI.Helper;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.brewchain.browserAPI.gens.Additional.ResGetAdditional;
import org.brewchain.browserAPI.gens.Block.BlockInfo;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import lombok.extern.slf4j.Slf4j;
import onight.tfw.outils.bean.JsonPBFormat;

/**
 * @author jack
 * 
 * 服务启动后需要先启动 socket http://localhost:8000/lct/pbsss.do
 *
 */
@Slf4j
public class WsServer extends WebSocketServer {

	BlockHelper blockHelper = null;
	
	AdditionalHelper additionalHelper = null;

	private static ObjectMapper mapper = new ObjectMapper();

	private static WsServer instance = null;
	
	private final static long DELAY = 5 * 1000;
	
	public int BEST_HEIGHT = 0;
	public String AVG_BLOCK_TIME = "";
	public String TPS = "";
	public String NODES = "";
	public String D_NODES = "";
	public String P_NODES = "";
	public String CONFIRM = "";

	private WsServer(int port, BlockHelper blockHelper, AdditionalHelper additionalHelper) {
		super(new InetSocketAddress(port));
		this.blockHelper = blockHelper;
		this.additionalHelper = additionalHelper;
	}

	public static WsServer getInstance(int port, BlockHelper blockHelper, AdditionalHelper additionalHelper) {
		if (instance == null) {
			WsServer singleton;
			synchronized (WsServer.class) {
				singleton = instance;
				if (singleton == null) {
					synchronized (WsServer.class) {
						if (singleton == null) {
							singleton = new WsServer(port, blockHelper, additionalHelper);
						}
					}
					instance = singleton;
				}
			}
		}

		return instance;
	}

	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake) {
		// ws连接的时候触发的代码，onOpen中我们不做任何操作
		log.debug("open connection " + conn.getRemoteSocketAddress());
	}

	@Override
	public void onClose(WebSocket conn, int code, String reason, boolean remote) {
		// 断开连接时候触发代码
		userLeave(conn);
		log.debug(reason);
	}

	JsonPBFormat format = new JsonPBFormat();
	@Override
	public void onMessage(WebSocket conn, String message) {
		System.out.println(message);
		JsonNode jsonNode = null;
		try {
			jsonNode = mapper.readTree(message);
		} catch (IOException e) {
			onError(conn, e);
		}
		// {"api":"/blk/pbgtb.do"}
		if (jsonNode != null && jsonNode.has("api")) {
			String api = jsonNode.get("api").asText();
			if (api.equals("/blk/pbgtb.do")) {
				blkgtb(conn);
			} else if(api.equals("/adi/pbget.do")){
				adipbget(conn);
			}
		}
	}

	@Override
	public void onError(WebSocket conn, Exception ex) {
		// 错误时候触发的代码
		log.error("socket error : " + ex.getMessage());
	}

	/**
	 * 去除掉失效的websocket链接
	 * 
	 * @param conn
	 */
	private void userLeave(WebSocket conn) {
		WsPool.removeUser(conn);
	}

	@Override
	public void onStart() {
		log.debug("websocket start");
	}
	
	/**
	 * @param conn
	 */
	public void adipbget (WebSocket conn){
		ResGetAdditional.Builder ret = null;
		while(true){
			try{
				Thread.sleep(DELAY);
			} catch (InterruptedException e){
				log.warn("error : " + e.getMessage());
			}
			ret = additionalHelper.getAdditional();
			if(ret != null){
				String avgBlockTime = ret.getAvgBlockTime();
				String tps = ret.getTps();
				String nodes = ret.getNodes();
				String dNodes = ret.getDNodes();
				String pNodes = ret.getPNodes();
				String confirm = ret.getConfirm();
				
				if(!avgBlockTime.equals(AVG_BLOCK_TIME) || !tps.equals(TPS) || !nodes.equals(NODES) || !dNodes.equals(D_NODES) || !pNodes.equals(P_NODES) || !confirm.equals(CONFIRM)){
					conn.send(format.printToString(ret.build()));
				}
			}
		}
	}
	
	/**
	 * @param conn
	 */
	public void blkgtb (WebSocket conn) {
		BlockInfo.Builder bestBlock = null;
		while (true) {
			try {
				Thread.sleep(DELAY);
			} catch (InterruptedException e) {
				log.warn("error : " + e.getMessage());
			}
			bestBlock = blockHelper.getTheBestBlock();
			if(bestBlock != null){
				int height = bestBlock.getHeader().getHeight();
				if(height > BEST_HEIGHT){
					BEST_HEIGHT = height;
					conn.send(format.printToString(bestBlock.build()));
				}
			}
			
		}
	}

}
