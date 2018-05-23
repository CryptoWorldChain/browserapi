package org.brewchain.browserAPI.test;

import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.brewchain.account.gens.Tx.MultiTransaction;
import org.brewchain.account.gens.Tx.MultiTransactionBody;
import org.brewchain.account.gens.Tx.MultiTransactionInput;
import org.brewchain.account.gens.Tx.MultiTransactionOutput;
import org.brewchain.account.gens.Tx.MultiTransactionSignature;
import org.brewchain.browserAPI.Helper.BlockHelper;
import org.brewchain.browserAPI.gens.Lct.PLCTCommand;
import org.brewchain.browserAPI.gens.Lct.PLCTModule;
import org.brewchain.browserAPI.gens.Lct.ReqDtx;
import org.brewchain.browserAPI.gens.Lct.RetDtx;
import org.fc.brewchain.bcapi.EncAPI;
import org.fc.brewchain.bcapi.KeyPairs;

import com.google.protobuf.ByteString;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.oapi.scala.commons.SessionModules;
import onight.osgi.annotation.NActorProvider;
import onight.tfw.async.CompleteHandler;
import onight.tfw.ntrans.api.annotation.ActorRequire;
import onight.tfw.otransio.api.PacketHelper;
import onight.tfw.otransio.api.beans.FramePacket;

@NActorProvider
@Slf4j
@Data
public class DoTransaction extends SessionModules<ReqDtx> {

	@ActorRequire(name = "Transaction_Helper", scope = "global")
	org.brewchain.account.core.TransactionHelper oTransactionHelper;

	@ActorRequire(name = "Account_Helper", scope = "global")
	org.brewchain.account.core.AccountHelper oAccountHelper;

	@ActorRequire(name = "bc_encoder", scope = "global")
	EncAPI encApi;

	private static int count = 0;

	@Override
	public String[] getCmds() {
		return new String[] { PLCTCommand.DTX.name() };
	}

	@Override
	public String getModule() {
		return PLCTModule.LCT.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqDtx pb, final CompleteHandler handler) {
		RetDtx.Builder ret = RetDtx.newBuilder();
		if (pb != null) {
			if (StringUtils.isNotBlank(pb.getIntputAddr())) {
				if (StringUtils.isNotBlank(pb.getIntputPuk())) {
					if (StringUtils.isNotBlank(pb.getIntputPki())) {
						if (StringUtils.isNotBlank(pb.getOutputAddr())) {

							try {
								int nonce = oAccountHelper.getNonce(encApi.hexDec(pb.getIntputAddr()));

								MultiTransaction.Builder oMultiTransaction = MultiTransaction.newBuilder();
								MultiTransactionBody.Builder oMultiTransactionBody = MultiTransactionBody.newBuilder();
								MultiTransactionInput.Builder oMultiTransactionInput4 = MultiTransactionInput.newBuilder();
								oMultiTransactionInput4.setAddress(ByteString.copyFrom(encApi.hexDec(pb.getIntputAddr())));
								oMultiTransactionInput4.setAmount(pb.getAmount());
								oMultiTransactionInput4.setFee(0);
								oMultiTransactionInput4.setFeeLimit(0);
								oMultiTransactionInput4.setNonce(nonce);
								oMultiTransactionBody.addInputs(oMultiTransactionInput4);

								MultiTransactionOutput.Builder oMultiTransactionOutput1 = MultiTransactionOutput.newBuilder();
								oMultiTransactionOutput1.setAddress(ByteString.copyFrom(encApi.hexDec(pb.getOutputAddr())));
								oMultiTransactionOutput1.setAmount(pb.getAmount());
								oMultiTransactionBody.addOutputs(oMultiTransactionOutput1);

								oMultiTransactionBody.setData(ByteString.EMPTY);
								oMultiTransaction.setTxHash(ByteString.EMPTY);
								oMultiTransactionBody.clearSignatures();

								oMultiTransactionBody.setTimestamp(System.currentTimeMillis());
								// 签名
								MultiTransactionSignature.Builder oMultiTransactionSignature21 = MultiTransactionSignature.newBuilder();
								oMultiTransactionSignature21.setPubKey(pb.getIntputPuk());
								oMultiTransactionSignature21.setSignature(encApi.hexEnc(encApi.ecSign(pb.getIntputPki(), oMultiTransactionBody.build().toByteArray())));
								oMultiTransactionBody.addSignatures(oMultiTransactionSignature21);

								oMultiTransaction.setTxBody(oMultiTransactionBody);
								ByteString txHashByte = oTransactionHelper.CreateMultiTransaction(oMultiTransaction);
								count += 1;
								ret.setTxHash(encApi.hexEnc(txHashByte.toByteArray()));
								ret.setRetCode(1).setMsg("success");
							} catch (Exception e) {
								e.printStackTrace();
								log.debug(String.format("=====> 执行 %s 交易异常 %s", count, e.getMessage()));
							}
						} else {
							ret.setRetCode(-1).setMsg("output address is null");
						}
					} else {
						ret.setRetCode(-1).setMsg("input privateKey is null");
					}
				} else {
					ret.setRetCode(-1).setMsg("input publicKey is null");
				}
			} else {
				ret.setRetCode(-1).setMsg("input address is null");
			}
		} else {
			ret.setRetCode(-1).setMsg("no params");
		}

		handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
	}
}