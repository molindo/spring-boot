/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.couchbase;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseBucket;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.cluster.ClusterInfo;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.env.DefaultCouchbaseEnvironment;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.couchbase.config.CouchbaseConfigurer;

/**
 * {@link EnableAutoConfiguration Auto-Configuration} for Couchbase.
 *
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @since 1.4.0
 */
@Configuration
@ConditionalOnClass({CouchbaseBucket.class, Cluster.class})
@Conditional(CouchbaseAutoConfiguration.CouchbaseCondition.class)
@EnableConfigurationProperties(CouchbaseProperties.class)
public class CouchbaseAutoConfiguration {


	@Configuration
	@ConditionalOnMissingBean(CouchbaseConfigurer.class)
	public static class CouchbaseConfiguration {

		private final CouchbaseProperties properties;

		public CouchbaseConfiguration(CouchbaseProperties properties) {
			this.properties = properties;
		}

		@Bean
		@Primary
		public CouchbaseEnvironment couchbaseEnvironment() throws Exception {
			return createEnvironment(this.properties);
		}

		@Bean
		@Primary
		public Cluster couchbaseCluster() throws Exception {
			return CouchbaseCluster.create(couchbaseEnvironment(),
					this.properties.getBootstrapHosts());
		}

		@Bean
		@Primary
		public ClusterInfo couchbaseClusterInfo() throws Exception {
			return couchbaseCluster().clusterManager(this.properties.getBucket().getName(),
					this.properties.getBucket().getPassword()).info();
		}

		@Bean
		@Primary
		public Bucket couchbaseClient() throws Exception {
			return couchbaseCluster().openBucket(this.properties.getBucket().getName(),
					this.properties.getBucket().getPassword());
		}

		/**
		 * Create a {@link CouchbaseEnvironment} based on the specified settings.
		 * @param properties the couchbase properties to use
		 * @return a {@link CouchbaseEnvironment}
		 */
		protected CouchbaseEnvironment createEnvironment(CouchbaseProperties properties) {
			CouchbaseProperties.Endpoints endpoints = properties.getEnv().getEndpoints();
			CouchbaseProperties.Timeouts timeouts = properties.getEnv().getTimeouts();
			DefaultCouchbaseEnvironment.Builder builder = DefaultCouchbaseEnvironment.builder()
					.connectTimeout(timeouts.getConnect())
					.kvEndpoints(endpoints.getKeyValue())
					.kvTimeout(timeouts.getKeyValue())
					.queryEndpoints(endpoints.getQuery())
					.queryTimeout(timeouts.getQuery())
					.viewEndpoints(endpoints.getView())
					.viewTimeout(timeouts.getView());
			CouchbaseProperties.Ssl ssl = properties.getEnv().getSsl();
			if (ssl.getEnabled()) {
				builder.sslEnabled(true);
				if (ssl.getKeyStore() != null) {
					builder.sslKeystoreFile(ssl.getKeyStore());
				}
				if (ssl.getKeyStorePassword() != null) {
					builder.sslKeystorePassword(ssl.getKeyStorePassword());
				}
			}
			return builder.build();
		}

	}

	/**
	 * Determine if Couchbase should be configured. This happens if either the
	 * user-configuration defines a {@link CouchbaseConfigurer} or if at least
	 * the "bootstrapHosts" property is specified.
	 */
	static class CouchbaseCondition extends AnyNestedCondition {

		CouchbaseCondition() {
			super(ConfigurationPhase.REGISTER_BEAN);
		}

		@ConditionalOnProperty(prefix = "spring.couchbase", name = "bootstrapHosts")
		static class BootstrapHostsProperty {
		}

		@ConditionalOnBean(CouchbaseConfigurer.class)
		static class CouchbaseConfigurerAvailable {
		}

	}

}
