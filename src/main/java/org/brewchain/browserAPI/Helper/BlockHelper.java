package org.brewchain.browserAPI.Helper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.brewchain.browserAPI.gens.Block.BlockBody;
import org.brewchain.browserAPI.gens.Block.BlockHeader;
import org.brewchain.browserAPI.gens.Block.BlockInfo;
import org.brewchain.browserAPI.gens.Block.BlockMiner;
import org.brewchain.browserAPI.gens.Tx.*;
import org.brewchain.browserAPI.util.DataUtil;
import org.brewchain.evmapi.gens.Block.BlockEntity;
import org.brewchain.evmapi.gens.Tx.MultiTransaction;
import org.brewchain.evmapi.gens.Tx.MultiTransactionBody;
import org.brewchain.evmapi.gens.Tx.MultiTransactionInput;
import org.brewchain.evmapi.gens.Tx.MultiTransactionOutput;
import org.brewchain.rcvm.utils.ByteUtil;
import org.fc.brewchain.bcapi.EncAPI;
import org.fc.brewchain.bcapi.UnitUtil;

import com.google.protobuf.ByteString;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.osgi.annotation.NActorProvider;
import onight.tfw.ntrans.api.ActorService;
import onight.tfw.ntrans.api.annotation.ActorRequire;

/**
 * @author jack
 * 
 *         block 相关信息获取
 * 
 */
@NActorProvider
@Provides(specifications = { ActorService.class }, strategy = "SINGLETON")
@Instantiate(name = "blockHelper")
@Slf4j
@Data
public class BlockHelper implements ActorService {

	@ActorRequire(name = "Block_Helper", scope = "global")
	org.brewchain.account.core.BlockHelper oBlockHelper;

	@ActorRequire(name = "BlockChain_Helper", scope = "global")
	org.brewchain.account.core.BlockChainHelper oBlockChainHelper;

	@ActorRequire(name = "Transaction_Helper", scope = "global")
	org.brewchain.account.core.TransactionHelper oTxHelper;

	@ActorRequire(name = "bc_encoder", scope = "global")
	EncAPI encApi;

	private final static boolean STABLE_BLOCK = false;

	/**
	 * 获取最新的 block
	 * 
	 * @return
	 */
	public BlockInfo getTheBestBlock() {

		BlockInfo block = null;
		org.brewchain.evmapi.gens.Block.BlockEntity.Builder oBlock = null;
		try {
			oBlock = getTheBestBlockEntity();
			if (oBlock != null) {
				block = oBlock2BlockInfo(oBlock.build());
			}
		} catch (Exception e) {
			log.error("get the best block error" + e.getMessage());
		}
		return block;
	}

	/**
	 * 获取最新 block entity
	 * 
	 * @return
	 */
	public org.brewchain.evmapi.gens.Block.BlockEntity.Builder getTheBestBlockEntity() {
		org.brewchain.evmapi.gens.Block.BlockEntity oBlock = null;
		long bestHeight = getLastBlockNumber();
		try {
			oBlock = oBlockChainHelper.getBlockByNumber(bestHeight);
		} catch (Exception e) {
			log.error("get the best block entity error" + e.getMessage());
		}
		if (oBlock != null) {
			return oBlock.toBuilder();
		}
		return null;
	}

	/**
	 * @return
	 */
	public BlockInfo getGenisBlock() {
		BlockInfo block = null;
		org.brewchain.evmapi.gens.Block.BlockEntity.Builder oBlock = null;
		try {
			oBlock = getGenesisBlockEntity();
			if (oBlock != null) {
				block = oBlock2BlockInfo(oBlock.build());
			}
		} catch (Exception e) {
			log.error("get the best block error" + e.getMessage());
		}
		return block;
	}

	/**
	 * 获取 创世区块 entity
	 * 
	 * @return
	 */
	public org.brewchain.evmapi.gens.Block.BlockEntity.Builder getGenesisBlockEntity() {
		org.brewchain.evmapi.gens.Block.BlockEntity.Builder oBlock = null;
		try {
			oBlock = oBlockChainHelper.getGenesisBlock().toBuilder();
		} catch (Exception e) {
			log.error("get genesis block entity error" + e.getMessage());
		}

		return oBlock;
	}

	/**
	 * 获取 所有 区块 entity
	 * 
	 * @return
	 */
	public List<org.brewchain.evmapi.gens.Block.BlockEntity> getAllBlocks() {
		org.brewchain.evmapi.gens.Block.BlockEntity.Builder best = getTheBestBlockEntity();
		org.brewchain.evmapi.gens.Block.BlockEntity.Builder oldest = getGenesisBlockEntity();
		List<org.brewchain.evmapi.gens.Block.BlockEntity> blockLists = null;
		try {
			blockLists = oBlockChainHelper.getParentsBlocks(best.getHeader().getBlockHash(),
					oldest.getHeader().getBlockHash(), best.getHeader().getNumber());
		} catch (Exception e) {
			log.error("get all blocks error" + e.getMessage());
		}

		return blockLists;
	}

	/**
	 * 分页查询 blocks
	 * 
	 * @param pageNo
	 * @param pageSize
	 * @return
	 */
	public List<BlockInfo> getBatchBlocks(int pageNo, int pageSize) {
		List<BlockInfo> retList = null;
		int offset = pageSize * (pageNo - 1);
		try {
			/*
			 * 思路 1、根据 pageNo、 pageSize 获取 页首、页尾 1.1、通过
			 * oBlockChainHelper.getLastBlockNumber() 得到最高区块，倒序，通过 offset 得到页首 1.2、通过
			 * pageSize 和页首获取 页尾
			 * 
			 * 2、获取页首页尾两个 block 的height oBlockChainHelper.getBlockByNumber(int number);
			 * 
			 * 3、再获取到一组 block oBlockChainHelper.getBlocks(byte[] blockHash, byte[]
			 * endBlockHash, int maxCount)
			 */

			long bestHeight = getLastBlockNumber();
			long first = bestHeight - offset;
			if (first > 0) {
				long end = first - pageSize + 1;
				if (end < 0) {
					end = 0;// 第0块
				}
				BlockEntity startBlock = oBlockChainHelper.getBlockByNumber(first);
				BlockEntity endBlock = oBlockChainHelper.getBlockByNumber(end);

				if (startBlock != null && endBlock != null) {
					LinkedList<BlockEntity> list = getParentsBlocks(startBlock.getHeader().getBlockHash(),
							endBlock.getHeader().getBlockHash(), pageSize);
					if (list != null && !list.isEmpty()) {
						retList = new LinkedList<BlockInfo>();
						for (BlockEntity blockEntity : list) {
							BlockInfo block = oBlock2BlockInfo(blockEntity);
							if (block != null)
								retList.add(block);
						}
					}
				}
			}
		} catch (Exception e) {
			log.error("get batch blocks error" + e.getMessage());
		}

		return retList;
	}

	/**
	 * 获取最新区块
	 * 
	 * @return
	 */
	public long getLastBlockNumber() {
		try {
			if (STABLE_BLOCK) {
				long bestHeight = oBlockChainHelper.getLastStableBlockNumber();
				return bestHeight;
			}
			long bestHeight = oBlockChainHelper.getLastBlockNumber();
			return bestHeight;
		} catch (Exception e) {
			log.error("get the last block number error" + e.getMessage());
		}
		return 0;
	}

	/**
	 * 得到父区块
	 * 
	 * @param startBlockHash
	 * @param endBlockHash
	 * @param size
	 * @return
	 */
	public LinkedList<org.brewchain.evmapi.gens.Block.BlockEntity> getParentsBlocks(String startBlockHash,
			String endBlockHash, int size) {
		LinkedList<BlockEntity> list = null;
		try {
			list = oBlockChainHelper.getParentsBlocks(startBlockHash, endBlockHash, size);
		} catch (Exception e) {
			log.error("get parent block error");
		}
		return list;
	}

	/**
	 * 根据 block Hash 获取 block
	 * 
	 * @param blockHash
	 * @return
	 */
	public BlockInfo getBlockByBlockHash(String blockHash) {
		BlockInfo block = null;
		BlockEntity.Builder oBlock = null;
		try {
			oBlock = oBlockHelper.getBlock(blockHash);
			if (oBlock != null) {
				block = oBlock2BlockInfo(oBlock.build());
			}
		} catch (Exception e) {
			log.error("get block by blockhash error :" + e.getMessage());
		}

		return block;
	}

	/**
	 * 通过 blockHeight 获取 block 信息
	 * 
	 * @param blockHeight
	 * @return
	 */
	public BlockInfo getBlockByBlockHeight(long blockHeight) {
		BlockInfo block = null;
		BlockEntity oBlock = null;
		try {
			oBlock = getBlockEntityByBlockHeight(blockHeight);
			if (oBlock != null)
				block = oBlock2BlockInfo(oBlock);
		} catch (Exception e) {
			log.error("get block by blockHeight error " + e.getMessage());
		}
		return block;
	}

	/**
	 * 通过 blockHeight 获取 blockEntity 信息
	 * 
	 * @param blockHeight
	 * @return
	 */
	public org.brewchain.evmapi.gens.Block.BlockEntity getBlockEntityByBlockHeight(long blockHeight) {
		org.brewchain.evmapi.gens.Block.BlockEntity blockEntity = null;
		try {
			blockEntity = oBlockChainHelper.getBlockByNumber(blockHeight);
		} catch (Exception e) {
			log.error("get block entity by blockHeight error " + e.getMessage());
		}

		return blockEntity;
	}

	/**
	 * 通过 txHash 获取 block 信息
	 * 
	 * @param txHash
	 * @return
	 */
	public BlockInfo getBlockByTxHash(String txHash) {
		BlockInfo block = null;
		try {
			BlockEntity oBlock = oBlockHelper.getBlockByTransaction(encApi.hexDec(txHash));
			if (oBlock != null)
				block = oBlock2BlockInfo(oBlock);
		} catch (Exception e) {
			log.error("get block by txHash error " + e.getMessage());
		}
		return block;
	}

	/**
	 * account 中的 block 转成 browserAPI 中的 block
	 * 
	 * @param blockEntity
	 * @return
	 */
	public BlockInfo oBlock2BlockInfo(BlockEntity blockEntity) {
		BlockInfo.Builder block = null;
		if (blockEntity != null) {
			block = BlockInfo.newBuilder();

			BlockBody.Builder blockBody = BlockBody.newBuilder();
			BlockHeader.Builder blockHeader = oBlockHeader2BlockHeader(blockEntity.getHeader());
			long blockHeight = blockHeader.getHeight();

			List<String> list = blockEntity.getHeader().getTxHashsList();
			List<Transaction> txList = new ArrayList<>();
			if (list != null && !list.isEmpty()) {
				for (String string : list) {
					Transaction tx = getTxByTxHash(string);
					tx = tx.toBuilder().setBlockHeight(blockHeight).build();
					blockBody.addTransactions(tx);
					txList.add(tx);
				}
			}

			String aveTx = "0.00";

			// header
			if (blockHeader != null) {
				// miner
				blockHeader.setMiner(oBlockMiner2Miner(blockEntity.getMiner()));
				// nodes
				// List<MultiTransaction> txs =
				// blockEntity.getBody().getTxsList();
				// if (txs != null && !txs.isEmpty()) {
				// for (MultiTransaction tx : txs) {
				// blockHeader.addNodes(tx.getTxNode().getBcuid());// 节点唯一性标识
				// }
				// }

				if (txList.size() > 2) {
					long sumTime = 0L;
					List<Long> times = new ArrayList<>();
					for (int i = 0; i < txList.size(); i++) {
						times.add(txList.get(i).getTimeStamp());
					}

					Collections.sort(times);

					long temp = times.get(0);
					for (int i = 1; i < times.size(); i++) {
						sumTime = times.get(i) - temp;
						temp = times.get(i);
					}

					double a = (double) sumTime / txList.size();
					aveTx = DataUtil.formateStr(a + "");
				}

				blockHeader.setAvetx(aveTx);

				block.setHeader(blockHeader);
			}

			block.setBody(blockBody);
		}

		return block.build();
	}

	/**
	 * @param oBlockMiner
	 * @return
	 */
	public BlockMiner.Builder oBlockMiner2Miner(org.brewchain.evmapi.gens.Block.BlockMiner oBlockMiner) {
		BlockMiner.Builder miner = BlockMiner.newBuilder();
		miner.setNode(StringUtils.isNotBlank(oBlockMiner.getNode()) ? oBlockMiner.getNode() : "");
		miner.setAddress(StringUtils.isNotBlank(oBlockMiner.getAddress()) ? oBlockMiner.getAddress() : "");
		miner.setReward(String.valueOf(UnitUtil.fromWei(ByteUtil.bytesToBigInteger(oBlockMiner.getReward().toByteArray()))));
		miner.setBcuid(StringUtils.isNotBlank(oBlockMiner.getBcuid()) ? oBlockMiner.getBcuid() : "");

		return miner;
	}

	/**
	 * @param oBlockHeader
	 * @return
	 */
	public BlockHeader.Builder oBlockHeader2BlockHeader(org.brewchain.evmapi.gens.Block.BlockHeader oBlockHeader) {
		BlockHeader.Builder header = null;

		// header
		if (oBlockHeader != null) {
			header = BlockHeader.newBuilder();
			header.setBlockHash(oBlockHeader.getBlockHash() != null ? oBlockHeader.getBlockHash() : "");
			header.setParentHash(oBlockHeader.getParentHash() != null ? oBlockHeader.getParentHash() : "");
			header.setTxTrieRoot(oBlockHeader.getTxTrieRoot() != null ? oBlockHeader.getTxTrieRoot() : "");
			header.setTimestamp(oBlockHeader.getTimestamp());
			header.setHeight(oBlockHeader.getNumber());
			// header.setNonce(oBlockHeader.getNonce() != null ?
			// encApi.hexEnc(oBlockHeader.getNonce().toByteArray()) : "");
			header.setSliceId(oBlockHeader.getSliceId());
			if (oBlockHeader.getTxHashsList() != null && !oBlockHeader.getTxHashsList().isEmpty()) {
				header.setTxCount(oBlockHeader.getTxHashsCount());
				for (String bs : oBlockHeader.getTxHashsList()) {
					header.addTxHashs(bs);
				}
			}
		}

		return header;
	}

	/**
	 * @param oBlockBody
	 * @return
	 */
	public BlockBody.Builder oBlockBody2BlockBody(org.brewchain.evmapi.gens.Block.BlockBody oBlockBody) {
		BlockBody.Builder body = null;
		if (oBlockBody != null && !oBlockBody.getTxsList().isEmpty()) {
			body = BlockBody.newBuilder();
			List<MultiTransaction> txs = oBlockBody.getTxsList();
			if (txs != null && !txs.isEmpty()) {
				for (MultiTransaction mt : txs) {
					Transaction tx = OTx2Tx(mt);
					if (tx != null)
						body.addTransactions(tx);
				}
			}
		}

		return body;
	}

	/**
	 * 通过 tx hash 获取 tx 详情
	 * 
	 * @param txHash
	 * @return
	 */
	public Transaction getTxByTxHash(String txHash) {
		Transaction tx = null;
		MultiTransaction mt = null;
		try {
			mt = oTxHelper.GetTransaction(txHash);
			if (mt != null) {
				tx = OTx2Tx(mt);
			}
		} catch (Exception e) {
			log.error("get tx error " + e.getMessage());
		}
		return tx;
	}

	/**
	 * 根据 address 获取 交易 transaction 列表
	 * 
	 * @param address
	 * @return
	 */
	public Map<String, Transaction> getTxByAddress(ByteString address) {
		List<BlockEntity> oBlockEntityList = null;
		Map<String, Transaction> retMap = new HashMap<>();

		org.brewchain.evmapi.gens.Block.BlockEntity.Builder startBlock = getTheBestBlockEntity();
		String startHash = null;
		BlockEntity lastBlock = null;
		while (startBlock != null) {
			startHash = startBlock.getHeader().getBlockHash();
			try {
				oBlockEntityList = oBlockChainHelper.getParentsBlocks(startHash, null, 200);
			} catch (Exception e) {
				log.debug("oBlockChainHelper.getParentsBlocks(startHash,null,200) error");
			}

			if (oBlockEntityList != null && !oBlockEntityList.isEmpty()) {
				int i = 0;
				for (; i < oBlockEntityList.size(); i++) {
					List<MultiTransaction> txList = oBlockEntityList.get(i).getBody().getTxsList();
					if (!txList.isEmpty()) {
						long blockHeight = oBlockEntityList.get(i).getHeader().getNumber();
						for (MultiTransaction oMultiTransaction : oBlockEntityList.get(i).getBody().getTxsList()) {
							String txHash = "";
							Transaction tx = null;
							boolean added = false;
							for (MultiTransactionInput oMultiTransactionInput : oMultiTransaction.getTxBody()
									.getInputsList()) {
								if (oMultiTransactionInput.getAddress().equals(address)) {
									txHash = oMultiTransaction.getTxHash();
									added = true;
									break;
								}
							}
							if (!added) {
								for (MultiTransactionOutput oMultiTransactionOutput : oMultiTransaction.getTxBody()
										.getOutputsList()) {
									if (oMultiTransactionOutput.getAddress().equals(address)) {
										txHash = oMultiTransaction.getTxHash();
										break;
									}
								}
							}
							if (StringUtils.isNotBlank(txHash)) {
								tx = getTxByTxHash(txHash);
								if (tx != null) {
									tx = tx.toBuilder().setBlockHeight(blockHeight).build();
									retMap.put(txHash, tx);
								}
							}
						}
					}
				}
				lastBlock = oBlockEntityList.get(i - 1);

				if (lastBlock.getHeader().getNumber() == 0) {
					break;
				}

				startBlock = getBlockEntityByBlockHeight(lastBlock.getHeader().getNumber() - 1).toBuilder();
			} else {
				break;
			}
		}

		return retMap;
	}

	public long getBlockHeightByTxHash(String txHash) {
		long blockHeight = 0;
		BlockEntity oBlock = null;
		try {
			oBlock = oBlockHelper.getBlockByTransaction(encApi.hexDec(txHash));
			if (oBlock != null && oBlock.getHeader() != null) {
				blockHeight = oBlock.getHeader().getNumber();
			}
		} catch (Exception e) {
			log.error("get block by txHash error " + e.getMessage());
		}

		return blockHeight;
	}

	/**
	 * account 中 transaction 转成 browserAPI 中的 transaction
	 * 
	 * @param mtx
	 * @return
	 */
	public Transaction OTx2Tx(MultiTransaction mtx) {

		// 获取区块的高度
		long blockHeight = getBlockHeightByTxHash(mtx.getTxHash());
		// int blockHeight = 1;

		// 交易内容
		MultiTransactionBody mtBody = mtx.getTxBody();

		// 交易时间
		long timeStamp = mtBody.getTimestamp();

		// 交易状态
		String txStatus = mtx.getStatus();

		// data
		ByteString data = mtBody.getData();

		// 委托代理
		List<ByteString> delegates = mtBody.getDelegateList();
		List<String> delegateStrs = null;
		if (delegates != null && !delegates.isEmpty()) {
			delegateStrs = new LinkedList<String>();
			for (ByteString byteStr : delegates) {
				delegateStrs.add(encApi.hexEnc(byteStr.toByteArray()));
			}
		}

		// 输入
		List<MultiTransactionInput> mtxInput = mtBody.getInputsList();
		List<TxInput.Builder> froms = null;
		if (mtxInput != null && !mtxInput.isEmpty()) {
			froms = new LinkedList<TxInput.Builder>();
			for (MultiTransactionInput mtxI : mtxInput) {
				TxInput.Builder input = TxInput.newBuilder();
				input.setAddress(encApi.hexEnc(mtxI.getAddress().toByteArray()));
				input.setAmount(
						String.valueOf(UnitUtil.fromWei(ByteUtil.bytesToBigInteger(mtxI.getAmount().toByteArray()))));
				input.setCryptoToken(encApi.hexEnc(mtxI.getCryptoToken().toByteArray()));
				input.setFee(0);
				input.setNonce(mtxI.getNonce());
				input.setPubKey(mtxI.getPubKey() == null ? encApi.hexEnc(mtxI.getPubKey().toByteArray()) : "");
				input.setSymbol(StringUtils.isNotBlank(mtxI.getSymbol()) ? mtxI.getSymbol() : "");
				input.setToken(StringUtils.isNotBlank(mtxI.getToken()) ? mtxI.getToken() : "");
				froms.add(input);
			}
		}
		// 输出
		List<MultiTransactionOutput> mtxOutput = mtBody.getOutputsList();
		List<TxOutput.Builder> tos = null;
		if (mtxOutput != null && !mtxOutput.isEmpty()) {
			tos = new LinkedList<TxOutput.Builder>();
			for (MultiTransactionOutput mto : mtxOutput) {
				TxOutput.Builder output = TxOutput.newBuilder();
				output.setAddress(encApi.hexEnc(mto.getAddress().toByteArray()));
				output.setAmount(
						String.valueOf(UnitUtil.fromWei(ByteUtil.bytesToBigInteger(mto.getAmount().toByteArray()))));
				output.setCryptoToken(encApi.hexEnc(mto.getCryptoToken().toByteArray()));
				output.setSymbol(StringUtils.isNotBlank(mto.getSymbol()) ? mto.getSymbol() : "");
				tos.add(output);
			}
		}

		// 构建对象
		Transaction tx = getTransactionEntityByParams(mtx.getTxHash(), txStatus, blockHeight, timeStamp, froms, tos,
				delegateStrs, data);

		return tx;
	}

	/**
	 * 构建 transaction 对象
	 * 
	 * @param txHash
	 * @param blockHeight
	 * @param timeStamp
	 * @param froms
	 * @param tos
	 * @param delegates
	 * @param data
	 * @return
	 */
	public Transaction getTransactionEntityByParams(String txHash, String txStatus, long blockHeight, long timeStamp,
			List<TxInput.Builder> froms, List<TxOutput.Builder> tos, List<String> delegates, ByteString data) {
		Transaction.Builder tx = Transaction.newBuilder();

		tx.setTxHash(txHash);
		tx.setBlockHeight(blockHeight);

		tx.setTimeStamp(timeStamp);

		if (StringUtils.isNotBlank(txStatus)) {
			tx.setStatus(txStatus);
		} else {
			tx.setStatus("null");
		}

		if (froms != null && !froms.isEmpty()) {
			for (TxInput.Builder txInput : froms) {
				if (txInput != null)
					tx.addFroms(txInput);
			}
		}

		if (tos != null && !tos.isEmpty()) {
			for (TxOutput.Builder txOutput : tos) {
				if (txOutput != null)
					tx.addTos(txOutput);
			}
		}

		if (delegates != null && !delegates.isEmpty()) {
			for (String delegate : delegates) {
				if (delegate != null)
					tx.addDelegates(delegate);
			}
		}

		if (data != null)
			tx.setData(encApi.hexEnc(data.toByteArray()));

		return tx.build();
	}

	/************* 测试 ****************/

	public List<String> getBlocksHash() {
		List<String> list = new ArrayList<String>();

		return list;
	}
}
