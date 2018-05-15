package org.brewchain.browserAPI.Helper;

import java.util.concurrent.TimeUnit;

import org.brewchain.account.core.AbstractLocalCache;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * @author jack
 *
 */
public abstract class BrowserAPILocalCache extends AbstractLocalCache {
	/**
	 * key: avg、tps、 node、 dpos
	 */

	public static LoadingCache<String, String> additional = CacheBuilder.newBuilder().maximumSize(10000)
			.expireAfterAccess(86400, TimeUnit.SECONDS).build(new CacheLoader<String, String>() {
				@Override
				public String load(String key) throws Exception {
					return "0";//找不到对应的value时返回的默认值
				}
			});
}
