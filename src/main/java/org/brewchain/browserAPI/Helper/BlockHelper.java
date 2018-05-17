package org.brewchain.browserAPI.Helper;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.brewchain.account.gens.Block.BlockEntity;
import org.brewchain.account.gens.Tx.MultiTransaction;
import org.brewchain.account.gens.Tx.MultiTransactionBody;
import org.brewchain.account.gens.Tx.MultiTransactionInput;
import org.brewchain.account.gens.Tx.MultiTransactionOutput;
import org.brewchain.browserAPI.gens.Address.AddressInfo;
import org.brewchain.browserAPI.gens.Block.BlockBody;
import org.brewchain.browserAPI.gens.Block.BlockHeader;
import org.brewchain.browserAPI.gens.Block.BlockInfo;
import org.brewchain.browserAPI.gens.Tx.Transaction;
import org.brewchain.browserAPI.gens.Tx.TxInput;
import org.brewchain.browserAPI.gens.Tx.TxOutput;
import org.brewchain.browserAPI.util.DataUtil;
import org.fc.brewchain.bcapi.EncAPI;

import com.google.protobuf.ByteString;
import com.google.protobuf.ProtocolStringList;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.osgi.annotation.NActorProvider;
import onight.tfw.ntrans.api.ActorService;
import onight.tfw.ntrans.api.annotation.ActorRequire;
import onight.tfw.outils.serialize.UUIDGenerator;

/**
 * @author jack
 * 
 * block 相关信息获取
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
	
	
	/**
	 * 获取最新的 block 
	 * @return
	 */
	public BlockInfo.Builder getTheBestBlock(){
		BlockInfo.Builder block = BlockInfo.newBuilder();
		org.brewchain.account.gens.Block.BlockEntity.Builder oBlock = null;
		try {
			oBlock = getTheBestBlockEntity();
			if(oBlock != null){
				block = oBlock2BlockInfo(oBlock.build());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return block;
	}
	
	/**
	 * 获取最新 block entity
	 * @return
	 */
	public org.brewchain.account.gens.Block.BlockEntity.Builder getTheBestBlockEntity(){
		org.brewchain.account.gens.Block.BlockEntity.Builder oBlock = null;
		try {
			oBlock = oBlockHelper.GetBestBlock();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return oBlock;
	}
	
	/**
	 * 获取 最新 block height
	 * @return
	 */
	public synchronized int getTheBestBlockHeight(){
		org.brewchain.account.gens.Block.BlockEntity.Builder blockEntity = getTheBestBlockEntity();
		int theBestBlockHeight = 1;
		if(blockEntity != null && blockEntity.getHeader() != null){
			theBestBlockHeight = blockEntity.getHeader().getNumber();
		}
		return theBestBlockHeight;
	}
	
	/**
	 * 获取 创世区块 entity
	 * @return
	 */
	public org.brewchain.account.gens.Block.BlockEntity.Builder getGenesisBlockEntity(){
		org.brewchain.account.gens.Block.BlockEntity.Builder oBlock = null;
		try {
			oBlock = oBlockChainHelper.getGenesisBlock().toBuilder();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return oBlock;
	}
	
	/**
	 * 获取 所有 区块 entity
	 * @return
	 */
	public List<org.brewchain.account.gens.Block.BlockEntity> getAllBlocks(){
		org.brewchain.account.gens.Block.BlockEntity.Builder best = getTheBestBlockEntity();
		org.brewchain.account.gens.Block.BlockEntity.Builder oldest = getGenesisBlockEntity();
		List<org.brewchain.account.gens.Block.BlockEntity> blockLists = null;
		try {
			blockLists = oBlockChainHelper.getParentsBlocks(best.getHeader().getBlockHash().toByteArray(), oldest.getHeader().getBlockHash().toByteArray(), best.getHeader().getNumber());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return blockLists;
	}
	
	/**
	 * 分页查询 blocks
	 * @param pageNo
	 * @param pageSize
	 * @return
	 */
	public List<BlockInfo.Builder> getBatchBlocks(int pageNo, int pageSize){
		List<BlockInfo.Builder> retList = null;
		int offset = pageSize * (pageNo - 1);
		try {
			/*
			 * 思路
			 * 1、根据 pageNo、 pageSize 获取 页首、页尾
			 * 		1.1、通过 oBlockChainHelper.getLastBlockNumber() 得到最高区块，倒序，通过 offset 得到页首
			 * 		1.2、通过 pageSize 和页首获取 页尾
			 * 
			 * 2、获取页首页尾两个 block 的height
			 * oBlockChainHelper.getBlockByNumber(int number);
			 * 
			 * 3、再获取到一组 block
			 * oBlockChainHelper.getBlocks(byte[] blockHash, byte[] endBlockHash, int maxCount)
			 */
			
			int bestHeight = oBlockChainHelper.getLastBlockNumber();
			int first = bestHeight - offset;
			if(first < 0)
				first = 1;
			int end = first - pageSize + 1;
			if(end < 0){
				end = 1;
			}
			
			BlockEntity startBlock = oBlockChainHelper.getBlockByNumber(first);
			BlockEntity endBlock = oBlockChainHelper.getBlockByNumber(end);
			
			if(startBlock != null && endBlock != null){
				LinkedList<BlockEntity> list = getParentsBlocks(startBlock.getHeader().getBlockHash().toByteArray(), endBlock.getHeader().getBlockHash().toByteArray(), pageSize);
				if(list != null && !list.isEmpty()){
					retList = new LinkedList<BlockInfo.Builder>();
					for (BlockEntity blockEntity : list) {
						BlockInfo.Builder block = oBlock2BlockInfo(blockEntity);
						if(block != null)
							retList.add(block);
					}
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return retList;
	}
	
	/**
	 * 得到父区块
	 * @param startBlockHash
	 * @param endBlockHash
	 * @param size
	 * @return
	 */
	public LinkedList<org.brewchain.account.gens.Block.BlockEntity> getParentsBlocks(byte[] startBlockHash, byte[] endBlockHash, int size){
		LinkedList<BlockEntity> list = null;
		try {
			list = oBlockChainHelper.getParentsBlocks(startBlockHash, endBlockHash, size);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return list;
	}
	
	/**
	 * 根据 block Hash 获取 block 
	 * @param blockHash
	 * @return
	 */
	public BlockInfo.Builder getBlockByBlockHash(byte[] blockHash){
		BlockInfo.Builder block = null;
		BlockEntity.Builder oBlock = null;
		try {
			oBlock = oBlockHelper.getBlock(blockHash);
			if(oBlock != null){
				block = oBlock2BlockInfo(oBlock.build());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return block;
	}
	
	/**
	 * 通过 blockHeight 获取 block 信息
	 * @param blockHeight
	 * @return
	 */
	public BlockInfo.Builder getBlockByBlockHeight(int blockHeight){
		BlockInfo.Builder block = BlockInfo.newBuilder();
		BlockEntity oBlock = null;
		try {
			oBlock = getBlockEntityByBlockHeight(blockHeight);
			if(oBlock != null)
				block = oBlock2BlockInfo(oBlock);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return block;
	}
	
	/**
	 * 通过 blockHeight 获取 blockEntity 信息
	 * @param blockHeight
	 * @return
	 */
	public org.brewchain.account.gens.Block.BlockEntity getBlockEntityByBlockHeight(int blockHeight){
		org.brewchain.account.gens.Block.BlockEntity blockEntity = null;
		try {
			blockEntity = oBlockChainHelper.getBlockByNumber(blockHeight);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return blockEntity;
	}
	
	/**
	 * 通过 txHash 获取 block 信息
	 * 
	 * @param txHash
	 * @return
	 */
	public BlockInfo.Builder getBlockByTxHash(byte[] txHash){
		BlockInfo.Builder block = BlockInfo.newBuilder();
		try {
			BlockEntity oBlock = oBlockHelper.getBlockByTransaction(txHash);
			if(oBlock != null)
				block = oBlock2BlockInfo(oBlock);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return block;
	}
	
	/**
	 * account 中的 block 转成 browserAPI 中的 block
	 * @param blockEntity
	 * @return
	 */
	public BlockInfo.Builder oBlock2BlockInfo(BlockEntity blockEntity){
		BlockInfo.Builder block = BlockInfo.newBuilder();
		org.brewchain.account.gens.Block.BlockHeader oBlockHeader = blockEntity.getHeader();
		org.brewchain.account.gens.Block.BlockBody oBlockBody = blockEntity.getBody();
	
		//header
		BlockHeader.Builder blockHeader = oBlockHeader2BlockHeader(oBlockHeader); 
		if(blockHeader != null)
			block.setHeader(blockHeader);
		
		//body
//		BlockBody.Builder blockBody = oBlockBody2BlockBody(oBlockBody); 
//		if(blockBody != null)
//			block.setBody(blockBody);
		
		BlockBody.Builder blockBody = BlockBody.newBuilder();
		List<ByteString>  list = oBlockHeader.getTxHashsList();
		for (ByteString string : list) {
			Transaction.Builder tx = getTxByTxHash(string.toByteArray());
			blockBody.addTransactions(tx);
		}
		
		block.setBody(blockBody);
		
		return block;
	}
	
	public BlockHeader.Builder oBlockHeader2BlockHeader(org.brewchain.account.gens.Block.BlockHeader oBlockHeader){
		BlockHeader.Builder header = null;
	
		//header
		if(oBlockHeader != null){
			header = BlockHeader.newBuilder();
			header.setBlockHash(DataUtil.byteString2String(oBlockHeader.getBlockHash(), encApi));
			header.setParentHash(DataUtil.byteString2String(oBlockHeader.getParentHash(), encApi));
			header.setCoinbase(DataUtil.byteString2String(oBlockHeader.getCoinbase(), encApi));
			header.setTxTrieRoot(DataUtil.byteString2String(oBlockHeader.getTxTrieRoot(), encApi));
			header.setTimestamp(oBlockHeader.getTimestamp());
			header.setHeight(oBlockHeader.getNumber());
			header.setReward(DataUtil.byteString2String(oBlockHeader.getReward(), encApi));
			header.setNonce(DataUtil.byteString2String(oBlockHeader.getNonce(), encApi));
			header.setSliceId(oBlockHeader.getSliceId());
			if(oBlockHeader.getTxHashsList() != null && !oBlockHeader.getTxHashsList().isEmpty()){
				header.setTxCount(oBlockHeader.getTxHashsCount());
				for(ByteString bs : oBlockHeader.getTxHashsList()){
					header.addTxHashs(DataUtil.byteString2String(bs, encApi));
				}
			}
			
			//TODO 
			AddressInfo.Builder addressInfo = AddressInfo.newBuilder();
			addressInfo.addAddress(UUIDGenerator.generate());
			addressInfo.setBalance(new Random().nextInt(1000));
			header.setMiner(addressInfo);
			
			header.addNodes("127.0.0.1");
		}
		
		return header;
	}
	
	public BlockBody.Builder oBlockBody2BlockBody(org.brewchain.account.gens.Block.BlockBody oBlockBody){
		BlockBody.Builder body = null;
		if(oBlockBody != null && !oBlockBody.getTxsList().isEmpty()){
			body = BlockBody.newBuilder();
			List<MultiTransaction> txs = oBlockBody.getTxsList();
			if(txs != null){
				for (MultiTransaction mt : txs) {
					Transaction.Builder tx = OTx2Tx(mt);
					if(tx != null)
						body.addTransactions(tx);
				}
			}
		}
		
		return body;
	}
	
	/**
	 * 通过 tx hash 获取 tx 详情
	 * @param txHash
	 * @return
	 */
	public Transaction.Builder getTxByTxHash(byte[] txHash){
		Transaction.Builder tx = null;
		MultiTransaction mt = null;
		try {
			mt = oTxHelper.GetTransaction(txHash);
			if(mt != null){
				tx = OTx2Tx(mt);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return tx;
	}
	
	/**
	 * 根据 address 获取 交易 transaction 列表 
	 * @param address	
	 * @return
	 */
	public LinkedList<Transaction.Builder> getTxByAddress(byte[] address){
		LinkedList<MultiTransaction> list = null;
		LinkedList<Transaction.Builder> retList = null;
		try {
			list = oBlockHelper.getTransactionByAddress(address);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		//存在交易
		if(list != null && !list.isEmpty()){
			//构建返回队列
			retList = new LinkedList<Transaction.Builder>();
			for (MultiTransaction mt : list) {
				Transaction.Builder tx = OTx2Tx(mt);
				if(tx != null)
					retList.add(tx);
			}
		}
		
		return retList;
	}
	
	public int getBlockHeightByTxHash(byte[] txHash){
		int blockHeight = 0;
		try {
			BlockEntity oBlock = oBlockHelper.getBlockByTransaction(txHash);
			if(oBlock != null && oBlock.getHeader() != null){
				blockHeight = oBlock.getHeader().getNumber();
			}
		} catch (Exception e) {
			e.printStackTrace();
		};
		return blockHeight;
	}
	
	/**
	 * account 中 transaction 转成 browserAPI 中的 transaction
	 * 
	 * @param mtx
	 * @return
	 */
	public Transaction.Builder OTx2Tx(MultiTransaction mtx){

		//获取区块的高度
		int blockHeight = getBlockHeightByTxHash(mtx.getTxHash().toByteArray());
//		int blockHeight = 1;
		
		//交易内容
		MultiTransactionBody mtBody = mtx.getTxBody();
		
		//交易时间
		long timeStamp = mtBody.getTimestamp();
		
		//交易状态
		System.out.println("the txHash is " + encApi.hexEnc(mtx.getTxHash().toByteArray()) + " and the status is " + mtx.getStatus());
		String txStatus = mtx.getStatus();
		
		// data
		ByteString data = mtBody.getData();
		
		//委托代理
		List<ByteString> delegates = mtBody.getDelegateList();
		List<String> delegateStrs = null;
		if(delegates != null && !delegates.isEmpty()){
			delegateStrs = new LinkedList<String>();
			for(ByteString byteStr : delegates){
				delegateStrs.add(DataUtil.byteString2String(byteStr, encApi));
			}
		}
		
		//输入
		List<MultiTransactionInput> mtxInput = mtBody.getInputsList();
		List<TxInput.Builder> froms = null;
		if(mtxInput != null && !mtxInput.isEmpty()){
			froms = new LinkedList<TxInput.Builder>();
			for (MultiTransactionInput mtxI : mtxInput) {
				TxInput.Builder input = TxInput.newBuilder();
				input.setAddress(DataUtil.byteString2String(mtxI.getAddress(), encApi));
				input.setAmount(mtxI.getAmount());
				input.setCryptoToken(DataUtil.byteString2String(mtxI.getCryptoToken(), encApi));
				input.setFee(mtxI.getFee());
				input.setNonce(mtxI.getNonce());
				input.setPubKey(StringUtils.isNotBlank(mtxI.getPubKey()) ? mtxI.getPubKey() : "");
				input.setSymbol(StringUtils.isNotBlank(mtxI.getSymbol()) ? mtxI.getSymbol() : "");
				input.setToken(StringUtils.isNotBlank(mtxI.getToken()) ? mtxI.getToken() : "");
				froms.add(input);
			}
		}
		//输出
		List<MultiTransactionOutput> mtxOutput = mtBody.getOutputsList();
		List<TxOutput.Builder> tos = null;
		if(mtxOutput != null && !mtxOutput.isEmpty()){
			tos = new LinkedList<TxOutput.Builder>();
			for (MultiTransactionOutput mto : mtxOutput) {
				TxOutput.Builder output = TxOutput.newBuilder();
				output.setAddress(DataUtil.byteString2String(mto.getAddress(), encApi));
				output.setAmount(mto.getAmount());
				output.setCryptoToken(DataUtil.byteString2String(mto.getCryptoToken(), encApi));
				output.setSymbol(StringUtils.isNotBlank(mto.getSymbol()) ? mto.getSymbol() : "");
				tos.add(output);
			}
		}
		
		//构建对象
		Transaction.Builder tx = getTransactionEntityByParams(mtx.getTxHash(), txStatus, blockHeight, timeStamp, froms, tos, delegateStrs, data);
	
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
	public Transaction.Builder getTransactionEntityByParams(ByteString txHash, String txStatus, int blockHeight, long timeStamp, List<TxInput.Builder> froms, List<TxOutput.Builder> tos, List<String> delegates, ByteString data){
		Transaction.Builder tx = Transaction.newBuilder();
	
		tx.setTxHash(DataUtil.byteString2String(txHash, encApi));
		tx.setBlockHeight(blockHeight);
		
		tx.setTimeStamp(timeStamp);
		
		if(StringUtils.isNotBlank(txStatus)){
			tx.setStatus(txStatus);
		}else{
			tx.setStatus("null");
		}
		
		if(froms != null && !froms.isEmpty()){
			for(TxInput.Builder txInput : froms){
				if(txInput != null)
					tx.addFroms(txInput);
			}
		}
		
		if(tos != null && !tos.isEmpty()){
			for(TxOutput.Builder txOutput : tos){
				if(txOutput != null)
					tx.addTos(txOutput);
			}
		}
		
		if(delegates != null && !delegates.isEmpty()){
			for(String delegate : delegates){
				if(delegate != null)
					tx.addDelegates(delegate);
			}
		}
		
		if(data != null)
			tx.setData(DataUtil.byteString2String(data, encApi));
		
		return tx;
	}	
}
