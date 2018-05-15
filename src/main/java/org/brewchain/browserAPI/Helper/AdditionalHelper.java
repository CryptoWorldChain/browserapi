package org.brewchain.browserAPI.Helper;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.brewchain.browserAPI.gens.Additional.Count;
import org.brewchain.browserAPI.gens.Additional.Node;
import org.brewchain.browserAPI.gens.Additional.ResGetAdditional;
import org.brewchain.browserAPI.gens.Additional.ResGetTxCount;
import org.brewchain.browserAPI.util.CallHelper;
import org.brewchain.browserAPI.util.DataUtil;
import org.fc.brewchain.bcapi.EncAPI;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.Data;
import lombok.val;
import lombok.extern.slf4j.Slf4j;
import onight.osgi.annotation.NActorProvider;
import onight.tfw.ntrans.api.ActorService;
import onight.tfw.ntrans.api.annotation.ActorRequire;
import onight.tfw.otransio.api.IPacketSender;
import onight.tfw.otransio.api.PacketHelper;
import onight.tfw.otransio.api.beans.FramePacket;
import onight.tfw.outils.serialize.JsonSerializer;

/**
 * @author jack
 * 
 *         block 相关信息获取
 * 
 */
@NActorProvider
@Provides(specifications = { ActorService.class }, strategy = "SINGLETON")
@Instantiate(name = "additionalHelper")
@Slf4j
@Data
public class AdditionalHelper implements ActorService {

	@ActorRequire(name = "blockHelper", scope = "global")
	BlockHelper blockHelper;
	
	@ActorRequire(name = "bc_encoder", scope = "global")
	EncAPI encApi;
	
	@ActorRequire(name = "http", scope = "global")
	IPacketSender sender;
	
	private final static String QUERY_TX = "http://128.14.133.222:9200/transaction/_search";
	
	private final static String QUERY_NODE = "http://128.14.133.226:30800/fbs/pzp/pbinf.do";//30802
	
	public ResGetAdditional.Builder getAdditional() {
		ResGetAdditional.Builder ret = ResGetAdditional.newBuilder();
		getAvgBlockAndTps(ret);
		
		List<Node> dposList = getDposNode();
		List<Node> raftList = getRaftNodes();
		List<Node> allNode = new LinkedList<Node>();
		
		//add dpos to node
		if(dposList != null && !dposList.isEmpty()){
			allNode.addAll(dposList);
		}
		
		//raft and add raft to node
		if(raftList != null && !raftList.isEmpty()){
			allNode.addAll(raftList);
			ret.setDpos(raftList.size() + "");
		}else{
			ret.setDpos("0");
		}
		
		// nodes
		ret.setNodes(allNode.size() + "");
		
		return ret;
	}
	
	/**
	 * @param ret
	 */
	public List<Node> getNodes(){
		List<Node> raftList = getRaftNodes();
		List<Node> dposList = getDposNode();
		List<Node> allNode = new LinkedList<Node>();
		if(raftList != null && !raftList.isEmpty()){
			allNode.addAll(raftList);
		}
		
		if(dposList != null && !dposList.isEmpty()){
			allNode.addAll(dposList);
		}
		
		return allNode;
	}
	
	public  List<Node> getRaftNodes(){
		List<Node> raftList = getNodesBase("raft");
		return raftList;
	}
	
	public List<Node> getDposNode(){
		List<Node> dposList = getNodesBase("dpos");
		return dposList;
	}
	
	/**
	 * @return
	 */
	public List<Node> getNodesBase(String nodeType){
		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> reqParam = new HashMap<String, Object>();
		reqParam.put("nid", nodeType);
		FramePacket fp = PacketHelper.buildUrlFromJson(JsonSerializer.formatToString(reqParam), "POST", QUERY_NODE);
		
		List<Node> list = new LinkedList<Node>();
		try {
			val nodeRet = sender.send(fp, 30000);
			if(nodeRet.getBody() != null && nodeRet.getBody().length > 0){
				JsonNode jsonObject = mapper.readTree(nodeRet.getBody());
				
				if(jsonObject != null){
					if(jsonObject.has("dnodes")){
						ArrayNode a = (ArrayNode) jsonObject.get("dnodes");
						for(JsonNode jn : a){
							list.add(getNodeInfo(jn));
						}
					}
					
					if(jsonObject.has("pnodes")){
						ArrayNode a = (ArrayNode) jsonObject.get("pnodes");
						for(JsonNode jn : a){
							list.add(getNodeInfo(jn));
						}
					}
				}
			}else{
				log.error("request node list error");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return list;
	}
	
	/**
	 * 
	 * {
    "ret_code": 0,
    "current": { //当前节点
        "node_name": "128.14.152.28.30510",
        "uri": "tcp://128.14.152.28:30510",
        "startup_time": 1526287167452,
        "pub_key": "048a28eb1fc9f68c4c23c521c673f5a876c1063ab1fb01faf34477f6a8d8d29bf4cbe57c3daa9a13b2f57bae73445b9f914694992330b5cc3b754ffd4cd19f1e85",
        "bcuid": "R6PlDWjaC7NQMNEiHoYl1vC1ZmlYO",
        "try_node_idx": 48,
        "node_idx": -1,
        "recv_cc": 4517,
        "send_cc": 0,
        "block_cc": 0,
        "sign": "3045022100f7008d8db7b903f0c97f730c1121378aa7a7fc4703991563cd5ed41ea3515f6f02207cb682fdfff64c1b95e04b20bd13cb5c73e7250eb98cda33938b0158316adb23"
    },
    "bit_encs": "vNpfQQEh9YEliT8CUexHn1i5azL4NJ6O5Q",
    "pnodes": [//pendding nodes 正在加入的nodes
        {
            "node_name": "128.14.133.250.30510",
            "uri": "tcp://128.14.133.250:30510",
            "startup_time": 1526287168493,
            "pub_key": "04356092522025afbbcabbbb3138561616dcdaf787dec42dee7b28a799388005cf938b61534b45c32b994ce1e270b487ee0311c1602748e14eafe5e34498747c45",
            "bcuid": "RkEn9D8pxuNNwTxL1UjG9hSSOl5U4",
            "try_node_idx": 214,
            "node_idx": -1,
            "recv_cc": 0,
            "send_cc": 0,
            "block_cc": 0,
            "sign": "30460221008d0be27b33deb4d385e53cac73c9473afd5edf2ff8206b0b487acc93a2ff5d92022100e4889f1e87de43dc85a326230635214489491f45da1d90e05aed78730ffdce08"
        }
    ],
    "dnodes": [//已经连接上的节点
        {
            "node_name": "128.14.133.226.30510",
            "uri": "tcp://128.14.133.226:30510",
            "startup_time": 1526287165398,
            "pub_key": "0498164ada2ee8db10e0148a0dd0d377bb7eda8f33fedff39a40d331df7ab2c3dbdb8dfc566825f0284f9fc4d0188d90b82263f89da0583795c5ebd2233f8ae246",
            "bcuid": "Rg2pYYwxCsl0Hnh0WvXMjcvWFbHzk",
            "try_node_idx": 153,
            "node_idx": 153,
            "recv_cc": 0,
            "send_cc": 0,
            "block_cc": 0,
            "sign": "3046022100e0dc848f531ac6f300d905b826341c0f8bd4c8fc32e82e5877fc39ad3bc9871502210090d252f71d4da1ff6fbec52c3338d53dc05f21b5b8f2dffa197dadc110d7d60d"
        }
    ]
}
	 */
	
	/**
	 * @param jn
	 * @return
	 */
	public Node getNodeInfo(JsonNode jn){
		Node.Builder node = Node.newBuilder();
		String node_name = "";
		String uri = "";
		long startup_time = 1l;
		int node_idx = 0;
		long recv_cc = 1l;
		long send_cc =1l;
		long block_cc = 1l;
		
		if(jn.has("node_name")){
			node_name = jn.get("node_name").asText();
			node.setNodeName(node_name);
		}
		
		if(jn.has("uri")){
			uri = jn.get("uri").asText();
			node.setUri(uri);
		}
		
		if(jn.has("startup_time")){
			startup_time = jn.get("startup_time").asLong();
			node.setStartupTime(startup_time);
		}
		
		if(jn.has("node_idx")){
			node_idx = jn.get("node_idx").asInt();
			node.setNodeIdx(node_idx);
		}
		
		if(jn.has("recv_cc")){
			recv_cc = jn.get("recv_cc").asLong();
			node.setRecvCc(recv_cc);
		}
		
		if(jn.has("send_cc")){
			send_cc = jn.get("send_cc").asLong();
			node.setSendCc(send_cc);
		}
		
		if(jn.has("block_cc")){
			block_cc = jn.get("block_cc").asLong();
			node.setBlockCc(block_cc);
		}
	
		return node.build();
		
	}
	
	/**
	 * @param ret
	 */
	public void getAvgBlockAndTps(ResGetAdditional.Builder ret) {
		double avg = 0.1d;
		/**
		 * 1、获取现有块的平均出块时间
		 *  1.1、缓存存在则从缓存中取出数据进行计算 
		 *  1.2、缓存中不存在，则从新计算
		 * 2、计算最新块之后的平均出块时间
		 * 	2.1、比较缓存中最新区块高度是否是现有最新区块高度 
		 * 	2.1、计算公式：新的平均出块时间 = （（现有平均出块时间 X 现有平均出块个数）+ 新的出块时间）/
		 * 新的出块个数
		 * 
		 * 
		 * 注意：缓存中还是现实中，平均出块时间都要是 ’秒 ‘
		 */

		try {
			org.brewchain.account.gens.Block.BlockEntity.Builder theBestBlockEntity = blockHelper.getTheBestBlockEntity();
			int localBestHeight = 0;
			String localBestHeightStr = BrowserAPILocalCache.additional.get("bestHeight");
			if(localBestHeightStr.equals("0")){
				//未缓存任何平均出块时间相关数据
				// 没有缓存则需要重新计算，然后加入到缓存中
				org.brewchain.account.gens.Block.BlockEntity.Builder genesisBlockEntity = blockHelper.getGenesisBlockEntity();
				LinkedList<org.brewchain.account.gens.Block.BlockEntity> list = blockHelper.getParentsBlocks(theBestBlockEntity.getHeader().getBlockHash().toByteArray(), genesisBlockEntity.getHeader().getBlockHash().toByteArray(), theBestBlockEntity.getHeader().getNumber());
				
				calAvg(list, ret);
			}else{
				String avgStr = BrowserAPILocalCache.additional.get("avg");
				//已经缓存到该块高度的平均出块时间
				localBestHeight = Integer.parseInt(localBestHeightStr);
				if(theBestBlockEntity.getHeader().getNumber() == localBestHeight){
					//如果已缓存高度与实际高度相等，直接返回
//					avg = Double.parseDouble(avgStr);
					String tps = BrowserAPILocalCache.additional.get("tps");
					ret.setTps(tps);
					ret.setAvgBlockTime(avgStr);
				}else{
					//已经计算过的txCount
					int localTxCount = Integer.parseInt(BrowserAPILocalCache.additional.get("txCount"));
					//计算从该块开始到最新区块的平均出块时间
					org.brewchain.account.gens.Block.BlockEntity oldestBlockEntity = blockHelper.getBlockEntityByBlockHeight(localBestHeight); 
					LinkedList<org.brewchain.account.gens.Block.BlockEntity> list = blockHelper.getParentsBlocks(theBestBlockEntity.getHeader().getBlockHash().toByteArray(), oldestBlockEntity.getHeader().getBlockHash().toByteArray(), theBestBlockEntity.getHeader().getNumber());
					//注意：这里计算的平均出块时间只是部分区块的，不完全，需要加上之前的
					calAvg(list, ret);
					double localAvg = Double.parseDouble(avgStr);
					avg = ((Double.parseDouble(ret.getAvgBlockTime()) * list.size()) + (localAvg * localBestHeight)) / (list.size() + localBestHeight);//ret中的avg是秒,缓存的avg也是秒
					ret.setAvgBlockTime(DataUtil.formateStr(avg + ""));
					double tps = (Double.parseDouble(ret.getAvgBlockTime()) * (list.size() + localBestHeight)) / (ret.getTxCount() + localTxCount);
					ret.setTps(DataUtil.formateStr(tps + ""));//avg 发生变化,tps也会发生变化
				}
				
			}
			//重新构建缓存
			BrowserAPILocalCache.additional.put("avg", ret.getAvgBlockTime());
			BrowserAPILocalCache.additional.put("bestHeight", theBestBlockEntity.getHeader().getNumber() + "");
			BrowserAPILocalCache.additional.put("txCount", ret.getTxCount() + "");
			BrowserAPILocalCache.additional.put("tps", ret.getTps());
			
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
	}

	public void calAvg(LinkedList<org.brewchain.account.gens.Block.BlockEntity> list, ResGetAdditional.Builder ret) {
		double avg = 0;
		int txCount = 0;
		int blockCount = 0;
		if (list != null && !list.isEmpty()) {
			blockCount = list.size();
			int bestHeight = list.get(0).getHeader().getNumber();
			long oldTimeStamp = list.get(0).getHeader().getTimestamp();
			int i = 0;
			for (org.brewchain.account.gens.Block.BlockEntity blockEntity : list) {
				if (blockEntity != null && blockEntity.getHeader() != null) {
					if (blockEntity.getHeader().getNumber() != bestHeight) {
						txCount += blockEntity.getHeader().getTxHashsCount();
						long a = oldTimeStamp - blockEntity.getHeader().getTimestamp();
						avg = ((avg * i) + a) / (i + 1);
						i += 1;
						oldTimeStamp = blockEntity.getHeader().getTimestamp();
						bestHeight = blockEntity.getHeader().getNumber();
					}
				} else {
					log.warn("entity or entity's header is null");
				}
			}
		}
		
		ret.setAvgBlockTime(DataUtil.formateStr((avg/1000) + ""));
		ret.setTxCount(txCount);
		ret.setTps(DataUtil.formateStr((Double.parseDouble(ret.getAvgBlockTime()) * blockCount/txCount) + ""));
	}

	
	public void searchTx(ResGetTxCount.Builder ret){
		long nowTimeStamp = new Date().getTime();
		//week
		long lastWeekTimeStamp = nowTimeStamp;
		int lastWeekTotal = 0;
		//day
		long lastDayTimeStamp = nowTimeStamp;
		int lastDayTotal = 0;
		//hour
		long lastHourTimeStamp = nowTimeStamp;
		int lastHourTotal = 0;
		//ten
		long lastTenTimeStamp = nowTimeStamp;
		int lastTenTotal = 0;
		
//		for(int i = 0; i < 20; i++){
//			Random random = new Random();
//			//week
//			Count.Builder weekCount = Count.newBuilder();
//			weekCount.setValue(1 + random.nextInt(100));
//			ret.addWeek(i, weekCount);
//			
//			//day
//			Count.Builder dayCount = Count.newBuilder();
//			dayCount.setValue(1 + random.nextInt(100));
//			ret.addDay(i, dayCount);
//			
//			//hour
//			Count.Builder hourCount = Count.newBuilder();
//			hourCount.setValue(1 + random.nextInt(100));
//			ret.addHour(i, hourCount);
//			
//			//ten
//			Count.Builder tenCount = Count.newBuilder();
//			tenCount.setValue(1 + random.nextInt(100));
//			ret.addTen(i, tenCount);
//		}

		for(int i = 0; i < 20; i++){
			//week
			long w = 60 * 60 * 24 * 7 * 1000;
			lastWeekTotal = searchTxBetweenRange(lastWeekTimeStamp - w, lastWeekTimeStamp);
			Count.Builder weekCount = Count.newBuilder();
			weekCount.setValue(lastWeekTotal);
			ret.addWeek(i, weekCount);
			lastWeekTimeStamp -= w;
			
			//day
			long d = 60 * 60 * 24 * 1000;
			lastDayTotal = searchTxBetweenRange(lastDayTimeStamp - d, lastDayTimeStamp);
			Count.Builder dayCount = Count.newBuilder();
			dayCount.setValue(lastDayTotal);
			ret.addDay(i, dayCount);
			lastDayTimeStamp -= d;
			
			//hour
			long h = 60 * 60 * 1000;
			lastHourTotal = searchTxBetweenRange(lastHourTimeStamp - h, lastHourTimeStamp);
			Count.Builder hourCount = Count.newBuilder();
			hourCount.setValue(lastHourTotal);
			ret.addHour(i, hourCount);
			lastHourTimeStamp -= h;
			
			//ten
			long t = 60 * 10 * 1000;
			lastTenTotal = searchTxBetweenRange(lastTenTimeStamp - t, lastTenTimeStamp);
			Count.Builder tenCount = Count.newBuilder();
			tenCount.setValue(lastTenTotal);
			ret.addTen(i, tenCount);
			lastTenTimeStamp -= t;
		}
	}
	
	/**
	 * 时间段内交易总数
	 * 
	 * @param gt
	 * @param lt
	 * @return
	 */
	public int searchTxBetweenRange(long gt, long lt){
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode param = mapper.createObjectNode();
		ObjectNode query = mapper.createObjectNode();
		ObjectNode bool = mapper.createObjectNode();
		ArrayNode must = mapper.createArrayNode();
		ObjectNode must1 = mapper.createObjectNode();
		ObjectNode range = mapper.createObjectNode();
		ObjectNode timestamp = mapper.createObjectNode();
		timestamp.put("gt", gt);
		timestamp.put("lt", lt);
		range.set("@timestamp", timestamp);
		must1.set("range", range);
		must.add(must1);
		bool.set("must", must);
		query.set("bool", bool);
		param.set("query", query);
		log.debug("request txCount between " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSSS").format(new Date(gt)) + " and" +new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSSS").format(new Date(lt)));
		String ret = CallHelper.remoteCallPost(QUERY_TX, param.toString());
		JsonNode jn = null;
		int total = 0;
		try {
			jn = mapper.readTree(ret);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if(jn != null && jn.has("hits") && jn.get("hits").has("total")){
			total = jn.get("hits").get("total").asInt();
		}
		
		return total;
	}
}

