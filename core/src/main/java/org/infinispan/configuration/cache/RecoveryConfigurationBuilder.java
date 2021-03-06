package org.infinispan.configuration.cache;

import org.infinispan.commons.configuration.Builder;
import org.infinispan.commons.CacheConfigurationException;
import org.infinispan.configuration.global.GlobalConfiguration;

import static org.infinispan.configuration.cache.RecoveryConfiguration.DEFAULT_RECOVERY_INFO_CACHE;

/**
 * Defines recovery configuration for the cache.
 *
 * @author pmuir
 *
 */
public class RecoveryConfigurationBuilder extends AbstractTransportConfigurationChildBuilder implements Builder<RecoveryConfiguration> {

   boolean enabled = true;
   private String recoveryInfoCacheName = DEFAULT_RECOVERY_INFO_CACHE;

   RecoveryConfigurationBuilder(TransactionConfigurationBuilder builder) {
      super(builder);
   }

   /**
    * Enable recovery for this cache
    */
   public RecoveryConfigurationBuilder enable() {
      this.enabled = true;
      return this;
   }

   /**
    * Disable recovery for this cache
    */
   public RecoveryConfigurationBuilder disable() {
      this.enabled = false;
      return this;
   }

   /**
    * Enable recovery for this cache
    */
   public RecoveryConfigurationBuilder enabled(boolean enabled) {
      this.enabled = enabled;
      return this;
   }

   /**
    * Sets the name of the cache where recovery related information is held. If not specified
    * defaults to a cache named {@link RecoveryConfiguration#DEFAULT_RECOVERY_INFO_CACHE}
    */
   public RecoveryConfigurationBuilder recoveryInfoCacheName(String recoveryInfoName) {
      this.recoveryInfoCacheName = recoveryInfoName;
      return this;
   }

   @Override
   public void validate() {
      if (!enabled || transaction().useSynchronization()) {
         return;
      }
      if (!clustering().cacheMode().isSynchronous()) {
         throw new CacheConfigurationException("Recovery not supported with Asynchronous " +
                                                clustering().cacheMode().friendlyCacheModeString() + " cache mode.");
      }
      if (!transaction().syncCommitPhase()) {
         //configuration not supported because the Transaction Manager would not retain any transaction log information to
         //allow it to perform useful recovery anyhow. Usually you just log it in the hope a human notices and sorts
         //out the mess. Of course properly paranoid humans don't use async commit in the first place.
         throw new CacheConfigurationException("Recovery not supported with asynchronous commit phase.");
      }
   }

   @Override
   public void validate(GlobalConfiguration globalConfig) {
   }

   @Override
   public RecoveryConfiguration create() {
      return new RecoveryConfiguration(enabled, recoveryInfoCacheName);
   }

   @Override
   public RecoveryConfigurationBuilder read(RecoveryConfiguration template) {
      this.enabled = template.enabled();
      this.recoveryInfoCacheName = template.recoveryInfoCacheName();

      return this;
   }

   @Override
   public String toString() {
      return "RecoveryConfigurationBuilder{" +
            "enabled=" + enabled +
            ", recoveryInfoCacheName='" + recoveryInfoCacheName + '\'' +
            '}';
   }
}
