package org.brewchain.browserAPI.Helper;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
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

	private final static String QUERY_NODE = "http://128.14.133.226:30800/fbs/pzp/pbinf.do";// 30802

	private final static String[] DELAYS = new String[] { "1w", "1d", "1h", "10m" };

	private final static int GROUP_COUNT = 20;// 每组数据个数

	private final static long THOUSAND = 1000;

	private final static long TEN_MIN = 60 * 10;

	private final static long HOUR = 60 * 60;

	private final static long DAY = HOUR * 24;

	private final static long WEEK = DAY * 7;

	public ResGetAdditional.Builder getAdditional() {
		ResGetAdditional.Builder ret = ResGetAdditional.newBuilder();
		getAvgBlockAndTps(ret);

		List<Node> dposList = getDposNode();
		List<Node> raftList = getRaftNodes();
		List<Node> allNode = new LinkedList<Node>();

		// add dpos to node
		if (dposList != null && !dposList.isEmpty()) {
			allNode.addAll(dposList);
		}

		// raft and add raft to node
		if (raftList != null && !raftList.isEmpty()) {
			allNode.addAll(raftList);
			ret.setDpos(raftList.size() + "");
		} else {
			ret.setDpos("0");
		}

		// nodes
		ret.setNodes(allNode.size() + "");

		return ret;
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

	public List<Node> getRaftNodes() {
		List<Node> raftList = getNodesBase("raft");
		return raftList;
	}

	public List<Node> getDposNode() {
		List<Node> dposList = getNodesBase("dpos");
		return dposList;
	}

	/**
	 * @return
	 */
	public List<Node> getNodesBase(String nodeType) {
		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> reqParam = new HashMap<String, Object>();
		reqParam.put("nid", nodeType);
		FramePacket fp = PacketHelper.buildUrlFromJson(JsonSerializer.formatToString(reqParam), "POST", QUERY_NODE);

		List<Node> list = new LinkedList<Node>();
		try {
			val nodeRet = sender.send(fp, 30000);
			if (nodeRet.getBody() != null && nodeRet.getBody().length > 0) {
				JsonNode jsonObject = mapper.readTree(nodeRet.getBody());

				if (jsonObject != null) {
					if (jsonObject.has("dnodes")) {
						ArrayNode a = (ArrayNode) jsonObject.get("dnodes");
						for (JsonNode jn : a) {
							list.add(getNodeInfo(jn));
						}
					}

					if (jsonObject.has("pnodes")) {
						ArrayNode a = (ArrayNode) jsonObject.get("pnodes");
						for (JsonNode jn : a) {
							list.add(getNodeInfo(jn));
						}
					}
				}
			} else {
				log.error("request node list error");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return list;
	}

	/**
	 * @param jn
	 * @return
	 */
	public Node getNodeInfo(JsonNode jn) {
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

		return node.build();

	}

	/**
	 * @param ret
	 */
	public void getAvgBlockAndTps(ResGetAdditional.Builder ret) {
		double avg = 0.1d;
		/**
		 * 1、获取现有块的平均出块时间 1.1、缓存存在则从缓存中取出数据进行计算 1.2、缓存中不存在，则从新计算
		 * 2、计算最新块之后的平均出块时间 2.1、比较缓存中最新区块高度是否是现有最新区块高度 2.1、计算公式：新的平均出块时间 =
		 * （（现有平均出块时间 X 现有平均出块个数）+ 新的出块时间）/ 新的出块个数
		 * 
		 * 
		 * 注意：缓存中还是现实中，平均出块时间都要是 ’秒 ‘
		 */

		try {
			org.brewchain.account.gens.Block.BlockEntity.Builder theBestBlockEntity = blockHelper
					.getTheBestBlockEntity();
			int localBestHeight = 0;
			String localBestHeightStr = BrowserAPILocalCache.additional.get("bestHeight");
			if (localBestHeightStr.equals("0")) {
				// 未缓存任何平均出块时间相关数据
				// 没有缓存则需要重新计算，然后加入到缓存中
				org.brewchain.account.gens.Block.BlockEntity.Builder genesisBlockEntity = blockHelper
						.getGenesisBlockEntity();
				LinkedList<org.brewchain.account.gens.Block.BlockEntity> list = blockHelper.getParentsBlocks(
						theBestBlockEntity.getHeader().getBlockHash().toByteArray(),
						genesisBlockEntity.getHeader().getBlockHash().toByteArray(),
						theBestBlockEntity.getHeader().getNumber());

				calAvg(list, ret);
			} else {
				String avgStr = BrowserAPILocalCache.additional.get("avg");
				// 已经缓存到该块高度的平均出块时间
				localBestHeight = Integer.parseInt(localBestHeightStr);
				if (theBestBlockEntity.getHeader().getNumber() == localBestHeight) {
					// 如果已缓存高度与实际高度相等，直接返回
					// avg = Double.parseDouble(avgStr);
					String tps = BrowserAPILocalCache.additional.get("tps");
					ret.setTps(tps);
					ret.setAvgBlockTime(avgStr);
				} else {
					// 已经计算过的txCount
					int localTxCount = Integer.parseInt(BrowserAPILocalCache.additional.get("txCount"));
					// 计算从该块开始到最新区块的平均出块时间
					org.brewchain.account.gens.Block.BlockEntity oldestBlockEntity = blockHelper
							.getBlockEntityByBlockHeight(localBestHeight);
					LinkedList<org.brewchain.account.gens.Block.BlockEntity> list = blockHelper.getParentsBlocks(
							theBestBlockEntity.getHeader().getBlockHash().toByteArray(),
							oldestBlockEntity.getHeader().getBlockHash().toByteArray(),
							theBestBlockEntity.getHeader().getNumber());
					// 注意：这里计算的平均出块时间只是部分区块的，不完全，需要加上之前的
					calAvg(list, ret);
					double localAvg = Double.parseDouble(avgStr);
					avg = ((Double.parseDouble(ret.getAvgBlockTime()) * list.size()) + (localAvg * localBestHeight))
							/ (list.size() + localBestHeight);// ret中的avg是秒,缓存的avg也是秒
					ret.setAvgBlockTime(DataUtil.formateStr(avg + ""));
					double tps = (Double.parseDouble(ret.getAvgBlockTime()) * (list.size() + localBestHeight))
							/ (ret.getTxCount() + localTxCount);
					ret.setTps(DataUtil.formateStr(tps + ""));// avg
																// 发生变化,tps也会发生变化
				}

			}
			// 重新构建缓存
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

		ret.setAvgBlockTime(DataUtil.formateStr((avg / 1000) + ""));
		ret.setTxCount(txCount);
		ret.setTps(DataUtil.formateStr((Double.parseDouble(ret.getAvgBlockTime()) * blockCount / txCount) + ""));
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
		ObjectNode aggs = mapper.createObjectNode();
		ObjectNode by_time = mapper.createObjectNode();
		ObjectNode date_histogram = mapper.createObjectNode();
		date_histogram.put("field", "@timestamp");
		date_histogram.put("interval", delay);

		by_time.set("date_histogram", date_histogram);
		aggs.set("by_time", by_time);
		param.set("aggs", aggs);
		log.debug("request txCount between " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSSS").format(new Date(gt))
				+ " and" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSSS").format(new Date(lt)));
		String ret = CallHelper.remoteCallPost(QUERY_TX, param.toString());
		JsonNode jn = null;
		try {
			if(ret != null)
				jn = mapper.readTree(ret);
		} catch (IOException e) {
			e.printStackTrace();
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
				System.out.println("request " + delay + ", the startTime is " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSSS").format(new Date(startTime))); 
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
