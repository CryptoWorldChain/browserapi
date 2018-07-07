package org.brewchain.browserAPI.Helper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;

import org.brewchain.browserAPI.gens.Additional.ResGetAdditional;
import org.brewchain.browserAPI.gens.Block.BlockInfo;
import org.brewchain.browserAPI.gens.Block.ResGetBatchBlocks;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import lombok.extern.slf4j.Slf4j;
import onight.tfw.outils.bean.JsonPBFormat;
import onight.tfw.outils.conf.PropHelper;

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
	
	public long BEST_HEIGHT = 0;
	public String AVG_BLOCK_TIME = "";
	public String TPS = "";
	public String NODES = "";
	public String D_NODES = "";
	public String P_NODES = "";
	public String CONFIRM = "";
	
	PropHelper props = new PropHelper(null);
	
	private WsServer(String ip, int port, BlockHelper blockHelper, AdditionalHelper additionalHelper) {
		super(new InetSocketAddress(ip, port));
		this.blockHelper = blockHelper;
		this.additionalHelper = additionalHelper;
	}

	public static WsServer getInstance(String ip, int port, BlockHelper blockHelper, AdditionalHelper additionalHelper) {
		if (instance == null) {
			WsServer singleton;
			synchronized (WsServer.class) {
				singleton = instance;
				if (singleton == null) {
					synchronized (WsServer.class) {
						if (singleton == null) {
							singleton = new WsServer(ip, port, blockHelper, additionalHelper);
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
		removeConnection(conn);
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
		}else {
			ObjectNode ret = mapper.createObjectNode();
			ret.put("retCode", "-1");
			ret.put("msg", "wrong message");
			conn.send(ret.toString());
		}
	}

	@Override
	public void onError(WebSocket conn, Exception ex) {
		// 错误时候触发的代码
		log.error("socket error : " + ex.getMessage());
		removeConnection(conn);
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
					TPS = tps;
					NODES = nodes;
					D_NODES = dNodes;
					P_NODES = pNodes;
					CONFIRM = confirm;
				}
			}
		}
	}
	
	/**
	 * @param conn
	 */
	public void blkgtb (WebSocket conn) {
		BlockInfo bestBlock = null;
		while (true) {
			try {
				Thread.sleep(DELAY);
			} catch (InterruptedException e) {
				log.warn("error : " + e.getMessage());
			}
			bestBlock = blockHelper.getTheBestBlock();
			if(bestBlock != null){
				long height = bestBlock.getHeader().getHeight();
				if(height > BEST_HEIGHT){
					BEST_HEIGHT = height;
					List<BlockInfo> list = blockHelper.getBatchBlocks(1, 10);
					ResGetBatchBlocks.Builder ret = ResGetBatchBlocks.newBuilder();
					ret.setRetCode(1);
					if(list != null && !list.isEmpty()){
						for (BlockInfo block : list) {
							ret.addBlocks(block);
						}
					}
					conn.send(format.printToString(ret.build()));
				}
			}
			
		}
	}

}
