package org.brewchain.browserAPI.Helper;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.brewchain.browserAPI.gens.Additional.Count;
import org.brewchain.browserAPI.gens.Additional.Node;
import org.brewchain.browserAPI.gens.Additional.ResGetAdditional;
import org.brewchain.browserAPI.gens.Additional.ResGetTxCount;
import org.brewchain.browserAPI.gens.Block.BlockInfo;
import org.brewchain.browserAPI.gens.Tx.Transaction;
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
import onight.tfw.outils.conf.PropHelper;
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

	private static PropHelper props = new PropHelper(null);

	private static String QUERY_TX = "http://128.14.133.222:9200/transaction/_search";
	private static String QUERY_NODE = "http://128.14.133.226:30800/fbs/pzp/pbinf.do";// 30802
	
	 static {
		QUERY_TX = props.get("query_tx", "http://128.14.133.222:9200/transaction/_search"); 
		QUERY_NODE = props.get("query_node", "http://128.14.133.226:30800/fbs/pzp/pbinf.do");
		log.debug("init query tx and node url successful");
		log.debug("query_tx is : " + QUERY_TX);
		log.debug("query_node is : " + QUERY_NODE);
	}

	private final static String[] DELAYS = new String[] { "1w", "1d", "1h", "10m" };

	private final static int GROUP_COUNT = 20;// 每组数据个数

	private final static long THOUSAND = 1000;

	private final static long TEN_MIN = 60 * 10;

	private final static long HOUR = 60 * 60;

	private final static long DAY = HOUR * 24;

	private final static long WEEK = DAY * 7;
	
	private final static String PENDING = "PENDING";//	待连节点状态
	private final static String DIRECT = "DIRECT";// 直连节点状态
	
	private final static String NID_RAFT = "raft";
	private final static String NID_DPOS = "dpos";

	/**
	 * @return
	 */
	public ResGetAdditional.Builder getAdditional() {
		ResGetAdditional.Builder ret = ResGetAdditional.newBuilder();

		List<Node> dposList = getDposNode();
		List<Node> raftList = getRaftNodes();
		
		//确定不同状态的节点总数
		int pendingCount = 0;
		int directCount = 0;
		int allCount = 0;
		pendingCount += getPendingCount(dposList);
		pendingCount += getPendingCount(raftList);
		directCount += getDirectCount(dposList);
		directCount += getDirectCount(raftList);
		
		//两种状态确定节点总数
		allCount = pendingCount + directCount;
		
		ret.setNodes(allCount + "");
		ret.setPNodes(pendingCount + "");
		ret.setDNodes(directCount + "");
		
		List<Long> blockTimeList = new ArrayList<Long>(); 
		List<Long> txTimeList = new ArrayList<Long>();
		List<BlockInfo.Builder> blockList = blockHelper.getBatchBlocks(1, 5);
		
		int txCount = 0;
		long confirmTimeSum = 0l;
		log.debug("debug for blockList : " + blockList == null ? " blockList is null" : "blockList is not null");
		if(blockList != null && !blockList.isEmpty()){
			for(BlockInfo.Builder block : blockList){
				if(block.getHeader() != null && block.getHeader().getTimestamp() > 0l){
					long blockTime = block.getHeader().getTimestamp();
					if(blockTime > 0){
						blockTimeList.add(blockTime);
						if(block.getBody() != null && !block.getBody().getTransactionsList().isEmpty()){
							List<Transaction> txList = block.getBody().getTransactionsList();
							for(Transaction tx : txList){
								if(tx.getTimeStamp() > 0){
									txTimeList.add(tx.getTimeStamp());
									 confirmTimeSum += (blockTime - tx.getTimeStamp());
								}
							}
							txCount += txList.size();
						}
					}
				}
			}
		}
		
		double aveBlockTime = 0d;
		if(blockTimeList.size() > 2){
			//排序
			Collections.sort(blockTimeList);
			long allBlockTime = 0l;
			for(int i = 1; i < blockTimeList.size(); i++){
				allBlockTime += (blockTimeList.get(i) - blockTimeList.get(i - 1));
			}
			aveBlockTime = (double)allBlockTime / (blockTimeList.size() - 1);
			aveBlockTime /= THOUSAND;
		}
		
		double aveTxTime = 0d;
		if(txTimeList.size() > 2){
			//排序
			Collections.sort(txTimeList);
			aveTxTime = (double)(txTimeList.get(txTimeList.size() - 1) - txTimeList.get(0)) / txTimeList.size();
			aveTxTime /= THOUSAND;
		}
		
		double confirm = 0d;
		if(confirmTimeSum > 0 && txCount > 0){
			confirmTimeSum /= THOUSAND;//毫秒变秒
			confirm = (double)confirmTimeSum / txCount;
		}
		
		ret.setAvgBlockTime(DataUtil.formateStr(aveBlockTime + ""));
		ret.setTps(DataUtil.formateStr(aveTxTime + ""));
		ret.setConfirm(DataUtil.formateStr(confirm + ""));
		
		return ret;
	}
	
	/**
	 * 获取节点列表中 pending 状态的节点个数
	 * @param nodeList
	 * @return
	 */
	public synchronized int getPendingCount(List<Node> nodeList){
		return getNodeCountByStatus(nodeList, PENDING);
	}
	
	/**
	 * @param nodeList
	 * @return
	 */
	public int getDirectCount(List<Node> nodeList){
		return getNodeCountByStatus(nodeList, DIRECT);
	}
	
	/**
	 * @param nodeList
	 * @param status
	 * @return
	 */
	public int getNodeCountByStatus(List<Node> nodeList, String status){
		int count = 0;
		if(nodeList != null && !nodeList.isEmpty()){
			for(Node node : nodeList){
				if(StringUtils.isNotBlank(node.getStatus()) && node.getStatus().equals(status)){
					count += 1;
				}
			}
		}
		return count;
	}

	/**
	 * @param ret
	 */
	public List<Node> getNodes() {
		List<Node> raftList = getRaftNodes();
		List<Node> dposList = getDposNode();
		List<Node> allNode = new LinkedList<Node>();
		if (raftList != null && !raftList.isEmpty()) {
			allNode.addAll(raftList);
		}

		if (dposList != null && !dposList.isEmpty()) {
			allNode.addAll(dposList);
		}

		return allNode;
	}

	/**
	 * @return
	 */
	public List<Node> getRaftNodes() {
		List<Node> raftList = getNodesBase1(NID_RAFT);
		return raftList;
	}

	/**
	 * @return
	 */
	public List<Node> getDposNode() {
		List<Node> dposList = getNodesBase2(NID_DPOS);
		return dposList;
	}

	/**
	 * @return
	 */
	public List<Node> getNodesBase1(String nodeType) {
		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> reqParam = new HashMap<String, Object>();
		reqParam.put("nid", nodeType);
		FramePacket fp = PacketHelper.buildUrlFromJson(JsonSerializer.formatToString(reqParam), "POST", QUERY_NODE);

		List<Node> list = new LinkedList<Node>();
		try {
			val nodeRet = sender.send(fp, 3000);
			if (nodeRet.getBody() != null && nodeRet.getBody().length > 0) {
				JsonNode jsonObject = mapper.readTree(nodeRet.getBody());

				if (jsonObject != null) {
					
					//相应的要添加节点的类型（raft、dpos）和节点的状态（pending、direct）
					if (jsonObject.has("dnodes")) {
						ArrayNode a = (ArrayNode) jsonObject.get("dnodes");
						for (JsonNode jn : a) {
							list.add(getNodeInfo(jn, nodeType, DIRECT));
						}
					}

					if (jsonObject.has("pnodes")) {
						ArrayNode a = (ArrayNode) jsonObject.get("pnodes");
						for (JsonNode jn : a) {
							list.add(getNodeInfo(jn, nodeType, PENDING));
						}
					}
				}
			} else {
				log.error("request node list is empty");
			}
		} catch (IOException e) {
			log.error(String.format("get node info error : %s", e.getMessage()));
		}

		return list;
	}
	public List<Node> getNodesBase2(String nodeType) {
		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> reqParam = new HashMap<String, Object>();
		reqParam.put("nid", nodeType);
		FramePacket fp = PacketHelper.buildUrlFromJson(JsonSerializer.formatToString(reqParam), "POST", QUERY_NODE);
		
		List<Node> list = new LinkedList<Node>();
		try {
			val nodeRet = sender.send(fp, 3000);
			if (nodeRet.getBody() != null && nodeRet.getBody().length > 0) {
				JsonNode jsonObject = mapper.readTree(nodeRet.getBody());
				
				if (jsonObject != null) {
					
					//相应的要添加节点的类型（raft、dpos）和节点的状态（pending、direct）
					if (jsonObject.has("dnodes")) {
						ArrayNode a = (ArrayNode) jsonObject.get("dnodes");
						for (JsonNode jn : a) {
							list.add(getNodeInfo(jn, nodeType, DIRECT));
						}
					}
					
					if (jsonObject.has("pnodes")) {
						ArrayNode a = (ArrayNode) jsonObject.get("pnodes");
						for (JsonNode jn : a) {
							list.add(getNodeInfo(jn, nodeType, PENDING));
						}
					}
				}
			} else {
				log.error("request node list is empty");
			}
		} catch (IOException e) {
			log.error(String.format("get node info error : %s", e.getMessage()));
		}
		
		return list;
	}

	/**
	 * @param jn
	 * @return
	 */
	public Node getNodeInfo(JsonNode jn, String type, String status) {
		Node.Builder node = Node.newBuilder();
		String node_name = "";
		String uri = "";
		long startup_time = 1l;
		int node_idx = 0;
		long recv_cc = 1l;
		long send_cc = 1l;
		long block_cc = 1l;

		if (jn.has("node_name")) {
			node_name = jn.get("node_name").asText();
			node.setNodeName(node_name);
		}

		if (jn.has("uri")) {
			uri = jn.get("uri").asText();
			node.setUri(uri);
		}

		if (jn.has("startup_time")) {
			startup_time = jn.get("startup_time").asLong();
			node.setStartupTime(startup_time);
		}

		if (jn.has("node_idx")) {
			node_idx = jn.get("node_idx").asInt();
			node.setNodeIdx(node_idx);
		}

		if (jn.has("recv_cc")) {
			recv_cc = jn.get("recv_cc").asLong();
			node.setRecvCc(recv_cc);
		}

		if (jn.has("send_cc")) {
			send_cc = jn.get("send_cc").asLong();
			node.setSendCc(send_cc);
		}

		if (jn.has("block_cc")) {
			block_cc = jn.get("block_cc").asLong();
			node.setBlockCc(block_cc);
		}
		
		node.setStatus(status);
		node.setType(type);

		return node.build();

	}

	public void searchTx(ResGetTxCount.Builder ret, long now) {
		for (int i = 0; i < DELAYS.length; i++) {
			/**
			 * 一次请求拿到所有的分组数据
			 * 所需要的即是计算开始和结束时间，同样是分为20组，但是需要注意，时间段包含开头不包含结尾，[10,12),数据集为：10、11，
			 * 不包含12
			 * 
			 */
			long gt = getGt(DELAYS[i], now);
			long lt = getLt(DELAYS[i], now);
			int[] a = searchTxBetweenRange(gt, lt, DELAYS[i]);
			switch (DELAYS[i]) {
			case "1w":
				for(int j : a){
					Count.Builder count = Count.newBuilder();
					count.setValue(j);
					ret.addWeek(count);
				}
				break;
			case "1d":
				for(int j : a){
					Count.Builder count = Count.newBuilder();
					count.setValue(j);
					ret.addDay(count);
				}
				break;
			case "1h":
				for(int j : a){
					Count.Builder count = Count.newBuilder();
					count.setValue(j);
					ret.addHour(count);
				}
				break;
			case "10m":
				for(int j : a){
					Count.Builder count = Count.newBuilder();
					count.setValue(j);
					ret.addTen(count);
				}
				break;

			default:
				break;
			}
		}
	}

	/**
	 * 最低时间 = 当前时间 - 20 * 间隔
	 * 
	 * @param delay
	 * @param now
	 * @return
	 */
	public synchronized long getGt(String delay, long now) {
		long gt = 0l;
		switch (delay) {
		case "1w":
			gt = now - THOUSAND * GROUP_COUNT * WEEK;
			break;
		case "1d":
			gt = now - THOUSAND * GROUP_COUNT * DAY;
			break;
		case "1h":
			gt = now - THOUSAND * GROUP_COUNT * HOUR;
			break;
		case "10m":
			gt = now - THOUSAND * GROUP_COUNT * TEN_MIN;
			break;
		default:
			break;
		}

		return gt;
	}

	/**
	 * 最高时间 = 当前时间 + 时间间隔
	 * 
	 * @param delay
	 * @param now
	 * @return
	 */
	public synchronized long getLt(String delay, long now) {
		long lt = 0l;

		switch (delay) {
		case "1w":
			lt = now + THOUSAND * WEEK;
			break;
		case "1d":
			lt = now + THOUSAND * DAY;
			break;
		case "1h":
			lt = now + THOUSAND * HOUR;
			break;
		case "10m":
			lt = now + THOUSAND * TEN_MIN;
			break;
		default:
			break;
		}

		return lt;
	}

	/**
	 * 时间段内交易总数
	 * 
	 * @param gt
	 * @param lt
	 * @return
	 */
	public synchronized int[] searchTxBetweenRange(long gt, long lt, String delay) {
		ObjectMapper mapper = new ObjectMapper();
//		
//		ObjectNode param = mapper.createObjectNode();
//		ObjectNode query = mapper.createObjectNode();
//		ObjectNode bool = mapper.createObjectNode();
//		ArrayNode must = mapper.createArrayNode();
//		ObjectNode must1 = mapper.createObjectNode();
//		ObjectNode range = mapper.createObjectNode();
//		ObjectNode timestamp = mapper.createObjectNode();
//		timestamp.put("gt", gt);
//		timestamp.put("lt", lt);
//		range.set("@timestamp", timestamp);
//		must1.set("range", range);
//		must.add(must1);
//		bool.set("must", must);
//		query.set("bool", bool);
//		param.set("query", query);
//		ObjectNode aggs = mapper.createObjectNode();
//		ObjectNode by_time = mapper.createObjectNode();
//		ObjectNode date_histogram = mapper.createObjectNode();
//		date_histogram.put("field", "@timestamp");
//		date_histogram.put("interval", delay);
//
//		by_time.set("date_histogram", date_histogram);
//		aggs.set("by_time", by_time);
//		param.set("aggs", aggs);
		
		String str = "{\"query\" :" +
                "{\"constant_score\" : " +
                        "{\"filter\" : " +
                            "{\"range\" : " +
                                    "{ \"@timestamp\" : " +
                                            "{" +
                                                "\"gte\" : " + gt + "," +
                                                "\"lte\" : " + lt +
                                            "}" +
                                    "}" +
                            "}" +
                        "}" +
                "}," +
                "\"aggs\" : " +
                "{" +
                    "\"by_time\" : " +
                    "{\"date_histogram\" : " +
                        "{" +
                            "\"field\" : \"@timestamp\"," +
                            "\"interval\" : \"" + delay + "\"" +
                        "}" +
                    "}" +
                "}" +
            "}";
		
		log.debug("request txCount between " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSSS").format(new Date(gt))
				+ " and" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSSS").format(new Date(lt)));
		String ret = CallHelper.remoteCallPost(QUERY_TX, str);
		JsonNode jn = null;
		try {
			if(ret != null)
				jn = mapper.readTree(ret);
		} catch (IOException e) {
			log.error("parse string 2 json error while geting recent transactions ");
		}

		List<Long> timeList = new LinkedList<Long>();
		Map<Long, Integer> m = new HashMap<Long, Integer>();

		if (jn != null && jn.has("aggregations") && jn.get("aggregations").has("by_time") && jn.get("aggregations").get("by_time").has("buckets")) {
			ArrayNode nodes = (ArrayNode) jn.get("aggregations").get("by_time").get("buckets");
			for (JsonNode node : nodes) {
				long key = 0l;
				int count = 0;
				if (node.has("key")) {
					key = node.get("key").asLong();
					if (node.has("doc_count")) {
						count = node.get("doc_count").asInt();
					} else {
						count = 0;
					}
					// 时间序列，进行排序
					m.put(key, count);
					// 时间、数量集合，方便根据时间进行数量的获取
					timeList.add(key);

				}
			}
		}

		Collections.sort(timeList, new CompareLongDes());//倒序排序
		int[] a = getFullyCounts(timeList, m, gt, lt, delay);

		return a;
	}

	/**
	 * @param list
	 * @return
	 */
	public int[] getFullyCounts(List<Long> timeList, Map<Long, Integer> countMap, long gt, long lt, String delay) {
		int[] a = new int[GROUP_COUNT];
		if (timeList != null && !timeList.isEmpty()) {
			long firstTime = timeList.get(0);//需要time按照倒序排序，5:20、5:10、5:00
			long startTime = getStartTime(firstTime, lt, delay);
			for(int i = GROUP_COUNT; i > 0; i--){
				if(countMap.get(startTime) != null){
					a[i] = countMap.get(startTime);
				}
				startTime = startTime - getDiff(delay);
			}
		}
		return a;
	}

	/**
	 * @param firstTime
	 * @param lt
	 * @param delay
	 * @return
	 */
	public long getStartTime(long firstTime, long lt, String delay) {
		long startTime = 0l;
		// 获取 delay
		long diff = getDiff(delay);

		long mul = (lt - firstTime) / diff;
		if (mul == 0) {
			startTime = firstTime;
		} else {
			startTime = firstTime + mul * diff;
		}
		return startTime;
	}
	
	/**
	 * @param delay
	 * @return
	 */
	public long getDiff(String delay){
		long diff = 0l;
		switch (delay) {
		case "1w":
			diff = THOUSAND * WEEK;
			break;
		case "1d":
			diff = THOUSAND * DAY;
			break;
		case "1h":
			diff = THOUSAND * HOUR;
			break;
		case "10m":
			diff = THOUSAND * TEN_MIN;
			break;
		default:
			break;
		}
		
		return diff;
	}

}

/**
 * 倒序排序
 * @author jack
 *
 * @param <Long>
 */
class CompareLongDes implements Comparator<java.lang.Long> {

    public int compare(java.lang.Long o1, java.lang.Long o2) {

    	Integer i = new Integer(1);
    	i.intValue();
    	
        long l1 = o1;
        long l2 = o2;
        if(l1 < l2){
            return 1;
        }else{
            return -1;
        }

    }
}
